package com.bharatconnect.app.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.presentation.components.NotificationItemSkeleton
import com.bharatconnect.app.core.session.SessionManager
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import com.bharatconnect.app.core.datetime.DateTimeUtils
import io.github.jan.supabase.gotrue.auth

import androidx.activity.compose.BackHandler

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val category: String, // messages, likes, system
    val icon: ImageVector,
    val iconBg: Color,
    var isRead: Boolean = false,
    val conversationId: String? = null,
    val senderId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    chatViewModel: com.bharatconnect.app.presentation.chat.ChatViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var selectedCategory by remember { mutableStateOf("all") }
    val rawNotifications by chatViewModel.notifications.collectAsState()
    val isLoadingNotifications by chatViewModel.isLoadingNotifications.collectAsState()

    LaunchedEffect(Unit) {
        chatViewModel.fetchNotifications()
    }

    val notifications = remember(rawNotifications) {
        val currentUserId = SessionManager.getCachedUserProfile()?.id 
            ?: try { SupabaseClient.client.auth.currentUserOrNull()?.id } catch (_: Exception) { null }
        val currentUserName = SessionManager.getCachedUserProfile()?.fullName 
            ?: SessionManager.getCachedUserProfile()?.username

        rawNotifications
            // 1. Exclude notifications where sender is the current user
            .filter { dto ->
                val isFromMe = (currentUserId != null && dto.senderId != null && dto.senderId == currentUserId) ||
                               (currentUserName != null && dto.title.equals(currentUserName, ignoreCase = true))
                !isFromMe
            }
            // 2. Deduplicate notifications
            .distinctBy { dto ->
                val conv = dto.conversationId ?: dto.senderId ?: dto.title
                val desc = dto.description.trim()
                val time = dto.createdAt?.take(16) ?: ""
                "$conv-$desc-$time"
            }
            .map { dto ->
                val icon = when (dto.category) {
                    "messages" -> Icons.Default.ChatBubble
                    "likes" -> Icons.Default.Favorite
                    else -> Icons.Default.Notifications
                }
                val iconBg = when (dto.category) {
                    "messages" -> ColorPrimary6367FF
                    "likes" -> Color(0xFFFF2D55)
                    else -> Color(0xFF007AFF)
                }

                // 3. Decrypt description if encrypted with AES
                val decryptedDescription = if (dto.description.startsWith("ENC:")) {
                    val fallbackPairId = if (currentUserId != null && !dto.senderId.isNullOrBlank()) {
                        val sorted = listOf(currentUserId, dto.senderId).sorted()
                        java.util.UUID.nameUUIDFromBytes("${sorted[0]}_${sorted[1]}".toByteArray()).toString()
                    } else null
                    SignalEncryptionManager.decrypt(dto.conversationId ?: "", dto.description, fallbackPairId)
                } else {
                    dto.description
                }

                // 4. Format clean local time
                val formattedTime = DateTimeUtils.formatMessageTime(dto.createdAt).ifBlank {
                    dto.createdAt?.take(16) ?: "Just now"
                }

                NotificationItem(
                    id = dto.id ?: "",
                    title = dto.title,
                    description = decryptedDescription,
                    timeAgo = formattedTime,
                    category = dto.category,
                    icon = icon,
                    iconBg = iconBg,
                    isRead = dto.isRead,
                    conversationId = dto.conversationId,
                    senderId = dto.senderId
                )
            }
    }

    val filteredNotifications = notifications.filter {
        selectedCategory == "all" || it.category == selectedCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                chatViewModel.markNotificationsRead()
                            }
                        ) {
                            Text("Mark Read", color = ColorPrimary6367FF, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0D24))
            )
        },
        containerColor = Color(0xFF080616)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "all" to "All",
                    "messages" to "Messages 💬",
                    "likes" to "Likes ❤️",
                    "system" to "System 🔔"
                )

                items(categories) { (key, label) ->
                    val isSelected = selectedCategory == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorPrimary6367FF,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16142E),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            // Notifications List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoadingNotifications && filteredNotifications.isEmpty()) {
                    items(6) {
                        NotificationItemSkeleton()
                    }
                } else if (filteredNotifications.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF221F45)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = ColorPrimary6367FF,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No Notifications",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "You're all caught up! Likes, comments, mentions, and updates will appear here.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNotifications) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) Color(0xFF121026) else Color(0xFF1A173A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                notif.isRead = true
                                val isMessage = notif.category == "messages" || !notif.conversationId.isNullOrBlank() || !notif.senderId.isNullOrBlank()
                                if (isMessage) {
                                    chatViewModel.openChatFromNotification(
                                        senderName = notif.title,
                                        messageSnippet = notif.description,
                                        conversationId = notif.conversationId,
                                        senderId = notif.senderId
                                    ) {
                                        onBack()
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(notif.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = notif.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    color = Color.White,
                                    fontWeight = if (notif.isRead) FontWeight.Medium else FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = notif.description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.timeAgo,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }

                            if (notif.category == "messages") {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = ColorPrimary6367FF.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorPrimary6367FF)
                                ) {
                                    Text(
                                        text = "Chat 💬",
                                        color = ColorPrimary6367FF,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!notif.isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ColorPrimary6367FF)
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
