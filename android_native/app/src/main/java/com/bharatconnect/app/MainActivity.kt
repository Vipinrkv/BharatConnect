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
import com.bharatconnect.app.presentation.navigation.BharatConnectNavGraph

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)

        setContent {
            BharatConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    BharatConnectNavGraph(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(targetIntent: Intent?) {
        val rawUri: Uri? = targetIntent?.data
        if (rawUri != null && rawUri.scheme == "bharatconnect" && rawUri.host == "auth") {
            targetIntent.data = null
            authViewModel.handleAuthCallback(rawUri)
        }
    }
}
