package com.bharatconnect.app.domain

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import com.bharatconnect.app.domain.usecase.chat.FetchConversationsUseCase
import com.bharatconnect.app.domain.usecase.chat.GetConversationsUseCase
import com.bharatconnect.app.domain.usecase.chat.GetMessagesUseCase
import com.bharatconnect.app.domain.usecase.chat.SendMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FakeChatRepository : ChatRepository {
    private val conversations = mutableListOf(
        Conversation(id = "conv_1", title = "BharatConnect Devs", lastMessage = "Welcome!", unreadCount = 0)
    )
    private val messages = mutableListOf<Message>()

    override fun getConversationsFlow(): Flow<List<Conversation>> = flowOf(conversations)

    override fun getMessagesFlow(conversationId: String): Flow<List<Message>> =
        flowOf(messages.filter { it.conversationId == conversationId })

    override suspend fun fetchConversations(): Result<List<Conversation>> = Result.success(conversations)

    override suspend fun fetchMessages(conversationId: String): Result<List<Message>> =
        Result.success(messages.filter { it.conversationId == conversationId })

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        mediaUrl: String?,
        mediaType: String?
    ): Result<Message> {
        val msg = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "sender_1",
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            status = "sent",
            createdAt = "2026-08-22 22:30:00",
            isPendingSync = false
        )
        messages.add(msg)
        return Result.success(msg)
    }

    override suspend fun retryPendingMessages(): Result<Int> = Result.success(0)

    override suspend fun subscribeToRealtime(conversationId: String) {}

    override suspend fun unsubscribeRealtime() {}
}

class ChatUseCasesTest {

    private val fakeChatRepository = FakeChatRepository()
    private val getConversationsUseCase = GetConversationsUseCase(fakeChatRepository)
    private val getMessagesUseCase = GetMessagesUseCase(fakeChatRepository)
    private val sendMessageUseCase = SendMessageUseCase(fakeChatRepository)
    private val fetchConversationsUseCase = FetchConversationsUseCase(fakeChatRepository)

    @Test
    fun `getConversationsUseCase returns active conversations`() = runBlocking {
        val convList = getConversationsUseCase().first()
        assertEquals(1, convList.size)
        assertEquals("BharatConnect Devs", convList[0].title)
    }

    @Test
    fun `sendMessageUseCase inserts message and returns success`() = runBlocking {
        val sendResult = sendMessageUseCase("conv_1", "Testing secure chat message")
        assertTrue(sendResult.isSuccess)
        assertEquals("Testing secure chat message", sendResult.getOrNull()?.content)
        assertEquals("conv_1", sendResult.getOrNull()?.conversationId)

        val messages = getMessagesUseCase("conv_1").first()
        assertEquals(1, messages.size)
        assertEquals("Testing secure chat message", messages[0].content)
    }
}
