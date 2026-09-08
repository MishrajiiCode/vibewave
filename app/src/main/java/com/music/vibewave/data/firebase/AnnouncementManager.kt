package com.music.vibewave.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.music.vibewave.MainActivity
import com.music.vibewave.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Real-time announcement and notification engine for VibeWave.
 * Listens for broadcasts and targeted personal notifications sent by Raj Mishra (Admin),
 * creates native Android system notifications with rich styling, and provides in-app alert cards.
 */
object AnnouncementManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "AnnouncementManager"
    private const val CHANNEL_ID = "vibewave_announcements"
    private const val CHANNEL_NAME = "VibeWave Announcements"
    private const val DM_CHANNEL_ID = "vibewave_direct_messages"
    private const val DM_CHANNEL_NAME = "Direct Messages"
    private const val UPDATE_CHANNEL_ID = "vibewave_updates"
    private const val UPDATE_CHANNEL_NAME = "VibeWave App Updates"
    private const val MUSIC_CHANNEL_ID = "vibewave_music_picks"
    private const val MUSIC_CHANNEL_NAME = "Music Recommendations"
    private const val GROUP_KEY = "com.music.vibewave.NOTIFICATIONS"
    private const val PREFS_NAME = "vibewave_announcement_prefs"
    private const val KEY_LAST_SEEN_TIME = "last_seen_announcement_time"

    data class Announcement(
        val id: String = UUID.randomUUID().toString(),
        val title: String = "",
        val message: String = "",
        val type: String = "GENERAL", // GENERAL, FESTIVAL_WISH, ROMANTIC_VIBES, BIRTHDAY_WISH, MUSIC_RECOMMENDATION, APP_UPDATE, PERSONAL_WISH, MOTIVATION, AI_PICKS
        val author: String = "Raj Mishra (Admin)",
        val targetUserId: String? = null,
        val targetUserEmail: String? = null,
        val targetVersion: String? = null,
        val onlyNonUpdated: Boolean = false,
        val timestamp: Long = System.currentTimeMillis(),
        val read: Boolean = false,
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "type" to type,
            "author" to author,
            "targetUserId" to targetUserId,
            "targetUserEmail" to targetUserEmail,
            "targetVersion" to targetVersion,
            "onlyNonUpdated" to onlyNonUpdated,
            "timestamp" to timestamp,
            "read" to read,
        )

        fun getEmoji(): String = when (type.uppercase()) {
            "FESTIVAL_WISH" -> "🪔"
            "ROMANTIC_VIBES" -> "💖"
            "BIRTHDAY_WISH" -> "🎂"
            "MUSIC_RECOMMENDATION" -> "🎵"
            "APP_UPDATE" -> "🚀"
            "PERSONAL_WISH" -> "💌"
            "DIRECT_MESSAGE" -> "💬"
            "MOTIVATION" -> "⚡"
            "AI_PICKS" -> "✨"
            else -> "📢"
        }
    }

    private var globalListener: ListenerRegistration? = null
    private var directListener: ListenerRegistration? = null
    private var appContext: Context? = null

    private val _latestAnnouncement = MutableStateFlow<Announcement?>(null)
    val latestAnnouncement = _latestAnnouncement.asStateFlow()

    private val _announcementsList = MutableStateFlow<List<Announcement>>(emptyList())
    val announcementsList = _announcementsList.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        createNotificationChannels(context)
        startListening()
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel: Global Announcements from Admin
            val announcementChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Festival greetings, motivation, and broadcasts from Raj Mishra (Admin)"
                enableLights(true)
                lightColor = 0xFF7C4DFF.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                group = GROUP_KEY
            }

            // Channel: 1-on-1 Direct Messages (WhatsApp style)
            val dmChannel = NotificationChannel(
                DM_CHANNEL_ID,
                DM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Personal messages from other VibeWave community users"
                enableLights(true)
                lightColor = 0xFF00BFA5.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 80, 150)
                group = GROUP_KEY
            }

            // Channel: App Updates (GitHub Releases)
            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                UPDATE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when new VibeWave releases and features are available"
                enableLights(true)
                lightColor = 0xFF00E5FF.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                group = GROUP_KEY
            }

            // Channel: AI Music Picks / Contextual Music
            val musicChannel = NotificationChannel(
                MUSIC_CHANNEL_ID,
                MUSIC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personalized music suggestions and AI-curated vibes"
                enableLights(false)
                enableVibration(false)
                group = GROUP_KEY
            }

            // Notification group for VibeWave
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannelGroup(
                    android.app.NotificationChannelGroup(GROUP_KEY, "VibeWave")
                )
            }

            manager.createNotificationChannel(announcementChannel)
            manager.createNotificationChannel(dmChannel)
            manager.createNotificationChannel(updateChannel)
            manager.createNotificationChannel(musicChannel)
        }
    }

    fun startListening() {
        val context = appContext ?: return
        val db = FirestoreManager.getFirestoreOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong(KEY_LAST_SEEN_TIME, System.currentTimeMillis() - (24 * 3600 * 1000L))

        // 1. Listen for Global Announcements (recent 24 hours or newer)
        globalListener?.remove()
        globalListener = db.collection("announcements")
            .whereGreaterThan("timestamp", lastSeenTime)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Global announcements listener error: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val announcement = Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "VibeWave Announcement",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "GENERAL",
                            author = doc.getString("author") ?: "Raj Mishra (Admin)",
                            targetVersion = doc.getString("targetVersion"),
                            onlyNonUpdated = doc.getBoolean("onlyNonUpdated") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        )

                        handleNewAnnouncement(context, announcement)
                    }
                }
            }

        // 2. Listen for Direct User Notifications whenever user registers or logs in
        scope.launch {
            FirestoreManager.currentUser.collect { user ->
                if (user != null && user.uid.isNotBlank() && user.uid != "guest") {
                    attachDirectUserListener(context, user.uid, user.email)
                }
            }
        }
    }

    private fun attachDirectUserListener(context: Context, uid: String, email: String) {
        val db = FirestoreManager.getFirestoreOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong(KEY_LAST_SEEN_TIME, System.currentTimeMillis() - (24 * 3600 * 1000L))

        directListener?.remove()
        directListener = db.collection("users")
            .document(uid)
            .collection("notifications")
            .whereGreaterThan("timestamp", lastSeenTime)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Direct notifications listener error: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val notif = Announcement(
                            id = doc.id,
                            title = doc.getString("title") ?: "Personal Message",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "PERSONAL_WISH",
                            author = doc.getString("author") ?: "Raj Mishra (Admin)",
                            targetUserId = uid,
                            targetUserEmail = email,
                            targetVersion = doc.getString("targetVersion"),
                            onlyNonUpdated = doc.getBoolean("onlyNonUpdated") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        )

                        handleNewAnnouncement(context, notif)
                    }
                }
            }
    }

    private fun handleNewAnnouncement(context: Context, announcement: Announcement) {
        // If this announcement is for app updates or non-updated users only:
        // check whether the current device is already on or past the target version.
        if (announcement.type.equals("APP_UPDATE", ignoreCase = true) || announcement.onlyNonUpdated) {
            val currentVersion = com.music.vibewave.BuildConfig.VERSION_NAME.removePrefix("v")
            val targetVersion = announcement.targetVersion
                ?: extractVersion(announcement.title)
                ?: extractVersion(announcement.message)
                ?: "1.5.5"

            if (!com.music.vibewave.data.AppUpdateChecker.isNewer(targetVersion, currentVersion)) {
                Log.d(TAG, "User already on $currentVersion >= $targetVersion. Suppressing update broadcast.")
                return
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_TIME, 0L)

        if (announcement.timestamp > lastSeen) {
            prefs.edit().putLong(KEY_LAST_SEEN_TIME, announcement.timestamp).apply()

            // Update in-app state
            _latestAnnouncement.value = announcement
            _announcementsList.value = listOf(announcement) + _announcementsList.value

            // Show Android native notification in drawer
            showRichNotification(
                context = context,
                id = announcement.id,
                title = announcement.title,
                message = announcement.message,
                type = announcement.type,
                author = announcement.author,
                isUpdate = announcement.type.equals("APP_UPDATE", ignoreCase = true),
            )
        }
    }

    private fun extractVersion(text: String): String? {
        val match = Regex("""v?(\d+\.\d+(\.\d+)?)""").find(text)
        return match?.groupValues?.get(1)
    }

    fun dismissCurrentAnnouncement() {
        _latestAnnouncement.value = null
    }

    /**
     * Shows a rich, high-visibility notification modeled after modern engagement notifications (Swiggy / Zomato).
     * Features:
     * - BigTextStyle with large readable copy and summary line
     * - Accent color (#7C4DFF vibrant purple)
     * - Large app icon
     * - Interactive action buttons ("Open VibeWave" / "Listen Now")
     * - Heads-up pop on screen (HIGH priority)
     */
    fun showRichNotification(
        context: Context,
        id: String = UUID.randomUUID().toString(),
        title: String,
        message: String,
        type: String = "GENERAL",
        author: String = "Raj Mishra (Admin)",
        isUpdate: Boolean = false,
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_announcement", true)
                putExtra("announcement_id", id)
                putExtra("announcement_type", type)
                if (isUpdate || type.equals("APP_UPDATE", ignoreCase = true)) {
                    putExtra("open_update_dialog", true)
                }
                if (type.equals("DIRECT_MESSAGE", ignoreCase = true)) {
                    putExtra("open_community", true)
                }
                if (type.equals("MUSIC_RECOMMENDATION", ignoreCase = true) || type.equals("AI_PICKS", ignoreCase = true)) {
                    putExtra("open_ai_picks", true)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val emoji = when (type.uppercase()) {
                "FESTIVAL_WISH" -> "🎊"
                "BIRTHDAY_WISH" -> "🎂"
                "MUSIC_RECOMMENDATION" -> "🎵"
                "AI_PICKS" -> "✨"
                "APP_UPDATE" -> "🚀"
                "PERSONAL_WISH" -> "💖"
                "DIRECT_MESSAGE" -> "💬"
                else -> "📢"
            }

            val channelId = when {
                isUpdate || type.uppercase() == "APP_UPDATE" -> UPDATE_CHANNEL_ID
                type.uppercase() == "DIRECT_MESSAGE" -> DM_CHANNEL_ID
                type.uppercase() == "MUSIC_RECOMMENDATION" || type.uppercase() == "AI_PICKS" -> MUSIC_CHANNEL_ID
                else -> CHANNEL_ID
            }
            val formattedTitle = "$emoji $title"
            val summaryText = if (isUpdate) "VibeWave • Update Available" else "VibeWave • $author"

            // Decode large icon
            val largeIcon = try {
                android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            } catch (t: Throwable) {
                null
            }

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle(formattedTitle)
                .bigText(message)
                .setSummaryText(summaryText)

            val actionButtonTitle = when (type.uppercase()) {
                "APP_UPDATE" -> "Update Now 🚀"
                "DIRECT_MESSAGE" -> "Reply Now 💬"
                "MUSIC_RECOMMENDATION", "AI_PICKS" -> "Play AI Picks 🎵"
                else -> "Open VibeWave ✨"
            }

            val actionIntent = PendingIntent.getActivity(
                context,
                id.hashCode() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .apply {
                    if (largeIcon != null) setLargeIcon(largeIcon)
                }
                .setContentTitle(formattedTitle)
                .setContentText(message)
                .setSubText(when {
                    isUpdate || type.uppercase() == "APP_UPDATE" -> "Update Available"
                    type.uppercase() == "DIRECT_MESSAGE" -> "New Message"
                    else -> "VibeWave"
                })
                .setGroup(GROUP_KEY)
                .setStyle(bigTextStyle)
                .setColor(0xFF7C4DFF.toInt())
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_send,
                    actionButtonTitle,
                    actionIntent
                )

            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify(id.hashCode(), builder.build())
                Log.d(TAG, "Rich notification dispatched: $formattedTitle")
            } else {
                Log.w(TAG, "Notifications disabled for this app on system")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted: ${e.message}")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to display rich notification: ${e.message}")
        }
    }
}
