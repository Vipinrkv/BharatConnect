# BharatConnect System Connectivity & Dependency Map
Document Version: 1.2.0
Cycle: Cycle 3 Integration & Stress Testing
Status: Production Integrated, State Guarded & Concurrency Verified

## 1. High-Level Communication Flow
```
User Action (Compose UI: DirectChatScreen / ChatsTab)
  │
  ▼
Presentation State (ChatViewModel StateFlow)
  │
  ├──► Local Cache (Room SQLite v4: ConversationDao / MessageDao)
  │      └──► Transactional Guards: upsertMessageSafely & updateMessageStatusValidated
  │
  ▼
Domain Layer (Use Cases: SendMessageUseCase, GetMessagesUseCase, MarkMessageAsReadUseCase)
  │
  ├──► Connectivity Contract (ChatConnectivity.kt)
  │
  ▼
Data Repository (ChatRepositoryImpl: implements ChatRepository, ChatConnectivity)
  │
  ├──► Cryptographic Layer (SignalEncryptionManager: AES-GCM-256 + HMAC-SHA256)
  │
  ├──► Remote API (Supabase Ktor PostgREST / Realtime WebSocket)
  │      ├──► PostgREST: HTTP/2 Upsert with Client UUID Idempotency Key
  │      └──► Realtime: PostgresAction Insert/Update Streams with Local Safe Upsert
  │
  └──► Background Sync (WorkManager: SyncWorker -> retryPendingMessages())
```

## 2. Integrated Connectivity Contract (`ChatConnectivity.kt`)
The contract is implemented by `ChatRepositoryImpl` to guarantee clean modular separation:
- `observeConversations(): Flow<List<Conversation>>`: Streams Room SQLite conversations with pinned sorting.
- `observeMessages(conversationId): Flow<List<Message>>`: Streams Room SQLite messages in chronological order.
- `sendMessage(conversationId, content, mediaUrl, mediaType)`: Executes 0ms optimistic local insert (`status = "sending"`, `isPendingSync = true`), encrypts payload, and dispatches to Supabase.
- `acknowledgeDelivery(messageId, conversationId)`: Atomically transitions local Room record to `Delivered` and updates Supabase remote `WHERE status = 'sent'`.
- `markAsRead(conversationId)`: Atomically transitions all incoming messages in conversation to `Read` locally and remotely.
- `retryPendingQueue()`: Sweeps local Room messages where `isPendingSync = 1`, marks them `Retrying`, and synchronizes upstream.
- `connectRealtime(conversationId)` / `disconnectRealtime()`: Manages conversation-specific WebSocket subscription channel.
- `resolveCanonicalConversationId(userA, userB)`: Derives deterministic alphanumeric conversation ID: `SignalEncryptionManager.getDeterministicConversationId(userA, userB)`.

## 3. Database State Guard Architecture
- **Race Condition Prevention**: When duplicate WebSocket events arrive or out-of-order delivery receipts hit the client, `MessageDao.upsertMessageSafely` inspects the existing record. If the existing record is `Read` or `Delivered`, the incoming lower-rank state (e.g. `Sent`) is rejected, preventing UI regressions.
- **Atomic State Transitions**: Status modifications execute via `MessageDao.updateMessageStatusValidated`, ensuring no uncontrolled string writes bypass the `MessageStatus` transition rules.

## 4. Offline Queue Reconciliation Protocol
1. User sends message without active network.
2. `SendMessageUseCase` generates unique UUID v4 idempotency key.
3. Message stored in Room `messages` table with `status = "sending"` and `isPendingSync = 1`.
4. Network transport immediately catches offline exception and transitions state to `Failed` (`isPendingSync = 1`).
5. UI displays message bubble with retry icon.
6. When connectivity returns, `SyncWorker` (or foreground sync dispatcher) calls `retryPendingMessages()`.
7. Worker transitions messages to `Retrying`, encrypts payload, and posts to Supabase.
8. Upon HTTP 201 response, `updateMessageStatusValidated` marks message `Sent` with `isPendingSync = 0`.
9. UI automatically reflects single grey checkmark via Room's reactive `Flow`.
