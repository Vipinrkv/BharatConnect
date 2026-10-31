package com.bharatconnect.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * =========================================================================================
 * BHARATCONNECT SECURITY & BIOMETRIC APP LOCK MANAGER
 * =========================================================================================
 *
 * Provides client-side app locking with encrypted PIN verification and screen privacy guard.
 * Protects sensitive financial and confidential E2EE chat conversations from prying eyes.
 */
object SecurityManager {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isBiometricLockEnabled = MutableStateFlow(false)
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private var storedPinHash: String? = null

    fun setAppLock(enabled: Boolean, pin: String? = null) {
        _isBiometricLockEnabled.value = enabled
        if (enabled && !pin.isNullOrBlank()) {
            storedPinHash = hashPin(pin)
            _isAppLocked.value = true
        } else if (!enabled) {
            storedPinHash = null
            _isAppLocked.value = false
        }
    }

    fun lockApp() {
        if (_isBiometricLockEnabled.value && storedPinHash != null) {
            _isAppLocked.value = true
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        if (storedPinHash == null) {
            _isAppLocked.value = false
            return true
        }
        val incomingHash = hashPin(pin)
        val matches = MessageDigest.isEqual(
            storedPinHash!!.toByteArray(Charsets.UTF_8),
            incomingHash.toByteArray(Charsets.UTF_8)
        )
        if (matches) {
            _isAppLocked.value = false
        }
        return matches
    }

    fun unlockDirectlyForTesting() {
        _isAppLocked.value = false
    }

    private var lastActiveTimestamp: Long = System.currentTimeMillis()

    fun updateLastActive(now: Long = System.currentTimeMillis()) {
        lastActiveTimestamp = now
    }

    /**
     * Checks if the app was backgrounded longer than timeoutSec.
     */
    fun shouldAutoLock(currentTimeMillis: Long = System.currentTimeMillis(), timeoutSec: Long = 60L): Boolean {
        if (!_isBiometricLockEnabled.value || storedPinHash == null) return false
        val elapsedSec = (currentTimeMillis - lastActiveTimestamp) / 1000L
        return elapsedSec >= timeoutSec
    }

    /**
     * Lightweight sovereign environment check to detect rooted/tampered environments without third-party bloat.
     */
    fun checkEnvironmentIntegrity(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        for (path in suPaths) {
            if (java.io.File(path).exists()) {
                return false // Compromised environment detected
            }
        }
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return false // Custom ROM or insecure test keys
        }
        return true // Environment is clean
    }

    const val HARDWARE_KEY_ALIAS = "BharatConnect_Master_Vault_Key"

    /**
     * Verifies key specification validity for Android KeyStore AES-256 GCM vault encryption.
     * Ensures key meets NIST SP 800-38D and CERT-In / RBI security guidelines (256-bit AES, GCM, NoPadding).
     */
    fun isVaultKeySpecValid(algorithm: String, keySizeBytes: Int, mode: String, padding: String): Boolean {
        return algorithm.equals("AES", ignoreCase = true) &&
                keySizeBytes == 32 && // 256 bits
                mode.equals("GCM", ignoreCase = true) &&
                padding.equals("NoPadding", ignoreCase = true)
    }

    /**
     * Zeroizes in-memory credential byte arrays to prevent RAM inspection and memory scrapers.
     */
    fun zeroizeByteArray(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Zeroizes in-memory PIN character arrays.
     */
    fun zeroizeCharArray(array: CharArray) {
        array.fill('0')
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("BHARAT_PIN_SALT:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
