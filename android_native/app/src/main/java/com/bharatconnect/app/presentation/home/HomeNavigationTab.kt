package com.bharatconnect.app.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class HomeNavigationTab(
    val title: String,
    val icon: ImageVector
) {
    FEED("Feed", Icons.Default.Home),
    CHATS("Chats", Icons.Default.ChatBubble),
    NEARBY("Nearby", Icons.Default.NearMe),
    MARKET("Market", Icons.Default.Storefront),
    PROFILE("Profile", Icons.Default.Person)
}
