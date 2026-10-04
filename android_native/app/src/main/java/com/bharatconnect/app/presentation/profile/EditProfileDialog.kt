package com.bharatconnect.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    initialFullName: String,
    initialBio: String,
    initialPhone: String,
    initialDob: String,
    initialAvatarUrl: String?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, bio: String, phone: String, dob: String, avatarUrl: String?, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var tempName by remember { mutableStateOf(initialFullName) }
    var tempBio by remember { mutableStateOf(initialBio) }
    var tempPhone by remember { mutableStateOf(if (initialPhone == "Not provided") "" else initialPhone) }
    var tempDob by remember { mutableStateOf(if (initialDob == "Not set") "" else initialDob) }
    var tempAvatarUrl by remember { mutableStateOf(initialAvatarUrl) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val editDialogAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = CloudinaryManager.uploadProfilePicture(context, uri)
                result.fold(
                    onSuccess = { newUrl -> tempAvatarUrl = newUrl },
                    onFailure = { error -> errorMessage = "Upload failed: ${error.message}" }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar change button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262347))
                        .clickable { editDialogAvatarLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!tempAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = tempAvatarUrl,
                            contentDescription = "Avatar Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color(0xFFFF9933))
                    }
                }

                Text(
                    text = "Tap to Change Profile Picture",
                    color = Color(0xFFFF9933),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { editDialogAvatarLauncher.launch("image/*") }
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = tempBio,
                    onValueChange = { tempBio = it },
                    label = { Text("Bio / Status") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = tempPhone,
                    onValueChange = { tempPhone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = tempDob,
                    onValueChange = { tempDob = it },
                    label = { Text("Date of Birth") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    onSave(tempName, tempBio, tempPhone, tempDob, tempAvatarUrl) { success, error ->
                        isSaving = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = error
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF16142E),
        shape = RoundedCornerShape(18.dp)
    )
}
