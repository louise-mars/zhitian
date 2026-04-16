package com.weathercalendar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.ui.components.GlassCard
import java.time.format.DateTimeFormatter

/**
 * 今日日程迷你卡片 — 首页显示，不用进日历页就能看到今天的安排。
 * 最多显示 3 条，超过显示"+N 条"。
 */
@Composable
fun TodayEventsCard(
    events: List<CalendarEvent>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) return

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(
                text = "今日日程",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(8.dp))

            val displayEvents = events.take(3)
            displayEvents.forEach { event ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(event.color)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = event.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (events.size > 3) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "+${events.size - 3} 条日程",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.4f),
                )
            }
        }
    }
}
