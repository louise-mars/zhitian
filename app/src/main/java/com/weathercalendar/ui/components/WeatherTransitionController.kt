package com.weathercalendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.theme.WeatherColors

/**
 * 天气转场动画控制器 — 背景渐变色平滑过渡。
 *
 * 使用 animateColorAsState 实现 600-800ms ease-in-out 交叉淡入。
 * 天然支持中断：当 target 在动画进行中变化时，从当前插值颜色开始新过渡。
 *
 * @param condition 当前天气条件
 * @param isDay 是否白天
 * @param transitionDuration 过渡时长（正常 700ms，降级 200ms，省电 0ms）
 * @param content 内容
 */
@Composable
fun AnimatedWeatherBackground(
    condition: WeatherCondition,
    isDay: Boolean,
    transitionDuration: Int = 700,
    content: @Composable () -> Unit,
) {
    val targetGradient = WeatherColors.gradientFor(condition, isDay)

    val startColor by animateColorAsState(
        targetValue = targetGradient.start,
        animationSpec = tween(durationMillis = transitionDuration, easing = EaseInOut),
        label = "gradient_start",
    )
    val endColor by animateColorAsState(
        targetValue = targetGradient.end,
        animationSpec = tween(durationMillis = transitionDuration, easing = EaseInOut),
        label = "gradient_end",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(startColor, endColor)))
    ) {
        content()
    }
}
