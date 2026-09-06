package com.music.bitchord.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.music.bitchord.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Centralized Firebase Firestore manager owned and controlled by Raj Mishra.
 * Manages user profile sync, location history (latest and previous),
 * and real-time app activity logging.
 */
object FirestoreManager {

    private const val TAG = "FirestoreManager"
    private const val PREFS_NAME = "vibewave_user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_SETUP_DONE = "setup_done"

    private const val USERS_COLLECTION = "users"
    private const val ACTIVITIES_COLLECTION = "activity_logs"
    private const val ANNOUNCEMENTS_COLLECTION = "announcements"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered = _isRegistered.asStateFlow()

    data class LocationData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val city: String = "",
        val state: String = "",
        val country: String = "",
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "city" to city,
            "state" to state,
            "country" to country,
            "timestamp" to timestamp,
        )
    }

    data class UserProfile(
        val uid: String = "",
        val name: String = "",
        val email: String = "",
        val currentLocation: LocationData? = null,
        val previousLocation: LocationData? = null,
        val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val appVersion: String = BuildConfig.VERSION_NAME,
        val createdAt: Long = System.currentTimeMillis(),
        val lastActive: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "currentLocation" to currentLocation?.toMap(),
            "previousLocation" to previousLocation?.toMap(),
            "deviceModel" to deviceModel,
            "appVersion" to appVersion,
            "createdAt" to createdAt,
            "lastActive" to lastActive,
        )
    }

    data class ActivityLog(
        val id: String = UUID.randomUUID().toString(),
        val userId: String = "",
        val userName: String = "",
        val userEmail: String = "",
        val activityType: String = "",
        val title: String = "",
        val details: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val locationCity: String = "",
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "activityType" to activityType,
            "title" to title,
            "details" to details,
            "timestamp" to timestamp,
            "locationCity" to locationCity,
        )
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val setupDone = prefs?.getBoolean(KEY_SETUP_DONE, false) ?: false
        val email = prefs?.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs?.getString(KEY_USER_NAME, "") ?: ""
        val uid = prefs?.getString(KEY_USER_ID, "") ?: ""

        if (setupDone && email.isNotBlank()) {
            _isRegistered.value = true
            _currentUser.value = UserProfile(
                uid = uid.ifBlank { sanitizeEmail(email) },
                name = name,
                email = email,
                lastActive = System.currentTimeMillis(),
            )
            updateLastActive()
        }
    }

    fun sanitizeEmail(email: String): String {
        return email.lowercase().replace(".", "_").replace("@", "_at_").trim()
    }

    fun registerUser(name: String, email: String, initialLocation: LocationData? = null) {
        val uid = sanitizeEmail(email)
        prefs?.edit()
            ?.putString(KEY_USER_ID, uid)
            ?.putString(KEY_USER_NAME, name)
            ?.putString(KEY_USER_EMAIL, email)
            ?.putBoolean(KEY_SETUP_DONE, true)
            ?.apply()

        val profile = UserProfile(
            uid = uid,
            name = name,
            email = email,
            currentLocation = initialLocation,
            createdAt = System.currentTimeMillis(),
            lastActive = System.currentTimeMillis(),
        )

        _currentUser.value = profile
        _isRegistered.value = true

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection(USERS_COLLECTION).document(uid)
                    .set(profile.toMap(), SetOptions.merge())
                    .await()
                Log.d(TAG, "User profile saved to Firestore: $email")
                logActivity(
                    activityType = "USER_REGISTERED",
                    title = "User Registered: $name",
                    details = "New user registered with email $email",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save user to Firestore: ${e.message}")
            }
        }
    }

    fun updateLocation(newLocation: LocationData) {
        val user = _currentUser.value ?: return
        val currentLoc = user.currentLocation

        // If location hasn't changed significantly, skip
        if (currentLoc != null &&
            Math.abs(currentLoc.latitude - newLocation.latitude) < 0.0001 &&
            Math.abs(currentLoc.longitude - newLocation.longitude) < 0.0001
        ) {
            return
        }

        val updated = user.copy(
            previousLocation = currentLoc,
            currentLocation = newLocation,
            lastActive = System.currentTimeMillis(),
        )
        _currentUser.value = updated

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection(USERS_COLLECTION).document(user.uid)

                val updates = mapOf(
                    "previousLocation" to currentLoc?.toMap(),
                    "currentLocation" to newLocation.toMap(),
                    "lastActive" to System.currentTimeMillis(),
                )
                userDoc.set(updates, SetOptions.merge()).await()

                // Also append to location history subcollection
                userDoc.collection("locations").add(newLocation.toMap()).await()

                Log.d(TAG, "Location updated in Firestore for ${user.email}: ${newLocation.city}")
                logActivity(
                    activityType = "LOCATION_UPDATE",
                    title = "Location Updated: ${newLocation.city}",
                    details = "Coordinates: (${newLocation.latitude}, ${newLocation.longitude})",
                    cityOverride = newLocation.city,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update location in Firestore: ${e.message}")
            }
        }
    }

    fun updateLastActive() {
        val user = _currentUser.value ?: return
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection(USERS_COLLECTION).document(user.uid)
                    .set(mapOf("lastActive" to System.currentTimeMillis()), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update last active: ${e.message}")
            }
        }
    }

    fun logActivity(
        activityType: String,
        title: String,
        details: String,
        cityOverride: String? = null,
    ) {
        val user = _currentUser.value
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            userId = user?.uid ?: "guest",
            userName = user?.name ?: "Guest",
            userEmail = user?.email ?: "guest@vibewave.app",
            activityType = activityType,
            title = title,
            details = details,
            timestamp = System.currentTimeMillis(),
            locationCity = cityOverride ?: user?.currentLocation?.city.orEmpty(),
        )

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                // 1. Write to global activity_logs collection for admin overview
                db.collection(ACTIVITIES_COLLECTION).document(log.id).set(log.toMap()).await()

                // 2. Also write to user-specific activities subcollection if registered
                if (user != null && user.uid.isNotBlank() && user.uid != "guest") {
                    db.collection(USERS_COLLECTION)
                        .document(user.uid)
                        .collection("activities")
                        .document(log.id)
                        .set(log.toMap())
                        .await()
                }
                Log.d(TAG, "Activity logged: $activityType - $title")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log activity to Firestore: ${e.message}")
            }
        }
    }
}
