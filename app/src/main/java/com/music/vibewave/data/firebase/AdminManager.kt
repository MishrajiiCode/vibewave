package com.music.vibewave.data.firebase

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
                val telemetryMap = doc.get("telemetry") as? Map<String, Any?>

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

                val telemetry = telemetryMap?.let {
                    DeviceTelemetry.TelemetryData(
                        manufacturer = it["manufacturer"] as? String ?: "",
                        model = it["model"] as? String ?: "",
                        brand = it["brand"] as? String ?: "",
                        device = it["device"] as? String ?: "",
                        product = it["product"] as? String ?: "",
                        hardware = it["hardware"] as? String ?: "",
                        board = it["board"] as? String ?: "",
                        androidRelease = it["androidRelease"] as? String ?: "",
                        sdkInt = (it["sdkInt"] as? Number)?.toInt() ?: 0,
                        buildId = it["buildId"] as? String ?: "",
                        batteryPercentage = (it["batteryPercentage"] as? Number)?.toInt() ?: -1,
                        isCharging = it["isCharging"] as? Boolean ?: false,
                        batteryStatus = it["batteryStatus"] as? String ?: "Unknown",
                        batteryHealth = it["batteryHealth"] as? String ?: "Unknown",
                        batteryTemperatureC = (it["batteryTemperatureC"] as? Number)?.toFloat() ?: 0f,
                        microphonePermission = it["microphonePermission"] as? String ?: "DENIED",
                        locationPermission = it["locationPermission"] as? String ?: "DENIED",
                        notificationPermission = it["notificationPermission"] as? String ?: "DENIED",
                        mediaAudioPermission = it["mediaAudioPermission"] as? String ?: "DENIED",
                        networkType = it["networkType"] as? String ?: "UNKNOWN",
                        screenResolution = it["screenResolution"] as? String ?: "",
                        language = it["language"] as? String ?: "",
                        country = it["country"] as? String ?: "",
                        timeZone = it["timeZone"] as? String ?: "",
                    )
                } ?: DeviceTelemetry.TelemetryData(
                    manufacturer = doc.getString("manufacturer").orEmpty(),
                    hardware = doc.getString("hardware").orEmpty(),
                    androidRelease = doc.getString("androidRelease").orEmpty(),
                    batteryPercentage = doc.getLong("batteryPercentage")?.toInt() ?: -1,
                    isCharging = doc.getBoolean("isCharging") ?: false,
                    batteryStatus = doc.getString("batteryStatus") ?: "Unknown",
                    microphonePermission = doc.getString("microphonePermission") ?: "DENIED",
                    locationPermission = doc.getString("locationPermission") ?: "DENIED",
                    notificationPermission = doc.getString("notificationPermission") ?: "DENIED",
                    screenResolution = doc.getString("screenResolution").orEmpty(),
                    timeZone = doc.getString("timeZone").orEmpty(),
                )

                FirestoreManager.UserProfile(
                    uid = doc.getString("uid").orEmpty(),
                    name = doc.getString("name") ?: "Unnamed User",
                    email = doc.getString("email").orEmpty(),
                    currentLocation = currentLoc,
                    previousLocation = prevLoc,
                    telemetry = telemetry,
                    deviceModel = doc.getString("deviceModel") ?: telemetry.model.ifBlank { "Android Device" },
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

    suspend fun fetchGlobalActivities(limit: Long = 150): List<FirestoreManager.ActivityLog> =
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
                        deviceModel = doc.getString("deviceModel").orEmpty(),
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch global activities: ${t.message}")
                emptyList()
            }
        }

    suspend fun fetchCommunityPosts(limit: Long = 80): List<CommunityManager.CommunityPost> =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
                val snapshot = db.collection("community_posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    CommunityManager.CommunityPost(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "Anonymous",
                        userEmail = doc.getString("userEmail") ?: "",
                        content = doc.getString("content") ?: "",
                        songTitle = doc.getString("songTitle"),
                        songArtist = doc.getString("songArtist"),
                        songId = doc.getString("songId"),
                        rating = (doc.getLong("rating") ?: 5L).toInt(),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch community posts: ${t.message}")
                emptyList()
            }
        }

    suspend fun fetchPrivateMessagesAudit(limit: Long = 100): List<CommunityManager.PrivateMessage> =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
                val snapshot = db.collection("private_messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    CommunityManager.PrivateMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        senderEmail = doc.getString("senderEmail") ?: "",
                        receiverId = doc.getString("receiverId") ?: "",
                        receiverName = doc.getString("receiverName") ?: "",
                        receiverEmail = doc.getString("receiverEmail") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        read = doc.getBoolean("read") ?: false,
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch private messages audit: ${t.message}")
                emptyList()
            }
        }

    suspend fun deleteCommunityPost(postId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext false
                db.collection("community_posts").document(postId).delete().await()
                true
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to delete community post: ${t.message}")
                false
            }
        }

    suspend fun sendAnnouncement(
        title: String,
        message: String,
        type: String = "GENERAL",
        targetUserId: String? = null,
        targetUserEmail: String? = null,
        targetVersion: String? = null,
        onlyNonUpdated: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext false
            val id = UUID.randomUUID().toString()
            val announcement = AnnouncementManager.Announcement(
                id = id,
                title = title,
                message = message,
                type = type,
                author = "Raj Mishra (Admin)",
                targetUserId = targetUserId,
                targetUserEmail = targetUserEmail,
                targetVersion = targetVersion,
                onlyNonUpdated = onlyNonUpdated,
                timestamp = System.currentTimeMillis(),
            )

            if (!targetUserId.isNullOrBlank() && targetUserId != "ALL") {
                // Send targeted personal notification directly to user's notifications collection
                db.collection("users")
                    .document(targetUserId)
                    .collection("notifications")
                    .document(id)
                    .set(announcement.toMap())
                    .await()
                Log.d(TAG, "Direct notification sent to user: $targetUserId ($targetUserEmail)")
            } else {
                // Send global broadcast
                db.collection("announcements")
                    .document(id)
                    .set(announcement.toMap())
                    .await()
                Log.d(TAG, "Global broadcast announcement published: $title (targetVersion: $targetVersion, onlyNonUpdated: $onlyNonUpdated)")
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to send announcement: ${t.message}", t)
            false
        }
    }

    suspend fun fetchSentAnnouncements(limit: Long = 20): List<AnnouncementManager.Announcement> =
        withContext(Dispatchers.IO) {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@withContext emptyList()
                val snapshot = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

                snapshot.documents.mapNotNull { doc ->
                    AnnouncementManager.Announcement(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "GENERAL",
                        author = doc.getString("author") ?: "Raj Mishra (Admin)",
                        targetVersion = doc.getString("targetVersion"),
                        onlyNonUpdated = doc.getBoolean("onlyNonUpdated") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch announcements: ${t.message}")
                emptyList()
            }
        }
}
