package com.music.bitchord.data.firebase

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
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Real-time announcement and notification engine for VibeWave.
 * Listens for broadcasts and targeted personal notifications sent by Raj Mishra (Admin),
 * creates native Android system notifications with rich styling, and provides in-app alert cards.
 */
object AnnouncementManager {

    private const val TAG = "AnnouncementManager"
    private const val CHANNEL_ID = "vibewave_announcements"
    private const val CHANNEL_NAME = "VibeWave Announcements & Updates"
    private const val PREFS_NAME = "vibewave_announcement_prefs"
    private const val KEY_LAST_SEEN_TIME = "last_seen_announcement_time"

    data class Announcement(
        val id: String = UUID.randomUUID().toString(),
        val title: String = "",
        val message: String = "",
        val type: String = "GENERAL", // GENERAL, FESTIVAL_WISH, BIRTHDAY_WISH, MUSIC_RECOMMENDATION, APP_UPDATE, PERSONAL_WISH
        val author: String = "Raj Mishra (Admin)",
        val targetUserId: String? = null,
        val targetUserEmail: String? = null,
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
            "timestamp" to timestamp,
            "read" to read,
        )

        fun getEmoji(): String = when (type.uppercase()) {
            "FESTIVAL_WISH" -> "🎊"
            "BIRTHDAY_WISH" -> "🎂"
            "MUSIC_RECOMMENDATION" -> "🎵"
            "APP_UPDATE" -> "🚀"
            "PERSONAL_WISH" -> "💖"
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
        createNotificationChannel(context)
        startListening()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct updates, festival wishes, and music alerts from Raj Mishra (Admin)"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
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
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        )

                        handleNewAnnouncement(context, announcement)
                    }
                }
            }

        // 2. Listen for Direct User Notifications if registered
        val user = FirestoreManager.currentUser.value
        if (user != null && user.uid.isNotBlank() && user.uid != "guest") {
            directListener?.remove()
            directListener = db.collection("users")
                .document(user.uid)
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
                                targetUserId = user.uid,
                                targetUserEmail = user.email,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            )

                            handleNewAnnouncement(context, notif)
                        }
                    }
                }
        }
    }

    private fun handleNewAnnouncement(context: Context, announcement: Announcement) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_TIME, 0L)

        if (announcement.timestamp > lastSeen) {
            prefs.edit().putLong(KEY_LAST_SEEN_TIME, announcement.timestamp).apply()

            // Update in-app state
            _latestAnnouncement.value = announcement
            _announcementsList.value = listOf(announcement) + _announcementsList.value

            // Show Android native notification in drawer
            showSystemNotification(context, announcement)
        }
    }

    fun dismissCurrentAnnouncement() {
        _latestAnnouncement.value = null
    }

    private fun showSystemNotification(context: Context, announcement: Announcement) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_announcement", true)
                putExtra("announcement_id", announcement.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                announcement.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val emoji = announcement.getEmoji()
            val formattedTitle = "$emoji ${announcement.title}"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(formattedTitle)
                .setContentText(announcement.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(announcement.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                manager.notify(announcement.id.hashCode(), builder.build())
                Log.d(TAG, "Notification delivered: $formattedTitle")
            } else {
                Log.w(TAG, "Notification permission disabled on device")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted: ${e.message}")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to display notification: ${e.message}")
        }
    }
}
