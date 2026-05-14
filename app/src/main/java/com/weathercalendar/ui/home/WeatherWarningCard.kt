package com.weathercalendar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.WeatherWarning

/**
 * 天气预警卡片 — 气象局官方预警，醒目显示。
 *
 * 设计：
 * - 根据预警等级（红/橙/黄/蓝）使用对应颜色边框和背景色调
 * - 位置在首页最醒目区域（诗词下方、小时预报上方）
 * - 红色/橙色预警使用更强烈的视觉提示
 */
@Composable
fun WeatherWarningCard(
    warnings: List<WeatherWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return

    // 取最高等级预警的颜色
    val highestLevel = warnings.firstOrNull()?.level ?: "蓝色"
    val accentColor = when (highestLevel) {
        "红色" -> Color(0xFFE53935)
        "橙色" -> Color(0xFFFF9800)
        "黄色" -> Color(0xFFFFEB3B)
        else -> Color(0xFF42A5F5)
    }

    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accentColor.copy(alpha = 0.12f))
            .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.6f), shape = shape)
            .padding(14.dp),
    ) {
        // 标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🚨", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "气象预警",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        Spacer(Modifier.height(8.dp))

        warnings.forEachIndexed { index, warning ->
            Row(verticalAlignment = Alignment.Top) {
                Text("•", fontSize = 14.sp, color = accentColor)
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${warning.typeName}${warning.level}预警",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    Text(
                        text = warning.text.take(100) + if (warning.text.length > 100) "…" else "",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 3,
                        lineHeight = 16.sp,
                    )
                }
            }
            if (index < warnings.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
