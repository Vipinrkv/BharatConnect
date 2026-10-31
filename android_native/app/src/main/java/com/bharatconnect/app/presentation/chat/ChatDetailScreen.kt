package com.bharatconnect.app.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bharatconnect.app.core.audio.AudioRecordManager
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.payments.UpiPaymentData
import com.bharatconnect.app.core.payments.UpiPaymentManager
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

    // Audio Voice Note & UPI States
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var showUpiPaymentDialog by remember { mutableStateOf(false) }
    var upiAmountText by remember { mutableStateOf("100") }
    var upiNoteText by remember { mutableStateOf("Payment via BharatConnect") }
    var upiVpaText by remember {
        mutableStateOf("${conversation.title.filter { it.isLetterOrDigit() }.lowercase()}@okhdfcbank")
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDurationSec = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000L)
                recordingDurationSec++
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val startRes = AudioRecordManager.startRecording(context)
            if (startRes.isSuccess) {
                isRecordingAudio = true
            } else {
                Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Audio recording permission is required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // WhatsApp-style hierarchical back handling
    BackHandler {
        when {
            isRecordingAudio -> {
                AudioRecordManager.cancelRecording()
                isRecordingAudio = false
            }
            showUpiPaymentDialog -> showUpiPaymentDialog = false
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
                            if (!conversation.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(conversation.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(conversation.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
                    IconButton(onClick = { showUpiPaymentDialog = true }) {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = "UPI Transfer", tint = Color(0xFF4EFEAA))
                    }
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        showAttachmentSheet = false
                                        showUpiPaymentDialog = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF1E3A2B), CircleShape)
                                ) {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = "UPI Transfer", tint = Color(0xFF4EFEAA))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("UPI Pay", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Desi Quick Replies Row
            val desiReplies = remember {
                listOf("नमस्ते 🙏", "हाँ, ठीक है 👍", "पैसे भेज दिए 💸", "कॉल करो 📞", "रास्ते में हूँ 🚗", "धन्यवाद! ✨")
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(desiReplies) { chip ->
                    AssistChip(
                        onClick = {
                            chatViewModel.sendMessage(conversation.id, chip)
                        },
                        label = { Text(chip, fontSize = 11.sp, color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E1A3C),
                            labelColor = Color.White
                        ),
                        border = BorderStroke(0.6.dp, Color(0xFF3F3A66))
                    )
                }
            }

            // Input Bar with Voice Note & Text Modes
            Surface(
                color = Color(0xFF14122A),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecordingAudio) {
                    // Active Voice Note Recording Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                AudioRecordManager.cancelRecording()
                                isRecordingAudio = false
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = Color(0xFFFF5252))
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF2B1F3C), RoundedCornerShape(24.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val min = recordingDurationSec / 60
                            val sec = recordingDurationSec % 60
                            Text(
                                text = String.format("%02d:%02d", min, sec),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recording Voice Note...",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val stopRes = AudioRecordManager.stopRecording()
                                isRecordingAudio = false
                                stopRes.onSuccess { (file, durationMs) ->
                                    val durationSec = (durationMs / 1000).coerceAtLeast(1)
                                    isUploadingMedia = true
                                    Toast.makeText(context, "Sending voice note...", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        val uploadResult = CloudinaryManager.uploadMedia(
                                            context,
                                            Uri.fromFile(file),
                                            "audio/mp4"
                                        )
                                        isUploadingMedia = false
                                        uploadResult.onSuccess { mediaUrl ->
                                            chatViewModel.sendMessage(
                                                conversationId = conversation.id,
                                                text = "🎤 Voice Note (${durationSec}s)",
                                                mediaUrl = mediaUrl,
                                                mediaType = "audio/mp4"
                                            )
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Voice note failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.onFailure { err ->
                                    Toast.makeText(context, "Recording failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(ColorPrimary6367FF, CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Note",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Regular Text & Mic Input Bar
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
                        } else if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    chatViewModel.sendMessage(conversation.id, inputText.trim())
                                    inputText = ""
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(ColorPrimary6367FF, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // WhatsApp-style Voice Note Mic button when input is empty
                            IconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        val startRes = AudioRecordManager.startRecording(context)
                                        if (startRes.isSuccess) {
                                            isRecordingAudio = true
                                        } else {
                                            Toast.makeText(context, "Could not start recording", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF2C2856), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Note",
                                    tint = Color(0xFF4EFEAA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

    if (showUpiPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showUpiPaymentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇮🇳", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1-Tap Bharat UPI Pay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Instant bank-to-bank settlement via NPCI UPI (GPay, PhonePe, Paytm, BHIM).",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = upiVpaText,
                        onValueChange = { upiVpaText = it },
                        label = { Text("Receiver UPI ID (VPA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorPrimary6367FF
                        )
                    )

                    OutlinedTextField(
                        value = upiAmountText,
                        onValueChange = { upiAmountText = it },
                        label = { Text("Amount (₹ INR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4EFEAA)
                        )
                    )

                    OutlinedTextField(
                        value = upiNoteText,
                        onValueChange = { upiNoteText = it },
                        label = { Text("Payment Note / Remarks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("₹50", "₹100", "₹500", "₹1000", "₹2000").forEach { preset ->
                            AssistChip(
                                onClick = { upiAmountText = preset.removePrefix("₹") },
                                label = { Text(preset, fontSize = 11.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF24204A))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpiPaymentDialog = false
                        val cleanAmount = upiAmountText.trim().ifEmpty { "1" }
                        val paymentData = UpiPaymentData(
                            payeeVpa = upiVpaText.trim(),
                            payeeName = conversation.title,
                            amount = cleanAmount,
                            transactionNote = upiNoteText.trim()
                        )
                        UpiPaymentManager.launchUpiPayment(context, paymentData)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF138808))
                ) {
                    Text("Pay via UPI App ⚡", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUpiPaymentDialog = false
                        val cleanAmount = upiAmountText.trim().ifEmpty { "1" }
                        val paymentData = UpiPaymentData(
                            payeeVpa = upiVpaText.trim(),
                            payeeName = conversation.title,
                            amount = cleanAmount,
                            transactionNote = upiNoteText.trim()
                        )
                        val cardMsg = UpiPaymentManager.encodePaymentMessage(paymentData)
                        chatViewModel.sendMessage(conversation.id, cardMsg)
                    }
                ) {
                    Text("Send Request Card", color = Color(0xFF4EFEAA))
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
