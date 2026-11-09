package com.example.action

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NovaActionExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private var torchCameraId: String? = null

    init {
        findTorchCameraId()
        registerTorchCallback()
    }

    private fun findTorchCameraId() {
        try {
            val cm = cameraManager ?: return
            for (id in cm.cameraIdList) {
                val characteristics = cm.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            if (torchCameraId == null) {
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        torchCameraId = id
                        break
                    }
                }
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    private fun registerTorchCallback() {
        try {
            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchCameraId) {
                        _isFlashlightOn.value = enabled
                    }
                }
            }, null)
        } catch (_: Exception) {}
    }

    fun execute(
        command: ParsedCommand,
        activePending: PendingConfirmation? = null
    ): NovaActionResult {
        return when (command.actionType) {
            NovaActionType.FLASHLIGHT_ON -> setFlashlight(true)
            NovaActionType.FLASHLIGHT_OFF -> setFlashlight(false)
            NovaActionType.FLASHLIGHT_TOGGLE -> setFlashlight(!_isFlashlightOn.value)
            NovaActionType.GET_BATTERY_STATUS -> getBatteryStatus()
            NovaActionType.GET_CURRENT_TIME -> getCurrentTimeAndDate()
            NovaActionType.OPEN_YOUTUBE -> openYouTube(command.searchQuery)
            NovaActionType.OPEN_WHATSAPP -> openWhatsApp()
            NovaActionType.OPEN_CHROME -> openChrome()
            NovaActionType.OPEN_SETTINGS -> openSettings()
            NovaActionType.OPEN_WIFI_SETTINGS -> openWifiSettings()
            NovaActionType.OPEN_BLUETOOTH_SETTINGS -> openBluetoothSettings()
            NovaActionType.OPEN_APP -> openInstalledApp(command.targetApp.orEmpty())
            NovaActionType.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            NovaActionType.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            NovaActionType.VOLUME_MUTE -> muteVolume()
            NovaActionType.VOLUME_MAX -> setMaxVolume()
            NovaActionType.MEDIA_PLAY -> controlMedia(KeyEvent.KEYCODE_MEDIA_PLAY, "resumed")
            NovaActionType.MEDIA_PAUSE -> controlMedia(KeyEvent.KEYCODE_MEDIA_PAUSE, "paused")
            NovaActionType.MEDIA_TOGGLE -> controlMedia(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "toggled")
            NovaActionType.MEDIA_NEXT -> controlMedia(KeyEvent.KEYCODE_MEDIA_NEXT, "skipped to next track")
            NovaActionType.MEDIA_PREVIOUS -> controlMedia(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "switched to previous track")
            NovaActionType.CALL_PHONE -> requestCallConfirmation(command.phoneNumber.orEmpty())
            NovaActionType.SEND_SMS -> requestSmsConfirmation(command.phoneNumber.orEmpty(), command.messageBody.orEmpty())
            NovaActionType.CONFIRM_ACTION -> executeConfirmedAction(activePending)
            NovaActionType.CANCEL_ACTION -> cancelPendingAction(activePending)
        }
    }

    // 1. FLASHLIGHT
    private fun setFlashlight(enable: Boolean): NovaActionResult {
        val cm = cameraManager
        if (cm == null) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Camera service is not available on this device, Sir.",
                emotion = "serious",
                errorMessage = "CameraManager service unavailable"
            )
        }

        if (torchCameraId == null) {
            findTorchCameraId()
        }

        val id = torchCameraId
        if (id == null) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "This device does not appear to have an accessible camera flash.",
                emotion = "serious",
                errorMessage = "Flash hardware not found"
            )
        }

        return try {
            cm.setTorchMode(id, enable)
            _isFlashlightOn.value = enable
            val spoken = if (enable) {
                "Flashlight has been turned on, Sir."
            } else {
                "Flashlight is now turned off, Sir."
            }
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "warm"
            )
        } catch (e: CameraAccessException) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to access the flashlight at this moment. The camera may be in use.",
                emotion = "empathetic",
                errorMessage = e.localizedMessage
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Flashlight control encountered an error: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    // 2. BATTERY
    private fun getBatteryStatus(): NovaActionResult {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryFilter)

        if (batteryStatus == null) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to read battery telemetry from the system.",
                emotion = "serious",
                errorMessage = "Battery telemetry unavailable"
            )
        }

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val percentage = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            -1
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "via AC power"
            BatteryManager.BATTERY_PLUGGED_USB -> "via USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wirelessly"
            else -> "fast charging"
        }

        val spoken = when {
            percentage < 0 -> "Battery level is currently unavailable from device telemetry."
            isCharging && percentage >= 100 -> "Your battery is fully charged at 100%, Sir."
            isCharging -> "Your battery is currently at $percentage% and charging $plugType, Sir."
            percentage <= 15 -> "Your battery is low at $percentage%, Sir. Consider connecting your charger soon."
            else -> "Your battery is currently at $percentage% and discharging normally, Sir."
        }

        val emotion = if (percentage in 1..15 && !isCharging) "serious" else "warm"

        return NovaActionResult(
            isHandled = true,
            isSuccess = true,
            spokenText = spoken,
            displayText = spoken,
            emotion = emotion
        )
    }

    // 3. TIME AND DATE
    private fun getCurrentTimeAndDate(): NovaActionResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)

        val spoken = "The time is currently $timeStr on $dateStr, Sir."
        return NovaActionResult(
            isHandled = true,
            isSuccess = true,
            spokenText = spoken,
            displayText = spoken,
            emotion = "warm"
        )
    }

    // 4. OPEN APPS
    private fun openYouTube(searchQuery: String?): NovaActionResult {
        return try {
            val intent = if (!searchQuery.isNullOrBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}")).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?:
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            val spoken = if (!searchQuery.isNullOrBlank()) {
                "Searching YouTube for \"$searchQuery\", Sir."
            } else {
                "Opening YouTube now, Sir."
            }

            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "warm"
            )
        } catch (e: Exception) {
            // Web fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                NovaActionResult(
                    isHandled = true,
                    isSuccess = true,
                    spokenText = "Opening YouTube in browser, Sir.",
                    displayText = "Opening YouTube in browser, Sir.",
                    emotion = "warm"
                )
            } catch (e2: Exception) {
                NovaActionResult(
                    isHandled = true,
                    isSuccess = false,
                    spokenText = "Unable to launch YouTube on this device.",
                    emotion = "serious",
                    errorMessage = e2.localizedMessage
                )
            }
        }
    }

    private fun openWhatsApp(): NovaActionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = "Opening WhatsApp now, Sir.",
                displayText = "Opening WhatsApp now, Sir.",
                emotion = "warm"
            )
        } else {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "WhatsApp does not appear to be installed on this device, Sir.",
                displayText = "WhatsApp is not installed on this device.",
                emotion = "serious",
                errorMessage = "WhatsApp package not installed"
            )
        }
    }

    private fun openChrome(): NovaActionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
        return try {
            val intent = launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = "Opening Google Chrome, Sir.",
                displayText = "Opening Google Chrome, Sir.",
                emotion = "warm"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to open Chrome or browser: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun openSettings(): NovaActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = "Opening device Settings, Sir.",
                displayText = "Opening device Settings, Sir.",
                emotion = "warm"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to open device Settings.",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun openWifiSettings(): NovaActionResult {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = "Opening Wi-Fi settings, Sir.",
                displayText = "Opening Wi-Fi settings, Sir.",
                emotion = "warm"
            )
        } catch (e: Exception) {
            openSettings()
        }
    }

    private fun openBluetoothSettings(): NovaActionResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = "Opening Bluetooth settings, Sir.",
                displayText = "Opening Bluetooth settings, Sir.",
                emotion = "warm"
            )
        } catch (e: Exception) {
            openSettings()
        }
    }

    private fun openInstalledApp(appName: String): NovaActionResult {
        if (appName.isBlank()) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Please tell me which app you would like me to open, Sir.",
                emotion = "warm"
            )
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val cleanTarget = appName.lowercase(Locale.ROOT).trim()

        var bestMatchPkg: String? = null
        var bestMatchLabel: String? = null

        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val labelLower = label.lowercase(Locale.ROOT)
            val pkg = info.activityInfo.packageName.lowercase(Locale.ROOT)

            if (labelLower == cleanTarget || labelLower.contains(cleanTarget) || pkg.contains(cleanTarget)) {
                bestMatchPkg = info.activityInfo.packageName
                bestMatchLabel = label
                if (labelLower == cleanTarget) break
            }
        }

        if (bestMatchPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(bestMatchPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val spoken = "Opening $bestMatchLabel, Sir."
                return NovaActionResult(
                    isHandled = true,
                    isSuccess = true,
                    spokenText = spoken,
                    displayText = spoken,
                    emotion = "warm"
                )
            }
        }

        val failSpoken = "I could not find an installed app named \"$appName\" on this device, Sir."
        return NovaActionResult(
            isHandled = true,
            isSuccess = false,
            spokenText = failSpoken,
            displayText = failSpoken,
            emotion = "serious",
            errorMessage = "App not found: $appName"
        )
    }

    // 5. VOLUME FUNCTIONS
    private fun adjustVolume(direction: Int): NovaActionResult {
        val am = audioManager ?: return NovaActionResult(
            isHandled = true,
            isSuccess = false,
            spokenText = "Audio manager is unavailable on this device.",
            emotion = "serious",
            errorMessage = "AudioManager unavailable"
        )

        return try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = if (maxVol > 0) (currentVol * 100) / maxVol else currentVol

            val actionWord = if (direction == AudioManager.ADJUST_RAISE) "increased" else "lowered"
            val spoken = "Volume $actionWord to $percent%, Sir."

            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "warm"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Could not adjust volume: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun muteVolume(): NovaActionResult {
        val am = audioManager ?: return NovaActionResult(
            isHandled = true,
            isSuccess = false,
            spokenText = "Audio manager is unavailable on this device.",
            emotion = "serious"
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            } else {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            }
            val spoken = "Media audio has been muted, Sir."
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "warm"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to mute volume: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun setMaxVolume(): NovaActionResult {
        val am = audioManager ?: return NovaActionResult(
            isHandled = true,
            isSuccess = false,
            spokenText = "Audio manager is unavailable on this device.",
            emotion = "serious"
        )

        return try {
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
            val spoken = "Volume set to maximum (100%), Sir."
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "excited"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Could not set maximum volume: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    // 6. MEDIA PLAYBACK
    private fun controlMedia(keyCode: Int, actionDescription: String): NovaActionResult {
        val am = audioManager ?: return NovaActionResult(
            isHandled = true,
            isSuccess = false,
            spokenText = "Audio service is unavailable on this device.",
            emotion = "serious"
        )

        return try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            val spoken = "Media playback $actionDescription, Sir."
            NovaActionResult(
                isHandled = true,
                isSuccess = true,
                spokenText = spoken,
                displayText = spoken,
                emotion = "warm"
            )
        } catch (e: Exception) {
            NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Unable to control media playback: ${e.localizedMessage}",
                emotion = "serious",
                errorMessage = e.localizedMessage
            )
        }
    }

    // 7. CALL & SMS REQUIRING USER CONFIRMATION
    private fun requestCallConfirmation(phoneNumberOrContact: String): NovaActionResult {
        val clean = phoneNumberOrContact.trim()
        if (clean.isBlank()) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Please specify a phone number or contact to call, Sir.",
                emotion = "warm"
            )
        }

        val pending = PendingConfirmation(
            id = UUID.randomUUID().toString(),
            actionType = NovaActionType.CALL_PHONE,
            title = "Confirm Phone Call",
            description = "NOVA is ready to place a call to: $clean",
            target = clean
        )

        val spoken = "I am ready to call $clean. Please confirm to proceed, Sir."
        val display = "📞 Call Confirmation Requested: $clean\nTap Confirm or say \"Yes\" to proceed."

        return NovaActionResult(
            isHandled = true,
            isSuccess = true,
            spokenText = spoken,
            displayText = display,
            emotion = "warm",
            requiresConfirmation = true,
            pendingConfirmation = pending
        )
    }

    private fun requestSmsConfirmation(phoneNumberOrContact: String, messageBody: String): NovaActionResult {
        val cleanTarget = phoneNumberOrContact.trim()
        if (cleanTarget.isBlank()) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "Please specify a recipient for your SMS, Sir.",
                emotion = "warm"
            )
        }

        val body = messageBody.ifBlank { "Hello from NOVA AI" }

        val pending = PendingConfirmation(
            id = UUID.randomUUID().toString(),
            actionType = NovaActionType.SEND_SMS,
            title = "Confirm SMS Message",
            description = "NOVA has drafted an SMS to $cleanTarget:\n\"$body\"",
            target = cleanTarget,
            extraData = body
        )

        val spoken = "I have drafted your message to $cleanTarget: \"$body\". Please confirm to send, Sir."
        val display = "💬 SMS Confirmation Requested:\nTo: $cleanTarget\nMessage: \"$body\"\nTap Confirm or say \"Yes\" to send."

        return NovaActionResult(
            isHandled = true,
            isSuccess = true,
            spokenText = spoken,
            displayText = display,
            emotion = "warm",
            requiresConfirmation = true,
            pendingConfirmation = pending
        )
    }

    private fun executeConfirmedAction(pending: PendingConfirmation?): NovaActionResult {
        if (pending == null) {
            return NovaActionResult(
                isHandled = true,
                isSuccess = false,
                spokenText = "There is no pending action to confirm, Sir.",
                emotion = "neutral"
            )
        }

        return when (pending.actionType) {
            NovaActionType.CALL_PHONE -> {
                try {
                    // ACTION_DIAL is the safest, Android 14 compliant intent that opens dialer ready with number
                    val dialUri = Uri.parse("tel:${Uri.encode(pending.target)}")
                    val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                    val spoken = "Opening phone dialer for ${pending.target}, Sir."
                    NovaActionResult(
                        isHandled = true,
                        isSuccess = true,
                        spokenText = spoken,
                        displayText = spoken,
                        emotion = "warm"
                    )
                } catch (e: Exception) {
                    NovaActionResult(
                        isHandled = true,
                        isSuccess = false,
                        spokenText = "Unable to launch phone dialer: ${e.localizedMessage}",
                        emotion = "serious",
                        errorMessage = e.localizedMessage
                    )
                }
            }
            NovaActionType.SEND_SMS -> {
                try {
                    val smsUri = Uri.parse("smsto:${Uri.encode(pending.target)}")
                    val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", pending.extraData ?: "")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    val spoken = "Opening message composer for ${pending.target}, Sir."
                    NovaActionResult(
                        isHandled = true,
                        isSuccess = true,
                        spokenText = spoken,
                        displayText = spoken,
                        emotion = "warm"
                    )
                } catch (e: Exception) {
                    NovaActionResult(
                        isHandled = true,
                        isSuccess = false,
                        spokenText = "Unable to open SMS composer: ${e.localizedMessage}",
                        emotion = "serious",
                        errorMessage = e.localizedMessage
                    )
                }
            }
            else -> {
                NovaActionResult(
                    isHandled = true,
                    isSuccess = true,
                    spokenText = "Action confirmed, Sir.",
                    emotion = "warm"
                )
            }
        }
    }

    private fun cancelPendingAction(pending: PendingConfirmation?): NovaActionResult {
        val spoken = if (pending != null) {
            "The ${pending.title.lowercase(Locale.ROOT)} has been canceled, Sir."
        } else {
            "Action canceled, Sir."
        }
        return NovaActionResult(
            isHandled = true,
            isSuccess = true,
            spokenText = spoken,
            displayText = spoken,
            emotion = "warm"
        )
    }
}
