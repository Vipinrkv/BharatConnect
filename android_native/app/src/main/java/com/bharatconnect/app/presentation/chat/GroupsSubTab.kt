package com.bharatconnect.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Conversation

@Composable
fun GroupsSubTab(
    activeGroups: List<Conversation>,
    featuredGroups: List<Pair<String, String>>,
    onSelectConversation: (Conversation) -> Unit,
    onStartGroupChat: (groupId: String, title: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activeGroups.isNotEmpty()) {
            item {
                Text(
                    text = "YOUR ACTIVE GROUPS (${activeGroups.size})",
                    color = Color(0xFF4EFEAA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(items = activeGroups, key = { it.id }) { conv ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191638)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectConversation(conv) }
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
                                .background(ColorPrimary6367FF),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conv.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(conv.lastMessage ?: "Active group chat", color = Color.LightGray, fontSize = 12.sp, maxLines = 1)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Text(
                text = "EXPLORE FEATURED GROUPS",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = if (activeGroups.isNotEmpty()) 12.dp else 4.dp, bottom = 4.dp)
            )
        }

        items(featuredGroups) { (name, details) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStartGroupChat(name.lowercase().replace(" ", "_"), name)
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
                    Button(
                        onClick = {
                            onStartGroupChat(name.lowercase().replace(" ", "_"), name)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
