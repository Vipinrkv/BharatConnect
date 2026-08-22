package com.bharatconnect.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.data.repository.FeedRepositoryImpl
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.repository.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository = FeedRepositoryImpl()
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        observePosts()
        refreshFeed()
    }

    private fun observePosts() {
        viewModelScope.launch {
            feedRepository.getPostsFlow().collect { list ->
                if (list.isNotEmpty()) {
                    _posts.value = list
                } else {
                    // Seed initial sample posts for offline demo
                    _posts.value = listOf(
                        Post(
                            id = "post_1",
                            authorId = "user_1",
                            authorName = "Aarav Sharma",
                            content = "Namaste! Welcome to BharatConnect on Jetpack Compose + Supabase + Room DB! 🚀",
                            likesCount = 42,
                            commentsCount = 12,
                            createdAt = "2m ago"
                        ),
                        Post(
                            id = "post_2",
                            authorId = "user_2",
                            authorName = "Priya Patel",
                            content = "Media streaming powered by Cloudinary CDN & offline Room synchronization works seamlessly! ✨",
                            likesCount = 29,
                            commentsCount = 7,
                            createdAt = "15m ago"
                        ),
                        Post(
                            id = "post_3",
                            authorId = "user_3",
                            authorName = "Vikram Singh",
                            content = "Zero-dependency native Android architecture built cleanly with Gradle and Kotlin. 🇮🇳",
                            likesCount = 68,
                            commentsCount = 19,
                            createdAt = "1h ago"
                        )
                    )
                }
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            feedRepository.fetchPosts()
        }
    }

    fun createPost(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            feedRepository.createPost(content)
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleLike(postId)
        }
    }
}
