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
 * Background worker that runs even when VibeWave is completely closed or killed.
 * Modeled after modern delivery & messaging apps (WhatsApp / Zomato):
 * 1. Syncs unread global announcements from Raj Mishra (Admin).
 * 2. Checks for unread 1-on-1 private chat messages for the current user.
 * 3. Delivers contextual Zomato-style music picks (morning, midday, evening, late-night)
 *    to keep the listener engaged with personalized AI tracks.
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
            val lastSeenAnnouncement = prefs.getLong(KEY_LAST_SEEN_ANNOUNCEMENT, System.currentTimeMillis() - (12 * 3600 * 1000L))
            val lastMusicNotifTime = prefs.getLong(KEY_LAST_MUSIC_NOTIF, 0L)
            val lastSeenPrivateMsgTime = prefs.getLong(KEY_LAST_SEEN_PRIVATE_MSG, System.currentTimeMillis() - (12 * 3600 * 1000L))
            val now = System.currentTimeMillis()

            val db = FirestoreManager.getFirestoreOrNull()

            // 1. Check for unread Global Announcements
            if (db != null) {
                runCatching {
                    val announcementsSnapshot = db.collection("announcements")
                        .whereGreaterThan("timestamp", lastSeenAnnouncement)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .await()

                    var newestAnnouncement = lastSeenAnnouncement
                    for (doc in announcementsSnapshot.documents) {
                        val ts = doc.getLong("timestamp") ?: now
                        if (ts > newestAnnouncement) newestAnnouncement = ts

                        val type = doc.getString("type") ?: "GENERAL"
                        val onlyNonUpdated = doc.getBoolean("onlyNonUpdated") ?: false
                        val targetVersion = doc.getString("targetVersion")

                        // Skip update announcements if already running target version
                        if (type.equals("APP_UPDATE", ignoreCase = true) || onlyNonUpdated) {
                            val currentVersion = com.music.vibewave.BuildConfig.VERSION_NAME.removePrefix("v")
                            val needed = targetVersion ?: "1.5.8"
                            if (!AppUpdateChecker.isNewer(needed, currentVersion)) {
                                continue
                            }
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
                    prefs.edit().putLong(KEY_LAST_SEEN_ANNOUNCEMENT, newestAnnouncement).apply()
                }.onFailure {
                    Log.w(TAG, "Global announcement check failed: ${it.message}")
                }
            }

            // 2. Check for unread 1-on-1 private messages (WhatsApp style)
            if (db != null) {
                runCatching {
                    val myUid = FirestoreManager.currentUser.value?.uid?.takeIf { it.isNotBlank() && it != "guest" }
                        ?: prefs.getString("cached_user_uid", null)

                    if (!myUid.isNullOrBlank()) {
                        val messagesSnapshot = db.collection("private_messages")
                            .whereEqualTo("receiverId", myUid)
                            .whereEqualTo("read", false)
                            .whereGreaterThan("timestamp", lastSeenPrivateMsgTime)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(5)
                            .get()
                            .await()

                        var newestMsgTime = lastSeenPrivateMsgTime
                        for (doc in messagesSnapshot.documents) {
                            val ts = doc.getLong("timestamp") ?: now
                            if (ts > newestMsgTime) newestMsgTime = ts

                            val senderName = doc.getString("senderName") ?: "A Community Member"
                            val messageSnippet = doc.getString("message") ?: "Sent you a message"

                            AnnouncementManager.showRichNotification(
                                context = context,
                                id = doc.id,
                                title = "💬 $senderName",
                                message = messageSnippet,
                                type = "DIRECT_MESSAGE",
                                author = senderName,
                                isUpdate = false,
                            )
                        }
                        prefs.edit().putLong(KEY_LAST_SEEN_PRIVATE_MSG, newestMsgTime).apply()
                    }
                }.onFailure {
                    Log.w(TAG, "Private message check failed: ${it.message}")
                }
            }

            // 3. Zomato-style contextual music recommendation (Morning, Afternoon, Evening, Night)
            // Triggered if at least 6 hours have passed since the last music prompt
            val sixHoursMs = 6 * 3600 * 1000L
            if (now - lastMusicNotifTime >= sixHoursMs) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val (title, message) = when (hour) {
                    in 6..11 -> Pair(
                        "Morning Coffee & Acoustic Vibe ☕",
                        "Start your morning with serene acoustic melodies. AI tuned your morning flow on VibeWave 🎵"
                    )
                    in 12..16 -> Pair(
                        "Afternoon Energy & Focus Boost ⚡",
                        "Power through your afternoon! Your AI Smart Mix has fresh upbeat rhythms ready for you 🎧"
                    )
                    in 17..21 -> Pair(
                        "Sunset Acoustic Chill 🌆",
                        "Unwind after a long day. Dim the lights and sink into your AI personalized evening vibe ✨"
                    )
                    else -> Pair(
                        "Late Night Lo-Fi & Soul 🌙",
                        "Deep night reflections. Gentle, soothing lo-fi and melodic tracks curated for your quiet hours 🌌"
                    )
                }

                AnnouncementManager.showRichNotification(
                    context = context,
                    id = "zomato_music_vibe_${now / (1000 * 3600 * 6)}",
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
        private const val PREFS_NAME = "vibewave_bg_sync_prefs"
        private const val KEY_LAST_SEEN_ANNOUNCEMENT = "last_seen_announcement_ts"
        private const val KEY_LAST_SEEN_PRIVATE_MSG = "last_seen_private_msg_ts"
        private const val KEY_LAST_MUSIC_NOTIF = "last_music_notif_ts"

        /**
         * Schedules periodic background sync every 15 minutes.
         * Runs even if the application is killed or swiped away.
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
                Log.d(TAG, "NotificationSyncWorker successfully scheduled (15 min interval)")
            }.onFailure {
                Log.w(TAG, "Failed to schedule NotificationSyncWorker: ${it.message}")
            }
        }
    }
}
