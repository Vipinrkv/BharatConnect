package com.bharatconnect.app.data.remote.dto

import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.model.Post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: String,
    val type: String = "direct",
    val title: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("last_message")
    val lastMessage: String? = null,
    @SerialName("last_message_time")
    val lastMessageTime: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(overrideTitle: String? = null, overrideLastMessage: String? = null): Conversation {
        return Conversation(
            id = id,
            isGroup = type != "direct",
            title = overrideTitle ?: title ?: "Conversation",
            createdBy = createdBy,
            lastMessage = overrideLastMessage ?: lastMessage ?: "Tap to open chat",
            lastMessageTime = lastMessageTime ?: createdAt,
            unreadCount = 0
        )
    }
}

@Serializable
data class ConversationMemberDto(
    val id: String? = null,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String,
    val role: String = "member",
    @SerialName("joined_at")
    val joinedAt: String? = null
)

@Serializable
data class NotificationDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val description: String,
    val category: String = "messages",
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("sender_name")
    val senderName: String? = "User",
    val content: String = "",
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val status: String = "sent",
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(overrideSenderName: String? = null): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = overrideSenderName ?: senderName ?: "User",
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
