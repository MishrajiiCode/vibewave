package com.music.bitchord.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.BuildConfig
import com.music.bitchord.R

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowScale",
    )

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0C14),
                        Color(0xFF10121F),
                        Color(0xFF0A0B12),
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
                .size(160.dp)
                .scale(pulseGlow)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C5CE7).copy(alpha = 0.28f),
                            Color(0xFF00CEC9).copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        ) {
            // New App Icon
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f),
                            ),
                        ),
                        shape = RoundedCornerShape(26.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = "VibeWave Logo",
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // App Name
            Text(
                text = "VibeWave",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
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
                    letterSpacing = 2.5.sp,
                ),
                color = Color(0xFF81ECEC).copy(alpha = 0.9f),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Pulsing Audio Visualizer Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(26.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar1Height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6C5CE7)),
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar2Height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF00CEC9)),
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar3Height.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFD79A8)),
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
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Crafted with ❤️ by Raj Mishra",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
