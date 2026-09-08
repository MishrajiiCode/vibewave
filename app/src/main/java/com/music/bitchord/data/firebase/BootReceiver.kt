package com.music.bitchord.data.firebase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Ensures background notification polling and FCM subscriptions are restored
 * whenever the user's phone restarts or when the app is updated.
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
                // 1. Reschedule background sync worker
                NotificationSyncWorker.schedule(context)

                // 2. Ensure FCM topic subscriptions are active
                runCatching {
                    FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                    FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    FirebaseMessaging.getInstance().subscribeToTopic("vibewave_updates")
                    Log.d(TAG, "FCM topics refreshed on boot/update")
                }.onFailure {
                    Log.w(TAG, "Failed to refresh FCM topics on boot: ${it.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
