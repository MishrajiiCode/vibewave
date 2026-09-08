package com.music.vibewave.data.firebase

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.Query
import com.music.vibewave.data.AppUpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background worker that runs even when VibeWave is completely closed, killed, or swiped away.
 *
 * This is the WorkManager-backed fallback polling system for devices/OEMs that kill
 * background processes aggressively (Xiaomi, Huawei, Samsung One UI in battery saver).
 * FCM push is the primary mechanism; this guarantees delivery even when FCM is throttled.
 *
 * Covers all three notification types:
 *   1. Admin Global Announcements → polls `announcements` Firestore collection
 *   2. Direct User Messages (DM)  → polls `private_messages` where receiverId = current user
 *   3. App Updates (GitHub)       → checks GitHub Releases API for a newer version
 *   4. Contextual Music Picks     → Zomato-style time-aware music recommendation
 *
 * Scheduled every 15 minutes via WorkManager (minimum interval allowed by Android).
 * Survives device reboots via [BootReceiver].
 */
class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        Log.d(TAG, "NotificationSyncWorker waking up for background sync...")

        runCatching {
            // Ensure notification channels exist
            AnnouncementManager.init(context)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastSeenAnnouncement = prefs.getLong(KEY_LAST_SEEN_ANNOUNCEMENT, now - TWELVE_HOURS_MS)
            val lastMusicNotifTime = prefs.getLong(KEY_LAST_MUSIC_NOTIF, 0L)
            val lastSeenPrivateMsgTime = prefs.getLong(KEY_LAST_SEEN_PRIVATE_MSG, now - TWELVE_HOURS_MS)
            val lastUpdateCheckTime = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)

            val db = FirestoreManager.getFirestoreOrNull()

            // ── 1. App Update Check (GitHub Releases API) ──────────────────────────────────
            // Check every 6 hours max to avoid rate limiting GitHub API
            if (now - lastUpdateCheckTime >= SIX_HOURS_MS) {
                runCatching {
                    Log.d(TAG, "Checking GitHub for app updates...")
                    AppUpdateChecker.check(context)
                    prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, now).apply()
                }.onFailure {
                    Log.w(TAG, "GitHub update check failed: ${it.message}")
                }
            }

            // ── 2. Admin Global Announcements ──────────────────────────────────────────────
            if (db != null) {
                runCatching {
                    val announcementsSnapshot = db.collection("announcements")
                        .whereGreaterThan("timestamp", lastSeenAnnouncement)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .await()

                    var newestTs = lastSeenAnnouncement
                    for (doc in announcementsSnapshot.documents) {
                        val ts = doc.getLong("timestamp") ?: now
                        if (ts > newestTs) newestTs = ts

                        val type = doc.getString("type") ?: "GENERAL"
                        val onlyNonUpdated = doc.getBoolean("onlyNonUpdated") ?: false
                        val targetVersion = doc.getString("targetVersion")

                        // Skip update announcements if device is already on the target version
                        if (type.equals("APP_UPDATE", ignoreCase = true) || onlyNonUpdated) {
                            val currentVersion = com.music.vibewave.BuildConfig.VERSION_NAME.removePrefix("v")
                            val needed = targetVersion ?: "1.5.8"
                            if (!AppUpdateChecker.isNewer(needed, currentVersion)) continue
                        }

                        AnnouncementManager.showRichNotification(
                            context = context,
                            id = doc.id,
                            title = doc.getString("title") ?: "VibeWave Alert",
                            message = doc.getString("message") ?: "",
                            type = type,
                            author = doc.getString("author") ?: "Raj Mishra (Admin)",
                            isUpdate = type.equals("APP_UPDATE", ignoreCase = true),
                        )
                    }
                    prefs.edit().putLong(KEY_LAST_SEEN_ANNOUNCEMENT, newestTs).apply()
                }.onFailure {
                    Log.w(TAG, "Global announcement check failed: ${it.message}")
                }
            }

            // ── 3. 1-on-1 Direct Messages (WhatsApp style) ───────────────────────────────
            // Query `private_messages` where receiverId matches this user and message is unread.
            // NOTE: Uses only a single `where` clause + simple `orderBy` to avoid needing a
            // composite Firestore index (which would require Firebase Console setup).
            if (db != null) {
                runCatching {
                    val myUid = FirestoreManager.currentUser.value?.uid
                        ?.takeIf { it.isNotBlank() && it != "guest" }
                        ?: prefs.getString(KEY_CACHED_UID, null)

                    if (!myUid.isNullOrBlank()) {
                        // Simple query — only filter by receiverId to avoid composite index requirement
                        val messagesSnapshot = db.collection("private_messages")
                            .whereEqualTo("receiverId", myUid)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(10)
                            .get()
                            .await()

                        var newestMsgTime = lastSeenPrivateMsgTime
                        var shownCount = 0

                        for (doc in messagesSnapshot.documents) {
                            val ts = doc.getLong("timestamp") ?: now
                            // Only show messages newer than the last seen timestamp
                            if (ts <= lastSeenPrivateMsgTime) continue
                            // Don't spam too many at once
                            if (shownCount >= 3) break

                            if (ts > newestMsgTime) newestMsgTime = ts

                            val senderName = doc.getString("senderName") ?: "A Community Member"
                            val messageSnippet = (doc.getString("message") ?: "Sent you a message").take(120)

                            AnnouncementManager.showRichNotification(
                                context = context,
                                id = doc.id,
                                title = "💬 $senderName",
                                message = messageSnippet,
                                type = "DIRECT_MESSAGE",
                                author = senderName,
                                isUpdate = false,
                            )
                            shownCount++
                        }
                        prefs.edit().putLong(KEY_LAST_SEEN_PRIVATE_MSG, newestMsgTime).apply()
                    }
                }.onFailure {
                    Log.w(TAG, "Private message check failed: ${it.message}")
                }
            }

            // ── 4. Contextual Zomato-Style Music Recommendation ───────────────────────────
            // Fires every 6–8 hours, time-of-day aware
            if (now - lastMusicNotifTime >= SIX_HOURS_MS) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val (title, message) = when (hour) {
                    in 6..11 -> Pair(
                        "Morning Coffee & Acoustic Vibe ☕",
                        "Start your morning with serene acoustic melodies. Your AI has tuned the perfect morning flow on VibeWave 🎵"
                    )
                    in 12..16 -> Pair(
                        "Afternoon Energy & Focus Boost ⚡",
                        "Power through your afternoon! Your AI Smart Mix has fresh upbeat rhythms ready for you 🎧"
                    )
                    in 17..21 -> Pair(
                        "Sunset Acoustic Chill 🌆",
                        "Unwind after a long day. Dim the lights and sink into your AI-personalized evening vibe ✨"
                    )
                    else -> Pair(
                        "Late Night Lo-Fi & Soul 🌙",
                        "Deep night reflections. Gentle, soothing lo-fi and melodic tracks curated for your quiet hours 🌌"
                    )
                }

                AnnouncementManager.showRichNotification(
                    context = context,
                    id = "music_vibe_${now / (SIX_HOURS_MS)}",
                    title = title,
                    message = message,
                    type = "MUSIC_RECOMMENDATION",
                    author = "AI Neural Music Engine",
                    isUpdate = false,
                )
                prefs.edit().putLong(KEY_LAST_MUSIC_NOTIF, now).apply()
            }
        }

        Result.success()
    }

    companion object {
        private const val TAG = "NotificationSyncWorker"
        private const val WORK_NAME = "vibewave_bg_notification_sync"
        const val PREFS_NAME = "vibewave_bg_sync_prefs"
        private const val KEY_LAST_SEEN_ANNOUNCEMENT = "last_seen_announcement_ts"
        private const val KEY_LAST_SEEN_PRIVATE_MSG = "last_seen_private_msg_ts"
        private const val KEY_LAST_MUSIC_NOTIF = "last_music_notif_ts"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check_ts"
        const val KEY_CACHED_UID = "cached_user_uid"
        private val SIX_HOURS_MS = 6 * 3600 * 1000L
        private val TWELVE_HOURS_MS = 12 * 3600 * 1000L

        /**
         * Schedules periodic background sync every 15 minutes.
         * [ExistingPeriodicWorkPolicy.KEEP] ensures only one instance runs at a time.
         * Guaranteed to resume after device reboot via [BootReceiver].
         */
        fun schedule(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest,
                )
                Log.d(TAG, "NotificationSyncWorker scheduled (15 min interval, network required)")
            }.onFailure {
                Log.w(TAG, "Failed to schedule NotificationSyncWorker: ${it.message}")
            }
        }
    }
}
