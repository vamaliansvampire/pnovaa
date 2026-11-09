package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.ui.NovaViewModel
import com.example.ui.VisualizerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaChatScreen(
    viewModel: NovaViewModel,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val visualizerState by viewModel.visualizerState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    val listState = rememberLazyListState()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Chat Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.nova_avatar),
                        contentDescription = "NOVA Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GoldPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NOVA Assistant",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = if (isListening) "Listening…" else if (visualizerState == VisualizerState.SPEAKING) "Speaking…" else "Action System Online",
                            fontSize = 11.sp,
                            color = if (isListening) Color(0xFF64B5F6) else GoldSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GoldPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.startNewSession() },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        tint = GoldPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ObsidianSurface
            )
        )

        // Pending Action Confirmation Banner inside Chat
        AnimatedVisibility(visible = pendingConfirmation != null) {
            pendingConfirmation?.let { pending ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A14)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pending.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pending.description,
                            fontSize = 12.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelPendingAction() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.confirmPendingAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm", color = ObsidianDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Chat messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Welcome / intro banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianGoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "NOVA AI Action & Voice System",
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NOVA executes real Android voice commands (Flashlight, Battery, Time, YouTube, WhatsApp, Chrome, Settings, Volume) alongside conversational intelligence.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onSpeakAgain = {
                        viewModel.sendMessage(message.text)
                    },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("NOVA Message", message.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Thinking animation item
            if (visualizerState == VisualizerState.THINKING) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOVA is processing your request…",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Quick Command Suggestions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Toggle flashlight",
                "Battery status",
                "What time is it?",
                "Open YouTube",
                "Open WhatsApp",
                "Open Settings",
                "Volume up"
            ).forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ObsidianBorder),
                    modifier = Modifier.clickable { viewModel.sendMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = GoldSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Composer
        Surface(
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    placeholder = {
                        Text(
                            text = "Type command or tap mic…",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
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
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Microphone button
                IconButton(
                    onClick = {
                        if (!hasAudioPermission) {
                            onRequestPermission()
                        } else {
                            viewModel.onMicClicked(true)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isListening) Color(0xFF1E88E5) else ObsidianCard,
                            CircleShape
                        )
                        .border(1.dp, if (isListening) Color(0xFF64B5F6) else ObsidianBorder, CircleShape)
                        .testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isListening) TextWhite else GoldPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (inputText.isNotBlank()) GoldPrimary else ObsidianCard,
                            CircleShape
                        )
                        .border(1.dp, ObsidianBorder, CircleShape)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) ObsidianDark else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeakAgain: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.92f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Image(
                    painter = painterResource(id = R.drawable.nova_avatar),
                    contentDescription = "NOVA Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, GoldPrimary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) Color(0xFF231E15) else ObsidianCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) GoldDark.copy(alpha = 0.5f) else ObsidianGoldBorder
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isUser && message.emotion.isNotBlank() && message.emotion != "neutral") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldDark.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Tone: ${message.emotion.replaceFirstChar { it.uppercase() }}",
                                fontSize = 10.sp,
                                color = TextGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = TextWhite,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        if (!isUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Aloud",
                                tint = GoldSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onSpeakAgain() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onCopy() }
                            )
                        }
                    }
                }
            }
        }
    }
}
