package com.bharatconnect.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bharatconnect.app.domain.model.UserProfile

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val createdAt: String? = null
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            id = id,
            username = username,
            fullName = fullName,
            avatarUrl = avatarUrl,
            bio = bio,
            phoneNumber = phoneNumber,
            isOnline = isOnline,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(user: UserProfile): UserEntity {
            return UserEntity(
                id = user.id,
                username = user.username,
                fullName = user.fullName,
                avatarUrl = user.avatarUrl,
                bio = user.bio,
                phoneNumber = user.phoneNumber,
                isOnline = user.isOnline,
                createdAt = user.createdAt
            )
        }
    }
}
