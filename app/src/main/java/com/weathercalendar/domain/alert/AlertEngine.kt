package com.weathercalendar.domain.alert

import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.repository.CalendarRepository
import com.weathercalendar.data.repository.EventRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程天气预警引擎 — 将未来日程与天气预报交叉匹配，生成预警。
 *
 * 逻辑：
 * 1. 查询未来 15 天的所有事件（App 内 + 系统日历）
 * 2. 对每个有事件的日期，检查天气是否为恶劣天气
 * 3. 如果匹配，生成预警卡片数据
 */
@Singleton
class AlertEngine @Inject constructor(
    private val eventRepository: EventRepository,
    private val calendarRepository: CalendarRepository,
) {
    /**
     * 生成日程天气预警列表。
     * @param dailyForecasts 15 天天气预报数据
     * @return 按日期排序的预警列表（最多 5 条）
     */
    suspend fun generateAlerts(dailyForecasts: List<DailyWeather>): List<WeatherAlert> {
        if (dailyForecasts.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val endDate = today.plusDays(14)

        // 构建日期 → 天气映射
        val weatherByDate = dailyForecasts.associateBy { it.date }

        // 收集所有事件（两个数据源独立容错）
        val allEvents = mutableListOf<CalendarEvent>()

        try {
            val appEvents = eventRepository.getEventsBetween(today, endDate)
            allEvents.addAll(appEvents)
        } catch (_: Exception) { /* 跳过 */ }

        try {
            val systemEvents = calendarRepository.getEventsGroupedByDate(today, endDate)
            systemEvents.values.forEach { allEvents.addAll(it) }
        } catch (_: Exception) { /* 跳过 */ }

        if (allEvents.isEmpty()) return emptyList()

        // 交叉匹配：事件日期 × 恶劣天气
        val alerts = mutableListOf<WeatherAlert>()

        for (event in allEvents) {
            val weather = weatherByDate[event.date] ?: continue
            val conditions = detectBadWeather(weather)
            if (conditions.isEmpty()) continue

            val suggestion = SuggestionGenerator.getSuggestion(conditions)
            val highestCondition = conditions.minByOrNull { it.priority } ?: continue

            alerts.add(
                WeatherAlert(
                    eventName = truncateName(event.title),
                    eventDate = event.date,
                    eventTime = event.time,
                    weatherLabel = getWeatherLabel(highestCondition, weather),
                    triggerMetric = getTriggerMetric(highestCondition, weather),
                    suggestion = suggestion,
                    relativeDateText = getRelativeDateText(today, event.date),
                )
            )
        }

        // 按日期排序，同日按时间排序（全天事件在前），最多 5 条
        return alerts
            .sortedWith(compareBy({ it.eventDate }, { it.eventTime ?: java.time.LocalTime.MIN }))
            .take(5)
    }

    /**
     * 检测某天的恶劣天气条件。
     */
    private fun detectBadWeather(weather: DailyWeather): List<BadWeatherCondition> {
        val conditions = mutableListOf<BadWeatherCondition>()

        when (weather.condition) {
            WeatherCondition.STORMY -> conditions.add(BadWeatherCondition.Stormy)
            WeatherCondition.SNOWY -> conditions.add(BadWeatherCondition.Snowy)
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> conditions.add(BadWeatherCondition.Rainy)
            else -> {}
        }

        if (weather.tempMax > 35) {
            conditions.add(BadWeatherCondition.ExtremeHeat(weather.tempMax))
        }
        if (weather.tempMin < -10) {
            conditions.add(BadWeatherCondition.ExtremeCold(weather.tempMin))
        }
        if (weather.windSpeed > 39) {
            conditions.add(BadWeatherCondition.StrongWind(weather.windSpeed))
        }

        return conditions
    }

    private fun getWeatherLabel(condition: BadWeatherCondition, weather: DailyWeather): String {
        return when (condition) {
            is BadWeatherCondition.Stormy -> "雷暴"
            is BadWeatherCondition.Snowy -> "雪"
            is BadWeatherCondition.Rainy -> weather.condition.label
            is BadWeatherCondition.StrongWind -> "大风"
            is BadWeatherCondition.ExtremeHeat -> "高温"
            is BadWeatherCondition.ExtremeCold -> "严寒"
        }
    }

    private fun getTriggerMetric(condition: BadWeatherCondition, weather: DailyWeather): String {
        return when (condition) {
            is BadWeatherCondition.Stormy -> weather.condition.label
            is BadWeatherCondition.Snowy -> weather.condition.label
            is BadWeatherCondition.Rainy -> weather.condition.label
            is BadWeatherCondition.StrongWind -> "风速 ${condition.speed}km/h"
            is BadWeatherCondition.ExtremeHeat -> "最高温 ${condition.temp}°C"
            is BadWeatherCondition.ExtremeCold -> "最低温 ${condition.temp}°C"
        }
    }

    private fun getRelativeDateText(today: LocalDate, eventDate: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, eventDate).toInt()
        return when (days) {
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            else -> "${days}天后"
        }
    }

    private fun truncateName(name: String): String {
        return if (name.length > 20) name.take(20) + "…" else name
    }
}
