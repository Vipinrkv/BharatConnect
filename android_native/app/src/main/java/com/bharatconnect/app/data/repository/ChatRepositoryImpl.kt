package com.bharatconnect.app.data.repository

import com.bharatconnect.app.BharatConnectApp
import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.core.notifications.NotificationHelper
import com.bharatconnect.app.core.contacts.ContactsManager
import com.bharatconnect.app.core.encryption.SignalEncryptionManager
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

    private suspend fun ensureAuthSession() {
        if (supabase.auth.currentUserOrNull() == null) {
            val (access, refresh) = com.bharatconnect.app.core.session.SessionManager.getAuthTokens()
            if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
                try {
                    supabase.auth.importAuthToken(access, refresh)
                } catch (_: Exception) {}
            }
        }
    }

    private fun resolveCurrentUserId(): String? {
        val authId = supabase.auth.currentUserOrNull()?.id
        if (!authId.isNullOrBlank()) return authId

        val sessionUser = com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()
        if (!sessionUser?.id.isNullOrBlank()) return sessionUser!!.id

        return null
    }

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
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            val local = conversationDao.getAllConversations().map { it.toDomain() }
            return@withContext Result.success(local)
        }
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
            val allMembers = try {
                supabase.postgrest["conversation_members"]
                    .select()
                    .decodeList<ConversationMemberDto>()
            } catch (_: Exception) {
                emptyList()
            }
            val memberConvIds = allMembers.filter { it.userId == currentUserId }.map { it.conversationId }.toSet()
            val counterpartByConvId = allMembers
                .filter { it.userId != currentUserId }
                .associate { it.conversationId to it.userId }

            // 3. Find conversations from messages where user sent or received
            val myMessageConvIds = try {
                supabase.postgrest["messages"].select {
                    filter { eq("sender_id", currentUserId) }
                }.decodeList<MessageDto>().map { it.conversationId }.toSet()
            } catch (_: Exception) { emptySet() }

            // Filter for conversations relevant to this user
            val myConversations = remoteConversations.filter { conv ->
                conv.createdBy == currentUserId ||
                memberConvIds.contains(conv.id) ||
                myMessageConvIds.contains(conv.id)
            }

            // Fetch profiles to resolve the other participant's name for direct chats
            val allProfiles = try {
                supabase.postgrest["profiles"].select().decodeList<ProfileDto>()
            } catch (_: Exception) {
                emptyList()
            }.associateBy { it.id }

            val entities = myConversations.map { conv ->
                val otherId = counterpartByConvId[conv.id] ?: if (conv.createdBy != currentUserId) conv.createdBy else null
                val otherProfile = otherId?.let { allProfiles[it] }

                val resolvedTitle = if (conv.type == "direct" || conv.id.startsWith("direct_")) {
                    ContactsManager.resolveCounterpartDisplayName(
                        context = BharatConnectApp.appContext,
                        phoneNumber = otherProfile?.phoneNumber,
                        fullName = otherProfile?.fullName,
                        username = otherProfile?.username,
                        fallbackTitle = conv.title
                    )
                } else {
                    conv.title ?: "Group Conversation"
                }

                val decryptedLastMsg = conv.lastMessage?.let { SignalEncryptionManager.decrypt(conv.id, it) }
                ConversationEntity.fromDomain(conv.toDomain(resolvedTitle, decryptedLastMsg))
            }

            conversationDao.insertConversations(entities)
            val localConvs = conversationDao.getAllConversations().map { it.toDomain() }
            val remoteIds = entities.map { it.id }.toSet()
            val merged = entities.map { it.toDomain() } + localConvs.filter { !remoteIds.contains(it.id) }
            Result.success(merged)
        } catch (e: Exception) {
            // Offline fallback to Room
            val local = conversationDao.getAllConversations().map { it.toDomain() }
            Result.success(local)
        }
    }

    override suspend fun fetchMessages(conversationId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        try {
            val remoteMessages = supabase.postgrest["messages"]
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                }
                .decodeList<MessageDto>()

            val currentUserId = resolveCurrentUserId()
            val entities = remoteMessages.map { 
                val decrypted = SignalEncryptionManager.decrypt(it.conversationId, it.content)
                if (currentUserId != null && it.senderId != currentUserId && it.status == "sent") {
                    try { acknowledgeMessageDelivered(it.id, conversationId) } catch (_: Exception) {}
                }
                MessageEntity.fromDomain(it.toDomain().copy(content = decrypted))
            }
            messageDao.insertMessages(entities)

            val local = messageDao.getMessagesByConversation(conversationId).map { it.toDomain() }
            Result.success(local)
        } catch (e: Exception) {
            // Offline fallback to Room
            val local = messageDao.getMessagesByConversation(conversationId).map { it.toDomain() }
            Result.success(local)
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        mediaUrl: String?,
        mediaType: String?
    ): Result<Message> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))
        val messageId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val senderProfile = com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()
        val senderName = senderProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: senderProfile?.username?.takeIf { it.isNotBlank() }
            ?: "You"

        val localMessage = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            senderName = senderName,
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
            val encryptedContent = SignalEncryptionManager.encrypt(conversationId, content)
            val messageDto = MessageDto(
                id = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                senderName = senderName,
                content = encryptedContent,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                status = "sent",
                createdAt = timestamp
            )

            supabase.postgrest["messages"].upsert(messageDto)

            val finalMessage = localMessage.copy(status = "sent", isPendingSync = false)
            // 3. Mark synced in Room DB
            messageDao.updateMessageStatus(messageId, "sent", false)

            // 4. Update or upsert conversation in remote Supabase
            try {
                val convEntity = conversationDao.getConversationById(conversationId)
                val convTitle = convEntity?.title ?: "Chat"
                supabase.postgrest["conversations"].upsert(
                    ConversationDto(
                        id = conversationId,
                        type = "direct",
                        title = convTitle,
                        createdBy = currentUserId,
                        lastMessage = encryptedContent,
                        lastMessageTime = timestamp,
                        createdAt = timestamp
                    )
                )
            } catch (_: Exception) {}

            // 5. Notify recipient in public.notifications
            val recipientId = try {
                supabase.postgrest["conversation_members"].select {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("user_id", currentUserId)
                    }
                }.decodeList<ConversationMemberDto>().firstOrNull()?.userId
            } catch (_: Exception) { null }
                ?: if (conversationId.startsWith("direct_")) {
                    val parts = conversationId.removePrefix("direct_").split("_")
                    if (parts.size == 2) {
                        if (parts[0] == currentUserId) parts[1] else parts[0]
                    } else null
                } else null

            // Ensure both members are registered in conversation_members
            if (recipientId != null && recipientId != currentUserId) {
                try {
                    val m1 = java.util.UUID.nameUUIDFromBytes("${conversationId}_${currentUserId}".toByteArray()).toString()
                    val m2 = java.util.UUID.nameUUIDFromBytes("${conversationId}_${recipientId}".toByteArray()).toString()
                    supabase.postgrest["conversation_members"].upsert(
                        ConversationMemberDto(id = m1, conversationId = conversationId, userId = currentUserId, role = "member")
                    )
                    supabase.postgrest["conversation_members"].upsert(
                        ConversationMemberDto(id = m2, conversationId = conversationId, userId = recipientId, role = "member")
                    )
                } catch (_: Exception) {}

                try {
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
                val encryptedContent = SignalEncryptionManager.encrypt(msg.conversationId, msg.content)
                val messageDto = MessageDto(
                    id = msg.id,
                    conversationId = msg.conversationId,
                    senderId = msg.senderId,
                    senderName = msg.senderName ?: "User",
                    content = encryptedContent,
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
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Generate deterministic conversation ID for 1-on-1 pairs
        val sortedIds = listOf(currentUserId, participantId).sorted()
        val deterministicKey = "${sortedIds[0]}_${sortedIds[1]}"
        val convId = java.util.UUID.nameUUIDFromBytes(deterministicKey.toByteArray()).toString()

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

            // 3. Register both users as members in remote Supabase
            val validParticipantUuid = try {
                java.util.UUID.fromString(participantId)
                participantId
            } catch (_: Exception) {
                java.util.UUID.nameUUIDFromBytes(participantId.toByteArray()).toString()
            }
            val member1Id = java.util.UUID.nameUUIDFromBytes("${convId}_${currentUserId}".toByteArray()).toString()
            val member2Id = java.util.UUID.nameUUIDFromBytes("${convId}_${validParticipantUuid}".toByteArray()).toString()
            supabase.postgrest["conversation_members"].upsert(
                ConversationMemberDto(
                    id = member1Id,
                    conversationId = convId,
                    userId = currentUserId,
                    role = "member",
                    joinedAt = timestamp
                )
            )
            supabase.postgrest["conversation_members"].upsert(
                ConversationMemberDto(
                    id = member2Id,
                    conversationId = convId,
                    userId = validParticipantUuid,
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
                                val currentUserId = supabase.auth.currentUserOrNull()?.id
                                val decrypted = SignalEncryptionManager.decrypt(record.conversationId, record.content)
                                val domainMsg = record.toDomain().copy(content = decrypted)
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(domainMsg))

                                // If receiver is viewing this conversation, mark as read immediately!
                                if (currentUserId != null && record.senderId != currentUserId) {
                                    markMessagesAsRead(conversationId)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    is PostgresAction.Update -> {
                        try {
                            val record = action.decodeRecord<MessageDto>()
                            if (record.conversationId == conversationId) {
                                val decrypted = SignalEncryptionManager.decrypt(record.conversationId, record.content)
                                val domainMsg = record.toDomain().copy(content = decrypted)
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(domainMsg))
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
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext
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
                            if (record.senderId != currentUserId) {
                                val sortedPair = listOf(currentUserId, record.senderId).sorted()
                                val expectedDirectId = UUID.nameUUIDFromBytes("${sortedPair[0]}_${sortedPair[1]}".toByteArray()).toString()
                                val isMyDirectChat = (record.conversationId == expectedDirectId)

                                val isMyConv = isMyDirectChat ||
                                    conversationDao.getConversationById(record.conversationId) != null ||
                                    record.conversationId.contains(currentUserId) ||
                                    try {
                                        supabase.postgrest["conversation_members"].select {
                                            filter {
                                                eq("conversation_id", record.conversationId)
                                                eq("user_id", currentUserId)
                                            }
                                        }.decodeList<ConversationMemberDto>().isNotEmpty()
                                    } catch (_: Exception) { false }

                                if (isMyConv) {
                                    val decrypted = SignalEncryptionManager.decrypt(record.conversationId, record.content)
                                    val domainMsg = record.toDomain().copy(content = decrypted)
                                    messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(domainMsg))

                                    // Acknowledge delivery to sender so sender gets double ticks!
                                    if (record.status == "sent") {
                                        acknowledgeMessageDelivered(record.id, record.conversationId)
                                    }

                                    // Resolve sender name (WhatsApp style: device phonebook if saved, else phone number)
                                    val senderProfile = try {
                                        supabase.postgrest["profiles"].select {
                                            filter { eq("id", record.senderId) }
                                        }.decodeSingleOrNull<ProfileDto>()
                                    } catch (_: Exception) { null }
                                    val senderName = ContactsManager.resolveCounterpartDisplayName(
                                        context = BharatConnectApp.appContext,
                                        phoneNumber = senderProfile?.phoneNumber,
                                        fullName = senderProfile?.fullName,
                                        username = senderProfile?.username
                                    )

                                    // Ensure conversation exists in local Room DB
                                    val existingConv = conversationDao.getConversationById(record.conversationId)
                                    if (existingConv == null) {
                                        val newConv = Conversation(
                                            id = record.conversationId,
                                            isGroup = false,
                                            title = senderName,
                                            createdBy = record.senderId,
                                            lastMessage = decrypted,
                                            lastMessageTime = record.createdAt,
                                            unreadCount = 1
                                        )
                                        conversationDao.insertOrUpdateConversation(ConversationEntity.fromDomain(newConv))
                                    } else {
                                        conversationDao.updateLastMessage(
                                            record.conversationId,
                                            decrypted,
                                            senderName,
                                            record.createdAt ?: ""
                                        )
                                    }

                                    onNewMessage?.invoke(domainMsg, senderName)

                                    // Trigger native heads-up system alert
                                    NotificationHelper.showMessageNotification(
                                        context = BharatConnectApp.appContext,
                                        title = senderName,
                                        body = decrypted,
                                        conversationId = record.conversationId
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    is PostgresAction.Update -> {
                        try {
                            val record = action.decodeRecord<MessageDto>()
                            val decrypted = SignalEncryptionManager.decrypt(record.conversationId, record.content)
                            messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(record.toDomain().copy(content = decrypted)))
                        } catch (_: Exception) {}
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun fetchNotifications(): Result<List<NotificationDto>> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.success(emptyList())
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
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.success(Unit)
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

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Wipe all messages and conversation locally from Room DB
            messageDao.deleteMessagesByConversation(conversationId)
            conversationDao.deleteConversation(conversationId)

            // 2. Permanently delete from remote Supabase
            try {
                supabase.postgrest["messages"].delete {
                    filter { eq("conversation_id", conversationId) }
                }
            } catch (_: Exception) {}

            try {
                supabase.postgrest["conversation_members"].delete {
                    filter { eq("conversation_id", conversationId) }
                }
            } catch (_: Exception) {}

            try {
                supabase.postgrest["conversations"].delete {
                    filter { eq("id", conversationId) }
                }
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.success(Unit)
        try {
            // Update local Room database
            messageDao.markIncomingMessagesRead(conversationId, currentUserId, "read")

            // Update remote Supabase
            try {
                supabase.postgrest["messages"].update({
                    set("status", "read")
                }) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("sender_id", currentUserId)
                        neq("status", "read")
                    }
                }
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeMessageDelivered(messageId: String, conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest["messages"].update({
                set("status", "delivered")
            }) {
                filter {
                    eq("id", messageId)
                    eq("status", "sent")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
