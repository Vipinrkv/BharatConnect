package com.bharatconnect.app.domain.usecase.chat

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetConversationsUseCase(private val chatRepository: ChatRepository) {
    operator fun invoke(): Flow<List<Conversation>> {
        return chatRepository.getConversationsFlow()
    }
}

class GetMessagesUseCase(private val chatRepository: ChatRepository) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return chatRepository.getMessagesFlow(conversationId)
    }
}

class SendMessageUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        mediaUrl: String? = null,
        mediaType: String? = null
    ): Result<Message> {
        return chatRepository.sendMessage(conversationId, content, mediaUrl, mediaType)
    }
}

class FetchConversationsUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(): Result<List<Conversation>> {
        return chatRepository.fetchConversations()
    }
}

class FetchMessagesUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(conversationId: String): Result<List<Message>> {
        return chatRepository.fetchMessages(conversationId)
    }
}

class SubscribeToRealtimeUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(conversationId: String) {
        chatRepository.subscribeToRealtime(conversationId)
    }
}

class UnsubscribeRealtimeUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke() {
        chatRepository.unsubscribeRealtime()
    }
}

class DeleteConversationUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return chatRepository.deleteConversation(conversationId)
    }
}
