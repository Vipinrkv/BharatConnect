package com.bharatconnect.app

import android.app.Application
import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.session.SessionManager
import com.bharatconnect.app.core.sync.SyncManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BharatConnectApp : Application() {

    companion object {
        lateinit var appContext: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // Initialize Persistent Session Manager
        SessionManager.init(this)
        // Initialize Supabase Kotlin SDK
        SupabaseClient.init(this)
        // Initialize Local Room Database
        DatabaseProvider.init(this)
        // Asynchronously warm up Supabase auth session if cached tokens exist
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (access, refresh) = SessionManager.getAuthTokens()
                if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
                    SupabaseClient.client.auth.importAuthToken(access, refresh)
                }
            } catch (_: Exception) {}
        }
        // Create FCM Notification Channels
        NotificationHelper.createNotificationChannels(this)
        // Schedule Offline Sync Engine
        SyncManager.scheduleBackgroundSync(this)
    }
}
