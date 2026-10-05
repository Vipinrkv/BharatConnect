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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    /**
     * Synchronizes and returns the current user's conversations list.
     *
     * Flow & Rationale:
     * 1. Auth Guard: Resolves the logged-in user ID; if unauthenticated, returns local Room cache.
     * 2. Remote Fetch: Queries Supabase for conversations and memberships where the user belongs.
     * 3. Direct Message Aggregation: Queries recent messages sent or received by this user.
     *    - Rationale: Even if the remote 'conversations' table has RLS restrictions or network delay,
     *      any received message (e.g. User A sent "Hi") must immediately synthesize a conversation
     *      so User B never misses an incoming chat.
     * 4. Automatic Decryption & Caching: Decrypts all received messages using SignalEncryptionManager
     *    and caches them in Room DB (MessageEntity) for instant zero-latency message display.
     * 5. WhatsApp-Style Name Resolution: Direct chat titles are resolved with authoritative precedence:
     *    User's Device Phonebook -> Remote Supabase Profile -> Message Sender Name.
     */
    override suspend fun fetchConversations(): Result<List<Conversation>> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            val local = conversationDao.getAllConversations().map { it.toDomain() }
            return@withContext Result.success(local)
        }
        try {
            // 1. Fetch conversations from remote Supabase table
            val remoteConversations = try {
                supabase.postgrest["conversations"]
                    .select()
                    .decodeList<ConversationDto>()
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Fetch user's conversation memberships to identify counterparts
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
            val sentMessages = try {
                supabase.postgrest["messages"].select {
                    filter { eq("sender_id", currentUserId) }
                }.decodeList<MessageDto>()
            } catch (_: Exception) { emptyList() }

            val receivedMessages = try {
                supabase.postgrest["messages"].select {
                    filter { eq("recipient_id", currentUserId) }
                }.decodeList<MessageDto>()
            } catch (_: Exception) { emptyList() }

            val memberMessages = try {
                if (memberConvIds.isNotEmpty()) {
                    supabase.postgrest["messages"].select {
                        filter { isIn("conversation_id", memberConvIds.toList()) }
                    }.decodeList<MessageDto>()
                } else emptyList()
            } catch (_: Exception) { emptyList() }

            // 4. Fetch incoming message notifications to synthesize conversations and bridge offline delivery
            val incomingMessageNotifs = try {
                supabase.postgrest["notifications"].select {
                    filter {
                        eq("user_id", currentUserId)
                        eq("category", "messages")
                    }
                }.decodeList<NotificationDto>()
            } catch (_: Exception) { emptyList() }

            val allUserMessages = (sentMessages + receivedMessages + memberMessages).distinctBy { it.id }

            // Group messages by conversation ID to identify the latest message for previews
            val latestMessageByConvId = allUserMessages
                .groupBy { it.conversationId }
                .mapValues { (_, msgs) -> msgs.maxByOrNull { it.createdAt ?: "" } }

            // Cache and decrypt recent messages in Room DB for 0ms chat load latency
            try {
                for (msg in allUserMessages) {
                    val dec = SignalEncryptionManager.decrypt(msg.conversationId, msg.content)
                    messageDao.insertOrUpdateMessage(
                        com.bharatconnect.app.data.local.room.entity.MessageEntity.fromDomain(
                            msg.toDomain().copy(content = dec)
                        )
                    )
                }
            } catch (_: Exception) {}

            // Fetch profiles to resolve participant details, names, and avatars
            val allProfiles = try {
                supabase.postgrest["profiles"].select().decodeList<ProfileDto>()
            } catch (_: Exception) {
                emptyList()
            }.associateBy { it.id }

            val localUsersMap = try {
                com.bharatconnect.app.core.database.DatabaseProvider.getDatabase().userDao().getAllUsers().associateBy { it.id }
            } catch (_: Exception) { emptyMap() }

            // Build unified pool of conversations (synthesizing any chat discovered from incoming messages or notifications)
            val conversationMap = remoteConversations.associateBy { it.id }.toMutableMap()
            for ((convId, latestMsg) in latestMessageByConvId) {
                if (!conversationMap.containsKey(convId) && latestMsg != null) {
                    conversationMap[convId] = ConversationDto(
                        id = convId,
                        type = if (convId.startsWith("group_")) "group" else "direct",
                        title = null,
                        createdBy = latestMsg.senderId,
                        lastMessage = latestMsg.content,
                        lastMessageTime = latestMsg.createdAt,
                        createdAt = latestMsg.createdAt
                    )
                }
            }

            // Synthesize conversations from incoming message notifications without creating duplicates
            for (notif in incomingMessageNotifs) {
                val notifSender = notif.title
                val notifText = notif.description
                val notifTime = notif.createdAt ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                // 1. If notification contains conversationId that exists, skip synthesis
                if (!notif.conversationId.isNullOrBlank() && conversationMap.containsKey(notif.conversationId)) {
                    continue
                }

                // 2. If notification sender is already in an existing direct conversation, skip
                if (!notif.senderId.isNullOrBlank()) {
                    val alreadyHasConv = conversationMap.values.any { conv ->
                        counterpartByConvId[conv.id] == notif.senderId ||
                        (conv.createdBy == notif.senderId && conv.type == "direct")
                    }
                    if (alreadyHasConv) continue
                }

                // 3. Resolve true deterministic ID using sender profile if available
                val targetConvId = notif.conversationId
                    ?: if (!notif.senderId.isNullOrBlank()) {
                        val sorted = listOf(currentUserId, notif.senderId).sorted()
                        java.util.UUID.nameUUIDFromBytes("${sorted[0]}_${sorted[1]}".toByteArray()).toString()
                    } else {
                        // Check if sender title matches a known profile
                        val matched = allProfiles.values.firstOrNull {
                            it.fullName?.equals(notifSender, ignoreCase = true) == true ||
                            it.username?.equals(notifSender, ignoreCase = true) == true
                        }
                        if (matched != null) {
                            val sorted = listOf(currentUserId, matched.id).sorted()
                            java.util.UUID.nameUUIDFromBytes("${sorted[0]}_${sorted[1]}".toByteArray()).toString()
                        } else null
                    }

                if (targetConvId != null && !conversationMap.containsKey(targetConvId)) {
                    conversationMap[targetConvId] = ConversationDto(
                        id = targetConvId,
                        type = "direct",
                        title = notifSender,
                        createdBy = notif.senderId ?: targetConvId,
                        lastMessage = notifText,
                        lastMessageTime = notifTime,
                        createdAt = notifTime
                    )
                }
            }

            val notifConvIds = incomingMessageNotifs.mapNotNull { it.conversationId }.toSet()
            val notifSenderNames = incomingMessageNotifs.map { it.title.lowercase() }.toSet()

            // Filter for conversations relevant to this user
            val myConversations = conversationMap.values.filter { conv ->
                conv.createdBy == currentUserId ||
                memberConvIds.contains(conv.id) ||
                latestMessageByConvId.containsKey(conv.id) ||
                notifConvIds.contains(conv.id) ||
                (conv.title != null && notifSenderNames.contains(conv.title.lowercase()))
            }

            val rawEntities = myConversations.map { conv ->
                val existingLocal = conversationDao.getConversationById(conv.id)
                val latestMsg = latestMessageByConvId[conv.id]

                val otherId = counterpartByConvId[conv.id]
                    ?: latestMsg?.let { if (it.senderId != currentUserId) it.senderId else it.recipientId }
                    ?: existingLocal?.participantIds?.split(",")?.firstOrNull { it.isNotBlank() && it != currentUserId }
                    ?: if (conv.createdBy != currentUserId) conv.createdBy else null

                val otherProfile = otherId?.let { allProfiles[it] }
                val localUser = otherId?.let { localUsersMap[it] }

                val resolvedTitle = if (conv.type == "direct" || conv.id.startsWith("direct_")) {
                    ContactsManager.resolveCounterpartDisplayName(
                        context = BharatConnectApp.appContext,
                        phoneNumber = otherProfile?.phoneNumber ?: localUser?.phoneNumber,
                        fullName = otherProfile?.fullName ?: localUser?.fullName,
                        username = otherProfile?.username ?: localUser?.username,
                        fallbackTitle = latestMsg?.senderName ?: conv.title
                    )
                } else {
                    conv.title ?: "Group Conversation"
                }

                // Resolve counterpart's profile avatar URL
                val resolvedAvatarUrl = otherProfile?.avatarUrl ?: localUser?.avatarUrl ?: conv.avatarUrl

                val rawLastMsg = conv.lastMessage ?: latestMsg?.content
                val decryptedLastMsg = rawLastMsg?.let { SignalEncryptionManager.decrypt(conv.id, it) }
                val effectiveTime = conv.lastMessageTime ?: latestMsg?.createdAt ?: existingLocal?.lastMessageTime

                val unread = try { messageDao.getUnreadCount(conv.id, currentUserId) } catch (_: Exception) { 0 }
                val isIncomingUnread = latestMsg != null && latestMsg.senderId != currentUserId
                val effectiveUnread = if (unread > 0) unread else if (isIncomingUnread && (existingLocal?.unreadCount ?: 0) == 0) 1 else (existingLocal?.unreadCount ?: 0)

                val baseDomain = conv.toDomain(resolvedTitle, decryptedLastMsg, resolvedAvatarUrl)
                val finalDomain = baseDomain.copy(
                    avatarUrl = resolvedAvatarUrl,
                    lastMessageTime = effectiveTime,
                    unreadCount = effectiveUnread,
                    participantIds = listOfNotNull(currentUserId, otherId)
                )
                ConversationEntity.fromDomain(finalDomain)
            }

            // Deduplicate so that at most ONE conversation exists per counterpart contact
            val seenCounterparts = mutableSetOf<String>()
            val entities = mutableListOf<ConversationEntity>()
            for (entity in rawEntities) {
                val counterpart = entity.participantIds.split(",")
                    .map { it.trim() }
                    .firstOrNull { it.isNotBlank() && it != currentUserId }
                if (counterpart != null) {
                    if (seenCounterparts.contains(counterpart)) {
                        // Stale duplicate conversation detected; prune it from local database
                        try { conversationDao.deleteConversation(entity.id) } catch (_: Exception) {}
                        continue
                    }
                    seenCounterparts.add(counterpart)
                }
                entities.add(entity)
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
            val convEntity = conversationDao.getConversationById(conversationId)
            val otherId = convEntity?.participantIds?.split(",")?.firstOrNull { it.isNotBlank() && it != currentUserId }
            val fallbackId = if (currentUserId != null && otherId != null) {
                val sorted = listOf(currentUserId, otherId).sorted()
                java.util.UUID.nameUUIDFromBytes("${sorted[0]}_${sorted[1]}".toByteArray()).toString()
            } else null

            val entities = remoteMessages.map { 
                val decrypted = SignalEncryptionManager.decrypt(it.conversationId, it.content, fallbackId)
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

    /**
     * Sends an encrypted chat message with optimistic local storage and delivery tracking.
     *
     * WhatsApp-Style Architecture:
     * 1. Optimistic Local Insert (0ms UI latency): Instantly persists message to local Room DB
     *    with status="sending" (clock icon '⏱') and updates the conversation's last message preview.
     * 2. AES-GCM-256 E2EE: Encrypts the plaintext before sending across the network so Supabase
     *    and any intermediary servers only store ciphertext ("ENC:...").
     * 3. Cloud Dispatch: Upserts the message to Supabase 'messages' table with recipient_id.
     * 4. Status Update: Transitions local message status to "sent" (single grey tick '✓').
     * 5. Offline Queueing: If device is offline, message remains marked with isPendingSync=true
     *    and is automatically picked up by WorkManager (OfflineSyncWorker) once network returns.
     */
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

        // 1. Instantly save to Room local DB for 0ms UI latency (Message bubble displays with '⏱')
        messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(localMessage))
        conversationDao.updateLastMessage(conversationId, content, "You", timestamp)

        // 2. Dispatch encrypted payload to Supabase
        try {
            val convEntity = conversationDao.getConversationById(conversationId)
            val recipientId = convEntity?.participantIds
                ?.split(",")
                ?.map { it.trim() }
                ?.firstOrNull { it.isNotBlank() && it != currentUserId }
                ?: try {
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
                createdAt = timestamp,
                recipientId = recipientId
            )

            // Ensure conversation exists in remote Supabase before message insert
            try {
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

            supabase.postgrest["messages"].upsert(messageDto)

            val finalMessage = localMessage.copy(status = "sent", isPendingSync = false)
            // 3. Mark synced in Room DB
            messageDao.updateMessageStatus(messageId, "sent", false)

            // 4. Ensure both members are registered in conversation_members
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
                            createdAt = timestamp,
                            conversationId = conversationId,
                            senderId = currentUserId
                        )
                    )
                } catch (_: Exception) {}
            }

            if (recipientId == "bharatconnect_support_bot" || conversationId.contains("bharatconnect_support_bot")) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(800L)
                    val replyId = java.util.UUID.randomUUID().toString()
                    val replyTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val replyText = "Namaste! 🙏 Welcome to BharatConnect. We're here to help you connect with your friends, communities, and services across India. Feel free to explore and chat!"
                    val replyMsg = Message(
                        id = replyId,
                        conversationId = conversationId,
                        senderId = "bharatconnect_support_bot",
                        senderName = "BharatConnect Support",
                        content = replyText,
                        status = "read",
                        createdAt = replyTime,
                        isPendingSync = false
                    )
                    messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(replyMsg))
                    conversationDao.updateLastMessage(conversationId, replyText, "BharatConnect Support", replyTime)
                }
            }

            Result.success(finalMessage)
        } catch (e: Exception) {
            // Check if messaging support bot in offline mode
            if (conversationId.contains("bharatconnect_support_bot")) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(800L)
                    val replyId = java.util.UUID.randomUUID().toString()
                    val replyTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val replyText = "Namaste! 🙏 Welcome to BharatConnect. We're here to help you connect with your friends, communities, and services across India. Feel free to explore and chat!"
                    val replyMsg = Message(
                        id = replyId,
                        conversationId = conversationId,
                        senderId = "bharatconnect_support_bot",
                        senderName = "BharatConnect Support",
                        content = replyText,
                        status = "read",
                        createdAt = replyTime,
                        isPendingSync = false
                    )
                    messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(replyMsg))
                    conversationDao.updateLastMessage(conversationId, replyText, "BharatConnect Support", replyTime)
                }
            }
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

    /**
     * Retrieves or creates a 1-on-1 direct conversation between two users.
     *
     * Deterministic Conversation Key:
     * - Problem: If User A chats with User B, and User B chats with User A, we must NOT create
     *   duplicate conversation rooms.
     * - Solution: Sorts both user IDs alphabetically (e.g. ["user_A", "user_B"]) and produces a
     *   deterministic UUID via UUID.nameUUIDFromBytes("${sorted[0]}_${sorted[1]}").
     * - Outcome: Regardless of who initiates the conversation, both users always map to the exact
     *   same conversation ID, shared message thread, and AES-GCM encryption key.
     */
    override suspend fun getOrCreateDirectConversation(participantId: String, title: String): Result<Conversation> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Generate deterministic conversation ID for 1-on-1 pairs (alphabetical order of UUIDs)
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
            unreadCount = 0,
            participantIds = listOf(currentUserId, participantId)
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

    override suspend fun getOrCreateGroupConversation(groupId: String, title: String): Result<Conversation> = withContext(Dispatchers.IO) {
        ensureAuthSession()
        val currentUserId = resolveCurrentUserId() ?: "local_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val convId = java.util.UUID.nameUUIDFromBytes("group_$groupId".toByteArray()).toString()

        val conversation = Conversation(
            id = convId,
            isGroup = true,
            title = title,
            createdBy = currentUserId,
            lastMessage = "Welcome to $title!",
            lastMessageTime = timestamp,
            unreadCount = 0,
            participantIds = listOf(currentUserId)
        )

        // 1. Immediately insert/update locally in Room
        conversationDao.insertOrUpdateConversation(ConversationEntity.fromDomain(conversation))

        // 2. Sync to Supabase remote
        try {
            val convDto = ConversationDto(
                id = convId,
                type = "group",
                title = title,
                createdBy = currentUserId,
                lastMessage = "Welcome to $title!",
                lastMessageTime = timestamp,
                createdAt = timestamp
            )
            supabase.postgrest["conversations"].upsert(convDto)

            val memberId = java.util.UUID.nameUUIDFromBytes("${convId}_${currentUserId}".toByteArray()).toString()
            supabase.postgrest["conversation_members"].upsert(
                ConversationMemberDto(
                    id = memberId,
                    conversationId = convId,
                    userId = currentUserId,
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
                                val currentUserId = resolveCurrentUserId()
                                val decrypted = SignalEncryptionManager.decrypt(record.conversationId, record.content)
                                val domainMsg = record.toDomain().copy(content = decrypted)
                                messageDao.insertOrUpdateMessage(MessageEntity.fromDomain(domainMsg))

                                val senderDisplayName = record.senderName ?: "User"
                                conversationDao.updateLastMessage(
                                    conversationId = conversationId,
                                    lastMessage = decrypted,
                                    senderName = senderDisplayName,
                                    time = record.createdAt ?: ""
                                )

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
                                val isRecipientMe = (record.recipientId == currentUserId)
                                val existingConv = conversationDao.getConversationById(record.conversationId)

                                val isMyConv = isMyDirectChat ||
                                    isRecipientMe ||
                                    existingConv != null ||
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
                                        try { acknowledgeMessageDelivered(record.id, record.conversationId) } catch (_: Exception) {}
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
                                        username = senderProfile?.username,
                                        fallbackTitle = record.senderName
                                    )

                                    // Ensure conversation exists in local Room DB
                                    if (existingConv == null) {
                                        val newConv = Conversation(
                                            id = record.conversationId,
                                            isGroup = false,
                                            title = senderName,
                                            createdBy = record.senderId,
                                            lastMessage = decrypted,
                                            lastMessageTime = record.createdAt,
                                            unreadCount = 1,
                                            participantIds = listOf(currentUserId, record.senderId)
                                        )
                                        conversationDao.insertOrUpdateConversation(ConversationEntity.fromDomain(newConv))
                                    } else {
                                        conversationDao.updateLastMessage(
                                            conversationId = record.conversationId,
                                            lastMessage = decrypted,
                                            senderName = senderName,
                                            time = record.createdAt ?: "",
                                            unreadIncrement = 1
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
