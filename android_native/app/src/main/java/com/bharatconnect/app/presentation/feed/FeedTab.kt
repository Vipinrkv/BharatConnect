package com.bharatconnect.app.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.domain.model.Post
import com.bharatconnect.app.domain.model.UserProfile
import com.bharatconnect.app.presentation.components.FeedPostSkeleton
import com.bharatconnect.app.presentation.components.StoriesRowSkeleton
import com.bharatconnect.app.presentation.story.StoriesRow
import com.bharatconnect.app.presentation.story.StoryItem

@Composable
fun FeedTab(
    feedViewModel: FeedViewModel,
    stories: List<StoryItem>,
    currentUser: UserProfile? = null,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryItem) -> Unit
) {
    val posts by feedViewModel.posts.collectAsState()
    val isLoadingFeed by feedViewModel.isLoading.collectAsState()
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var viewingCommentsForPost by remember { mutableStateOf<Post?>(null) }
    var viewingAuthorProfile by remember { mutableStateOf<String?>(null) }
    val followedAuthors = remember { mutableStateListOf<String>() }

    // In-memory comments map for posts
    val postComments = remember {
        mutableStateMapOf<String, MutableList<Pair<String, String>>>().apply {
            put("demo_1", mutableListOf(
                "Priya Verma" to "Amazing update! Proud of Indian innovation 🇮🇳",
                "Amit Patel" to "Completely agree, loving the speed and privacy features!"
            ))
            put("demo_2", mutableListOf(
                "Rajesh Kumar" to "Count me in for the meetup! 🔥"
            ))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                if (isLoadingFeed && stories.isEmpty()) {
                    StoriesRowSkeleton()
                } else {
                    StoriesRow(
                        stories = stories,
                        currentUserAvatar = currentUser?.avatarUrl,
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = onStoryClick
                    )
                }
            }

            if (isLoadingFeed && posts.isEmpty()) {
                items(3) {
                    FeedPostSkeleton()
                }
            } else if (posts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF221F45)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    tint = ColorPrimary6367FF,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Feed is Ready",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Be the first to share an update, thought, or story with your community!",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showCreatePostDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Post")
                            }
                        }
                    }
                }
            } else {
                items(posts) { post ->
                    val isFollowing = followedAuthors.contains(post.authorName)
                    PostCard(
                        post = post,
                        isFollowing = isFollowing,
                        onLikeClick = { feedViewModel.toggleLike(post.id) },
                        onFollowClick = {
                            if (isFollowing) {
                                followedAuthors.remove(post.authorName)
                            } else {
                                followedAuthors.add(post.authorName)
                            }
                        },
                        onCommentClick = { viewingCommentsForPost = post },
                        onProfileClick = { viewingAuthorProfile = post.authorName }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreatePostDialog = true },
            containerColor = ColorPrimary6367FF,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Post")
        }
    }

    // Comments Sheet
    if (viewingCommentsForPost != null) {
        val targetPost = viewingCommentsForPost!!
        val commentsList = postComments.getOrPut(targetPost.id) { mutableListOf() }
        PostCommentsBottomSheet(
            post = targetPost,
            commentsList = commentsList,
            currentUser = currentUser,
            onDismiss = { viewingCommentsForPost = null }
        )
    }

    // Profile View Dialog
    if (viewingAuthorProfile != null) {
        val author = viewingAuthorProfile!!
        AlertDialog(
            onDismissRequest = { viewingAuthorProfile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ColorPrimary6367FF),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(author.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(author, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Verified BharatConnect Member 🛡️", color = Color(0xFF4EFEAA), fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Connect with $author while keeping your phone number private.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("142", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Followers", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("89", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Following", color = Color.Gray, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("18", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Posts", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingAuthorProfile = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Send Friend Request / Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingAuthorProfile = null }) {
                    Text("Close", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF16142E),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onPublish = { fullContent ->
                feedViewModel.createPost(fullContent)
                showCreatePostDialog = false
            }
        )
    }
}
