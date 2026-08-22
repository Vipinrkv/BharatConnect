package com.bharatconnect.app.domain

import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.repository.FeedRepository
import com.bharatconnect.app.domain.usecase.feed.CreatePostUseCase
import com.bharatconnect.app.domain.usecase.feed.GetFeedPostsUseCase
import com.bharatconnect.app.domain.usecase.feed.TogglePostLikeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FakeFeedRepository : FeedRepository {
    private val posts = mutableListOf<Post>()

    override fun getPostsFlow(): Flow<List<Post>> = flowOf(posts)

    override suspend fun fetchPosts(): Result<List<Post>> = Result.success(posts)

    override suspend fun createPost(content: String, mediaUrl: String?, mediaType: String?): Result<Post> {
        val post = Post(
            id = UUID.randomUUID().toString(),
            authorId = "author_1",
            authorName = "Aarav Sharma",
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            likesCount = 0,
            commentsCount = 0,
            isLikedByMe = false,
            createdAt = "Just now"
        )
        posts.add(post)
        return Result.success(post)
    }

    override suspend fun toggleLike(postId: String): Result<Boolean> {
        val index = posts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val p = posts[index]
            posts[index] = p.copy(
                isLikedByMe = !p.isLikedByMe,
                likesCount = if (p.isLikedByMe) p.likesCount - 1 else p.likesCount + 1
            )
            return Result.success(true)
        }
        return Result.failure(NoSuchElementException("Post not found"))
    }
}

class FeedUseCasesTest {

    private val fakeFeedRepository = FakeFeedRepository()
    private val getFeedPostsUseCase = GetFeedPostsUseCase(fakeFeedRepository)
    private val createPostUseCase = CreatePostUseCase(fakeFeedRepository)
    private val togglePostLikeUseCase = TogglePostLikeUseCase(fakeFeedRepository)

    @Test
    fun `createPostUseCase creates and stores post`() = runBlocking {
        val createResult = createPostUseCase("Celebrating Bharat's technology milestone! 🚀")
        assertTrue(createResult.isSuccess)

        val posts = getFeedPostsUseCase().first()
        assertEquals(1, posts.size)
        assertEquals("Celebrating Bharat's technology milestone! 🚀", posts[0].content)
        assertEquals(0, posts[0].likesCount)
    }

    @Test
    fun `togglePostLikeUseCase updates like status and count`() = runBlocking {
        val post = createPostUseCase("Like this post!").getOrThrow()
        val likeResult = togglePostLikeUseCase(post.id)
        assertTrue(likeResult.isSuccess)

        val updatedPosts = getFeedPostsUseCase().first()
        val updatedPost = updatedPosts.first { it.id == post.id }
        assertTrue(updatedPost.isLikedByMe)
        assertEquals(1, updatedPost.likesCount)
    }
}
