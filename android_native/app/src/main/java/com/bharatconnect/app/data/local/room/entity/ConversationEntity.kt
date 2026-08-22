package com.bharatconnect.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bharatconnect.app.domain.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val isGroup: Boolean = false,
    val title: String? = null,
    val createdBy: String? = null,
    val lastMessage: String? = null,
    val lastMessageSenderName: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val participantIds: String = "" // Comma-separated user IDs
) {
    fun toDomain(): Conversation {
        return Conversation(
            id = id,
            isGroup = isGroup,
            title = title ?: "Chat",
            createdBy = createdBy,
            lastMessage = lastMessage,
            lastMessageSenderName = lastMessageSenderName,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            participantIds = participantIds.split(",").filter { it.isNotBlank() }
        )
    }

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity {
            return ConversationEntity(
                id = conv.id,
                isGroup = conv.isGroup,
                title = conv.title,
                createdBy = conv.createdBy,
                lastMessage = conv.lastMessage,
                lastMessageSenderName = conv.lastMessageSenderName,
                lastMessageTime = conv.lastMessageTime,
                unreadCount = conv.unreadCount,
                participantIds = conv.participantIds.joinToString(",")
            )
        }
    }
}
