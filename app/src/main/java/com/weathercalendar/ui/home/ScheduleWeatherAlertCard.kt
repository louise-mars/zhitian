package com.weathercalendar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.domain.alert.WeatherAlert
import com.weathercalendar.ui.components.GlassCard

/**
 * 日程天气预警卡片 — 在首页显示未来日程与恶劣天气的交叉预警。
 *
 * 设计：
 * - 醒目的 ⚠️ 图标 + 暖色背景
 * - 格式："[相对日期]有[事件名]，预计[天气]，建议[行动]"
 * - 最多显示 5 条，按日期排序
 * - 显示/隐藏带垂直展开动画
 */
@Composable
fun ScheduleWeatherAlertCard(
    alerts: List<WeatherAlert>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = alerts.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        GlassCard(
            modifier = modifier.fillMaxWidth(),
            alpha = 0.18f,
            cornerRadius = 16.dp,
            elevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                // 标题行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "日程天气提醒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F),
                    )
                }
                Spacer(Modifier.height(8.dp))

                // 预警列表
                alerts.take(5).forEach { alert ->
                    AlertItem(alert)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertItem(alert: WeatherAlert) {
    val text = buildString {
        append(alert.relativeDateText)
        append("有")
        append(alert.eventName)
        append("，预计")
        append(alert.weatherLabel)
        append("，")
        append(alert.suggestion)
    }

    Text(
        text = text,
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.9f),
        lineHeight = 18.sp,
    )
}
