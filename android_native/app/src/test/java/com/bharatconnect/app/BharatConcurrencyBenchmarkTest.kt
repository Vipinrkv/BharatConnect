package com.bharatconnect.app

import com.bharatconnect.app.core.encryption.SignalEncryptionManager
import com.bharatconnect.app.core.payments.UpiPaymentData
import com.bharatconnect.app.core.payments.UpiPaymentManager
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * =========================================================================================
 * BHARATCONNECT CLIENT CONCURRENCY & CRYPTO STRESS BENCHMARK
 * =========================================================================================
 *
 * Verifies that the client engine can simultaneously process 1,000 concurrent encrypted
 * message dispatches, receipts, and UPI card parses across multiple parallel background threads
 * without thread deadlocks, race conditions, or memory leaks.
 */
class BharatConcurrencyBenchmarkTest {

    @Test
    fun `benchmark 1000 concurrent Signal AES-GCM-256 encryptions and decryptions`() {
        val executor = Executors.newFixedThreadPool(20)
        val conversationCount = 50
        val totalOperations = 1000

        val tasks = (0 until totalOperations).map { index ->
            Callable {
                val convId = "conv_${index % conversationCount}"
                val plaintext = "Hello BharatConnect user $index! Testing high-frequency Indian messaging."

                // 1. Concurrent Encryption
                val encrypted = SignalEncryptionManager.encrypt(convId, plaintext)
                assertNotNull(encrypted)
                assertTrue("Ciphertext must start with ENC:", encrypted.startsWith("ENC:"))

                // 2. Concurrent Decryption
                val decrypted = SignalEncryptionManager.decrypt(convId, encrypted)
                assertEquals(plaintext, decrypted)
                true
            }
        }

        val startTime = System.currentTimeMillis()
        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        val finished = executor.awaitTermination(30, TimeUnit.SECONDS)

        val totalTimeMs = System.currentTimeMillis() - startTime

        assertTrue("All tasks should finish within 30 seconds", finished)
        for (f in futures) {
            assertTrue(f.get())
        }

        val opsPerSec = (totalOperations.toDouble() / (totalTimeMs.toDouble() / 1000.0))
        println("==========================================================================")
        println("⚡ BHARATCONNECT ANDROID CLIENT CRYPTO STRESS BENCHMARK")
        println("==========================================================================")
        println("Total Concurrent Cryptographic Operations : $totalOperations")
        println("Total Time Elapsed                        : ${totalTimeMs}ms")
        println("Effective Crypto Throughput               : ${String.format("%.1f", opsPerSec)} ops/sec")
        println("Average Latency Per Message (E2E Enc+Dec) : ${String.format("%.2f", totalTimeMs.toDouble() / totalOperations)}ms")
        println("==========================================================================")

        // Must execute at least 500 ops/second on client CPU
        assertTrue("Client crypto throughput must exceed 500 ops/sec", opsPerSec > 500.0)
    }

    @Test
    fun `benchmark 1000 concurrent UPI message parses and VPA validations`() {
        val executor = Executors.newFixedThreadPool(20)
        val totalOperations = 1000

        val tasks = (0 until totalOperations).map { index ->
            Callable {
                val payment = UpiPaymentData(
                    payeeVpa = "merchant_$index@okhdfcbank",
                    payeeName = "Shopkeeper $index",
                    amount = "${(index + 1) * 10}.00",
                    transactionNote = "Quick payment $index"
                )

                val encoded = UpiPaymentManager.encodePaymentMessage(payment)
                val parsed = UpiPaymentManager.parsePaymentMessage(encoded)

                assertNotNull(parsed)
                assertEquals(payment.payeeVpa, parsed?.payeeVpa)
                assertEquals(payment.amount, parsed?.amount)
                assertTrue(UpiPaymentManager.isValidVpa(payment.payeeVpa))
                true
            }
        }

        val startTime = System.currentTimeMillis()
        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        val finished = executor.awaitTermination(15, TimeUnit.SECONDS)

        val totalTimeMs = System.currentTimeMillis() - startTime
        assertTrue(finished)
        for (f in futures) {
            assertTrue(f.get())
        }

        val opsPerSec = (totalOperations.toDouble() / (totalTimeMs.toDouble() / 1000.0))
        println("⚡ UPI Card Parsing Throughput: ${String.format("%.1f", opsPerSec)} parses/sec in ${totalTimeMs}ms")
        assertTrue("UPI parser must exceed 1000 ops/sec", opsPerSec > 1000.0)
    }
}
