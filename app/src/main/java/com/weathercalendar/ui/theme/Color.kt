package com.weathercalendar.ui.theme

import androidx.compose.ui.graphics.Color
import com.weathercalendar.data.model.WeatherCondition

/**
 * 天气动态渐变配色 — 用于全页背景。
 * 高饱和度渐变，提升视觉冲击力。
 */
object WeatherColors {

    data class GradientPair(val start: Color, val end: Color)

    // 晴天 — 柔和天蓝（降低饱和度，不刺眼）
    private val Sunny = GradientPair(Color(0xFF5B9BD5), Color(0xFF2E86C1))
    // 多云 — 提亮灰蓝（保证白色文字可读）
    private val Cloudy = GradientPair(Color(0xFF7B9AAF), Color(0xFF4A6B7A))
    // 雨天 — 柔和青灰
    private val Rainy = GradientPair(Color(0xFF5A8A8C), Color(0xFF2C4A4C))
    // 雪天 — 冰蓝（稍暗，避免太白）
    private val Snowy = GradientPair(Color(0xFFD0E0F0), Color(0xFF9AB8D0))
    // 夜间 — 深蓝
    private val Night = GradientPair(Color(0xFF1A237E), Color(0xFF0D47A1))
    // 雷暴 — 深灰
    private val Storm = GradientPair(Color(0xFF3E4E5A), Color(0xFF1E2E3A))
    // 雾 — 中灰（不太亮也不太暗）
    private val Foggy = GradientPair(Color(0xFFA0B0BC), Color(0xFF6E8898))

    fun gradientFor(condition: WeatherCondition, isDay: Boolean = true): GradientPair {
        if (!isDay) return Night
        return when (condition) {
            WeatherCondition.SUNNY -> Sunny
            WeatherCondition.PARTLY_CLOUDY -> Sunny
            WeatherCondition.CLOUDY -> Cloudy
            WeatherCondition.FOGGY -> Foggy
            WeatherCondition.DRIZZLE, WeatherCondition.RAINY -> Rainy
            WeatherCondition.SNOWY -> Snowy
            WeatherCondition.STORMY -> Storm
        }
    }

    /** 雪天/雾天背景偏亮，需要深色文字；其余用白色 */
    fun textColorOn(condition: WeatherCondition, isDay: Boolean = true): Color {
        if (!isDay) return Color.White
        return when (condition) {
            WeatherCondition.SNOWY -> Color(0xFF1A3050)
            else -> Color.White
        }
    }

    /** 日历页用：天气联动渐变（比首页稍柔和但仍有辨识度） */
    fun calendarGradientFor(condition: WeatherCondition, isDay: Boolean = true): GradientPair {
        if (!isDay) return GradientPair(Color(0xFF141E30), Color(0xFF243B55))
        return when (condition) {
            WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY ->
                GradientPair(Color(0xFF5B9BD5), Color(0xFF3A7BBF))
            WeatherCondition.CLOUDY ->
                GradientPair(Color(0xFF8AA0B0), Color(0xFF4A6070))
            WeatherCondition.FOGGY ->
                GradientPair(Color(0xFF95A8B5), Color(0xFF5A7585))
            WeatherCondition.DRIZZLE, WeatherCondition.RAINY ->
                GradientPair(Color(0xFF5A8A8C), Color(0xFF2C4A4C))
            WeatherCondition.SNOWY ->
                GradientPair(Color(0xFFC0D4E4), Color(0xFF7A9AB0))
            WeatherCondition.STORMY ->
                GradientPair(Color(0xFF3D4E5C), Color(0xFF1A252F))
        }
    }

    // 农历节日红色
    val LunarFestival = Color(0xFFE53935)
    // 节气绿色
    val SolarTerm = Color(0xFF43A047)
}
