package com.bharatconnect.app.core.session

import android.content.Context
import android.content.SharedPreferences
import com.bharatconnect.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private const val PREFS_NAME = "bharatconnect_session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE_NUMBER = "phone_number"
    private const val KEY_DOB = "dob"
    private const val KEY_AVATAR_URL = "avatar_url"
    private const val KEY_BIO = "bio"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_REMEMBER_CREDENTIALS = "remember_credentials"
    private const val KEY_SAVED_IDENTIFIER = "saved_identifier"

    private lateinit var prefs: SharedPreferences

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isSessionActive.value = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !prefs.getString(KEY_USER_ID, null).isNullOrBlank()
    }

    fun hasActiveSession(): Boolean {
        if (!::prefs.isInitialized) return false
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        return isLoggedIn && !userId.isNullOrBlank()
    }

    fun getCachedUserProfile(): UserProfile? {
        if (!hasActiveSession()) return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, "User") ?: "User"
        val fullName = prefs.getString(KEY_FULL_NAME, username) ?: username
        val email = prefs.getString(KEY_EMAIL, null)
        val phoneNumber = prefs.getString(KEY_PHONE_NUMBER, null)
        val dob = prefs.getString(KEY_DOB, null)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        val bio = prefs.getString(KEY_BIO, null)

        return UserProfile(
            id = userId,
            email = email,
            username = username,
            fullName = fullName,
            avatarUrl = avatarUrl,
            bio = bio,
            phoneNumber = phoneNumber,
            dob = dob,
            isOnline = true
        )
    }

    fun saveSession(
        user: UserProfile,
        accessToken: String? = null,
        refreshToken: String? = null
    ) {
        if (!::prefs.isInitialized) return
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_PHONE_NUMBER, user.phoneNumber)
            putString(KEY_DOB, user.dob)
            putString(KEY_AVATAR_URL, user.avatarUrl)
            putString(KEY_BIO, user.bio)
            if (accessToken != null) putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        _isSessionActive.value = true
    }

    fun updateCachedProfile(user: UserProfile) {
        if (!::prefs.isInitialized) return
        prefs.edit().apply {
            putString(KEY_EMAIL, user.email)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_PHONE_NUMBER, user.phoneNumber)
            putString(KEY_DOB, user.dob)
            putString(KEY_AVATAR_URL, user.avatarUrl)
            putString(KEY_BIO, user.bio)
            apply()
        }
    }

    fun getAuthTokens(): Pair<String?, String?> {
        if (!::prefs.isInitialized) return Pair(null, null)
        val access = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        return Pair(access, refresh)
    }

    fun setRememberCredentials(enabled: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(KEY_REMEMBER_CREDENTIALS, enabled).apply()
    }

    fun isRememberCredentialsEnabled(): Boolean {
        if (!::prefs.isInitialized) return true
        return prefs.getBoolean(KEY_REMEMBER_CREDENTIALS, true)
    }

    fun saveRememberedIdentifier(identifier: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_SAVED_IDENTIFIER, identifier.trim()).apply()
    }

    fun getRememberedIdentifier(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_SAVED_IDENTIFIER, "") ?: ""
    }

    fun clearSession() {
        if (!::prefs.isInitialized) return
        val savedIdentifier = getRememberedIdentifier()
        val rememberCreds = isRememberCredentialsEnabled()

        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_USERNAME)
            remove(KEY_FULL_NAME)
            remove(KEY_PHONE_NUMBER)
            remove(KEY_DOB)
            remove(KEY_AVATAR_URL)
            remove(KEY_BIO)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            if (rememberCreds) {
                putString(KEY_SAVED_IDENTIFIER, savedIdentifier)
                putBoolean(KEY_REMEMBER_CREDENTIALS, true)
            } else {
                remove(KEY_SAVED_IDENTIFIER)
                remove(KEY_REMEMBER_CREDENTIALS)
            }
            apply()
        }
        _isSessionActive.value = false
    }
}
