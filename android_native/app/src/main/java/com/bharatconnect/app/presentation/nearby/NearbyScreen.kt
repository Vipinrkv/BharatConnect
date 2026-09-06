package com.bharatconnect.app.presentation.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.bharatconnect.app.presentation.components.NearbyUserSkeleton

data class LiveBroadcast(
    val id: String,
    val authorName: String,
    val username: String,
    val location: String,
    val distanceKm: Double,
    val content: String,
    val timeAgo: String,
    val isPinnedToTop: Boolean = false,
    val pinnedRegion: String? = null,
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

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
    var selectedMainTab by remember { mutableStateOf(0) } // 0: Live Broadcasts, 1: Bharat Hubs, 2: Nearby Radar
    var selectedRadius by remember { mutableStateOf(5) } // 1km, 5km, 10km
    var isGhostModeActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateBroadcastDialog by remember { mutableStateOf(false) }
    var showPayToPinDialogForBroadcast by remember { mutableStateOf<LiveBroadcast?>(null) }
    var selectedPinRegion by remember { mutableStateOf("Delhi NCR & North") }
    var pinSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isScanningRadar by remember { mutableStateOf(false) }

    val liveBroadcasts = remember {
        mutableStateListOf(
            LiveBroadcast(
                id = "bc_1",
                authorName = "Aarav Sharma",
                username = "@aarav_tech",
                location = "Connaught Place, New Delhi",
                distanceKm = 1.2,
                content = "🚀 Live Hackathon happening at Central Park Plaza! Come check out projects and connect with developers.",
                timeAgo = "10m ago",
                isPinnedToTop = true,
                pinnedRegion = "Delhi NCR",
                likesCount = 42
            ),
            LiveBroadcast(
                id = "bc_2",
                authorName = "Ananya Iyer",
                username = "@ananya_iyer",
                location = "Indiranagar 100ft Rd, Bengaluru",
                distanceKm = 2.8,
                content = "☕ Startup & Indie Creators open coffee meetup at 4 PM today. All builders welcome!",
                timeAgo = "25m ago",
                isPinnedToTop = true,
                pinnedRegion = "Bengaluru Urban",
                likesCount = 29
            ),
            LiveBroadcast(
                id = "bc_3",
                authorName = "Rohan Mehta",
                username = "@rohan_m",
                location = "Bandra West, Mumbai",
                distanceKm = 3.5,
                content = "📸 Sunset photography and live vlog walk by Bandstand. Join the photowalk group!",
                timeAgo = "45m ago",
                isPinnedToTop = false,
                likesCount = 18
            )
        )
    }

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
        // Main Tab Switcher (Live Broadcasts vs Bharat Hubs vs Discovery Radar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Broadcasts 📍", "Bharat Hubs 🇮🇳", "Radar 📡").forEachIndexed { index, label ->
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
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        // Success Notification Banner
        if (pinSuccessMessage != null) {
            Surface(
                color = Color(0xFF142E1F),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFF9933), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pinSuccessMessage!!, color = Color(0xFF4EFEAA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.LightGray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { pinSuccessMessage = null }
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
                    text = when (selectedMainTab) {
                        0 -> "Search broadcasts by location or topic..."
                        1 -> "Search city hubs, campus circles, topics..."
                        else -> "Search nearby members or interests..."
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (selectedMainTab == 0) {
            // ==================== LIVE BROADCASTS TAB ====================
            Box(modifier = Modifier.fillMaxSize()) {
                val filteredBroadcasts = liveBroadcasts.filter {
                    searchQuery.isBlank() || it.content.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true)
                }.sortedByDescending { it.isPinnedToTop }

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
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFFFF9933), Color(0xFFFF5E93)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hyperlocal Live Broadcasts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Post real-time updates with geotags or Pin your broadcast to the top in selected regions.", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(filteredBroadcasts) { broadcast ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (broadcast.isPinnedToTop) Color(0xFF1A1538) else Color(0xFF14122A)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (broadcast.isPinnedToTop) BorderStroke(1.5.dp, Color(0xFFFF9933)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (broadcast.isPinnedToTop) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFF382310),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "📌 TOP PINNED • ${broadcast.pinnedRegion ?: "Selected Region"}",
                                                color = Color(0xFFFF9933),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ColorPrimary6367FF),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(broadcast.authorName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(broadcast.authorName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(broadcast.username, color = Color.Gray, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("• ${broadcast.timeAgo}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF1E1B45),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "📍 ${broadcast.location} (~${broadcast.distanceKm} km)",
                                            color = Color(0xFF4EFEAA),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(broadcast.content, color = Color(0xFFE2E2F0), fontSize = 13.sp, lineHeight = 18.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5E93), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${broadcast.likesCount}", color = Color.LightGray, fontSize = 12.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!broadcast.isPinnedToTop) {
                                            OutlinedButton(
                                                onClick = { showPayToPinDialogForBroadcast = broadcast },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9933)),
                                                border = BorderStroke(1.dp, Color(0xFFFF9933)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pay to Pin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Button(
                                            onClick = { onStartChat(broadcast.authorName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateBroadcastDialog = true },
                    containerColor = ColorPrimary6367FF,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    Icon(Icons.Default.Podcasts, contentDescription = "Broadcast")
                }
            }
        } else if (selectedMainTab == 1) {
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
                if (isScanningRadar && filteredUsers.isEmpty()) {
                    items(5) {
                        NearbyUserSkeleton()
                    }
                } else if (filteredUsers.isEmpty()) {
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

    // ==================== CREATE BROADCAST DIALOG ====================
    if (showCreateBroadcastDialog) {
        var broadcastContent by remember { mutableStateOf("") }
        var broadcastLocation by remember { mutableStateOf("Connaught Place, New Delhi") }

        AlertDialog(
            onDismissRequest = { showCreateBroadcastDialog = false },
            title = {
                Text(
                    text = "📢 Post Live Broadcast",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Broadcast instantly to nearby BharatConnect users with your live geo-tag.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = broadcastContent,
                        onValueChange = { broadcastContent = it },
                        placeholder = { Text("What's happening nearby? Announce meetup, event, or alert...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = broadcastLocation,
                        onValueChange = { broadcastLocation = it },
                        label = { Text("Geo Location Tag") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4EFEAA)) },
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
                        if (broadcastContent.isNotBlank()) {
                            liveBroadcasts.add(
                                0,
                                LiveBroadcast(
                                    id = "bc_${System.currentTimeMillis()}",
                                    authorName = "You",
                                    username = "@you",
                                    location = broadcastLocation,
                                    distanceKm = 0.1,
                                    content = broadcastContent,
                                    timeAgo = "Just now",
                                    isPinnedToTop = false,
                                    likesCount = 0
                                )
                            )
                            showCreateBroadcastDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                ) {
                    Text("Broadcast Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBroadcastDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // ==================== PAY TO PIN BROADCAST TO TOP DIALOG ====================
    if (showPayToPinDialogForBroadcast != null) {
        val targetBroadcast = showPayToPinDialogForBroadcast!!
        val regions = listOf("Delhi NCR & North", "Mumbai & West", "Bengaluru & South", "Kolkata & East", "Pan-India Top Pin")
        var selectedPlan by remember { mutableStateOf("₹99 / 24 Hours") }

        AlertDialog(
            onDismissRequest = { showPayToPinDialogForBroadcast = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFF9933))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pin Broadcast to Top",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Promote this broadcast to the #1 spot for all users in the selected region.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = Color(0xFF1B1838),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Target Region:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            regions.forEach { region ->
                                val isSelected = selectedPinRegion == region
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPinRegion = region }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPinRegion = region },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF9933))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(region, color = if (isSelected) Color.White else Color.LightGray, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Promote Duration: 24h", color = Color.Gray, fontSize = 12.sp)
                        Text("Fee: $selectedPlan", color = Color(0xFF4EFEAA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = liveBroadcasts.indexOfFirst { it.id == targetBroadcast.id }
                        if (index != -1) {
                            val updated = targetBroadcast.copy(
                                isPinnedToTop = true,
                                pinnedRegion = selectedPinRegion
                            )
                            liveBroadcasts.removeAt(index)
                            liveBroadcasts.add(0, updated)
                        }
                        pinSuccessMessage = "Broadcast successfully pinned to top in $selectedPinRegion! ⭐"
                        showPayToPinDialogForBroadcast = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9933)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm & Pin to Top (₹99)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayToPinDialogForBroadcast = null }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
    }
}
