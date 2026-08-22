package com.bharatconnect.app.domain.repository

import com.bharatconnect.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPostsFlow(): Flow<List<Post>>
    suspend fun fetchPosts(): Result<List<Post>>
    suspend fun createPost(content: String, mediaUrl: String? = null, mediaType: String? = null): Result<Post>
    suspend fun toggleLike(postId: String): Result<Boolean>
}
