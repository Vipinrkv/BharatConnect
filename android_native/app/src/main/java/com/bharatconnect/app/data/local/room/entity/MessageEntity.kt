package com.bharatconnect.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bharatconnect.app.domain.model.Message

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"]), Index(value = ["createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val status: String = "sent", // sending, sent, delivered, read, failed
    val createdAt: String,
    val isPendingSync: Boolean = false
) {
    fun toDomain(): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            status = status,
            createdAt = createdAt,
            isPendingSync = isPendingSync
        )
    }

    companion object {
        fun fromDomain(msg: Message): MessageEntity {
            return MessageEntity(
                id = msg.id,
                conversationId = msg.conversationId,
                senderId = msg.senderId,
                senderName = msg.senderName,
                content = msg.content,
                mediaUrl = msg.mediaUrl,
                mediaType = msg.mediaType,
                status = msg.status,
                createdAt = msg.createdAt,
                isPendingSync = msg.isPendingSync
            )
        }
    }
}
