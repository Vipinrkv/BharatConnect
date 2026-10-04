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
 * =========================================================================================
 * BHARATCONNECT ARCHITECTURAL CONNECTORS & DEPENDENCY BRIDGE
 * =========================================================================================
 *
 * AppConnectors acts as the centralized service locator and connector layer for BharatConnect.
 * It provides decoupled, singleton-style access to data repositories, managers, system bridges,
 * encryption algorithms, and background sync engines without enforcing rigid DI frameworks.
 *
 * Key Pillars:
 * 1. Data Repositories: Mediates between Room SQLite (offline-first) and Supabase (cloud).
 * 2. Core Managers: Encapsulates user session, device contacts matching, media, and notifications.
 * 3. Security & Sync: Provides AES-GCM-256 end-to-end encryption and background offline sync.
 */
object AppConnectors {

    // =====================================================================================
    // 1. DATA REPOSITORIES (Domain & Local/Remote Data Management)
    // =====================================================================================

    /**
     * AuthRepository: Handles user registration, OTP generation/validation, passwordless login,
     * token refreshing, and persistent session storage.
     */
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }

    /**
     * ChatRepository: Manages 1-on-1 direct messaging, group chat channels, communities,
     * real-time message streams, WhatsApp-style delivery ticks, and offline message queue.
     */
    val chatRepository: ChatRepository by lazy { ChatRepositoryImpl() }

    /**
     * FeedRepository: Orchestrates social feed posts, multimedia uploads, likes, comments,
     * and background periodic feed refresh.
     */
    val feedRepository: FeedRepository by lazy { FeedRepositoryImpl() }

    // =====================================================================================
    // 2. CORE MANAGERS & SYSTEM BRIDGES
    // =====================================================================================

    /**
     * SessionManager: Encapsulates local SharedPreferences session state, user tokens,
     * and authenticated user profile cache.
     */
    val session = SessionManager

    /**
     * ContactsManager: Scans device phonebook, normalizes phone numbers (+91), cross-references
     * with registered users on Supabase, and dispatches native SMS invitations.
     */
    val contacts = ContactsManager

    /**
     * SignalEncryptionManager: Implements AES-GCM-256 end-to-end encryption (E2EE) with
     * deterministic key derivation for zero-knowledge message security.
     */
    val encryption = SignalEncryptionManager

    /**
     * CloudinaryManager: Bridges multimedia uploads (photos, attachments) to Cloudinary CDN
     * and caches local URIs.
     */
    val storage = CloudinaryManager

    /**
     * NotificationHelper: Builds and posts native Android heads-up push notifications
     * and handles notification badge clearing.
     */
    val notifications = NotificationHelper

    /**
     * DatabaseProvider: Provides initialized Room SQLite AppDatabase instance for offline-first caching.
     */
    val database = DatabaseProvider

    /**
     * SyncManager: Triggers WorkManager offline synchronization worker for pending messages and posts.
     */
    val sync = SyncManager

    /**
     * SupabaseClient: Main client gateway for Supabase GoTrue Auth, PostgREST, Realtime WebSockets, and Storage.
     */
    val network = SupabaseClient
}
