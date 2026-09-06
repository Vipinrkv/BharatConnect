package com.bharatconnect.app.core.encryption

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SignalEncryptionManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT = "BharatConnect_Sentinel_E2EE_2026"

    /**
     * Derives a deterministic 256-bit AES key from the conversation ID and salt.
     */
    private fun deriveKey(conversationId: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("$conversationId:$SALT".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext message into a Base64-encoded AES-GCM ciphertext with IV prefix.
     * Output format: [12-byte IV][Ciphertext + Tag] Base64 encoded with prefix "ENC:".
     */
    fun encrypt(conversationId: String, plainText: String): String {
        if (plainText.isBlank()) return plainText
        return try {
            val key = deriveKey(conversationId)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            "ENC:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            plainText
        }
    }

    /**
     * Decrypts ciphertext if it starts with "ENC:". Returns original text if unencrypted or on failure.
     */
    fun decrypt(conversationId: String, cipherText: String): String {
        if (!cipherText.startsWith("ENC:")) return cipherText
        return try {
            val rawBase64 = cipherText.removePrefix("ENC:")
            val combined = Base64.decode(rawBase64, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH_BYTE) return cipherText

            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE)

            val cipherBytes = ByteArray(combined.size - IV_LENGTH_BYTE)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherBytes, 0, cipherBytes.size)

            val key = deriveKey(conversationId)
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            cipherText
        }
    }
}
