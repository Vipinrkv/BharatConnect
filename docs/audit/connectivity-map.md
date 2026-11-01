# BharatConnect System Connectivity & Dependency Map

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
  ▼
Data Repository (ChatRepositoryImpl)
  │
  ├──► Cryptographic Layer (SignalEncryptionManager: AES-GCM-256 + HMAC-SHA256)
  │
  ├──► Remote API (Supabase Ktor PostgREST / Realtime WebSocket)
  │
  └──► Background Sync (WorkManager: SyncWorker)
```

## 2. Determinants of Connectivity
- **Auth Determinant**: Active GoTrue session (`ensureAuthenticatedSession()` rehydrated from SessionManager).
- **Network Determinant**: Active internet connection monitored by `NetworkMonitor` (broadcast receiver); falls back to local Room DB when offline.
- **Crypto Determinant**: Symmetric 256-bit AES-GCM session key derived deterministically from canonical conversation ID.
- **Delivery Determinant**: Outbound messages transition from Room local cache (`isPendingSync = 1`, `status = "sending"`) -> Remote Supabase row (`status = "sent"`) -> Recipient presence ACK (`status = "delivered"`) -> Recipient view (`status = "read"`).
