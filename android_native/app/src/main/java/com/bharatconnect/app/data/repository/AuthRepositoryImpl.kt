package com.bharatconnect.app.data.repository

import android.net.Uri
import com.bharatconnect.app.core.network.NetworkErrorSanitizer
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.data.remote.dto.ProfileDto
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepositoryImpl : AuthRepository {

    private val supabase = SupabaseClient.client
    private val _currentUserFlow = MutableStateFlow<UserProfile?>(null)
    override val currentUserFlow: Flow<UserProfile?> = _currentUserFlow.asStateFlow()

    override suspend fun login(identifier: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmed = identifier.trim()
            val emailToUse = if (trimmed.contains("@")) {
                trimmed
            } else {
                // Lookup in profiles table by username or phone_number
                val profile = try {
                    supabase.postgrest["profiles"].select {
                        filter {
                            or {
                                eq("username", trimmed)
                                eq("phone_number", trimmed)
                            }
                        }
                    }.decodeSingleOrNull<ProfileDto>()
                } catch (e: Exception) {
                    null
                }

                profile?.email ?: return@withContext Result.failure(
                    Exception("No registered account found for '$trimmed'. Please sign in with your email or check your username/mobile number.")
                )
            }

            supabase.auth.signInWith(Email) {
                this.email = emailToUse
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Authentication succeeded but user session is unavailable"))

            val userProfile = fetchOrCreateProfile(user.id, user.email, null, null, null, null, null)
            _currentUserFlow.value = userProfile
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String?,
        dob: String?,
        avatarUrl: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedUsername = username.trim()
            val trimmedFullName = fullName.trim()
            val trimmedPhone = phoneNumber?.trim()
            val trimmedDob = dob?.trim()
            val trimmedAvatar = avatarUrl?.trim()

            supabase.auth.signUpWith(Email, redirectUrl = SupabaseClient.AUTH_REDIRECT_URL) {
                this.email = trimmedEmail
                this.password = password
                this.data = buildJsonObject {
                    put("username", trimmedUsername)
                    put("full_name", trimmedFullName)
                    if (!trimmedPhone.isNullOrBlank()) put("phone_number", trimmedPhone)
                    if (!trimmedDob.isNullOrBlank()) put("dob", trimmedDob)
                    if (!trimmedAvatar.isNullOrBlank()) put("avatar_url", trimmedAvatar)
                }
            }

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val userProfile = fetchOrCreateProfile(
                    userId = user.id,
                    email = user.email ?: trimmedEmail,
                    username = trimmedUsername,
                    fullName = trimmedFullName,
                    phoneNumber = trimmedPhone,
                    dob = trimmedDob,
                    avatarUrl = trimmedAvatar
                )
                _currentUserFlow.value = userProfile
                Result.success(userProfile)
            } else {
                // User account created in Supabase Auth; email verification required
                val pendingProfile = UserProfile(
                    id = "",
                    email = trimmedEmail,
                    username = trimmedUsername,
                    fullName = trimmedFullName,
                    phoneNumber = trimmedPhone,
                    dob = trimmedDob,
                    avatarUrl = trimmedAvatar
                )
                Result.success(pendingProfile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun handleAuthCallback(uri: Uri): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            // Check for error parameters in query or fragment
            val errorDescription = uri.getQueryParameter("error_description")
                ?: uri.getQueryParameter("error")
                ?: extractParam(uri.fragment, "error_description")
                ?: extractParam(uri.fragment, "error")

            if (!errorDescription.isNullOrBlank()) {
                return@withContext Result.failure(Exception(NetworkErrorSanitizer.sanitize(Exception(errorDescription))))
            }

            // Check for PKCE authorization code in query or fragment
            val code = uri.getQueryParameter("code") ?: extractParam(uri.fragment, "code")
            if (!code.isNullOrBlank()) {
                try {
                    supabase.auth.exchangeCodeForSession(code)
                } catch (_: Exception) {}
            }

            // Check for implicit access_token and refresh_token in fragment or query
            val accessToken = extractParam(uri.fragment, "access_token") ?: uri.getQueryParameter("access_token")
            val refreshToken = extractParam(uri.fragment, "refresh_token") ?: uri.getQueryParameter("refresh_token")
            if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
                try {
                    supabase.auth.importAuthToken(accessToken = accessToken, refreshToken = refreshToken)
                } catch (_: Exception) {}
            }

            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Could not establish user session from verification link"))

            val userProfile = fetchOrCreateProfile(
                userId = user.id,
                email = user.email,
                username = null,
                fullName = null
            )
            _currentUserFlow.value = userProfile
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmedEmail = email.trim()
            val trimmedToken = token.trim()

            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = trimmedEmail,
                token = trimmedToken
            )

            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Verification succeeded but user session is unavailable"))

            val userProfile = fetchOrCreateProfile(
                userId = user.id,
                email = user.email ?: trimmedEmail,
                username = null,
                fullName = null
            )
            _currentUserFlow.value = userProfile
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun resendEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.resendEmail(
                type = OtpType.Email.EMAIL,
                email = email.trim()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun updateProfile(
        fullName: String,
        bio: String?,
        phoneNumber: String?,
        dob: String?,
        avatarUrl: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val existing = supabase.postgrest["profiles"].select {
                filter { eq("id", user.id) }
            }.decodeSingleOrNull<ProfileDto>()

            val updated = (existing ?: ProfileDto(id = user.id)).copy(
                fullName = fullName.trim(),
                bio = bio?.trim(),
                phoneNumber = phoneNumber?.trim() ?: existing?.phoneNumber,
                dob = dob?.trim() ?: existing?.dob,
                avatarUrl = avatarUrl?.trim() ?: existing?.avatarUrl
            )

            supabase.postgrest["profiles"].upsert(updated)

            val domainProfile = updated.toDomain(user.email)
            _currentUserFlow.value = domainProfile
            Result.success(domainProfile)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun resetPassword(emailOrIdentifier: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmed = emailOrIdentifier.trim()
            val emailToUse = if (trimmed.contains("@")) {
                trimmed
            } else {
                val profile = try {
                    supabase.postgrest["profiles"].select {
                        filter {
                            or {
                                eq("username", trimmed)
                                eq("phone_number", trimmed)
                            }
                        }
                    }.decodeSingleOrNull<ProfileDto>()
                } catch (e: Exception) {
                    null
                }
                profile?.email ?: return@withContext Result.failure(
                    Exception("No account email found for '$trimmed'. Please enter your registered email address.")
                )
            }

            supabase.auth.resetPasswordForEmail(emailToUse)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signOut()
            _currentUserFlow.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorSanitizer.sanitize(e)))
        }
    }

    override suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext null
            val profile = fetchOrCreateProfile(user.id, user.email, null, null, null, null, null)
            _currentUserFlow.value = profile
            profile
        } catch (e: Exception) {
            null
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    private fun extractParam(rawString: String?, key: String): String? {
        if (rawString.isNullOrBlank()) return null
        val clean = rawString.trimStart('#', '?')
        return clean.split("&")
            .map { it.split("=") }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
            ?.let { Uri.decode(it) }
    }

    private fun extractMetadata(user: UserInfo?, key: String): String? {
        if (user == null) return null
        return try {
            user.userMetadata?.get(key)?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchOrCreateProfile(
        userId: String,
        email: String?,
        username: String?,
        fullName: String?,
        phoneNumber: String? = null,
        dob: String? = null,
        avatarUrl: String? = null
    ): UserProfile {
        val user = supabase.auth.currentUserOrNull()
        val metaUsername = extractMetadata(user, "username")
        val metaFullName = extractMetadata(user, "full_name")
        val metaPhone = extractMetadata(user, "phone_number")
        val metaDob = extractMetadata(user, "dob")
        val metaAvatar = extractMetadata(user, "avatar_url")

        val finalUsername = username ?: metaUsername ?: email?.substringBefore("@") ?: "user_${userId.take(6)}"
        val finalFullName = fullName ?: metaFullName ?: finalUsername
        val finalPhone = phoneNumber ?: metaPhone
        val finalDob = dob ?: metaDob
        val finalAvatar = avatarUrl ?: metaAvatar

        return try {
            val existing = supabase.postgrest["profiles"].select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<ProfileDto>()

            if (existing != null) {
                val updated = existing.copy(
                    email = existing.email ?: email,
                    username = existing.username ?: finalUsername,
                    fullName = existing.fullName ?: finalFullName,
                    phoneNumber = existing.phoneNumber ?: finalPhone,
                    dob = existing.dob ?: finalDob,
                    avatarUrl = existing.avatarUrl ?: finalAvatar
                )
                try {
                    supabase.postgrest["profiles"].update(updated) {
                        filter { eq("id", userId) }
                    }
                } catch (_: Exception) {}
                updated.toDomain(email)
            } else {
                val newProfile = ProfileDto(
                    id = userId,
                    email = email,
                    username = finalUsername,
                    fullName = finalFullName,
                    phoneNumber = finalPhone,
                    dob = finalDob,
                    avatarUrl = finalAvatar
                )
                try {
                    supabase.postgrest["profiles"].insert(newProfile)
                } catch (_: Exception) {}
                newProfile.toDomain(email)
            }
        } catch (e: Exception) {
            UserProfile(
                id = userId,
                email = email,
                username = finalUsername,
                fullName = finalFullName,
                phoneNumber = finalPhone,
                dob = finalDob,
                avatarUrl = finalAvatar
            )
        }
    }
}
