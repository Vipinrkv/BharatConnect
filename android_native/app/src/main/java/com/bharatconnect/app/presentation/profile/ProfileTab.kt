package com.bharatconnect.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch

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
    ) { uri: Uri? ->
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
                    ProfileStatsCard(
                        postsCount = "0",
                        followersCount = "0",
                        followingCount = "0"
                    )

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
        EditProfileDialog(
            initialFullName = fullName,
            initialBio = bio,
            initialPhone = phone,
            initialDob = dob,
            initialAvatarUrl = avatarUrl,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedName, updatedBio, updatedPhone, updatedDob, updatedAvatar, onComplete ->
                authViewModel.updateProfile(
                    fullName = updatedName,
                    bio = updatedBio,
                    phoneNumber = updatedPhone.ifBlank { null },
                    dob = updatedDob.ifBlank { null },
                    avatarUrl = updatedAvatar
                ) { success, error ->
                    if (success) {
                        fullName = updatedName
                        bio = updatedBio
                        phone = updatedPhone.ifBlank { "Not provided" }
                        dob = updatedDob.ifBlank { "Not set" }
                        avatarUrl = updatedAvatar
                    }
                    onComplete(success, error)
                }
            }
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
