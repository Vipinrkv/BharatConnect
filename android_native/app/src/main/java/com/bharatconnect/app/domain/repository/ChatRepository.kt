package com.bharatconnect.app.domain.repository

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversationsFlow(): Flow<List<Conversation>>
    fun getMessagesFlow(conversationId: String): Flow<List<Message>>
    
    suspend fun fetchConversations(): Result<List<Conversation>>
    suspend fun fetchMessages(conversationId: String): Result<List<Message>>
    suspend fun sendMessage(conversationId: String, content: String, mediaUrl: String? = null, mediaType: String? = null): Result<Message>
    suspend fun retryPendingMessages(): Result<Int>
    suspend fun getOrCreateDirectConversation(participantId: String, title: String): Result<Conversation>
    suspend fun subscribeToRealtime(conversationId: String)
    suspend fun unsubscribeRealtime()
    suspend fun subscribeToGlobalUserMessages(onNewMessage: ((Message, String) -> Unit)? = null)
    suspend fun fetchNotifications(): Result<List<com.bharatconnect.app.data.remote.dto.NotificationDto>>
    suspend fun markNotificationsRead(): Result<Unit>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
