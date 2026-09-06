package com.bharatconnect.app.presentation.marketplace

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.presentation.components.MarketItemSkeleton

data class MarketItem(
    val id: String,
    val title: String,
    val price: String,
    val category: String,
    val location: String,
    val sellerName: String
)

data class JobListing(
    val id: String,
    val title: String,
    val company: String,
    val salary: String,
    val type: String, // Full Time / Remote
    val location: String
)

data class QuickJob(
    val id: String,
    val title: String,
    val payout: String,
    val duration: String,
    val urgency: String, // Urgent / Today
    val requester: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onItemContact: (title: String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Items, 1: Jobs, 2: Quick Jobs
    var searchQuery by remember { mutableStateOf("") }
    var showCreateListingDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val itemsList = remember {
        mutableStateListOf<MarketItem>()
    }

    val jobsList = remember {
        mutableStateListOf<JobListing>()
    }

    val quickJobsList = remember {
        mutableStateListOf<QuickJob>()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080616))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Market Tabs Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Items (Buy/Sell)", "Jobs", "Quick Gigs").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Surface(
                        color = if (isSelected) ColorPrimary6367FF else Color(0xFF16142E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        when (selectedTab) {
                            0 -> "Search products, electronics, furniture..."
                            1 -> "Search job titles, companies, skills..."
                            else -> "Search quick gigs, tasks, payouts..."
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

            // Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        val filteredItems = itemsList.filter {
                            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
                        }
                        if (isLoading && filteredItems.isEmpty()) {
                            items(4) {
                                MarketItemSkeleton()
                            }
                        } else if (filteredItems.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
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
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = ColorPrimary6367FF,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("No Items Listed for Sale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Tap '+' below to list your electronics, vehicles, furniture, or gadgets with ₹ pricing.", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(filteredItems) { item ->
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
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF2C2856), ColorPrimary6367FF)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "Item", tint = Color.White)
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(item.price, color = Color(0xFF4EFEAA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${item.category} • ${item.location}", color = Color.Gray, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { onItemContact(item.title) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val filteredJobs = jobsList.filter {
                            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.company.contains(searchQuery, ignoreCase = true)
                        }
                        if (filteredJobs.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
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
                                                imageVector = Icons.Default.WorkOutline,
                                                contentDescription = null,
                                                tint = Color(0xFF4EFEAA),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("No Open Job Vacancies", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Check back soon for new hiring updates or post a job opening in the community.", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(filteredJobs) { job ->
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(job.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(job.company, color = ColorPrimary6367FF, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Surface(
                                                color = Color(0xFF1E1B45),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = job.type,
                                                    color = Color(0xFF4EFEAA),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("💰 ${job.salary}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Button(
                                                onClick = { onItemContact(job.title) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        val filteredQuick = quickJobsList.filter {
                            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
                        }
                        if (filteredQuick.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
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
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9933),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("No Quick Gigs Available", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Tap '+' to post quick tasks, local deliveries, or freelance gigs with instant payouts.", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(filteredQuick) { task ->
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("⚡ ${task.payout}", color = Color(0xFF4EFEAA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("⏱ ${task.duration}", color = Color.Gray, fontSize = 12.sp)
                                                Text("• ${task.urgency}", color = Color(0xFFFF9933), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        Button(
                                            onClick = { onItemContact(task.title) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF138808)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to post new listing
        FloatingActionButton(
            onClick = { showCreateListingDialog = true },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Post Listing")
        }

        if (showCreateListingDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newPriceOrSalary by remember { mutableStateOf("") }
            var newCategory by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreateListingDialog = false },
                title = {
                    Text(
                        text = "Create Marketplace Listing",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            placeholder = { Text("Title (e.g. Mechanical Keyboard)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = newPriceOrSalary,
                            onValueChange = { newPriceOrSalary = it },
                            placeholder = { Text("Price / Payout (e.g. ₹2,500)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            placeholder = { Text("Category / Location", color = Color.Gray) },
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
                            if (newTitle.isNotBlank()) {
                                itemsList.add(
                                    0,
                                    MarketItem(
                                        id = System.currentTimeMillis().toString(),
                                        title = newTitle,
                                        price = if (newPriceOrSalary.startsWith("₹")) newPriceOrSalary else "₹$newPriceOrSalary",
                                        category = if (newCategory.isNotBlank()) newCategory else "General",
                                        location = "Local",
                                        sellerName = "You"
                                    )
                                )
                                showCreateListingDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
                    ) {
                        Text("Post Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateListingDialog = false }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF16142E)
            )
        }
    }
}
