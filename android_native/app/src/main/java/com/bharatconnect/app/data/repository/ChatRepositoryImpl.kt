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
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
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
        val messageId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val localMessage = Message(
            id = messageId,
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

        // 2. Dispatch to Supabase idempotently with client-assigned message ID
        try {
            val messageDto = MessageDto(
                id = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                status = "sent",
                createdAt = timestamp
            )

            val inserted = supabase.postgrest["messages"]
                .upsert(messageDto) {
                    select()
                }
                .decodeSingle<MessageDto>()

            val finalMessage = inserted.toDomain()
            // 3. Mark synced in Room DB
            messageDao.updateMessageStatus(messageId, "sent", false)

            Result.success(finalMessage)
        } catch (e: Exception) {
            // Keep in Room DB with failed/pending_sync status for WorkManager offline retry
            messageDao.updateMessageStatus(messageId, "failed", true)
            Result.success(localMessage.copy(status = "failed"))
        }
    }

    override suspend fun retryPendingMessages(): Result<Int> = withContext(Dispatchers.IO) {
        val pending = messageDao.getPendingSyncMessages()
        var successCount = 0

        for (msg in pending) {
            try {
                val messageDto = MessageDto(
                    id = msg.id,
                    conversationId = msg.conversationId,
                    senderId = msg.senderId,
                    content = msg.content,
                    mediaUrl = msg.mediaUrl,
                    mediaType = msg.mediaType,
                    status = "sent",
                    createdAt = msg.createdAt
                )
                supabase.postgrest["messages"].upsert(messageDto)
                messageDao.updateMessageStatus(msg.id, "sent", false)
                successCount++
            } catch (_: Exception) {}
        }
        Result.success(successCount)
    }

    override suspend fun getOrCreateDirectConversation(participantId: String, title: String): Result<Conversation> = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: "local_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Generate deterministic conversation ID for 1-on-1 pairs
        val sortedIds = listOf(currentUserId, participantId).sorted()
        val convId = "direct_${sortedIds[0]}_${sortedIds[1]}"

        val conversation = Conversation(
            id = convId,
            isGroup = false,
            title = title,
            createdBy = currentUserId,
            lastMessage = "Start your conversation with $title",
            lastMessageTime = timestamp,
            unreadCount = 0
        )

        // 1. Immediately insert/update locally in Room
        conversationDao.insertOrUpdateConversation(ConversationEntity.fromDomain(conversation))

        // 2. Sync to Supabase remote
        try {
            val convDto = ConversationDto(
                id = convId,
                isGroup = false,
                title = title,
                createdBy = currentUserId,
                createdAt = timestamp
            )
            supabase.postgrest["conversations"].upsert(convDto)
        } catch (_: Exception) {}

        Result.success(conversation)
    }

    private var activeRealtimeChannel: RealtimeChannel? = null

    override suspend fun subscribeToRealtime(conversationId: String): Unit = withContext(Dispatchers.IO) {
        try {
            unsubscribeRealtime()
            val channel = supabase.realtime.channel("messages_$conversationId")
            activeRealtimeChannel = channel

            channel.subscribe()

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public")
            changeFlow.collect { action: PostgresAction ->
                when (action) {
                    is PostgresAction.Insert -> {
                        try {
                            val record = action.decodeRecord<MessageDto>()
                            if (record.conversationId == conversationId) {
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(record.toDomain()))
                            }
                        } catch (_: Exception) {}
                    }
                    is PostgresAction.Update -> {
                        try {
                            val record = action.decodeRecord<MessageDto>()
                            if (record.conversationId == conversationId) {
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(record.toDomain()))
                            }
                        } catch (_: Exception) {}
                    }
                    is PostgresAction.Delete -> {
                        try {
                            val oldRecord = action.decodeOldRecord<MessageDto>()
                            messageDao.deleteMessage(oldRecord.id)
                        } catch (_: Exception) {}
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun unsubscribeRealtime(): Unit = withContext(Dispatchers.IO) {
        try {
            activeRealtimeChannel?.let { channel ->
                supabase.realtime.removeChannel(channel)
                activeRealtimeChannel = null
            }
        } catch (_: Exception) {}
        Unit
    }
}
