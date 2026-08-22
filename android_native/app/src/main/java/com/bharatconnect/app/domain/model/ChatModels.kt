package com.bharatconnect.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    val isGroup: Boolean = false,
    val title: String = "Chat",
    val createdBy: String? = null,
    val lastMessage: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList()
)

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val status: String = "sent", // sending, sent, delivered, read, failed
    val createdAt: String,
    val isPendingSync: Boolean = false
)

@Serializable
data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: String
)
