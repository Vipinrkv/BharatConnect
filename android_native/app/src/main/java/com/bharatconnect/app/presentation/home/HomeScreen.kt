package com.bharatconnect.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.presentation.auth.AuthViewModel
import com.bharatconnect.app.presentation.chat.ChatViewModel
import com.bharatconnect.app.presentation.marketplace.MarketplaceScreen
import com.bharatconnect.app.presentation.nearby.NearbyScreen
import com.bharatconnect.app.presentation.notifications.NotificationsScreen
import com.bharatconnect.app.presentation.story.CreateStoryDialog
import com.bharatconnect.app.presentation.story.StoriesRow
import com.bharatconnect.app.presentation.story.StoryItem
import com.bharatconnect.app.presentation.story.StoryViewerDialog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bharatconnect.app.core.storage.CloudinaryManager
import kotlinx.coroutines.launch

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

    // Stories state
    val stories = remember {
        mutableStateListOf<StoryItem>()
    }
    var activeViewingStory by remember { mutableStateOf<StoryItem?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }

    if (showNotificationsScreen) {
        NotificationsScreen(onBack = { showNotificationsScreen = false })
        return
    }

    if (selectedConversation != null) {
        ChatDetailScreen(
            conversation = selectedConversation!!,
            chatViewModel = chatViewModel,
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
                        BadgedBox(
                            badge = {
                                Badge(containerColor = Color(0xFFFF3B30)) {
                                    Text("3", color = Color.White)
                                }
                            }
                        ) {
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
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
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

@Composable
fun FeedTab(
    feedViewModel: FeedViewModel,
    stories: List<StoryItem>,
    currentUser: UserProfile? = null,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryItem) -> Unit
) {
    val posts by feedViewModel.posts.collectAsState()
    var showCreatePostDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StoriesRow(
                    stories = stories,
                    currentUserAvatar = currentUser?.avatarUrl,
                    onAddStoryClick = onAddStoryClick,
                    onStoryClick = onStoryClick
                )
            }

            if (posts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
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
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    tint = ColorPrimary6367FF,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Feed is Ready",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Be the first to share an update, thought, or story with your community!",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showCreatePostDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Post")
                            }
                        }
                    }
                }
            } else {
                items(posts) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { feedViewModel.toggleLike(post.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreatePostDialog = true },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Post")
        }
    }

    if (showCreatePostDialog) {
        var captionText by remember { mutableStateOf("") }
        var topicTitle by remember { mutableStateOf("") }
        var selectedLocation by remember { mutableStateOf("New Delhi, India") }
        var selectedAudience by remember { mutableStateOf("Everyone 🌐") }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = {
                Text(
                    "Create BharatConnect Post",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("What's happening? Share your thoughts...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = topicTitle,
                        onValueChange = { topicTitle = it },
                        placeholder = { Text("Topic / Tag (e.g. #Tech, #Design)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFF1E1B45),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📍 $selectedLocation",
                                color = Color(0xFF4EFEAA),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFF1E1B45),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedAudience,
                                color = ColorPrimary6367FF,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (captionText.isNotBlank()) {
                            val fullContent = if (topicTitle.isNotBlank()) "$captionText\n\n$topicTitle" else captionText
                            feedViewModel.createPost(fullContent)
                            showCreatePostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    Text("Publish Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E)
        )
    }
}

@Composable
fun PostCard(post: Post, onLikeClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorPrimary6367FF),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = post.authorName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(text = post.createdAt, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.content, color = Color(0xFFE2E2F0), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLikedByMe) Color(0xFFFF4B6B) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.likesCount} Likes", color = Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.commentsCount} Comments", color = Color.Gray, fontSize = 12.sp)
                }
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ChatsTab(chatViewModel: ChatViewModel) {
    val conversations by chatViewModel.conversations.collectAsState()
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Individual, 1: Groups, 2: Communities
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredConversations = conversations.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sub-Tabs Header (Individual / Groups / Communities)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Individual", "Groups", "Communities").forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    Surface(
                        color = if (isSelected) ColorPrimary6367FF else Color(0xFF16142E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSubTab = index }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages, groups...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF14122A),
                    unfocusedContainerColor = Color(0xFF14122A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF2C2856)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Conversations List
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1C3F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = ColorPrimary6367FF,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Conversations Yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start an encrypted 1-on-1 or group chat with friends and colleagues.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showNewChatDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start New Chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(filteredConversations) { conv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chatViewModel.selectConversation(conv) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2856)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(conv.title.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(conv.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(conv.lastMessage ?: "", color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF138808))
                            )
                        }
                        HorizontalDivider(color = Color(0xFF1B1933), thickness = 0.8.dp)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Chat")
        }

        if (showNewChatDialog) {
            var directContactInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = { Text("Start Direct Chat", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = directContactInput,
                        onValueChange = { directContactInput = it },
                        placeholder = { Text("Enter Phone Number or @handle", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (directContactInput.isNotBlank()) {
                                showNewChatDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                    ) {
                        Text("Start Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF16142E)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiDrawer by remember { mutableStateOf(false) }
    var showCallNoticeDialog by remember { mutableStateOf<String?>(null) }

    val emojiCategories = remember {
        listOf(
            "Smileys" to listOf("😀", "😂", "🥰", "😎", "🤩", "🤔", "🥳", "🙌", "🔥", "✨"),
            "India 🇮🇳" to listOf("🇮🇳", "🙏", "🪔", "🏏", "🦚", "🐅", "🍛", "🫓", "☕", "🕉️"),
            "Gestures" to listOf("👍", "👌", "✌️", "👏", "💪", "🤝", "🤙", "❤️", "💖", "💯"),
            "Reactions" to listOf("🚀", "⚡", "🎉", "🌟", "💡", "🎯", "🏆", "🔒", "💬", "✅")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C2856)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(conversation.title.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(conversation.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("● Online • Encrypted", color = Color(0xFF4EFEAA), fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showCallNoticeDialog = "Starting Encrypted Voice Call with ${conversation.title}..." }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White)
                    }
                    IconButton(onClick = { showCallNoticeDialog = "Starting HD Video Call with ${conversation.title}..." }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0D24))
            )
        },
        containerColor = ColorBackground080616
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId != "other"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) ColorPrimary6367FF else Color(0xFF1C1A38)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(msg.content, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(msg.createdAt.takeLast(8), color = Color(0xFFD1D1E0), fontSize = 10.sp)
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (msg.status == "sending") "⏳" else "✓✓",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emoji Drawer
            if (showEmojiDrawer) {
                Surface(
                    color = Color(0xFF14122A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Emojis", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showEmojiDrawer = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            items(emojiCategories.flatMap { it.second }) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { inputText += emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Chat Input Bar
            Surface(
                color = Color(0xFF0F0D24),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEmojiDrawer = !showEmojiDrawer }) {
                        Icon(Icons.Default.SentimentSatisfied, contentDescription = "Emoji", tint = Color.LightGray)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color(0xFF2C2856)
                        )
                    )

                    IconButton(onClick = { showAttachmentSheet = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.LightGray)
                    }

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(conversation.id, inputText)
                                inputText = ""
                                showEmojiDrawer = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = ColorPrimary6367FF,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    // Attachment Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = Color(0xFF14122A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Share Content",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val attachmentOptions = listOf(
                        Triple(Icons.Default.CameraAlt, "Camera", Color(0xFFFF5252)),
                        Triple(Icons.Default.Image, "Gallery", Color(0xFF00E5FF)),
                        Triple(Icons.Default.InsertDriveFile, "Document", Color(0xFF00C853)),
                        Triple(Icons.Default.LocationOn, "Location", Color(0xFFFF9100)),
                        Triple(Icons.Default.Person, "Contact", Color(0xFF6367FF))
                    )

                    attachmentOptions.forEach { (icon, label, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                chatViewModel.sendMessage(conversation.id, "📎 Shared $label")
                                showAttachmentSheet = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(label, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showCallNoticeDialog != null) {
        AlertDialog(
            onDismissRequest = { showCallNoticeDialog = null },
            title = { Text("Voice & Video Call", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(showCallNoticeDialog!!, color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = { showCallNoticeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xFF16142E)
        )
    }
}

@Composable
fun ProfileTab(
    user: UserProfile?,
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var fullName by remember(user) { mutableStateOf(user?.fullName ?: "Bharat User") }
    var username by remember(user) { mutableStateOf(user?.username ?: "user") }
    var bio by remember(user) { mutableStateOf(user?.bio ?: "Welcome to BharatConnect. Ready to connect and explore!") }
    var phone by remember(user) { mutableStateOf(user?.phoneNumber ?: "Not provided") }
    var dob by remember(user) { mutableStateOf(user?.dob ?: "Not set") }
    var avatarUrl by remember(user) { mutableStateOf(user?.avatarUrl) }

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var editStatusMessage by remember { mutableStateOf<String?>(null) }

    val editAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingAvatar = true
                val result = CloudinaryManager.uploadProfilePicture(context, uri)
                isUploadingAvatar = false
                result.fold(
                    onSuccess = { newUrl ->
                        avatarUrl = newUrl
                        authViewModel.updateProfile(
                            fullName = fullName,
                            bio = bio,
                            phoneNumber = if (phone == "Not provided") null else phone,
                            dob = if (dob == "Not set") null else dob,
                            avatarUrl = newUrl
                        ) { success, error ->
                            if (!success) editStatusMessage = error
                        }
                    },
                    onFailure = { error ->
                        editStatusMessage = "Avatar upload failed: ${error.message}"
                    }
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Profile Card Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9933), ColorPrimary6367FF, Color(0xFF138808))
                                )
                            )
                            .padding(3.dp)
                            .clickable { editAvatarLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF181535)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = fullName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isUploadingAvatar) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF9933),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "@$username",
                        fontSize = 13.sp,
                        color = ColorPrimary6367FF
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bio,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Posts", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Followers", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Following", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Profile")
                        }
                        OutlinedButton(
                            onClick = { showSettingsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }
        }

        item {
            // Detailed User Contact Information
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📧 Email", color = Color.Gray, fontSize = 13.sp)
                        Text(user?.email ?: "user@bharatconnect.app", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Color(0xFF221F45))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📞 Phone", color = Color.Gray, fontSize = 13.sp)
                        Text(phone, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Color(0xFF221F45))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🎂 Birthday", color = Color.Gray, fontSize = 13.sp)
                        Text(dob, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            // Architecture & Security Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Network & Security Status", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Cloud Account & Data Sync: Active", color = Color(0xFF8CE99A), fontSize = 12.sp)
                    Text("• Local Offline Storage: Active", color = Color(0xFF8CE99A), fontSize = 12.sp)
                    Text("• Media & Attachment Service: Ready", color = Color(0xFF8CE99A), fontSize = 12.sp)
                    Text("• End-to-End Encryption: Active 🔒", color = Color(0xFF4EFEAA), fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = Color(0xFFFF6B6B))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of BharatConnect", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(fullName) }
        var tempBio by remember { mutableStateOf(bio) }
        var tempPhone by remember { mutableStateOf(if (phone == "Not provided") "" else phone) }
        var tempDob by remember { mutableStateOf(if (dob == "Not set") "" else dob) }
        var tempAvatarUrl by remember { mutableStateOf(avatarUrl) }
        var isSaving by remember { mutableStateOf(false) }

        val editDialogAvatarLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    isUploadingAvatar = true
                    val result = CloudinaryManager.uploadProfilePicture(context, uri)
                    isUploadingAvatar = false
                    result.fold(
                        onSuccess = { newUrl ->
                            tempAvatarUrl = newUrl
                        },
                        onFailure = { error ->
                            editStatusMessage = "Upload failed: ${error.message}"
                        }
                    )
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar change button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262347))
                            .clickable { editDialogAvatarLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!tempAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = tempAvatarUrl,
                                contentDescription = "Avatar Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color(0xFFFF9933))
                        }
                    }

                    Text(
                        text = "Tap to Change Profile Picture",
                        color = Color(0xFFFF9933),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { editDialogAvatarLauncher.launch("image/*") }
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Bio / Status") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = tempDob,
                        onValueChange = { tempDob = it },
                        label = { Text("Date of Birth") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        authViewModel.updateProfile(
                            fullName = tempName,
                            bio = tempBio,
                            phoneNumber = tempPhone.ifBlank { null },
                            dob = tempDob.ifBlank { null },
                            avatarUrl = tempAvatarUrl
                        ) { success, error ->
                            isSaving = false
                            if (success) {
                                fullName = tempName
                                bio = tempBio
                                phone = tempPhone.ifBlank { "Not provided" }
                                dob = tempDob.ifBlank { "Not set" }
                                avatarUrl = tempAvatarUrl
                                showEditProfileDialog = false
                            } else {
                                editStatusMessage = error
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E)
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("BharatConnect Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔒 End-to-End Encryption: Enabled", color = Color(0xFF4EFEAA), fontSize = 13.sp)
                    Text("🌙 Theme: Dark Modern Aesthetic", color = Color.White, fontSize = 13.sp)
                    Text("🌐 Language: English (Default) / Hindi", color = Color.White, fontSize = 13.sp)
                    Text("ℹ️ App Version: BharatConnect Native v2.0.0", color = Color.LightGray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    Text("Done")
                }
            },
            containerColor = Color(0xFF16142E)
        )
    }
}
