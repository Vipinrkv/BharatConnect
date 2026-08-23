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

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val category: String, // messages, likes, system
    val icon: ImageVector,
    val iconBg: Color,
    var isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("all") }

    val notifications = remember {
        mutableStateListOf(
            NotificationItem(
                "1",
                "Emma Watson started following you.",
                "Check out her profile and recent posts.",
                "5m ago",
                "system",
                Icons.Default.Person,
                ColorPrimary6367FF,
                false
            ),
            NotificationItem(
                "2",
                "Alice Johnson liked your post.",
                "\"Building offline-first Sentinel encryption in Kotlin!\"",
                "1h ago",
                "likes",
                Icons.Default.Favorite,
                Color(0xFFFF4B6B),
                false
            ),
            NotificationItem(
                "3",
                "Michael Brown in Project Team",
                "\"Awesome architecture! When are we deploying to Play Store?\"",
                "2h ago",
                "messages",
                Icons.Default.ChatBubble,
                Color(0xFF00E5FF),
                false
            ),
            NotificationItem(
                "4",
                "Welcome to BharatConnect 2.0!",
                "Enjoy offline SQLite sync, Supabase Realtime, and Sentinel 7-layer security.",
                "1d ago",
                "system",
                Icons.Default.Celebration,
                Color(0xFF138808),
                true
            ),
            NotificationItem(
                "5",
                "Karan Malhotra liked your listing.",
                "\"Dell UltraSharp 27 4K Monitor\"",
                "1d ago",
                "likes",
                Icons.Default.Favorite,
                Color(0xFFFF4B6B),
                true
            )
        )
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
                    TextButton(
                        onClick = {
                            notifications.forEach { it.isRead = true }
                        }
                    ) {
                        Text("Mark Read", color = ColorPrimary6367FF, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                items(filteredNotifications) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) Color(0xFF121026) else Color(0xFF1A173A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notif.isRead = true }
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

                            if (!notif.isRead) {
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
