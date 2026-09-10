package com.music.vibewave.data.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Real-time community discussion, song reviews, and 1-on-1 private messaging engine for VibeWave.
 * Backed by Google Cloud Firestore.
 */
object CommunityManager {

    private const val TAG = "CommunityManager"
    private const val POSTS_COLLECTION = "community_posts"
    private const val MESSAGES_COLLECTION = "private_messages"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class CommunityPost(
        val id: String = UUID.randomUUID().toString(),
        val userId: String = "",
        val userName: String = "Anonymous",
        val userEmail: String = "",
        val content: String = "",
        val songTitle: String? = null,
        val songArtist: String? = null,
        val songId: String? = null,
        val rating: Int = 5, // 1 to 5
        val timestamp: Long = System.currentTimeMillis(),
        val likesCount: Int = 0,
        val likedBy: List<String> = emptyList(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "content" to content,
            "songTitle" to songTitle,
            "songArtist" to songArtist,
            "songId" to songId,
            "rating" to rating,
            "timestamp" to timestamp,
            "likesCount" to likesCount,
            "likedBy" to likedBy,
        )
    }

    data class PrivateMessage(
        val id: String = UUID.randomUUID().toString(),
        val conversationId: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val senderEmail: String = "",
        val receiverId: String = "",
        val receiverName: String = "",
        val receiverEmail: String = "",
        val message: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val read: Boolean = false,
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "conversationId" to (conversationId.ifBlank { computeConversationId(senderId, receiverId) }),
            "senderId" to senderId,
            "senderName" to senderName,
            "senderEmail" to senderEmail,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "receiverEmail" to receiverEmail,
            "message" to message,
            "timestamp" to timestamp,
            "read" to read,
        )

        companion object {
            fun computeConversationId(userA: String, userB: String): String {
                return if (userA < userB) "${userA}_$userB" else "${userB}_$userA"
            }
        }
    }

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _currentConversation = MutableStateFlow<List<PrivateMessage>>(emptyList())
    val currentConversation = _currentConversation.asStateFlow()

    private val _recentConversations = MutableStateFlow<List<PrivateMessage>>(emptyList())
    val recentConversations = _recentConversations.asStateFlow()

    private val _communityUsers = MutableStateFlow<List<FirestoreManager.UserProfile>>(emptyList())
    val communityUsers = _communityUsers.asStateFlow()

    private var postsListener: ListenerRegistration? = null
    private var conversationListener: ListenerRegistration? = null
    private var myMessagesListener: ListenerRegistration? = null

    init {
        startListeningPosts()
    }

    fun startListeningPosts() {
        if (postsListener != null) return
        val db = FirestoreManager.getFirestoreOrNull() ?: return
        postsListener = db.collection(POSTS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to posts failed", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        CommunityPost(
                            id = doc.getString("id") ?: doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Anonymous",
                            userEmail = doc.getString("userEmail") ?: "",
                            content = doc.getString("content") ?: "",
                            songTitle = doc.getString("songTitle"),
                            songArtist = doc.getString("songArtist"),
                            songId = doc.getString("songId"),
                            rating = (doc.getLong("rating") ?: 5L).toInt(),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                            likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        )
                    }.getOrNull()
                } ?: emptyList()
                _posts.value = list
            }
    }

    fun postReview(
        content: String,
        songTitle: String? = null,
        songArtist: String? = null,
        songId: String? = null,
        rating: Int = 5,
    ) {
        if (content.isBlank()) return
        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val user = FirestoreManager.currentUser.value
                val post = CommunityPost(
                    id = UUID.randomUUID().toString(),
                    userId = user?.uid ?: "guest",
                    userName = user?.name?.takeIf { it.isNotBlank() } ?: "VibeWave Listener",
                    userEmail = user?.email ?: "",
                    content = content.trim(),
                    songTitle = songTitle?.takeIf { it.isNotBlank() },
                    songArtist = songArtist?.takeIf { it.isNotBlank() },
                    songId = songId?.takeIf { it.isNotBlank() },
                    rating = rating.coerceIn(1, 5),
                    timestamp = System.currentTimeMillis(),
                )
                db.collection(POSTS_COLLECTION).document(post.id).set(post.toMap()).await()
                ActivityTracker.onCommunityPost(post.songTitle, post.rating, post.content)
                Log.d(TAG, "Community post published: ${post.id}")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to publish community post", t)
            }
        }
    }

    fun likePost(postId: String) {
        val myUid = FirestoreManager.currentUser.value?.uid ?: return
        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val docRef = db.collection(POSTS_COLLECTION).document(postId)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val currentLiked = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val isLiked = currentLiked.contains(myUid)
                    val updated = if (isLiked) currentLiked - myUid else currentLiked + myUid
                    docRef.update(
                        mapOf(
                            "likesCount" to updated.size,
                            "likedBy" to updated,
                        ),
                    ).await()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to like post", t)
            }
        }
    }

    fun deletePost(postId: String) {
        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                db.collection(POSTS_COLLECTION).document(postId).delete().await()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to delete post", t)
            }
        }
    }

    fun openConversation(otherUserId: String) {
        conversationListener?.remove()
        val myUid = FirestoreManager.currentUser.value?.uid ?: return
        val db = FirestoreManager.getFirestoreOrNull() ?: return
        val convId = PrivateMessage.computeConversationId(myUid, otherUserId)

        conversationListener = db.collection(MESSAGES_COLLECTION)
            .whereEqualTo("conversationId", convId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen conversation failed", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val sId = doc.getString("senderId") ?: ""
                    val rId = doc.getString("receiverId") ?: ""
                    PrivateMessage(
                        id = doc.getString("id") ?: doc.id,
                        conversationId = doc.getString("conversationId") ?: convId,
                        senderId = sId,
                        senderName = doc.getString("senderName") ?: "",
                        senderEmail = doc.getString("senderEmail") ?: "",
                        receiverId = rId,
                        receiverName = doc.getString("receiverName") ?: "",
                        receiverEmail = doc.getString("receiverEmail") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        read = doc.getBoolean("read") ?: false,
                    )
                }?.sortedBy { it.timestamp } ?: emptyList()
                _currentConversation.value = messages
            }
    }

    fun sendPrivateMessage(
        receiverId: String,
        receiverName: String,
        receiverEmail: String,
        messageText: String,
    ) {
        if (messageText.isBlank()) return
        val myUser = FirestoreManager.currentUser.value ?: return
        val myUid = myUser.uid.takeIf { it.isNotBlank() } ?: return

        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val convId = PrivateMessage.computeConversationId(myUid, receiverId)
                val msg = PrivateMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    senderId = myUid,
                    senderName = myUser.name.takeIf { it.isNotBlank() } ?: "VibeWave User",
                    senderEmail = myUser.email,
                    receiverId = receiverId,
                    receiverName = receiverName,
                    receiverEmail = receiverEmail,
                    message = messageText.trim(),
                    timestamp = System.currentTimeMillis(),
                    read = false,
                )
                // 1. Save to private_messages collection
                db.collection(MESSAGES_COLLECTION).document(msg.id).set(msg.toMap()).await()

                // 2. Automatically dispatch instant push notification to recipient
                AdminManager.sendAnnouncement(
                    title = "💬 ${msg.senderName}",
                    message = msg.message,
                    type = "DIRECT_MESSAGE",
                    targetUserId = receiverId,
                    targetUserEmail = receiverEmail,
                )
                ActivityTracker.onPrivateMessage(receiverName, msg.message)
                Log.d(TAG, "Private message delivered to $receiverId")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to send private message", t)
            }
        }
    }

    fun loadCommunityUsers() {
        scope.launch {
            try {
                val db = FirestoreManager.getFirestoreOrNull() ?: return@launch
                val myUid = FirestoreManager.currentUser.value?.uid ?: ""
                val snapshot = db.collection("users")
                    .orderBy("lastActive", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val users = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        FirestoreManager.UserProfile(
                            uid = doc.getString("uid") ?: doc.id,
                            name = doc.getString("name") ?: "User",
                            email = doc.getString("email") ?: "",
                            appVersion = doc.getString("appVersion") ?: "",
                            deviceModel = doc.getString("deviceModel") ?: "",
                            lastActive = doc.getLong("lastActive") ?: 0L,
                        )
                    }.getOrNull()
                }.filter { it.uid != myUid }
                _communityUsers.value = users
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load community users", t)
            }
        }
    }
}
