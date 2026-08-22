package com.bharatconnect.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.data.repository.ChatRepositoryImpl
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepositoryImpl()
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    init {
        observeConversations()
        refreshConversations()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.getConversationsFlow().collect { list ->
                if (list.isNotEmpty()) {
                    _conversations.value = list
                } else {
                    // Seed initial sample conversations for offline demo
                    _conversations.value = listOf(
                        Conversation(id = "conv_1", title = "BharatConnect Devs", lastMessage = "Supabase Realtime socket is active! 🚀", lastMessageTime = "Just now", unreadCount = 2),
                        Conversation(id = "conv_2", title = "Aarav Sharma", lastMessage = "Did you check the new Jetpack Compose layout?", lastMessageTime = "10m ago"),
                        Conversation(id = "conv_3", title = "Priya Patel", lastMessage = "Sent a high-res photo via Cloudinary CDN", lastMessageTime = "1h ago"),
                        Conversation(id = "conv_4", title = "Vikram Singh", lastMessage = "Room offline persistence is ready!", lastMessageTime = "Yesterday")
                    )
                }
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            chatRepository.fetchConversations()
        }
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        observeMessages(conversation.id)
    }

    private fun observeMessages(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getMessagesFlow(conversationId).collect { msgList ->
                _messages.value = msgList
            }
        }
        viewModelScope.launch {
            chatRepository.fetchMessages(conversationId)
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(conversationId, text)
        }
    }

    fun closeChat() {
        _selectedConversation.value = null
        _messages.value = emptyList()
    }
}
