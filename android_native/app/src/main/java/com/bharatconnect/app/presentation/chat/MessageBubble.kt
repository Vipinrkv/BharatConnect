package com.bharatconnect.app.presentation.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bharatconnect.app.core.audio.AudioPlayerManager
import com.bharatconnect.app.core.payments.UpiPaymentManager
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Message

/**
 * =========================================================================================
 * ENCRYPTED MESSAGE BUBBLE (WHATSAPP STATUS TICKS + VOICE NOTES + BHARAT UPI CARDS)
 * =========================================================================================
 *
 * WhatsApp Delivery Progression:
 * 1. ⏱ Clock Icon: Message is sending or queued locally in Room DB (offline-first).
 * 2. ✓ Single Grey Tick: Message successfully dispatched to and acknowledged by Supabase server.
 * 3. ✓✓ Double Grey Tick: Message delivered to recipient's device and written to their local Room DB.
 * 4. ✓✓ Double Sky Blue Tick (#38BDF8): Recipient opened the chat screen and read the message.
 *
 * Indian Power Features:
 * - WhatsApp-Style Audio Voice Notes with Play/Pause waveform and progress indicator.
 * - 1-Tap Bharat UPI Payment Cards with instant Google Pay / PhonePe / Paytm dispatch.
 */
@Composable
fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    onReact: ((String) -> Unit)? = null,
    onToggleStar: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showReactionPicker by remember { mutableStateOf(false) }

    val displayContent = remember(msg.content) {
        if (msg.content.startsWith("ENC:")) {
            com.bharatconnect.app.core.encryption.SignalEncryptionManager.decrypt(msg.conversationId, msg.content)
        } else {
            msg.content
        }
    }

    val upiPaymentData = remember(displayContent) {
        UpiPaymentManager.parsePaymentMessage(displayContent)
    }

    val isVoiceNote = remember(msg.mediaType, msg.mediaUrl) {
        (msg.mediaType?.startsWith("audio") == true) && !msg.mediaUrl.isNullOrBlank()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Quick Reaction Picker Bar
        if (showReactionPicker) {
            Surface(
                color = Color(0xFF1F1B3E),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF3E3966)),
                modifier = Modifier
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickEmojis = listOf("🙏", "❤️", "👍", "😂", "😮", "🇮🇳")
                    quickEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReact?.invoke(emoji)
                                    showReactionPicker = false
                                }
                                .padding(4.dp)
                        )
                    }

                    if (onToggleStar != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (msg.isStarred) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onToggleStar.invoke()
                                    showReactionPicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (msg.isStarred) "★" else "☆", color = Color(0xFFFFD700), fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        upiPaymentData != null -> Color(0xFF191438)
                        isMe -> ColorPrimary6367FF
                        else -> Color(0xFF1C1A38)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (upiPaymentData != null) BorderStroke(1.dp, Color(0xFF4EFEAA)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showReactionPicker = !showReactionPicker
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                // 1. Photo Media Content
                if (!msg.mediaUrl.isNullOrBlank() && !isVoiceNote) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(msg.mediaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo message",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. WhatsApp-Style Voice Note Audio Player
                if (isVoiceNote) {
                    val currentPlayingUrl by AudioPlayerManager.currentPlayingUrl.collectAsState()
                    val isPlaying by AudioPlayerManager.isPlaying.collectAsState()
                    val progress by AudioPlayerManager.playbackProgress.collectAsState()

                    val isThisPlaying = (currentPlayingUrl == msg.mediaUrl && isPlaying)
                    val activeProgress = if (currentPlayingUrl == msg.mediaUrl) progress else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                msg.mediaUrl?.let { url ->
                                    AudioPlayerManager.play(url)
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color.White.copy(alpha = 0.25f) else ColorPrimary6367FF)
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Audio Waveform Progress Bar
                            LinearProgressIndicator(
                                progress = { activeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isMe) Color(0xFF4EFEAA) else ColorPrimary6367FF,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isMe) Color(0xFFD1D1E0) else Color(0xFF818CF8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voice note",
                                        color = if (isMe) Color(0xFFD1D1E0) else Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }

                                val playbackSpeed by AudioPlayerManager.playbackSpeed.collectAsState()
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFF2C2856),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { AudioPlayerManager.cyclePlaybackSpeed() }
                                ) {
                                    Text(
                                        text = if (playbackSpeed == 1.0f) "1x" else if (playbackSpeed == 1.5f) "1.5x" else "2x",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 3. 1-Tap Bharat UPI Payment Request Card
                if (upiPaymentData != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF100C26)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1A3826)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("₹", color = Color(0xFF4EFEAA), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Bharat UPI Payment", color = Color(0xFF4EFEAA), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Instant NPCI Transfer", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "₹${upiPaymentData.amount}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            if (upiPaymentData.transactionNote.isNotBlank()) {
                                Text(
                                    text = upiPaymentData.transactionNote,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Payee VPA: ${upiPaymentData.payeeVpa}",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    UpiPaymentManager.launchUpiPayment(context, upiPaymentData)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pay ₹${upiPaymentData.amount} via UPI App",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 4. Standard Text Content
                if (displayContent.isNotBlank() && upiPaymentData == null && (!isVoiceNote || displayContent != "🎤 Voice Note")) {
                    Text(displayContent, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 5. Timestamp and WhatsApp Status Ticks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val formattedTime = remember(msg.createdAt) {
                        com.bharatconnect.app.core.datetime.DateTimeUtils.formatMessageTime(msg.createdAt)
                    }

                    if (msg.isStarred) {
                        Text(
                            text = "⭐",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    if (msg.ephemeralDurationSec > 0) {
                        Text(
                            text = "⏳",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    Text(formattedTime, color = Color(0xFFD1D1E0), fontSize = 10.sp)

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        if (msg.isPendingSync || msg.status == "sending") {
                            Text(
                                text = "⏱",
                                fontSize = 11.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        } else {
                            when (msg.status) {
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

            // Reaction Badge Chip
            if (!msg.reaction.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF28244D),
                    border = BorderStroke(1.dp, Color(0xFF3E3966)),
                    modifier = Modifier
                        .align(if (isMe) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(y = 10.dp)
                        .clickable { onReact?.invoke(msg.reaction) }
                ) {
                    Text(
                        text = msg.reaction,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
