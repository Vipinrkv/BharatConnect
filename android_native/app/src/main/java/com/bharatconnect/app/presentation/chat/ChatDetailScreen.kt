package com.bharatconnect.app.presentation.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.session.SessionManager
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.core.theme.ColorBackground080616
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Conversation
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

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
        NotificationHelper.clearMessageNotifications(context, conversation.id)
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

    // WhatsApp-style hierarchical back handling
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
                // WhatsApp-style End-to-End Encryption Notice Badge
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFF1E1A34).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.6.dp, Color(0xFF4A4468))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Messages and calls are end-to-end encrypted with AES-GCM-256. No one outside of this chat, not even BharatConnect, can read or listen to them.",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(items = messages, key = { it.id }) { msg ->
                    val resolvedUserId = currentUserId 
                        ?: SessionManager.getCachedUserProfile()?.id 
                        ?: com.bharatconnect.app.core.network.SupabaseClient.client.auth.currentUserOrNull()?.id
                    val isMe = (resolvedUserId != null && msg.senderId == resolvedUserId) ||
                               (msg.senderName == "You")
                    MessageBubble(msg = msg, isMe = isMe)
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
                            Text("Fast Reactions & Emojis", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { showEmojiDrawer = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emojiCategories.forEach { (_, emojis) ->
                                items(emojis) { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                inputText += emoji
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attachment Sheet
            if (showAttachmentSheet) {
                Surface(
                    color = Color(0xFF16142E),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Share Media & Documents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        showAttachmentSheet = false
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF2C2856), CircleShape)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Gallery", tint = ColorPrimary6367FF)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Gallery", color = Color.LightGray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        showAttachmentSheet = false
                                        showCallNoticeDialog = "Document sharing securely encrypted via BharatConnect Cloud."
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF2C2856), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "Document", tint = Color(0xFF4EFEAA))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Document", color = Color.LightGray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        showAttachmentSheet = false
                                        showCallNoticeDialog = "Live location beacon ready to share securely."
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF2C2856), CircleShape)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color(0xFFFF9933))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Location", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = Color(0xFF14122A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showEmojiDrawer = !showEmojiDrawer },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Mood, contentDescription = "Emoji", tint = if (showEmojiDrawer) ColorPrimary6367FF else Color.Gray)
                    }

                    IconButton(
                        onClick = { showAttachmentSheet = !showAttachmentSheet },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = if (showAttachmentSheet) ColorPrimary6367FF else Color.Gray)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1C1A38),
                            unfocusedContainerColor = Color(0xFF1C1A38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorPrimary6367FF,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isUploadingMedia) {
                        CircularProgressIndicator(
                            color = ColorPrimary6367FF,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    chatViewModel.sendMessage(conversation.id, inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (inputText.isNotBlank()) ColorPrimary6367FF else Color(0xFF232045),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCallNoticeDialog != null) {
        AlertDialog(
            onDismissRequest = { showCallNoticeDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF4EFEAA))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Peer Service", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = { Text(showCallNoticeDialog!!, color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { showCallNoticeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
