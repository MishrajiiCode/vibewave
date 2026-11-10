package com.music.vibewave.data.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.music.vibewave.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

/**
 * Real-time music synchronization engine for VibeWave ("Listen Together").
 * Backed by Cloud Firestore to enable seamless, low-latency shared playback,
 * live in-room chat, member list tracking, and host DJ access controls.
 */
object ListenTogetherManager {

    private const val TAG = "ListenTogetherManager"
    private const val ROOMS_COLLECTION = "listen_together_rooms"
    private const val CHAT_COLLECTION = "chat"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val random = SecureRandom()

    data class RoomChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val senderUid: String = "",
        val senderName: String = "",
        val text: String = "",
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "senderUid" to senderUid,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to timestamp,
        )
    }

    data class ListenTogetherRoom(
        val roomId: String = "",
        val hostUid: String = "",
        val hostName: String = "Host",
        val songId: String = "",
        val songTitle: String = "",
        val songArtist: String = "",
        val thumbnailUrl: String = "",
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val updatedAt: Long = System.currentTimeMillis(),
        val participants: Map<String, String> = emptyMap(),
        val hostOnlyControl: Boolean = true, // true = DJ Lock (only host), false = Collaborative (anyone can change music)
        val status: String = "ACTIVE", // "ACTIVE" | "CLOSED"
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        val inRoom: Boolean get() = roomId.isNotBlank() && status == "ACTIVE"
        val participantCount: Int get() = participants.size.coerceAtLeast(1)

        fun toMap(): Map<String, Any?> = mapOf(
            "roomId" to roomId,
            "hostUid" to hostUid,
            "hostName" to hostName,
            "songId" to songId,
            "songTitle" to songTitle,
            "songArtist" to songArtist,
            "thumbnailUrl" to thumbnailUrl,
            "isPlaying" to isPlaying,
            "positionMs" to positionMs,
            "updatedAt" to updatedAt,
            "participants" to participants,
            "hostOnlyControl" to hostOnlyControl,
            "status" to status,
            "createdAt" to createdAt,
        )
    }

    private val _room = MutableStateFlow<ListenTogetherRoom?>(null)
    val room = _room.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost = _isHost.asStateFlow()

    private val _isInRoom = MutableStateFlow(false)
    val isInRoom = _isInRoom.asStateFlow()

    private val _messages = MutableStateFlow<List<RoomChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private var roomListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var myUserId: String = UUID.randomUUID().toString().take(8)

    fun getMyUserId(): String {
        val user = FirestoreManager.currentUser.value
        return if (!user?.uid.isNullOrBlank()) user!!.uid else myUserId
    }

    fun canChangePlayback(): Boolean {
        if (!_isInRoom.value) return true
        if (_isHost.value) return true
        val current = _room.value ?: return true
        return !current.hostOnlyControl
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = StringBuilder()
        for (i in 0 until 6) {
            code.append(chars[random.nextInt(chars.length)])
        }
        return code.toString()
    }

    /**
     * Create a new Listen Together room and become the host.
     */
    fun createRoom(
        hostName: String,
        initialSong: Song?,
        isPlaying: Boolean,
        positionMs: Long,
        onResult: (Result<String>) -> Unit,
    ) {
        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull()
                    ?: throw IllegalStateException("Firebase is not connected")

                leaveRoom() // Clean up any previous session

                val roomId = generateRoomCode()
                val uid = getMyUserId()
                val displayName = hostName.ifBlank { "Vibe Master" }

                val newRoom = ListenTogetherRoom(
                    roomId = roomId,
                    hostUid = uid,
                    hostName = displayName,
                    songId = initialSong?.videoId.orEmpty(),
                    songTitle = initialSong?.title.orEmpty(),
                    songArtist = initialSong?.artist.orEmpty(),
                    thumbnailUrl = initialSong?.thumbnailUrl.orEmpty(),
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    updatedAt = System.currentTimeMillis(),
                    participants = mapOf(uid to displayName),
                    hostOnlyControl = true,
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis(),
                )

                db.collection(ROOMS_COLLECTION).document(roomId)
                    .set(newRoom.toMap())
                    .await()

                _isHost.value = true
                _isInRoom.value = true
                _room.value = newRoom

                listenToRoom(roomId)
                listenToChat(roomId)
                onResult(Result.success(roomId))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create room", e)
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Join an existing Listen Together room with a 6-character code.
     */
    fun joinRoom(
        roomCode: String,
        guestName: String,
        onResult: (Result<ListenTogetherRoom>) -> Unit,
    ) {
        val normalizedCode = roomCode.trim().uppercase()
        if (normalizedCode.length != 6) {
            onResult(Result.failure(IllegalArgumentException("Room code must be 6 characters")))
            return
        }

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull()
                    ?: throw IllegalStateException("Firebase is not connected")

                leaveRoom() // Clean up any previous session

                val docRef = db.collection(ROOMS_COLLECTION).document(normalizedCode)
                val snapshot = docRef.get().await()

                if (!snapshot.exists()) {
                    throw NoSuchElementException("Room '$normalizedCode' does not exist")
                }

                val status = snapshot.getString("status") ?: "ACTIVE"
                if (status != "ACTIVE") {
                    throw IllegalStateException("Room '$normalizedCode' has ended")
                }

                val uid = getMyUserId()
                val displayName = guestName.ifBlank { "Listener ${uid.take(4)}" }

                // Add participant
                docRef.update("participants.$uid", displayName).await()

                _isHost.value = false
                _isInRoom.value = true

                listenToRoom(normalizedCode)
                listenToChat(normalizedCode)

                val parsedRoom = parseRoom(normalizedCode, snapshot.data ?: emptyMap())
                _room.value = parsedRoom
                onResult(Result.success(parsedRoom))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to join room $normalizedCode", e)
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Toggle whether only the host can change music, or everyone has DJ access.
     */
    fun setHostOnlyControl(hostOnly: Boolean) {
        if (!_isHost.value || !_isInRoom.value) return
        val current = _room.value ?: return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                db.collection(ROOMS_COLLECTION).document(current.roomId)
                    .update("hostOnlyControl", hostOnly)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Error updating hostOnlyControl", e)
            }
        }
    }

    /**
     * Send a real-time message in the room chat.
     */
    fun sendChatMessage(text: String, onResult: ((Result<Unit>) -> Unit)? = null) {
        val current = _room.value ?: return
        val messageText = text.trim()
        if (messageText.isBlank()) return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val uid = getMyUserId()
                val senderName = current.participants[uid] ?: FirestoreManager.currentUser.value?.name ?: "Listener"

                val chatMessage = RoomChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderUid = uid,
                    senderName = senderName,
                    text = messageText,
                    timestamp = System.currentTimeMillis(),
                )

                db.collection(ROOMS_COLLECTION).document(current.roomId)
                    .collection(CHAT_COLLECTION)
                    .document(chatMessage.id)
                    .set(chatMessage.toMap())
                    .await()

                onResult?.invoke(Result.success(Unit))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send chat message", e)
                onResult?.invoke(Result.failure(e))
            }
        }
    }

    /**
     * Sync playback status to the room. Allowed if user is host or if hostOnlyControl is false.
     */
    fun syncPlaybackState(
        song: Song?,
        isPlaying: Boolean,
        positionMs: Long,
    ) {
        if (!_isInRoom.value) return
        if (!canChangePlayback()) return
        val current = _room.value ?: return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val updates = mapOf<String, Any?>(
                    "songId" to (song?.videoId.orEmpty()),
                    "songTitle" to (song?.title.orEmpty()),
                    "songArtist" to (song?.artist.orEmpty()),
                    "thumbnailUrl" to (song?.thumbnailUrl.orEmpty()),
                    "isPlaying" to isPlaying,
                    "positionMs" to positionMs,
                    "updatedAt" to System.currentTimeMillis(),
                )
                db.collection(ROOMS_COLLECTION).document(current.roomId)
                    .update(updates)
            } catch (e: Exception) {
                Log.w(TAG, "Sync playback error", e)
            }
        }
    }

    /**
     * Leave the current room. If host, mark as CLOSED.
     */
    fun leaveRoom() {
        val current = _room.value
        roomListener?.remove()
        roomListener = null
        chatListener?.remove()
        chatListener = null

        val wasHost = _isHost.value
        val roomId = current?.roomId

        _isHost.value = false
        _isInRoom.value = false
        _room.value = null
        _messages.value = emptyList()

        if (roomId.isNullOrBlank()) return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val docRef = db.collection(ROOMS_COLLECTION).document(roomId)
                if (wasHost) {
                    docRef.update("status", "CLOSED").await()
                } else {
                    docRef.update("participants.${getMyUserId()}", FieldValue.delete()).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Leave room cleanup error", e)
            }
        }
    }

    private fun listenToRoom(roomId: String) {
        roomListener?.remove()
        val db = FirestoreManager.getFirestoreOrNull() ?: return

        roomListener = db.collection(ROOMS_COLLECTION).document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to room $roomId", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    leaveRoom()
                    return@addSnapshotListener
                }

                val data = snapshot.data ?: return@addSnapshotListener
                val updatedRoom = parseRoom(roomId, data)

                if (updatedRoom.status != "ACTIVE") {
                    leaveRoom()
                    return@addSnapshotListener
                }

                _room.value = updatedRoom
            }
    }

    private fun listenToChat(roomId: String) {
        chatListener?.remove()
        val db = FirestoreManager.getFirestoreOrNull() ?: return

        chatListener = db.collection(ROOMS_COLLECTION).document(roomId)
            .collection(CHAT_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to chat in $roomId", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        RoomChatMessage(
                            id = doc.getString("id") ?: doc.id,
                            senderUid = doc.getString("senderUid") ?: "",
                            senderName = doc.getString("senderName") ?: "Listener",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        )
                    }.getOrNull()
                } ?: emptyList()

                _messages.value = list
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRoom(roomId: String, map: Map<String, Any?>): ListenTogetherRoom {
        return ListenTogetherRoom(
            roomId = roomId,
            hostUid = map["hostUid"] as? String ?: "",
            hostName = map["hostName"] as? String ?: "Host",
            songId = map["songId"] as? String ?: "",
            songTitle = map["songTitle"] as? String ?: "",
            songArtist = map["songArtist"] as? String ?: "",
            thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
            isPlaying = map["isPlaying"] as? Boolean ?: false,
            positionMs = (map["positionMs"] as? Number)?.toLong() ?: 0L,
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            participants = (map["participants"] as? Map<String, String>) ?: emptyMap(),
            hostOnlyControl = map["hostOnlyControl"] as? Boolean ?: true,
            status = map["status"] as? String ?: "ACTIVE",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        )
    }
}
