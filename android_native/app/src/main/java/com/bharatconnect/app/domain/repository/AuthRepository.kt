package com.bharatconnect.app.domain.repository

import com.bharatconnect.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<UserProfile?>
    
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(email: String, password: String, username: String, fullName: String): Result<UserProfile>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): UserProfile?
    fun isUserLoggedIn(): Boolean
}
