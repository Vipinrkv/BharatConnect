# BharatConnect Message Status State Machine Specification
Document Version: 1.2.0
Cycle: Cycle 3 Integration & Stress Testing
Status: Fully Integrated into Room SQLite & Supabase Sync Pipeline

## 1. Executive Summary
In Cycle 3, the `MessageStatus` state machine transitioned from a domain abstraction into the authoritative, enforced execution path governing all local SQLite persistence, background sync retries, and incoming Realtime WebSocket broadcasts.

Every message status modification in BharatConnect now passes through transactional validation guards:
1. Direct uncontrolled SQL string updates are eliminated.
2. Room DAO level transactional guards (`updateMessageStatusValidated` and `upsertMessageSafely`) prevent race conditions and regressions.
3. Out-of-order delivery receipts and duplicate server ACKs cannot downgrade higher-order states (`Read` and `Delivered`).

---

## 2. State Definitions & Hierarchy
Every status is modeled as a strongly typed Kotlin sealed class with an assigned ordering rank:

| State | Level / Rank | Persistence Scope | Description |
|---|---|---|---|
| `Draft` | 0 | Local (UI/Cache) | Message composed in input field but not yet queued for transmission. |
| `Pending` | 1 | Room SQLite (`isPendingSync = 1`) | Message enqueued locally while offline or awaiting network dispatcher. |
| `Sending` | 2 | Memory / In-Flight | Dispatcher is actively transmitting payload over HTTP/WebSocket. |
| `Sent` | 3 | Remote Supabase | Payload committed to backend database; single checkmark displayed. |
| `Delivered` | 4 | Remote Supabase + WebSocket | Recipient device acknowledged reception; double checkmark displayed. |
| `Read` | 5 | Remote Supabase + WebSocket | Recipient opened conversation and marked viewed; double blue checkmark displayed. |
| `Failed` | -1 | Room SQLite | Network error, timeout, or crypto failure occurred; retry action enabled. |
| `Retrying` | 2 | WorkManager Queue | Background backoff retry actively re-attempting transmission. |

---

## 3. Hardened Transition Matrix

| Current State | Target State | Permitted? | Condition / Trigger |
|---|---|---|---|
| `Draft` | `Pending` | YES | User taps Send button while offline. |
| `Draft` | `Sending` | YES | User taps Send button while network is active. |
| `Pending` | `Sending` | YES | Network monitor detects connectivity; sync dispatcher awakens. |
| `Pending` | `Retrying` | YES | WorkManager offline retry sweeps pending queue. |
| `Pending` | `Failed` | YES | Immediate socket failure or offline storage limit reached. |
| `Sending` | `Sent` | YES | Supabase PostgREST returns 201 Created HTTP confirmation. |
| `Sending` | `Failed` | YES | Network connection drops, timeout (>15s), or server 5xx error. |
| `Sending` | `Delivered` | YES | Immediate delivery ACK bundled with server response. |
| `Sending` | `Read` | YES | Immediate read ACK (e.g. active bot or live counterpart). |
| `Sent` | `Delivered` | YES | Supabase Realtime broadcast receipt received from recipient client. |
| `Sent` | `Read` | YES | Direct jump if recipient reads immediately without intermediate ACK. |
| `Delivered` | `Read` | YES | Supabase Realtime broadcast receipt received indicating user viewed message. |
| `Failed` | `Retrying` | YES | WorkManager auto-retry or user taps retry button. |
| `Failed` | `Sending` | YES | User manually forces immediate re-dispatch. |
| `Retrying` | `Sending` | YES | Retry worker starts active HTTP dispatch. |
| `Retrying` | `Sent` | YES | Retry HTTP request succeeds. |
| `Retrying` | `Failed` | YES | Retry exhausted or network immediately unavailable. |
| `Read` | `Delivered` | **NO (BLOCKED)** | Out-of-order delayed delivery receipt arrives after read receipt. |
| `Read` | `Sent` | **NO (BLOCKED)** | Delayed server broadcast arrives after recipient has already read. |
| `Delivered` | `Sent` | **NO (BLOCKED)** | Duplicate initial server ACK received after recipient delivery ACK. |
| `Delivered` | `Failed` | **NO (BLOCKED)** | Late network error on a message already confirmed as delivered. |
| `Read` | `Failed` | **NO (BLOCKED)** | Late network error on a message already confirmed as read. |
| `Sent` | `Failed` | **NO (BLOCKED)** | Network transport timeout cannot fail an already-sent message. |

---

## 4. Database-Level Enforcement Architecture

### A. Atomic Status Validation (`MessageDao.kt`)
```kotlin
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
```

### B. Downgrade-Protected Remote Upsert (`MessageDao.kt`)
```kotlin
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
            existing.status // Preserves existing higher status!
        }
        val resolvedPendingSync = if (resolvedStatus == "sent" || resolvedStatus == "delivered" || resolvedStatus == "read") {
            false
        } else {
            message.isPendingSync
        }
        insertOrUpdateMessage(message.copy(status = resolvedStatus, isPendingSync = resolvedPendingSync))
    }
}
```

---

## 5. Automated Test Suite Verification
Verified across both unit test suites (`BharatWhatsAppReplacementTest` & `ChatConcurrencyAndRealtimeStressTest`):
- `Simultaneous incoming and outgoing message streams resolve with monotonic timestamps and valid statuses`: **PASSED**
- `Duplicate incoming WebSocket broadcast events are safely deduplicated without state regression`: **PASSED**
- `Out-of-order event sequence delivered followed by delayed sent ACK preserves Delivered state`: **PASSED**
- `Out-of-order event sequence read followed by delayed delivered ACK preserves Read state`: **PASSED**
- `Late network transport error after message delivered or read cannot downgrade status to Failed`: **PASSED**
- `Offline batch queue scalability test processes batches of 10, 50, and 100 messages`: **PASSED**
- `Simulated network timeout triggers Retry transition and subsequent successful dispatch`: **PASSED**
- `Concurrent multi-worker status updates on identical message resolve to highest monotonic status without race conditions`: **PASSED**
- `Process restart simulation restores un-synced messages in Pending state for WorkManager flush`: **PASSED**
