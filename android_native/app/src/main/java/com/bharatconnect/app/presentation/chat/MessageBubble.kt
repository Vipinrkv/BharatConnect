package com.bharatconnect.app.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Message

@Composable
fun MessageBubble(
    msg: Message,
    isMe: Boolean
) {
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
    }
}
