package com.bharatconnect.app.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.bharatconnect.app.core.contacts.ContactsManager
import com.bharatconnect.app.core.contacts.PhoneContact
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
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
import com.bharatconnect.app.presentation.components.*

import androidx.compose.ui.layout.ContentScale
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
    val isLoadingFeed by feedViewModel.isLoading.collectAsState()
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var viewingCommentsForPost by remember { mutableStateOf<Post?>(null) }
    var viewingAuthorProfile by remember { mutableStateOf<String?>(null) }
    val followedAuthors = remember { mutableStateListOf<String>() }

    // In-memory comments map for posts
    val postComments = remember {
        mutableStateMapOf<String, MutableList<Pair<String, String>>>().apply {
            put("demo_1", mutableListOf(
                "Priya Verma" to "Amazing update! Proud of Indian innovation 🇮🇳",
                "Amit Patel" to "Completely agree, loving the speed and privacy features!"
            ))
            put("demo_2", mutableListOf(
                "Rajesh Kumar" to "Count me in for the meetup! 🔥"
            ))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                if (isLoadingFeed && stories.isEmpty()) {
                    StoriesRowSkeleton()
                } else {
                    StoriesRow(
                        stories = stories,
                        currentUserAvatar = currentUser?.avatarUrl,
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = onStoryClick
                    )
                }
            }

            if (isLoadingFeed && posts.isEmpty()) {
                items(3) {
                    FeedPostSkeleton()
                }
            } else if (posts.isEmpty()) {
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
                    val isFollowing = followedAuthors.contains(post.authorName)
                    PostCard(
                        post = post,
                        isFollowing = isFollowing,
                        onLikeClick = { feedViewModel.toggleLike(post.id) },
                        onFollowClick = {
                            if (isFollowing) {
                                followedAuthors.remove(post.authorName)
                            } else {
                                followedAuthors.add(post.authorName)
                            }
                        },
                        onCommentClick = { viewingCommentsForPost = post },
                        onProfileClick = { viewingAuthorProfile = post.authorName }
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

    // ==================== COMMENTS BOTTOM SHEET / DIALOG ====================
    if (viewingCommentsForPost != null) {
        val targetPost = viewingCommentsForPost!!
        var newCommentText by remember { mutableStateOf("") }
        val commentsList = postComments.getOrPut(targetPost.id) { mutableListOf() }

        AlertDialog(
            onDismissRequest = { viewingCommentsForPost = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = ColorPrimary6367FF)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comments (${commentsList.size})", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        Text(
                            text = "No comments yet. Be the first to start the conversation!",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(bottom = 12.dp)
                        ) {
                            items(commentsList) { (author, text) ->
                                Surface(
                                    color = Color(0xFF1B1838),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = author, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = text, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write a comment...", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ColorPrimary6367FF,
                                unfocusedBorderColor = Color(0xFF2C2856)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    commentsList.add((currentUser?.fullName ?: currentUser?.username ?: "You") to newCommentText.trim())
                                    newCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(ColorPrimary6367FF, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingCommentsForPost = null }) {
                    Text("Close", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // ==================== PROFILE VIEW DIALOG ====================
    if (viewingAuthorProfile != null) {
        val author = viewingAuthorProfile!!
        AlertDialog(
            onDismissRequest = { viewingAuthorProfile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ColorPrimary6367FF),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(author.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Verified BharatConnect Member 🛡️", color = Color(0xFF4EFEAA), fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Connect with $author while keeping your phone number private.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("142", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Followers", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("89", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Following", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("18", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Posts", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingAuthorProfile = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Send Friend Request / Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingAuthorProfile = null }) {
                    Text("Close", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
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
fun PostCard(
    post: Post,
    isFollowing: Boolean = false,
    onLikeClick: () -> Unit,
    onFollowClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
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

                // Follow / Following Button
                Surface(
                    color = if (isFollowing) Color(0xFF1E1838) else ColorPrimary6367FF,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isFollowing) BorderStroke(1.dp, ColorPrimary6367FF) else null,
                    modifier = Modifier.clickable { onFollowClick() }
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "+ Follow",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCommentClick() }
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.commentsCount} Comments", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTab(chatViewModel: ChatViewModel) {
    val context = LocalContext.current
    val conversations by chatViewModel.conversations.collectAsState()
    val isLoadingConversations by chatViewModel.isLoadingConversations.collectAsState()
    val phoneContacts by chatViewModel.phoneContacts.collectAsState()
    val isLoadingContacts by chatViewModel.isLoadingContacts.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Individual, 1: Groups, 2: Communities
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    // Long press context menu states
    var selectedConversationForMenu by remember { mutableStateOf<Conversation?>(null) }
    var conversationForNicknameDialog by remember { mutableStateOf<Conversation?>(null) }
    var customNicknameInput by remember { mutableStateOf("") }
    val customNicknames = remember { mutableStateMapOf<String, String>() }
    val pinnedConvIds = remember { mutableStateListOf<String>() }
    val archivedConvIds = remember { mutableStateListOf<String>() }
    val unreadConvIds = remember { mutableStateListOf<String>() }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var headerNoticeMessage by remember { mutableStateOf<String?>(null) }

    // Intercept back button if any sub-dialog or sheet in ChatsTab is active
    BackHandler(
        enabled = showNewChatDialog ||
                selectedConversationForMenu != null ||
                conversationForNicknameDialog != null ||
                showThreeDotMenu ||
                searchQuery.isNotBlank()
    ) {
        when {
            showNewChatDialog -> showNewChatDialog = false
            selectedConversationForMenu != null -> selectedConversationForMenu = null
            conversationForNicknameDialog != null -> conversationForNicknameDialog = null
            showThreeDotMenu -> showThreeDotMenu = false
            searchQuery.isNotBlank() -> searchQuery = ""
        }
    }

    // Mock Groups state
    val groupChats = remember {
        mutableStateListOf(
            "Tech Innovators Delhi" to "142 members • Rajesh: Next meetup on Saturday!",
            "Bengaluru Developers Club" to "530 members • Priya: APK released on repo",
            "Mumbai Founders & Creators" to "280 members • Rohan: Who is attending tomorrow?"
        )
    }

    // Mock Communities state
    val communities = remember {
        mutableStateListOf(
            "Bharat Tech Hub" to "Official community for developers, engineers & builders across India",
            "Indian Freelancers & Designers" to "Connect, share gigs, collaborate on projects",
            "Campus Connect India" to "College networks, university clubs, study circles"
        )
    }

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            chatViewModel.loadDeviceContacts(context)
        }
    }

    LaunchedEffect(showNewChatDialog, hasContactPermission) {
        if (showNewChatDialog) {
            if (hasContactPermission) {
                chatViewModel.loadDeviceContacts(context)
            } else {
                chatViewModel.loadDeviceContacts(null)
            }
        }
    }

    val filteredConversations = conversations.filter { conv ->
        !archivedConvIds.contains(conv.id) &&
        (searchQuery.isBlank() ||
         (customNicknames[conv.id] ?: conv.title).contains(searchQuery, ignoreCase = true) ||
         (conv.lastMessage ?: "").contains(searchQuery, ignoreCase = true))
    }.sortedByDescending { pinnedConvIds.contains(it.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sub-Tabs Header (Individual / Groups / Communities) + Three Dot Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Three-dot Options Menu
                Box {
                    IconButton(onClick = { showThreeDotMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.LightGray)
                    }

                    DropdownMenu(
                        expanded = showThreeDotMenu,
                        onDismissRequest = { showThreeDotMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1638))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                unreadConvIds.clear()
                                showThreeDotMenu = false
                                headerNoticeMessage = "All chats marked as read"
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Select all", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showThreeDotMenu = false
                                headerNoticeMessage = "Selected all conversations"
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to favourites", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showThreeDotMenu = false
                                headerNoticeMessage = "Added to favourites"
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to list", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showThreeDotMenu = false
                                headerNoticeMessage = "Added to custom list"
                            }
                        )
                        HorizontalDivider(color = Color(0xFF2C2856))
                        DropdownMenuItem(
                            text = { Text("Clear all chats", color = Color(0xFFFF6B6B), fontSize = 13.sp) },
                            onClick = {
                                showThreeDotMenu = false
                                headerNoticeMessage = "Chats cleared"
                            }
                        )
                    }
                }
            }

            if (headerNoticeMessage != null) {
                Surface(
                    color = Color(0xFF142E1F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(headerNoticeMessage!!, color = Color(0xFF4EFEAA), fontSize = 12.sp)
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { headerNoticeMessage = null }
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        when (selectedSubTab) {
                            0 -> "Search chats, contacts, nicknames..."
                            1 -> "Search groups & channels..."
                            else -> "Search community hubs..."
                        },
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
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

            when (selectedSubTab) {
                0 -> {
                    // ==================== INDIVIDUAL CHATS ====================
                    if (isLoadingConversations && filteredConversations.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(5) {
                                ConversationItemSkeleton()
                            }
                        }
                    } else if (filteredConversations.isEmpty()) {
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
                                    text = "Start a 1-on-1 chat with your phonebook contacts or invite them to BharatConnect.",
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
                                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select from Contacts")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredConversations) { conv ->
                                val isPinned = pinnedConvIds.contains(conv.id)
                                val displayName = customNicknames[conv.id] ?: conv.title

                                Surface(
                                    color = if (isPinned) Color(0xFF171333) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { chatViewModel.selectConversation(conv) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2C2856)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = displayName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (customNicknames.containsKey(conv.id)) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(${conv.title})", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                if (isPinned) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Color(0xFFFF9933), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = conv.lastMessage ?: "Tap to start conversation",
                                                    color = Color.Gray,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text("• Online", color = Color(0xFF4EFEAA), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { selectedConversationForMenu = conv },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.MoreHoriz, contentDescription = "Hold options", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1B1933), thickness = 0.8.dp)
                            }
                        }
                    }
                }

                1 -> {
                    // ==================== GROUPS TAB ====================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groupChats) { (name, details) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C2856)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = ColorPrimary6367FF)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(details, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ==================== COMMUNITIES TAB ====================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(communities) { (name, desc) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            color = Color(0xFF1E1B45),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Verified Hub",
                                                color = Color(0xFF4EFEAA),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Chat from Contacts")
        }

        // ==================== HOLD / LONG-PRESS OPTIONS DIALOG ====================
        if (selectedConversationForMenu != null) {
            val conv = selectedConversationForMenu!!
            val isPinned = pinnedConvIds.contains(conv.id)

            AlertDialog(
                onDismissRequest = { selectedConversationForMenu = null },
                title = {
                    Text(
                        text = "Chat Options: ${customNicknames[conv.id] ?: conv.title}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Pin / Unpin
                        Surface(
                            color = Color(0xFF1A1638),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isPinned) pinnedConvIds.remove(conv.id) else pinnedConvIds.add(conv.id)
                                    selectedConversationForMenu = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFFF9933), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (isPinned) "Unpin Chat" else "Pin Chat to Top", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Set Custom Nickname
                        Surface(
                            color = Color(0xFF1A1638),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customNicknameInput = customNicknames[conv.id] ?: ""
                                    conversationForNicknameDialog = conv
                                    selectedConversationForMenu = null
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Set Custom Nickname for Contact", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Archive
                        Surface(
                            color = Color(0xFF1A1638),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    archivedConvIds.add(conv.id)
                                    selectedConversationForMenu = null
                                    headerNoticeMessage = "Chat archived"
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Archive Chat", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Delete
                        Surface(
                            color = Color(0xFF381419),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val targetId = conv.id
                                    selectedConversationForMenu = null
                                    chatViewModel.deleteConversation(targetId) {
                                        headerNoticeMessage = "Chat permanently deleted"
                                    }
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Delete Chat", color = Color(0xFFFF6B6B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedConversationForMenu = null }) {
                        Text("Close", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF15112E),
                shape = RoundedCornerShape(18.dp)
            )
        }

        // ==================== SET CUSTOM NICKNAME DIALOG ====================
        if (conversationForNicknameDialog != null) {
            val targetConv = conversationForNicknameDialog!!
            AlertDialog(
                onDismissRequest = { conversationForNicknameDialog = null },
                title = {
                    Text("Custom Nickname", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column {
                        Text(
                            text = "Set a personal nickname for ${targetConv.title}. Only you will see this name.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customNicknameInput,
                            onValueChange = { customNicknameInput = it },
                            placeholder = { Text("e.g. Rahul Work / Brother", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customNicknameInput.isNotBlank()) {
                                customNicknames[targetConv.id] = customNicknameInput.trim()
                            } else {
                                customNicknames.remove(targetConv.id)
                            }
                            conversationForNicknameDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                    ) {
                        Text("Save Nickname")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { conversationForNicknameDialog = null }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF16142E),
                shape = RoundedCornerShape(18.dp)
            )
        }

        // ==================== PHONEBOOK CONTACT PICKER BOTTOM SHEET ====================
        if (showNewChatDialog) {
            var contactSearchQuery by remember { mutableStateOf("") }
            val queryDigits = remember(contactSearchQuery) { contactSearchQuery.filter { it.isDigit() } }
            val registeredContacts = remember(phoneContacts, contactSearchQuery, queryDigits) {
                phoneContacts.filter {
                    it.isRegistered && (
                        contactSearchQuery.isBlank() ||
                        it.name.contains(contactSearchQuery, ignoreCase = true) ||
                        it.rawPhone.contains(contactSearchQuery) ||
                        (it.username != null && it.username.contains(contactSearchQuery, ignoreCase = true)) ||
                        (queryDigits.isNotEmpty() && (it.normalizedPhone.contains(queryDigits) || it.rawPhone.filter { c -> c.isDigit() }.contains(queryDigits)))
                    )
                }
            }
            val unregisteredContacts = remember(phoneContacts, contactSearchQuery, queryDigits) {
                phoneContacts.filter {
                    !it.isRegistered && (
                        contactSearchQuery.isBlank() ||
                        it.name.contains(contactSearchQuery, ignoreCase = true) ||
                        it.rawPhone.contains(contactSearchQuery) ||
                        (it.username != null && it.username.contains(contactSearchQuery, ignoreCase = true)) ||
                        (queryDigits.isNotEmpty() && (it.normalizedPhone.contains(queryDigits) || it.rawPhone.filter { c -> c.isDigit() }.contains(queryDigits)))
                    )
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showNewChatDialog = false },
                containerColor = Color(0xFF120F2A),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF332F63)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Select Contact", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                text = "${registeredContacts.size} registered members available",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { showNewChatDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    // Search Bar - ALWAYS available
                    OutlinedTextField(
                        value = contactSearchQuery,
                        onValueChange = { contactSearchQuery = it },
                        placeholder = { Text("Search name, @username, or phone number...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF16142E),
                            unfocusedContainerColor = Color(0xFF16142E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color(0xFF2C2856)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )

                    if (!hasContactPermission) {
                        // Phonebook Sync Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1840)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Contacts, contentDescription = null, tint = ColorPrimary6367FF, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Sync Device Contacts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Find phonebook friends & invite via SMS", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                        if (isLoadingContacts) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(6) {
                                    ContactItemSkeleton()
                                }
                            }
                        } else if (phoneContacts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No contacts found in device phonebook.", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                // 1. Registered Contacts Section
                                if (registeredContacts.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "REGISTERED ON BHARATCONNECT (${registeredContacts.size})",
                                            color = Color(0xFF4EFEAA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(registeredContacts) { contact ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF191638)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(ColorPrimary6367FF),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = contact.name.take(1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = contact.name,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF4EFEAA))
                                                        )
                                                    }
                                                    Text(contact.rawPhone, color = Color.Gray, fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        chatViewModel.startChatWithContact(contact) {
                                                            showNewChatDialog = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.ChatBubble, contentDescription = "Chat", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Unregistered Contacts Section (Invite via Native SMS)
                                if (unregisteredContacts.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "INVITE TO BHARATCONNECT (${unregisteredContacts.size})",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(unregisteredContacts) { contact ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14112E)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF25214E)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = contact.name.take(1),
                                                        color = Color.LightGray,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = contact.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(contact.rawPhone, color = Color.Gray, fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        ContactsManager.sendSmsInvite(context, contact.rawPhone)
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4EFEAA)),
                                                    border = BorderStroke(1.dp, Color(0xFF4EFEAA)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Sms, contentDescription = "Invite", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    chatViewModel: ChatViewModel,
    currentUserId: String? = null,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(conversation.id) {
        com.bharatconnect.app.core.notifications.NotificationHelper.clearMessageNotifications(context, conversation.id)
        chatViewModel.markMessagesAsRead(conversation.id)
    }

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiDrawer by remember { mutableStateOf(false) }
    var showCallNoticeDialog by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isUploadingMedia by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploadingMedia = true
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                val uploadResult = CloudinaryManager.uploadMedia(context, uri, "image/jpeg")
                isUploadingMedia = false
                uploadResult.onSuccess { mediaUrl ->
                    chatViewModel.sendMessage(
                        conversationId = conversation.id,
                        text = "📷 Photo",
                        mediaUrl = mediaUrl,
                        mediaType = "image/jpeg"
                    )
                }.onFailure { err ->
                    Toast.makeText(context, "Upload failed: ${err.message ?: "Network error"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val emojiCategories = remember {
        listOf(
            "Smileys" to listOf("😀", "😂", "🥰", "😎", "🤩", "🤔", "🥳", "🙌", "🔥", "✨"),
            "India 🇮🇳" to listOf("🇮🇳", "🙏", "🪔", "🏏", "🦚", "🐅", "🍛", "🫓", "☕", "🕉️"),
            "Gestures" to listOf("👍", "👌", "✌️", "👏", "💪", "🤝", "🤙", "❤️", "💖", "💯"),
            "Reactions" to listOf("🚀", "⚡", "🎉", "🌟", "💡", "🎯", "🏆", "🔒", "💬", "✅")
        )
    }

    var showChatMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // WhatsApp-style hierarchical back handling: closes sheets/drawers first, then exits chat
    BackHandler {
        when {
            showEmojiDrawer -> showEmojiDrawer = false
            showAttachmentSheet -> showAttachmentSheet = false
            showCallNoticeDialog != null -> showCallNoticeDialog = null
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            showChatMenu -> showChatMenu = false
            else -> onBack()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete this chat?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("All messages in this chat will be permanently deleted from this device and BharatConnect servers.", color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        chatViewModel.deleteConversation(conversation.id) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1B1736)
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
                    Box {
                        IconButton(onClick = { showChatMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showChatMenu,
                            onDismissRequest = { showChatMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1A3C))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Chat", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                                },
                                onClick = {
                                    showChatMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
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
            val listState = rememberLazyListState()

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUserId || (currentUserId == null && msg.senderId != "other")
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
                                if (!msg.mediaUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(msg.mediaUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Photo message",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                if (msg.content.isNotBlank() && (msg.mediaUrl.isNullOrBlank() || msg.content != "📷 Photo")) {
                                    Text(msg.content, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(msg.createdAt.takeLast(8), color = Color(0xFFD1D1E0), fontSize = 10.sp)
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        when (msg.status) {
                                            "sending" -> {
                                                Text(
                                                    text = "⏳",
                                                    fontSize = 10.sp
                                                )
                                            }
                                            "sent" -> {
                                                Text(
                                                    text = "✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB0BEC5)
                                                )
                                            }
                                            "delivered" -> {
                                                Text(
                                                    text = "✓✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB0BEC5)
                                                )
                                            }
                                            "read" -> {
                                                Text(
                                                    text = "✓✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = "✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB0BEC5)
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

            if (isUploadingMedia) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14122A))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ColorPrimary6367FF
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uploading media...",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
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
                        maxLines = 4,
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
                        Triple(Icons.AutoMirrored.Filled.InsertDriveFile, "Document", Color(0xFF00C853)),
                        Triple(Icons.Default.LocationOn, "Location", Color(0xFFFF9100)),
                        Triple(Icons.Default.Person, "Contact", Color(0xFF6367FF))
                    )

                    attachmentOptions.forEach { (icon, label, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (label == "Camera" || label == "Gallery") {
                                    imagePickerLauncher.launch("image/*")
                                    showAttachmentSheet = false
                                } else {
                                    chatViewModel.sendMessage(conversation.id, "📎 Shared $label")
                                    showAttachmentSheet = false
                                }
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
