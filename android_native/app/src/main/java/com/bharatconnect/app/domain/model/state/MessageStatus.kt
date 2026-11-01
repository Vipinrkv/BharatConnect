package com.bharatconnect.app.domain.model.state

/**
 * =========================================================================================
 * BHARATCONNECT FORMAL MESSAGE STATE MACHINE
 * =========================================================================================
 *
 * Models strict, monotonic progression of chat message delivery.
 * Prevents invalid out-of-order regressions (e.g. late network delivery ACK cannot downgrade READ -> DELIVERED).
 *
 * State Progression Hierarchy:
 * 1. Draft      (Level 0)  -> Unsent text in UI compose box
 * 2. Pending    (Level 1)  -> Saved locally in Room SQLite, queued for sync (isPendingSync = 1)
 * 3. Sending    (Level 2)  -> Request actively in flight over network socket/HTTP
 * 4. Sent       (Level 3)  -> Persisted in Supabase backend (Single grey tick "✓")
 * 5. Delivered  (Level 4)  -> Recipient client received message & wrote to local DB (Double grey tick "✓✓")
 * 6. Read       (Level 5)  -> Recipient viewed conversation (Double sky-blue tick "✓✓")
 * 7. Failed     (Level -1) -> Network, timeout, or crypto failure occurred; retry enabled
 * 8. Retrying   (Level 2)  -> WorkManager or manual retry actively re-attempting transmission
 */
sealed class MessageStatus(val rawValue: String, val level: Int) {
    object Draft : MessageStatus("draft", 0)
    object Pending : MessageStatus("pending", 1)
    object Sending : MessageStatus("sending", 2)
    object Sent : MessageStatus("sent", 3)
    object Delivered : MessageStatus("delivered", 4)
    object Read : MessageStatus("read", 5)
    object Failed : MessageStatus("failed", -1)
    object Retrying : MessageStatus("retrying", 2)

    val isTerminal: Boolean get() = this is Read
    val isSynced: Boolean get() = this is Sent || this is Delivered || this is Read
    val isOutboundPending: Boolean get() = this is Draft || this is Pending || this is Sending || this is Retrying || this is Failed

    companion object {
        fun fromString(value: String?): MessageStatus {
            return when (value?.lowercase()?.trim()) {
                "draft" -> Draft
                "pending" -> Pending
                "sending" -> Sending
                "sent" -> Sent
                "delivered" -> Delivered
                "read" -> Read
                "failed" -> Failed
                "retrying" -> Retrying
                else -> Sent // Safe default fallback for legacy/unrecognized strings
            }
        }

        /**
         * Validates whether a state transition from [current] to [next] is permitted.
         * Enforces strict monotonic forward progression and prevents invalid regressions.
         *
         * Transition Invariants:
         * 1. Idempotent: current == next is always permitted (no-op).
         * 2. Read (Level 5): Terminal state. Cannot be downgraded to any other state.
         * 3. Delivered (Level 4): Can only transition forward to Read. Cannot downgrade to Sent or fail.
         * 4. Sent (Level 3): Can only transition forward to Delivered or Read. Cannot downgrade to Sending or fail.
         * 5. Sending (Level 2): Can transition forward to Sent, Delivered, Read, or to Failed on error.
         * 6. Retrying (Level 2): Can transition to Sending, Sent, Delivered, Read, or back to Failed.
         * 7. Pending (Level 1): Can transition to Sending, Sent, or Failed.
         * 8. Failed (Level -1): Can transition to Retrying, Pending, or Sending when retried.
         * 9. Draft (Level 0): Can transition to Pending or Sending when queued.
         */
        fun canTransition(current: MessageStatus, next: MessageStatus): Boolean {
            if (current == next) return true // Idempotent no-op

            return when (current) {
                is Read -> false // Terminal state: once read, never downgraded by late network ACKs
                is Delivered -> next is Read
                is Sent -> next is Delivered || next is Read
                is Sending -> next is Sent || next is Delivered || next is Read || next is Failed
                is Retrying -> next is Sending || next is Sent || next is Delivered || next is Read || next is Failed
                is Pending -> next is Sending || next is Sent || next is Delivered || next is Read || next is Failed || next is Retrying
                is Failed -> next is Retrying || next is Pending || next is Sending
                is Draft -> next is Pending || next is Sending || next is Failed
            }
        }
    }
}
