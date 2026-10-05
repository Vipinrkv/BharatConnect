package com.bharatconnect.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bharatconnect.app.core.theme.BharatConnectTheme
import com.bharatconnect.app.presentation.auth.AuthViewModel
import com.bharatconnect.app.presentation.chat.ChatViewModel
import com.bharatconnect.app.presentation.navigation.BharatConnectNavGraph

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)
        handleNotificationIntent(intent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            BharatConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    BharatConnectNavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        chatViewModel = chatViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(targetIntent: Intent?) {
        val convId = targetIntent?.getStringExtra("conversationId")
        val senderId = targetIntent?.getStringExtra("senderId")
        val senderName = targetIntent?.getStringExtra("senderName") ?: "Chat"
        if (!convId.isNullOrBlank() || !senderId.isNullOrBlank()) {
            targetIntent.removeExtra("conversationId")
            targetIntent.removeExtra("senderId")
            targetIntent.removeExtra("senderName")
            chatViewModel.openChatFromNotification(
                senderName = senderName,
                conversationId = convId,
                senderId = senderId
            )
        }
    }

    private fun handleDeepLinkIntent(targetIntent: Intent?) {
        val rawUri: Uri? = targetIntent?.data
        if (rawUri != null && rawUri.scheme == "bharatconnect" && rawUri.host == "auth") {
            targetIntent.data = null
            authViewModel.handleAuthCallback(rawUri)
        }
    }
}
