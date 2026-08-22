package com.bharatconnect.app.data.repository

import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.data.local.room.entity.ConversationEntity
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.data.remote.dto.ConversationDto
import com.bharatconnect.app.data.remote.dto.MessageDto
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepositoryImpl : ChatRepository {

    private val db = DatabaseProvider.getDatabase()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val supabase = SupabaseClient.client

    override fun getConversationsFlow(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesByConversationFlow(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun fetchConversations(): Result<List<Conversation>> = withContext(Dispatchers.IO) {
        try {
            val remoteConversations = supabase.postgrest["conversations"]
                .select()
                .decodeList<ConversationDto>()

            val entities = remoteConversations.map { ConversationEntity.fromDomain(it.toDomain()) }
            conversationDao.insertConversations(entities)

            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            // Offline fallback to Room
            val local = conversationDao.getAllConversationsFlow()
            Result.success(emptyList())
        }
    }

    override suspend fun fetchMessages(conversationId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val remoteMessages = supabase.postgrest["messages"]
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                }
                .decodeList<MessageDto>()

            val entities = remoteMessages.map { MessageEntity.fromDomain(it.toDomain()) }
            messageDao.insertMessages(entities)

            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        mediaUrl: String?,
        mediaType: String?
    ): Result<Message> = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: "local_user"
        val tempId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val localMessage = Message(
            id = tempId,
            conversationId = conversationId,
            senderId = currentUserId,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            status = "sending",
            createdAt = timestamp,
            isPendingSync = true
        )

        // 1. Instantly save to Room local DB for 0ms UI latency
        messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(localMessage))
        conversationDao.updateLastMessage(conversationId, content, "You", timestamp)

        // 2. Dispatch to Supabase
        try {
            val messageDto = MessageDto(
                conversationId = conversationId,
                senderId = currentUserId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                status = "sent"
            )

            val inserted = supabase.postgrest["messages"]
                .insert(messageDto) {
                    select()
                }
                .decodeSingle<MessageDto>()

            val finalMessage = inserted.toDomain()
            // 3. Reconcile in Room DB
            messageDao.deleteMessage(tempId)
            messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(finalMessage))

            Result.success(finalMessage)
        } catch (e: Exception) {
            // Keep in Room DB with failed/pending_sync status for WorkManager offline retry
            messageDao.updateMessageStatus(tempId, "failed", true)
            Result.success(localMessage.copy(status = "failed"))
        }
    }

    override suspend fun retryPendingMessages(): Result<Int> = withContext(Dispatchers.IO) {
        val pending = messageDao.getPendingSyncMessages()
        var successCount = 0

        for (msg in pending) {
            try {
                val messageDto = MessageDto(
                    conversationId = msg.conversationId,
                    senderId = msg.senderId,
                    content = msg.content,
                    mediaUrl = msg.mediaUrl,
                    mediaType = msg.mediaType,
                    status = "sent"
                )
                val inserted = supabase.postgrest["messages"]
                    .insert(messageDto) {
                        select()
                    }
                    .decodeSingle<MessageDto>()

                messageDao.deleteMessage(msg.id)
                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(inserted.toDomain()))
                successCount++
            } catch (_: Exception) {}
        }
        Result.success(successCount)
    }
}
