package com.music.vibewave.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.vibewave.data.firebase.CommunityManager
import com.music.vibewave.data.firebase.FirestoreManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunitySheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by CommunityManager.posts.collectAsStateWithLifecycle()
    val communityUsers by CommunityManager.communityUsers.collectAsStateWithLifecycle()
    val currentUser by FirestoreManager.currentUser.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Feed, 1 = Direct Messages
    var activeChatUser by remember { mutableStateOf<FirestoreManager.UserProfile?>(null) }

    LaunchedEffect(Unit) {
        CommunityManager.loadCommunityUsers()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0F1018),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = Color(0xFF161826),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = Color(0xFF81ECEC),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VibeWave Community",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "Live Reviews & WhatsApp-Style Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF81ECEC).copy(alpha = 0.8f),
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Top Tab Navigation (Feed vs Direct Messages)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val tabs = listOf("🌐 Community Feed & Reviews", "💬 Direct Messages (1-on-1)")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFF1F2233),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = index
                                if (index == 0) activeChatUser = null
                            },
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Content Body
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    CommunityFeedView(
                        posts = posts,
                        currentUserId = currentUser?.uid ?: "",
                    )
                } else {
                    if (activeChatUser == null) {
                        CommunityUsersListView(
                            users = communityUsers,
                            onUserClick = { targetUser ->
                                activeChatUser = targetUser
                                CommunityManager.openConversation(targetUser.uid)
                            },
                        )
                    } else {
                        PrivateChatThreadView(
                            otherUser = activeChatUser!!,
                            currentUserId = currentUser?.uid ?: "",
                            onBack = { activeChatUser = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityFeedView(
    posts: List<CommunityManager.CommunityPost>,
    currentUserId: String,
) {
    var newPostContent by remember { mutableStateOf("") }
    var newPostSongTitle by remember { mutableStateOf("") }
    var newPostArtist by remember { mutableStateOf("") }
    var newPostRating by remember { mutableIntStateOf(5) }
    var isPosting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            // Compose Review Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181A28),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Share Music Review or Thought",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        placeholder = { Text("What are you listening to? Write your review or thoughts...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C5CE7),
                            unfocusedBorderColor = Color(0xFF2E3248),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newPostSongTitle,
                            onValueChange = { newPostSongTitle = it },
                            placeholder = { Text("Song (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2E3248),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            ),
                        )
                        OutlinedTextField(
                            value = newPostArtist,
                            onValueChange = { newPostArtist = it },
                            placeholder = { Text("Artist (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2E3248),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Star Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rating:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            (1..5).forEach { star ->
                                val isFilled = star <= newPostRating
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = if (isFilled) Color(0xFFFFD166) else Color(0xFF383B52),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { newPostRating = star },
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newPostContent.isNotBlank()) {
                                    isPosting = true
                                    CommunityManager.postReview(
                                        content = newPostContent,
                                        songTitle = newPostSongTitle,
                                        songArtist = newPostArtist,
                                        rating = newPostRating,
                                    )
                                    newPostContent = ""
                                    newPostSongTitle = ""
                                    newPostArtist = ""
                                    isPosting = false
                                }
                            },
                            enabled = newPostContent.isNotBlank() && !isPosting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Post 🚀", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No community posts yet. Be the first to share a review!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                CommunityPostCard(
                    post = post,
                    isMyPost = post.userId == currentUserId,
                    onLikeClick = { CommunityManager.likePost(post.id) },
                    onDeleteClick = { CommunityManager.deletePost(post.id) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityManager.CommunityPost,
    isMyPost: Boolean,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val isLiked = remember(post.likedBy) { post.likedBy.isNotEmpty() }
    val timeAgo = remember(post.timestamp) {
        val diff = System.currentTimeMillis() - post.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(post.timestamp))
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1D2C),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6C5CE7).copy(alpha = 0.35f),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = post.userName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF81ECEC),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }

                // Rating stars
                Row {
                    (1..post.rating).forEach {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Song pill if attached
            if (!post.songTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF26293D),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF00CEC9),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.songTitle} ${post.songArtist?.let { "• $it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF81ECEC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Like count + actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() },
                ) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF7675) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount} likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }

                if (isMyPost) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF7675).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityUsersListView(
    users: List<FirestoreManager.UserProfile>,
    onUserClick: (FirestoreManager.UserProfile) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Search user
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users to message...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6C5CE7),
                unfocusedBorderColor = Color(0xFF2E3248),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(14.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No other listeners found. Invite a friend!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                items(filtered, key = { it.uid }) { user ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1C1E2E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(user) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00CEC9).copy(alpha = 0.25f),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF00CEC9),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = user.email.takeIf { it.isNotBlank() } ?: "VibeWave User",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.ChatBubble,
                                        contentDescription = null,
                                        tint = Color(0xFF6C5CE7),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chat", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C5CE7))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PrivateChatThreadView(
    otherUser: FirestoreManager.UserProfile,
    currentUserId: String,
    onBack: () -> Unit,
) {
    val messages by CommunityManager.currentConversation.collectAsStateWithLifecycle()
    var typedText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Chat Header
        Surface(
            color = Color(0xFF181A28),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6C5CE7).copy(alpha = 0.3f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherUser.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF81ECEC),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = otherUser.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Private 1-on-1 Chat",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF81ECEC),
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No messages yet. Say hi to ${otherUser.name}! 👋",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUserId
                    val timeStr = remember(msg.timestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isMe) 14.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 14.dp,
                            ),
                            color = if (isMe) Color(0xFF6C5CE7) else Color(0xFF222538),
                            modifier = Modifier.widthIn(max = 280.dp),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = msg.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.6f),
                                    )
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "✓✓",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color(0xFF81ECEC),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = Color(0xFF161826),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF2E3248),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(22.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (typedText.isNotBlank()) {
                            val textToSend = typedText
                            typedText = ""
                            CommunityManager.sendPrivateMessage(
                                receiverId = otherUser.uid,
                                receiverName = otherUser.name,
                                receiverEmail = otherUser.email,
                                messageText = textToSend,
                            )
                        }
                    },
                    enabled = typedText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (typedText.isNotBlank()) Color(0xFF6C5CE7) else Color(0xFF272A3C)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (typedText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
