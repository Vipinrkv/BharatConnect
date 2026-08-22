package com.bharatconnect.app

import android.app.Application
import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.sync.SyncManager

class BharatConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase Kotlin SDK
        SupabaseClient.init(this)
        // Initialize Local Room Database
        DatabaseProvider.init(this)
        // Create FCM Notification Channels
        NotificationHelper.createNotificationChannels(this)
        // Schedule Offline Sync Engine
        SyncManager.scheduleBackgroundSync(this)
    }
}
