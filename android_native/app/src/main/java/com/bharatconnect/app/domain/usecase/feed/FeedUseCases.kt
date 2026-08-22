package com.bharatconnect.app.domain.usecase.feed

import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

class GetFeedPostsUseCase(private val feedRepository: FeedRepository) {
    operator fun invoke(): Flow<List<Post>> {
        return feedRepository.getPostsFlow()
    }
}

class FetchFeedPostsUseCase(private val feedRepository: FeedRepository) {
    suspend operator fun invoke(): Result<List<Post>> {
        return feedRepository.fetchPosts()
    }
}

class CreatePostUseCase(private val feedRepository: FeedRepository) {
    suspend operator fun invoke(
        content: String,
        mediaUrl: String? = null,
        mediaType: String? = null
    ): Result<Post> {
        return feedRepository.createPost(content, mediaUrl, mediaType)
    }
}

class TogglePostLikeUseCase(private val feedRepository: FeedRepository) {
    suspend operator fun invoke(postId: String): Result<Boolean> {
        return feedRepository.toggleLike(postId)
    }
}
