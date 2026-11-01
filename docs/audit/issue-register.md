# BharatConnect Issue Register & Defect Log
Audit Updated: 2026-11-02T00:09:00+05:30 (Cycle 3 Completed)

| Issue ID | Subsystem | Severity | Description | Root Cause | Status |
|---|---|---|---|---|---|
| ISS-001 | Chat / UI | Medium | Disconnected dropdown menu options in `ChatsTab.kt` ("Mark all read", "Clear all chats") | Dropdown items only set transient notice strings without invoking Room DAO or repository | Resolved in Cycle 1 (wired to `ChatViewModel`) |
| ISS-002 | Chat / UI | Low | Reversed navigation icon in `GroupsSubTab.kt` on active group cards | ArrowBack vector icon used instead of ArrowForward chevron | Resolved in Cycle 1 (updated to `Icons.AutoMirrored.Filled.ArrowForward`) |
| ISS-003 | Architecture | Medium | Chat feature lacks explicit formal `ChatConnectivity` contract document/interface | Connectivity logic dispersed across ViewModel, Repository, and Supabase client | Resolved in Cycle 2 (`com.bharatconnect.app.domain.connectivity.ChatConnectivity` implemented) |
| ISS-004 | Chat / State | High | Message status transitions lacked atomic transactional database enforcement, permitting regressions | Direct SQL status updates and standard REPLACE onConflict allowed late ACKs to downgrade Read/Delivered states | Resolved in Cycle 3 (Integrated `MessageDao.updateMessageStatusValidated`, `MessageDao.upsertMessageSafely`, and verified with 10 concurrency stress tests) |
| ISS-005 | Security / Crypto | Low | Symmetric key derivation in `SignalEncryptionManager.kt` uses deterministic SHA-256 derivation over conversation ID and salt rather than asymmetric X3DH key agreement | Simplified protocol implementation for mobile prototype | Documented in Cycle 2; queued for Cycle 4 security hardening |
| ISS-006 | Network / WSS | Medium | Global realtime listener in `ChatViewModel.kt` reconnects via coroutine polling loop (`while(isActive)`) on socket disruption | Native Ktor WebSocket client does not expose auto-reconnect with randomized jitter hooks directly to UI | Queued for Cycle 5 Network & Sync resilience hardening |
