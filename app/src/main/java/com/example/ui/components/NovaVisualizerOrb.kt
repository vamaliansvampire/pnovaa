package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.VisualizerState
import com.example.ui.theme.GlowGold
import com.example.ui.theme.GlowListening
import com.example.ui.theme.GlowSpeaking
import com.example.ui.theme.GlowThinking
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NovaVisualizerOrb(
    state: VisualizerState,
    rmsLevel: Float,
    speakingAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    onOrbClick: () -> Unit = {}
) {
    var showAvatar by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    // Slow atmospheric rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation for neural thinking
    val thinkingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_rotation"
    )

    // Gentle breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Determine primary glow & scale based on active state
    val (primaryGlow, targetScale) = when (state) {
        VisualizerState.IDLE -> Pair(GlowGold, breathingPulse)
        VisualizerState.LISTENING -> Pair(GlowListening, 1.0f + (rmsLevel * 0.35f))
        VisualizerState.THINKING -> Pair(GlowThinking, breathingPulse * 1.02f)
        VisualizerState.SPEAKING -> Pair(GlowSpeaking, 1.0f + (speakingAmplitude * 0.28f))
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("nova_visualizer_orb")
            .clickable {
                showAvatar = !showAvatar
                onOrbClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Background dynamic Canvas for energy rings & particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.85f

            // Outer atmospheric aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow, primaryGlow.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.25f,
                center = center
            )

            // Dynamic acoustic ripple waves
            val ringCount = if (state == VisualizerState.SPEAKING || state == VisualizerState.LISTENING) 4 else 2
            val waveEnergy = if (state == VisualizerState.SPEAKING) speakingAmplitude else if (state == VisualizerState.LISTENING) rmsLevel else 0.15f

            for (i in 1..ringCount) {
                val ringRadius = (baseRadius * (0.6f + (i * 0.12f))) * (1f + (waveEnergy * 0.15f * i))
                val alpha = (0.5f / i) * (0.4f + waveEnergy * 0.6f)
                val strokeColor = when (state) {
                    VisualizerState.LISTENING -> Color(0xFF64B5F6).copy(alpha = alpha.coerceIn(0.1f, 0.9f))
                    VisualizerState.THINKING -> Color(0xFFCE93D8).copy(alpha = alpha.coerceIn(0.1f, 0.9f))
                    VisualizerState.SPEAKING -> GoldAccent.copy(alpha = alpha.coerceIn(0.15f, 0.95f))
                    VisualizerState.IDLE -> GoldPrimary.copy(alpha = (alpha * 0.7f).coerceIn(0.1f, 0.7f))
                }
                drawCircle(
                    color = strokeColor,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = if (i == 1) 3f else 1.8f)
                )
            }

            // Orbital particle nodes
            val particleCount = 12
            val rotAngle = if (state == VisualizerState.THINKING) thinkingRotation else rotation
            for (p in 0 until particleCount) {
                val angle = Math.toRadians((p * (360.0 / particleCount) + rotAngle))
                val pRadius = baseRadius * 0.82f + (sin(angle * 2).toFloat() * 10f * waveEnergy)
                val px = center.x + (cos(angle).toFloat() * pRadius)
                val py = center.y + (sin(angle).toFloat() * pRadius)
                drawCircle(
                    color = GoldSecondary.copy(alpha = 0.65f),
                    radius = 3.5f,
                    center = Offset(px, py)
                )
            }
        }

        // Inner Core: Swappable between Avatar Portrait and Futuristic Geometric Core
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .scale(targetScale)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            GoldPrimary,
                            GoldSecondary,
                            GoldDark,
                            GoldAccent,
                            GoldPrimary
                        )
                    ),
                    shape = CircleShape
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ObsidianCard,
                            ObsidianDark
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showAvatar) {
                // Female AI Avatar portrait
                Image(
                    painter = painterResource(id = R.drawable.nova_avatar),
                    contentDescription = "NOVA Female AI Consciousness Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Futuristic Geometric Vector Core
                Image(
                    painter = painterResource(id = R.drawable.nova_icon),
                    contentDescription = "NOVA Quantum Core",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .rotate(if (state == VisualizerState.THINKING) thinkingRotation else 0f)
                )
            }
        }
    }
}
