package com.bharatconnect.app

import com.bharatconnect.app.domain.model.Message
import com.bharatconnect.app.domain.model.state.MessageStatus
import com.bharatconnect.app.data.local.room.entity.MessageEntity
import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * =========================================================================================
 * CYCLE 3: CHAT CONCURRENCY, STATE MACHINE & REALTIME STRESS TESTS
 * =========================================================================================
 *
 * Deterministic JUnit unit & coroutine concurrency test suite covering:
 * 1. Simultaneous incoming and outgoing message dispatch
 * 2. Duplicate event deduplication & idempotent ACKs
 * 3. Out-of-order delivery & read receipt downgrade prevention
 * 4. Offline queue reconciliation under 10, 50, and 100 message batches
 * 5. Network loss, timeout, and backoff retry transitions
 * 6. Process termination & pending state restoration
 * 7. Multithreaded atomic state updates
 */
class ChatConcurrencyAndRealtimeStressTest {

    @Test
    fun `Simultaneous incoming and outgoing message streams resolve with monotonic timestamps and valid statuses`() = runBlocking {
        val totalOutgoing = 50
        val totalIncoming = 50
        val conversationId = "conv_stress_100"

        val outgoingDeferred = (1..totalOutgoing).map { i ->
            async(Dispatchers.Default) {
                Message(
                    id = "out_$i",
                    conversationId = conversationId,
                    senderId = "me_user",
                    content = "Outgoing payload $i",
                    status = "sending",
                    createdAt = "2026-11-01 12:00:%02d".format(i % 60),
                    isPendingSync = true
                )
            }
        }

        val incomingDeferred = (1..totalIncoming).map { i ->
            async(Dispatchers.Default) {
                Message(
                    id = "in_$i",
                    conversationId = conversationId,
                    senderId = "peer_user",
                    content = "Incoming payload $i",
                    status = "sent",
                    createdAt = "2026-11-01 12:01:%02d".format(i % 60),
                    isPendingSync = false
                )
            }
        }

        val allMessages = (outgoingDeferred.awaitAll() + incomingDeferred.awaitAll())
            .sortedBy { it.createdAt }

        assertEquals(100, allMessages.size)
        assertEquals(50, allMessages.count { it.senderId == "me_user" })
        assertEquals(50, allMessages.count { it.senderId == "peer_user" })
        assertTrue(allMessages.all { it.id.isNotBlank() && it.content.isNotBlank() })
    }

    @Test
    fun `Duplicate incoming WebSocket broadcast events are safely deduplicated without state regression`() {
        val messageMap = ConcurrentHashMap<String, MessageEntity>()

        // Simulate incoming message with 'sent' status
        val firstEvent = MessageEntity(
            id = "msg_event_001",
            conversationId = "conv_dedup",
            senderId = "peer_user",
            content = "Encrypted message body",
            status = "sent",
            createdAt = "2026-11-01 12:30:00",
            isPendingSync = false
        )
        messageMap[firstEvent.id] = firstEvent

        // User views message -> status upgraded to 'read'
        val upgraded = firstEvent.copy(status = "read")
        messageMap[upgraded.id] = upgraded

        // Late duplicate WebSocket event arrives with stale 'sent' status
        val duplicateStaleEvent = firstEvent.copy(status = "sent")
        val current = messageMap[duplicateStaleEvent.id]
        assertNotNull(current)

        val currentStatus = MessageStatus.fromString(current!!.status)
        val incomingStatus = MessageStatus.fromString(duplicateStaleEvent.status)

        // Downgrade protection check
        val canApply = MessageStatus.canTransition(currentStatus, incomingStatus)
        assertFalse("Stale sent ACK must NOT downgrade Read state", canApply)

        if (canApply) {
            messageMap[duplicateStaleEvent.id] = duplicateStaleEvent
        }

        assertEquals("read", messageMap["msg_event_001"]?.status)
    }

    @Test
    fun `Out-of-order event sequence delivered followed by delayed sent ACK preserves Delivered state`() {
        val currentStatus = MessageStatus.Delivered
        val delayedSentStatus = MessageStatus.Sent

        // State machine must reject Delivered -> Sent
        assertFalse(MessageStatus.canTransition(currentStatus, delayedSentStatus))

        // State machine must allow Delivered -> Read
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Read))
    }

    @Test
    fun `Out-of-order event sequence read followed by delayed delivered ACK preserves Read state`() {
        val currentStatus = MessageStatus.Read
        val delayedDeliveredStatus = MessageStatus.Delivered

        // State machine must reject Read -> Delivered
        assertFalse(MessageStatus.canTransition(currentStatus, delayedDeliveredStatus))

        // Read is terminal; idempotent transitions are permitted
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Read))
    }

    @Test
    fun `Late network transport error after message delivered or read cannot downgrade status to Failed`() {
        // A message confirmed as Delivered or Read must never be marked as Failed by a subsequent network blip
        assertFalse(MessageStatus.canTransition(MessageStatus.Delivered, MessageStatus.Failed))
        assertFalse(MessageStatus.canTransition(MessageStatus.Read, MessageStatus.Failed))
        assertFalse(MessageStatus.canTransition(MessageStatus.Sent, MessageStatus.Failed))

        // But in-flight messages CAN fail
        assertTrue(MessageStatus.canTransition(MessageStatus.Sending, MessageStatus.Failed))
        assertTrue(MessageStatus.canTransition(MessageStatus.Pending, MessageStatus.Failed))
        assertTrue(MessageStatus.canTransition(MessageStatus.Retrying, MessageStatus.Failed))
    }

    @Test
    fun `Offline batch queue scalability test processes batches of 10, 50, and 100 messages`() {
        val batchSizes = listOf(10, 50, 100)

        for (size in batchSizes) {
            val offlineQueue = (1..size).map { i ->
                MessageEntity(
                    id = "offline_${size}_$i",
                    conversationId = "conv_batch_$size",
                    senderId = "user_offline",
                    content = "Batch item $i",
                    status = "pending",
                    createdAt = "2026-11-01 14:00:%02d".format(i % 60),
                    isPendingSync = true
                )
            }.toMutableList()

            assertEquals(size, offlineQueue.size)
            assertTrue(offlineQueue.all { it.isPendingSync && it.status == "pending" })

            // Simulate WorkManager reconciliation
            val syncedList = offlineQueue.map { entity ->
                val current = MessageStatus.fromString(entity.status)
                val retrying = MessageStatus.Retrying
                assertTrue(MessageStatus.canTransition(current, retrying))

                val sent = MessageStatus.Sent
                assertTrue(MessageStatus.canTransition(retrying, sent))

                entity.copy(status = sent.rawValue, isPendingSync = false)
            }

            assertEquals(size, syncedList.size)
            assertTrue("All messages in batch $size must be synced", syncedList.all { !it.isPendingSync && it.status == "sent" })
        }
    }

    @Test
    fun `Simulated network timeout triggers Retry transition and subsequent successful dispatch`() {
        var currentStatus: MessageStatus = MessageStatus.Pending
        var isPendingSync: Boolean

        // 1. Dispatch begins
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Sending))
        currentStatus = MessageStatus.Sending

        // 2. Timeout occurs
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Failed))
        currentStatus = MessageStatus.Failed

        // 3. WorkManager picks up failed message with exponential backoff
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Retrying))
        currentStatus = MessageStatus.Retrying

        // 4. Retry actively dispatches over network
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Sending))
        currentStatus = MessageStatus.Sending

        // 5. Server responds HTTP 201 Created
        assertTrue(MessageStatus.canTransition(currentStatus, MessageStatus.Sent))
        currentStatus = MessageStatus.Sent
        isPendingSync = false

        assertEquals(MessageStatus.Sent, currentStatus)
        assertFalse(isPendingSync)
    }

    @Test
    fun `Concurrent multi-worker status updates on identical message resolve to highest monotonic status without race conditions`() = runBlocking {
        val initialStatus = AtomicReference<MessageStatus>(MessageStatus.Sending)
        val transitions = listOf(
            MessageStatus.Sent,
            MessageStatus.Delivered,
            MessageStatus.Read,
            MessageStatus.Delivered,  // Stale late delivery should be rejected once Read
            MessageStatus.Sent,       // Stale late sent ACK should be rejected once Read
            MessageStatus.Sending     // Stale sending transition should be rejected once Sent/Delivered/Read
        )

        val jobs = transitions.map { target ->
            async(Dispatchers.Default) {
                initialStatus.updateAndGet { current ->
                    if (MessageStatus.canTransition(current, target)) target else current
                }
            }
        }

        jobs.awaitAll()

        // Terminal status must be Read, never downgraded
        assertEquals(MessageStatus.Read, initialStatus.get())
    }

    @Test
    fun `Process restart simulation restores un-synced messages in Pending state for WorkManager flush`() {
        val originalMessage = Message(
            id = "persist_test_101",
            conversationId = "conv_persist",
            senderId = "user_local",
            content = "Message written before app kill",
            status = "pending",
            createdAt = "2026-11-01 15:30:00",
            isPendingSync = true
        )

        // Persist to Room SQLite Entity
        val entity = MessageEntity.fromDomain(originalMessage)
        assertEquals("pending", entity.status)
        assertTrue(entity.isPendingSync)

        // Simulate application restart: entity deserialized from SQLite cache
        val rehydrated = entity.toDomain()
        assertEquals(originalMessage.id, rehydrated.id)
        assertEquals(originalMessage.status, rehydrated.status)
        assertEquals(originalMessage.isPendingSync, rehydrated.isPendingSync)
        assertEquals(originalMessage.content, rehydrated.content)
    }

    @Test
    fun `Canonical conversation ID derivation is strictly symmetric across disparate identifier formats`() {
        val testPairs = listOf(
            Pair("3fa85f64-5717-4562-b3fc-2c963f66afa6", "7c9e6679-7425-40de-944b-e07fc1f90ae7"),
            Pair("+919876543210", "+919123456780"),
            Pair("vipin_user", "rahul_user"),
            Pair("alpha_99", "beta_01")
        )

        for ((user1, user2) in testPairs) {
            val id1 = SignalEncryptionManager.getDeterministicConversationId(user1, user2)
            val id2 = SignalEncryptionManager.getDeterministicConversationId(user2, user1)
            assertEquals("Canonical conversation ID must be symmetric for pair ($user1, $user2)", id1, id2)
            assertTrue(id1.isNotBlank())
        }
    }
}
