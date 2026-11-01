package com.bharatconnect.app.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.domain.model.state.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesByConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

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

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != 'read'")
    suspend fun getUnreadCount(conversationId: String, currentUserId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE senderId != :currentUserId AND status != 'read'")
    suspend fun getTotalUnreadCount(currentUserId: String): Int

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateReaction(messageId: String, reaction: String?)

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT 50")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun updateStarred(messageId: String, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY createdAt DESC")
    suspend fun getStarredMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaged(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE ephemeralDurationSec > 0 AND (strftime('%s', 'now') - strftime('%s', createdAt)) > ephemeralDurationSec")
    suspend fun purgeExpiredMessages(): Int

    /**
     * Atomically updates a message's status ONLY if the state transition is valid.
     * Prevents invalid regressions (e.g. late delivery ACK downgrading terminal Read).
     * Returns true if transition applied, false if rejected.
     */
    @Transaction
    suspend fun updateMessageStatusValidated(
        messageId: String,
        targetStatus: MessageStatus,
        isPendingSync: Boolean
    ): Boolean {
        val existing = getMessageById(messageId) ?: return false
        val currentStatus = MessageStatus.fromString(existing.status)
        return if (MessageStatus.canTransition(currentStatus, targetStatus)) {
            updateMessageStatus(messageId, targetStatus.rawValue, isPendingSync)
            true
        } else {
            false
        }
    }

    /**
     * Atomically upserts a message without downgrading an existing higher status.
     * If message already exists in local SQLite with higher status (e.g. Read or Delivered),
     * retains the existing higher status.
     */
    @Transaction
    suspend fun upsertMessageSafely(message: MessageEntity) {
        val existing = getMessageById(message.id)
        if (existing == null) {
            insertOrUpdateMessage(message)
        } else {
            val currentStatus = MessageStatus.fromString(existing.status)
            val incomingStatus = MessageStatus.fromString(message.status)
            val resolvedStatus = if (MessageStatus.canTransition(currentStatus, incomingStatus)) {
                incomingStatus.rawValue
            } else {
                existing.status
            }
            val resolvedPendingSync = if (resolvedStatus == "sent" || resolvedStatus == "delivered" || resolvedStatus == "read") {
                false
            } else {
                message.isPendingSync
            }
            insertOrUpdateMessage(message.copy(status = resolvedStatus, isPendingSync = resolvedPendingSync))
        }
    }

    /**
     * Safely inserts or updates a list of messages using state-machine downgrade protection.
     */
    @Transaction
    suspend fun insertMessagesSafely(messages: List<MessageEntity>) {
        for (message in messages) {
            upsertMessageSafely(message)
        }
    }
}
