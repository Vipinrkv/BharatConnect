# BharatConnect Message Status State Machine Specification
Document Version: 1.0.0
Cycle: Cycle 2 Audit
Status: Formally Defined & Unit Tested

## 1. Executive Summary
Message delivery integrity in a high-concurrency, offline-capable messaging application requires a formal, monotonic state machine. Without strict transition enforcement, late out-of-order WebSocket broadcast events or duplicated network ACKs can overwrite terminal states (e.g., reverting a `read` status back to `delivered` or `sent`).

In Cycle 2, BharatConnect formalizes `com.bharatconnect.app.domain.model.state.MessageStatus` with an explicit hierarchy, strict forward progression, and downgrade prevention.

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

## 3. Transition Matrix

| Current State | Target State | Permitted? | Condition / Trigger |
|---|---|---|---|
| `Draft` | `Pending` | YES | User taps Send button while offline. |
| `Draft` | `Sending` | YES | User taps Send button while network is active. |
| `Pending` | `Sending` | YES | Network monitor detects connectivity; sync dispatcher awakens. |
| `Sending` | `Sent` | YES | Supabase PostgREST returns 201 Created HTTP confirmation. |
| `Sending` | `Failed` | YES | Network connection drops, timeout (>15s), or server 5xx error. |
| `Sent` | `Delivered` | YES | Supabase Realtime broadcast receipt received from recipient client. |
| `Sent` | `Read` | YES | Direct jump if recipient reads immediately without intermediate ACK. |
| `Delivered` | `Read` | YES | Supabase Realtime broadcast receipt received indicating user viewed message. |
| `Failed` | `Retrying` | YES | WorkManager auto-retry or user taps retry button. |
| `Retrying` | `Sending` | YES | Retry worker starts active HTTP dispatch. |
| `Retrying` | `Failed` | YES | Retry exhausted or network immediately unavailable. |
| `Read` | `Delivered` | **NO (BLOCKED)** | Out-of-order delayed delivery receipt arrives after read receipt. |
| `Read` | `Sent` | **NO (BLOCKED)** | Delayed server broadcast arrives after recipient has already read. |
| `Delivered` | `Sent` | **NO (BLOCKED)** | Duplicate initial server ACK received after recipient delivery ACK. |
| `Read` | `Failed` | **NO (BLOCKED)** | Network error on a message already confirmed as read. |

---

## 4. Downgrade Prevention Logic
The core enforcement logic is implemented in `MessageStatus.canTransition(current, next)`:

```kotlin
fun canTransition(current: MessageStatus, next: MessageStatus): Boolean {
    // 1. Terminal Read state is immutable against network regressions
    if (current is Read) return false

    // 2. Failed state allows retry paths
    if (current is Failed) {
        return next is Retrying || next is Pending || next is Sending
    }

    // 3. Retrying state can transition to Sending or back to Failed
    if (current is Retrying) {
        return next is Sending || next is Failed || next is Sent
    }

    // 4. Strictly enforce non-decreasing monotonic progression
    return next.level >= current.level
}
```

---

## 5. Storage & Serialization Mapping
- In Room SQLite (`MessageEntity`), status is persisted as an indexed lowercase string: `"draft"`, `"pending"`, `"sending"`, `"sent"`, `"delivered"`, `"read"`, `"failed"`, `"retrying"`.
- `MessageStatus.fromString(value)` parses strings safely, with unrecognized strings falling back to `Pending` to prevent application crashes or data corruption.

---

## 6. Test Suite Verification
Verified by automated JUnit test suite `com.bharatconnect.app.BharatWhatsAppReplacementTest`:
- `MessageStatus state machine enforces forward progression and prevents invalid downgrades`: **PASSED**
- `MessageStatus string mapping correctly parses valid and fallback values`: **PASSED**
