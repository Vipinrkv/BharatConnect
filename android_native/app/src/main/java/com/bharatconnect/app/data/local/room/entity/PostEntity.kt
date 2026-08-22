package com.bharatconnect.app.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bharatconnect.app.domain.model.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: String
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorId = authorId,
            authorName = authorName,
            authorAvatar = authorAvatar,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            likesCount = likesCount,
            commentsCount = commentsCount,
            isLikedByMe = isLikedByMe,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(post: Post): PostEntity {
            return PostEntity(
                id = post.id,
                authorId = post.authorId,
                authorName = post.authorName,
                authorAvatar = post.authorAvatar,
                content = post.content,
                mediaUrl = post.mediaUrl,
                mediaType = post.mediaType,
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                isLikedByMe = post.isLikedByMe,
                createdAt = post.createdAt
            )
        }
    }
}
