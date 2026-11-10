package com.music.vibewave.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.vibewave.data.gamification.VibeRewardsManager

/**
 * Celebratory popup dialog shown when a user levels up to a new tier or
 * unlocks an achievement / streak milestone in VibeWave.
 */
@Composable
fun EngagementCelebrationDialog(
    event: VibeRewardsManager.CelebrationEvent,
    onDismiss: () -> Unit,
    onOpenHub: (() -> Unit)? = null,
) {
    val scaleAnim = remember { Animatable(0.6f) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlow",
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF131525),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFF7675),
                            Color(0xFF6C5CE7),
                            Color(0xFF00CEC9),
                        )
                    ),
                ),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Close affordance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }

                    // Pulsing Glowing Icon Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseGlow)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.35f),
                                        Color(0xFF6C5CE7).copy(alpha = 0.20f),
                                        Color.Transparent,
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFF7675))
                                ),
                                shape = CircleShape,
                            ),
                    ) {
                        Text(
                            text = event.iconEmoji,
                            fontSize = 44.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tagline
                    Text(
                        text = when (event.type) {
                            "TIER" -> "🌟 NEW TIER UNLOCKED!"
                            "STREAK" -> "🔥 STREAK MILESTONE!"
                            else -> "🎉 TROPHY UNLOCKED!"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                        ),
                        color = Color(0xFFFFD700),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Title
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0B5D6),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bonus XP Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFB300).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+${event.xpBonus} Vibe XP Added",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD700),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Community Feed flex notification
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "📢 Broadcasted to Engagement Hub Live Feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00CEC9),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (onOpenHub != null) {
                            Button(
                                onClick = onOpenHub,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF23273E),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    text = "View Hub 🌐",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6C5CE7),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "Keep Vibing 🔥",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

