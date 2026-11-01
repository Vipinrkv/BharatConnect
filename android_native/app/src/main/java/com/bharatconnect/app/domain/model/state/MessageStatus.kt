package com.bharatconnect.app.domain.model.state

/**
 * =========================================================================================
 * BHARATCONNECT FORMAL MESSAGE STATE MACHINE
 * =========================================================================================
 *
 * Models strict, irreversible progression of an outbound/inbound chat message.
 * Prevents invalid out-of-order transitions (e.g. late network delivery ACK cannot downgrade READ -> DELIVERED).
 *
 * State Progression:
 * 1. Draft      -> Unsent text in UI compose box
 * 2. Pending    -> Saved locally in Room SQLite, queued for network dispatch (isPendingSync = 1)
 * 3. Sending    -> Network connection established, request actively in flight over HTTPS/WSS
 * 4. Sent       -> Persisted in Supabase PostgreSQL (Single grey tick "✓")
 * 5. Delivered  -> Recipient client received message & wrote to local SQLite (Double grey tick "✓✓")
 * 6. Read       -> Recipient actively opened conversation screen (Double sky-blue tick "✓✓")
 * 7. Failed     -> Permanent or transient failure (e.g. network dropped or rate limit)
 * 8. Retrying   -> WorkManager / Auto-retry engine re-dispatching from queue
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
                else -> Sent // Default safe fallback
            }
        }

        /**
         * Determines if an incoming state transition is valid.
         * Enforces unidirectional forward progression:
         * Cannot transition from Read -> Sent or Read -> Delivered.
         * Failed can transition to Retrying or Pending.
         */
        fun canTransition(current: MessageStatus, next: MessageStatus): Boolean {
            if (current == next) return true // Idempotent no-op
            if (current == Read) return false // Terminal read state cannot be downgraded
            if (next == Failed) return true // Any non-read state can fail
            if (current == Failed && (next == Retrying || next == Pending || next == Sending)) return true
            return next.level > current.level
        }
    }
}
