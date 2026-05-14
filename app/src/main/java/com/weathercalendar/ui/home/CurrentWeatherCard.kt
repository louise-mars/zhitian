package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.mock.MockData
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.ui.components.GlassCard
import com.weathercalendar.ui.components.WeatherIconAnimator
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import com.weathercalendar.util.formatTempShort

@Composable
fun CurrentWeatherCard(
    weather: CurrentWeather,
    textColor: Color,
    modifier: Modifier = Modifier,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    iconAnimationEnabled: Boolean = true,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.15f,
        cornerRadius = 24.dp,
        elevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 天气图标（微动画）
            WeatherIconAnimator(
                condition = weather.condition,
                enabled = iconAnimationEnabled,
            )

            Spacer(Modifier.height(4.dp))

            // 温度
            Text(
                text = weather.temperature.formatTempShort(tempUnit),
                color = textColor,
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(2.dp))

            // 天气描述
            Text(
                text = weather.condition.label,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(10.dp))

            // 体感温度 + 建议
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Only show feels-like when it differs from actual temperature
                if (weather.feelsLike != weather.temperature) {
                    Text(
                        text = "体感 ${weather.feelsLike.formatTempShort(tempUnit)}",
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = weather.condition.tip,
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            // 体感偏差解释（差值 >= 3°C 时显示）
            val tempDiff = weather.feelsLike - weather.temperature
            if (kotlin.math.abs(tempDiff) >= 3) {
                Spacer(Modifier.height(4.dp))
                val explanation = if (tempDiff < 0) {
                    "🌬️ 风大体感偏冷"
                } else {
                    "💧 湿度高体感闷热"
                }
                Text(
                    text = explanation,
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0288D1)
@Composable
private fun CurrentWeatherCardPreview() {
    WeatherCalendarTheme(dynamicColor = false) {
        CurrentWeatherCard(weather = MockData.currentWeather, textColor = Color.White)
    }
}
