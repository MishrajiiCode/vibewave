package com.music.vibewave.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Redesigned, ultra-sleek Gemini-style AI bar displayed under "Listen Now".
 * Uncluttered, visually stunning frosted pill that expands into the full
 * descriptive VibeWave AI Companion Studio & Chat when tapped.
 */
@Composable
fun AiBannerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPromptStudioClick: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "starScale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF131525),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFF6C5CE7).copy(alpha = 0.8f),
                    Color(0xFF00CEC9).copy(alpha = 0.8f),
                    Color(0xFFFF7675).copy(alpha = 0.6f),
                )
            ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF6C5CE7).copy(alpha = 0.22f),
                            Color(0xFF00CEC9).copy(alpha = 0.12f),
                            Color(0xFF10121F),
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Sparkling Gemini 4-Point Star
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .scale(starScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFF00CEC9),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "VibeWave AI",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ask VibeWave AI",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                            ),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00CEC9).copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = "GEMINI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.5.sp,
                                ),
                                color = Color(0xFF00CEC9),
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
                            )
                        }
                    }

                    Text(
                        text = "Smart song finder, playlist curation & app tips",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E92BC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF6C5CE7),
                modifier = Modifier.clickable(onClick = onClick),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}
