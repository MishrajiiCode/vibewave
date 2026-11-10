package com.music.vibewave.data.gamification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.music.vibewave.data.firebase.FirestoreManager
import com.music.vibewave.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Gamification & Engagement Engine for VibeWave.
 * Manages Vibe XP, leveling progression, daily listening streaks,
 * unlockable trophies/badges, and real-time weekly leaderboards.
 */
object VibeRewardsManager {

    private const val TAG = "VibeRewardsManager"
    private const val PREFS_NAME = "vibewave_rewards_prefs"
    private const val COLLECTION_REWARDS = "user_rewards"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    data class VibeBadge(
        val id: String,
        val title: String,
        val description: String,
        val iconEmoji: String,
        val xpReward: Long,
    )

    val ALL_BADGES = listOf(
        VibeBadge("first_note", "First Note", "Play your very first track on VibeWave", "🎵", 50),
        VibeBadge("streak_3", "Spark of Rhythm", "Reach a 3-day listening streak", "✨", 100),
        VibeBadge("streak_7", "Week On Fire", "Listen every day for 7 consecutive days", "🔥", 250),
        VibeBadge("night_owl", "Night Owl", "Listen to tunes late at night (12 AM - 4 AM)", "🌙", 75),
        VibeBadge("together_vibes", "Better Together", "Join or host a live Listen Together party", "🤝", 100),
        VibeBadge("audiophile", "Pure Acoustic", "Stream high-fidelity music with the equalizer tuned", "🎛️", 100),
        VibeBadge("centurion", "Music Marathoner", "Play over 100 complete songs", "🚀", 500),
        VibeBadge("dj_master", "Master DJ", "Host a room with friends listening in sync", "👑", 200),
    )

    data class RewardProfile(
        val uid: String = "",
        val displayName: String = "Viber",
        val totalXp: Long = 0L,
        val weeklyXp: Long = 0L,
        val currentWeekKey: String = "",
        val level: Int = 1,
        val levelTitle: String = "Acoustic Novice",
        val currentLevelProgress: Float = 0f,
        val nextLevelXp: Long = 100L,
        val streakDays: Int = 1,
        val lastStreakDate: String = "",
        val songsPlayedCount: Int = 0,
        val unlockedBadgeIds: Set<String> = emptySet(),
    )

    data class LeaderboardEntry(
        val rank: Int,
        val uid: String,
        val displayName: String,
        val weeklyXp: Long,
        val level: Int,
        val levelTitle: String,
        val isCurrentUser: Boolean = false,
    )

    private val _profile = MutableStateFlow(RewardProfile())
    val profile = _profile.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    private val _levelUpEvent = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 1)
    val levelUpEvent = _levelUpEvent.asSharedFlow()

    private val _badgeUnlockedEvent = MutableSharedFlow<VibeBadge>(extraBufferCapacity = 1)
    val badgeUnlockedEvent = _badgeUnlockedEvent.asSharedFlow()

    private var leaderboardListener: ListenerRegistration? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromLocal()
        checkWeeklyReset()
        checkDailyStreak()
        startLeaderboardSync()
    }

    private fun getCurrentWeekKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W$week"
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun loadFromLocal() {
        val p = prefs ?: return
        val totalXp = p.getLong("total_xp", 0L)
        val weeklyXp = p.getLong("weekly_xp", 0L)
        val weekKey = p.getString("week_key", getCurrentWeekKey()) ?: getCurrentWeekKey()
        val streakDays = p.getInt("streak_days", 1)
        val lastStreakDate = p.getString("last_streak_date", "") ?: ""
        val songsPlayed = p.getInt("songs_played", 0)
        val badges = p.getStringSet("unlocked_badges", emptySet()) ?: emptySet()

        val uid = FirestoreManager.currentUser.value?.uid?.ifBlank { null }
            ?: p.getString("my_uid", UUID.randomUUID().toString().take(8))!!
        val name = FirestoreManager.currentUser.value?.name?.ifBlank { null }
            ?: p.getString("my_name", "Viber")!!

        p.edit().putString("my_uid", uid).putString("my_name", name).apply()

        updateProfileState(
            uid = uid,
            name = name,
            totalXp = totalXp,
            weeklyXp = weeklyXp,
            weekKey = weekKey,
            streakDays = streakDays,
            lastStreakDate = lastStreakDate,
            songsPlayed = songsPlayed,
            badges = badges,
        )
    }

    private fun saveToLocal() {
        val p = prefs ?: return
        val prof = _profile.value
        p.edit()
            .putLong("total_xp", prof.totalXp)
            .putLong("weekly_xp", prof.weeklyXp)
            .putString("week_key", prof.currentWeekKey)
            .putInt("streak_days", prof.streakDays)
            .putString("last_streak_date", prof.lastStreakDate)
            .putInt("songs_played", prof.songsPlayedCount)
            .putStringSet("unlocked_badges", prof.unlockedBadgeIds)
            .apply()
    }

    private fun calculateLevel(totalXp: Long): Pair<Int, String> {
        val level = when {
            totalXp < 100 -> 1
            totalXp < 250 -> 2
            totalXp < 500 -> 3
            totalXp < 900 -> 4
            totalXp < 1500 -> 5
            totalXp < 2400 -> 6
            totalXp < 3600 -> 7
            totalXp < 5200 -> 8
            totalXp < 7200 -> 9
            totalXp < 10000 -> 10
            totalXp < 15000 -> 12
            totalXp < 22000 -> 15
            totalXp < 35000 -> 20
            else -> 25
        }

        val title = when (level) {
            1, 2 -> "Acoustic Novice"
            3, 4 -> "Sound Explorer"
            5, 6, 7 -> "Rhythm Rider"
            8, 9, 10 -> "Vibe Master"
            11, 12, 13, 14, 15 -> "Sonic Alchemist"
            16, 17, 18, 19, 20 -> "Harmony Legend"
            else -> "Cosmic Audiophile"
        }
        return level to title
    }

    private fun getNextLevelThreshold(level: Int): Long {
        return when (level) {
            1 -> 100L
            2 -> 250L
            3 -> 500L
            4 -> 900L
            5 -> 1500L
            6 -> 2400L
            7 -> 3600L
            8 -> 5200L
            9 -> 7200L
            10 -> 10000L
            12 -> 15000L
            15 -> 22000L
            20 -> 35000L
            else -> 50000L
        }
    }

    private fun updateProfileState(
        uid: String,
        name: String,
        totalXp: Long,
        weeklyXp: Long,
        weekKey: String,
        streakDays: Int,
        lastStreakDate: String,
        songsPlayed: Int,
        badges: Set<String>,
    ) {
        val (level, title) = calculateLevel(totalXp)
        val nextLevelXp = getNextLevelThreshold(level)
        val prevLevelXp = if (level > 1) getNextLevelThreshold(level - 1) else 0L
        val progress = if (nextLevelXp > prevLevelXp) {
            ((totalXp - prevLevelXp).toFloat() / (nextLevelXp - prevLevelXp).toFloat()).coerceIn(0f, 1f)
        } else 1f

        _profile.value = RewardProfile(
            uid = uid,
            displayName = name,
            totalXp = totalXp,
            weeklyXp = weeklyXp,
            currentWeekKey = weekKey,
            level = level,
            levelTitle = title,
            currentLevelProgress = progress,
            nextLevelXp = nextLevelXp,
            streakDays = streakDays,
            lastStreakDate = lastStreakDate,
            songsPlayedCount = songsPlayed,
            unlockedBadgeIds = badges,
        )
    }

    private fun checkWeeklyReset() {
        val currentWeek = getCurrentWeekKey()
        val prof = _profile.value
        if (prof.currentWeekKey != currentWeek) {
            _profile.value = prof.copy(weeklyXp = 0L, currentWeekKey = currentWeek)
            saveToLocal()
            syncToFirestore()
        }
    }

    private fun checkDailyStreak() {
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val prof = _profile.value

        when (prof.lastStreakDate) {
            today -> {
                // Already counted for today
            }
            yesterday -> {
                // Streak continued!
                val newStreak = prof.streakDays + 1
                addXp(50, "Daily Listening Streak: Day $newStreak 🔥")
                _profile.value = _profile.value.copy(
                    streakDays = newStreak,
                    lastStreakDate = today,
                )
                saveToLocal()
                syncToFirestore()
                if (newStreak >= 3) unlockBadge("streak_3")
                if (newStreak >= 7) unlockBadge("streak_7")
            }
            else -> {
                // Streak reset
                addXp(50, "Daily Listening Streak Started! 🔥")
                _profile.value = _profile.value.copy(
                    streakDays = 1,
                    lastStreakDate = today,
                )
                saveToLocal()
                syncToFirestore()
            }
        }
    }

    fun addXp(amount: Long, reason: String) {
        if (amount <= 0) return
        val current = _profile.value
        val oldLevel = current.level
        val newTotal = current.totalXp + amount
        val newWeekly = current.weeklyXp + amount

        updateProfileState(
            uid = current.uid,
            name = current.displayName,
            totalXp = newTotal,
            weeklyXp = newWeekly,
            weekKey = current.currentWeekKey,
            streakDays = current.streakDays,
            lastStreakDate = current.lastStreakDate,
            songsPlayed = current.songsPlayedCount,
            badges = current.unlockedBadgeIds,
        )

        saveToLocal()
        syncToFirestore()

        val newLevel = _profile.value.level
        if (newLevel > oldLevel) {
            _levelUpEvent.tryEmit(newLevel to _profile.value.levelTitle)
        }
    }

    fun onSongStarted(song: Song) {
        val prof = _profile.value
        val newCount = prof.songsPlayedCount + 1
        _profile.value = prof.copy(songsPlayedCount = newCount)
        addXp(10, "Song Played: ${song.title}")

        unlockBadge("first_note")
        if (newCount >= 100) unlockBadge("centurion")

        // Check Night Owl (between midnight and 4 AM)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 0..4) {
            unlockBadge("night_owl")
        }
    }

    fun onSongCompleted(song: Song) {
        addXp(15, "Full Song Completed: ${song.title}")
    }

    fun onListenTogetherJoined() {
        addXp(30, "Listen Together Party 🤝")
        unlockBadge("together_vibes")
    }

    fun onListenTogetherHosted() {
        addXp(40, "Room Hosted 👑")
        unlockBadge("together_vibes")
        unlockBadge("dj_master")
    }

    fun onEqualizerAdjusted() {
        unlockBadge("audiophile")
    }

    fun unlockBadge(badgeId: String) {
        val current = _profile.value
        if (current.unlockedBadgeIds.contains(badgeId)) return

        val badge = ALL_BADGES.find { it.id == badgeId } ?: return
        val updated = current.unlockedBadgeIds + badgeId
        _profile.value = current.copy(unlockedBadgeIds = updated)

        saveToLocal()
        addXp(badge.xpReward, "Unlocked Achievement: ${badge.title}")
        _badgeUnlockedEvent.tryEmit(badge)
    }

    private fun syncToFirestore() {
        val prof = _profile.value
        if (prof.uid.isBlank()) return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val data = mapOf(
                    "uid" to prof.uid,
                    "displayName" to (FirestoreManager.currentUser.value?.name ?: prof.displayName),
                    "totalXp" to prof.totalXp,
                    "weeklyXp" to prof.weeklyXp,
                    "weekKey" to prof.currentWeekKey,
                    "level" to prof.level,
                    "levelTitle" to prof.levelTitle,
                    "streakDays" to prof.streakDays,
                    "songsPlayedCount" to prof.songsPlayedCount,
                    "unlockedBadgeIds" to prof.unlockedBadgeIds.toList(),
                    "updatedAt" to System.currentTimeMillis(),
                )

                db.collection(COLLECTION_REWARDS).document(prof.uid)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Sync rewards error", e)
            }
        }
    }

    fun startLeaderboardSync() {
        if (leaderboardListener != null) return
        val db = FirestoreManager.getFirestoreOrNull() ?: return

        leaderboardListener = db.collection(COLLECTION_REWARDS)
            .orderBy("weeklyXp", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Leaderboard sync failed", error)
                    return@addSnapshotListener
                }

                val myUid = _profile.value.uid
                val entries = snapshot?.documents?.mapIndexedNotNull { index, doc ->
                    runCatching {
                        val uid = doc.getString("uid") ?: doc.id
                        val name = doc.getString("displayName") ?: "Viber"
                        val weeklyXp = doc.getLong("weeklyXp") ?: 0L
                        val level = (doc.getLong("level") ?: 1L).toInt()
                        val title = doc.getString("levelTitle") ?: "Acoustic Novice"

                        LeaderboardEntry(
                            rank = index + 1,
                            uid = uid,
                            displayName = name,
                            weeklyXp = weeklyXp,
                            level = level,
                            levelTitle = title,
                            isCurrentUser = uid == myUid,
                        )
                    }.getOrNull()
                } ?: emptyList()

                _leaderboard.value = entries
            }
    }
}

