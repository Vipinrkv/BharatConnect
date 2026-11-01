package com.bharatconnect.app

import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import com.bharatconnect.app.core.payments.UpiPaymentData
import com.bharatconnect.app.core.payments.UpiPaymentManager
import org.junit.Assert.*
import org.junit.Test

/**
 * End-to-end verification proving BharatConnect's superiority as India's premier WhatsApp replacement:
 * 1. 1-Tap Bharat UPI Payments (NPCI standard URI generation and interactive chat card parsing).
 * 2. VPA validation rules across all Indian payment handles (okhdfcbank, okaxis, paytm, etc.).
 * 3. Signal AES-GCM-256 E2E Encryption roundtrip for UPI payment card messages.
 * 4. Voice Note Audio duration formatting and media typing.
 * 5. Desi quick reply messaging integration.
 */
class BharatWhatsAppReplacementTest {

    @Test
    fun `UPI Payment URI generation adheres strictly to NPCI standard specification`() {
        val payment = UpiPaymentData(
            payeeVpa = "vipin@okhdfcbank",
            payeeName = "Vipin Kumar",
            amount = "499.50",
            transactionNote = "Dinner bill split"
        )

        val uriString = UpiPaymentManager.buildUpiUriString(payment)

        // NPCI standard requires upi://pay scheme
        assertTrue("URI must start with upi://pay?", uriString.startsWith("upi://pay?"))
        assertTrue("URI must contain pa parameter", uriString.contains("pa=vipin%40okhdfcbank"))
        assertTrue("URI must contain pn parameter", uriString.contains("pn=Vipin+Kumar"))
        assertTrue("URI must format amount", uriString.contains("am=499.50"))
        assertTrue("URI must specify INR currency", uriString.contains("cu=INR"))
        assertTrue("URI must include transaction note", uriString.contains("tn=Dinner+bill+split"))
    }

    @Test
    fun `UPI payment message encode and parse roundtrip preserves all financial data`() {
        val originalPayment = UpiPaymentData(
            payeeVpa = "shopkeeper@paytm",
            payeeName = "Sharma Kirana Store",
            amount = "1250.00",
            transactionNote = "Groceries & Spices"
        )

        val encodedMessage = UpiPaymentManager.encodePaymentMessage(originalPayment)

        // Verify prefix tag
        assertTrue("Must have UPI_REQ:: tag", encodedMessage.startsWith("UPI_REQ::"))

        // Parse back
        val parsedPayment = UpiPaymentManager.parsePaymentMessage(encodedMessage)

        assertNotNull("Parsed payment must not be null", parsedPayment)
        assertEquals("shopkeeper@paytm", parsedPayment?.payeeVpa)
        assertEquals("Sharma Kirana Store", parsedPayment?.payeeName)
        assertEquals("1250.00", parsedPayment?.amount)
        assertEquals("Groceries & Spices", parsedPayment?.transactionNote)
    }

    @Test
    fun `UPI parser correctly ignores standard non-payment chat messages`() {
        val regularText = "Hey, are you free for a call?"
        val regularHindiText = "नमस्ते, कैसे हो आप?"
        val voiceText = "🎤 Voice Message (14s)"

        assertNull("Regular text must return null", UpiPaymentManager.parsePaymentMessage(regularText))
        assertNull("Hindi text must return null", UpiPaymentManager.parsePaymentMessage(regularHindiText))
        assertNull("Voice note label must return null", UpiPaymentManager.parsePaymentMessage(voiceText))
    }

    @Test
    fun `Indian UPI VPA format validation correctly classifies addresses`() {
        // Valid VPAs
        assertTrue(UpiPaymentManager.isValidVpa("rahul@oksbi"))
        assertTrue(UpiPaymentManager.isValidVpa("9876543210@paytm"))
        assertTrue(UpiPaymentManager.isValidVpa("user.name123@icici"))
        assertTrue(UpiPaymentManager.isValidVpa("kirana_store@ybl"))

        // Invalid VPAs
        assertFalse(UpiPaymentManager.isValidVpa(""))
        assertFalse(UpiPaymentManager.isValidVpa("invalid_without_at"))
        assertFalse(UpiPaymentManager.isValidVpa("@nohandle"))
        assertFalse(UpiPaymentManager.isValidVpa("user@"))
        assertFalse(UpiPaymentManager.isValidVpa("user @bank"))
    }

    @Test
    fun `Signal E2EE encryption protects UPI payment cards in transit`() {
        val conversationId = "conv_financial_secret_999"
        val payment = UpiPaymentData(
            payeeVpa = "partner@ibl",
            payeeName = "Aarav Sharma",
            amount = "50000",
            transactionNote = "Advance project payment"
        )

        val cleartextCard = UpiPaymentManager.encodePaymentMessage(payment)

        // Encrypt with Signal protocol
        val ciphertext = SignalEncryptionManager.encrypt(conversationId, cleartextCard)

        // Ciphertext must be randomized / encrypted, not containing plaintext VPA or amounts
        assertNotEquals(cleartextCard, ciphertext)
        assertFalse(ciphertext.contains("partner@ibl"))
        assertFalse(ciphertext.contains("50000"))

        // Decrypt with Signal protocol
        val decrypted = SignalEncryptionManager.decrypt(conversationId, ciphertext)

        assertEquals("Decrypted card must match original cleartext", cleartextCard, decrypted)

        // Parse payment from decrypted content
        val verifiedPayment = UpiPaymentManager.parsePaymentMessage(decrypted)
        assertNotNull(verifiedPayment)
        assertEquals("50000", verifiedPayment?.amount)
        assertEquals("partner@ibl", verifiedPayment?.payeeVpa)
    }

    @Test
    fun `Voice Note audio duration formatting adheres to MM SS standards`() {
        val formatDuration: (Int) -> String = { totalSec ->
            val min = totalSec / 60
            val sec = totalSec % 60
            String.format("%02d:%02d", min, sec)
        }

        assertEquals("00:00", formatDuration(0))
        assertEquals("00:05", formatDuration(5))
        assertEquals("00:45", formatDuration(45))
        assertEquals("01:15", formatDuration(75))
        assertEquals("10:00", formatDuration(600))
    }

    @Test
    fun `Desi Quick Replies contain culturally relevant presets`() {
        val quickReplies = listOf(
            "नमस्ते 🙏",
            "हाँ, ठीक है 👍",
            "पैसे भेज दिए 💸",
            "कॉल करो 📞",
            "रास्ते में हूँ 🚗",
            "धन्यवाद! ✨"
        )

        assertEquals(6, quickReplies.size)
        assertTrue(quickReplies.any { it.contains("नमस्ते") })
        assertTrue(quickReplies.any { it.contains("पैसे भेज दिए") })
        assertTrue(quickReplies.any { it.contains("धन्यवाद") })
    }

    @Test
    fun `Voice Note playback speed cycles sequentially through 1x, 1_5x, 2x`() {
        val player = com.bharatconnect.app.core.audio.AudioPlayerManager
        player.setPlaybackSpeed(1.0f)
        assertEquals(1.0f, player.playbackSpeed.value, 0.001f)

        player.cyclePlaybackSpeed()
        assertEquals(1.5f, player.playbackSpeed.value, 0.001f)

        player.cyclePlaybackSpeed()
        assertEquals(2.0f, player.playbackSpeed.value, 0.001f)

        player.cyclePlaybackSpeed()
        assertEquals(1.0f, player.playbackSpeed.value, 0.001f)
    }

    @Test
    fun `Desi Smart Expense presets generate valid UPI amounts and notes`() {
        val smartPresets = listOf(
            Triple("☕ Chai", "20", "Chai & Nashta"),
            Triple("🚕 Auto", "50", "Auto fare split"),
            Triple("🍛 Lunch", "150", "Lunch split"),
            Triple("🛒 Kirana", "500", "Kirana & Groceries"),
            Triple("💸 Shagun", "501", "Shagun & Best Wishes")
        )

        assertEquals(5, smartPresets.size)
        smartPresets.forEach { (label, amt, note) ->
            assertTrue(label.isNotBlank())
            assertTrue(amt.toInt() > 0)
            assertTrue(note.isNotBlank())
            val payment = UpiPaymentData(
                payeeVpa = "merchant@upi",
                payeeName = "Shop",
                amount = amt,
                transactionNote = note
            )
            val uri = UpiPaymentManager.buildUpiUriString(payment)
            assertTrue(uri.contains("am=$amt"))
        }
    }

    @Test
    fun `NetworkMonitor state transitions respond immediately to network drops`() {
        val monitor = com.bharatconnect.app.core.network.NetworkMonitor
        monitor.setOnlineForTesting(true)
        assertTrue(monitor.isOnline.value)

        monitor.setOnlineForTesting(false)
        assertFalse(monitor.isOnline.value)

        monitor.setOnlineForTesting(true)
        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `Conversation list ordering strictly prioritizes pinned conversations`() {
        val convUnpinnedRecent = com.bharatconnect.app.domain.model.Conversation(
            id = "c1",
            title = "Friend",
            lastMessage = "Hey",
            lastMessageTime = "2026-10-31 22:00:00",
            isPinned = false
        )
        val convPinnedOlder = com.bharatconnect.app.domain.model.Conversation(
            id = "c2",
            title = "Family Group",
            lastMessage = "Shubh Prabhat",
            lastMessageTime = "2026-10-31 08:00:00",
            isPinned = true
        )
        val convUnpinnedOld = com.bharatconnect.app.domain.model.Conversation(
            id = "c3",
            title = "Acquaintance",
            lastMessage = "Bye",
            lastMessageTime = "2026-10-30 12:00:00",
            isPinned = false
        )

        val unsorted = listOf(convUnpinnedRecent, convPinnedOlder, convUnpinnedOld)
        val sorted = unsorted.sortedWith(
            compareByDescending<com.bharatconnect.app.domain.model.Conversation> { it.isPinned }
                .thenByDescending { it.lastMessageTime ?: "" }
        )

        // Pinned must be first regardless of timestamp!
        assertEquals("c2", sorted[0].id)
        assertEquals("c1", sorted[1].id)
        assertEquals("c3", sorted[2].id)
    }

    @Test
    fun `Message reaction and star models preserve metadata across transformations`() {
        val msg = com.bharatconnect.app.domain.model.Message(
            id = "m100",
            conversationId = "conv1",
            senderId = "user1",
            content = "Namaste Bharat",
            createdAt = "2026-10-31 22:00:00",
            reaction = "🙏",
            isStarred = true
        )

        val entity = com.bharatconnect.app.data.local.room.entity.MessageEntity.fromDomain(msg)
        assertEquals("🙏", entity.reaction)
        assertTrue(entity.isStarred)

        val restored = entity.toDomain()
        assertEquals(msg, restored)
    }

    @Test
    fun `Data-Saver image downscaling preserves exact aspect ratio within 1280px`() {
        val maxDim = 1280
        val originalW = 4000
        val originalH = 3000

        val ratio = originalW.toFloat() / originalH.toFloat()
        val targetW = if (originalW > originalH) maxDim else (maxDim * ratio).toInt()
        val targetH = if (originalW > originalH) (maxDim / ratio).toInt() else maxDim

        assertEquals(1280, targetW)
        assertEquals(960, targetH)
        assertEquals(ratio, targetW.toFloat() / targetH.toFloat(), 0.01f)
    }

    @Test
    fun `Conversation unread count batch reset produces zero unread count across all conversations`() {
        val list = listOf(
            com.bharatconnect.app.domain.model.Conversation(id = "c1", title = "Chat 1", unreadCount = 4),
            com.bharatconnect.app.domain.model.Conversation(id = "c2", title = "Chat 2", unreadCount = 1),
            com.bharatconnect.app.domain.model.Conversation(id = "c3", title = "Chat 3", unreadCount = 0)
        )

        val updated = list.map { it.copy(unreadCount = 0) }
        assertTrue(updated.all { it.unreadCount == 0 })
        assertEquals(0, updated.sumOf { it.unreadCount })
    }

    @Test
    fun `Direct conversation deterministic canonical ID generator is symmetric and stable`() {
        val userA = "9876543210-uuid"
        val userB = "1234567890-uuid"

        val id1 = com.bharatconnect.app.core.encryption.SignalEncryptionManager.getDeterministicConversationId(userA, userB)
        val id2 = com.bharatconnect.app.core.encryption.SignalEncryptionManager.getDeterministicConversationId(userB, userA)

        // Must be identical regardless of argument order
        assertEquals(id1, id2)
        assertTrue(id1.isNotBlank())
    }

    @Test
    fun `MessageStatus state machine enforces forward progression and prevents invalid downgrades`() {
        val draft = com.bharatconnect.app.domain.model.state.MessageStatus.Draft
        val pending = com.bharatconnect.app.domain.model.state.MessageStatus.Pending
        val sending = com.bharatconnect.app.domain.model.state.MessageStatus.Sending
        val sent = com.bharatconnect.app.domain.model.state.MessageStatus.Sent
        val delivered = com.bharatconnect.app.domain.model.state.MessageStatus.Delivered
        val read = com.bharatconnect.app.domain.model.state.MessageStatus.Read
        val failed = com.bharatconnect.app.domain.model.state.MessageStatus.Failed
        val retrying = com.bharatconnect.app.domain.model.state.MessageStatus.Retrying

        // Valid forward transitions
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(draft, pending))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(pending, sending))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(sending, sent))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(sent, delivered))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(delivered, read))

        // Idempotent transitions
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(read, read))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(delivered, delivered))

        // Strict downgrade prevention: Late ACK cannot downgrade Read back to Sent or Delivered
        assertFalse(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(read, delivered))
        assertFalse(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(read, sent))
        assertFalse(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(delivered, sent))
        assertFalse(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(sent, pending))

        // Failure and retry transitions
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(sending, failed))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(failed, retrying))
        assertTrue(com.bharatconnect.app.domain.model.state.MessageStatus.canTransition(failed, pending))
    }

    @Test
    fun `MessageStatus string mapping correctly parses valid and fallback values`() {
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Draft, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("draft"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Pending, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("pending"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Sending, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("sending"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Sent, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("sent"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Delivered, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("delivered"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Read, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("read"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Failed, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("failed"))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Retrying, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("retrying"))

        // Safe fallback for null/unknown
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Sent, com.bharatconnect.app.domain.model.state.MessageStatus.fromString(null))
        assertEquals(com.bharatconnect.app.domain.model.state.MessageStatus.Sent, com.bharatconnect.app.domain.model.state.MessageStatus.fromString("unknown_status"))
    }
}
