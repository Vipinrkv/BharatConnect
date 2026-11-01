# BharatConnect Issue Register & Defect Log
Audit Updated: 2026-11-01T23:42:00+05:30 (Cycle 2 Completed)

| Issue ID | Subsystem | Severity | Description | Root Cause | Status |
|---|---|---|---|---|---|
| ISS-001 | Chat / UI | Medium | Disconnected dropdown menu options in `ChatsTab.kt` ("Mark all read", "Clear all chats") | Dropdown items only set transient notice strings without invoking Room DAO or repository | Resolved in Cycle 1 (wired to `ChatViewModel`) |
| ISS-002 | Chat / UI | Low | Reversed navigation icon in `GroupsSubTab.kt` on active group cards | ArrowBack vector icon used instead of ArrowForward chevron | Resolved in Cycle 1 (updated to `Icons.AutoMirrored.Filled.ArrowForward`) |
| ISS-003 | Architecture | Medium | Chat feature lacks explicit formal `ChatConnectivity` contract document/interface | Connectivity logic dispersed across ViewModel, Repository, and Supabase client | Resolved in Cycle 2 (`com.bharatconnect.app.domain.connectivity.ChatConnectivity` implemented) |
| ISS-004 | Chat / State | Medium | Message status transitions lack a dedicated isolated state machine engine, vulnerable to status downgrades | Status values strings ("pending", "sending", "sent", "delivered", "read") mapped loosely in repository | Resolved in Cycle 2 (`com.bharatconnect.app.domain.model.state.MessageStatus` sealed class state machine with downgrade prevention implemented and tested) |
| ISS-005 | Security / Crypto | Low | Symmetric key derivation in `SignalEncryptionManager.kt` uses deterministic SHA-256 derivation over conversation ID and salt rather than asymmetric X3DH key agreement | Simplified protocol implementation for mobile prototype | Documented in Cycle 2; queued for Cycle 4 security hardening |
