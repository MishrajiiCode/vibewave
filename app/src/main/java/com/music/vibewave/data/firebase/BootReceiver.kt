package com.music.vibewave.data.firebase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Ensures background notification polling and FCM subscriptions are restored
 * whenever the user's phone restarts or when the app is updated/reinstalled.
 *
 * Why this is critical for closed-app notifications:
 * - Android kills WorkManager scheduled workers on device reboot
 * - FCM subscriptions can expire or need re-registration after reboot
 * - Without this receiver, notifications would stop working after a phone restart
 *   until the user manually reopens the app
 *
 * Handles both clean reboots (BOOT_COMPLETED) and Huawei/OnePlus/Xiaomi quick boots
 * (QUICKBOOT_POWERON), ensuring the sync runs on all major OEMs.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver triggered with action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {

                // 1. Reschedule background notification sync worker
                NotificationSyncWorker.schedule(context)

                // 2. Ensure FCM global topic subscriptions are active
                runCatching {
                    val messaging = FirebaseMessaging.getInstance()
                    messaging.subscribeToTopic("announcements")
                    messaging.subscribeToTopic("all_users")
                    messaging.subscribeToTopic("vibewave_updates")
                    Log.d(TAG, "FCM topics refreshed on boot/update")
                }.onFailure {
                    Log.w(TAG, "Failed to refresh FCM topics on boot: ${it.message}")
                }

                // 3. Refresh FCM token and save to Firestore so the backend
                //    can send targeted DM/update notifications to this specific device
                runCatching {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            // Cache locally first (Firestore save needs user to be authenticated)
                            context.getSharedPreferences(NotificationSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putString("fcm_token", token)
                                .apply()
                            // Save to Firestore for server-side targeting
                            FirestoreManager.saveFcmToken(token)
                            Log.d(TAG, "FCM token refreshed on boot and saved")
                        }
                    }
                }.onFailure {
                    Log.w(TAG, "Failed to refresh FCM token on boot: ${it.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
