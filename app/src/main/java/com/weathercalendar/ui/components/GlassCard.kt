package com.weathercalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 毛玻璃风格卡片 — 半透明白底，用于浮在天气渐变背景之上。
 *
 * @param alpha 白色透明度，不同组件使用不同值：
 *   - Hero Card: 0.15f
 *   - 融合卡/详情区: 0.12f
 *   - 小时 chip 普通: 0.10f
 *   - 小时 chip 高亮: 0.25f
 * @param cornerRadius 圆角半径
 * @param isDark 暗色模式下用黑色透明替代白色透明
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.15f,
    cornerRadius: Dp = 16.dp,
    isDark: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val baseColor = if (isDark) Color.Black else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor.copy(alpha = alpha)),
        content = content,
    )
}
