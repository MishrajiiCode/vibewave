package com.music.vibewave.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.vibewave.data.ai.AiMusicEngine
import com.music.vibewave.data.gamification.VibeRewardsManager
import com.music.vibewave.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isAi: Boolean,
    val text: String,
    val recommendedSongs: List<Song> = emptyList(),
    val detectedMood: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Full-app interactive AI Companion sheet.
 * Powered by on-device neural acoustic intelligence and VibeWave knowledge base.
 * Allows users to converse with VibeWave AI, receive personalized music recommendations
 * with 1-tap playback, and ask any questions about streaks, levels, and app features.
 */
@Composable
fun VibeWaveAiChatSheet(
    onDismissRequest: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayQueue: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                isAi = true,
                text = "Hey there! I'm **VibeWave AI** ✨ — your smart on-device musical companion.\n\nAsk me anything! I can pick songs for any mood, suggest personalized tracks you'll love, or answer questions about streaks, Vibe XP, Listen Together, and equalizer tuning.",
            )
        )
    }

    val quickSuggestions = remember {
        listOf(
            "🎧 What should I listen to right now?",
            "🔥 How do Daily Streaks & Vibe XP work?",
            "🤝 How does Listen Together with Host DJ work?",
            "🌙 Recommend late night chill acoustic vibes",
            "⚡ High-energy gym phonk workout beats",
            "❤️ Romantic soulful Bollywood melodies",
            "🎛️ How do I tune the Equalizer for best bass?",
            "🏆 How does the Weekly Leaderboard work?",
        )
    }

    fun handleSend(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isThinking) return

        messages.add(AiChatMessage(isAi = false, text = trimmed))
        inputText = ""
        keyboardController?.hide()
        isThinking = true

        scope.launch {
            val response = processUserQuery(trimmed)
            messages.add(response)
            isThinking = false
            // Award XP for engaging with AI!
            VibeRewardsManager.addXp(15, "Consulted VibeWave AI ✨")
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0C0D16),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = Color(0xFF131524),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6C5CE7).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00CEC9)),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF00CEC9),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VibeWave AI Companion",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF00CEC9).copy(alpha = 0.2f),
                                ) {
                                    Text(
                                        text = "NEURAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color(0xFF00CEC9),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                            Text(
                                text = "Zero-API Intelligence & Music Discovery",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8E92BC),
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Quick Suggestions Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(quickSuggestions) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E32),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7).copy(alpha = 0.6f), Color(0xFF00CEC9).copy(alpha = 0.4f))
                            )
                        ),
                        modifier = Modifier.clickable { handleSend(prompt) },
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFE0E3F5),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onPlaySong = onPlaySong,
                        onPlayQueue = onPlayQueue,
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF00CEC9),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VibeWave AI is thinking & finding tunes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00CEC9),
                            )
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = Color(0xFF141624),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about songs, vibes, or app features...") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { handleSend(inputText) }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C5CE7),
                            unfocusedBorderColor = Color(0xFF2B2E46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        ),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { handleSend(inputText) },
                        enabled = inputText.isNotBlank() && !isThinking,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (inputText.isNotBlank() && !isThinking) Color(0xFF6C5CE7) else Color(0xFF22263C),
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank() && !isThinking) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: AiChatMessage,
    onPlaySong: (Song) -> Unit,
    onPlayQueue: (List<Song>) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End,
    ) {
        if (message.isAi) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF6C5CE7).copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00CEC9)),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00CEC9),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isAi) 4.dp else 16.dp,
                bottomEnd = if (message.isAi) 16.dp else 4.dp,
            ),
            color = if (message.isAi) Color(0xFF181B2C) else Color(0xFF6C5CE7),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (message.isAi) Color(0xFF2B2F48) else Color(0xFF7D6DF2),
            ),
            modifier = Modifier.fillMaxWidth(if (message.isAi) 0.92f else 0.8f),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.detectedMood != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00CEC9).copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "VIBE: ${message.detectedMood.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = Color(0xFF00CEC9),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 21.sp,
                )

                // Recommended Songs Cards
                if (message.recommendedSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CURATED TRACKS (${message.recommendedSongs.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB300),
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF6C5CE7).copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00CEC9)),
                            modifier = Modifier.clickable { onPlayQueue(message.recommendedSongs) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = Color(0xFF00CEC9),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play All",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        message.recommendedSongs.take(5).forEach { song ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF131522),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282C44)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaySong(song) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artist ?: "Unknown Artist",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF8E92BC),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF00CEC9).copy(alpha = 0.2f),
                                        modifier = Modifier.size(30.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color(0xFF00CEC9),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Intelligent neural router that answers app questions or fetches music recommendations.
 */
private suspend fun processUserQuery(query: String): AiChatMessage = withContext(Dispatchers.IO) {
    val lower = query.lowercase().trim()

    // 1. App Knowledge & Feature Inquiries
    if (lower.contains("streak") || lower.contains("flame")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "🔥 **Daily Listening Streaks in VibeWave:**\n\n" +
                    "• **How it works**: Play at least one song every day to maintain your streak!\n" +
                    "• **Streak Flame**: Displayed right at the front of your top bar (`🔥 1`, `🔥 7`, etc.).\n" +
                    "• **Daily XP**: You get **+50 bonus Vibe XP** each day your streak continues.\n" +
                    "• **Milestones**: Hitting 3, 7, 14, and 30-day streaks unlocks prestigious trophies and broadcasts your milestone to the Engagement Center!",
        )
    }

    if (lower.contains("xp") || lower.contains("level") || lower.contains("tier") || lower.contains("points")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "⚡ **Vibe XP & Level Progression System:**\n\n" +
                    "• **Earning XP**:\n" +
                    "  - **+5 XP** per minute of real playback.\n" +
                    "  - **+15 XP** for completing a full track.\n" +
                    "  - **+30 XP** for joining a *Listen Together* room.\n" +
                    "  - **+50 XP** for daily listening streaks.\n" +
                    "  - **+5 XP** for congratulating fellow listeners in the Hub!\n\n" +
                    "• **Level Tiers (1 to 25)**: Climb from **Acoustic Novice** up through *Sound Explorer*, *Vibe Master*, *Sonic Alchemist*, and reach the supreme rank of **Cosmic Audiophile**!",
        )
    }

    if (lower.contains("together") || lower.contains("party") || lower.contains("host") || lower.contains("dj") || lower.contains("room")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "🤝 **Real-Time Listen Together with Host DJ:**\n\n" +
                    "• **Synchronized Playback**: Create or join a room using the `Together` pill in the top bar. You and your friends hear the exact same song at the exact same millisecond!\n" +
                    "• **Live In-Room Chat**: Text and react with friends right while music is playing.\n" +
                    "• **Host DJ Access Control**: Room hosts can toggle whether guests can add songs, or lock the DJ booth so only the host manages the queue.\n" +
                    "• **Earn XP**: Hosting or joining awards up to +40 XP!",
        )
    }

    if (lower.contains("community") || lower.contains("hub") || lower.contains("engagement") || lower.contains("quest") || lower.contains("mission")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "⚡ **Engagement Center & Vibe Hub:**\n\n" +
                    "• **Live Pulse**: See music reviews and live celebratory announcements whenever users unlock trophies or reach new tiers.\n" +
                    "• **👏 Congratulate (+5 XP)**: Tap the congratulate button on any user's achievement to give cheers and earn bonus XP for yourself!\n" +
                    "• **🎯 Daily Missions**: Daily tune challenges to keep you in the groove.\n" +
                    "• **🏆 Leaderboard**: Podium view (🥇, 🥈, 🥉) celebrating the top weekly listeners.",
        )
    }

    if (lower.contains("equalizer") || lower.contains("eq") || lower.contains("bass") || lower.contains("sound")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "🎛️ **10-Band Studio Equalizer & Audio Calibration:**\n\n" +
                    "• Open Settings ➔ **Equalizer** to access the 10-band audio processor.\n" +
                    "• **Dynamic Bass Punch**: Boost deep sub-bass frequencies (31Hz & 62Hz) for headphones and speakers.\n" +
                    "• **Presets**: Choose from *Acoustic*, *Bass Boost*, *Vocal Clarity*, *Electronic*, and *Rock*.",
        )
    }

    if (lower.contains("download") || lower.contains("offline")) {
        return@withContext AiChatMessage(
            isAi = true,
            text = "📲 **Offline Downloads:**\n\n" +
                    "• Tap the **Download** button on any album, playlist, or song detail screen.\n" +
                    "• Downloaded songs are saved to your device cache so you can enjoy high-fidelity music without an internet connection!",
        )
    }

    // 2. Music Recommendation Query
    try {
        val result = AiMusicEngine.generateFromPrompt(query).getOrNull()
        if (result != null && result.songs.isNotEmpty()) {
            return@withContext AiChatMessage(
                isAi = true,
                text = "I've synthesized a hand-picked selection of tracks matching \"$query\". Tap any track below to start listening right away 🎶:",
                recommendedSongs = result.songs,
                detectedMood = result.detectedMood,
            )
        }
    } catch (_: Exception) {}

    // 3. General Fallback
    AiChatMessage(
        isAi = true,
        text = "I'm tuned to help you with everything in VibeWave! You can ask me:\n\n" +
                "• *\"Play high-energy gym phonk beats\"*\n" +
                "• *\"Suggest romantic Bollywood songs\"*\n" +
                "• *\"How do streaks and levels work?\"*\n" +
                "• *\"How to use Listen Together?\"*",
    )
}

