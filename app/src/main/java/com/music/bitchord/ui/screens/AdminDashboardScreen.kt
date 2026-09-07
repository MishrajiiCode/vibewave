package com.music.bitchord.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.AppUpdateChecker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.firebase.AdminManager
import com.music.bitchord.data.firebase.FirestoreManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
) {
    val isAuthenticated by AdminManager.isAuthenticated.collectAsState()
    val scope = rememberCoroutineScope()

    var passcode by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<FirestoreManager.UserProfile>>(emptyList()) }
    var globalActivities by remember { mutableStateOf<List<FirestoreManager.ActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val updateInfo by AppUpdateChecker.available.collectAsState()
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateBroadcastSent by remember { mutableStateOf(false) }

    var selectedUserForDetail by remember { mutableStateOf<FirestoreManager.UserProfile?>(null) }
    var userActivities by remember { mutableStateOf<List<FirestoreManager.ActivityLog>>(emptyList()) }
    var isUserActivitiesLoading by remember { mutableStateOf(false) }

    var userForPersonalMessage by remember { mutableStateOf<FirestoreManager.UserProfile?>(null) }
    var personalMessageTitle by remember { mutableStateOf("") }
    var personalMessageBody by remember { mutableStateOf("") }
    var personalMessageType by remember { mutableStateOf("PERSONAL_WISH") }
    var isSendingPersonalMessage by remember { mutableStateOf(false) }
    var personalMessageSentConfirmation by remember { mutableStateOf(false) }

    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var announcementSent by remember { mutableStateOf(false) }
    var announcementType by remember { mutableStateOf("GENERAL") }
    var isTargetedSend by remember { mutableStateOf(false) }
    var onlyNonUpdatedAudience by remember { mutableStateOf(false) }
    var targetVersionForBroadcast by remember { mutableStateOf<String?>(null) }
    var targetUserId by remember { mutableStateOf("") }
    var targetUserEmail by remember { mutableStateOf("") }
    var showUserPicker by remember { mutableStateOf(false) }
    var sentAnnouncements by remember { mutableStateOf<List<com.music.bitchord.data.firebase.AnnouncementManager.Announcement>>(emptyList()) }
    var isSendingAnnouncement by remember { mutableStateOf(false) }

    val loadData = {
        scope.launch {
            isLoading = true
            users = AdminManager.fetchAllUsers()
            globalActivities = AdminManager.fetchGlobalActivities()
            isLoading = false
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            loadData()
        }
    }

    if (!isAuthenticated) {
        // Admin Login Modal
        AlertDialog(
            onDismissRequest = onBack,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFF6C5CE7),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Admin Access Required", color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter Raj Mishra master credentials or passcode to access live user tracking and activity analytics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = {
                            passcode = it
                            loginError = false
                        },
                        label = { Text("Master Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C5CE7),
                            unfocusedBorderColor = Color(0xFF3B3E52),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                    if (loginError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Invalid credentials. Access denied.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = AdminManager.authenticate(passcode)
                        if (!ok) loginError = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                ) {
                    Text("Unlock Admin")
                }
            },
            dismissButton = {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1A1B28),
        )
        return
    }

    // Authenticated Dashboard Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1018))
            .navigationBarsPadding()
    ) {
        // Top Bar
        Surface(
            color = Color(0xFF161725),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "👑 Admin Console",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Controller: Raj Mishra",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00CEC9),
                        )
                    }
                }

                Row {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF00CEC9),
                        )
                    }
                }
            }
        }

        // Metrics Overview Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdminMetricCard(
                title = "Total Users",
                value = "${users.size}",
                color = Color(0xFF6C5CE7),
                modifier = Modifier.weight(1f),
            )
            AdminMetricCard(
                title = "Total Logs",
                value = "${globalActivities.size}",
                color = Color(0xFF00CEC9),
                modifier = Modifier.weight(1f),
            )
            AdminMetricCard(
                title = "Live Status",
                value = "Active",
                color = Color(0xFF00B894),
                modifier = Modifier.weight(1f),
            )
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161725),
            contentColor = Color(0xFF6C5CE7),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF6C5CE7),
                )
            },
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Users (${users.size})") },
                icon = { Icon(Icons.Rounded.People, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Live Activity") },
                icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Announce") },
                icon = { Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Releases") },
                icon = { Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF6C5CE7))
            }
        } else {
            when (selectedTab) {
                0 -> {
                    // Users List with Search & Filter
                    val filteredUsers = remember(users, userSearchQuery) {
                        if (userSearchQuery.isBlank()) users
                        else users.filter {
                            it.name.contains(userSearchQuery, ignoreCase = true) ||
                            it.email.contains(userSearchQuery, ignoreCase = true) ||
                            (it.currentLocation?.city?.contains(userSearchQuery, ignoreCase = true) == true) ||
                            it.deviceModel.contains(userSearchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            OutlinedTextField(
                                value = userSearchQuery,
                                onValueChange = { userSearchQuery = it },
                                placeholder = { Text("Search users by name, email, city...", color = Color.White.copy(alpha = 0.5f)) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF00CEC9)) },
                                trailingIcon = {
                                    if (userSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { userSearchQuery = "" }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6C5CE7),
                                    unfocusedBorderColor = Color(0xFF2A2D3E),
                                    focusedContainerColor = Color(0xFF161725),
                                    unfocusedContainerColor = Color(0xFF161725),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )
                        }

                        if (filteredUsers.isEmpty()) {
                            item {
                                Text(
                                    text = if (userSearchQuery.isNotBlank()) "No users matching \"$userSearchQuery\"" else "No users recorded in Firestore yet.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }

                        items(filteredUsers) { user ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedUserForDetail = user
                                        scope.launch {
                                            isUserActivitiesLoading = true
                                            userActivities = AdminManager.fetchUserActivities(user.uid)
                                            isUserActivitiesLoading = false
                                        }
                                    },
                                color = Color(0xFF1E2030),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                        Text(
                                            text = formatTimestamp(user.lastActive),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF81ECEC),
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocationOn,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00CEC9),
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = user.currentLocation?.city?.ifBlank { "Unknown" } ?: "Not Set",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PhoneAndroid,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = user.deviceModel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        // Quick Action Button: Message this user
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                                            modifier = Modifier.clickable {
                                                userForPersonalMessage = user
                                                personalMessageTitle = ""
                                                personalMessageBody = ""
                                                personalMessageSentConfirmation = false
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Send,
                                                    contentDescription = "Message",
                                                    tint = Color(0xFFA29BFE),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "Message",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFA29BFE)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Global Live Activity Feed
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (globalActivities.isEmpty()) {
                            item {
                                Text(
                                    text = "No activities logged yet.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }

                        items(globalActivities) { log ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = Color(0xFF1C1E2D),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6C5CE7).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = log.activityType.take(2),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF6C5CE7),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = log.userName.ifBlank { "User" },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                            )
                                            Text(
                                                text = formatTimestamp(log.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.4f),
                                            )
                                        }
                                        Text(
                                            text = log.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                        )
                                        if (log.details.isNotBlank()) {
                                            Text(
                                                text = log.details,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Enhanced Announcement & Notification Center
                    val notificationTypes = listOf(
                        "GENERAL" to "📢 General",
                        "FESTIVAL_WISH" to "🪔 Festivals",
                        "ROMANTIC_VIBES" to "💖 Romantic",
                        "APP_UPDATE" to "🚀 App Update",
                        "MOTIVATION" to "⚡ Motivation",
                        "AI_PICKS" to "✨ AI Picks",
                        "WISH" to "🎉 Wish/Greet",
                        "ALERT" to "🚨 Alert",
                    )

                    var selectedPresetTab by remember { mutableStateOf("FESTIVALS") }

                    val presets = remember {
                        listOf(
                            // Festivals
                            Triple("Diwali 🪔", "🪔 Happy Diwali from VibeWave!", "May the festival of lights illuminate your life with joy, prosperity, and melodious beats! Keep rocking on VibeWave ✨🪔") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("Eid 🌙", "🌙 Eid Mubarak from VibeWave!", "Wishing you and your loved ones abundant peace, happiness, and harmonious melodies on this blessed day! 🌙💫") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("Holi 🎨", "🎨 Happy Holi from VibeWave!", "Fill your world with vibrant colors of celebration and groove to electrifying festive tunes! 🌈🎵") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("Christmas 🎄", "🎄 Merry Christmas from VibeWave!", "Wishing you a warm, joyous season filled with smiles and your favorite holiday soundtracks! 🎅🎶") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("New Year ✨", "✨ Happy New Year 2026!", "Cheers to another year of unforgettable melodies, late-night acoustic sessions, and pure rhythm! 🥂🎉") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("Navratri 🌸", "🌸 Shubh Navratri!", "Celebrate the divine rhythms and dance along to energetic festive Garba melodies with VibeWave! 🥁💫") to ("FESTIVALS" to "FESTIVAL_WISH"),
                            Triple("Independence Day 🇮🇳", "🇮🇳 Happy Independence Day!", "Celebrating the spirit of freedom, unity, and pride with patriotic melodies! Jai Hind! 🇮🇳✨") to ("FESTIVALS" to "FESTIVAL_WISH"),

                            // Romantic & Late Night
                            Triple("Late Night Acoustics 🌙", "🌙 Late Night Acoustics for You", "Deep thoughts or quiet moments? Put on your headphones and let these gentle melodies keep you company 💕") to ("ROMANTIC" to "ROMANTIC_VIBES"),
                            Triple("Monsoon Romance 🌧️", "🌧️ Rainy Day Romantic Melodies", "Hot coffee, raindrops tapping on the glass, and heartfelt acoustic chords. Tap to immerse ☕☔") to ("ROMANTIC" to "ROMANTIC_VIBES"),
                            Triple("Special Someone ❤️", "❤️ For Someone Special", "When words fall short, melody speaks. Dedicate a song to the one who makes your heart flutter ✨") to ("ROMANTIC" to "ROMANTIC_VIBES"),
                            Triple("Midnight Serenade 💌", "💌 Midnight Serenade", "A soothing romantic mix curated especially for late-night dreamers and starry souls 🎧💫") to ("ROMANTIC" to "ROMANTIC_VIBES"),
                            Triple("Nostalgic Love 🍂", "🍂 Nostalgic Love Chords", "Relive your sweetest memories with timeless golden melodies that never fade 💖") to ("ROMANTIC" to "ROMANTIC_VIBES"),

                            // Updates
                            Triple("v1.5.5 Ready 🚀", "🚀 Fresh VibeWave v1.5.5 Ready!", "A faster release with categorized settings, deep AI Picks tracking, and sleek UI. Tap to install now! ⚡") to ("UPDATES" to "APP_UPDATE"),
                            Triple("Performance Boost ⚡", "⚡ Performance & Stability Update v1.5.5", "Resolved update loops, enhanced audio precision, and updated music engine. Tap to update with 1 click!") to ("UPDATES" to "APP_UPDATE"),

                            // Motivation & Chill
                            Triple("Morning Surge ⚡", "⚡ Good Morning! Energy Surge", "Kick off your day with unstoppable momentum and high-vibe tunes! ☀️🚀") to ("MOTIVATION" to "MOTIVATION"),
                            Triple("Weekend Chill 🍃", "🍃 Weekend Chillout Session", "Unwind, breathe easy, and let the acoustic breeze wash away the week's tension 🏖️🎵") to ("MOTIVATION" to "GENERAL"),
                            Triple("Workout Fuel 🔥", "🔥 Workout Beast Mode", "Pump up the volume and crush your fitness goals with relentless rhythm! 🥊💪") to ("MOTIVATION" to "MOTIVATION"),

                            // AI Picks
                            Triple("AI Picks Refreshed ✨", "✨ Your AI Picks Just Got Smarter!", "Based on what you've been playing and liking, AI has tuned your personal picks playlist with fresh hidden gems! Tap to listen 🎶") to ("AI_PICKS" to "AI_PICKS"),
                        )
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            Text(
                                text = "📣 Notification Center",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "Broadcast festival greetings, romantic vibes, and updates with 1 tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }

                        // 1-Tap Quick Presets Section
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "⚡ 1-Tap Quick Presets",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Tap any to autofill",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF00CEC9),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Category tabs for presets
                                    val presetCategories = listOf(
                                        "FESTIVALS" to "🪔 Festivals",
                                        "ROMANTIC" to "💖 Romantic",
                                        "UPDATES" to "🚀 Updates",
                                        "MOTIVATION" to "⚡ Motivation",
                                        "AI_PICKS" to "✨ AI Picks",
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        items(presetCategories) { (catKey, catLabel) ->
                                            val isSelected = selectedPresetTab == catKey
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFF252736),
                                                modifier = Modifier.clickable { selectedPresetTab = catKey },
                                            ) {
                                                Text(
                                                    text = catLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Filtered presets for selected category
                                    val currentCategoryPresets = remember(selectedPresetTab) {
                                        presets.filter { it.second.first == selectedPresetTab }
                                    }

                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(currentCategoryPresets) { (presetData, meta) ->
                                            val (chipTitle, title, body) = presetData
                                            val (_, type) = meta
                                            val isCurrent = announcementTitle == title
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isCurrent) Color(0xFF6C5CE7).copy(alpha = 0.25f) else Color(0xFF262838),
                                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C5CE7)) else null,
                                                modifier = Modifier
                                                    .width(220.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        announcementTitle = title
                                                        announcementBody = body
                                                        announcementType = type
                                                        if (type == "APP_UPDATE") {
                                                            onlyNonUpdatedAudience = true
                                                            targetVersionForBroadcast = "1.5.5"
                                                        } else {
                                                            onlyNonUpdatedAudience = false
                                                        }
                                                    },
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = chipTitle,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isCurrent) Color(0xFF81ECEC) else Color.White,
                                                        maxLines = 1,
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = body,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Notification Type Selector
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Notification Category",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(notificationTypes) { (type, label) ->
                                            val selected = announcementType == type
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (selected) Color(0xFF6C5CE7) else Color(0xFF2A2D3E),
                                                modifier = Modifier.clickable { announcementType = type },
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Audience Selector (Global vs Non-Updated vs Targeted)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Target Audience",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val audienceModes = listOf(
                                            "ALL" to "🌍 All Users",
                                            "NON_UPDATED" to "🚀 Non-Updated (< v1.5.5)",
                                            "SPECIFIC" to "👤 Specific User",
                                        )
                                        audienceModes.forEach { (mode, label) ->
                                            val isSelected = when (mode) {
                                                "ALL" -> !isTargetedSend && !onlyNonUpdatedAudience
                                                "NON_UPDATED" -> !isTargetedSend && onlyNonUpdatedAudience
                                                "SPECIFIC" -> isTargetedSend
                                                else -> false
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) Color(0xFF00CEC9) else Color(0xFF2A2D3E),
                                                modifier = Modifier.clickable {
                                                    when (mode) {
                                                        "ALL" -> {
                                                            isTargetedSend = false
                                                            onlyNonUpdatedAudience = false
                                                            targetUserId = ""
                                                            targetUserEmail = ""
                                                        }
                                                        "NON_UPDATED" -> {
                                                            isTargetedSend = false
                                                            onlyNonUpdatedAudience = true
                                                            targetVersionForBroadcast = "1.5.5"
                                                            targetUserId = ""
                                                            targetUserEmail = ""
                                                        }
                                                        "SPECIFIC" -> {
                                                            isTargetedSend = true
                                                            onlyNonUpdatedAudience = false
                                                        }
                                                    }
                                                },
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    color = if (isSelected) Color(0xFF0F1018) else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                )
                                            }
                                        }
                                    }

                                    if (onlyNonUpdatedAudience) {
                                        val nonUpdatedCount = remember(users) {
                                            users.count { com.music.bitchord.data.AppUpdateChecker.isNewer("1.5.5", it.appVersion.removePrefix("v")) }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF00CEC9).copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Rounded.Upgrade, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Smart Target: Only users on older versions (< v1.5.5) will receive this alert ($nonUpdatedCount users). Updated users will be excluded.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF81ECEC),
                                                )
                                            }
                                        }
                                    }

                                    if (isTargetedSend) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Select Target User:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (targetUserId.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF6C5CE7).copy(alpha = 0.15f),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = targetUserEmail,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color(0xFF81ECEC),
                                                        )
                                                        Text(
                                                            text = "UID: $targetUserId",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White.copy(alpha = 0.5f),
                                                        )
                                                    }
                                                    androidx.compose.material3.TextButton(onClick = {
                                                        targetUserId = ""
                                                        targetUserEmail = ""
                                                    }) {
                                                        Text("Clear", color = Color(0xFFFF7675), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        } else {
                                            // Show a scrollable user list to pick from
                                            if (users.isEmpty()) {
                                                Text(
                                                    text = "No users loaded. Refresh the Users tab first.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                )
                                            } else {
                                                users.take(5).forEach { user ->
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0xFF2A2D3E),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp)
                                                            .clickable {
                                                                targetUserId = user.uid
                                                                targetUserEmail = user.email
                                                            },
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Column {
                                                                Text(
                                                                    text = user.name,
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                    color = Color.White,
                                                                )
                                                                Text(
                                                                    text = "${user.email} • v${user.appVersion}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFF81ECEC),
                                                                )
                                                            }
                                                            Text("Select", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C5CE7))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Compose Message
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Compose Message",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = announcementTitle,
                                        onValueChange = { announcementTitle = it },
                                        label = { Text("Notification Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6C5CE7),
                                            unfocusedBorderColor = Color(0xFF3B3E52),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF6C5CE7),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        ),
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = announcementBody,
                                        onValueChange = { announcementBody = it },
                                        label = { Text("Message Body") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6C5CE7),
                                            unfocusedBorderColor = Color(0xFF3B3E52),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFF6C5CE7),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        ),
                                    )
                                }
                            }
                        }

                        item {
                            // Send Button
                            val buttonLabel = when {
                                isSendingAnnouncement -> "Sending..."
                                isTargetedSend && targetUserId.isNotBlank() -> "📨 Send to ${targetUserEmail.take(20)}..."
                                isTargetedSend -> "Select a user first"
                                onlyNonUpdatedAudience -> "🚀 Alert Non-Updated Users Only (< v1.5.5)"
                                else -> "📢 Broadcast to All Users"
                            }
                            val canSend = announcementTitle.isNotBlank() && announcementBody.isNotBlank() &&
                                    (!isTargetedSend || targetUserId.isNotBlank()) && !isSendingAnnouncement

                            Button(
                                onClick = {
                                    if (canSend) {
                                        scope.launch {
                                            isSendingAnnouncement = true
                                            val success = AdminManager.sendAnnouncement(
                                                title = announcementTitle,
                                                message = announcementBody,
                                                type = announcementType,
                                                targetUserId = if (isTargetedSend) targetUserId else null,
                                                targetUserEmail = if (isTargetedSend) targetUserEmail else null,
                                                targetVersion = if (onlyNonUpdatedAudience || announcementType == "APP_UPDATE") (targetVersionForBroadcast ?: "1.5.5") else null,
                                                onlyNonUpdated = onlyNonUpdatedAudience || announcementType == "APP_UPDATE",
                                            )
                                            isSendingAnnouncement = false
                                            if (success) {
                                                announcementSent = true
                                                announcementTitle = ""
                                                announcementBody = ""
                                                if (isTargetedSend) {
                                                    targetUserId = ""
                                                    targetUserEmail = ""
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = canSend,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6C5CE7),
                                    disabledContainerColor = Color(0xFF6C5CE7).copy(alpha = 0.4f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isSendingAnnouncement) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                            }

                            if (announcementSent) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF00B894).copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "✅ Notification broadcasted successfully via Firestore!",
                                        color = Color(0xFF00B894),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // App Releases & In-App Update Management
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            Text(
                                text = "🚀 Release & Update Center",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "Manage GitHub releases and push instant in-app update notifications to all users.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }

                        item {
                            // Current Installed Build Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = "Current App Version",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.6f),
                                            )
                                            Text(
                                                text = "v${BuildConfig.VERSION_NAME}",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF00CEC9),
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF00B894).copy(alpha = 0.2f),
                                        ) {
                                            Text(
                                                text = "PRODUCTION BUILD",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF00B894),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // GitHub Latest Release Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "GitHub Release Status",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val latest = updateInfo
                                    if (latest != null) {
                                        Text(
                                            text = "Latest Release: v${latest.version}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF7C4DFF),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "APK Download: ${if (latest.apkUrl != null) "Ready for direct in-app install ✅" else "Release page only"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                        if (!latest.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF161725),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(
                                                    text = latest.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    maxLines = 8,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(10.dp),
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No newer release detected on GitHub.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isCheckingUpdates = true
                                                    AppUpdateChecker.check()
                                                    isCheckingUpdates = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D3E)),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            if (isCheckingUpdates) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Check GitHub", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Broadcast Update Alert Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Smart Broadcast Update Notification",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    val targetVer = updateInfo?.version ?: BuildConfig.VERSION_NAME
                                    val nonUpdatedCount = remember(users, targetVer) {
                                        users.count { com.music.bitchord.data.AppUpdateChecker.isNewer(targetVer, it.appVersion.removePrefix("v")) }
                                    }
                                    Text(
                                        text = "Smart Delivery: Only non-updated devices (< v$targetVer) receive this notification ($nonUpdatedCount users). Users already running v$targetVer are automatically excluded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF81ECEC),
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                                    )

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                AdminManager.sendAnnouncement(
                                                    title = "New VibeWave Update v$targetVer Ready! 🚀",
                                                    message = "A fresh update is available! Tap here to view the full changelog and install it directly in the app.",
                                                    type = "APP_UPDATE",
                                                    targetVersion = targetVer,
                                                    onlyNonUpdated = true,
                                                )
                                                updateBroadcastSent = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Alert Non-Updated Users (< v$targetVer) 🚀")
                                    }

                                    if (updateBroadcastSent) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF00B894).copy(alpha = 0.2f),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = "✅ Update notification broadcasted to non-updated users (< v$targetVer)!",
                                                color = Color(0xFF00B894),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(10.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Highly Structured User Audit Modal
    selectedUserForDetail?.let { user ->
        val telem = user.telemetry
        val taste = user.tasteProfile

        AlertDialog(
            onDismissRequest = { selectedUserForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6C5CE7).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFA29BFE),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user.name.ifBlank { "User Audit" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00CEC9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(onClick = { selectedUserForDetail = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 1. Direct Message Action Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF6C5CE7).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Send Direct Message ✉️",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFA29BFE),
                                    )
                                    Text(
                                        text = "Notification will pop up on ${user.name.split(" ").firstOrNull() ?: "user"}'s device",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                    )
                                }
                                Button(
                                    onClick = {
                                        userForPersonalMessage = user
                                        personalMessageTitle = ""
                                        personalMessageBody = ""
                                        personalMessageSentConfirmation = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compose", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    // 2. Battery & Power Status Card
                    item {
                        val batteryLevel = telem?.batteryPercentage ?: -1
                        val isCharging = telem?.isCharging ?: false
                        val battStatus = telem?.batteryStatus ?: "Unknown"
                        val battHealth = telem?.batteryHealth ?: "Good"
                        val battTemp = telem?.batteryTemperatureC ?: 0f

                        val battColor = when {
                            batteryLevel >= 50 -> Color(0xFF00B894)
                            batteryLevel >= 20 -> Color(0xFFFDCB6E)
                            batteryLevel >= 0 -> Color(0xFFFF7675)
                            else -> Color.White.copy(alpha = 0.5f)
                        }

                        AuditSectionCard(
                            title = "Power & Battery Telemetry",
                            icon = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                            iconTint = battColor,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(
                                    label = "Battery Level",
                                    value = if (batteryLevel >= 0) "$batteryLevel% ${if (isCharging) "⚡ Charging" else "🔋 On Battery"}" else "Unknown",
                                    valueColor = battColor,
                                )
                                AuditKeyValue(
                                    label = "Status",
                                    value = battStatus,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(
                                    label = "Health",
                                    value = battHealth,
                                )
                                AuditKeyValue(
                                    label = "Temperature",
                                    value = if (battTemp > 0) String.format(Locale.US, "%.1f °C", battTemp) else "Normal",
                                )
                            }
                        }
                    }

                    // 3. Hardware & OS Specifications Card
                    item {
                        AuditSectionCard(
                            title = "Device & Hardware Specs",
                            icon = Icons.Rounded.PhoneAndroid,
                            iconTint = Color(0xFF00CEC9),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(label = "Device Model", value = user.deviceModel)
                                AuditKeyValue(label = "Manufacturer", value = telem?.manufacturer ?: "Android")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(
                                    label = "Android OS",
                                    value = "v${telem?.androidRelease ?: "14"} (SDK ${telem?.sdkInt ?: 34})",
                                    valueColor = Color(0xFF81ECEC),
                                )
                                AuditKeyValue(label = "App Version", value = "v${user.appVersion}")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(label = "Chipset/HW", value = telem?.hardware?.ifBlank { "Universal" } ?: "Universal")
                                AuditKeyValue(label = "Resolution", value = telem?.screenResolution?.ifBlank { "Standard" } ?: "Standard")
                            }
                            if (!telem?.buildId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AuditKeyValue(label = "Build ID", value = telem?.buildId.orEmpty(), valueColor = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // 4. Permissions Audit Grid Card
                    item {
                        AuditSectionCard(
                            title = "Permissions Audit Matrix",
                            icon = Icons.Rounded.Security,
                            iconTint = Color(0xFFA29BFE),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AuditPermissionChip(
                                    name = "🎤 Mic",
                                    status = telem?.microphonePermission ?: "DENIED",
                                    modifier = Modifier.weight(1f),
                                )
                                AuditPermissionChip(
                                    name = "📍 Location",
                                    status = telem?.locationPermission ?: "DENIED",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AuditPermissionChip(
                                    name = "🔔 Notification",
                                    status = telem?.notificationPermission ?: "DENIED",
                                    modifier = Modifier.weight(1f),
                                )
                                AuditPermissionChip(
                                    name = "🎵 Audio Media",
                                    status = telem?.mediaAudioPermission ?: "DENIED",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // 5. Geographical & Network Footprint Card
                    item {
                        AuditSectionCard(
                            title = "Location & Network Footprint",
                            icon = Icons.Rounded.LocationOn,
                            iconTint = Color(0xFFFF7675),
                        ) {
                            Text(
                                text = "📍 Latest Location:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF81ECEC),
                            )
                            user.currentLocation?.let { loc ->
                                Text(
                                    text = "${loc.city}, ${loc.state}, ${loc.country}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Coordinates: (${loc.latitude}, ${loc.longitude})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = "Logged: ${formatTimestamp(loc.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                )
                            } ?: Text(
                                text = "No location permission granted / not logged.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🕒 Previous Location:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFDFE6E9),
                            )
                            user.previousLocation?.let { prev ->
                                Text(
                                    text = "${prev.city}, ${prev.state}, ${prev.country} (${prev.latitude}, ${prev.longitude})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            } ?: Text(
                                text = "No previous location recorded.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AuditKeyValue(label = "Network", value = telem?.networkType ?: "UNKNOWN")
                                AuditKeyValue(label = "Timezone", value = telem?.timeZone ?: "UTC")
                            }
                        }
                    }

                    // 6. Music Taste & Listening Habits Card
                    item {
                        AuditSectionCard(
                            title = "Taste Profile & Listening Habits",
                            icon = Icons.Rounded.MusicNote,
                            iconTint = Color(0xFFFD79A8),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${taste?.playCount ?: 0}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF00CEC9),
                                    )
                                    Text("Plays", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${taste?.favoriteCount ?: 0}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFD79A8),
                                    )
                                    Text("Favorites", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${taste?.skipCount ?: 0}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFF7675),
                                    )
                                    Text("Skips", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                            }

                            if (!taste?.topArtists.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Top Artists: " + taste?.topArtists?.keys?.take(5)?.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                            if (!taste?.favoriteSongTitles.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Favorites: " + taste?.favoriteSongTitles?.take(3)?.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }

                    // 7. Activity History Timeline
                    item {
                        Text(
                            text = "Activity History Timeline:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (isUserActivitiesLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF6C5CE7))
                            }
                        }
                    } else if (userActivities.isEmpty()) {
                        item {
                            Text(
                                text = "No recorded activity events for this user.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        items(userActivities) { act ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = act.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                        Text(
                                            text = formatTimestamp(act.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.4f),
                                        )
                                    }
                                    if (act.details.isNotBlank()) {
                                        Text(
                                            text = act.details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedUserForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                ) {
                    Text("Close")
                }
            },
            containerColor = Color(0xFF161725),
        )
    }

    // Direct Personalized Message Dialog (Admin to User)
    userForPersonalMessage?.let { targetUser ->
        val quickPresets = listOf(
            Triple("🎉 Festive Wish", "🎉 Festive Greetings from VibeWave!", "Wishing you and your family abundant happiness, joy, and peace! Keep the music playing on VibeWave 🎵✨"),
            Triple("🎂 Birthday Wish", "🎂 Happy Birthday from Raj Mishra!", "Wishing you a rocking birthday filled with great melodies and unforgettable beats! 🎶🎂"),
            Triple("🎵 Song Rec", "🎵 Specially Recommended For You", "Hey ${targetUser.name.split(" ").firstOrNull() ?: ""}! Based on your listening taste, we picked an amazing song for you. Tap to listen! 🎧"),
            Triple("💖 Personal Note", "💖 A Note from Raj Mishra", "Thank you for being an active listener on VibeWave! If you ever need anything or have feedback, I am just a message away."),
            Triple("🚀 App Update", "🚀 Fresh Update Available!", "A brand new update of VibeWave is ready for you with enhanced performance and sleek new design. Check it out!"),
            Triple("⚠️ Notice", "⚠️ Notice from VibeWave Team", "Important update regarding your account. Please review your settings."),
        )

        AlertDialog(
            onDismissRequest = {
                userForPersonalMessage = null
                personalMessageSentConfirmation = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C5CE7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = null,
                            tint = Color(0xFFA29BFE),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Direct Message",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "To: ${targetUser.name.ifBlank { "User" }} (${targetUser.email})",
                            color = Color(0xFF00CEC9),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(quickPresets) { (chipLabel, presetTitle, presetBody) ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (personalMessageTitle == presetTitle) Color(0xFF6C5CE7) else Color(0xFF232536),
                                    modifier = Modifier.clickable {
                                        personalMessageTitle = presetTitle
                                        personalMessageBody = presetBody
                                    }
                                ) {
                                    Text(
                                        text = chipLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (personalMessageTitle == presetTitle) Color.White else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = personalMessageTitle,
                            onValueChange = { personalMessageTitle = it },
                            label = { Text("Notification Title") },
                            placeholder = { Text("e.g. 🎉 Festive Greetings from Raj Mishra!") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2A2D3E),
                                focusedContainerColor = Color(0xFF161725),
                                unfocusedContainerColor = Color(0xFF161725),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = personalMessageBody,
                            onValueChange = { personalMessageBody = it },
                            label = { Text("Message Body") },
                            placeholder = { Text("Enter personal message that will appear as a notification...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color(0xFF2A2D3E),
                                focusedContainerColor = Color(0xFF161725),
                                unfocusedContainerColor = Color(0xFF161725),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )
                    }

                    if (personalMessageSentConfirmation) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00B894).copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "✅ Notification sent! It will pop up immediately on ${targetUser.name}'s phone.",
                                    color = Color(0xFF00B894),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (personalMessageTitle.isNotBlank() && personalMessageBody.isNotBlank()) {
                            scope.launch {
                                isSendingPersonalMessage = true
                                val success = AdminManager.sendAnnouncement(
                                    title = personalMessageTitle.trim(),
                                    message = personalMessageBody.trim(),
                                    type = "PERSONAL_WISH",
                                    targetUserId = targetUser.uid,
                                    targetUserEmail = targetUser.email,
                                )
                                isSendingPersonalMessage = false
                                if (success) {
                                    personalMessageSentConfirmation = true
                                }
                            }
                        }
                    },
                    enabled = !isSendingPersonalMessage && personalMessageTitle.isNotBlank() && personalMessageBody.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                ) {
                    if (isSendingPersonalMessage) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Notification")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        userForPersonalMessage = null
                        personalMessageSentConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232536)),
                ) {
                    Text("Close", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF161725),
        )
    }
}

@Composable
private fun AuditSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2030),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            content()
        }
    }
}

@Composable
private fun AuditKeyValue(
    label: String,
    value: String,
    valueColor: Color = Color.White,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
        )
    }
}

@Composable
private fun AuditPermissionChip(
    name: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    val isGranted = status.contains("GRANTED", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isGranted) Color(0xFF00B894).copy(alpha = 0.15f) else Color(0xFFFF7675).copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
            Text(
                text = if (isGranted) "GRANTED" else "DENIED",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isGranted) Color(0xFF00B894) else Color(0xFFFF7675),
            )
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = Color(0xFF1E2030),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

private fun formatTimestamp(time: Long): String {
    if (time <= 0) return "Never"
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}
