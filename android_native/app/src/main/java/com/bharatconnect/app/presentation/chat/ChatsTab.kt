package com.bharatconnect.app.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.presentation.components.ConversationItemSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTab(
    chatViewModel: ChatViewModel,
    currentUserId: String? = null
) {
    val context = LocalContext.current
    val conversations by chatViewModel.conversations.collectAsState()
    val isLoadingConversations by chatViewModel.isLoadingConversations.collectAsState()
    val phoneContacts by chatViewModel.phoneContacts.collectAsState()
    val isLoadingContacts by chatViewModel.isLoadingContacts.collectAsState()
    val selectedConversation by chatViewModel.selectedConversation.collectAsState()

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
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var headerNoticeMessage by remember { mutableStateOf<String?>(null) }

    // BackHandler for sub-dialogs/sheets
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

    val groupChats = remember {
        listOf(
            "Tech Innovators Delhi" to "142 members • Rajesh: Next meetup on Saturday!",
            "Bengaluru Developers Club" to "530 members • Priya: APK released on repo",
            "Mumbai Founders & Creators" to "280 members • Rohan: Who is attending tomorrow?"
        )
    }

    val communities = remember {
        listOf(
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

    // If a conversation is actively open, display ChatDetailScreen
    if (selectedConversation != null) {
        ChatDetailScreen(
            conversation = selectedConversation!!,
            chatViewModel = chatViewModel,
            currentUserId = currentUserId,
            onBack = { chatViewModel.closeChat() }
        )
        return
    }

    val filteredConversations = conversations.filter { conv ->
        !conv.isGroup &&
        !archivedConvIds.contains(conv.id) &&
        (searchQuery.isBlank() ||
         (customNicknames[conv.id] ?: conv.title).contains(searchQuery, ignoreCase = true) ||
         (conv.lastMessage ?: "").contains(searchQuery, ignoreCase = true))
    }.sortedWith(
        compareByDescending<Conversation> { pinnedConvIds.contains(it.id) }
            .thenByDescending { it.lastMessageTime ?: "" }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sub-Tabs Header (Individual / Groups / Communities) + Add (+) & Three Dot Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subTabs = listOf("Chats", "Groups", "Communities")
                    subTabs.forEachIndexed { index, title ->
                        val isSelected = selectedSubTab == index
                        Surface(
                            color = if (isSelected) ColorPrimary6367FF else Color(0xFF16142E),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { selectedSubTab = index }
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Plus (+) Button to Select Contacts
                IconButton(
                    onClick = {
                        if (!hasContactPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                        showNewChatDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Three Dot Menu
                Box {
                    IconButton(
                        onClick = { showThreeDotMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Chat Options", tint = Color.LightGray)
                    }

                    DropdownMenu(
                        expanded = showThreeDotMenu,
                        onDismissRequest = { showThreeDotMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1A3C))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all read", color = Color.White, fontSize = 13.sp) },
                            onClick = {
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
                            items(items = filteredConversations, key = { it.id }) { conv ->
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
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Text(
                                                        text = displayName,
                                                        color = Color.White,
                                                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        maxLines = 1
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
                                                val formattedTime = remember(conv.lastMessageTime) {
                                                    conv.lastMessageTime?.takeIf { it.isNotBlank() }?.let { t ->
                                                        if (t.length >= 16 && t.contains(" ")) {
                                                            t.substring(11, 16)
                                                        } else if (t.length >= 8) {
                                                            t.takeLast(8).take(5)
                                                        } else t
                                                    } ?: ""
                                                }
                                                if (formattedTime.isNotBlank()) {
                                                    Text(
                                                        text = formattedTime,
                                                        color = if (conv.unreadCount > 0) Color(0xFF25D366) else Color.Gray,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = conv.lastMessage ?: "Tap to start conversation",
                                                    color = if (conv.unreadCount > 0) Color.White else Color.Gray,
                                                    fontWeight = if (conv.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (conv.unreadCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = 8.dp)
                                                            .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF25D366)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (conv.unreadCount > 99) "99+" else "${conv.unreadCount}",
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                } else {
                                                    Text("• Online", color = Color(0xFF4EFEAA), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { selectedConversationForMenu = conv },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(20.dp))
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
                    GroupsSubTab(
                        activeGroups = conversations.filter { it.isGroup },
                        featuredGroups = groupChats,
                        onSelectConversation = { chatViewModel.selectConversation(it) },
                        onStartGroupChat = { id, title -> chatViewModel.startGroupChat(id, title) }
                    )
                }

                2 -> {
                    // ==================== COMMUNITIES TAB ====================
                    CommunitiesSubTab(
                        communities = communities,
                        onJoinCommunity = { id, title -> chatViewModel.startGroupChat(id, title) }
                    )
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

        // Long Press / Options Dialog
        if (selectedConversationForMenu != null) {
            val conv = selectedConversationForMenu!!
            val isPinned = pinnedConvIds.contains(conv.id)
            AlertDialog(
                onDismissRequest = { selectedConversationForMenu = null },
                title = { Text(customNicknames[conv.id] ?: conv.title, color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                if (isPinned) pinnedConvIds.remove(conv.id) else pinnedConvIds.add(conv.id)
                                selectedConversationForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFFF9933))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPinned) "Unpin Chat" else "Pin Chat to Top", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                conversationForNicknameDialog = conv
                                customNicknameInput = customNicknames[conv.id] ?: ""
                                selectedConversationForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = ColorPrimary6367FF)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Custom Nickname", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                archivedConvIds.add(conv.id)
                                selectedConversationForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF4EFEAA))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Archive Chat", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                chatViewModel.deleteConversation(conv.id)
                                selectedConversationForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Chat", color = Color(0xFFFF6B6B))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedConversationForMenu = null }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF16142E),
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Custom Nickname Dialog
        if (conversationForNicknameDialog != null) {
            val targetConv = conversationForNicknameDialog!!
            AlertDialog(
                onDismissRequest = { conversationForNicknameDialog = null },
                title = { Text("Set Contact Nickname", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Give ${targetConv.title} a custom local nickname only visible to you:",
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

        // Contact Picker Sheet
        if (showNewChatDialog) {
            SelectContactBottomSheet(
                phoneContacts = phoneContacts,
                isLoadingContacts = isLoadingContacts,
                hasContactPermission = hasContactPermission,
                permissionLauncher = permissionLauncher,
                onDismiss = { showNewChatDialog = false },
                onContactSelected = { contact ->
                    chatViewModel.startChatWithContact(contact) {
                        showNewChatDialog = false
                    }
                }
            )
        }

        // WhatsApp-style Floating Action Button for New Chat (+)
        FloatingActionButton(
            onClick = {
                if (!hasContactPermission) {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                showNewChatDialog = true
            },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
