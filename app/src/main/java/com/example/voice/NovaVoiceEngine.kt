package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.pref.VoiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class VoiceOption(
    val id: String,
    val displayName: String,
    val locale: String,
    val isFemale: Boolean
)

enum class NovaEmotion(val label: String, val pitchModifier: Float, val rateModifier: Float) {
    NEUTRAL("Calm & Poised", 0.0f, 0.0f),
    WARM("Warm & Caring", 0.02f, -0.04f),
    EXCITED("Happy & Enthusiastic", 0.08f, 0.08f),
    EMPATHETIC("Empathetic & Gentle", -0.05f, -0.12f),
    PLAYFUL("Playful & Charming", 0.06f, 0.04f),
    SERIOUS("Calm & Authoritative", -0.07f, -0.06f)
}

class NovaVoiceEngine(
    private val context: Context,
    private val onSpeechFinishedCallback: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var amplitudeJob: Job? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentEmotion = MutableStateFlow(NovaEmotion.WARM)
    val currentEmotion: StateFlow<NovaEmotion> = _currentEmotion.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _speakingAmplitude = MutableStateFlow(0f)
    val speakingAmplitude: StateFlow<Float> = _speakingAmplitude.asStateFlow()

    private var lastSpokenText: String = ""
    private var isMuted: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupTtsEngine()
            _isInitialized.value = true
        }
    }

    private fun setupTtsEngine() {
        val engine = tts ?: return

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                startSimulatedAmplitude()
            }

            override fun onDone(utteranceId: String?) {
                stopSimulatedAmplitude()
                _isSpeaking.value = false
                onSpeechFinishedCallback()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                stopSimulatedAmplitude()
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                stopSimulatedAmplitude()
                _isSpeaking.value = false
            }
        })

        // Enumerate voices and filter prioritizing high-quality female voices
        try {
            val voicesList = mutableListOf<VoiceOption>()
            engine.voices?.let { voices ->
                for (v in voices) {
                    val nameLower = v.name.lowercase(Locale.ROOT)
                    val isFemale = nameLower.contains("female") ||
                            nameLower.contains("#female") ||
                            nameLower.contains("sfg") ||
                            nameLower.contains("network") ||
                            v.name.contains("en-us-x-sfg") ||
                            v.name.contains("en-gb-x-rjs") ||
                            v.name.contains("en-us-x-tpd")

                    // Include voices that match common languages (English, Urdu, etc.)
                    val lang = v.locale.language
                    if (lang == "en" || lang == "ur") {
                        val qualityTag = if (v.isNetworkConnectionRequired) "HD" else "Standard"
                        val genderTag = if (isFemale) "Female" else "Voice"
                        voicesList.add(
                            VoiceOption(
                                id = v.name,
                                displayName = "NOVA ${v.locale.displayLanguage} ($genderTag · $qualityTag)",
                                locale = v.locale.toLanguageTag(),
                                isFemale = isFemale
                            )
                        )
                    }
                }
            }

            // Sort with female voices on top
            val sorted = voicesList.sortedWith(
                compareByDescending<VoiceOption> { it.isFemale }
                    .thenBy { it.displayName }
            )
            _availableVoices.value = sorted

            // Auto-select best female voice
            val bestVoice = sorted.firstOrNull { it.isFemale && it.locale.startsWith("en") }
            if (bestVoice != null) {
                engine.voices?.find { it.name == bestVoice.id }?.let {
                    engine.voice = it
                }
            } else {
                engine.language = Locale.US
            }
        } catch (_: Exception) {
            engine.language = Locale.US
        }
    }

    fun speak(
        text: String,
        emotion: NovaEmotion = NovaEmotion.WARM,
        settings: VoiceSettings
    ) {
        if (isMuted || settings.textOnlyMode || text.isBlank()) {
            return
        }

        val engine = tts ?: return
        lastSpokenText = text
        _currentEmotion.value = emotion

        // Check if text is in Urdu (Arabic script range)
        val hasUrduScript = text.any { it in '\u0600'..'\u06FF' }

        if (hasUrduScript || settings.language == "ur") {
            val urLocale = Locale("ur", "PK")
            val langResult = engine.isLanguageAvailable(urLocale)
            if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = urLocale
            }
        } else {
            // Apply selected voice if user selected one
            if (settings.voiceName.isNotBlank()) {
                val selectedVoice = engine.voices?.find { it.name == settings.voiceName }
                if (selectedVoice != null) {
                    engine.voice = selectedVoice
                }
            } else {
                // Ensure default female voice is set
                val femaleVoice = engine.voices?.find {
                    it.name.lowercase(Locale.ROOT).contains("female") && it.locale.language == "en"
                }
                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                } else {
                    engine.language = Locale.US
                }
            }
        }

        // Apply pitch & rate with emotional modulation
        val basePitch = settings.pitch
        val baseRate = settings.rate

        val effectivePitch = if (settings.emotionalDelivery) {
            (basePitch + emotion.pitchModifier).coerceIn(0.7f, 1.6f)
        } else {
            basePitch.coerceIn(0.7f, 1.6f)
        }

        val effectiveRate = if (settings.emotionalDelivery) {
            (baseRate + emotion.rateModifier).coerceIn(0.6f, 1.5f)
        } else {
            baseRate.coerceIn(0.6f, 1.5f)
        }

        engine.setPitch(effectivePitch)
        engine.setSpeechRate(effectiveRate)

        // Clean text of emotion tags or markdown symbols for natural vocalization
        val cleanSpeech = cleanTextForSpeech(text)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.volume.coerceIn(0f, 1f))
        }

        engine.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, "nova_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        stopSimulatedAmplitude()
        _isSpeaking.value = false
    }

    fun repeatLast(settings: VoiceSettings) {
        if (lastSpokenText.isNotBlank()) {
            speak(lastSpokenText, _currentEmotion.value, settings)
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            stopSpeaking()
        }
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun testVoice(settings: VoiceSettings, emotion: NovaEmotion = NovaEmotion.WARM) {
        val sampleText = when (settings.language) {
            "ur" -> "السلام علیکم! میں نووا ہوں۔ میں آپ کے پاس ہی ہوں۔ بتائیے، آج ہم کیا کریں؟"
            "roman_urdu" -> "Hello, Sir! Main NOVA hoon, hamesha aapke saath. Batayein, aaj hum kya karein?"
            else -> "Hello, Sir. I'm NOVA. I'm right here whenever you need me. So, tell me… what shall we do today?"
        }
        speak(sampleText, emotion, settings)
    }

    private fun cleanTextForSpeech(raw: String): String {
        return raw
            // Remove emotion tags e.g. [EMOTION:excited]
            .replace(Regex("\\[EMOTION:[a-zA-Z_]+\\]", RegexOption.IGNORE_CASE), "")
            // Remove markdown bold/italics
            .replace(Regex("[*#`_~]"), "")
            // Remove code blocks
            .replace(Regex("```[\\s\\S]*?```"), "Code sample provided in chat.")
            // Format pauses for cinematic rhythm
            .replace("...", ", ")
            .trim()
    }

    private fun startSimulatedAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = coroutineScope.launch {
            var step = 0
            while (isActive) {
                // Generate a natural audio pulse rhythm for the visualizer
                val base = 0.45f
                val pulse = (Math.sin(step * 0.45).toFloat() * 0.35f) +
                        (Math.cos(step * 0.25).toFloat() * 0.20f)
                _speakingAmplitude.value = (base + pulse).coerceIn(0.15f, 1.0f)
                step++
                delay(60)
            }
        }
    }

    private fun stopSimulatedAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _speakingAmplitude.value = 0f
    }

    fun shutdown() {
        stopSpeaking()
        tts?.shutdown()
        tts = null
    }
}
