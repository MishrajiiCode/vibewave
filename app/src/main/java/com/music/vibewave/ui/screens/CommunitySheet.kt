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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.vibewave.data.firebase.CommunityManager
import com.music.vibewave.data.firebase.FirestoreManager
import com.music.vibewave.data.gamification.VibeRewardsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunitySheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by CommunityManager.posts.collectAsStateWithLifecycle()
    val communityUsers by CommunityManager.communityUsers.collectAsStateWithLifecycle()
    val currentUser by FirestoreManager.currentUser.collectAsStateWithLifecycle()
    val profile by VibeRewardsManager.profile.collectAsStateWithLifecycle()
    val dailyQuests by VibeRewardsManager.dailyQuests.collectAsStateWithLifecycle()
    val leaderboard by VibeRewardsManager.leaderboard.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pulse/Feed, 1 = Leaderboard, 2 = DMs
    var activeChatUser by remember { mutableStateOf<FirestoreManager.UserProfile?>(null) }

    LaunchedEffect(Unit) {
        CommunityManager.loadCommunityUsers()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0D0E16),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = Color(0xFF141624),
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.6f)),
                            modifier = Modifier.size(40.dp),
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Engagement Center",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = "Live Pulse, Community Unlocks & Leaderboards",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF81ECEC).copy(alpha = 0.85f),
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Interactive Engagement Banner
            EngagementStatsBanner(profile = profile)

            // Daily Mystery Vibe Box Card
            DailyMysteryBoxCard()

            // Daily Quests Bar
            DailyQuestsSection(quests = dailyQuests)

            // Top Tab Navigation (3 Tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tabs = listOf("🌟 Live Pulse", "🏆 Leaderboard", "💬 Viber Chat")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFF1E2133),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = index
                                if (index != 2) activeChatUser = null
                            },
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(vertical = 9.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Content Body
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        EngagementFeedView(
                            posts = posts,
                            currentUserId = currentUser?.uid ?: "",
                        )
                    }
                    1 -> {
                        EngagementLeaderboardView(
                            leaderboard = leaderboard,
                            myUid = profile.uid,
                        )
                    }
                    2 -> {
                        if (activeChatUser == null) {
                            CommunityUsersListView(
                                users = communityUsers,
                                onUserClick = { targetUser ->
                                    activeChatUser = targetUser
                                    CommunityManager.openConversation(targetUser.uid)
                                },
                            )
                        } else {
                            PrivateChatThreadView(
                                otherUser = activeChatUser!!,
                                currentUserId = currentUser?.uid ?: "",
                                onBack = { activeChatUser = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngagementStatsBanner(profile: VibeRewardsManager.RewardProfile) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151829),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFF6C5CE7).copy(alpha = 0.6f), Color(0xFFFFB300).copy(alpha = 0.6f)))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6C5CE7),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Lv.${profile.level}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = profile.levelTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "🎧 ${profile.minutesPlayedCount} mins • 🎵 ${profile.songsPlayedCount} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00CEC9),
                        )
                    }
                }

                // Daily Streak Pill with Multiplier
                val multiplier = VibeRewardsManager.getStreakMultiplier(profile.streakDays)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF4757).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757).copy(alpha = 0.6f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF4757),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (multiplier > 1f) "${profile.streakDays}d (${multiplier}x)" else "${profile.streakDays}d Streak",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFFFD32A),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress to next tier
            LinearProgressIndicator(
                progress = { profile.currentLevelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFFFB300),
                trackColor = Color(0xFF25293E),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(profile.currentLevelProgress * 100).toInt()}% towards next Tier",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = Color(0xFF8E92BC),
                )
                Text(
                    text = "${profile.weeklyXp} Weekly XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB300),
                )
            }
        }
    }
}

/**
 * 1-Tap Daily Mystery Vibe Box in the Engagement Center.
 * Offers users a mystery gift box once every 24 hours, awarding +25 to +100 bonus Vibe XP
 * boosted by their active streak multiplier, accompanied by a celebration modal!
 */
@Composable
private fun DailyMysteryBoxCard() {
    var isClaimed by remember { mutableStateOf(!VibeRewardsManager.canClaimDailyVibeBox()) }
    val profile by VibeRewardsManager.profile.collectAsStateWithLifecycle()
    val multiplier = VibeRewardsManager.getStreakMultiplier(profile.streakDays)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isClaimed) Color(0xFF131522) else Color(0xFF1B1B34),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isClaimed) Brush.horizontalGradient(listOf(Color(0xFF282C44), Color(0xFF1E2134)))
            else Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF4757)))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isClaimed) Color(0xFF23263C) else Color(0xFFFFB300).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = if (isClaimed) "✅" else "🎁", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isClaimed) "Today's Mystery Vibe Box Claimed" else "Daily Mystery Vibe Box Ready!",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isClaimed) Color(0xFF8E92BC) else Color.White,
                    )
                    Text(
                        text = if (isClaimed) "Next mystery gift unlocks tomorrow" else "+25 to +100 XP (${multiplier}x streak boost active!)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isClaimed) Color(0xFF63678A) else Color(0xFFFFD32A),
                    )
                }
            }

            if (!isClaimed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF4757),
                    modifier = Modifier.clickable {
                        val xp = VibeRewardsManager.claimDailyVibeBox()
                        if (xp > 0L) {
                            isClaimed = true
                        }
                    },
                ) {
                    Text(
                        text = "Claim ✨",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuestsSection(quests: List<VibeRewardsManager.DailyQuest>) {
    if (quests.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🎯 DAILY MISSIONS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color(0xFF8E92BC),
            )
            Text(
                text = "+XP for active vibing",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF00CEC9),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(quests, key = { it.id }) { quest ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (quest.isCompleted) Color(0xFF142426) else Color(0xFF171A2B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (quest.isCompleted) Color(0xFF00CEC9).copy(alpha = 0.5f) else Color(0xFF262A40)
                    ),
                    modifier = Modifier.width(170.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = quest.iconEmoji, fontSize = 16.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (quest.isCompleted) Color(0xFF00CEC9).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = if (quest.isCompleted) "CLAIMED" else "+${quest.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                    ),
                                    color = if (quest.isCompleted) Color(0xFF00CEC9) else Color(0xFFFFB300),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = quest.description,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF8E92BC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngagementFeedView(
    posts: List<CommunityManager.CommunityPost>,
    currentUserId: String,
) {
    var newPostContent by remember { mutableStateOf("") }
    var newPostSongTitle by remember { mutableStateOf("") }
    var newPostArtist by remember { mutableStateOf("") }
    var newPostRating by remember { mutableIntStateOf(5) }
    var isPosting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Compose Review Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181A28),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Share Music Review or Flex Your Vibe",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        placeholder = { Text("What are you listening to? Write your review or thoughts...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C5CE7),
                            unfocusedBorderColor = Color(0xFF2E3248),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newPostSongTitle,
                            onValueChange = { newPostSongTitle = it },
                            placeholder = { Text("Song (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2E3248),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            ),
                        )
                        OutlinedTextField(
                            value = newPostArtist,
                            onValueChange = { newPostArtist = it },
                            placeholder = { Text("Artist (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2E3248),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rating:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            (1..5).forEach { star ->
                                val isFilled = star <= newPostRating
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = if (isFilled) Color(0xFFFFD166) else Color(0xFF383B52),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { newPostRating = star },
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newPostContent.isNotBlank()) {
                                    isPosting = true
                                    CommunityManager.postReview(
                                        content = newPostContent,
                                        songTitle = newPostSongTitle,
                                        songArtist = newPostArtist,
                                        rating = newPostRating,
                                    )
                                    VibeRewardsManager.addXp(20, "Shared Community Review 💬")
                                    newPostContent = ""
                                    newPostSongTitle = ""
                                    newPostArtist = ""
                                    isPosting = false
                                }
                            },
                            enabled = newPostContent.isNotBlank() && !isPosting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Post 🚀", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No community activity yet. Listen to songs and unlock trophies to start the pulse!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                if (post.postType in listOf("achievement", "tier", "streak")) {
                    AchievementBroadcastCard(
                        post = post,
                        currentUserId = currentUserId,
                        onCheerClick = {
                            CommunityManager.cheerPost(post.id) {
                                VibeRewardsManager.addXp(5, "Cheered ${post.userName} 👏")
                            }
                        },
                    )
                } else {
                    CommunityReviewCard(
                        post = post,
                        isMyPost = post.userId == currentUserId,
                        onLikeClick = { CommunityManager.likePost(post.id) },
                        onDeleteClick = { CommunityManager.deletePost(post.id) },
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AchievementBroadcastCard(
    post: CommunityManager.CommunityPost,
    currentUserId: String,
    onCheerClick: () -> Unit,
) {
    val isCheered = remember(post.cheeredBy, currentUserId) {
        post.cheeredBy.contains(currentUserId)
    }
    val timeAgo = remember(post.timestamp) {
        val diff = System.currentTimeMillis() - post.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(post.timestamp))
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141728),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFD700).copy(alpha = 0.8f),
                    Color(0xFFFF7675).copy(alpha = 0.7f),
                    Color(0xFF6C5CE7).copy(alpha = 0.8f),
                )
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = post.badgeEmoji ?: "🏆", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = when (post.postType) {
                                "tier" -> "🌟 Tier Ascension • $timeAgo"
                                "streak" -> "🔥 Streak Fire • $timeAgo"
                                else -> "🏆 Trophy Unlocked • $timeAgo"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFFFFD700),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Congratulate button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${post.cheersCount} vibers congratulated",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E92BC),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCheered) Color(0xFF00CEC9).copy(alpha = 0.25f) else Color(0xFF6C5CE7).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCheered) Color(0xFF00CEC9) else Color(0xFF6C5CE7),
                    ),
                    modifier = Modifier.clickable(enabled = !isCheered) { onCheerClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isCheered) "🎉 Cheered!" else "👏 Congratulate (+5 XP)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCheered) Color(0xFF00CEC9) else Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityReviewCard(
    post: CommunityManager.CommunityPost,
    isMyPost: Boolean,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val isLiked = remember(post.likedBy) { post.likedBy.isNotEmpty() }
    val timeAgo = remember(post.timestamp) {
        val diff = System.currentTimeMillis() - post.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(post.timestamp))
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1D2C),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6C5CE7).copy(alpha = 0.35f),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = post.userName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF81ECEC),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }

                // Rating stars
                Row {
                    (1..post.rating).forEach {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            if (!post.songTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF26293D),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF00CEC9),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.songTitle} ${post.songArtist?.let { "• $it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF81ECEC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() },
                ) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF7675) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount} likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                if (isMyPost) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF7675).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngagementLeaderboardView(
    leaderboard: List<VibeRewardsManager.LeaderboardEntry>,
    myUid: String,
) {
    if (leaderboard.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Weekly Leaderboard Reset",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Play songs and maintain your streak to top the podium!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E92BC),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PodiumSection(topEntries = leaderboard.take(3))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ALL LISTENERS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color(0xFF8E92BC),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            items(leaderboard, key = { it.uid }) { entry ->
                LeaderboardRow(entry = entry, isMe = entry.uid == myUid)
            }
        }
    }
}

@Composable
private fun PodiumSection(topEntries: List<VibeRewardsManager.LeaderboardEntry>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141726),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24283E)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            // #2 Silver
            val second = topEntries.getOrNull(1)
            PodiumPillar(
                rank = 2,
                entry = second,
                color = Color(0xFFBDC3C7),
                pillarHeight = 70.dp,
            )

            // #1 Gold
            val first = topEntries.getOrNull(0)
            PodiumPillar(
                rank = 1,
                entry = first,
                color = Color(0xFFFFD700),
                pillarHeight = 90.dp,
            )

            // #3 Bronze
            val third = topEntries.getOrNull(2)
            PodiumPillar(
                rank = 3,
                entry = third,
                color = Color(0xFFCD7F32),
                pillarHeight = 55.dp,
            )
        }
    }
}

@Composable
private fun PodiumPillar(
    rank: Int,
    entry: VibeRewardsManager.LeaderboardEntry?,
    color: Color,
    pillarHeight: androidx.compose.ui.unit.Dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp),
    ) {
        if (entry != null) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, color),
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "${entry.weeklyXp} XP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = color,
                textAlign = TextAlign.Center,
            )
            if (entry.minutesPlayed > 0) {
                Text(
                    text = "🎧 ${entry.minutesPlayed}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFF00CEC9),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Spacer(modifier = Modifier.height(60.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = color.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
            modifier = Modifier
                .width(70.dp)
                .height(pillarHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when (rank) {
                        1 -> "🥇 1st"
                        2 -> "🥈 2nd"
                        else -> "🥉 3rd"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: VibeRewardsManager.LeaderboardEntry,
    isMe: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMe) Color(0xFF1B1E33) else Color(0xFF131522),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMe) Color(0xFF00D9FF).copy(alpha = 0.6f) else Color(0xFF22263C),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (entry.rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFBDC3C7)
                        3 -> Color(0xFFCD7F32)
                        else -> Color(0xFF8E92BC)
                    },
                    modifier = Modifier.width(32.dp),
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6C63FF).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF9B8FFF),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        if (isMe) {
                            Text(
                                text = " (You)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00D9FF),
                            )
                        }
                    }
                    Text(
                        text = "Lv.${entry.level} • ${entry.levelTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E92BC),
                    )
                    Text(
                        text = "🎧 ${entry.minutesPlayed}m • 🎵 ${entry.songsPlayed} • 🔥 ${entry.streakDays}d",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = Color(0xFF00CEC9),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFB300).copy(alpha = 0.15f),
            ) {
                Text(
                    text = "${entry.weeklyXp} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB300),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CommunityUsersListView(
    users: List<FirestoreManager.UserProfile>,
    onUserClick: (FirestoreManager.UserProfile) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search vibers to message...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6C5CE7),
                unfocusedBorderColor = Color(0xFF2E3248),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text("No users found", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.uid }) { user ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B1D2C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(user) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00CEC9).copy(alpha = 0.25f),
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF00CEC9),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Tap to chat in real-time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF81ECEC).copy(alpha = 0.7f),
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
private fun PrivateChatThreadView(
    otherUser: FirestoreManager.UserProfile,
    currentUserId: String,
    onBack: () -> Unit,
) {
    val messages by CommunityManager.currentConversation.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Surface(
            color = Color(0xFF1A1D2E),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00CEC9).copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherUser.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF00CEC9),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = otherUser.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "Encrypted Private Chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00FFA3),
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp,
                        ),
                        color = if (isMe) Color(0xFF6C5CE7) else Color(0xFF202336),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // Message Input
        Surface(
            color = Color(0xFF161826),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF2E3248),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    maxLines = 3,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            CommunityManager.sendPrivateMessage(
                                receiverId = otherUser.uid,
                                receiverName = otherUser.name,
                                receiverEmail = otherUser.email,
                                messageText = messageText,
                            )
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) Color(0xFF00CEC9) else Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
