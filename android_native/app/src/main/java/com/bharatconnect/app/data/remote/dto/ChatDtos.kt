package com.bharatconnect.app.data.remote.dto

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.model.Post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: String,
    @SerialName("is_group")
    val isGroup: Boolean = false,
    val title: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): Conversation {
        return Conversation(
            id = id,
            isGroup = isGroup,
            title = title ?: "Conversation",
            createdBy = createdBy,
            lastMessage = "Tap to open chat",
            lastMessageTime = createdAt,
            unreadCount = 0
        )
    }
}

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val status: String = "sent",
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(senderName: String? = null): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            status = status,
            createdAt = createdAt ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            isPendingSync = false
        )
    }
}

@Serializable
data class PostDto(
    val id: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("author_name")
    val authorName: String? = null,
    val content: String,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorId = authorId,
            authorName = authorName ?: "BharatConnect Member",
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            likesCount = likesCount,
            commentsCount = commentsCount,
            isLikedByMe = false,
            createdAt = createdAt ?: "Just now"
        )
    }
}
