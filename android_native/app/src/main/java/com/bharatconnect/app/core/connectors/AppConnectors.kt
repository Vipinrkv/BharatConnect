package com.bharatconnect.app.core.connectors

import com.bharatconnect.app.core.contacts.ContactsManager
import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.session.SessionManager
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.core.sync.SyncManager
import com.bharatconnect.app.data.repository.AuthRepositoryImpl
import com.bharatconnect.app.data.repository.ChatRepositoryImpl
import com.bharatconnect.app.data.repository.FeedRepositoryImpl
import com.bharatconnect.app.domain.repository.AuthRepository
import com.bharatconnect.app.domain.repository.ChatRepository
import com.bharatconnect.app.domain.repository.FeedRepository

/**
 * AppConnectors provides a clean, unified architectural gateway to foundational
 * managers, repositories, encryption, and sync services across the BharatConnect application.
 */
object AppConnectors {

    // Data Repositories
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }
    val chatRepository: ChatRepository by lazy { ChatRepositoryImpl() }
    val feedRepository: FeedRepository by lazy { FeedRepositoryImpl() }

    // Core Managers & Bridges
    val session = SessionManager
    val contacts = ContactsManager
    val encryption = SignalEncryptionManager
    val storage = CloudinaryManager
    val notifications = NotificationHelper
    val database = DatabaseProvider
    val sync = SyncManager
    val network = SupabaseClient
}
