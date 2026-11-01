# BharatConnect Chat Subsystem Dependency Map & Layered Architecture
Document Version: 1.0.0
Cycle: Cycle 2 Audit
Status: Formally Documented & Verified

## 1. Architectural Topology Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                              │
│  Compose UI: ChatsTab.kt, DirectChatScreen.kt, GroupsSubTab.kt         │
│  State Holder: ChatViewModel.kt (StateFlow<ChatUiState>)               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ consumes
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                 │
│  Contracts: ChatConnectivity.kt, ChatRepository.kt                     │
│  Use Cases: SendMessageUseCase, GetMessagesUseCase,                    │
│             GetConversationsUseCase, MarkMessageAsReadUseCase          │
│  State Models: MessageStatus.kt, Message.kt, Conversation.kt           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ implemented by
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                  │
│  Repository: ChatRepositoryImpl.kt, Mappers.kt                         │
│  Idempotency: IdempotencyKeyManager.kt (UUID v4 + deduplication cache) │
└───────┬───────────────────────────┼───────────────────────────┬────────┘
        │                           │                           │
        ▼                           ▼                           ▼
┌──────────────┐          ┌──────────────────┐        ┌──────────────────┐
│ LOCAL DB     │          │ CRYPTO / E2EE    │        │ REMOTE NETWORK   │
│ Room SQLite  │          │ SignalEncryption │        │ Supabase Client  │
│ AppDatabase  │          │ AES-GCM-256      │        │ PostgREST Ktor   │
│ MessageDao   │          │ HMAC-SHA256      │        │ Realtime WSS     │
│ ConvDao      │          │ Keystore KeyGen  │        │ WorkManager Sync │
└──────────────┘          └──────────────────┘        └──────────────────┘
```

---

## 2. Layer-by-Layer Component Inventory

### A. Presentation Layer
- **Components**:
  - `ChatsTab.kt`: Primary chat list, unread counters, search filter, action overflow ("Mark all read", "Clear all chats").
  - `DirectChatScreen.kt`: Active 1-on-1 and group thread UI, message bubble rendering, attachment picker, typing indicator, send action.
  - `GroupsSubTab.kt`: Multi-user channel list, participant counts, group creation dialog.
  - `CallsSubTab.kt`: Audio/video call history, WebRTC call initiation intents.
  - `ChatViewModel.kt`: Encapsulates all presentation state (`ChatUiState`), exposes reactive `StateFlow`, handles UI events, and dispatches coroutine jobs.
- **Dependency Invariant**: Depends strictly on Domain Use Cases and `ChatConnectivity` contract. Has no direct knowledge of Supabase, Room DAOs, or raw network sockets.

### B. Domain Layer
- **Components**:
  - `ChatConnectivity.kt`: Contract defining message streams, connection lifecycle, send dispatch, and ACK handling.
  - `ChatRepository.kt`: Repository interface declaring operations (`getMessages`, `sendMessage`, `syncOfflineMessages`, `clearConversation`).
  - `SendMessageUseCase.kt`: Validates message length (1–4096 chars), assigns idempotency key, enqueues local draft, invokes repo.
  - `GetMessagesUseCase.kt`: Streams Room SQLite messages sorted by timestamp ascending.
  - `GetConversationsUseCase.kt`: Streams conversation list with latest snippet and unread badges.
  - `MessageStatus.kt`: Sealed-class state machine enforcing unidirectional forward progression and downgrade prevention.
- **Dependency Invariant**: Pure Kotlin layer. Zero dependencies on `android.*`, `io.github.jan.supabase.*`, or `androidx.room.*`.

### C. Data Layer
- **Components**:
  - `ChatRepositoryImpl.kt`: Orchestrates local-first Room cache, cryptographic encryption, remote Supabase REST dispatch, and Realtime event handling.
  - `Mappers.kt`: Bi-directional converters between `MessageEntity` <-> `Message` domain model and `SupabaseMessageDto` <-> `Message`.
  - `IdempotencyKeyManager.kt`: Guarantees exactly-once delivery semantics by attaching client UUIDs and deduplicating in-flight packets.
- **Dependency Invariant**: Bridges Domain abstractions to concrete infrastructure.

### D. Cryptographic & Security Subsystem
- **Components**:
  - `SignalEncryptionManager.kt`: AES-GCM-256 payload encryption with ephemeral 12-byte IV per message and HMAC-SHA256 tamper-evident signing.
  - `BiometricPromptManager.kt`: AndroidX BiometricPrompt integration protecting app launch and chat unlocking.
  - `SessionManager.kt`: Encrypted SharedPreferences storing GoTrue JWTs and refresh tokens.

### E. Remote & Sync Subsystem
- **Components**:
  - `SupabaseClient`: PostgREST table querying (`messages`, `conversations`, `profiles`).
  - Realtime WebSocket: Channel subscription to `public:messages` for low-latency live streaming.
  - `SyncWorker.kt`: AndroidX WorkManager task that pulls queued messages with `isPendingSync = 1` and delivers them when connectivity is restored.
  - `NetworkMonitor.kt`: Observes `ConnectivityManager` callbacks to notify state listeners.

---

## 3. Cross-Subsystem Interaction Boundaries

| Origin Feature | Target Feature | Communication Mechanism | Coupling Level |
|---|---|---|---|
| User Contacts | Chat Subsystem | Phone number hash lookup (`SHA-256`) against Supabase `profiles` table | Loose (Domain model) |
| Social Feed | Chat Subsystem | Deep-link to direct message sender via user profile ID | Loose (Navigation route) |
| Chat Subsystem | Push Notifications | Supabase database webhook -> FCM HTTP v1 -> `FirebaseMessagingService` | Decoupled (Cloud event) |
| Chat Subsystem | Media Subsystem | Cloudinary HTTPS upload -> Media URL attached to encrypted message payload | Loose (URL string) |

---

## 4. Architectural Health & Modularity Score
- **Circular Dependencies**: 0 detected across Chat packages.
- **Leaked UI References**: 0 (no Activity or Context retained in ViewModels or Repositories).
- **Interface Segregation**: `ChatConnectivity` separates connection monitoring from CRUD repository methods.
- **Offline Resiliency**: 100% of read and write operations hit Room SQLite cache first; background sync handles upstream egress.
