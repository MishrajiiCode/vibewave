package com.music.vibewave.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.music.vibewave.BuildConfig
import com.music.vibewave.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Centralized Firebase Firestore manager owned and controlled by Raj Mishra.
 * Manages user profile sync, location history (latest and previous),
 * listening habits (plays, skips, favorites), and real-time app activity logging.
 */
object FirestoreManager {

    private const val TAG = "FirestoreManager"
    private const val PREFS_NAME = "vibewave_user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_TASTE_PROFILE_JSON = "user_taste_profile_json"

    private const val USERS_COLLECTION = "users"
    private const val ACTIVITIES_COLLECTION = "activity_logs"
    private const val ANNOUNCEMENTS_COLLECTION = "announcements"

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Non-fatal Firestore coroutine error: ${throwable.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered = _isRegistered.asStateFlow()

    private val _tasteProfile = MutableStateFlow(UserTasteProfile())
    val tasteProfile = _tasteProfile.asStateFlow()

    private var lastRecordedPlayVideoId: String? = null
    private var lastRecordedPlayTime: Long = 0L

    private var appContext: Context? = null

    fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseFirestore instance unavailable: ${t.message}")
            null
        }
    }

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

    data class UserTasteProfile(
        val playCount: Int = 0,
        val skipCount: Int = 0,
        val favoriteCount: Int = 0,
        val topArtists: Map<String, Int> = emptyMap(),
        val favoriteSongs: List<String> = emptyList(), // video IDs
        val favoriteSongTitles: List<String> = emptyList(),
        val skippedSongs: List<String> = emptyList(), // video IDs
        val skippedArtists: Map<String, Int> = emptyMap(),
        val recentPlays: List<String> = emptyList(),
        val lastUpdated: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "playCount" to playCount,
            "skipCount" to skipCount,
            "favoriteCount" to favoriteCount,
            "topArtists" to topArtists,
            "favoriteSongs" to favoriteSongs,
            "favoriteSongTitles" to favoriteSongTitles,
            "skippedSongs" to skippedSongs,
            "skippedArtists" to skippedArtists,
            "recentPlays" to recentPlays,
            "lastUpdated" to lastUpdated,
        )
    }

    data class UserProfile(
        val uid: String = "",
        val name: String = "",
        val email: String = "",
        val currentLocation: LocationData? = null,
        val previousLocation: LocationData? = null,
        val tasteProfile: UserTasteProfile? = null,
        val telemetry: DeviceTelemetry.TelemetryData? = null,
        val fcmToken: String = "",
        val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val appVersion: String = BuildConfig.VERSION_NAME,
        val createdAt: Long = System.currentTimeMillis(),
        val lastActive: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "fcmToken" to fcmToken,
            "currentLocation" to currentLocation?.toMap(),
            "previousLocation" to previousLocation?.toMap(),
            "tasteProfile" to tasteProfile?.toMap(),
            "telemetry" to telemetry?.toMap(),
            "deviceModel" to deviceModel,
            "appVersion" to appVersion,
            // Top-level telemetry shortcuts for instant Firestore queries
            "manufacturer" to (telemetry?.manufacturer ?: Build.MANUFACTURER),
            "hardware" to (telemetry?.hardware ?: Build.HARDWARE),
            "androidRelease" to (telemetry?.androidRelease ?: Build.VERSION.RELEASE),
            "batteryPercentage" to (telemetry?.batteryPercentage ?: -1),
            "isCharging" to (telemetry?.isCharging ?: false),
            "batteryStatus" to (telemetry?.batteryStatus ?: "Unknown"),
            "microphonePermission" to (telemetry?.microphonePermission ?: "DENIED"),
            "locationPermission" to (telemetry?.locationPermission ?: "DENIED"),
            "notificationPermission" to (telemetry?.notificationPermission ?: "DENIED"),
            "screenResolution" to (telemetry?.screenResolution ?: ""),
            "timeZone" to (telemetry?.timeZone ?: ""),
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
        val deviceModel: String = "",
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
            "deviceModel" to deviceModel,
        )
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        }.onFailure {
            Log.w(TAG, "FirebaseApp.initializeApp skipped: ${it.message}")
        }

        // Authenticate anonymously so Firestore security rules never reject requests
        runCatching {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        Log.d(TAG, "FirebaseAuth anonymous sign-in success: ${it.user?.uid}")
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "FirebaseAuth anonymous sign-in failed: ${it.message}")
                    }
            }
        }

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load local cached taste profile
        val cachedTasteJson = prefs?.getString(KEY_TASTE_PROFILE_JSON, "") ?: ""
        if (cachedTasteJson.isNotBlank()) {
            _tasteProfile.value = parseTasteProfile(cachedTasteJson)
        }

        val setupDone = prefs?.getBoolean(KEY_SETUP_DONE, false) ?: false
        val email = prefs?.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs?.getString(KEY_USER_NAME, "") ?: ""
        val uid = prefs?.getString(KEY_USER_ID, "") ?: ""

        val currentTelemetry = DeviceTelemetry.collect(context)

        if (setupDone && email.isNotBlank()) {
            val resolvedUid = uid.ifBlank { sanitizeEmail(email) }
            _isRegistered.value = true
            _currentUser.value = UserProfile(
                uid = resolvedUid,
                name = name,
                email = email,
                tasteProfile = _tasteProfile.value,
                telemetry = currentTelemetry,
                lastActive = System.currentTimeMillis(),
            )
            updateLastActive()
            syncDeviceTelemetry()
            fetchRemoteTasteProfile(resolvedUid)
        }

        // Fetch current FCM token if available
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        saveFcmToken(token)
                    }
                }
        }
    }

    fun sanitizeEmail(email: String): String {
        return email.lowercase().replace(".", "_").replace("@", "_at_").trim()
    }

    fun registerUser(
        name: String,
        email: String,
        context: Context? = null,
        initialLocation: LocationData? = null
    ) {
        val uid = sanitizeEmail(email)
        prefs?.edit()
            ?.putString(KEY_USER_ID, uid)
            ?.putString(KEY_USER_NAME, name)
            ?.putString(KEY_USER_EMAIL, email)
            ?.putBoolean(KEY_SETUP_DONE, true)
            ?.apply()

        val ctx = context ?: appContext
        val telemetry = ctx?.let { DeviceTelemetry.collect(it) } ?: DeviceTelemetry.TelemetryData()

        val profile = UserProfile(
            uid = uid,
            name = name,
            email = email,
            currentLocation = initialLocation,
            telemetry = telemetry,
            createdAt = System.currentTimeMillis(),
            lastActive = System.currentTimeMillis(),
        )

        _currentUser.value = profile
        _isRegistered.value = true

        scope.launch {
            try {
                val db = getFirestoreOrNull() ?: return@launch
                db.collection(USERS_COLLECTION).document(uid)
                    .set(profile.toMap(), SetOptions.merge())
                    .await()
                Log.d(TAG, "User profile and rich device telemetry successfully saved to Firestore for: $email")
                logActivity(
                    activityType = "USER_REGISTERED",
                    title = "User Registered: $name",
                    details = "Registered with $email · Model: ${telemetry.model} · Battery: ${telemetry.batteryPercentage}%",
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to save user to Firestore: ${t.message}", t)
            }
        }
    }

    fun syncDeviceTelemetry() {
        val user = _currentUser.value ?: return
        val ctx = appContext ?: return
        scope.launch {
            try {
                val telemetry = DeviceTelemetry.collect(ctx)
                val db = getFirestoreOrNull() ?: return@launch
                val updates = mapOf(
                    "telemetry" to telemetry.toMap(),
                    "batteryPercentage" to telemetry.batteryPercentage,
                    "isCharging" to telemetry.isCharging,
                    "batteryStatus" to telemetry.batteryStatus,
                    "microphonePermission" to telemetry.microphonePermission,
                    "locationPermission" to telemetry.locationPermission,
                    "notificationPermission" to telemetry.notificationPermission,
                    "lastActive" to System.currentTimeMillis(),
                )
                db.collection(USERS_COLLECTION).document(user.uid)
                    .set(updates, SetOptions.merge())
                    .await()
                Log.d(TAG, "Device telemetry refreshed in Firestore for ${user.email} (Battery: ${telemetry.batteryPercentage}%)")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to sync device telemetry: ${t.message}")
            }
        }
    }

    fun saveFcmToken(token: String) {
        if (token.isBlank()) return
        val user = _currentUser.value
        _currentUser.value = user?.copy(fcmToken = token)
        // Resolve UID: from in-memory user, or from shared prefs cache (set when user logged in)
        val uid = user?.uid?.takeIf { it.isNotBlank() && it != "guest" }
            ?: prefs?.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
        if (!uid.isNullOrBlank()) {
            scope.launch {
                try {
                    val db = getFirestoreOrNull() ?: return@launch
                    db.collection(USERS_COLLECTION).document(uid)
                        .set(
                            mapOf(
                                "fcmToken" to token,
                                "lastActive" to System.currentTimeMillis(),
                                "platform" to "android",
                                "appVersion" to BuildConfig.VERSION_NAME,
                            ),
                            SetOptions.merge()
                        )
                        .await()
                    Log.d(TAG, "FCM token synced to Firestore for uid: $uid")
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to sync FCM token: ${t.message}")
                }
            }
        } else {
            Log.d(TAG, "FCM token cached locally; will sync when user logs in")
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
                val db = getFirestoreOrNull() ?: return@launch
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
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to update location in Firestore: ${t.message}")
            }
        }
    }

    fun updateLastActive() {
        val user = _currentUser.value ?: return
        scope.launch {
            try {
                val db = getFirestoreOrNull() ?: return@launch
                db.collection(USERS_COLLECTION).document(user.uid)
                    .set(mapOf("lastActive" to System.currentTimeMillis()), SetOptions.merge())
                    .await()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to update last active: ${t.message}")
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
        val fallbackDevice = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
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
            deviceModel = user?.deviceModel?.ifBlank { fallbackDevice } ?: fallbackDevice,
        )

        scope.launch {
            try {
                val db = getFirestoreOrNull() ?: return@launch
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
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to log activity to Firestore: ${t.message}")
            }
        }
    }

    fun getUserTasteProfile(): UserTasteProfile = _tasteProfile.value

    fun recordPlay(song: Song) {
        val now = System.currentTimeMillis()
        if (song.videoId == lastRecordedPlayVideoId && (now - lastRecordedPlayTime) < 4000L) {
            return
        }
        lastRecordedPlayVideoId = song.videoId
        lastRecordedPlayTime = now

        val current = _tasteProfile.value
        val artist = song.artist.trim()
        val updatedArtists = current.topArtists.toMutableMap()
        if (artist.isNotBlank() && artist != "Unknown Artist") {
            updatedArtists[artist] = (updatedArtists[artist] ?: 0) + 2
        }
        val updatedRecent = (listOf(song.title) + current.recentPlays.filterNot { it == song.title }).take(25)

        val updated = current.copy(
            playCount = current.playCount + 1,
            topArtists = updatedArtists,
            recentPlays = updatedRecent,
            lastUpdated = now,
        )
        _tasteProfile.value = updated
        saveAndSyncTasteProfile(updated)
    }

    fun recordSkip(song: Song) {
        val now = System.currentTimeMillis()
        val current = _tasteProfile.value
        val artist = song.artist.trim()

        val updatedSkippedArtists = current.skippedArtists.toMutableMap()
        if (artist.isNotBlank() && artist != "Unknown Artist") {
            updatedSkippedArtists[artist] = (updatedSkippedArtists[artist] ?: 0) + 1
        }
        val updatedSkippedSongs = (listOf(song.videoId) + current.skippedSongs.filterNot { it == song.videoId }).take(50)

        // Decrease artist affinity slightly when repeatedly skipped
        val updatedArtists = current.topArtists.toMutableMap()
        if (updatedArtists.containsKey(artist)) {
            val weight = updatedArtists[artist] ?: 0
            if (weight > 1) {
                updatedArtists[artist] = weight - 1
            }
        }

        val updated = current.copy(
            skipCount = current.skipCount + 1,
            skippedSongs = updatedSkippedSongs,
            skippedArtists = updatedSkippedArtists,
            topArtists = updatedArtists,
            lastUpdated = now,
        )
        _tasteProfile.value = updated
        saveAndSyncTasteProfile(updated)
    }

    fun recordLike(songTitle: String, artist: String, videoId: String, isLiked: Boolean) {
        val now = System.currentTimeMillis()
        val current = _tasteProfile.value
        val cleanArtist = artist.trim()

        val updatedFavSongs = current.favoriteSongs.toMutableList()
        val updatedFavTitles = current.favoriteSongTitles.toMutableList()
        val updatedArtists = current.topArtists.toMutableMap()

        if (isLiked) {
            if (!updatedFavSongs.contains(videoId)) updatedFavSongs.add(videoId)
            if (songTitle.isNotBlank() && !updatedFavTitles.contains(songTitle)) updatedFavTitles.add(songTitle)
            if (cleanArtist.isNotBlank() && cleanArtist != "Unknown Artist") {
                updatedArtists[cleanArtist] = (updatedArtists[cleanArtist] ?: 0) + 5
            }
        } else {
            updatedFavSongs.remove(videoId)
            updatedFavTitles.remove(songTitle)
            if (cleanArtist.isNotBlank()) {
                val weight = updatedArtists[cleanArtist] ?: 0
                if (weight > 3) updatedArtists[cleanArtist] = weight - 3
            }
        }

        val updated = current.copy(
            favoriteCount = if (isLiked) current.favoriteCount + 1 else (current.favoriteCount - 1).coerceAtLeast(0),
            favoriteSongs = updatedFavSongs,
            favoriteSongTitles = updatedFavTitles,
            topArtists = updatedArtists,
            lastUpdated = now,
        )
        _tasteProfile.value = updated
        saveAndSyncTasteProfile(updated)
    }

    fun recordLike(song: Song, isLiked: Boolean) {
        recordLike(song.title, song.artist, song.videoId, isLiked)
    }

    private fun saveAndSyncTasteProfile(profile: UserTasteProfile) {
        // 1. Save locally to SharedPreferences for instant, offline access
        prefs?.edit()?.putString(KEY_TASTE_PROFILE_JSON, tasteProfileToJson(profile))?.apply()

        // 2. Sync to Firestore in background
        val user = _currentUser.value
        scope.launch {
            try {
                val db = getFirestoreOrNull() ?: return@launch
                val uid = user?.uid?.takeIf { it.isNotBlank() && it != "guest" }
                if (uid != null) {
                    val userDoc = db.collection(USERS_COLLECTION).document(uid)
                    userDoc.set(mapOf("tasteProfile" to profile.toMap()), SetOptions.merge()).await()
                    userDoc.collection("taste").document("summary").set(profile.toMap(), SetOptions.merge()).await()
                    Log.d(TAG, "Taste profile synced to Firestore for $uid")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to sync taste profile to Firestore: ${t.message}")
            }
        }
    }

    private fun fetchRemoteTasteProfile(uid: String) {
        scope.launch {
            try {
                val db = getFirestoreOrNull() ?: return@launch
                val snap = db.collection(USERS_COLLECTION).document(uid).collection("taste").document("summary").get().await()
                if (snap.exists()) {
                    val remoteData = snap.data ?: return@launch
                    val playCount = (remoteData["playCount"] as? Number)?.toInt() ?: 0
                    val skipCount = (remoteData["skipCount"] as? Number)?.toInt() ?: 0
                    val favoriteCount = (remoteData["favoriteCount"] as? Number)?.toInt() ?: 0
                    val topArtists = (remoteData["topArtists"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                        if (k is String && v is Number) k to v.toInt() else null
                    }?.toMap() ?: emptyMap()
                    val favoriteSongs = (remoteData["favoriteSongs"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val favoriteSongTitles = (remoteData["favoriteSongTitles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val skippedSongs = (remoteData["skippedSongs"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val skippedArtists = (remoteData["skippedArtists"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                        if (k is String && v is Number) k to v.toInt() else null
                    }?.toMap() ?: emptyMap()
                    val recentPlays = (remoteData["recentPlays"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    val remoteProfile = UserTasteProfile(
                        playCount = playCount,
                        skipCount = skipCount,
                        favoriteCount = favoriteCount,
                        topArtists = topArtists,
                        favoriteSongs = favoriteSongs,
                        favoriteSongTitles = favoriteSongTitles,
                        skippedSongs = skippedSongs,
                        skippedArtists = skippedArtists,
                        recentPlays = recentPlays,
                        lastUpdated = (remoteData["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    )
                    val local = _tasteProfile.value
                    val merged = mergeTasteProfiles(local, remoteProfile)
                    _tasteProfile.value = merged
                    prefs?.edit()?.putString(KEY_TASTE_PROFILE_JSON, tasteProfileToJson(merged))?.apply()
                    Log.d(TAG, "Taste profile merged from Firestore: ${merged.playCount} plays, ${merged.topArtists.size} artists")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to fetch remote taste profile: ${t.message}")
            }
        }
    }

    private fun mergeTasteProfiles(local: UserTasteProfile, remote: UserTasteProfile): UserTasteProfile {
        val mergedArtists = (local.topArtists.keys + remote.topArtists.keys).associateWith { key ->
            maxOf(local.topArtists[key] ?: 0, remote.topArtists[key] ?: 0)
        }
        val mergedSkippedArtists = (local.skippedArtists.keys + remote.skippedArtists.keys).associateWith { key ->
            maxOf(local.skippedArtists[key] ?: 0, remote.skippedArtists[key] ?: 0)
        }
        return UserTasteProfile(
            playCount = maxOf(local.playCount, remote.playCount),
            skipCount = maxOf(local.skipCount, remote.skipCount),
            favoriteCount = maxOf(local.favoriteCount, remote.favoriteCount),
            topArtists = mergedArtists,
            favoriteSongs = (local.favoriteSongs + remote.favoriteSongs).distinct(),
            favoriteSongTitles = (local.favoriteSongTitles + remote.favoriteSongTitles).distinct(),
            skippedSongs = (local.skippedSongs + remote.skippedSongs).distinct(),
            skippedArtists = mergedSkippedArtists,
            recentPlays = (local.recentPlays + remote.recentPlays).distinct().take(25),
            lastUpdated = maxOf(local.lastUpdated, remote.lastUpdated),
        )
    }

    private fun tasteProfileToJson(profile: UserTasteProfile): String {
        return try {
            val obj = JSONObject()
            obj.put("playCount", profile.playCount)
            obj.put("skipCount", profile.skipCount)
            obj.put("favoriteCount", profile.favoriteCount)

            val artistsObj = JSONObject()
            profile.topArtists.forEach { (k, v) -> artistsObj.put(k, v) }
            obj.put("topArtists", artistsObj)

            val favSongsArr = JSONArray(profile.favoriteSongs)
            obj.put("favoriteSongs", favSongsArr)

            val favTitlesArr = JSONArray(profile.favoriteSongTitles)
            obj.put("favoriteSongTitles", favTitlesArr)

            val skippedSongsArr = JSONArray(profile.skippedSongs)
            obj.put("skippedSongs", skippedSongsArr)

            val skippedArtistsObj = JSONObject()
            profile.skippedArtists.forEach { (k, v) -> skippedArtistsObj.put(k, v) }
            obj.put("skippedArtists", skippedArtistsObj)

            val recentPlaysArr = JSONArray(profile.recentPlays)
            obj.put("recentPlays", recentPlaysArr)

            obj.put("lastUpdated", profile.lastUpdated)
            obj.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    private fun parseTasteProfile(jsonStr: String): UserTasteProfile {
        return try {
            val obj = JSONObject(jsonStr)
            val playCount = obj.optInt("playCount", 0)
            val skipCount = obj.optInt("skipCount", 0)
            val favoriteCount = obj.optInt("favoriteCount", 0)

            val topArtistsMap = mutableMapOf<String, Int>()
            val artistsObj = obj.optJSONObject("topArtists")
            artistsObj?.keys()?.forEach { key ->
                topArtistsMap[key] = artistsObj.optInt(key, 0)
            }

            val favSongsList = mutableListOf<String>()
            val favSongsArr = obj.optJSONArray("favoriteSongs")
            if (favSongsArr != null) {
                for (i in 0 until favSongsArr.length()) {
                    favSongsList.add(favSongsArr.getString(i))
                }
            }

            val favTitlesList = mutableListOf<String>()
            val favTitlesArr = obj.optJSONArray("favoriteSongTitles")
            if (favTitlesArr != null) {
                for (i in 0 until favTitlesArr.length()) {
                    favTitlesList.add(favTitlesArr.getString(i))
                }
            }

            val skippedSongsList = mutableListOf<String>()
            val skippedSongsArr = obj.optJSONArray("skippedSongs")
            if (skippedSongsArr != null) {
                for (i in 0 until skippedSongsArr.length()) {
                    skippedSongsList.add(skippedSongsArr.getString(i))
                }
            }

            val skippedArtistsMap = mutableMapOf<String, Int>()
            val skippedArtistsObj = obj.optJSONObject("skippedArtists")
            skippedArtistsObj?.keys()?.forEach { key ->
                skippedArtistsMap[key] = skippedArtistsObj.optInt(key, 0)
            }

            val recentPlaysList = mutableListOf<String>()
            val recentPlaysArr = obj.optJSONArray("recentPlays")
            if (recentPlaysArr != null) {
                for (i in 0 until recentPlaysArr.length()) {
                    recentPlaysList.add(recentPlaysArr.getString(i))
                }
            }

            val lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())

            UserTasteProfile(
                playCount = playCount,
                skipCount = skipCount,
                favoriteCount = favoriteCount,
                topArtists = topArtistsMap,
                favoriteSongs = favSongsList,
                favoriteSongTitles = favTitlesList,
                skippedSongs = skippedSongsList,
                skippedArtists = skippedArtistsMap,
                recentPlays = recentPlaysList,
                lastUpdated = lastUpdated,
            )
        } catch (e: Exception) {
            UserTasteProfile()
        }
    }
}
