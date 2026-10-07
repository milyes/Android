package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformVisualizer(
    amplitudes: List<Int>,
    progressRatio: Float = 0f,
    isLive: Boolean = false,
    onSeekRatio: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    activeBarColor: Color = MaterialTheme.colorScheme.secondary,
    inactiveBarColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val displayAmps = if (amplitudes.isEmpty()) {
        listOf(20, 35, 50, 75, 90, 60, 40, 80, 95, 70, 40, 65, 85, 50, 30, 60, 75, 40, 20, 50)
    } else amplitudes

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = onSeekRatio != null) {
                // Seek logic if needed
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = displayAmps.size
        val barGap = 4f
        val totalGaps = (barCount - 1) * barGap
        val barWidth = maxOf(4f, (width - totalGaps) / barCount)

        displayAmps.forEachIndexed { index, amp ->
            val scale = if (isLive && index >= barCount - 3) pulseAnim else 1.0f
            val rawNormalized = (amp.coerceIn(10, 100) / 100f)
            val barHeight = (height * rawNormalized * scale).coerceAtMost(height)

            val x = index * (barWidth + barGap)
            val y = (height - barHeight) / 2f

            val isPlayed = (index.toFloat() / barCount) <= progressRatio
            val currentColor = when {
                isLive -> barColor
                isPlayed -> activeBarColor
                else -> inactiveBarColor
            }

            drawRoundRect(
                color = currentColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
