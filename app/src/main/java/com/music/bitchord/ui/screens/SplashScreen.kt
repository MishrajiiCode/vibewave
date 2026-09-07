package com.music.bitchord.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.BuildConfig
import com.music.bitchord.R

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.65f) }
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700),
        )
    }

    LaunchedEffect(Unit) {
        // Smoothly fills across the 5.5-second splash interval
        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 5200, easing = LinearEasing),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowScale",
    )

    // 7 distinct animated visualizer equalizer bars
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1",
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2",
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3",
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 24f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar4",
    )
    val bar5Height by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar5",
    )
    val bar6Height by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(470, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar6",
    )
    val bar7Height by infiniteTransition.animateFloat(
        initialValue = 7f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(440, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar7",
    )

    // Progressive status message based on initialization timeline
    val statusText = when {
        progress.value < 0.25f -> "Initializing Bit-Perfect Audio Pipeline…"
        progress.value < 0.52f -> "Calibrating Neural Music Vector Engine…"
        progress.value < 0.80f -> "Synthesizing Personalized Frequency Graph…"
        else -> "Ready to vibe. Welcome to VibeWave."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07080F),
                        Color(0xFF0F1122),
                        Color(0xFF070810),
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        // Glowing halo behind icon
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(pulseGlow)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C5CE7).copy(alpha = 0.32f),
                            Color(0xFF00CEC9).copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
        ) {
            // App Icon with glowing neon outline
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF00CEC9).copy(alpha = 0.6f),
                                Color(0xFF6C5CE7).copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.15f),
                            ),
                        ),
                        shape = RoundedCornerShape(28.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = "VibeWave Logo",
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "VibeWave",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "YOUR WORLD OF MUSIC",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.8.sp,
                ),
                color = Color(0xFF81ECEC).copy(alpha = 0.95f),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 7-Bar Pulsing Harmonic Equalizer
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(30.dp),
            ) {
                val bars = listOf(
                    bar1Height to Color(0xFF6C5CE7),
                    bar2Height to Color(0xFF81ECEC),
                    bar3Height to Color(0xFF00CEC9),
                    bar4Height to Color(0xFFFD79A8),
                    bar5Height to Color(0xFFA29BFE),
                    bar6Height to Color(0xFF00CEC9),
                    bar7Height to Color(0xFF6C5CE7),
                )
                bars.forEach { (height, color) ->
                    Box(
                        modifier = Modifier
                            .width(4.5.dp)
                            .height(height.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(color),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dynamic Initialization Status Readout
            AnimatedContent(
                targetState = statusText,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                label = "statusAnimation",
            ) { target ->
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Glowing Smooth Loading Progress Bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFF00CEC9),
                                    Color(0xFFFD79A8),
                                ),
                            ),
                        ),
                )
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME} • Bit-Perfect Edition",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.45f),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Engineered with ❤️ by Raj Mishra",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color(0xFF81ECEC).copy(alpha = 0.65f),
            )
        }
    }
}
