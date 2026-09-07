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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
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
    var users by remember { mutableStateOf<List<FirestoreManager.UserProfile>>(emptyList()) }
    var globalActivities by remember { mutableStateOf<List<FirestoreManager.ActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedUserForDetail by remember { mutableStateOf<FirestoreManager.UserProfile?>(null) }
    var userActivities by remember { mutableStateOf<List<FirestoreManager.ActivityLog>>(emptyList()) }
    var isUserActivitiesLoading by remember { mutableStateOf(false) }

    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var announcementSent by remember { mutableStateOf(false) }
    var announcementType by remember { mutableStateOf("GENERAL") }
    var isTargetedSend by remember { mutableStateOf(false) }
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
                    // Users List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (users.isEmpty()) {
                            item {
                                Text(
                                    text = "No users recorded in Firestore yet.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }

                        items(users) { user ->
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF00CEC9),
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
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
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = user.deviceModel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.6f),
                                            )
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
                        "WISH" to "🎉 Wish/Greet",
                        "ALERT" to "🚨 Alert",
                        "PROMO" to "🎁 Promo",
                        "UPDATE" to "🔔 Update",
                    )

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
                                text = "Send notifications to all users or a specific individual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
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
                                        text = "Notification Type",
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
                            // Audience Selector (Global vs Targeted)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Audience",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(false to "🌍 All Users", true to "👤 Specific User").forEach { (targeted, label) ->
                                            val selected = isTargetedSend == targeted
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (selected) Color(0xFF00CEC9) else Color(0xFF2A2D3E),
                                                modifier = Modifier.clickable {
                                                    isTargetedSend = targeted
                                                    if (!targeted) {
                                                        targetUserId = ""
                                                        targetUserEmail = ""
                                                    }
                                                },
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    color = if (selected) Color(0xFF0F1018) else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                                                                    text = user.email,
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
                                        text = "✅ Notification sent successfully via Firestore!",
                                        color = Color(0xFF00B894),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // User Detail Modal
    selectedUserForDetail?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForDetail = null },
            title = {
                Text(text = "User Audit: ${user.name}", color = Color.White)
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = "Email: ${user.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00CEC9),
                        )
                        Text(
                            text = "Device: ${user.deviceModel} (App v${user.appVersion})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Location Audit
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF232536),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📍 Latest Location:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF81ECEC),
                                )
                                user.currentLocation?.let { loc ->
                                    Text(
                                        text = "${loc.city}, ${loc.state}, ${loc.country}\nCoordinates: (${loc.latitude}, ${loc.longitude})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Recorded: ${formatTimestamp(loc.timestamp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                } ?: Text(
                                    text = "No current location recorded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "🕒 Previous Location:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFDFE6E9),
                                )
                                user.previousLocation?.let { prev ->
                                    Text(
                                        text = "${prev.city}, ${prev.state}, ${prev.country}\nCoordinates: (${prev.latitude}, ${prev.longitude})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Recorded: ${formatTimestamp(prev.timestamp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                } ?: Text(
                                    text = "No previous location recorded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Activity Log History:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (isUserActivitiesLoading) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF6C5CE7),
                            )
                        }
                    } else {
                        items(userActivities) { act ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2030),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = act.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
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
            containerColor = Color(0xFF1A1B28),
        )
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
