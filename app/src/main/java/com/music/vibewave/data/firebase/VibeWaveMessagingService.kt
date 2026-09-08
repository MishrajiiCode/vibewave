package com.music.vibewave.data.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service for VibeWave.
 * Handles incoming push notifications sent directly by Raj Mishra (Admin)
 * and token refreshes.
 */
class VibeWaveMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received: $token")
        FirestoreManager.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"] ?: notification?.title ?: "VibeWave Alert"
        val message = data["message"] ?: data["body"] ?: notification?.body ?: ""
        val type = data["type"] ?: "GENERAL"
        val author = data["author"] ?: "Raj Mishra (Admin)"
        val isUpdate = type.equals("APP_UPDATE", ignoreCase = true) || data["is_update"] == "true"
        val id = data["id"] ?: remoteMessage.messageId ?: java.util.UUID.randomUUID().toString()

        AnnouncementManager.showRichNotification(
            context = applicationContext,
            id = id,
            title = title,
            message = message,
            type = type,
            author = author,
            isUpdate = isUpdate
        )
    }

    companion object {
        private const val TAG = "VibeWaveFCM"
    }
}
