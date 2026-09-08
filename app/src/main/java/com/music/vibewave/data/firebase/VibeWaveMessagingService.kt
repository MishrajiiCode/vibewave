package com.music.vibewave.data.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service for VibeWave.
 *
 * This service is the ONLY entry point that works when the app is completely
 * killed or in the background. Android/FCM starts this class as a lightweight
 * service process to deliver the message, without launching MainActivity.
 *
 * Handles all three critical closed-app notification types:
 *   1. Admin Announcements  → sent by Raj Mishra to all users / specific topics
 *   2. Direct Messages (DM) → sent when User A sends a message to User B in private chat
 *   3. App Update Alerts    → triggered when a new GitHub release is published
 *
 * FCM Behavior:
 *   - If the app is IN FOREGROUND → [onMessageReceived] is always called.
 *   - If the app is IN BACKGROUND or KILLED:
 *       * "notification" payloads are shown by the FCM SDK automatically (no code needed)
 *       * "data-only" payloads wake this service (requires HIGH_PRIORITY or data-only message)
 *       * Both modes are handled here.
 *
 * To send targeted DMs or admin messages to specific users from a backend/Admin dashboard,
 * use the FCM Admin SDK or Firebase Console with:
 *     { "to": "<user_fcm_token>",
 *       "data": { "type": "DIRECT_MESSAGE", "title": "...", "message": "...", "senderName": "..." }
 *     }
 *
 * The user's FCM token is saved to Firestore in [FirestoreManager.saveFcmToken] whenever
 * [onNewToken] fires (device boot, app reinstall, FCM token rotation).
 */
class VibeWaveMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received, saving to Firestore...")
        // Save to Firestore so the backend/admin can target this specific device
        FirestoreManager.saveFcmToken(token)
        // Also cache locally for offline resilience
        applicationContext
            .getSharedPreferences("vibewave_bg_sync_prefs", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        // FCM delivers either a "notification" block, a "data" block, or both.
        // We always read data first (higher priority, more fields), then fall back to notification.
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: "GENERAL"
        val title = data["title"] ?: notification?.title ?: defaultTitle(type)
        val message = data["message"] ?: data["body"] ?: notification?.body ?: ""
        val id = data["id"] ?: remoteMessage.messageId ?: java.util.UUID.randomUUID().toString()

        when (type.uppercase()) {
            "DIRECT_MESSAGE" -> {
                // User-to-user private chat message
                val senderName = data["senderName"] ?: data["author"] ?: "Someone"
                val snippet = message.take(120).ifBlank { "Sent you a message" }
                AnnouncementManager.showRichNotification(
                    context = applicationContext,
                    id = id,
                    title = "💬 $senderName",
                    message = snippet,
                    type = "DIRECT_MESSAGE",
                    author = senderName,
                    isUpdate = false,
                )
            }
            "APP_UPDATE" -> {
                // New GitHub release notification
                val version = data["version"] ?: data["targetVersion"] ?: ""
                val notifTitle = if (version.isNotBlank()) "🚀 VibeWave v$version is Available!" else title
                AnnouncementManager.showRichNotification(
                    context = applicationContext,
                    id = id,
                    title = notifTitle,
                    message = message.ifBlank { "A new version of VibeWave is ready to download. Tap to update now!" },
                    type = "APP_UPDATE",
                    author = data["author"] ?: "VibeWave",
                    isUpdate = true,
                )
            }
            "MUSIC_RECOMMENDATION", "AI_PICKS" -> {
                AnnouncementManager.showRichNotification(
                    context = applicationContext,
                    id = id,
                    title = title,
                    message = message,
                    type = type,
                    author = data["author"] ?: "AI Neural Music Engine",
                    isUpdate = false,
                )
            }
            else -> {
                // General admin announcement (FESTIVAL_WISH, BIRTHDAY_WISH, MOTIVATION, PERSONAL_WISH, etc.)
                val author = data["author"] ?: "Raj Mishra (Admin)"
                val isUpdate = data["is_update"] == "true"
                AnnouncementManager.showRichNotification(
                    context = applicationContext,
                    id = id,
                    title = title,
                    message = message,
                    type = type,
                    author = author,
                    isUpdate = isUpdate,
                )
            }
        }
    }

    private fun defaultTitle(type: String): String = when (type.uppercase()) {
        "DIRECT_MESSAGE" -> "New Message"
        "APP_UPDATE" -> "VibeWave Update Available 🚀"
        "MUSIC_RECOMMENDATION", "AI_PICKS" -> "Your AI Music Pick ✨"
        "FESTIVAL_WISH" -> "Festival Greetings 🎊"
        "BIRTHDAY_WISH" -> "Birthday Surprise 🎂"
        "MOTIVATION" -> "Daily Motivation ⚡"
        else -> "VibeWave Alert"
    }

    companion object {
        private const val TAG = "VibeWaveFCM"
    }
}
