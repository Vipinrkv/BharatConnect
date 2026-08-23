package com.bharatconnect.app.domain.repository

import com.bharatconnect.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<UserProfile?>
    
    suspend fun login(identifier: String, password: String): Result<UserProfile>
    suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null
    ): Result<UserProfile>
    suspend fun verifyEmailOtp(email: String, token: String): Result<UserProfile>
    suspend fun resendEmailOtp(email: String): Result<Unit>
    suspend fun updateProfile(
        fullName: String,
        bio: String? = null,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null
    ): Result<UserProfile>
    suspend fun resetPassword(emailOrIdentifier: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): UserProfile?
    fun isUserLoggedIn(): Boolean
}
