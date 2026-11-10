package com.music.vibewave.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.music.vibewave.data.gamification.VibeRewardsManager

@Composable
fun LeaderboardSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by VibeRewardsManager.profile.collectAsStateWithLifecycle()
    val leaderboard by VibeRewardsManager.leaderboard.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Leaderboard, 1 = Trophies

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0C0D14),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            color = Color(0xFFFFB300).copy(alpha = 0.18f),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Vibe Rewards & Ranks",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = "Listen, earn XP & climb weekly charts",
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

            // User XP & Level Overview Card
            UserLevelBanner(profile = profile)

            // Tabs: Weekly Leaderboard / Achievements
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF131522),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFFFB300),
                    )
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Weekly Chart", fontSize = 13.sp)
                        }
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Trophies (${profile.unlockedBadgeIds.size}/${VibeRewardsManager.ALL_BADGES.size})",
                                fontSize = 13.sp,
                            )
                        }
                    },
                )
            }

            if (selectedTab == 0) {
                LeaderboardView(leaderboard = leaderboard, myUid = profile.uid)
            } else {
                TrophiesView(unlockedIds = profile.unlockedBadgeIds)
            }
        }
    }
}

@Composable
private fun UserLevelBanner(profile: VibeRewardsManager.RewardProfile) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151829),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF6C63FF), Color(0xFFFFB300)),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF6C63FF),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Lv.${profile.level}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = profile.levelTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "${profile.weeklyXp} XP this week • ${profile.totalXp} Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E92BC),
                        )
                        Text(
                            text = "🎧 ${profile.minutesPlayedCount} mins • 🎵 ${profile.songsPlayedCount} songs",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF00CEC9),
                        )
                    }
                }

                // Daily Streak Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF7675).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7675).copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF7675),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.streakDays} Day${if (profile.streakDays == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF7675),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Level Progress Bar
            LinearProgressIndicator(
                progress = { profile.currentLevelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFFFB300),
                trackColor = Color(0xFF25293E),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(profile.currentLevelProgress * 100).toInt()}% to Level ${profile.level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E92BC),
                )
                Text(
                    text = "Goal: ${profile.nextLevelXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E92BC),
                )
            }
        }
    }
}

@Composable
private fun LeaderboardView(
    leaderboard: List<VibeRewardsManager.LeaderboardEntry>,
    myUid: String,
) {
    if (leaderboard.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF353952),
                    modifier = Modifier.size(54.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Weekly Chart Initializing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E92BC),
                )
                Text(
                    text = "Play songs to claim the #1 spot this week!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF63678E),
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
            // Podium for Top 3 (if we have at least 1 entry)
            item {
                PodiumSection(topEntries = leaderboard.take(3))
                Spacer(modifier = Modifier.height(12.dp))
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
private fun TrophiesView(unlockedIds: Set<String>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(VibeRewardsManager.ALL_BADGES, key = { it.id }) { badge ->
            val isUnlocked = unlockedIds.contains(badge.id)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isUnlocked) Color(0xFF161A2E) else Color(0xFF12141F),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUnlocked) Color(0xFFFFB300).copy(alpha = 0.5f) else Color(0xFF22263A),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isUnlocked) Color(0xFFFFB300).copy(alpha = 0.2f) else Color(0xFF22263C),
                            modifier = Modifier.size(46.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = badge.iconEmoji,
                                    fontSize = 22.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isUnlocked) Color.White else Color(0xFF8E92BC),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+${badge.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFFFB300),
                                )
                            }
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUnlocked) Color(0xFFB2BEC3) else Color(0xFF63678E),
                            )
                        }
                    }

                    if (isUnlocked) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF00FFA3),
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF484D6D),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

