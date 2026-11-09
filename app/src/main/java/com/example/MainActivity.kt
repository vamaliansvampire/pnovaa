package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NovaViewModel
import com.example.ui.screens.NovaChatScreen
import com.example.ui.screens.NovaSettingsScreen
import com.example.ui.screens.NovaVoiceCoreScreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.NovaAiTheme
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

enum class NovaScreen(val label: String) {
    VOICE_CORE("Voice Core"),
    CHAT("Chat"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaAiTheme {
                NovaAppRoot()
            }
        }
    }
}

@Composable
fun NovaAppRoot() {
    val context = LocalContext.current
    val viewModel: NovaViewModel = viewModel()

    var currentScreen by remember { mutableStateOf(NovaScreen.VOICE_CORE) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = ObsidianDark,
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, ObsidianBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("nova_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentScreen == NovaScreen.VOICE_CORE,
                    onClick = { currentScreen = NovaScreen.VOICE_CORE },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Core",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "NOVA Core",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == NovaScreen.VOICE_CORE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianDark,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = GoldPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_voice_core")
                )

                NavigationBarItem(
                    selected = currentScreen == NovaScreen.CHAT,
                    onClick = { currentScreen = NovaScreen.CHAT },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Dialogue",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == NovaScreen.CHAT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianDark,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = GoldPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_chat")
                )

                NavigationBarItem(
                    selected = currentScreen == NovaScreen.SETTINGS,
                    onClick = { currentScreen = NovaScreen.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Voice & AI",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == NovaScreen.SETTINGS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianDark,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = GoldPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    NovaScreen.VOICE_CORE -> NovaVoiceCoreScreen(
                        viewModel = viewModel,
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onNavigateToChat = { currentScreen = NovaScreen.CHAT },
                        onNavigateToSettings = { currentScreen = NovaScreen.SETTINGS }
                    )
                    NovaScreen.CHAT -> NovaChatScreen(
                        viewModel = viewModel,
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onNavigateBack = { currentScreen = NovaScreen.VOICE_CORE }
                    )
                    NovaScreen.SETTINGS -> NovaSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NovaScreen.VOICE_CORE }
                    )
                }
            }
        }
    }

    // Permission rationale dialog if user previously dismissed
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Text(
                    text = "Microphone Access Required",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.mic_permission_rationale),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text(text = "Grant Permission", color = ObsidianDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text(text = "Maybe Later", color = GoldSecondary)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
