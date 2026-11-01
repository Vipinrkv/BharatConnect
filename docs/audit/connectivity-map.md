# BharatConnect System Connectivity & Dependency Map
Document Version: 1.1.0
Cycle: Cycle 2 Audit
Status: Formally Documented, Contract Integrated & Verified

## 1. High-Level Communication Flow
```
User Action (Compose UI)
  │
  ▼
Presentation State (ChatViewModel StateFlow)
  │
  ├──► Local Cache (Room SQLite v4: ConversationDao / MessageDao)
  │
  ▼
Domain Layer (Use Cases: SendMessageUseCase, GetMessagesUseCase)
  │
  ├──► Connectivity Contract (ChatConnectivity.kt)
  │
  ▼
Data Repository (ChatRepositoryImpl)
  │
  ├──► Cryptographic Layer (SignalEncryptionManager: AES-GCM-256 + HMAC-SHA256)
  │
  ├──► Remote API (Supabase Ktor PostgREST / Realtime WebSocket)
  │
  └──► Background Sync (WorkManager: SyncWorker)
```

## 2. Formal Connectivity Contract (`ChatConnectivity.kt`)
The domain layer now exposes a decoupled contract governing all chat connectivity invariants:
- **`connectionState: StateFlow<ConnectionState>`**: Emits `Disconnected`, `Connecting`, `Connected(sessionId)`, `Reconnecting(attempt)`.
- **`messageStream: Flow<Message>`**: Emits deduplicated, decrypted incoming and outgoing message records.
- **`enqueueMessage(message)`**: Queues a message locally in Room with monotonic `Pending` status.
- **`acknowledgeDelivery(messageId)`**: Transitions remote message to `Delivered` unless already `Read`.
- **`acknowledgeRead(messageId)`**: Transitions remote message to terminal `Read` state.
- **`canTransition(current, next)`**: Strictly validates message progression using the `MessageStatus` state machine.
- **`resolveCanonicalConversationId(userA, userB)`**: Derives deterministic alphanumeric conversation ID independent of participant order (`min(userA, userB) + "_" + max(userA, userB)`).

## 3. Determinants of Connectivity
- **Auth Determinant**: Active GoTrue session (`ensureAuthenticatedSession()` rehydrated from SessionManager). Token refresh triggers automatically at expiry - 5 minutes.
- **Network Determinant**: Active network connectivity monitored via Android `ConnectivityManager.NetworkCallback`. When disconnected, all writes route to Room SQLite with `isPendingSync = 1`.
- **WebSocket Reconnection Determinant**: Supabase Realtime channel reconnects using exponential backoff (1s, 2s, 4s, 8s, up to max 30s) with randomized jitter to prevent thundering herd problems.
- **Crypto Determinant**: Symmetric 256-bit AES-GCM session key derived deterministically from canonical conversation ID. Every message uses a cryptographically secure 12-byte random IV and HMAC-SHA256 anti-tamper signature.
- **Delivery Determinant**: Outbound messages transition strictly forward: Room local cache (`isPendingSync = 1`, `status = "sending"`) -> Remote Supabase row (`status = "sent"`) -> Recipient presence ACK (`status = "delivered"`) -> Recipient view (`status = "read"`).

## 4. Offline Queue Reconciliation Protocol
1. User sends message without active network.
2. `SendMessageUseCase` generates unique UUID v4 idempotency key.
3. Message stored in Room `messages` table with `status = "pending"` and `isPendingSync = 1`.
4. UI immediately reflects message with pending clock icon.
5. `NetworkMonitor` detects restoration of internet connection (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
6. `SyncWorker` (or foreground sync dispatcher) queries Room for all rows where `isPendingSync = 1` ordered by `timestamp ASC`.
7. Each message is encrypted via `SignalEncryptionManager` and posted to Supabase via PostgREST with idempotency key.
8. Upon HTTP 201 response, Room row updated: `isPendingSync = 0`, `status = "sent"`.
9. UI updates to single grey checkmark.
