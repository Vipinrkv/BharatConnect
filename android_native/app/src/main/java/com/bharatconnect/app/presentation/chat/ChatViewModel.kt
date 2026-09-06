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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var messageObservationJob: Job? = null
    private var realtimeJob: Job? = null
    private var globalRealtimeJob: Job? = null
    private val chatRepo = chatRepository

    init {
        observeConversations()
        refreshConversations()
        startGlobalRealtimeListener()
        fetchNotifications()
    }

    private fun startGlobalRealtimeListener() {
        globalRealtimeJob?.cancel()
        globalRealtimeJob = viewModelScope.launch {
            chatRepo.subscribeToGlobalUserMessages { _, _ ->
                refreshConversations()
                fetchNotifications()
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
        _selectedConversation.value = conversation
        observeMessages(conversation.id)
        markMessagesAsRead(conversation.id)
        try {
            com.bharatconnect.app.core.notifications.NotificationHelper.clearMessageNotifications(
                com.bharatconnect.app.BharatConnectApp.appContext,
                conversation.id
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
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            sendMessageUseCase(conversationId, text)
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
            val participantId = contact.registeredUserId ?: "contact_${contact.normalizedPhone}"
            val contactName = contact.name // Authoritative name from user's phonebook
            val result = chatRepository.getOrCreateDirectConversation(participantId, contactName)
            result.getOrNull()?.let { conv ->
                selectConversation(conv)
                refreshConversations()
                onSuccess(conv)
            }
        }
    }

    fun closeChat() {
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
