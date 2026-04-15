package com.weathercalendar.ui.theme

import androidx.compose.ui.graphics.Color
import com.weathercalendar.data.model.WeatherCondition

/**
 * 天气动态渐变配色 — 用于全页背景。
 */
object WeatherColors {

    data class GradientPair(val start: Color, val end: Color)

    // 晴天
    private val Sunny = GradientPair(Color(0xFF4FC3F7), Color(0xFF0288D1))
    // 多云
    private val Cloudy = GradientPair(Color(0xFFB0BEC5), Color(0xFF78909C))
    // 雨天
    private val Rainy = GradientPair(Color(0xFF546E7A), Color(0xFF37474F))
    // 雪天
    private val Snowy = GradientPair(Color(0xFFE1F5FE), Color(0xFFB3E5FC))
    // 夜间
    private val Night = GradientPair(Color(0xFF1A237E), Color(0xFF0D47A1))
    // 雷暴
    private val Storm = GradientPair(Color(0xFF424242), Color(0xFF212121))
    // 雾
    private val Foggy = GradientPair(Color(0xFFCFD8DC), Color(0xFF90A4AE))

    fun gradientFor(condition: WeatherCondition, isDay: Boolean = true): GradientPair {
        if (!isDay) return Night
        return when (condition) {
            WeatherCondition.SUNNY -> Sunny
            WeatherCondition.PARTLY_CLOUDY -> Sunny  // 偏晴
            WeatherCondition.CLOUDY -> Cloudy
            WeatherCondition.FOGGY -> Foggy
            WeatherCondition.DRIZZLE, WeatherCondition.RAINY -> Rainy
            WeatherCondition.SNOWY -> Snowy
            WeatherCondition.STORMY -> Storm
        }
    }

    /** 雪天背景偏白，需要深色文字；其余用白色 */
    fun textColorOn(condition: WeatherCondition, isDay: Boolean = true): Color {
        if (!isDay) return Color.White
        return when (condition) {
            WeatherCondition.SNOWY -> Color(0xFF1A237E)
            else -> Color.White
        }
    }

    /** 日历页用：降低饱和度的渐变 */
    fun calendarGradientFor(condition: WeatherCondition, isDay: Boolean = true): GradientPair {
        val base = gradientFor(condition, isDay)
        return GradientPair(
            start = base.start.copy(alpha = 0.6f),
            end = base.end.copy(alpha = 0.6f),
        )
    }

    // 农历节日红色
    val LunarFestival = Color(0xFFE53935)
    // 节气绿色
    val SolarTerm = Color(0xFF43A047)
}
