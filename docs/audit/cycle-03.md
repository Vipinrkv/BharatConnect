# BharatConnect Engineering Audit — Cycle 3 Report
**Chat State Machine Integration, Real-Time Messaging & Concurrency Stress Testing**  
**Timestamp**: 2026-11-02T00:10:00+05:30  
**Audit Author**: Principal Software Architect & QA Automation Lead  
**Branch**: `main`  
**Cycle Status**: COMPLETE (All 81 Tests Passing, Zero Regressions, Native APK Verified)

---

## 1. Executive Summary & Cycle 3 Objectives
Cycle 3 transitioned the `MessageStatus` state machine from a domain model into the authoritative database and network runtime pipeline. The core objectives accomplished in this cycle include:
1. Eliminating all direct uncontrolled SQL string updates across the Chat persistence layer.
2. Implementing atomic, transactional database validation guards in `MessageDao.kt` (`updateMessageStatusValidated` and `upsertMessageSafely`).
3. Preventing status regressions caused by out-of-order WebSocket broadcast events, duplicate delivery ACKs, and delayed server confirmations.
4. Stress testing chat concurrency, offline batch queues (10, 50, and 100 messages), simulated network drops, and multithreaded state transitions.
5. Verifying the complete system with 81 passing unit tests and compiling a clean native Android release package.

---

## 2. State Machine Integration into Execution Paths

### A. Hardened Transition Logic (`MessageStatus.kt`)
The state machine transition validator was hardened to explicitly protect all lifecycle branches:
- **Terminal Read State**: Terminal state; once marked `Read` (level 5), no incoming network event or server ACK can downgrade it back to `Delivered`, `Sent`, or `Failed`.
- **Delivered State**: Can only advance to `Read`. Rejects late duplicate `Sent` ACKs and transport failure errors.
- **Sent State**: Can only advance to `Delivered` or `Read`. Rejects late transmission errors.
- **Retrying State**: Permitted from `Failed` and `Pending` during offline queue flushes.
- **Legacy Value Handling**: `MessageStatus.fromString()` safely maps unrecognized or null values to `Sent` to prevent runtime crashes.

### B. Transactional Database Status Guards (`MessageDao.kt`)
Direct calls to `@Query("UPDATE messages SET status = ...")` were encapsulated inside `@Transaction` methods:
1. **`updateMessageStatusValidated(messageId, targetStatus, isPendingSync)`**:
   Fetches existing entity from SQLite, verifies `canTransition(currentStatus, targetStatus)`, and executes update only if valid.
2. **`upsertMessageSafely(message)`**:
   Inspects existing record on primary key collision. If incoming payload contains a lower status rank than existing cache, preserves the higher rank (e.g. preserves `Read` when incoming payload carries `Sent`).
3. **`insertMessagesSafely(messages)`**:
   Applies safe upsert logic across batch fetch collections.

### C. Repository Integration (`ChatRepositoryImpl.kt`)
- `sendMessage`: Optimistic local insert via `upsertMessageSafely`, followed by `updateMessageStatusValidated` on Supabase HTTP 201 response (`Sent`, `isPendingSync = false`) or network exception (`Failed`, `isPendingSync = true`).
- `retryPendingMessages`: Transitions queued records to `Retrying`, sends payload, and updates status to `Sent` upon success.
- `subscribeToRealtime` & `subscribeToGlobalUserMessages`: Uses `upsertMessageSafely` for all incoming `PostgresAction.Insert` and `PostgresAction.Update` packets.
- `acknowledgeMessageDelivered`: Updates local SQLite atomically via `updateMessageStatusValidated(messageId, MessageStatus.Delivered, false)`.

---

## 3. Real-Time Messaging & WebSocket Reliability
- **Deduplication**: Incoming Realtime packets are keyed on message UUID in Room SQLite. Duplicate broadcast arrivals execute safe upserts that do not trigger duplicate insertions or recomposition loops.
- **Channel Cleanup**: Explicit `unsubscribeRealtime()` cleans up active Ktor Realtime subscriptions prior to establishing new conversation listeners, preventing leaked WebSocket connections.
- **Decryption Pipeline**: Inbound messages are decrypted via `SignalEncryptionManager.decrypt` prior to SQLite caching, ensuring zero plaintext exposure over the network and instant zero-latency rendering from local Room flows.

---

## 4. Concurrency & Stress Testing Evidence

A dedicated JUnit coroutine concurrency test suite was authored in [`ChatConcurrencyAndRealtimeStressTest.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/test/java/com/bharatconnect/app/ChatConcurrencyAndRealtimeStressTest.kt):

| Test Case Name | Objective / Scenario | Result |
|---|---|---|
| `Simultaneous incoming and outgoing message streams` | 50 outgoing + 50 incoming concurrent coroutines with interleaved timestamps | **PASSED** |
| `Duplicate incoming WebSocket broadcast events` | Stale `Sent` ACK arrives after message marked `Read` | **PASSED** (Downgrade rejected) |
| `Out-of-order Delivered followed by delayed Sent ACK` | Verifies `Delivered -> Sent` is blocked; `Delivered -> Read` permitted | **PASSED** |
| `Out-of-order Read followed by delayed Delivered ACK` | Verifies `Read -> Delivered` is blocked; `Read` is terminal | **PASSED** |
| `Late network transport error after Delivered or Read` | Verifies network blip cannot transition `Delivered` or `Read` to `Failed` | **PASSED** |
| `Offline batch queue scalability test` | Scalability verification under 10, 50, and 100 message offline bursts | **PASSED** |
| `Simulated network timeout triggers Retry transition` | `Pending -> Sending -> Failed -> Retrying -> Sending -> Sent` | **PASSED** |
| `Concurrent multi-worker status updates on identical message` | 6 concurrent coroutines updating atomic message state | **PASSED** (Terminal `Read` preserved) |
| `Process restart simulation restores un-synced messages` | Serialization & rehydration of un-synced entity across process death | **PASSED** |
| `Canonical conversation ID derivation symmetry` | Symmetry check across UUIDs, phone numbers, and username pairs | **PASSED** |

### Complete Test Execution Results:
Command executed:
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-19"; ./gradlew testDebugUnitTest
```
- **Total Test Suites**: 9 suites
- **Total Unit Tests Executed**: 81 tests (increased from 71 in Cycle 2, 69 in Cycle 1)
- **Tests Passed**: 81 (100%)
- **Failures / Errors**: 0
- **Skipped**: 0
- **Execution Time**: 1m 4s

---

## 5. Native Android APK Build & Checksum Verification
- **Gradle Task**: `:app:assembleDebug` executed successfully (35 actionable tasks: 4 executed, 31 up-to-date).
- **Target File**: `BharatConnect-Native.apk` (and `web/BharatConnect-Native.apk`).
- **File Size**: 26,410,578 bytes (~26.4 MB).
- **Cryptographic SHA-256 Checksum**:
  `3E9076EEF2D8C70FD995CAB798A2AFA1C777274BA8A22D7955B2488F2CFD8E15`
- **Web Distribution Synchronization**: Updated `web/index.html` and `web/app.js` with matching SHA-256 hash and size badge.

---

## 6. Remaining Risks & Observations
1. **ISS-005 (Cryptographic Key Derivation)**: Symmetric keys are derived from conversation ID and static application salt via SHA-256. While fast and offline-capable, it does not provide forward secrecy across session ratchet steps. Queued for Cycle 4 security hardening.
2. **ISS-006 (Realtime WebSocket Reconnection)**: Global realtime listener in `ChatViewModel.kt` reconnects via a coroutine delay loop on socket drop. Targeted for formal connection lifecycle manager with randomized exponential backoff jitter in Cycle 5.

---

## 7. Next Cycle Recommendations (Cycle 4 Preparation)
With the chat state machine fully integrated, tested under concurrency, and hardened against regressions in Cycle 3, the system is ready for **Cycle 4: Security Hardening, Cryptographic Ratchet & Identity Audit**:
1. Audit cryptographic key storage in Android Keystore (`KeyStore` provider).
2. Harden session token refresh cycles and biometric authentication timeout guards.
3. Assess RLS policies on Supabase tables to ensure message privacy enforcement.
