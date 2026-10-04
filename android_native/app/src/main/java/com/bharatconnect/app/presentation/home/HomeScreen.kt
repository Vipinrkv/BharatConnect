package com.bharatconnect.app.presentation.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.presentation.auth.AuthViewModel
import com.bharatconnect.app.presentation.chat.ChatDetailScreen
import com.bharatconnect.app.presentation.chat.ChatViewModel
import com.bharatconnect.app.presentation.chat.ChatsTab
import com.bharatconnect.app.presentation.feed.FeedTab
import com.bharatconnect.app.presentation.feed.FeedViewModel
import com.bharatconnect.app.presentation.marketplace.MarketplaceScreen
import com.bharatconnect.app.presentation.nearby.NearbyScreen
import com.bharatconnect.app.presentation.notifications.NotificationsScreen
import com.bharatconnect.app.presentation.profile.ProfileTab
import com.bharatconnect.app.presentation.story.CreateStoryDialog
import com.bharatconnect.app.presentation.story.StoryItem
import com.bharatconnect.app.presentation.story.StoryViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Feed, 1: Chats, 2: Nearby, 3: Market, 4: Profile
    var showNotificationsScreen by remember { mutableStateOf(false) }
    val currentUser by authViewModel.currentUser.collectAsState()
    val selectedConversation by chatViewModel.selectedConversation.collectAsState()

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Stories state
    val stories = remember {
        mutableStateListOf<StoryItem>()
    }
    var activeViewingStory by remember { mutableStateOf<StoryItem?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    val notificationsList by chatViewModel.notifications.collectAsState()
    val unreadNotifCount = remember(notificationsList) { notificationsList.count { !it.isRead } }
    val totalChatUnread by chatViewModel.totalUnreadCount.collectAsState()

    // WhatsApp-style device back button prevention
    BackHandler {
        when {
            showNotificationsScreen -> {
                showNotificationsScreen = false
            }
            selectedConversation != null -> {
                chatViewModel.closeChat()
            }
            showCreateStoryDialog -> {
                showCreateStoryDialog = false
            }
            activeViewingStory != null -> {
                activeViewingStory = null
            }
            selectedTab != 0 -> {
                // Return to main Feed tab first instead of quitting
                selectedTab = 0
            }
            else -> {
                // On main tab: double back press within 2 seconds to exit
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000L) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Press back again to exit BharatConnect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showNotificationsScreen) {
        NotificationsScreen(
            chatViewModel = chatViewModel,
            onBack = { showNotificationsScreen = false }
        )
        return
    }

    if (selectedConversation != null) {
        ChatDetailScreen(
            conversation = selectedConversation!!,
            chatViewModel = chatViewModel,
            currentUserId = currentUser?.id,
            onBack = { chatViewModel.closeChat() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFF9933), Color(0xFF6367FF), Color(0xFF138808))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> "BharatConnect"
                                1 -> "Messages & Hubs"
                                2 -> "Nearby Radar"
                                3 -> "Marketplace"
                                else -> "My Profile"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showNotificationsScreen = true }) {
                        if (unreadNotifCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFFFF3B30)) {
                                        Text(if (unreadNotifCount > 99) "99+" else "$unreadNotifCount", color = Color.White)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.LightGray
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Profile Avatar in Top Bar
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221F45))
                            .border(1.5.dp, ColorPrimary6367FF, CircleShape)
                            .clickable { selectedTab = 4 },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentUser?.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (currentUser?.fullName ?: currentUser?.username ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0D24)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0D24)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Home") },
                    label = { Text("Feed", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorPrimary6367FF,
                        selectedTextColor = ColorPrimary6367FF,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF221F45)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        if (totalChatUnread > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFF25D366)) {
                                        Text(
                                            text = if (totalChatUnread > 99) "99+" else "$totalChatUnread",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChatBubble, contentDescription = "Chats")
                            }
                        } else {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Chats")
                        }
                    },
                    label = { Text("Chats", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorPrimary6367FF,
                        selectedTextColor = ColorPrimary6367FF,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF221F45)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.LocationSearching, contentDescription = "Nearby") },
                    label = { Text("Nearby", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorPrimary6367FF,
                        selectedTextColor = ColorPrimary6367FF,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF221F45)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Market") },
                    label = { Text("Market", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorPrimary6367FF,
                        selectedTextColor = ColorPrimary6367FF,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF221F45)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ColorPrimary6367FF,
                        selectedTextColor = ColorPrimary6367FF,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF221F45)
                    )
                )
            }
        },
        containerColor = ColorBackground080616
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> FeedTab(
                    feedViewModel = feedViewModel,
                    stories = stories,
                    currentUser = currentUser,
                    onAddStoryClick = { showCreateStoryDialog = true },
                    onStoryClick = { activeViewingStory = it }
                )
                1 -> ChatsTab(chatViewModel = chatViewModel)
                2 -> NearbyScreen(
                    onStartChat = {
                        selectedTab = 1
                    }
                )
                3 -> MarketplaceScreen(
                    onItemContact = {
                        selectedTab = 1
                    }
                )
                4 -> ProfileTab(
                    user = currentUser,
                    authViewModel = authViewModel,
                    onSignOut = {
                        authViewModel.logout {
                            onSignOut()
                        }
                    }
                )
            }
        }
    }

    if (showCreateStoryDialog) {
        CreateStoryDialog(
            onDismiss = { showCreateStoryDialog = false },
            onPublish = { textContent, gradient ->
                stories.add(
                    0,
                    StoryItem(
                        id = System.currentTimeMillis().toString(),
                        authorName = currentUser?.fullName ?: currentUser?.username ?: "You",
                        textContent = textContent,
                        gradientColors = gradient,
                        timeAgo = "Just now"
                    )
                )
                showCreateStoryDialog = false
            }
        )
    }

    if (activeViewingStory != null) {
        StoryViewerDialog(
            story = activeViewingStory!!,
            onDismiss = { activeViewingStory = null }
        )
    }
}
