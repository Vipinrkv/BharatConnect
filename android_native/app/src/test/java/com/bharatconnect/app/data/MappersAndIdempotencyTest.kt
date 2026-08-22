package com.bharatconnect.app.data

import com.bharatconnect.app.data.local.room.entity.ConversationEntity
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.data.local.room.entity.PostEntity
import com.bharatconnect.app.data.local.room.entity.UserEntity
import com.bharatconnect.app.data.remote.dto.ConversationDto
import com.bharatconnect.app.data.remote.dto.MessageDto
import com.bharatconnect.app.data.remote.dto.PostDto
import com.bharatconnect.app.data.remote.dto.ProfileDto
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MappersAndIdempotencyTest {

    @Test
    fun `MessageDto retains client assigned UUID for idempotency`() {
        val clientMessageId = UUID.randomUUID().toString()
        val dto = MessageDto(
            id = clientMessageId,
            conversationId = "conv_100",
            senderId = "user_200",
            content = "Idempotent message test",
            status = "sent",
            createdAt = "2026-08-22 22:30:00"
        )
        val domain = dto.toDomain("Sender Name")

        assertEquals(clientMessageId, domain.id)
        assertEquals("conv_100", domain.conversationId)
        assertEquals("user_200", domain.senderId)
        assertEquals("Sender Name", domain.senderName)
        assertFalse(domain.isPendingSync)
    }

    @Test
    fun `MessageEntity bidirectional mapping preserves all fields and sync state`() {
        val message = Message(
            id = "msg_12345",
            conversationId = "conv_abc",
            senderId = "sender_xyz",
            senderName = "Vikram",
            content = "Testing offline-first room entity",
            mediaUrl = "https://res.cloudinary.com/test/image.jpg",
            mediaType = "image/jpeg",
            status = "sending",
            createdAt = "2026-08-22 22:35:00",
            isPendingSync = true
        )

        val entity = MessageEntity.fromDomain(message)
        assertEquals("msg_12345", entity.id)
        assertTrue(entity.isPendingSync)

        val reconstructed = entity.toDomain()
        assertEquals(message, reconstructed)
    }

    @Test
    fun `PostDto and PostEntity bidirectional mapping maintains counts and media`() {
        val postId = UUID.randomUUID().toString()
        val post = Post(
            id = postId,
            authorId = "author_1",
            authorName = "Aarav Sharma",
            content = "Great post about Kotlin!",
            likesCount = 42,
            commentsCount = 10,
            isLikedByMe = true,
            createdAt = "2026-08-22 20:00:00"
        )

        val entity = PostEntity.fromDomain(post)
        assertEquals(postId, entity.id)
        assertEquals(42, entity.likesCount)
        assertTrue(entity.isLikedByMe)

        val reconstructed = entity.toDomain()
        assertEquals(post, reconstructed)
    }

    @Test
    fun `ProfileDto and UserEntity mapping correctly handles defaults`() {
        val profileDto = ProfileDto(
            id = "user_abc",
            username = "bharat_user",
            fullName = "Bharat Member",
            avatarUrl = "https://avatar.url/1.png"
        )

        val domain = profileDto.toDomain("bharat@domain.in")
        assertEquals("user_abc", domain.id)
        assertEquals("bharat_user", domain.username)
        assertEquals("bharat@domain.in", domain.email)

        val entity = UserEntity.fromDomain(domain)
        assertEquals("user_abc", entity.id)
        assertEquals("Bharat Member", entity.fullName)
    }
}
