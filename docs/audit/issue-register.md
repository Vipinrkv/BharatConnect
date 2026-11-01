# BharatConnect Issue Register & Defect Log
Audit started: 2026-11-01T23:25:00+05:30 (Cycle 1)

| Issue ID | Subsystem | Severity | Description | Root Cause | Status |
|---|---|---|---|---|---|
| ISS-001 | Chat / UI | Medium | Disconnected dropdown menu options in `ChatsTab.kt` ("Mark all read", "Clear all chats") | Dropdown items only set transient notice strings without invoking Room DAO or repository | Resolved in Cycle 1 (wired to `ChatViewModel`) |
| ISS-002 | Chat / UI | Low | Reversed navigation icon in `GroupsSubTab.kt` on active group cards | ArrowBack vector icon used instead of ArrowForward chevron | Resolved in Cycle 1 (updated to `Icons.AutoMirrored.Filled.ArrowForward`) |
| ISS-003 | Architecture | Medium | Chat feature lacks explicit formal `ChatConnectivity` contract document/interface | Connectivity logic dispersed across ViewModel, Repository, and Supabase client | Under observation for Cycle 2/6 modularization |
| ISS-004 | Chat / State | Low | Message status transitions lack a dedicated isolated state machine engine | Status values strings ("pending", "sending", "sent", "delivered", "read") mapped in repository | Target for Cycle 3 state machine formalization |
