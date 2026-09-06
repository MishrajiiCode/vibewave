package com.music.bitchord.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Administrative control layer for Raj Mishra.
 * Provides live access to all user profiles, location audits (current and previous),
 * activity feeds, and broadcast messaging.
 */
object AdminManager {

    private const val TAG = "AdminManager"
    private const val MASTER_PASSCODE = "rajmishra2026"
    private const val MASTER_EMAIL = "mishrajiicode@gmail.com"

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    fun authenticate(passcodeOrEmail: String): Boolean {
        val trimmed = passcodeOrEmail.trim()
        val success = trimmed == MASTER_PASSCODE ||
                trimmed.equals(MASTER_EMAIL, ignoreCase = true) ||
                trimmed == "rajmishra"
        _isAuthenticated.value = success
        return success
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    suspend fun fetchAllUsers(): List<FirestoreManager.UserProfile> = withContext(Dispatchers.IO) {
        try {
            val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
            val snapshot = db.collection("users")
                .orderBy("lastActive", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val currentLocMap = doc.get("currentLocation") as? Map<String, Any?>
                val prevLocMap = doc.get("previousLocation") as? Map<String, Any?>

                val currentLoc = currentLocMap?.let {
                    FirestoreManager.LocationData(
                        latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                        city = it["city"] as? String ?: "",
                        state = it["state"] as? String ?: "",
                        country = it["country"] as? String ?: "",
                        timestamp = (it["timestamp"] as? Number)?.toLong() ?: 0L,
                    )
                }

                val prevLoc = prevLocMap?.let {
                    FirestoreManager.LocationData(
                        latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                        city = it["city"] as? String ?: "",
                        state = it["state"] as? String ?: "",
                        country = it["country"] as? String ?: "",
                        timestamp = (it["timestamp"] as? Number)?.toLong() ?: 0L,
                    )
                }

                FirestoreManager.UserProfile(
                    uid = doc.getString("uid").orEmpty(),
                    name = doc.getString("name") ?: "Unnamed User",
                    email = doc.getString("email").orEmpty(),
                    currentLocation = currentLoc,
                    previousLocation = prevLoc,
                    deviceModel = doc.getString("deviceModel") ?: "Android Device",
                    appVersion = doc.getString("appVersion") ?: "1.5.2",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    lastActive = doc.getLong("lastActive") ?: 0L,
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch users: ${t.message}")
            emptyList()
        }
    }

    suspend fun fetchUserActivities(userId: String): List<FirestoreManager.ActivityLog> =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("activities")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    FirestoreManager.ActivityLog(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        userName = doc.getString("userName").orEmpty(),
                        userEmail = doc.getString("userEmail").orEmpty(),
                        activityType = doc.getString("activityType").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        details = doc.getString("details").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        locationCity = doc.getString("locationCity").orEmpty(),
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch user activities: ${t.message}")
                emptyList()
            }
        }

    suspend fun fetchGlobalActivities(limit: Long = 60): List<FirestoreManager.ActivityLog> =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
                val snapshot = db.collection("activity_logs")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    FirestoreManager.ActivityLog(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        userName = doc.getString("userName").orEmpty(),
                        userEmail = doc.getString("userEmail").orEmpty(),
                        activityType = doc.getString("activityType").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        details = doc.getString("details").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        locationCity = doc.getString("locationCity").orEmpty(),
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch global activities: ${t.message}")
                emptyList()
            }
        }

    suspend fun sendAnnouncement(title: String, message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext false
            val id = UUID.randomUUID().toString()
            val announcement = mapOf(
                "id" to id,
                "title" to title,
                "message" to message,
                "author" to "Raj Mishra (Admin)",
                "timestamp" to System.currentTimeMillis(),
            )
            db.collection("announcements").document(id).set(announcement).await()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to post announcement: ${t.message}")
            false
        }
    }
}
