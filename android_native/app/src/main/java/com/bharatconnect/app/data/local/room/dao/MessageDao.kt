package com.bharatconnect.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE isPendingSync = 1 ORDER BY createdAt ASC")
    suspend fun getPendingSyncMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status, isPendingSync = :isPendingSync WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String, isPendingSync: Boolean)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != 'read'")
    suspend fun markIncomingMessagesRead(conversationId: String, currentUserId: String, status: String = "read")

    @Query("UPDATE messages SET status = 'delivered' WHERE conversationId = :conversationId AND senderId = :currentUserId AND status = 'sent'")
    suspend fun markOutgoingMessagesDelivered(conversationId: String, currentUserId: String)
}
