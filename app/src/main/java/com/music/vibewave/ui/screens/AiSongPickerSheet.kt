package com.music.vibewave.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.vibewave.data.ai.AiMusicEngine
import com.music.vibewave.data.firebase.ActivityTracker
import com.music.vibewave.data.firebase.FirestoreManager
import com.music.vibewave.data.model.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSongPickerSheet(
    onDismissRequest: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayRadio: (Song) -> Unit,
    onPlayQueue: (List<Song>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val tasteProfile by FirestoreManager.tasteProfile.collectAsStateWithLifecycle()
    var selectedMood by remember { mutableStateOf(AiMusicEngine.getContextualTimeVibe()) }
    var selectedEnergy by remember { mutableStateOf(AiMusicEngine.Energy.BALANCED) }
    var isLoading by remember { mutableStateOf(false) }
    var recommendation by remember { mutableStateOf<AiMusicEngine.AiRecommendation?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animated Equalizer for AI neural status
    val infiniteTransition = rememberInfiniteTransition(label = "aiWave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 9f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(410, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF090A13),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Cosmic Mesh Background with deep indigo, electric violet, and cyan flares
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C0E1B),
                            Color(0xFF13112A),
                            Color(0xFF0A0B14),
                        ),
                    ),
                )
        ) {
            // Ambient Radial Flare overlay behind top header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6C5CE7).copy(alpha = 0.22f),
                                Color(0xFF00CEC9).copy(alpha = 0.08f),
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
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
                                    )
                                )
                                .padding(1.5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(39.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color(0xFF111226)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF00CEC9),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Neural Studio",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.3).sp,
                                    ),
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Live Neural Pulsing Wave
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(14.dp)
                                ) {
                                    Box(Modifier.width(3.dp).height(wave1.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFF6C5CE7)))
                                    Box(Modifier.width(3.dp).height(wave2.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFF00CEC9)))
                                    Box(Modifier.width(3.dp).height(wave3.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFFFD79A8)))
                                }
                            }

                            Text(
                                text = if (tasteProfile.playCount > 0 || tasteProfile.favoriteCount > 0) {
                                    "✨ Calibrated with ${tasteProfile.playCount} plays · ${tasteProfile.favoriteCount} likes"
                                } else {
                                    "Autonomous on-device acoustic vector engine"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF81ECEC).copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Acoustic Vibe Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "1. SELECT ACOUSTIC VIBE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = "${AiMusicEngine.Mood.entries.size} Neural Modes",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFF00CEC9),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiMusicEngine.Mood.entries.forEach { mood ->
                        val isSelected = mood == selectedMood
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF6C5CE7), Color(0xFF4834D4))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF17192C), Color(0xFF141525))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00CEC9), Color(0xFF6C5CE7))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { selectedMood = mood }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "${mood.emoji} ${mood.label}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                ),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Energy & Tempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "2. ENERGY & TEMPO COEFFICIENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = selectedEnergy.speedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00CEC9),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiMusicEngine.Energy.entries.forEach { energy ->
                        val isSelected = energy == selectedEnergy
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00CEC9), Color(0xFF0984E3))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF17192C), Color(0xFF131524))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.6f), Color(0xFF00CEC9))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { selectedEnergy = energy }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = energy.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                    ),
                                    color = if (isSelected) Color(0xFF070913) else Color.White.copy(alpha = 0.82f),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = energy.speedLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = if (isSelected) Color(0xFF070913).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons: Synthesize Pick vs Smart Surprise
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                AiMusicEngine.pickBestSong(selectedMood, selectedEnergy)
                                    .onSuccess { rec ->
                                        recommendation = rec
                                        ActivityTracker.onAiPick(
                                            vibe = selectedMood.label,
                                            songTitle = rec.song.title,
                                            artist = rec.song.artist,
                                        )
                                    }
                                    .onFailure {
                                        errorMessage = "Could not find songs for this vibe. Try another mood."
                                    }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7),
                        ),
                        enabled = !isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Synthesize Pick",
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                AiMusicEngine.getSmartSurprise()
                                    .onSuccess { rec ->
                                        recommendation = rec
                                        selectedMood = rec.mood
                                        selectedEnergy = rec.energy
                                        ActivityTracker.onAiPick(
                                            vibe = "Smart Surprise (${rec.mood.label})",
                                            songTitle = rec.song.title,
                                            artist = rec.song.artist,
                                        )
                                    }
                                    .onFailure {
                                        errorMessage = "Could not fetch smart surprise."
                                    }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                    ) {
                        Text(
                            text = "🎲 Surprise",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Loading Status Readout
                if (isLoading) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF00CEC9),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI Neural Engine is analyzing frequencies & taste profile…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81ECEC),
                        )
                    }
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Instant 20-Track AI Smart Station Generator Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF6C5CE7).copy(alpha = 0.5f), Color(0xFF00CEC9).copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable {
                            scope.launch {
                                isLoading = true
                                AiMusicEngine.generateAiStation(selectedMood, selectedEnergy, 20)
                                    .onSuccess { station ->
                                        if (station.isNotEmpty()) {
                                            onPlayQueue(station)
                                            onDismissRequest()
                                        }
                                    }
                                isLoading = false
                            }
                        },
                    color = Color(0xFF151428).copy(alpha = 0.85f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Generate 20-Track AI Station",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Dynamic arc (Intro → Climax → Cool-down) for ${selectedMood.label}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF81ECEC).copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00CEC9),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // AI Neural Recommendation Result Card
                recommendation?.let { rec ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00CEC9).copy(alpha = 0.7f),
                                        Color(0xFF6C5CE7).copy(alpha = 0.6f),
                                        Color(0xFFFD79A8).copy(alpha = 0.3f),
                                    ),
                                ),
                                shape = RoundedCornerShape(20.dp),
                            ),
                        color = Color(0xFF131224),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00CEC9).copy(alpha = 0.2f))
                                        .border(
                                            1.dp,
                                            Color(0xFF00CEC9).copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🎯 ${rec.matchScore}% NEURAL MATCH",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp,
                                        ),
                                        color = Color(0xFF00CEC9),
                                    )
                                }

                                Text(
                                    text = "${rec.mood.emoji} ${rec.energy.label}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = rec.song.thumbnailUrl,
                                    contentDescription = rec.song.title,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.song.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rec.song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Acoustic Metrics Radar Breakdown Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    "⚡ Energy ${rec.acousticMetrics.energyPercent}%" to Color(0xFF6C5CE7),
                                    "💃 Groove ${rec.acousticMetrics.danceabilityPercent}%" to Color(0xFF00CEC9),
                                    "🌿 Warmth ${rec.acousticMetrics.acousticWarmthPercent}%" to Color(0xFFFD79A8),
                                    "⏱️ ${rec.acousticMetrics.estimatedBpm} BPM" to Color(0xFFA29BFE),
                                ).forEach { (metric, tint) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tint.copy(alpha = 0.15f))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = metric,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = tint,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = rec.reason,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = Color.White.copy(alpha = 0.8f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Playback Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        onPlaySong(rec.song)
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6C5CE7),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play Now", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onPlayRadio(rec.song)
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF202238),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Radio,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Radio")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            AiMusicEngine.generateAiQueue(rec.mood)
                                                .onSuccess { queue ->
                                                    onPlayQueue(queue)
                                                    onDismissRequest()
                                                }
                                            isLoading = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF202238),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QueueMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Queue")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
