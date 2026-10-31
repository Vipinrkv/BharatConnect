package com.bharatconnect.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bharatconnect.app.domain.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val isGroup: Boolean = false,
    val title: String? = null,
    val avatarUrl: String? = null,
    val createdBy: String? = null,
    val lastMessage: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val participantIds: String = "", // Comma-separated user IDs
    val isPinned: Boolean = false,
    val adminIds: String = "", // Comma-separated admin IDs
    val groupDescription: String? = null
) {
    fun toDomain(): Conversation {
        return Conversation(
            id = id,
            isGroup = isGroup,
            title = title ?: "Chat",
            avatarUrl = avatarUrl,
            createdBy = createdBy,
            lastMessage = lastMessage,
            lastMessageSenderName = lastMessageSenderName,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            participantIds = participantIds.split(",").filter { it.isNotBlank() },
            isPinned = isPinned,
            adminIds = adminIds.split(",").filter { it.isNotBlank() },
            groupDescription = groupDescription
        )
    }

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity {
            return ConversationEntity(
                id = conv.id,
                isGroup = conv.isGroup,
                title = conv.title,
                avatarUrl = conv.avatarUrl,
                createdBy = conv.createdBy,
                lastMessage = conv.lastMessage,
                lastMessageSenderName = conv.lastMessageSenderName,
                lastMessageTime = conv.lastMessageTime,
                unreadCount = conv.unreadCount,
                participantIds = conv.participantIds.joinToString(","),
                isPinned = conv.isPinned,
                adminIds = conv.adminIds.joinToString(","),
                groupDescription = conv.groupDescription
            )
        }
    }
}
