package com.bharatconnect.app.presentation.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF

data class BharatHub(
    val id: String,
    val title: String,
    val region: String,
    val category: String,
    val memberCount: String,
    val description: String,
    val tag: String,
    val iconColor: Color
)

data class NearbyUser(
    val id: String,
    val name: String,
    val username: String,
    val area: String,
    val distanceKm: Double,
    val status: String,
    val isOnline: Boolean,
    val category: String = "Member"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    onStartChat: (userName: String) -> Unit = {},
    onViewProfile: (userName: String) -> Unit = {}
) {
    var selectedMainTab by remember { mutableStateOf(0) } // 0: Bharat Hubs, 1: Nearby Radar
    var selectedRadius by remember { mutableStateOf(5) } // 1km, 5km, 10km
    var isGhostModeActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val defaultHubs = remember {
        listOf(
            BharatHub(
                id = "hub_delhi",
                title = "Delhi NCR Community Hub",
                region = "National Capital Region",
                category = "Metro Hub",
                memberCount = "12.4K members",
                description = "Local discussions, events, tech meetups, and neighborhood updates across Delhi, Noida & Gurugram.",
                tag = "#DelhiNCR",
                iconColor = Color(0xFFFF9933)
            ),
            BharatHub(
                id = "hub_bengaluru",
                title = "Bengaluru Innovators & Tech Circle",
                region = "Bengaluru, Karnataka",
                category = "Tech & Startups",
                memberCount = "18.9K members",
                description = "Connect with builders, founders, engineers, and creators in India's Silicon Valley.",
                tag = "#BengaluruTech",
                iconColor = ColorPrimary6367FF
            ),
            BharatHub(
                id = "hub_mumbai",
                title = "Mumbai Creators & Commerce",
                region = "Mumbai, Maharashtra",
                category = "Media & Business",
                memberCount = "15.2K members",
                description = "Entrepreneurs, creators, artists, and professionals collaborating in the City of Dreams.",
                tag = "#AamchiMumbai",
                iconColor = Color(0xFF4EFEAA)
            ),
            BharatHub(
                id = "hub_pune",
                title = "Pune Students & Campus Network",
                region = "Pune, Maharashtra",
                category = "Education & Youth",
                memberCount = "9.8K members",
                description = "College clubs, study groups, hackathons, and cultural discussions across Pune universities.",
                tag = "#PuneStudents",
                iconColor = Color(0xFFFF5E93)
            ),
            BharatHub(
                id = "hub_hyderabad",
                title = "Hyderabad Tech & Cyberabad Circle",
                region = "Hyderabad, Telangana",
                category = "Tech & Culture",
                memberCount = "11.1K members",
                description = "From HITEC City developers to heritage lovers, connecting the best of Hyderabad.",
                tag = "#HyderabadHub",
                iconColor = Color(0xFF00E5FF)
            )
        )
    }

    val allNearbyUsers = remember {
        mutableStateListOf<NearbyUser>()
    }

    val filteredHubs = defaultHubs.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.region.contains(searchQuery, ignoreCase = true) || it.tag.contains(searchQuery, ignoreCase = true)
    }

    val filteredUsers = allNearbyUsers.filter {
        it.distanceKm <= selectedRadius &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.status.contains(searchQuery, ignoreCase = true))
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080616))
    ) {
        // Main Tab Switcher (Bharat Hubs vs Discovery Radar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Bharat Hubs 🇮🇳", "Discovery Radar 📡").forEachIndexed { index, label ->
                val isSelected = selectedMainTab == index
                Surface(
                    color = if (isSelected) ColorPrimary6367FF else Color(0xFF16142E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMainTab = index }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (selectedMainTab == 0) "Search city hubs, campus circles, topics..." else "Search nearby members or interests...",
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (selectedMainTab == 0) {
            // ==================== BHARAT HUBS & CIRCLES TAB ====================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF9933), ColorPrimary6367FF, Color(0xFF138808))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hyperlocal & Interest Circles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Join your city or college circle to connect with active members with zero privacy risks.", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(filteredHubs) { hub ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(hub.iconColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(hub.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Surface(
                                    color = Color(0xFF1E1B45),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = hub.category,
                                        color = hub.iconColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(hub.description, color = Color(0xFFC0C0DC), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👥 ${hub.memberCount}", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(hub.tag, color = ColorPrimary6367FF, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = { onStartChat(hub.title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Join Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== PRIVACY-SAFE RADAR TAB ====================
            // Radar Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120F2D)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x4D6367FF),
                                        Color(0x1A4EFEAA),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFF4EFEAA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ColorPrimary6367FF, Color(0xFFFF5E93)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Radar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Privacy-Safe Discovery Radar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (isGhostModeActive) "👻 Ghost Mode Active • Your presence is hidden" else "● Approximate Area Scan • $selectedRadius km perimeter",
                        color = if (isGhostModeActive) Color(0xFFFF9933) else Color(0xFF4EFEAA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ghost Mode & Radius selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ghost Mode Toggle
                        FilterChip(
                            selected = isGhostModeActive,
                            onClick = { isGhostModeActive = !isGhostModeActive },
                            label = { Text(if (isGhostModeActive) "👻 Ghost: ON" else "👁️ Ghost: OFF", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9933),
                                selectedLabelColor = Color.Black
                            )
                        )

                        Row {
                            listOf(1, 5, 10).forEach { radius ->
                                val isSelected = selectedRadius == radius
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRadius = radius },
                                    label = { Text("${radius} km", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ColorPrimary6367FF,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1E1B45),
                                        labelColor = Color.LightGray
                                    ),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Nearby Users List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF221F45)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationSearching,
                                        contentDescription = null,
                                        tint = Color(0xFF4EFEAA),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Nearby Members Discovered",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try switching to the Bharat Hubs tab above to connect with thousands of active community members in your city.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.clickable { onViewProfile(user.name) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF2C2856), ColorPrimary6367FF)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                    if (user.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF138808))
                                                .border(2.dp, Color(0xFF14122A), CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${user.area} (~${user.distanceKm} km)",
                                            color = Color(0xFF4EFEAA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = user.status,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = { onStartChat(user.name) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF1F1C3F))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = "Chat",
                                        tint = ColorPrimary6367FF,
                                        modifier = Modifier.size(18.dp)
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
