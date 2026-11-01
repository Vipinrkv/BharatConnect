# Cycle 01 Audit Report — System Inventory, Baseline & Chat Architecture Audit
**Date**: 2026-11-01T23:25:00+05:30  
**Target**: Complete Application Inventory & Chat Subsystem Baseline

---

### 1. Scope Inspected
- **Architecture Stack**: Native Android (Kotlin 1.9.22, Compose M3, Room SQLite v4, Supabase Ktor Client 2.1.3, WorkManager 2.9.0).
- **Core Chat Files**:
  - Presentation: `ChatsTab.kt`, `ChatDetailScreen.kt`, `ChatViewModel.kt`, `MessageBubble.kt`, `SelectContactBottomSheet.kt`, `GroupsSubTab.kt`, `CommunitiesSubTab.kt`.
  - Domain: `ChatModels.kt`, `ChatRepository.kt`, `ChatUseCases.kt`.
  - Data: `ChatRepositoryImpl.kt`, `ChatDtos.kt`, `ConversationDao.kt`, `MessageDao.kt`, `ConversationEntity.kt`, `MessageEntity.kt`.
  - Security & Hardware: `SignalEncryptionManager.kt`, `UpiPaymentManager.kt`, `AudioRecordManager.kt`, `AudioPlayerManager.kt`.
- **Distribution Layer**: Root `BharatConnect-Native.apk`, `web/BharatConnect-Native.apk`, `web/index.html`, `web/app.js`.

---

### 2. Confirmed Findings & Root Causes
1. **Disconnected Bulk Menu Operations in ChatsTab**:
   - *Symptom*: Selecting "Mark all read" or "Clear all chats" displayed a temporary UI snackbar but did not alter the database or StateFlow.
   - *Root Cause*: Lack of underlying methods on `ChatViewModel` to perform mass updates on Room DB.
   - *Resolution*: Implemented `markAllConversationsRead()` and `clearAllConversations()` in `ChatViewModel.kt` updating Room SQLite DAOs.
2. **Reversed Arrow Vector in GroupsSubTab**:
   - *Symptom*: Active group items displayed a backwards-pointing arrow (`ArrowBack`).
   - *Root Cause*: Copy-paste error from back navigation icons.
   - *Resolution*: Updated to `Icons.AutoMirrored.Filled.ArrowForward`.
3. **Module Connectivity**:
   - The Chat subsystem is well-structured in Clean Architecture layers (Presentation -> Domain Use Cases -> Data Repository -> Room/Supabase), but lacks an explicit contract document detailing inter-module connectivity determinants.

---

### 3. Decisions Made
- **Keep**: MVVM Clean Architecture with Room SQLite v4 and Supabase Ktor Realtime client.
- **Improve**: Wire all UI menu controls to persistent storage.
- **Defer**: Architectural restructuring into isolated feature connectivity contracts until Cycle 2 & Cycle 6 after full analysis.

---

### 4. Changes Made
- `ChatViewModel.kt`: Added `markAllConversationsRead()` and `clearAllConversations()`.
- `ChatsTab.kt`: Wired dropdown menu items to ViewModel.
- `GroupsSubTab.kt`: Replaced navigation icon with forward chevron.
- `BharatWhatsAppReplacementTest.kt`: Added automated unit tests for batch reset and deterministic ID symmetry.
- `BharatConnect-Native.apk` & `web/BharatConnect-Native.apk`: Recompiled cleanly (26.4 MB, SHA-256: `e4f1d8c2c7937bcdc856bc5ea6df8adf6bf21eeed655d9763cd6e7fba1377186`).
- `SYSTEM_CHANGELOG.md`: Appended Entry #040.

---

### 5. Verification & Test Evidence
- **Command Executed**: `./gradlew testDebugUnitTest` under Java 19.
- **Result**: **69 / 69 unit & integration tests passing (100% pass rate)**.
- **Compilation**: `./gradlew assembleDebug` passed with 0 warnings.
- **Parity**: Verified SHA-256 matches across root and web APKs.

---

### 6. Remaining Risks & Observations
- Message state transitions ("pending", "sending", "sent", "delivered", "read") currently rely on string values in `MessageDao` and `ChatRepositoryImpl`. A formal enum/sealed-class state machine will prevent invalid out-of-order transitions during intermittent network conditions.

---

### 7. Next Cycle Focus (Cycle 2: Deep Architecture, File & Dependency Analysis)
- Deep inspection of the 4-layer architecture: Presentation -> Domain -> Data & Connectivity -> Backend & Infrastructure.
- Design of explicit `ChatConnectivity` contract interface to decouple Chat from Auth, Contacts, and Profile features.
