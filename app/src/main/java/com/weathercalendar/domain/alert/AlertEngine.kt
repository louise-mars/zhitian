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
 * 改进点（v2.1）：
 * 1. 室内日程关键词过滤 — "Zoom"、"线上会议"等不触发预警
 * 2. 降水量阈值 — precip > 10mm 触发（比单纯看天气类型更精准）
 * 3. 温度阈值调整 — 低温从 -10°C 调整为 0°C（冰冻即预警）
 */
@Singleton
class AlertEngine @Inject constructor(
    private val eventRepository: EventRepository,
    private val calendarRepository: CalendarRepository,
) {
    companion object {
        /** 室内活动关键词 — 匹配到则跳过预警 */
        private val INDOOR_KEYWORDS = setOf(
            "线上", "zoom", "teams", "电话", "视频", "远程", "online",
            "腾讯会议", "钉钉", "飞书", "webex", "skype", "网课",
            "直播", "录制", "居家", "在家", "wfh", "remote",
        )

        /** 降水量阈值 mm（超过则触发预警） */
        private const val PRECIP_THRESHOLD = 10.0

        /** 高温阈值 °C */
        private const val HEAT_THRESHOLD = 35

        /** 低温阈值 °C（冰冻） */
        private const val COLD_THRESHOLD = 0

        /** 强风阈值 km/h（约 6 级风） */
        private const val WIND_THRESHOLD = 39
    }

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

        // 交叉匹配：事件日期 × 恶劣天气（过滤室内事件）
        val alerts = mutableListOf<WeatherAlert>()

        for (event in allEvents) {
            // 室内日程过滤
            if (isIndoorEvent(event.title)) continue

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
     * 判断事件是否为室内/线上活动。
     */
    private fun isIndoorEvent(title: String): Boolean {
        val lower = title.lowercase()
        return INDOOR_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * 检测某天的恶劣天气条件。
     * 使用多维度判断：天气类型 + 降水量 + 温度 + 风速。
     */
    private fun detectBadWeather(weather: DailyWeather): List<BadWeatherCondition> {
        val conditions = mutableListOf<BadWeatherCondition>()

        // 天气类型判断
        when (weather.condition) {
            WeatherCondition.STORMY -> conditions.add(BadWeatherCondition.Stormy)
            WeatherCondition.SNOWY -> conditions.add(BadWeatherCondition.Snowy)
            WeatherCondition.RAINY -> conditions.add(BadWeatherCondition.Rainy)
            WeatherCondition.DRIZZLE -> {
                // 小雨只有降水量超阈值才预警（毛毛雨不值得打扰用户）
                if (weather.precip > PRECIP_THRESHOLD) {
                    conditions.add(BadWeatherCondition.Rainy)
                }
            }
            else -> {
                // 即使天气类型不是雨，但降水量超阈值也预警
                if (weather.precip > PRECIP_THRESHOLD) {
                    conditions.add(BadWeatherCondition.Rainy)
                }
            }
        }

        // 温度阈值（调整为 0°C 冰冻预警）
        if (weather.tempMax > HEAT_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeHeat(weather.tempMax))
        }
        if (weather.tempMin <= COLD_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeCold(weather.tempMin))
        }

        // 风速阈值
        if (weather.windSpeed > WIND_THRESHOLD) {
            conditions.add(BadWeatherCondition.StrongWind(weather.windSpeed))
        }

        return conditions
    }

    private fun getWeatherLabel(condition: BadWeatherCondition, weather: DailyWeather): String {
        return when (condition) {
            is BadWeatherCondition.Stormy -> "雷暴"
            is BadWeatherCondition.Snowy -> "雪"
            is BadWeatherCondition.Rainy -> {
                if (weather.precip > 25) "大雨" else if (weather.precip > PRECIP_THRESHOLD) "中雨" else weather.condition.label
            }
            is BadWeatherCondition.StrongWind -> "大风"
            is BadWeatherCondition.ExtremeHeat -> "高温"
            is BadWeatherCondition.ExtremeCold -> "低温"
        }
    }

    private fun getTriggerMetric(condition: BadWeatherCondition, weather: DailyWeather): String {
        return when (condition) {
            is BadWeatherCondition.Stormy -> "雷暴天气"
            is BadWeatherCondition.Snowy -> "降雪 ${weather.precip}mm"
            is BadWeatherCondition.Rainy -> "降水 ${weather.precip}mm"
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
