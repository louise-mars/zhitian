package com.weathercalendar.domain.alert

import java.time.LocalDate
import java.time.LocalTime

/**
 * 日程天气预警数据模型。
 */
data class WeatherAlert(
    val eventName: String,          // 事件名（截断至 20 字符）
    val eventDate: LocalDate,
    val eventTime: LocalTime?,      // null = 全天事件
    val weatherLabel: String,       // "雨", "雷暴", "雪" 等
    val triggerMetric: String,      // "最高温 37°C", "风速 42km/h" 等
    val suggestion: String,         // 建议文本（≤30 字符）
    val relativeDateText: String,   // "今天", "明天", "后天", "3天后"
)

/**
 * 恶劣天气条件密封类，按严重程度排序（priority 越小越严重）。
 */
sealed class BadWeatherCondition(val priority: Int) {
    data object Stormy : BadWeatherCondition(1)
    data object Snowy : BadWeatherCondition(2)
    data object Rainy : BadWeatherCondition(3)
    data class StrongWind(val speed: Int) : BadWeatherCondition(4)
    data class ExtremeHeat(val temp: Int) : BadWeatherCondition(5)
    data class ExtremeCold(val temp: Int) : BadWeatherCondition(6)
}
