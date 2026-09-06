package com.bharatconnect.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF

/**
 * Animated Shimmer Effect Modifier for smooth skeleton loading screens.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color(0xFF17142E),
    highlightColor: Color = Color(0xFF2C2754),
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(shape)
        .background(brush)
}

/**
 * Shimmering Skeleton Box placeholder.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color(0xFF17142E),
    highlightColor: Color = Color(0xFF2C2754)
) {
    Box(
        modifier = modifier.shimmerEffect(shape = shape, baseColor = baseColor, highlightColor = highlightColor)
    )
}

/**
 * Shimmering placeholder for ephemeral story bubbles row.
 */
@Composable
fun StoriesRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(6) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .shimmerEffect(CircleShape)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(48.dp)
                        .height(10.dp),
                    shape = RoundedCornerShape(5.dp)
                )
            }
        }
    }
}

/**
 * Shimmering placeholder for social feed post card.
 */
@Composable
fun FeedPostSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121026))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar + Name + Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
                SkeletonBox(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body text lines
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Image/Media block placeholder
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom action icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                }
                SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
            }
        }
    }
}

/**
 * Shimmering placeholder for chat conversation row item.
 */
@Composable
fun ConversationItemSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121026))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar
            SkeletonBox(
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Title and last message
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(11.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp / unread badge placeholder
            Column(horizontalAlignment = Alignment.End) {
                SkeletonBox(
                    modifier = Modifier
                        .width(40.dp)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape
                )
            }
        }
    }
}

/**
 * Shimmering placeholder for contact drawer row item.
 */
@Composable
fun ContactItemSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(13.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            // Action button placeholder
            SkeletonBox(
                modifier = Modifier
                    .width(72.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Shimmering placeholder for notification card item.
 */
@Composable
fun NotificationItemSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(13.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(11.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(9.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

/**
 * Shimmering placeholder for marketplace item card.
 */
@Composable
fun MarketItemSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            SkeletonBox(
                modifier = Modifier
                    .width(64.dp)
                    .height(34.dp),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * Shimmering placeholder for nearby radar user cards.
 */
@Composable
fun NearbyUserSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14122A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(46.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(11.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(10.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            SkeletonBox(
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
