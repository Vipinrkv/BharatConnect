package com.bharatconnect.app.data.repository

import com.bharatconnect.app.BharatConnectApp
import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.data.local.room.entity.ConversationEntity
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.data.remote.dto.ConversationDto
import com.bharatconnect.app.data.remote.dto.ConversationMemberDto
import com.bharatconnect.app.data.remote.dto.MessageDto
import com.bharatconnect.app.data.remote.dto.NotificationDto
import com.bharatconnect.app.data.remote.dto.ProfileDto
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
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
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.success(emptyList())
        try {
            // 1. Fetch conversations from remote
            val remoteConversations = try {
                supabase.postgrest["conversations"]
                    .select()
                    .decodeList<ConversationDto>()
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Fetch user's conversation memberships
            val memberConvIds = try {
                supabase.postgrest["conversation_members"]
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeList<ConversationMemberDto>()
                    .map { it.conversationId }
                    .toSet()
            } catch (_: Exception) {
                emptySet()
            }

            // Filter for conversations relevant to this user
            val myConversations = remoteConversations.filter { conv ->
                conv.createdBy == currentUserId ||
                conv.id.contains(currentUserId) ||
                memberConvIds.contains(conv.id)
            }

            // Fetch profiles to resolve the other participant's name for direct chats
            val allProfiles = try {
                supabase.postgrest["profiles"].select().decodeList<ProfileDto>()
            } catch (_: Exception) {
                emptyList()
            }.associateBy { it.id }

            val entities = myConversations.map { conv ->
                val resolvedTitle = if (conv.id.startsWith("direct_")) {
                    val parts = conv.id.removePrefix("direct_").split("_")
                    val otherId = if (parts.size == 2) {
                        if (parts[0] == currentUserId) parts[1] else parts[0]
                    } else null
                    val otherProfile = otherId?.let { allProfiles[it] }
                    otherProfile?.fullName?.takeIf { it.isNotBlank() }
                        ?: otherProfile?.username?.takeIf { it.isNotBlank() }
                        ?: conv.title
                        ?: "BharatConnect Member"
                } else {
                    conv.title ?: "Group Conversation"
                }

                ConversationEntity.fromDomain(conv.toDomain(resolvedTitle))
            }

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

        // 2. Dispatch to Supabase
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

            supabase.postgrest["messages"].upsert(messageDto)

            val finalMessage = messageDto.toDomain()
            // 3. Mark synced in Room DB
            messageDao.updateMessageStatus(messageId, "sent", false)

            // 4. Update conversation in remote Supabase
            try {
                supabase.postgrest["conversations"].update({
                    set("last_message", content)
                    set("last_message_time", timestamp)
                }) {
                    filter {
                        eq("id", conversationId)
                    }
                }
            } catch (_: Exception) {}

            // 5. Notify recipient in public.notifications
            if (conversationId.startsWith("direct_")) {
                val parts = conversationId.removePrefix("direct_").split("_")
                val recipientId = if (parts.size == 2) {
                    if (parts[0] == currentUserId) parts[1] else parts[0]
                } else null

                if (recipientId != null && recipientId != currentUserId) {
                    try {
                        val senderProfile = supabase.postgrest["profiles"].select {
                            filter { eq("id", currentUserId) }
                        }.decodeSingleOrNull<ProfileDto>()
                        val senderName = senderProfile?.fullName?.takeIf { it.isNotBlank() }
                            ?: senderProfile?.username?.takeIf { it.isNotBlank() }
                            ?: "BharatConnect Member"

                        supabase.postgrest["notifications"].insert(
                            NotificationDto(
                                userId = recipientId,
                                title = senderName,
                                description = content,
                                category = "messages",
                                isRead = false,
                                createdAt = timestamp
                            )
                        )
                    } catch (_: Exception) {}
                }
            }

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
                type = "direct",
                title = title,
                createdBy = currentUserId,
                lastMessage = "Start your conversation with $title",
                lastMessageTime = timestamp,
                createdAt = timestamp
            )
            supabase.postgrest["conversations"].upsert(convDto)

            // 3. Register both users as members
            supabase.postgrest["conversation_members"].upsert(
                ConversationMemberDto(
                    conversationId = convId,
                    userId = currentUserId,
                    role = "member",
                    joinedAt = timestamp
                )
            )
            supabase.postgrest["conversation_members"].upsert(
                ConversationMemberDto(
                    conversationId = convId,
                    userId = participantId,
                    role = "member",
                    joinedAt = timestamp
                )
            )
        } catch (_: Exception) {}

        Result.success(conversation)
    }

    private var activeRealtimeChannel: RealtimeChannel? = null
    private var globalRealtimeChannel: RealtimeChannel? = null

    override suspend fun subscribeToRealtime(conversationId: String): Unit = withContext(Dispatchers.IO) {
        try {
            unsubscribeRealtime()
            val channel = supabase.realtime.channel("messages_$conversationId")
            activeRealtimeChannel = channel

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }
            channel.subscribe()

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

    override suspend fun subscribeToGlobalUserMessages(onNewMessage: ((Message, String) -> Unit)?): Unit = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext
        try {
            globalRealtimeChannel?.let {
                try { supabase.realtime.removeChannel(it) } catch (_: Exception) {}
                globalRealtimeChannel = null
            }

            val channel = supabase.realtime.channel("global_user_$currentUserId")
            globalRealtimeChannel = channel

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }
            channel.subscribe()

            changeFlow.collect { action: PostgresAction ->
                when (action) {
                    is PostgresAction.Insert -> {
                        try {
                            val record = action.decodeRecord<MessageDto>()
                            if (record.senderId != currentUserId && record.conversationId.contains(currentUserId)) {
                                val domainMsg = record.toDomain()
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(domainMsg))

                                // Resolve sender name
                                val senderProfile = try {
                                    supabase.postgrest["profiles"].select {
                                        filter { eq("id", record.senderId) }
                                    }.decodeSingleOrNull<ProfileDto>()
                                } catch (_: Exception) { null }
                                val senderName = senderProfile?.fullName?.takeIf { it.isNotBlank() }
                                    ?: senderProfile?.username?.takeIf { it.isNotBlank() }
                                    ?: "BharatConnect Member"

                                // Ensure conversation exists in local Room DB
                                val existingConv = conversationDao.getConversationById(record.conversationId)
                                if (existingConv == null) {
                                    val newConv = Conversation(
                                        id = record.conversationId,
                                        isGroup = false,
                                        title = senderName,
                                        createdBy = record.senderId,
                                        lastMessage = record.content,
                                        lastMessageTime = record.createdAt,
                                        unreadCount = 1
                                    )
                                    conversationDao.insertOrUpdateConversation(ConversationEntity.fromDomain(newConv))
                                } else {
                                    conversationDao.updateLastMessage(
                                        record.conversationId,
                                        record.content,
                                        senderName,
                                        record.createdAt ?: ""
                                    )
                                }

                                onNewMessage?.invoke(domainMsg, senderName)

                                // Trigger native heads-up system alert
                                NotificationHelper.showMessageNotification(
                                    context = BharatConnectApp.appContext,
                                    title = senderName,
                                    body = record.content,
                                    conversationId = record.conversationId
                                )
                            }
                        } catch (_: Exception) {}
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun fetchNotifications(): Result<List<NotificationDto>> = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.success(emptyList())
        try {
            val list = supabase.postgrest["notifications"]
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<NotificationDto>()
                .sortedByDescending { it.createdAt }

            Result.success(list)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun markNotificationsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.success(Unit)
        try {
            supabase.postgrest["notifications"].update({
                set("is_read", true)
            }) {
                filter {
                    eq("user_id", currentUserId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
