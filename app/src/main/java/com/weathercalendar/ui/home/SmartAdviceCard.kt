package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DayInfo
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.components.GlassCard

/**
 * 一句话智能建议 — 用户出门前最需要的信息。
 * 综合温度、天气、降雨预报生成一句话。
 */
@Composable
fun SmartAdviceCard(
    currentWeather: CurrentWeather,
    todayInfo: DayInfo?,
    rainForecast: RainForecast?,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val advice = buildSmartAdvice(currentWeather, todayInfo, rainForecast)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = advice.icon, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = advice.text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            )
        }
    }
}

private data class Advice(val icon: String, val text: String)

private fun buildSmartAdvice(
    current: CurrentWeather,
    todayInfo: DayInfo?,
    rainForecast: RainForecast?,
): Advice {
    val temp = current.temperature
    val condition = current.condition
    val tempMin = todayInfo?.weather?.tempMin ?: temp
    val tempMax = todayInfo?.weather?.tempMax ?: temp
    val tempDiff = tempMax - tempMin

    // 优先级：降雨 > 极端天气 > 温差 > 穿衣
    // 1. 正在下雨
    if (rainForecast?.isRaining == true) {
        return Advice("☂️", "外面在下雨，出门记得带伞")
    }

    // 2. 即将下雨（30 分钟内）
    if (rainForecast?.minutesToRain != null && rainForecast.minutesToRain <= 30) {
        return Advice("🌦️", "${rainForecast.minutesToRain}分钟后可能下雨，建议带伞出门")
    }

    // 3. 极端天气
    when (condition) {
        WeatherCondition.STORMY -> return Advice("⛈️", "有雷暴天气，尽量待在室内")
        WeatherCondition.SNOWY -> return Advice("❄️", "今天有雪，注意保暖防滑")
        WeatherCondition.RAINY -> return Advice("🌧️", "今天有雨，出门带伞")
        WeatherCondition.FOGGY -> return Advice("🌫️", "有雾，开车注意能见度")
        else -> {}
    }

    // 4. 高温/低温
    if (temp >= 35) return Advice("🥵", "高温天气，注意防暑降温多喝水")
    if (temp <= 0) return Advice("🥶", "气温零下，注意防寒保暖")

    // 5. 温差大
    if (tempDiff > 10) {
        return Advice("🌡️", "今天温差${tempDiff}°（$tempMin°~$tempMax°），早晚注意添衣")
    }

    // 6. 穿衣建议 + 额外信息
    val dressing = when {
        temp >= 30 -> Advice("👕", "今天$temp°，穿短袖就好，适合户外活动")
        temp >= 25 -> Advice("👔", "今天$temp°，薄衫即可，适合散步")
        temp >= 20 -> Advice("🧥", "今天$temp°，建议穿长袖，舒适宜人")
        temp >= 15 -> Advice("🧥", "今天$temp°，建议穿外套")
        temp >= 10 -> Advice("🧣", "今天$temp°，穿夹克或薄棉衣")
        else -> Advice("🧤", "今天$temp°，穿厚外套注意保暖")
    }

    // 如果下午可能下雨，追加提醒
    if (rainForecast?.minutesToRain != null) {
        return Advice(dressing.icon, "${dressing.text}，${rainForecast.summary}")
    }

    return dressing
}
