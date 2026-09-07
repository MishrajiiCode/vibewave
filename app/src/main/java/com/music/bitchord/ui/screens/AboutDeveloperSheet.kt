package com.music.bitchord.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.BuildConfig
import com.music.bitchord.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutDeveloperSheet(
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF080913),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C0E1B),
                            Color(0xFF14122C),
                            Color(0xFF090A13),
                        ),
                    ),
                )
        ) {
            // Ambient Radial Flare at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6C5CE7).copy(alpha = 0.25f),
                                Color(0xFF00CEC9).copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header Close Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }

                // Avatar with Glowing Multi-Color Ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFF00CEC9),
                                    Color(0xFFFD79A8),
                                ),
                            ),
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF15162A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color(0xFF81ECEC),
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Creator Name + Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Raj Mishra",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Verified Creator",
                        tint = Color(0xFF00CEC9),
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Founder • Lead Software Architect",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                    color = Color(0xFF81ECEC),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Crafting high-fidelity, privacy-first audio experiences",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Mission & Vision Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141528).copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7).copy(alpha = 0.5f), Color(0xFF00CEC9).copy(alpha = 0.3f)),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF00CEC9),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The VibeWave Philosophy",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "VibeWave was envisioned and engineered by Raj Mishra to liberate modern music streaming from algorithmic manipulation, invasive ads, and subscription paywalls. Built from the ground up on reactive Android architecture, it combines studio-grade bit-perfect sound reproduction with an autonomous, zero-API neural recommendation engine that respects your privacy.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Technical Architecture Showcase Cards
                Text(
                    text = "CORE ARCHITECTURE & SYSTEMS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 1: Audio Engine
                ArchitectureFeatureCard(
                    icon = Icons.Rounded.Headphones,
                    iconTint = Color(0xFF6C5CE7),
                    title = "Bit-Perfect Lossless Audio Pipeline",
                    description = "Powered by ExoPlayer Media3 with custom audio decoders supporting up to 24-bit/96kHz Hi-Res FLAC playback. Features dynamic 10-band equalizer, spatial stereo imaging, Dolby Atmos tone mapping, and seamless gapless playback transitions.",
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Neural AI Engine
                ArchitectureFeatureCard(
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = Color(0xFF00CEC9),
                    title = "Autonomous Zero-API Neural AI",
                    description = "100% on-device intelligent music curation without third-party subscription fees or latency. Evaluates user taste profiles (plays, skips, favorites), circadian time-of-day rhythms, acoustic warmth, and tempo BPM to synthesize continuous dynamic radio stations.",
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 3: Community & Encrypted Private Chat
                ArchitectureFeatureCard(
                    icon = Icons.Rounded.ChatBubble,
                    iconTint = Color(0xFFFD79A8),
                    title = "Real-Time Community & Peer Chat",
                    description = "Global community board for ratings, music reviews, and track recommendations, paired with WhatsApp-style 1-on-1 private messaging powered by real-time Firebase Firestore snapshots.",
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 4: Privacy Manifesto
                ArchitectureFeatureCard(
                    icon = Icons.Rounded.Security,
                    iconTint = Color(0xFF81ECEC),
                    title = "Privacy-First Architecture",
                    description = "Zero trackers, zero invasive telemetry sales, and transparent permissions. Your listening preferences and favorites remain securely encrypted with local caching priority.",
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tech Stack Showcase
                Text(
                    text = "TECHNOLOGY STACK & RUNTIME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(
                        "Kotlin 2.3",
                        "Jetpack Compose",
                        "Media3 ExoPlayer 1.5",
                        "Firebase Firestore",
                        "Coroutines & StateFlow",
                        "On-Device AI Engine",
                        "Coil 3 Image Engine",
                        "Haze Glassmorphism",
                        "YouTube Music & JioSaavn",
                        "Automated CI/CD Releases",
                    ).forEach { tech ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B1D33))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = tech,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                ),
                                color = Color(0xFF81ECEC),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Developer Connect Action Buttons
                Button(
                    onClick = { openUrl("https://github.com/MishrajiiCode") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5CE7),
                    ),
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explore GitHub (@MishrajiiCode)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { openUrl("mailto:mishrajiicode@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00CEC9).copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Raj Mishra", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Build Telemetry Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF101122),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Release v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "Target SDK 35 • Bit-Perfect",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF00CEC9),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Crafted with immense passion & precision by Raj Mishra\n© 2026 VibeWave. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ArchitectureFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121424).copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
        }
    }
}
