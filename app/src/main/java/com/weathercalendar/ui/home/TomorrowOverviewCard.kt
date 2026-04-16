package com.weathercalendar.ui.home

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
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.DayInfo
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.components.GlassCard
import java.time.format.DateTimeFormatter

/**
 * 明日概览卡片 — 用户晚上最想知道的信息。
 * 明天天气 + 穿衣建议 + 日程提醒。
 */
@Composable
fun TomorrowOverviewCard(
    tomorrowInfo: DayInfo?,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (tomorrowInfo == null) return

    val weather = tomorrowInfo.weather
    val events = tomorrowInfo.events

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(
                text = "明日概览",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(10.dp))

            // 天气行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = weather.condition.icon, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${weather.condition.label}  ${weather.tempMin}° ~ ${weather.tempMax}°",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                    )
                    Text(
                        text = tomorrowDressingAdvice(weather.tempMin, weather.tempMax, weather.condition),
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.6f),
                    )
                }
            }

            // 明日日程
            if (events.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                events.take(2).forEach { event ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📅", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                event.time?.let { append(it.format(DateTimeFormatter.ofPattern("HH:mm"))) ; append(" ") }
                                append(event.title)
                            },
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
                // 如果明天有日程且天气不好，给出提醒
                if (weather.condition in listOf(
                        WeatherCondition.RAINY, WeatherCondition.DRIZZLE,
                        WeatherCondition.STORMY, WeatherCondition.SNOWY,
                    )
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "💡 明天有日程且预计${weather.condition.label}，建议提前出门",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD54F),
                    )
                }
            }
        }
    }
}

private fun tomorrowDressingAdvice(tempMin: Int, tempMax: Int, condition: WeatherCondition): String {
    val avgTemp = (tempMin + tempMax) / 2
    val dressing = when {
        avgTemp >= 30 -> "穿短袖"
        avgTemp >= 25 -> "穿薄衫"
        avgTemp >= 20 -> "穿长袖"
        avgTemp >= 15 -> "穿外套"
        avgTemp >= 10 -> "穿夹克"
        avgTemp >= 5 -> "穿棉衣"
        else -> "穿羽绒服"
    }
    val extra = when (condition) {
        WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> "，记得带伞"
        WeatherCondition.SNOWY -> "，注意防滑"
        WeatherCondition.STORMY -> "，尽量少出门"
        else -> ""
    }
    return "建议$dressing$extra"
}
