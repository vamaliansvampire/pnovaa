package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceSettings(
    val pitch: Float = 1.12f,
    val rate: Float = 1.0f,
    val volume: Float = 1.0f,
    val voiceName: String = "",
    val language: String = "auto", // "auto", "en", "ur", "roman_urdu"
    val continuousConversation: Boolean = false,
    val textOnlyMode: Boolean = false,
    val emotionalDelivery: Boolean = true,
    val customApiKey: String = ""
)

class NovaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_ai_preferences", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<VoiceSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): VoiceSettings = _settingsFlow.value

    fun updatePitch(pitch: Float) {
        prefs.edit().putFloat("pitch", pitch).apply()
        refresh()
    }

    fun updateRate(rate: Float) {
        prefs.edit().putFloat("rate", rate).apply()
        refresh()
    }

    fun updateVoiceName(voiceName: String) {
        prefs.edit().putString("voice_name", voiceName).apply()
        refresh()
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        refresh()
    }

    fun setContinuousConversation(enabled: Boolean) {
        prefs.edit().putBoolean("continuous_conv", enabled).apply()
        refresh()
    }

    fun setTextOnlyMode(enabled: Boolean) {
        prefs.edit().putBoolean("text_only", enabled).apply()
        refresh()
    }

    fun setEmotionalDelivery(enabled: Boolean) {
        prefs.edit().putBoolean("emotional_delivery", enabled).apply()
        refresh()
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        refresh()
    }

    private fun loadSettings(): VoiceSettings {
        return VoiceSettings(
            pitch = prefs.getFloat("pitch", 1.12f),
            rate = prefs.getFloat("rate", 1.0f),
            volume = prefs.getFloat("volume", 1.0f),
            voiceName = prefs.getString("voice_name", "") ?: "",
            language = prefs.getString("language", "auto") ?: "auto",
            continuousConversation = prefs.getBoolean("continuous_conv", false),
            textOnlyMode = prefs.getBoolean("text_only", false),
            emotionalDelivery = prefs.getBoolean("emotional_delivery", true),
            customApiKey = prefs.getString("custom_api_key", "") ?: ""
        )
    }

    private fun refresh() {
        _settingsFlow.value = loadSettings()
    }
}
