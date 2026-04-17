package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.ui.components.GlassCard

/**
 * 分钟级降雨预报卡片。
 * 显示："32 分钟后可能下雨" 或 "2 小时内无降雨"
 */
@Composable
fun RainForecastCard(
    rainForecast: RainForecast,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = if (rainForecast.isRaining) 0.15f else 0.12f,
        cornerRadius = 20.dp,
        elevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (rainForecast.isRaining) "🌧️" else if (rainForecast.minutesToRain != null) "🌦️" else "☀️"
            androidx.compose.material3.Text(
                text = icon,
                fontSize = 22.sp,
            )
            Spacer(Modifier.width(12.dp))
            androidx.compose.material3.Text(
                text = rainForecast.summary,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
