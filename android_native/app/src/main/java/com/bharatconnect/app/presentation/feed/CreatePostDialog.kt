package com.bharatconnect.app.presentation.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPublish: (content: String) -> Unit
) {
    var captionText by remember { mutableStateOf("") }
    var topicTitle by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("New Delhi, India") }
    var selectedAudience by remember { mutableStateOf("Everyone 🌐") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create BharatConnect Post",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("What's happening? Share your thoughts...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = topicTitle,
                    onValueChange = { topicTitle = it },
                    placeholder = { Text("Topic / Tag (e.g. #Tech, #Design)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(0xFF1E1B45),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📍 $selectedLocation",
                            color = Color(0xFF4EFEAA),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF1E1B45),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = selectedAudience,
                            color = ColorPrimary6367FF,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (captionText.isNotBlank()) {
                        val fullContent = if (topicTitle.isNotBlank()) "$captionText\n\n$topicTitle" else captionText
                        onPublish(fullContent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF)
            ) {
                Text("Publish Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF16142E)
    )
}
