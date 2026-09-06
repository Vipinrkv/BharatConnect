package com.bharatconnect.app.data.repository

import com.bharatconnect.app.core.database.DatabaseProvider
import com.bharatconnect.app.core.network.SupabaseClient
import com.bharatconnect.app.data.local.room.entity.PostEntity
import com.bharatconnect.app.data.remote.dto.PostDto
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.repository.FeedRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FeedRepositoryImpl : FeedRepository {

    private val db = DatabaseProvider.getDatabase()
    private val postDao = db.postDao()
    private val supabase = SupabaseClient.client

    override fun getPostsFlow(): Flow<List<Post>> {
        return postDao.getAllPostsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun fetchPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val remotePosts = supabase.postgrest["posts"]
                .select()
                .decodeList<PostDto>()

            val entities = remotePosts.map { PostEntity.fromDomain(it.toDomain()) }
            postDao.insertPosts(entities)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            // Offline fallback to Room
            val local = postDao.getAllPosts().map { it.toDomain() }
            Result.success(local)
        }
    }

    override suspend fun createPost(
        content: String,
        mediaUrl: String?,
        mediaType: String?
    ): Result<Post> = withContext(Dispatchers.IO) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id ?: "local_user"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val postId = UUID.randomUUID().toString()

        val post = Post(
            id = postId,
            authorId = currentUserId,
            authorName = "You",
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            likesCount = 0,
            commentsCount = 0,
            isLikedByMe = false,
            createdAt = timestamp
        )

        // Insert locally for immediate UI response
        postDao.insertOrUpdatePost(PostEntity.fromDomain(post))

        // Remote sync with idempotent postId
        try {
            val postDto = PostDto(
                id = postId,
                authorId = currentUserId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                createdAt = timestamp
            )
            val inserted = supabase.postgrest["posts"]
                .upsert(postDto) {
                    select()
                }
                .decodeSingle<PostDto>()

            val finalPost = inserted.toDomain()
            postDao.insertOrUpdatePost(PostEntity.fromDomain(finalPost))

            Result.success(finalPost)
        } catch (e: Exception) {
            Result.success(post)
        }
    }

    override suspend fun toggleLike(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
            // Optimistic local update
            postDao.toggleLike(postId, true)

            // Remote sync if user is authenticated
            if (currentUserId != null) {
                try {
                    @kotlinx.serialization.Serializable
                    data class PostLikePayload(
                        val post_id: String,
                        val user_id: String
                    )
                    supabase.postgrest["post_likes"].upsert(
                        PostLikePayload(post_id = postId, user_id = currentUserId)
                    )
                } catch (_: Exception) {
                    // Fallback delete if like was removed
                    try {
                        supabase.postgrest["post_likes"].delete {
                            filter {
                                eq("post_id", postId)
                                eq("user_id", currentUserId)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
