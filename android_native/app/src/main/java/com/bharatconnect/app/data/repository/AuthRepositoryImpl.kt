package com.bharatconnect.app.data.repository

import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.data.remote.dto.ProfileDto
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl : AuthRepository {

    private val supabase = SupabaseClient.client
    private val _currentUserFlow = MutableStateFlow<UserProfile?>(null)
    override val currentUserFlow: Flow<UserProfile?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Authentication succeeded but user is null"))

            val userProfile = fetchOrCreateProfile(user.id, user.email, null, null)
            _currentUserFlow.value = userProfile
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Registration succeeded. Please verify your email if required."))

            val userProfile = fetchOrCreateProfile(user.id, user.email, username.trim(), fullName.trim())
            _currentUserFlow.value = userProfile
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signOut()
            _currentUserFlow.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext null
            val profile = fetchOrCreateProfile(user.id, user.email, null, null)
            _currentUserFlow.value = profile
            profile
        } catch (e: Exception) {
            null
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    private suspend fun fetchOrCreateProfile(
        userId: String,
        email: String?,
        username: String?,
        fullName: String?
    ): UserProfile {
        return try {
            val existing = supabase.postgrest["profiles"].select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<ProfileDto>()

            if (existing != null) {
                existing.toDomain(email)
            } else {
                val newProfile = ProfileDto(
                    id = userId,
                    username = username ?: email?.substringBefore("@") ?: "user_${userId.take(6)}",
                    fullName = fullName ?: username ?: "BharatConnect User"
                )
                try {
                    supabase.postgrest["profiles"].insert(newProfile)
                } catch (_: Exception) {}
                newProfile.toDomain(email)
            }
        } catch (e: Exception) {
            // Fallback profile from auth metadata
            UserProfile(
                id = userId,
                email = email,
                username = username ?: email?.substringBefore("@") ?: "user",
                fullName = fullName ?: "BharatConnect User"
            )
        }
    }
}
