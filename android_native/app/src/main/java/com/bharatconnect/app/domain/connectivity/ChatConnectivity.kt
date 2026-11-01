package com.bharatconnect.app.domain.connectivity

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.model.state.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * =========================================================================================
 * CHAT CONNECTIVITY CONTRACT
 * =========================================================================================
 *
 * Explicit contract governing inter-module communication for the Chat feature.
 * Decouples presentation and background workers from concrete networking and database implementations.
 *
 * Exposes:
 * 1. Observable Conversation & Message streams.
 * 2. Authenticated message dispatch with offline-first Room persistence.
 * 3. Bidirectional Realtime WebSocket lifecycle (connect, reconnect, heartbeat, disconnect).
 * 4. Deterministic identity and conversation room resolution.
 * 5. Strict message state progression via [MessageStatus].
 */
interface ChatConnectivity {

    /**
     * Observable reactive stream of conversations sorted by (isPinned DESC, lastMessageTime DESC).
     */
    fun observeConversations(): Flow<List<Conversation>>

    /**
     * Observable reactive stream of messages for a given active conversation.
     */
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * Enqueues or dispatches a text/media message.
     * Guarantees local Room persistence before network dispatch.
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        mediaUrl: String? = null,
        mediaType: String? = null
    ): Result<Message>

    /**
     * Acknowledges message delivery (moves status -> Delivered).
     */
    suspend fun acknowledgeDelivery(messageId: String, conversationId: String): Result<Unit>

    /**
     * Marks all messages in conversation as read (moves status -> Read).
     */
    suspend fun markAsRead(conversationId: String): Result<Unit>

    /**
     * Flushes offline queued messages via FIFO ordering.
     */
    suspend fun retryPendingQueue(): Result<Int>

    /**
     * Connects Realtime WebSocket channel for the target conversation.
     */
    suspend fun connectRealtime(conversationId: String)

    /**
     * Disconnects Realtime WebSocket channel.
     */
    suspend fun disconnectRealtime()

    /**
     * Resolves canonical deterministic UUID for 1-on-1 conversations between two peers.
     */
    fun resolveCanonicalConversationId(currentUserId: String, peerUserId: String): String

    /**
     * Validates whether a state transition can be applied to an existing message.
     */
    fun validateStateTransition(current: String, next: String): Boolean {
        return MessageStatus.canTransition(
            MessageStatus.fromString(current),
            MessageStatus.fromString(next)
        )
    }
}
