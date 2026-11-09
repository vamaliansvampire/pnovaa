package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.NovaActionExecutor
import com.example.action.NovaActionParser
import com.example.action.NovaActionResult
import com.example.action.NovaActionType
import com.example.action.ParsedCommand
import com.example.action.PendingConfirmation
import com.example.ai.NovaAiService
import com.example.data.db.NovaDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.pref.NovaPreferences
import com.example.data.pref.VoiceSettings
import com.example.voice.NovaEmotion
import com.example.voice.NovaSpeechRecognizer
import com.example.voice.NovaVoiceEngine
import com.example.voice.VoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class VisualizerState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NovaDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val prefs = NovaPreferences(application)
    private val aiService = NovaAiService()

    private val actionParser = NovaActionParser()
    private val actionExecutor = NovaActionExecutor(application)

    private val voiceEngine: NovaVoiceEngine = NovaVoiceEngine(application) {
        onSpeechFinished()
    }

    private val speechRecognizer: NovaSpeechRecognizer = NovaSpeechRecognizer(
        context = application,
        onResult = { recognizedText ->
            onSpeechRecognized(recognizedText)
        },
        onError = { error ->
            _errorMessage.value = error
            _visualizerState.value = VisualizerState.IDLE
        }
    )

    // Current session
    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val allSessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings: StateFlow<VoiceSettings> = prefs.settingsFlow

    private val _visualizerState = MutableStateFlow(VisualizerState.IDLE)
    val visualizerState: StateFlow<VisualizerState> = _visualizerState.asStateFlow()

    val isListening: StateFlow<Boolean> = speechRecognizer.isListening
    val rmsLevel: StateFlow<Float> = speechRecognizer.rmsLevel
    val partialVoiceText: StateFlow<String> = speechRecognizer.partialText

    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val speakingAmplitude: StateFlow<Float> = voiceEngine.speakingAmplitude
    val currentEmotion: StateFlow<NovaEmotion> = voiceEngine.currentEmotion
    val availableVoices: StateFlow<List<VoiceOption>> = voiceEngine.availableVoices

    val isFlashlightOn: StateFlow<Boolean> = actionExecutor.isFlashlightOn

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _lastExecutedAction = MutableStateFlow<String?>(null)
    val lastExecutedAction: StateFlow<String?> = _lastExecutedAction.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    init {
        // Observe messages for current session
        observeSessionMessages(_currentSessionId.value)

        // Sync speech recognizer states to visualizer
        viewModelScope.launch {
            speechRecognizer.isListening.collect { listening ->
                if (listening) {
                    _visualizerState.value = VisualizerState.LISTENING
                } else if (_visualizerState.value == VisualizerState.LISTENING) {
                    _visualizerState.value = VisualizerState.IDLE
                }
            }
        }

        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                if (speaking) {
                    _visualizerState.value = VisualizerState.SPEAKING
                } else if (_visualizerState.value == VisualizerState.SPEAKING) {
                    _visualizerState.value = VisualizerState.IDLE
                }
            }
        }
    }

    private fun observeSessionMessages(sessionId: String) {
        viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { list ->
                if (list.isEmpty()) {
                    val now = System.currentTimeMillis()
                    val greeting = ChatMessage(
                        sessionId = sessionId,
                        sender = "nova",
                        text = "Hello, Sir. I'm NOVA. I'm right here whenever you need me. So, tell me… what shall we do today?",
                        emotion = "warm",
                        timestamp = now
                    )
                    chatDao.insertSession(
                        ChatSession(
                            sessionId = sessionId,
                            title = "Dialogue with NOVA",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    chatDao.insertMessage(greeting)
                } else {
                    _messages.value = list
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleMute() {
        val muted = voiceEngine.toggleMute()
        _isMuted.value = muted
    }

    fun onMicClicked(hasRecordPermission: Boolean) {
        if (!hasRecordPermission) {
            _errorMessage.value = "Microphone permission is required for voice conversation."
            return
        }

        if (speechRecognizer.isListening.value) {
            speechRecognizer.stopListening()
        } else {
            if (voiceEngine.isSpeaking.value) {
                voiceEngine.stopSpeaking()
            }
            _errorMessage.value = null
            speechRecognizer.startListening(settings.value.language)
        }
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
        _visualizerState.value = VisualizerState.IDLE
    }

    fun repeatLastResponse() {
        voiceEngine.repeatLast(settings.value)
    }

    fun testVoice() {
        voiceEngine.testVoice(settings.value)
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        executeCommandInternal(
            command = ParsedCommand(NovaActionType.CONFIRM_ACTION, rawQuery = "Yes, proceed"),
            activePending = pending,
            userPrompt = "Confirmed"
        )
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        executeCommandInternal(
            command = ParsedCommand(NovaActionType.CANCEL_ACTION, rawQuery = "No, cancel"),
            activePending = pending,
            userPrompt = "Canceled"
        )
    }

    fun toggleFlashlight() {
        sendMessage("toggle flashlight")
    }

    fun checkBattery() {
        sendMessage("battery status")
    }

    fun checkTime() {
        sendMessage("what time is it")
    }

    fun openYouTube() {
        sendMessage("open youtube")
    }

    fun openWhatsApp() {
        sendMessage("open whatsapp")
    }

    fun openChrome() {
        sendMessage("open chrome")
    }

    fun openSettings() {
        sendMessage("open settings")
    }

    fun openWifi() {
        sendMessage("open wifi settings")
    }

    fun openBluetooth() {
        sendMessage("open bluetooth settings")
    }

    fun sendMessage(text: String = _inputText.value) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _inputText.value = ""
        _errorMessage.value = null
        stopSpeaking()

        val activePending = _pendingConfirmation.value

        // Check for confirmation/cancellation of pending action
        if (activePending != null) {
            val confirmParsed = actionParser.parse(trimmed, hasPendingConfirmation = true)
            if (confirmParsed != null && (confirmParsed.actionType == NovaActionType.CONFIRM_ACTION || confirmParsed.actionType == NovaActionType.CANCEL_ACTION)) {
                _pendingConfirmation.value = null
                executeCommandInternal(confirmParsed, activePending = activePending, userPrompt = trimmed)
                return
            }
        }

        // Check if command is a recognized native device action
        val parsedCommand = actionParser.parse(trimmed, hasPendingConfirmation = false)
        if (parsedCommand != null) {
            executeCommandInternal(parsedCommand, activePending = null, userPrompt = trimmed)
            return
        }

        // Otherwise execute standard conversational AI
        executeAiConversation(trimmed)
    }

    private fun executeCommandInternal(
        command: ParsedCommand,
        activePending: PendingConfirmation?,
        userPrompt: String
    ) {
        val sessionId = _currentSessionId.value
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val sessionTitle = if (userPrompt.length > 30) userPrompt.take(30) + "…" else userPrompt
            chatDao.insertSession(
                ChatSession(
                    sessionId = sessionId,
                    title = sessionTitle,
                    createdAt = now,
                    updatedAt = now
                )
            )

            // Insert User Message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                sender = "user",
                text = userPrompt,
                timestamp = now
            )
            chatDao.insertMessage(userMsg)

            _visualizerState.value = VisualizerState.THINKING

            // Execute on Main thread to safely interact with Android Intents and Framework Services
            viewModelScope.launch(Dispatchers.Main) {
                val result: NovaActionResult = actionExecutor.execute(command, activePending)

                if (result.requiresConfirmation) {
                    _pendingConfirmation.value = result.pendingConfirmation
                } else {
                    _pendingConfirmation.value = null
                }

                if (!result.isSuccess && result.errorMessage != null) {
                    _errorMessage.value = result.errorMessage
                } else {
                    _errorMessage.value = null
                }

                _lastExecutedAction.value = result.displayText

                // Insert NOVA Response in database
                viewModelScope.launch(Dispatchers.IO) {
                    val novaMsg = ChatMessage(
                        sessionId = sessionId,
                        sender = "nova",
                        text = result.displayText,
                        emotion = result.emotion,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(novaMsg)
                    chatDao.updateSessionTitle(sessionId, sessionTitle, System.currentTimeMillis())
                }

                // Speak response in NOVA's voice
                _visualizerState.value = VisualizerState.SPEAKING
                val emotion = try {
                    NovaEmotion.valueOf(result.emotion.uppercase(Locale.ROOT))
                } catch (_: Exception) {
                    NovaEmotion.WARM
                }
                voiceEngine.speak(
                    text = result.spokenText,
                    emotion = emotion,
                    settings = settings.value
                )
            }
        }
    }

    private fun executeAiConversation(prompt: String) {
        val sessionId = _currentSessionId.value

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val sessionTitle = if (prompt.length > 30) prompt.take(30) + "…" else prompt
            chatDao.insertSession(
                ChatSession(
                    sessionId = sessionId,
                    title = sessionTitle,
                    createdAt = now,
                    updatedAt = now
                )
            )

            // Insert User Message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                sender = "user",
                text = prompt,
                timestamp = now
            )
            chatDao.insertMessage(userMsg)

            // Switch to Thinking state
            _visualizerState.value = VisualizerState.THINKING

            // Call AI
            val response = aiService.generateResponse(
                prompt = prompt,
                history = _messages.value,
                customApiKey = settings.value.customApiKey,
                preferredModel = settings.value.modelId
            )

            // Insert NOVA Response
            val novaMsg = ChatMessage(
                sessionId = sessionId,
                sender = "nova",
                text = response.text,
                emotion = response.emotion.name.lowercase(Locale.ROOT),
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(novaMsg)
            chatDao.updateSessionTitle(sessionId, sessionTitle, System.currentTimeMillis())

            if (!response.isSuccess && response.errorMessage != null) {
                _errorMessage.value = response.errorMessage
            }

            // Speak response on Main thread
            viewModelScope.launch(Dispatchers.Main) {
                _visualizerState.value = VisualizerState.SPEAKING
                voiceEngine.speak(
                    text = response.text,
                    emotion = response.emotion,
                    settings = settings.value
                )
            }
        }
    }

    private fun onSpeechRecognized(text: String) {
        sendMessage(text)
    }

    private fun onSpeechFinished() {
        if (settings.value.continuousConversation && !_isMuted.value) {
            viewModelScope.launch(Dispatchers.Main) {
                speechRecognizer.startListening(settings.value.language)
            }
        } else {
            _visualizerState.value = VisualizerState.IDLE
        }
    }

    // Settings actions
    fun updatePitch(pitch: Float) = prefs.updatePitch(pitch)
    fun updateRate(rate: Float) = prefs.updateRate(rate)
    fun updateVoiceName(name: String) = prefs.updateVoiceName(name)
    fun updateLanguage(lang: String) = prefs.updateLanguage(lang)
    fun setContinuousConversation(enabled: Boolean) = prefs.setContinuousConversation(enabled)
    fun setTextOnlyMode(enabled: Boolean) = prefs.setTextOnlyMode(enabled)
    fun setEmotionalDelivery(enabled: Boolean) = prefs.setEmotionalDelivery(enabled)
    fun setCustomApiKey(key: String) = prefs.setCustomApiKey(key)
    fun updateModelId(modelId: String) = prefs.updateModelId(modelId)

    // Session management
    fun startNewSession() {
        stopSpeaking()
        speechRecognizer.stopListening()
        val newId = UUID.randomUUID().toString()
        _currentSessionId.value = newId
        observeSessionMessages(newId)
    }

    fun switchSession(sessionId: String) {
        stopSpeaking()
        speechRecognizer.stopListening()
        _currentSessionId.value = sessionId
        observeSessionMessages(sessionId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewSession()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearAllMessages()
            chatDao.clearAllSessions()
            startNewSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
        speechRecognizer.destroy()
    }
}
