package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

/**
 * Animated concentric ripples reflecting microphone activity when recording is active.
 */
@Composable
fun MicrophonePulseRipples(
    isRecording: Boolean,
    isPaused: Boolean,
    latestAmplitude: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = CrimsonError
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_sonar_ripples")

    val normalizedAmp = (latestAmplitude.coerceIn(10, 100) / 100f)

    // Animated ripple 1
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    // Animated ripple 2 (offset)
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    // Animated ripple 3 (offset)
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    Canvas(
        modifier = modifier
            .size(200.dp)
            .testTag("microphone_sonar_ripples")
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f

        if (isRecording && !isPaused) {
            val waves = listOf(wave1, wave2, wave3)
            waves.forEach { waveProgress ->
                val dynamicRadius = 45.dp.toPx() + (maxRadius - 45.dp.toPx()) * waveProgress * (0.8f + normalizedAmp * 0.4f)
                val alpha = ((1f - waveProgress) * 0.45f).coerceIn(0f, 0.6f)

                drawCircle(
                    color = activeColor.copy(alpha = alpha),
                    radius = dynamicRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Subtle glowing fill
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = alpha * 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius
                    ),
                    radius = dynamicRadius,
                    center = center
                )
            }
        }
    }
}

/**
 * Visual multi-bar sound spectrum visualizer showing live microphone amplitude activity.
 */
@Composable
fun LiveMicrophoneSpectrumVisualizer(
    amplitudes: List<Int>,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    peakColor: Color = CrimsonError
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Fallback baseline amplitude list when idle
    val displayAmplitudes = remember(amplitudes, isRecording, isPaused) {
        if (!isRecording || amplitudes.isEmpty()) {
            listOf(18, 24, 30, 22, 35, 45, 38, 28, 20, 32, 40, 50, 35, 25, 18, 22, 30, 20)
        } else {
            amplitudes.takeLast(24)
        }
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("live_spectrum_visualizer")
    ) {
        val width = size.width
        val height = size.height
        val barCount = displayAmplitudes.size
        val barSpacing = 4.dp.toPx()
        val totalSpacing = (barCount - 1) * barSpacing
        val barWidth = maxOf(4.dp.toPx(), (width - totalSpacing) / barCount)

        displayAmplitudes.forEachIndexed { index, amp ->
            val sinMod = if (isRecording && !isPaused) {
                0.85f + 0.3f * sin(shimmerPhase + index * 0.4f)
            } else 1.0f

            val normalizedAmp = (amp.coerceIn(10, 100) / 100f)
            val barHeight = (height * normalizedAmp * sinMod).coerceIn(6.dp.toPx(), height)

            val x = index * (barWidth + barSpacing)
            val y = (height - barHeight) / 2f

            val gradientBrush = Brush.verticalGradient(
                colors = if (isRecording && !isPaused) {
                    listOf(
                        peakColor,
                        barColor,
                        barColor.copy(alpha = 0.7f)
                    )
                } else {
                    listOf(
                        outlineVariant,
                        surfaceVariant
                    )
                },
                startY = y,
                endY = y + barHeight
            )

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Prominent Start/Stop Toggle Button with fluid state animations and microphone activity cues.
 */
@Composable
fun RecordingToggleButton(
    isRecording: Boolean,
    isPaused: Boolean,
    latestAmplitude: Int,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = when {
            isRecording && isPaused -> AmberPending
            isRecording -> CrimsonError
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "btn_color"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isRecording) 22.dp else 40.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "corner_shape"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording && !isPaused) {
            1.0f + (latestAmplitude.coerceIn(0, 100) / 100f) * 0.08f
        } else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "btn_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Pulsing Sonar Ripples behind button
        MicrophonePulseRipples(
            isRecording = isRecording,
            isPaused = isPaused,
            latestAmplitude = latestAmplitude,
            activeColor = CrimsonError
        )

        // Main Toggle Button
        Surface(
            onClick = onToggleRecording,
            shape = RoundedCornerShape(cornerRadius),
            color = buttonColor,
            shadowElevation = if (isRecording) 12.dp else 6.dp,
            modifier = Modifier
                .size(76.dp)
                .scale(buttonScale)
                .testTag("record_toggle_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isRecording) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Arrêter l'enregistrement",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Démarrer l'enregistrement",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

/**
 * Real-time Decibel / dBFS Audio Level Meter
 */
@Composable
fun DecibelMeterIndicator(
    amplitude: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val safeAmp = if (isRecording) amplitude.coerceIn(0, 100) else 0
    val estimatedDb = if (isRecording) (-48 + (safeAmp * 0.48)).toInt() else -48

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("decibel_meter_indicator")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isRecording -> MaterialTheme.colorScheme.outline
                            safeAmp > 75 -> CrimsonError
                            safeAmp > 40 -> AmberPending
                            else -> EmeraldSynced
                        }
                    )
            )

            Text(
                text = if (isRecording) "${estimatedDb} dBFS" else "-- dBFS",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = when {
                    !isRecording -> TextSecondary
                    safeAmp > 75 -> CrimsonError
                    safeAmp > 40 -> AmberPending
                    else -> EmeraldSynced
                },
                fontSize = 11.sp
            )

            // Mini Level Meter
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..8) {
                    val threshold = i * 12
                    val isActive = safeAmp >= threshold
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((6 + i * 1.5).dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (isActive) {
                                    if (i > 6) CrimsonError else if (i > 4) AmberPending else EmeraldSynced
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            )
                    )
                }
            }
        }
    }
}
