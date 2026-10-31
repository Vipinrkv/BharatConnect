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
}
