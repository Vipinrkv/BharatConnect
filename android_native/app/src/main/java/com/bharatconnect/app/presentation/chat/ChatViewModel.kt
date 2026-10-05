package com.bharatconnect.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.data.repository.ChatRepositoryImpl
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import com.bharatconnect.app.domain.usecase.chat.FetchConversationsUseCase
import com.bharatconnect.app.domain.usecase.chat.FetchMessagesUseCase
import com.bharatconnect.app.domain.usecase.chat.GetConversationsUseCase
import com.bharatconnect.app.domain.usecase.chat.GetMessagesUseCase
import com.bharatconnect.app.domain.usecase.chat.SendMessageUseCase
import com.bharatconnect.app.domain.usecase.chat.SubscribeToRealtimeUseCase
import com.bharatconnect.app.domain.usecase.chat.UnsubscribeRealtimeUseCase
import com.bharatconnect.app.domain.usecase.chat.DeleteConversationUseCase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(
    chatRepository: ChatRepository = ChatRepositoryImpl(),
    private val getConversationsUseCase: GetConversationsUseCase = GetConversationsUseCase(chatRepository),
    private val getMessagesUseCase: GetMessagesUseCase = GetMessagesUseCase(chatRepository),
    private val sendMessageUseCase: SendMessageUseCase = SendMessageUseCase(chatRepository),
    private val fetchConversationsUseCase: FetchConversationsUseCase = FetchConversationsUseCase(chatRepository),
    private val fetchMessagesUseCase: FetchMessagesUseCase = FetchMessagesUseCase(chatRepository),
    private val subscribeToRealtimeUseCase: SubscribeToRealtimeUseCase = SubscribeToRealtimeUseCase(chatRepository),
    private val unsubscribeRealtimeUseCase: UnsubscribeRealtimeUseCase = UnsubscribeRealtimeUseCase(chatRepository),
    private val deleteConversationUseCase: DeleteConversationUseCase = DeleteConversationUseCase(chatRepository)
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _phoneContacts = MutableStateFlow<List<com.bharatconnect.app.core.contacts.PhoneContact>>(emptyList())
    val phoneContacts: StateFlow<List<com.bharatconnect.app.core.contacts.PhoneContact>> = _phoneContacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(true)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isLoadingNotifications = MutableStateFlow(false)
    val isLoadingNotifications: StateFlow<Boolean> = _isLoadingNotifications.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.bharatconnect.app.data.remote.dto.NotificationDto>>(emptyList())
    val notifications: StateFlow<List<com.bharatconnect.app.data.remote.dto.NotificationDto>> = _notifications.asStateFlow()

    val totalUnreadCount: StateFlow<Int> = _conversations.map { list ->
        list.sumOf { it.unreadCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var messageObservationJob: Job? = null
    private var realtimeJob: Job? = null
    private var globalRealtimeJob: Job? = null
    private var activeChatPollingJob: Job? = null
    private var conversationSyncJob: Job? = null
    private val chatRepo = chatRepository

    init {
        observeConversations()
        refreshConversations()
        startGlobalRealtimeListener()
        startPeriodicConversationSync()
        fetchNotifications()
    }

    private fun startGlobalRealtimeListener() {
        globalRealtimeJob?.cancel()
        globalRealtimeJob = viewModelScope.launch {
            while (isActive) {
                try {
                    chatRepo.subscribeToGlobalUserMessages { _, _ ->
                        refreshConversations()
                        fetchNotifications()
                    }
                } catch (_: Exception) {
                    delay(2000)
                }
                delay(1000)
            }
        }
    }

    private fun startPeriodicConversationSync() {
        conversationSyncJob?.cancel()
        conversationSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(1500)
                try {
                    fetchConversationsUseCase()
                    fetchNotifications()
                } catch (_: Exception) {}
            }
        }
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _isLoadingNotifications.value = true
            val res = chatRepo.fetchNotifications()
            res.getOrNull()?.let {
                _notifications.value = it
            }
            _isLoadingNotifications.value = false
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            chatRepo.markNotificationsRead()
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            getConversationsUseCase().collect { list ->
                _conversations.value = list
                if (list.isNotEmpty()) {
                    _isLoadingConversations.value = false
                }
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            fetchConversationsUseCase()
            _isLoadingConversations.value = false
        }
    }

    fun selectConversation(conversation: Conversation) {
        var effectiveConv = conversation
        val currentUserId = com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()?.id
        val counterpart = conversation.participantIds.firstOrNull { it.isNotBlank() && it != currentUserId }
        if (!conversation.isGroup && currentUserId != null && counterpart != null) {
            val canonicalId = com.bharatconnect.app.core.encryption.SignalEncryptionManager.getDeterministicConversationId(currentUserId, counterpart)
            if (conversation.id != canonicalId) {
                effectiveConv = conversation.copy(id = canonicalId)
            }
        }

        _selectedConversation.value = effectiveConv
        observeMessages(effectiveConv.id)
        markMessagesAsRead(effectiveConv.id)
        try {
            com.bharatconnect.app.core.notifications.NotificationHelper.clearMessageNotifications(
                com.bharatconnect.app.BharatConnectApp.appContext,
                effectiveConv.id
            )
        } catch (_: Exception) {}
    }

    fun markMessagesAsRead(conversationId: String) {
        viewModelScope.launch {
            chatRepo.markMessagesAsRead(conversationId)
        }
    }

    private fun observeMessages(conversationId: String) {
        messageObservationJob?.cancel()
        messageObservationJob = viewModelScope.launch {
            getMessagesUseCase(conversationId).collect { msgList ->
                _messages.value = msgList
            }
        }

        viewModelScope.launch {
            fetchMessagesUseCase(conversationId)
        }

        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            subscribeToRealtimeUseCase(conversationId)
        }

        // Fast adaptive polling heartbeat while chat screen is actively open
        activeChatPollingJob?.cancel()
        activeChatPollingJob = viewModelScope.launch {
            while (isActive && _selectedConversation.value?.id == conversationId) {
                delay(1000)
                try {
                    fetchMessagesUseCase(conversationId)
                } catch (_: Exception) {}
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        text: String,
        mediaUrl: String? = null,
        mediaType: String? = null
    ) {
        if (text.isBlank() && mediaUrl.isNullOrBlank()) return
        viewModelScope.launch {
            sendMessageUseCase(conversationId, text, mediaUrl, mediaType)
            refreshConversations()
        }
    }

    fun loadDeviceContacts(context: android.content.Context? = null) {
        viewModelScope.launch {
            _isLoadingContacts.value = true
            try {
                val rawContacts = if (context != null) {
                    com.bharatconnect.app.core.contacts.ContactsManager.getDeviceContacts(context)
                } else {
                    emptyList()
                }
                val matchedContacts = com.bharatconnect.app.core.contacts.ContactsManager.matchRegisteredContacts(rawContacts)
                _phoneContacts.value = matchedContacts
            } catch (_: Exception) {
                _phoneContacts.value = emptyList()
            } finally {
                _isLoadingContacts.value = false
            }
        }
    }

    fun startChatWithContact(
        contact: com.bharatconnect.app.core.contacts.PhoneContact,
        chatRepository: ChatRepository = chatRepo,
        onSuccess: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentUserId = com.bharatconnect.app.core.network.SupabaseClient.client.auth.currentUserOrNull()?.id
                ?: com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()?.id
            val currentUserPhone = com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()?.phoneNumber
                ?.let { com.bharatconnect.app.core.contacts.ContactsManager.normalizePhoneNumber(it) }

            // Guard against starting chat with oneself
            if (contact.registeredUserId == currentUserId ||
                (!currentUserPhone.isNullOrBlank() && contact.normalizedPhone == currentUserPhone)) {
                return@launch
            }

            var participantId = contact.registeredUserId
            if (participantId.isNullOrBlank()) {
                // Try looking up in local Room DB by phone number
                val db = com.bharatconnect.app.core.database.DatabaseProvider.getDatabase()
                val norm = com.bharatconnect.app.core.contacts.ContactsManager.normalizePhoneNumber(contact.rawPhone)
                val fullDigits = contact.rawPhone.filter { it.isDigit() }
                val localUser = db.userDao().getAllUsers().firstOrNull { u ->
                    u.id != currentUserId && run {
                        val uNorm = u.phoneNumber?.let { com.bharatconnect.app.core.contacts.ContactsManager.normalizePhoneNumber(it) }
                        val uDigits = u.phoneNumber?.filter { it.isDigit() }
                        uNorm == norm || uDigits == fullDigits || u.phoneNumber == contact.rawPhone
                    }
                }
                if (localUser != null) {
                    participantId = localUser.id
                } else {
                    // Try looking up in Supabase profiles table
                    participantId = try {
                        val profiles = com.bharatconnect.app.core.network.SupabaseClient.client.postgrest["profiles"]
                            .select()
                            .decodeList<com.bharatconnect.app.data.remote.dto.ProfileDto>()
                        val matched = profiles.firstOrNull { p: com.bharatconnect.app.data.remote.dto.ProfileDto ->
                            p.id != currentUserId && run {
                                val pNorm = p.phoneNumber?.let { com.bharatconnect.app.core.contacts.ContactsManager.normalizePhoneNumber(it) }
                                val pDigits = p.phoneNumber?.filter { it.isDigit() }
                                pNorm == norm || pDigits == fullDigits || p.phoneNumber == contact.rawPhone ||
                                (!contact.username.isNullOrBlank() && p.username.equals(contact.username, ignoreCase = true))
                            }
                        }
                        matched?.id
                    } catch (_: Exception) { null }
                }
            }

            if (participantId == currentUserId) {
                return@launch
            }

            val finalParticipantId = participantId ?: "contact_${contact.normalizedPhone}"
            val contactName = contact.name // Authoritative name from user's phonebook
            val result = chatRepository.getOrCreateDirectConversation(finalParticipantId, contactName)
            result.getOrNull()?.let { conv ->
                selectConversation(conv)
                refreshConversations()
                onSuccess(conv)
            }
        }
    }

    /**
     * Directs the user from a notification click straight into the target chat room.
     */
    fun openChatFromNotification(
        senderName: String,
        messageSnippet: String? = null,
        conversationId: String? = null,
        senderId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentUserId = com.bharatconnect.app.core.network.SupabaseClient.client.auth.currentUserOrNull()?.id
                ?: com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()?.id

            // Guard against self-notification loops
            if (senderId != null && senderId == currentUserId) {
                return@launch
            }

            // 1. Try finding conversation in currently loaded list
            val existing = _conversations.value.firstOrNull { conv ->
                (!conversationId.isNullOrBlank() && conv.id == conversationId) ||
                (!senderId.isNullOrBlank() && conv.participantIds.contains(senderId)) ||
                (conv.participantIds.any { it != currentUserId } && conv.title.equals(senderName, ignoreCase = true))
            }

            if (existing != null) {
                selectConversation(existing)
                onSuccess()
                return@launch
            }

            // 2. Try fetching from Room DB directly
            val db = com.bharatconnect.app.core.database.DatabaseProvider.getDatabase()
            val roomConv = if (!conversationId.isNullOrBlank()) {
                db.conversationDao().getConversationById(conversationId)?.toDomain()
            } else {
                db.conversationDao().getAllConversations()
                    .map { it.toDomain() }
                    .firstOrNull { conv ->
                        (!senderId.isNullOrBlank() && conv.participantIds.contains(senderId)) ||
                        (conv.participantIds.any { it != currentUserId } && conv.title.equals(senderName, ignoreCase = true))
                    }
            }

            if (roomConv != null) {
                selectConversation(roomConv)
                onSuccess()
                return@launch
            }

            // 3. Resolve participantId from senderId or local/remote profiles (STRICTLY EXCLUDE currentUserId)
            var resolvedParticipantId = senderId
            if (resolvedParticipantId.isNullOrBlank()) {
                val localUser = db.userDao().getAllUsers().firstOrNull {
                    it.id != currentUserId && (
                        it.fullName.equals(senderName, ignoreCase = true) ||
                        it.username.equals(senderName, ignoreCase = true)
                    )
                }
                resolvedParticipantId = localUser?.id
            }

            if (resolvedParticipantId == currentUserId) {
                return@launch
            }

            val finalParticipantId = resolvedParticipantId ?: "contact_${senderName.trim().replace(" ", "_")}"
            val result = chatRepo.getOrCreateDirectConversation(finalParticipantId, senderName)
            result.getOrNull()?.let { conv ->
                selectConversation(conv)
                refreshConversations()
                onSuccess()
            } ?: run {
                // Fallback: create conversation locally so user is never blocked from chatting
                val myId = currentUserId ?: "me"
                val convId = conversationId ?: com.bharatconnect.app.core.encryption.SignalEncryptionManager.getDeterministicConversationId(myId, finalParticipantId)
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val fallbackConv = Conversation(
                    id = convId,
                    isGroup = false,
                    title = senderName,
                    createdBy = finalParticipantId,
                    lastMessage = messageSnippet,
                    lastMessageTime = timestamp,
                    participantIds = listOf(myId, finalParticipantId)
                )
                db.conversationDao().insertOrUpdateConversation(
                    com.bharatconnect.app.data.local.room.entity.ConversationEntity.fromDomain(fallbackConv)
                )
                selectConversation(fallbackConv)
                refreshConversations()
                onSuccess()
            }
        }
    }

    fun startGroupChat(
        groupId: String,
        title: String,
        onSuccess: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = chatRepo.getOrCreateGroupConversation(groupId, title)
            result.getOrNull()?.let { conv ->
                selectConversation(conv)
                refreshConversations()
                onSuccess(conv)
            }
        }
    }

    fun closeChat() {
        activeChatPollingJob?.cancel()
        messageObservationJob?.cancel()
        realtimeJob?.cancel()
        viewModelScope.launch {
            unsubscribeRealtimeUseCase()
        }
        _selectedConversation.value = null
        _messages.value = emptyList()
    }

    fun deleteConversation(conversationId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (_selectedConversation.value?.id == conversationId) {
                closeChat()
            }
            deleteConversationUseCase(conversationId)
            // Immediately update in-memory list
            _conversations.value = _conversations.value.filter { it.id != conversationId }
            refreshConversations()
            onComplete()
        }
    }
}
