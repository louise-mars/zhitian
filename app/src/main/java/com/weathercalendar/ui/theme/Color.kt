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

    // 浅色模式专用渐变（更明亮、更柔和）
    private val SunnyLight = GradientPair(Color(0xFF87CEEB), Color(0xFF5DADE2))
    private val CloudyLight = GradientPair(Color(0xFFA8C4D4), Color(0xFF7FA8B8))
    private val RainyLight = GradientPair(Color(0xFF7FAAAC), Color(0xFF5A8A8C))
    private val SnowyLight = GradientPair(Color(0xFFE8F0F8), Color(0xFFBDD4E8))
    private val StormLight = GradientPair(Color(0xFF6E7E8A), Color(0xFF4E5E6A))
    private val FoggyLight = GradientPair(Color(0xFFC0D0DC), Color(0xFF90A8B8))

    fun gradientFor(condition: WeatherCondition, isDay: Boolean = true, forceDark: Boolean? = null): GradientPair {
        // forceDark: true = 强制深色, false = 强制浅色, null = 跟随日夜
        val useDark = forceDark ?: !isDay
        if (useDark) return Night

        // 浅色模式（forceDark == false）使用更明亮的渐变
        if (forceDark == false) {
            return when (condition) {
                WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> SunnyLight
                WeatherCondition.CLOUDY -> CloudyLight
                WeatherCondition.FOGGY -> FoggyLight
                WeatherCondition.DRIZZLE, WeatherCondition.RAINY -> RainyLight
                WeatherCondition.SNOWY -> SnowyLight
                WeatherCondition.STORMY -> StormLight
            }
        }

        // 默认（跟随系统/白天）
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
                GradientPair(Color(0xFF3A6B9F), Color(0xFF1E4A7A))
            WeatherCondition.CLOUDY ->
                GradientPair(Color(0xFF5A7080), Color(0xFF3A4E5A))
            WeatherCondition.FOGGY ->
                GradientPair(Color(0xFF6A8090), Color(0xFF4A6070))
            WeatherCondition.DRIZZLE, WeatherCondition.RAINY ->
                GradientPair(Color(0xFF4A7072), Color(0xFF1E3A3C))
            WeatherCondition.SNOWY ->
                GradientPair(Color(0xFF8AAABE), Color(0xFF5A7A90))
            WeatherCondition.STORMY ->
                GradientPair(Color(0xFF2E3E4A), Color(0xFF141E28))
        }
    }

    // 农历节日红色
    val LunarFestival = Color(0xFFE53935)
    // 节气绿色
    val SolarTerm = Color(0xFF43A047)
}
