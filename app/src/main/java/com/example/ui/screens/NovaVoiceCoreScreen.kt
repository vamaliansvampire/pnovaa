package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NovaViewModel
import com.example.ui.VisualizerState
import com.example.ui.components.NovaVisualizerOrb
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianGoldBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun NovaVoiceCoreScreen(
    viewModel: NovaViewModel,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualizerState by viewModel.visualizerState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val speakingAmplitude by viewModel.speakingAmplitude.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val partialVoiceText by viewModel.partialVoiceText.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val messages by viewModel.messages.collectAsState()

    val lastNovaMessage = messages.lastOrNull { it.sender == "nova" }

    val promptSuggestions = listOf(
        "Who are you, NOVA?",
        "Tell me something uplifting",
        "Mujhe ek khubsurat baat batao",
        "How are you feeling today?",
        "Explain quantum entanglement",
        "Say a poetic quote in Urdu"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NOVA",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent,
                        modifier = Modifier
                            .background(GoldDark.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Cinematic Female Voice Assistant",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mute toggle
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFEF5350) else GoldSecondary
                    )
                }

                // Chat icon
                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("nav_to_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat History",
                        tint = GoldPrimary
                    )
                }

                // Settings icon
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("nav_to_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = GoldSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emotion & Voice Status Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianGoldBorder),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (visualizerState) {
                                VisualizerState.LISTENING -> Color(0xFF64B5F6)
                                VisualizerState.THINKING -> Color(0xFFCE93D8)
                                VisualizerState.SPEAKING -> GoldAccent
                                VisualizerState.IDLE -> GoldPrimary
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vocal State: ${currentEmotion.label}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                if (settings.continuousConversation) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Continuous",
                        fontSize = 11.sp,
                        color = GoldAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Central Futuristic NOVA Voice Visualizer Orb
        NovaVisualizerOrb(
            state = visualizerState,
            rmsLevel = rmsLevel,
            speakingAmplitude = speakingAmplitude,
            size = 230.dp,
            onOrbClick = {
                // Tapping orb toggles between consciousness core and avatar view
            }
        )

        Text(
            text = "Tap visualizer to toggle avatar portrait",
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Voice Status Indicator
        val statusLabel = when (visualizerState) {
            VisualizerState.LISTENING -> "Listening to your voice…"
            VisualizerState.THINKING -> "NOVA is processing…"
            VisualizerState.SPEAKING -> "NOVA is speaking…"
            VisualizerState.IDLE -> if (isMuted) "Voice is muted" else "Ready for conversation"
        }

        Text(
            text = statusLabel,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (visualizerState) {
                VisualizerState.LISTENING -> Color(0xFF90CAF9)
                VisualizerState.SPEAKING -> GoldAccent
                VisualizerState.THINKING -> Color(0xFFE1BEE7)
                VisualizerState.IDLE -> TextWhite
            },
            textAlign = TextAlign.Center
        )

        // Real-time spoken transcript if listening
        AnimatedVisibility(
            visible = visualizerState == VisualizerState.LISTENING && partialVoiceText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "“$partialVoiceText”",
                fontSize = 14.sp,
                color = GoldSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Error message banner
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = Color(0xFFFFCDD2),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dismiss",
                            fontSize = 11.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearError() }
                        )
                    }
                }
            }
        }

        // Last spoken NOVA response card (with repeat / stop controls)
        if (lastNovaMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest Spoken Response",
                            fontSize = 11.sp,
                            color = TextGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            if (isSpeaking) {
                                IconButton(
                                    onClick = { viewModel.stopSpeaking() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color(0xFFEF5350)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.repeatLastResponse() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Repeat",
                                    tint = GoldSecondary
                                )
                            }
                        }
                    }
                    Text(
                        text = lastNovaMessage.text,
                        fontSize = 13.sp,
                        color = TextWhite,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Suggested Conversation Sparks
        Text(
            text = "Conversation Sparks",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            promptSuggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianGoldBorder),
                    modifier = Modifier.clickable {
                        viewModel.sendMessage(prompt)
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        color = TextWhite,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Microphone Button & Audio Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Repeat last button
            IconButton(
                onClick = { viewModel.repeatLastResponse() },
                modifier = Modifier
                    .size(48.dp)
                    .background(ObsidianCard, CircleShape)
                    .border(1.dp, ObsidianBorder, CircleShape)
                    .testTag("repeat_response_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Repeat Response",
                    tint = GoldSecondary
                )
            }

            // Central Main Mic Button (glowing large circular action button)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
                            } else {
                                listOf(GoldPrimary, GoldDark)
                            }
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(listOf(GoldAccent, GoldPrimary, GoldDark, GoldAccent)),
                        shape = CircleShape
                    )
                    .clickable {
                        if (!hasAudioPermission) {
                            onRequestPermission()
                        } else {
                            viewModel.onMicClicked(true)
                        }
                    }
                    .testTag("main_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Speak to NOVA",
                    tint = ObsidianDark,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Stop Speaking button
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.testVoice()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(ObsidianCard, CircleShape)
                    .border(1.dp, ObsidianBorder, CircleShape)
                    .testTag("stop_or_test_button")
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isSpeaking) "Stop Speaking" else "Test Voice",
                    tint = if (isSpeaking) Color(0xFFEF5350) else GoldSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Text Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                placeholder = {
                    Text(
                        text = "Ask NOVA or tap mic…",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = GoldPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_text_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (inputText.isNotBlank()) GoldPrimary else ObsidianCard,
                        CircleShape
                    )
                    .border(1.dp, ObsidianBorder, CircleShape)
                    .testTag("home_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) ObsidianDark else TextMuted
                )
            }
        }
    }
}
