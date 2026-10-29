package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianDark,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldSecondary,
    secondary = GoldSecondary,
    onSecondary = ObsidianDark,
    secondaryContainer = GoldMuted,
    onSecondaryContainer = GoldAccent,
    tertiary = GoldAccent,
    onTertiary = ObsidianDark,
    background = ObsidianDark,
    onBackground = TextWhite,
    surface = ObsidianSurface,
    onSurface = TextWhite,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianGoldBorder
)

@Composable
fun NovaAiTheme(
    content: @Composable () -> Unit
) {
    // NOVA uses a dedicated luxury obsidian-gold theme for its futuristic cinematic feel
    MaterialTheme(
        colorScheme = NovaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
