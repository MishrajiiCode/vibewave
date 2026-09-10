package com.music.vibewave.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Beat-Reactive Neon Audio Visualizer that simulates real-time frequency spectrum analysis
 * with vivid cyberpunk gradients, floating peaks, and dynamic bar bounce.
 */
@Composable
fun NeonAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    height: Dp = 42.dp,
    activeColorStart: Color = Color(0xFF8B5CF6), // Purple
    activeColorMid: Color = Color(0xFF06B6D4),   // Cyan
    activeColorEnd: Color = Color(0xFFEC4899),   // Pink
    idleColor: Color = Color(0xFF334155).copy(alpha = 0.35f),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonVisualizer")

    // Multiple animation phases with varied harmonic periods to emulate live FFT spectrum
    val phaseFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseFast",
    )

    val phaseMid by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseMid",
    )

    val phaseSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseSlow",
    )

    val randomOffsets = remember(barCount) {
        List(barCount) { i ->
            val factor1 = (i * 1.37f)
            val factor2 = (i * 2.81f)
            Pair(factor1, factor2)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val spacing = 3.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = ((totalWidth - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())
            val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(activeColorEnd, activeColorMid, activeColorStart),
                startY = 0f,
                endY = totalHeight,
            )

            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing)
                val (f1, f2) = randomOffsets[i]

                val normalizedHeight = if (isPlaying) {
                    // Combine multiple sine harmonics with index variation to create realistic frequency peaks
                    val wave1 = (sin(phaseFast + f1) + 1f) / 2f
                    val wave2 = (sin(phaseMid + f2) + 1f) / 2f
                    val wave3 = (sin(phaseSlow + (i * 0.45f)) + 1f) / 2f

                    // Bell-curve weighting: center/low frequencies have more energy
                    val centerWeight = 1.0f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 1.6f)).coerceIn(0f, 0.45f)
                    val raw = (wave1 * 0.45f + wave2 * 0.35f + wave3 * 0.20f) * centerWeight
                    raw.coerceIn(0.12f, 1.0f)
                } else {
                    0.08f // resting floor when paused
                }

                val currentBarHeight = (totalHeight * normalizedHeight).coerceAtLeast(4.dp.toPx())
                val topY = totalHeight - currentBarHeight

                drawRoundRect(
                    brush = if (isPlaying) gradientBrush else Brush.linearGradient(listOf(idleColor, idleColor)),
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}
