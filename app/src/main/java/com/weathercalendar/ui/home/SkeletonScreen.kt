package com.weathercalendar.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 骨架屏 — 加载时显示 shimmer 动画占位。
 */
@Composable
fun SkeletonScreen(textColor: Color) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            textColor.copy(alpha = 0.06f),
            textColor.copy(alpha = 0.15f),
            textColor.copy(alpha = 0.06f),
        ),
        start = Offset(shimmerOffset - 200f, 0f),
        end = Offset(shimmerOffset, 0f),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        // Header placeholder
        ShimmerBox(
            brush = shimmerBrush,
            width = 120.dp,
            height = 24.dp,
            cornerRadius = 8.dp,
        )
        Spacer(Modifier.height(8.dp))
        ShimmerBox(
            brush = shimmerBrush,
            width = 180.dp,
            height = 16.dp,
            cornerRadius = 6.dp,
        )

        Spacer(Modifier.height(24.dp))

        // Weather card placeholder
        ShimmerBox(
            brush = shimmerBrush,
            fillWidth = true,
            height = 160.dp,
            cornerRadius = 24.dp,
        )

        Spacer(Modifier.height(16.dp))

        // Advice placeholder
        ShimmerBox(
            brush = shimmerBrush,
            fillWidth = true,
            height = 40.dp,
            cornerRadius = 12.dp,
        )

        Spacer(Modifier.height(16.dp))

        // Hourly row placeholder
        ShimmerBox(
            brush = shimmerBrush,
            fillWidth = true,
            height = 80.dp,
            cornerRadius = 16.dp,
        )

        Spacer(Modifier.height(16.dp))

        // Forecast placeholder
        ShimmerBox(
            brush = shimmerBrush,
            fillWidth = true,
            height = 120.dp,
            cornerRadius = 16.dp,
        )
    }
}

@Composable
private fun ShimmerBox(
    brush: Brush,
    height: Dp,
    cornerRadius: Dp = 8.dp,
    width: Dp? = null,
    fillWidth: Boolean = false,
) {
    val sizeModifier = when {
        fillWidth -> Modifier.fillMaxWidth()
        width != null -> Modifier.width(width)
        else -> Modifier
    }
    Box(
        modifier = sizeModifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}
