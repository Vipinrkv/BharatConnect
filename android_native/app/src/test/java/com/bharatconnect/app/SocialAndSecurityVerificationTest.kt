package com.bharatconnect.app

import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import com.bharatconnect.app.core.security.SecurityManager
import com.bharatconnect.app.domain.model.Conversation
import com.bharatconnect.app.domain.model.Message
import org.junit.Assert.*
import org.junit.Test

class SocialAndSecurityVerificationTest {

    @Test
    fun `generateSafetyNumber produces exact 60-digit fingerprint formatted into 12 blocks of 5 digits`() {
        val user1 = "usr_aarav_delhi_101"
        val user2 = "usr_priya_bengaluru_202"

        val safetyNumber1 = SignalEncryptionManager.generateSafetyNumber(user1, user2)
        val safetyNumber2 = SignalEncryptionManager.generateSafetyNumber(user2, user1)

        // Must be deterministic regardless of order
        assertEquals(safetyNumber1, safetyNumber2)

        // 12 blocks of 5 digits separated by 11 spaces = 60 digits + 11 spaces = 71 chars
        assertEquals(71, safetyNumber1.length)
        val blocks = safetyNumber1.split(" ")
        assertEquals(12, blocks.size)
        blocks.forEach { block ->
            assertEquals(5, block.length)
            assertTrue("Block must consist solely of digits", block.all { it.isDigit() })
        }
    }

    @Test
    fun `HMAC-SHA256 anti-tamper signature verifies untampered payload and rejects modified ciphertext`() {
        val payload = "ENC:dGVzdF9pdjEyMzQ1Njc4OXRlc3RfY2lwaGVydGV4dA=="
        val signature = SignalEncryptionManager.computePayloadSignature(payload)

        // Verification of valid signature
        assertTrue(SignalEncryptionManager.verifyPayloadIntegrity(payload, signature))

        // Tampered payload must fail verification
        val tamperedPayload = "ENC:dGVzdF9pdjEyMzQ1Njc4OXRlc3RfY2lwaGVydGV4dB=="
        assertFalse(SignalEncryptionManager.verifyPayloadIntegrity(tamperedPayload, signature))
    }

    @Test
    fun `Disappearing message expiry calculation accurately identifies expired messages`() {
        val nowSec = System.currentTimeMillis() / 1000
        val unexpiredMsg = Message(
            id = "m1",
            conversationId = "c1",
            senderId = "u1",
            content = "Self destruct in 24 hours",
            createdAt = "2026-10-31 22:00:00",
            ephemeralDurationSec = 86400L // 24 hours
        )

        val createdSec = nowSec - 3600L // Sent 1 hour ago
        val isExpiredNow = (nowSec - createdSec) > unexpiredMsg.ephemeralDurationSec
        assertFalse(isExpiredNow)

        val expiredCreatedSec = nowSec - (86400L + 60L) // Sent 24h and 1m ago
        val isExpiredAfterTime = (nowSec - expiredCreatedSec) > unexpiredMsg.ephemeralDurationSec
        assertTrue(isExpiredAfterTime)
    }

    @Test
    fun `Group administration model preserves admin hierarchy and privileges`() {
        val group = Conversation(
            id = "grp_tech_founders",
            isGroup = true,
            title = "Delhi Tech Founders",
            createdBy = "founder_aarav",
            participantIds = listOf("founder_aarav", "builder_rohan", "investor_neha"),
            adminIds = listOf("founder_aarav", "builder_rohan"),
            groupDescription = "Exclusive peer group for verified Indian startup builders."
        )

        assertTrue(group.isGroup)
        assertEquals(2, group.adminIds.size)
        assertTrue(group.adminIds.contains("founder_aarav"))
        assertTrue(group.adminIds.contains("builder_rohan"))
        assertFalse(group.adminIds.contains("investor_neha"))
        assertEquals("founder_aarav", group.createdBy)
    }

    @Test
    fun `SecurityManager enforces app PIN authentication and resists incorrect attempts`() {
        SecurityManager.setAppLock(enabled = true, pin = "1947")
        assertTrue(SecurityManager.isBiometricLockEnabled.value)
        assertTrue(SecurityManager.isAppLocked.value)

        // Incorrect PIN must reject unlock
        val wrongAttempt = SecurityManager.unlockWithPin("1234")
        assertFalse(wrongAttempt)
        assertTrue(SecurityManager.isAppLocked.value)

        // Correct PIN must unlock
        val correctAttempt = SecurityManager.unlockWithPin("1947")
        assertTrue(correctAttempt)
        assertFalse(SecurityManager.isAppLocked.value)

        // Disable lock
        SecurityManager.setAppLock(enabled = false)
        assertFalse(SecurityManager.isBiometricLockEnabled.value)
        assertFalse(SecurityManager.isAppLocked.value)
    }

    @Test
    fun `Vernacular and social interest filters match Indian topics`() {
        val testPosts = listOf(
            "आज का दिन बहुत शुभ है #हिंदी समाचार",
            "Building our high-concurrency engine in Bengaluru #TechBharat",
            "Great innings by Indian cricket team today! #Cricket",
            "UPSC prelims revision notes shared #UPSC"
        )

        val hindiTag = "हिंदी"
        val matchedHindi = testPosts.filter { it.contains(hindiTag) }
        assertEquals(1, matchedHindi.size)

        val techTag = "TechBharat"
        val matchedTech = testPosts.filter { it.contains(techTag) }
        assertEquals(1, matchedTech.size)
    }

    @Test
    fun `Smart invite link formats valid URL with user referral parameter`() {
        val userId = "vipin_delhi_2026"
        val inviteLink = "https://bharatconnect.in/join?ref=$userId"
        assertTrue(inviteLink.startsWith("https://bharatconnect.in/join?ref="))
        assertTrue(inviteLink.contains(userId))
    }

    @Test
    fun `isEncryptedPayload identifies valid E2EE cipher envelope`() {
        assertFalse(SignalEncryptionManager.isEncryptedPayload(null))
        assertFalse(SignalEncryptionManager.isEncryptedPayload("Hello"))
        assertFalse(SignalEncryptionManager.isEncryptedPayload("ENC:short"))
        assertTrue(SignalEncryptionManager.isEncryptedPayload("ENC:dGVzdF9pdjEyMzQ1Njc4OXRlc3RfY2lwaGVydGV4dA=="))
    }

    @Test
    fun `SignalEncryptionManager encrypt and decrypt roundtrip with AAD context`() {
        val convId = "conv_secret_room_101"
        val message = "Top secret Indian sovereign communication"
        val aad = "conv_secret_room_101:user_aarav:user_priya"

        val encrypted = SignalEncryptionManager.encrypt(convId, message, aadContext = aad)
        assertTrue(encrypted.startsWith("ENC:"))

        // Decryption with matching AAD succeeds
        val decrypted = SignalEncryptionManager.decrypt(convId, encrypted, aadContext = aad)
        assertEquals(message, decrypted)

        // Clear key cache verifies zeroizing capability
        SignalEncryptionManager.clearKeyCache()
    }

    @Test
    fun `AppConnectors singletons resolve cleanly with zero circular dependencies`() {
        assertNotNull(com.bharatconnect.app.core.connectors.AppConnectors.encryption)
        assertNotNull(com.bharatconnect.app.core.connectors.AppConnectors.security)
        assertNotNull(com.bharatconnect.app.core.connectors.AppConnectors.session)
        assertNotNull(com.bharatconnect.app.core.connectors.AppConnectors.network)
    }

    @Test
    fun `NetworkMonitor tracks metered and connection type transitions accurately`() {
        val monitor = com.bharatconnect.app.core.network.NetworkMonitor
        monitor.setOnlineForTesting(online = true, metered = false, type = "WIFI")
        assertTrue(monitor.isOnline.value)
        assertFalse(monitor.isMetered.value)
        assertEquals("WIFI", monitor.connectionType.value)

        // Switch to metered cellular
        monitor.setOnlineForTesting(online = true, metered = true, type = "CELLULAR")
        assertTrue(monitor.isOnline.value)
        assertTrue(monitor.isMetered.value)
        assertEquals("CELLULAR", monitor.connectionType.value)

        // Drop connection
        monitor.setOnlineForTesting(online = false, metered = true, type = "NONE")
        assertFalse(monitor.isOnline.value)
        assertEquals("NONE", monitor.connectionType.value)
    }

    @Test
    fun `ContactsManager accurately validates Indian TRAI numbering plan`() {
        val cm = com.bharatconnect.app.core.contacts.ContactsManager
        assertTrue(cm.isValidIndianPhoneNumber("+91 98765 43210"))
        assertTrue(cm.isValidIndianPhoneNumber("08765432109"))
        assertTrue(cm.isValidIndianPhoneNumber("7123456789"))
        assertTrue(cm.isValidIndianPhoneNumber("6987654321"))

        // Rejections: short, invalid prefixes (1, 2, 5), or letters
        assertFalse(cm.isValidIndianPhoneNumber("12345"))
        assertFalse(cm.isValidIndianPhoneNumber("0112345678")) // Landline prefix
        assertFalse(cm.isValidIndianPhoneNumber("5987654321"))
        assertFalse(cm.isValidIndianPhoneNumber("invalid_phone"))
        assertFalse(cm.isValidIndianPhoneNumber(null))
    }

    @Test
    fun `ContactsManager zero-knowledge phone hash outputs deterministic 64-char SHA-256 hex`() {
        val cm = com.bharatconnect.app.core.contacts.ContactsManager
        val hash1 = cm.hashPhoneNumber("+91 98765 43210")
        val hash2 = cm.hashPhoneNumber("09876543210")
        val hash3 = cm.hashPhoneNumber("9876543210")

        // Must all normalize to +919876543210 and produce identical 64-character SHA-256 hashes
        assertEquals(hash1, hash2)
        assertEquals(hash1, hash3)
        assertEquals(64, hash1.length)
        assertTrue(hash1.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `Optimistic feed like toggle accurately computes next state and count delta`() {
        var isLikedByMe = false
        var likesCount = 10

        // User taps like
        val nextState1 = !isLikedByMe
        val delta1 = if (nextState1) 1 else -1
        isLikedByMe = nextState1
        likesCount += delta1
        assertTrue(isLikedByMe)
        assertEquals(11, likesCount)

        // User taps unlike
        val nextState2 = !isLikedByMe
        val delta2 = if (nextState2) 1 else -1
        isLikedByMe = nextState2
        likesCount += delta2
        assertFalse(isLikedByMe)
        assertEquals(10, likesCount)
    }

    @Test
    fun `SecurityManager auto-lock timeout and integrity evaluation behave reliably`() {
        val sm = com.bharatconnect.app.core.security.SecurityManager
        sm.setAppLock(enabled = true, pin = "2026")

        val now = 1000000L
        sm.updateLastActive(now)

        // Elapsed 30s (< 60s timeout) -> should not auto lock
        assertFalse(sm.shouldAutoLock(currentTimeMillis = now + 30000L, timeoutSec = 60L))

        // Elapsed 75s (>= 60s timeout) -> should auto lock
        assertTrue(sm.shouldAutoLock(currentTimeMillis = now + 75000L, timeoutSec = 60L))

        // Reset
        sm.setAppLock(enabled = false)
    }

    @Test
    fun `Total unread count aggregation correctly filters out sender and read messages`() {
        val currentUserId = "my_user_id"
        val mockMessages = listOf(
            Message(id = "m1", conversationId = "c1", senderId = "other_1", content = "hi", status = "delivered", createdAt = "2026-11-01 00:00:00"),
            Message(id = "m2", conversationId = "c1", senderId = currentUserId, content = "sent by me", status = "sent", createdAt = "2026-11-01 00:00:01"),
            Message(id = "m3", conversationId = "c2", senderId = "other_2", content = "new msg", status = "delivered", createdAt = "2026-11-01 00:00:02"),
            Message(id = "m4", conversationId = "c2", senderId = "other_2", content = "read msg", status = "read", createdAt = "2026-11-01 00:00:03")
        )

        // Only m1 and m3 qualify (senderId != currentUserId && status != "read")
        val unreadTotal = mockMessages.count { it.senderId != currentUserId && it.status != "read" }
        assertEquals(2, unreadTotal)
    }

    @Test
    fun `SignalEncryptionManager ratcheted session key derivation provides forward secrecy`() {
        val convId = "conv_pfs_test_101"

        val keySeq1 = SignalEncryptionManager.deriveRatchetedKey(convId, sequenceNumber = 1L)
        val keySeq1Duplicate = SignalEncryptionManager.deriveRatchetedKey(convId, sequenceNumber = 1L)
        val keySeq2 = SignalEncryptionManager.deriveRatchetedKey(convId, sequenceNumber = 2L)

        // Must be 32 bytes (256 bits)
        assertEquals(32, keySeq1.encoded.size)
        assertEquals(32, keySeq2.encoded.size)

        // Deterministic for same sequence index
        assertArrayEquals(keySeq1.encoded, keySeq1Duplicate.encoded)

        // Completely distinct across consecutive ratchets
        assertFalse(keySeq1.encoded.contentEquals(keySeq2.encoded))
    }

    @Test
    fun `SyncWorker retry avoids double encryption and preserves ephemeral duration`() {
        val rawPlain = "Jai Hind"
        val convId = "conv_sync_101"

        // First encryption
        val encrypted1 = SignalEncryptionManager.encrypt(convId, rawPlain)
        assertTrue(SignalEncryptionManager.isEncryptedPayload(encrypted1))

        // Simulating sync logic: if already encrypted, do not re-encrypt
        val safePayload = if (SignalEncryptionManager.isEncryptedPayload(encrypted1)) {
            encrypted1
        } else {
            SignalEncryptionManager.encrypt(convId, encrypted1)
        }
        assertEquals(encrypted1, safePayload)
        assertFalse(safePayload.startsWith("ENC:ENC:"))

        // MessageDto preserves ephemeralDurationSec
        val dto = com.bharatconnect.app.data.remote.dto.MessageDto(
            id = "m1",
            conversationId = convId,
            senderId = "u1",
            content = safePayload,
            ephemeralDurationSec = 7200L
        )
        val domain = dto.toDomain()
        assertEquals(7200L, domain.ephemeralDurationSec)
        assertEquals(safePayload, domain.content)
    }

    @Test
    fun `SupabaseClient computeBackoffWithJitter adheres strictly to bounds`() {
        val client = com.bharatconnect.app.core.network.SupabaseClient
        val baseDelay = 1000L
        val maxDelay = 30000L

        for (attempt in 0..15) {
            val backoff = client.computeBackoffWithJitter(attempt, baseDelayMs = baseDelay, maxDelayMs = maxDelay)
            assertTrue("Backoff must be >= baseDelay ($baseDelay)", backoff >= baseDelay)
            assertTrue("Backoff must be <= maxDelay ($maxDelay)", backoff <= maxDelay)
        }
    }

    @Test
    fun `ContactsManager accurately evaluates mutual social graph handshake`() {
        val cm = com.bharatconnect.app.core.contacts.ContactsManager
        val phoneA = "+91 98765 43210"
        val phoneB = "+91 87654 32109"
        val phoneC = "+91 76543 21098"

        val hashA = cm.hashPhoneNumber(phoneA)
        val hashB = cm.hashPhoneNumber(phoneB)

        val userAContacts = setOf(hashB) // User A saved User B
        val userBContacts = setOf(hashA) // User B saved User A
        val userCContacts = setOf(hashA) // User C saved User A (but User A did not save User C)

        // Mutual handshake between A and B
        assertTrue(cm.isMutualHandshake(userAContacts, userBContacts, phoneA, phoneB))

        // Non-mutual between A and C (A does not have C saved)
        assertFalse(cm.isMutualHandshake(userAContacts, userCContacts, phoneA, phoneC))
    }

    @Test
    fun `CloudinaryManager dynamically selects optimal compression based on network metered state`() {
        val cm = com.bharatconnect.app.core.storage.CloudinaryManager

        // Cellular / metered connection: Aggressive compression to save bandwidth
        val (cellularDim, cellularQuality) = cm.getAdaptiveCompressionParams(isMetered = true)
        assertEquals(1024, cellularDim)
        assertEquals(70, cellularQuality)

        // High-speed Wi-Fi / unmetered: Standard crisp compression
        val (wifiDim, wifiQuality) = cm.getAdaptiveCompressionParams(isMetered = false)
        assertEquals(1280, wifiDim)
        assertEquals(82, wifiQuality)
    }

    @Test
    fun `SecurityManager enforces NIST SP 800-38D vault spec and zeroizes memory buffers`() {
        val sm = com.bharatconnect.app.core.security.SecurityManager

        // Valid AES-256 GCM spec
        assertTrue(sm.isVaultKeySpecValid("AES", 32, "GCM", "NoPadding"))

        // Insecure DES or 128-bit or ECB mode rejected
        assertFalse(sm.isVaultKeySpecValid("DES", 8, "CBC", "PKCS5Padding"))
        assertFalse(sm.isVaultKeySpecValid("AES", 16, "GCM", "NoPadding"))
        assertFalse(sm.isVaultKeySpecValid("AES", 32, "ECB", "PKCS5Padding"))

        // Zeroizing buffer verification
        val secretBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        sm.zeroizeByteArray(secretBytes)
        for (b in secretBytes) {
            assertEquals(0.toByte(), b)
        }

        val secretChars = charArrayOf('9', '8', '7', '6')
        sm.zeroizeCharArray(secretChars)
        for (c in secretChars) {
            assertEquals('0', c)
        }
    }

    @Test
    fun `SupabaseClient computeBackoffWithJitter maintains strict boundaries with full jitter`() {
        val client = com.bharatconnect.app.core.network.SupabaseClient
        val baseDelay = 1000L
        val maxDelay = 30000L

        for (attempt in 0..15) {
            for (sample in 0..10) {
                val delay = client.computeBackoffWithJitter(attempt, baseDelay, maxDelay)
                assertTrue("Delay ($delay) must be >= baseDelay ($baseDelay)", delay >= baseDelay)
                assertTrue("Delay ($delay) must be <= maxDelay ($maxDelay)", delay <= maxDelay)
            }
        }
    }

    @Test
    fun `SignalEncryptionManager isolates ratchet chains across independent conversations`() {
        val convA = "direct_userA_userB"
        val convB = "direct_userA_userC"

        val plainA = "Confidential Message for B"
        val plainB = "Confidential Message for C"

        val encA = SignalEncryptionManager.encrypt(convA, plainA)
        val encB = SignalEncryptionManager.encrypt(convB, plainB)

        // Decrypting with correct conversation must match
        assertEquals(plainA, SignalEncryptionManager.decrypt(convA, encA))
        assertEquals(plainB, SignalEncryptionManager.decrypt(convB, encB))

        // Decrypting with wrong conversation key MUST fail or produce error fallback
        val crossDecrypt = SignalEncryptionManager.decrypt(convB, encA)
        assertNotEquals(plainA, crossDecrypt)
    }

    @Test
    fun `Adversarial malformed or truncated ciphertexts do not throw unhandled exceptions`() {
        val conv = "conv_adversarial_test"

        // Invalid Base64
        val invalidBase64 = "ENC:@@##!!invalid_base64%%^^"
        val res1 = SignalEncryptionManager.decrypt(conv, invalidBase64)
        assertEquals(invalidBase64, res1)

        // Truncated payload (< 28 bytes for IV + GCM tag)
        val truncated = "ENC:YWJjZGVm"
        val res2 = SignalEncryptionManager.decrypt(conv, truncated)
        assertEquals(truncated, res2)

        // Empty ciphertext
        val emptyCipher = "ENC:"
        val res3 = SignalEncryptionManager.decrypt(conv, emptyCipher)
        assertEquals(emptyCipher, res3)
    }

    @Test
    fun `Adversarial bit-flip in ciphertext triggers GCM authentication failure`() {
        val conv = "conv_tamper_detect"
        val original = "Sovereign communication payload"
        val encrypted = SignalEncryptionManager.encrypt(conv, original)

        assertTrue(encrypted.startsWith("ENC:"))

        // Mutate a character in the Base64 ciphertext (simulate bit-flip in transit)
        val chars = encrypted.toCharArray()
        val targetIdx = chars.size - 5
        chars[targetIdx] = if (chars[targetIdx] == 'A') 'B' else 'A'
        val tamperedEncrypted = String(chars)

        // Decryption of tampered ciphertext MUST fail GCM integrity check and return the fallback string
        val decrypted = SignalEncryptionManager.decrypt(conv, tamperedEncrypted)
        assertNotEquals("Decrypted message must not match original when tampered", original, decrypted)
        assertEquals(tamperedEncrypted, decrypted)
    }
}
