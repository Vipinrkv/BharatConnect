package com.bharatconnect.app.domain

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.repository.ChatRepository
import com.bharatconnect.app.domain.usecase.chat.*
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

    override suspend fun getOrCreateDirectConversation(participantId: String, title: String): Result<Conversation> {
        val existing = conversations.find { it.id == "direct_$participantId" }
        if (existing != null) return Result.success(existing)
        val newConv = Conversation(
            id = "direct_$participantId",
            title = title,
            lastMessage = "Start chat with $title",
            unreadCount = 0
        )
        conversations.add(newConv)
        return Result.success(newConv)
    }

    override suspend fun subscribeToRealtime(conversationId: String) {}

    override suspend fun unsubscribeRealtime() {}

    override suspend fun subscribeToGlobalUserMessages(onNewMessage: ((Message, String) -> Unit)?) {}

    override suspend fun fetchNotifications(): Result<List<com.bharatconnect.app.data.remote.dto.NotificationDto>> = Result.success(emptyList())

    override suspend fun markNotificationsRead(): Result<Unit> = Result.success(Unit)
}

class ChatUseCasesTest {

    private val fakeChatRepository = FakeChatRepository()
    private val getConversationsUseCase = GetConversationsUseCase(fakeChatRepository)
    private val getMessagesUseCase = GetMessagesUseCase(fakeChatRepository)
    private val sendMessageUseCase = SendMessageUseCase(fakeChatRepository)
    private val fetchConversationsUseCase = FetchConversationsUseCase(fakeChatRepository)
    private val fetchMessagesUseCase = FetchMessagesUseCase(fakeChatRepository)

    @Test
    fun testGetConversationsFlow_returnsInitialList() = runBlocking {
        val conversations = getConversationsUseCase().first()
        assertEquals(1, conversations.size)
        assertEquals("BharatConnect Devs", conversations[0].title)
    }

    @Test
    fun testSendMessage_appendsToMessagesFlow() = runBlocking {
        val sendResult = sendMessageUseCase("conv_1", "Namaste Bharat!")
        assertTrue(sendResult.isSuccess)

        val messages = getMessagesUseCase("conv_1").first()
        assertEquals(1, messages.size)
        assertEquals("Namaste Bharat!", messages[0].content)
    }

    @Test
    fun testDirectContactConversation_usesPhonebookName() = runBlocking {
        val result = fakeChatRepository.getOrCreateDirectConversation("contact_9876543210", "Amit Patel (Phonebook)")
        assertTrue(result.isSuccess)
        val conv = result.getOrNull()
        assertEquals("Amit Patel (Phonebook)", conv?.title)
    }

    @Test
    fun testFetchConversationsAndMessages_succeed() = runBlocking {
        val convResult = fetchConversationsUseCase()
        assertTrue(convResult.isSuccess)

        val msgResult = fetchMessagesUseCase("conv_1")
        assertTrue(msgResult.isSuccess)
    }
}
