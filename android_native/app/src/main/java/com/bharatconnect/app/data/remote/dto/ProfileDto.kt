package com.bharatconnect.app.data.remote.dto

import com.bharatconnect.app.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean? = false,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(email: String? = null): UserProfile {
        return UserProfile(
            id = id,
            email = email,
            username = username ?: "",
            fullName = fullName ?: username ?: "BharatConnect User",
            avatarUrl = avatarUrl,
            bio = bio,
            phoneNumber = phoneNumber,
            isOnline = isOnline ?: false,
            createdAt = createdAt
        )
    }
}
