package com.example.action

import java.util.Locale

class NovaActionParser {

    fun parse(input: String, hasPendingConfirmation: Boolean = false): ParsedCommand? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Pending Confirmation Resolution
        if (hasPendingConfirmation) {
            if (isConfirmation(lower)) {
                return ParsedCommand(NovaActionType.CONFIRM_ACTION, rawQuery = trimmed)
            }
            if (isCancellation(lower)) {
                return ParsedCommand(NovaActionType.CANCEL_ACTION, rawQuery = trimmed)
            }
        }

        // 2. Flashlight Commands
        if (matchesAny(lower, listOf(
                "turn on flashlight", "turn on the flashlight", "turn on torch", "turn on the torch",
                "flashlight on", "torch on", "switch on flashlight", "switch on torch",
                "torch on karo", "flashlight on karo", "torch jalao", "flashlight jalao",
                "batti jalao", "light on", "turn on light"
            ))) {
            return ParsedCommand(NovaActionType.FLASHLIGHT_ON, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "turn off flashlight", "turn off the flashlight", "turn off torch", "turn off the torch",
                "flashlight off", "torch off", "switch off flashlight", "switch off torch",
                "torch off karo", "flashlight off karo", "torch band karo", "flashlight band karo",
                "flashlight band kardo", "batti bujhao", "light off", "turn off light"
            ))) {
            return ParsedCommand(NovaActionType.FLASHLIGHT_OFF, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "toggle flashlight", "toggle torch", "flashlight", "torch"
            )) && !lower.contains("app")) {
            return ParsedCommand(NovaActionType.FLASHLIGHT_TOGGLE, rawQuery = trimmed)
        }

        // 3. Battery Status
        if (matchesAny(lower, listOf(
                "battery", "battery percentage", "battery level", "battery status",
                "how much battery", "what is my battery", "check battery", "check the battery",
                "battery kitni hai", "battery check karo", "charge kitna hai", "phone charge kitna hai",
                "is phone charging", "battery info"
            ))) {
            return ParsedCommand(NovaActionType.GET_BATTERY_STATUS, rawQuery = trimmed)
        }

        // 4. Device Time & Date
        if (matchesAny(lower, listOf(
                "what time is it", "what's the time", "tell me the time", "current time",
                "what is the time", "time kya hua hai", "waqt kya hua hai", "time batao",
                "what day is it", "what is today's date", "aaj kya date hai", "aaj ka din",
                "what is the date"
            ))) {
            return ParsedCommand(NovaActionType.GET_CURRENT_TIME, rawQuery = trimmed)
        }

        // 5. Wi-Fi and Bluetooth Settings
        if (matchesAny(lower, listOf(
                "open wifi", "open wi-fi", "open wifi settings", "open wi-fi settings",
                "wifi settings", "wi-fi settings", "wifi kholo", "wi-fi kholo", "turn on wifi",
                "connect wifi", "show wifi"
            ))) {
            return ParsedCommand(NovaActionType.OPEN_WIFI_SETTINGS, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "open bluetooth", "open bluetooth settings", "bluetooth settings",
                "bluetooth kholo", "turn on bluetooth", "connect bluetooth", "show bluetooth"
            ))) {
            return ParsedCommand(NovaActionType.OPEN_BLUETOOTH_SETTINGS, rawQuery = trimmed)
        }

        // 6. Specific Priority Apps: YouTube, WhatsApp, Chrome, Settings
        if (matchesAny(lower, listOf(
                "open youtube", "launch youtube", "start youtube", "youtube kholo", "youtube open karo"
            ))) {
            val searchQuery = extractQueryAfter(lower, "youtube search", "search on youtube", "on youtube search")
            return ParsedCommand(NovaActionType.OPEN_YOUTUBE, searchQuery = searchQuery, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "open whatsapp", "launch whatsapp", "start whatsapp", "whatsapp kholo", "whatsapp open karo"
            ))) {
            return ParsedCommand(NovaActionType.OPEN_WHATSAPP, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "open chrome", "launch chrome", "open google chrome", "chrome kholo", "chrome open karo",
                "open browser", "launch browser", "browser kholo"
            ))) {
            return ParsedCommand(NovaActionType.OPEN_CHROME, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "open settings", "launch settings", "phone settings", "device settings",
                "settings kholo", "settings open karo"
            ))) {
            return ParsedCommand(NovaActionType.OPEN_SETTINGS, rawQuery = trimmed)
        }

        // 7. Volume Functions
        if (matchesAny(lower, listOf(
                "volume up", "increase volume", "raise volume", "turn up volume",
                "louder", "awaz barhao", "volume barhao", "sound barhao", "more volume"
            ))) {
            return ParsedCommand(NovaActionType.VOLUME_UP, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "volume down", "decrease volume", "lower volume", "turn down volume",
                "quieter", "awaz kam karo", "volume kam karo", "sound kam karo", "less volume"
            ))) {
            return ParsedCommand(NovaActionType.VOLUME_DOWN, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "mute volume", "mute audio", "mute sound", "mute", "silence",
                "awaz band karo", "mute karo"
            ))) {
            return ParsedCommand(NovaActionType.VOLUME_MUTE, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "max volume", "maximum volume", "full volume", "volume 100", "volume full", "full sound"
            ))) {
            return ParsedCommand(NovaActionType.VOLUME_MAX, rawQuery = trimmed)
        }

        // 8. Media Playback Functions
        if (matchesAny(lower, listOf(
                "play music", "play song", "resume music", "resume song", "play audio",
                "gana chalao", "play media", "music chalao", "music on"
            ))) {
            return ParsedCommand(NovaActionType.MEDIA_PLAY, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "pause music", "stop music", "pause song", "stop song", "pause audio",
                "gana roko", "gana band karo", "pause media", "music roko", "pause"
            ))) {
            return ParsedCommand(NovaActionType.MEDIA_PAUSE, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "next song", "next track", "skip song", "skip track", "agla gana"
            ))) {
            return ParsedCommand(NovaActionType.MEDIA_NEXT, rawQuery = trimmed)
        }

        if (matchesAny(lower, listOf(
                "previous song", "previous track", "pichla gana", "last song"
            ))) {
            return ParsedCommand(NovaActionType.MEDIA_PREVIOUS, rawQuery = trimmed)
        }

        // 9. Phone Call (Always requires user confirmation!)
        val callPattern = Regex("^(?:call|dial|phone|make a call to)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val urduCallPattern = Regex("^(.+?)\\s+(?:ko call karo|ko call milao|ko phone karo)$", RegexOption.IGNORE_CASE)
        val callMatch = callPattern.find(trimmed) ?: urduCallPattern.find(trimmed)
        if (callMatch != null) {
            val target = callMatch.groupValues[1].trim()
            if (target.isNotBlank() && !target.equals("me", true) && !target.equals("back", true)) {
                return ParsedCommand(
                    actionType = NovaActionType.CALL_PHONE,
                    phoneNumber = target,
                    rawQuery = trimmed
                )
            }
        }

        // 10. SMS / Text Message (Always requires user confirmation!)
        val smsPattern = Regex("^(?:send\\s+sms|send\\s+message|text|send\\s+text)\\s+to\\s+(.+?)(?:\\s+(?:saying|that|with message|message)\\s+(.+))?$", RegexOption.IGNORE_CASE)
        val smsMatch = smsPattern.find(trimmed)
        if (smsMatch != null) {
            val target = smsMatch.groupValues[1].trim()
            val msgBody = smsMatch.groupValues.getOrNull(2)?.trim()
            if (target.isNotBlank()) {
                return ParsedCommand(
                    actionType = NovaActionType.SEND_SMS,
                    phoneNumber = target,
                    messageBody = msgBody,
                    rawQuery = trimmed
                )
            }
        }

        // Urdu SMS pattern: "X ko message bhejo Y"
        val urduSmsPattern = Regex("^(.+?)\\s+ko\\s+(?:message|sms)\\s+bhejo(?:\\s+(.+))?$", RegexOption.IGNORE_CASE)
        val urduSmsMatch = urduSmsPattern.find(trimmed)
        if (urduSmsMatch != null) {
            val target = urduSmsMatch.groupValues[1].trim()
            val msgBody = urduSmsMatch.groupValues.getOrNull(2)?.trim()
            if (target.isNotBlank()) {
                return ParsedCommand(
                    actionType = NovaActionType.SEND_SMS,
                    phoneNumber = target,
                    messageBody = msgBody,
                    rawQuery = trimmed
                )
            }
        }

        // 11. Generic "Open <App>"
        val openAppPattern = Regex("^(?:open|launch|start)\\s+([a-zA-Z0-9\\s_.-]+?)(?:\\s+app)?$", RegexOption.IGNORE_CASE)
        val urduOpenPattern = Regex("^([a-zA-Z0-9\\s_.-]+?)\\s+(?:kholo|open karo|chalao)$", RegexOption.IGNORE_CASE)
        val openMatch = openAppPattern.find(trimmed) ?: urduOpenPattern.find(trimmed)
        if (openMatch != null) {
            val appName = openMatch.groupValues[1].trim()
            if (appName.isNotBlank() && !isIgnoredAppWord(appName)) {
                return ParsedCommand(
                    actionType = NovaActionType.OPEN_APP,
                    targetApp = appName,
                    rawQuery = trimmed
                )
            }
        }

        return null
    }

    private fun isConfirmation(lower: String): Boolean {
        return matchesAny(lower, listOf(
            "yes", "confirm", "proceed", "sure", "call him", "call her", "call them",
            "call now", "send it", "send message", "send sms", "do it", "haan",
            "theek hai", "bhejo", "call mila do", "haan kardo", "okay", "ok", "yes please",
            "go ahead"
        ))
    }

    private fun isCancellation(lower: String): Boolean {
        return matchesAny(lower, listOf(
            "no", "cancel", "stop", "abort", "don't", "don't call", "don't send",
            "nahi", "mat karo", "ruk jao", "rehne do", "chhor do", "no thanks",
            "never mind", "nevermind", "dismiss"
        ))
    }

    private fun isIgnoredAppWord(word: String): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        return lower in listOf(
            "the", "it", "this", "that", "up", "down", "on", "off", "time", "date",
            "battery", "settings", "wifi", "bluetooth", "torch", "flashlight"
        )
    }

    private fun matchesAny(input: String, candidates: List<String>): Boolean {
        val clean = input.replace(Regex("[?.!,]"), "").trim()
        return candidates.any { candidate ->
            clean == candidate || clean.startsWith("$candidate ") || clean.endsWith(" $candidate")
        }
    }

    private fun extractQueryAfter(input: String, vararg prefixes: String): String? {
        for (prefix in prefixes) {
            val idx = input.indexOf(prefix)
            if (idx != -1) {
                val query = input.substring(idx + prefix.length).trim()
                if (query.isNotBlank()) return query
            }
        }
        return null
    }
}
