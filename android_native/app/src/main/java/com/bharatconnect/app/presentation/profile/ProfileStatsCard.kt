package com.bharatconnect.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ProfileStatsCard(
    postsCount: String = "0",
    followersCount: String = "0",
    followingCount: String = "0"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(postsCount, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Posts", color = Color.Gray, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(followersCount, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Followers", color = Color.Gray, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(followingCount, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Following", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
