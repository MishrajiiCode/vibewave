package com.music.vibewave.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.vibewave.data.firebase.FirestoreManager
import com.music.vibewave.data.firebase.ListenTogetherManager
import com.music.vibewave.data.model.Song

@Composable
fun ListenTogetherSheet(
    onDismiss: () -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val room by ListenTogetherManager.room.collectAsStateWithLifecycle()
    val isHost by ListenTogetherManager.isHost.collectAsStateWithLifecycle()
    val currentUser by FirestoreManager.currentUser.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Host Room, 1 = Join Room
    var displayName by remember {
        mutableStateOf(currentUser?.name?.ifBlank { "Viber" } ?: "Viber")
    }
    var roomCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0C0D14),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            // Header Bar
            Surface(
                color = Color(0xFF141622),
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
                            color = Color(0xFF00D9FF).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.WifiTethering,
                                    contentDescription = null,
                                    tint = Color(0xFF00D9FF),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Listen Together",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = "Real-time synchronized listening",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E92BC),
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Main Content Body
            if (room?.inRoom == true) {
                // Active Room View
                ActiveRoomContent(
                    room = room!!,
                    isHost = isHost,
                    onLeave = {
                        ListenTogetherManager.leaveRoom()
                        Toast.makeText(context, "Left the room", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = onDismiss,
                )
            } else {
                // Lobby View: Host or Join
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    // Display Name Field
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(30) },
                        label = { Text("Your Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = Color(0xFF282B3E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF9B8FFF),
                            unfocusedLabelColor = Color(0xFF8E92BC),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF161826),
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF00D9FF),
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    "Host Room",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) Color(0xFF00D9FF) else Color(0xFF8E92BC),
                                )
                            },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    "Join Room",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) Color(0xFF00D9FF) else Color(0xFF8E92BC),
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE74C3C).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = Color(0xFFFF7675),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        // Host Room Mode
                        HostRoomSection(
                            currentSong = currentSong,
                            isLoading = isLoading,
                            onHost = {
                                isLoading = true
                                errorMessage = null
                                ListenTogetherManager.createRoom(
                                    hostName = displayName,
                                    initialSong = currentSong,
                                    isPlaying = isPlaying,
                                    positionMs = playbackPositionMs,
                                ) { result ->
                                    isLoading = false
                                    result.onFailure {
                                        errorMessage = it.message ?: "Could not create room"
                                    }
                                }
                            },
                        )
                    } else {
                        // Join Room Mode
                        JoinRoomSection(
                            roomCode = roomCodeInput,
                            onRoomCodeChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                            isLoading = isLoading,
                            onJoin = {
                                if (roomCodeInput.length != 6) {
                                    errorMessage = "Please enter a valid 6-character room code"
                                    return@JoinRoomSection
                                }
                                isLoading = true
                                errorMessage = null
                                ListenTogetherManager.joinRoom(
                                    roomCode = roomCodeInput,
                                    guestName = displayName,
                                ) { result ->
                                    isLoading = false
                                    result.onFailure {
                                        errorMessage = it.message ?: "Failed to join room"
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostRoomSection(
    currentSong: Song?,
    isLoading: Boolean,
    onHost: () -> Unit,
) {
    Column {
        Text(
            text = "Create a synchronized listening room. Anyone with your 6-character code can listen to the exact same tracks in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB2BEC3),
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Song preview
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141724),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23273D)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentSong?.thumbnailUrl != null) {
                    AsyncImage(
                        model = currentSong.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6C63FF).copy(alpha = 0.2f),
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF9B8FFF),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No Song Playing Currently",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentSong?.artist ?: "You can pick any song after creating",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E92BC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onHost,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6C63FF),
                contentColor = Color.White,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Host Room & Get Code",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinRoomSection(
    roomCode: String,
    onRoomCodeChange: (String) -> Unit,
    isLoading: Boolean,
    onJoin: () -> Unit,
) {
    Column {
        Text(
            text = "Enter the 6-character room code shared by your friend to jump into their live listening session.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB2BEC3),
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = onRoomCodeChange,
            label = { Text("6-Character Room Code") },
            placeholder = { Text("e.g. VIBE88") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onJoin() }),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00D9FF),
                unfocusedBorderColor = Color(0xFF282B3E),
                focusedTextColor = Color(0xFF00D9FF),
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF00D9FF),
                unfocusedLabelColor = Color(0xFF8E92BC),
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onJoin,
            enabled = !isLoading && roomCode.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D9FF),
                contentColor = Color(0xFF0A0A1F),
                disabledContainerColor = Color(0xFF282B3E),
                disabledContentColor = Color(0xFF8E92BC),
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF0A0A1F),
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join Session Now",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomContent(
    room: ListenTogetherManager.ListenTogetherRoom,
    isHost: Boolean,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Room Code Banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141728),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF6C63FF), Color(0xFF00D9FF)),
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFA3).copy(alpha = pulseAlpha)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHost) "YOU ARE HOSTING" else "CONNECTED TO ROOM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color(0xFF00FFA3),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = room.roomId,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                    ),
                    color = Color.White,
                )

                Text(
                    text = "${room.participantCount} participant${if (room.participantCount == 1) "" else "s"} listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E92BC),
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("VibeWave Room Code", room.roomId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Room code ${room.roomId} copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E324E)),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my VibeWave Listen Together room! Room code: ${room.roomId}\nDownload VibeWave: https://github.com/MishrajiiCode/vibewave",
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Room Code"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Synced Track Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131522),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22263C)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SYNCHRONIZED TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color(0xFF8E92BC),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (room.isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = if (room.isPlaying) Color(0xFF00FFA3) else Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (room.isPlaying) "Playing in Sync" else "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (room.isPlaying) Color(0xFF00FFA3) else Color(0xFFFFB300),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (room.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = room.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF6C63FF).copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFF9B8FFF),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room.songTitle.ifBlank { "Waiting for host track..." },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = room.songArtist.ifBlank { "Host: ${room.hostName}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E92BC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Action Buttons
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E2135),
                contentColor = Color.White,
            ),
        ) {
            Text("Listen in Background (Keep in Sync)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF6B6B),
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.4f)),
        ) {
            Text(if (isHost) "End Room for Everyone" else "Leave Room")
        }
    }
}

