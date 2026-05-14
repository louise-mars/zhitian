package com.weathercalendar.domain.alert

import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.model.WeatherWarning
import com.weathercalendar.data.repository.CalendarRepository
import com.weathercalendar.data.repository.EventRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程天气预警引擎 — 将未来日程与天气预报交叉匹配，生成预警。
 *
 * 特性：
 * 1. 室内日程关键词过滤 — "Zoom"、"线上会议"等不触发预警
 * 2. 降水量阈值 — precip > 10mm 触发
 * 3. 全天 vs 小时级精确匹配 — 今天的定时事件用逐小时预报
 * 4. 灾害预警 × 日程关联 — 气象局预警信号直接触发
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

        /** 降水量阈值 mm */
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
     * @param hourlyForecasts 今天的逐小时预报（用于精确匹配定时事件）
     * @param warnings 当前生效的气象灾害预警
     * @return 按日期排序的预警列表（最多 5 条）
     */
    suspend fun generateAlerts(
        dailyForecasts: List<DailyWeather>,
        hourlyForecasts: List<HourlyForecast> = emptyList(),
        warnings: List<WeatherWarning> = emptyList(),
    ): List<WeatherAlert> {
        if (dailyForecasts.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val endDate = today.plusDays(14)

        // 构建日期 → 天气映射
        val weatherByDate = dailyForecasts.associateBy { it.date }

        // 构建小时 → 天气映射（仅今天）
        val hourlyByTime = hourlyForecasts.associateBy { it.time.hour }

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

        // 交叉匹配：事件 × 天气（过滤室内事件）
        val alerts = mutableListOf<WeatherAlert>()

        for (event in allEvents) {
            // 室内日程过滤
            if (isIndoorEvent(event.title)) continue

            val weather = weatherByDate[event.date] ?: continue

            // 精确匹配逻辑：今天的定时事件用小时级数据
            val conditions = if (event.date == today && event.time != null && hourlyByTime.isNotEmpty()) {
                detectBadWeatherHourly(event.time, hourlyByTime, weather)
            } else {
                detectBadWeather(weather)
            }

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

        // 灾害预警 × 日程关联：如果有气象局预警，为所有今天/明天的户外事件追加预警
        if (warnings.isNotEmpty()) {
            val warningAlert = generateWarningAlerts(warnings, allEvents, today)
            alerts.addAll(warningAlert)
        }

        // 去重（同一事件不重复预警）、排序、截断
        return alerts
            .distinctBy { "${it.eventName}_${it.eventDate}" }
            .sortedWith(compareBy({ it.eventDate }, { it.eventTime ?: LocalTime.MIN }))
            .take(5)
    }

    /**
     * 小时级精确匹配：检查事件时间前后 1 小时的天气。
     */
    private fun detectBadWeatherHourly(
        eventTime: LocalTime,
        hourlyByTime: Map<Int, HourlyForecast>,
        dailyWeather: DailyWeather,
    ): List<BadWeatherCondition> {
        // 检查事件时间 ±1 小时范围内的天气
        val hoursToCheck = listOf(
            eventTime.hour - 1,
            eventTime.hour,
            eventTime.hour + 1,
        ).filter { it in 0..23 }

        val hourlyConditions = hoursToCheck.mapNotNull { hourlyByTime[it]?.condition }

        // 如果小时级数据中有恶劣天气
        val conditions = mutableListOf<BadWeatherCondition>()
        for (condition in hourlyConditions) {
            when (condition) {
                WeatherCondition.STORMY -> conditions.add(BadWeatherCondition.Stormy)
                WeatherCondition.SNOWY -> conditions.add(BadWeatherCondition.Snowy)
                WeatherCondition.RAINY -> conditions.add(BadWeatherCondition.Rainy)
                WeatherCondition.DRIZZLE -> {
                    if (dailyWeather.precip > PRECIP_THRESHOLD) {
                        conditions.add(BadWeatherCondition.Rainy)
                    }
                }
                else -> {}
            }
        }

        // 温度和风速仍用日级数据（小时级没有这些字段）
        if (dailyWeather.tempMax > HEAT_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeHeat(dailyWeather.tempMax))
        }
        if (dailyWeather.tempMin <= COLD_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeCold(dailyWeather.tempMin))
        }
        if (dailyWeather.windSpeed > WIND_THRESHOLD) {
            conditions.add(BadWeatherCondition.StrongWind(dailyWeather.windSpeed))
        }

        return conditions.distinct()
    }

    /**
     * 灾害预警 × 日程关联。
     * 如果气象局发布了预警（暴雨/大风/高温等），为今天和明天的户外事件生成预警。
     */
    private fun generateWarningAlerts(
        warnings: List<WeatherWarning>,
        allEvents: List<CalendarEvent>,
        today: LocalDate,
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        val tomorrow = today.plusDays(1)

        // 只关注今天和明天的事件（预警通常是短期的）
        val relevantEvents = allEvents.filter {
            (it.date == today || it.date == tomorrow) && !isIndoorEvent(it.title)
        }

        if (relevantEvents.isEmpty()) return emptyList()

        // 取最严重的预警
        val topWarning = warnings.firstOrNull() ?: return emptyList()
        val warningText = "${topWarning.typeName}${topWarning.level}预警"

        for (event in relevantEvents) {
            alerts.add(
                WeatherAlert(
                    eventName = truncateName(event.title),
                    eventDate = event.date,
                    eventTime = event.time,
                    weatherLabel = warningText,
                    triggerMetric = "气象局${topWarning.level}预警",
                    suggestion = "关注预警信息，注意安全",
                    relativeDateText = getRelativeDateText(today, event.date),
                )
            )
        }

        return alerts
    }

    /**
     * 检测某天的恶劣天气条件（日级数据）。
     */
    private fun detectBadWeather(weather: DailyWeather): List<BadWeatherCondition> {
        val conditions = mutableListOf<BadWeatherCondition>()

        when (weather.condition) {
            WeatherCondition.STORMY -> conditions.add(BadWeatherCondition.Stormy)
            WeatherCondition.SNOWY -> conditions.add(BadWeatherCondition.Snowy)
            WeatherCondition.RAINY -> conditions.add(BadWeatherCondition.Rainy)
            WeatherCondition.DRIZZLE -> {
                if (weather.precip > PRECIP_THRESHOLD) {
                    conditions.add(BadWeatherCondition.Rainy)
                }
            }
            else -> {
                if (weather.precip > PRECIP_THRESHOLD) {
                    conditions.add(BadWeatherCondition.Rainy)
                }
            }
        }

        if (weather.tempMax > HEAT_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeHeat(weather.tempMax))
        }
        if (weather.tempMin <= COLD_THRESHOLD) {
            conditions.add(BadWeatherCondition.ExtremeCold(weather.tempMin))
        }
        if (weather.windSpeed > WIND_THRESHOLD) {
            conditions.add(BadWeatherCondition.StrongWind(weather.windSpeed))
        }

        return conditions
    }

    private fun isIndoorEvent(title: String): Boolean {
        val lower = title.lowercase()
        return INDOOR_KEYWORDS.any { lower.contains(it) }
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
