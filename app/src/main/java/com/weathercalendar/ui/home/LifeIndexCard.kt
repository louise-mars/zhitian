package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DayInfo
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.components.GlassCard

/**
 * 生活指数卡片 — 穿衣/洗车/运动/出行建议。
 * 基于温度、天气状况、风速推算，不需要额外 API。
 */
@Composable
fun LifeIndexCard(
    currentWeather: CurrentWeather,
    todayInfo: DayInfo?,
    windSpeed: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val temp = currentWeather.temperature
    val condition = currentWeather.condition
    val tempMin = todayInfo?.weather?.tempMin ?: temp
    val tempMax = todayInfo?.weather?.tempMax ?: temp
    val tempDiff = tempMax - tempMin

    val indices = listOf(
        LifeIndex(
            icon = dressingIcon(temp),
            label = "穿衣",
            value = dressingAdvice(temp),
        ),
        LifeIndex(
            icon = if (isGoodForCarWash(condition)) "🚗" else "💧",
            label = "洗车",
            value = carWashAdvice(condition),
        ),
        LifeIndex(
            icon = if (isGoodForExercise(condition, temp, windSpeed)) "🏃" else "🏠",
            label = "运动",
            value = exerciseAdvice(condition, temp, windSpeed),
        ),
        LifeIndex(
            icon = travelIcon(condition),
            label = "出行",
            value = travelAdvice(condition, tempDiff),
        ),
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "生活指数",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                indices.forEach { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = index.icon, fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = index.label,
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                        )
                        Text(
                            text = index.value,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private data class LifeIndex(val icon: String, val label: String, val value: String)

// ── 穿衣建议 ──

private fun dressingIcon(temp: Int): String = when {
    temp >= 30 -> "👕"
    temp >= 20 -> "👔"
    temp >= 10 -> "🧥"
    temp >= 0 -> "🧣"
    else -> "🧤"
}

private fun dressingAdvice(temp: Int): String = when {
    temp >= 30 -> "短袖"
    temp >= 25 -> "薄衫"
    temp >= 20 -> "长袖"
    temp >= 15 -> "外套"
    temp >= 10 -> "夹克"
    temp >= 5 -> "棉衣"
    temp >= 0 -> "羽绒服"
    else -> "厚羽绒"
}

// ── 洗车建议 ──

private fun isGoodForCarWash(condition: WeatherCondition): Boolean =
    condition in listOf(WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY)

private fun carWashAdvice(condition: WeatherCondition): String = when (condition) {
    WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> "适宜"
    WeatherCondition.CLOUDY -> "较适宜"
    WeatherCondition.FOGGY -> "不宜"
    else -> "不宜"
}

// ── 运动建议 ──

private fun isGoodForExercise(condition: WeatherCondition, temp: Int, windSpeed: Int): Boolean =
    condition in listOf(WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY) &&
        temp in 5..35 && windSpeed < 30

private fun exerciseAdvice(condition: WeatherCondition, temp: Int, windSpeed: Int): String = when {
    condition in listOf(WeatherCondition.RAINY, WeatherCondition.DRIZZLE, WeatherCondition.STORMY, WeatherCondition.SNOWY) -> "室内"
    windSpeed > 30 -> "不宜"
    temp > 35 || temp < 0 -> "不宜"
    else -> "适宜"
}

// ── 出行建议 ──

private fun travelIcon(condition: WeatherCondition): String = when (condition) {
    WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> "🚶"
    WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> "☂️"
    WeatherCondition.SNOWY -> "⛄"
    WeatherCondition.STORMY -> "⚠️"
    else -> "🚶"
}

private fun travelAdvice(condition: WeatherCondition, tempDiff: Int): String = when {
    condition == WeatherCondition.STORMY -> "避免出行"
    condition in listOf(WeatherCondition.RAINY, WeatherCondition.DRIZZLE) -> "带伞"
    condition == WeatherCondition.SNOWY -> "注意路滑"
    tempDiff > 10 -> "注意温差"
    else -> "适宜"
}
