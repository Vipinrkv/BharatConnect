package com.bharatconnect.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.data.repository.FeedRepositoryImpl
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.repository.FeedRepository
import com.bharatconnect.app.domain.usecase.feed.CreatePostUseCase
import com.bharatconnect.app.domain.usecase.feed.FetchFeedPostsUseCase
import com.bharatconnect.app.domain.usecase.feed.GetFeedPostsUseCase
import com.bharatconnect.app.domain.usecase.feed.TogglePostLikeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    feedRepository: FeedRepository = FeedRepositoryImpl(),
    private val getFeedPostsUseCase: GetFeedPostsUseCase = GetFeedPostsUseCase(feedRepository),
    private val fetchFeedPostsUseCase: FetchFeedPostsUseCase = FetchFeedPostsUseCase(feedRepository),
    private val createPostUseCase: CreatePostUseCase = CreatePostUseCase(feedRepository),
    private val togglePostLikeUseCase: TogglePostLikeUseCase = TogglePostLikeUseCase(feedRepository)
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        observePosts()
        refreshFeed()
    }

    private fun observePosts() {
        viewModelScope.launch {
            getFeedPostsUseCase().collect { list ->
                _posts.value = list
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            fetchFeedPostsUseCase()
        }
    }

    fun createPost(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            createPostUseCase(content)
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            togglePostLikeUseCase(postId)
        }
    }
}
