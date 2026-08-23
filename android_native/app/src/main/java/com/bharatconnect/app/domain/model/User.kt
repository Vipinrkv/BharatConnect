package com.bharatconnect.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null,
    val dob: String? = null,
    val isOnline: Boolean = false,
    val createdAt: String? = null
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class AwaitingOtp(
        val email: String,
        val username: String = "",
        val fullName: String = "",
        val phoneNumber: String? = null,
        val dob: String? = null,
        val avatarUrl: String? = null
    ) : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}
