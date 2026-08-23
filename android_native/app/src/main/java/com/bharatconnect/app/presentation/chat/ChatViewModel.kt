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
    private val unsubscribeRealtimeUseCase: UnsubscribeRealtimeUseCase = UnsubscribeRealtimeUseCase(chatRepository)
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

    private var messageObservationJob: Job? = null
    private var realtimeJob: Job? = null

    init {
        observeConversations()
        refreshConversations()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            getConversationsUseCase().collect { list ->
                _conversations.value = list
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            fetchConversationsUseCase()
        }
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        observeMessages(conversation.id)
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
        }
    }

    fun loadDeviceContacts(context: android.content.Context) {
        viewModelScope.launch {
            _isLoadingContacts.value = true
            try {
                val rawContacts = com.bharatconnect.app.core.contacts.ContactsManager.getDeviceContacts(context)
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
        chatRepository: ChatRepository = ChatRepositoryImpl(),
        onSuccess: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            val participantId = contact.registeredUserId ?: "contact_${contact.normalizedPhone}"
            val contactName = contact.name // Authoritative name from user's phonebook
            val result = chatRepository.getOrCreateDirectConversation(participantId, contactName)
            result.getOrNull()?.let { conv ->
                selectConversation(conv)
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
}
