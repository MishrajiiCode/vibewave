package com.music.bitchord.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
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
import coil3.compose.AsyncImage
import com.music.bitchord.data.ai.AiMusicEngine
import com.music.bitchord.data.firebase.ActivityTracker
import com.music.bitchord.data.firebase.FirestoreManager
import com.music.bitchord.data.model.Song
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF161722),
    ) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Song Picker",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = if (tasteProfile.playCount > 0 || tasteProfile.favoriteCount > 0) {
                                "Personalized with your taste (${tasteProfile.playCount} plays · ${tasteProfile.favoriteCount} favs)"
                            } else {
                                "Zero-API on-device curation"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81ECEC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mood & Vibe Section
            Text(
                text = "1. Select Your Vibe",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                                if (isSelected) Color(0xFF6C5CE7)
                                else Color(0xFF252736)
                            )
                            .clickable { selectedMood = mood }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mood.emoji} ${mood.label}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Energy Level Section
            Text(
                text = "2. Energy & Tempo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiMusicEngine.Energy.entries.forEach { energy ->
                    val isSelected = energy == selectedEnergy
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF00CEC9).copy(alpha = 0.85f)
                                else Color(0xFF252736)
                            )
                            .clickable { selectedEnergy = energy }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = energy.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color(0xFF13141E) else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action Buttons
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
                                    errorMessage = "Could not find songs for this vibe. Try another."
                                }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
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
                    Text("Pick Best Song")
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
                                    ActivityTracker.onAiPick(
                                        vibe = "Smart Surprise (${rec.mood.label})",
                                        songTitle = rec.song.title,
                                        artist = rec.song.artist,
                                    )
                                }
                                .onFailure {
                                    errorMessage = "Could not fetch recommendations."
                                }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading,
                ) {
                    Text("🎲 Smart Surprise")
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF81ECEC),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI is matching songs to your vibe…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
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

            // Recommendation Result Card
            recommendation?.let { rec ->
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    color = Color(0xFF222436),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00CEC9).copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🎯 ${rec.matchScore}% MATCH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color(0xFF00CEC9),
                                )
                            }

                            Text(
                                text = rec.mood.emoji,
                                fontSize = 20.sp,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = rec.song.thumbnailUrl,
                                contentDescription = rec.song.title,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop,
                            )

                            Spacer(modifier = Modifier.width(12.dp))

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
                                Text(
                                    text = rec.song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = rec.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
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
                                Text("Play")
                            }

                            Button(
                                onClick = {
                                    onPlayRadio(rec.song)
                                    onDismissRequest()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2D3436),
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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2D3436),
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
