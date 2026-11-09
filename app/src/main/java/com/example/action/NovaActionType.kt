package com.example.action

enum class NovaActionType {
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    FLASHLIGHT_TOGGLE,
    GET_BATTERY_STATUS,
    GET_CURRENT_TIME,
    OPEN_YOUTUBE,
    OPEN_WHATSAPP,
    OPEN_CHROME,
    OPEN_SETTINGS,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_APP,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_MAX,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_TOGGLE,
    MEDIA_NEXT,
    MEDIA_PREVIOUS,
    CALL_PHONE,
    SEND_SMS,
    CONFIRM_ACTION,
    CANCEL_ACTION
}

data class ParsedCommand(
    val actionType: NovaActionType,
    val targetApp: String? = null,
    val phoneNumber: String? = null,
    val messageBody: String? = null,
    val searchQuery: String? = null,
    val rawQuery: String
)

data class PendingConfirmation(
    val id: String,
    val actionType: NovaActionType,
    val title: String,
    val description: String,
    val target: String,
    val extraData: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class NovaActionResult(
    val isHandled: Boolean,
    val isSuccess: Boolean,
    val spokenText: String,
    val displayText: String = spokenText,
    val emotion: String = "warm",
    val requiresConfirmation: Boolean = false,
    val pendingConfirmation: PendingConfirmation? = null,
    val errorMessage: String? = null
)
