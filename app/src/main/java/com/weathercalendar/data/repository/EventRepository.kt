package com.weathercalendar.data.repository

import android.content.Context
import com.weathercalendar.data.local.EventDao
import com.weathercalendar.data.local.EventEntity
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.notification.EventReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App 内事件仓库 — CRUD 操作，独立于系统日历。
 * 创建/更新事件时自动安排提醒通知。
 */
@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    @ApplicationContext private val context: Context,
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 获取指定日期范围内的事件（包括展开重复事件） */
    suspend fun getEventsBetween(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> {
        val entities = eventDao.getBetween(startDate.toString(), endDate.toString())
        val nonRecurring = entities.filter { it.recurrenceRule.isNullOrBlank() || it.recurrenceRule == "none" }
            .map { it.toCalendarEvent() }

        // 重复事件：用专用查询，只获取有重复规则且起始日期在范围之前的
        val allRecurring = eventDao.getRecurringBefore(endDate.toString())
        val expanded = expandRecurringEvents(allRecurring, startDate, endDate)

        return (nonRecurring + expanded).sortedWith(compareBy({ it.date }, { it.time }))
    }

    /** 获取指定日期的事件 */
    suspend fun getEventsByDate(date: LocalDate): List<CalendarEvent> {
        val entities = eventDao.getByDate(date.toString())
        return entities.map { it.toCalendarEvent() }
    }

    /** 获取有事件的日期列表 */
    suspend fun getDatesWithEvents(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        return eventDao.getDatesWithEvents(startDate.toString(), endDate.toString())
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
    }

    /** 添加事件 */
    suspend fun addEvent(
        title: String,
        date: LocalDate,
        time: LocalTime? = null,
        description: String = "",
        reminderMinutes: Int? = null,
        recurrenceRule: String? = null,
        color: Long = 0xFF4CAF50,
    ): Long {
        val entity = EventEntity(
            title = title,
            description = description,
            date = date.toString(),
            time = time?.format(timeFormatter),
            reminderMinutes = reminderMinutes,
            recurrenceRule = recurrenceRule,
            color = color,
        )
        val id = eventDao.insert(entity)

        if (reminderMinutes != null) {
            EventReminderWorker.schedule(context, id, title, description, date, time, reminderMinutes)
        }

        return id
    }

    /** 更新事件 */
    suspend fun updateEvent(
        id: Long,
        title: String,
        date: LocalDate,
        time: LocalTime? = null,
        description: String = "",
        reminderMinutes: Int? = null,
        recurrenceRule: String? = null,
        color: Long = 0xFF4CAF50,
    ) {
        val entity = EventEntity(
            id = id,
            title = title,
            description = description,
            date = date.toString(),
            time = time?.format(timeFormatter),
            reminderMinutes = reminderMinutes,
            recurrenceRule = recurrenceRule,
            color = color,
        )
        eventDao.update(entity)

        EventReminderWorker.cancel(context, id)
        if (reminderMinutes != null) {
            EventReminderWorker.schedule(context, id, title, description, date, time, reminderMinutes)
        }
    }

    /** 删除事件 */
    suspend fun deleteEvent(id: Long) {
        EventReminderWorker.cancel(context, id)
        eventDao.deleteById(id)
    }

    /** 观察所有事件（Flow） */
    fun observeAll(): Flow<List<CalendarEvent>> {
        return eventDao.observeAll().map { entities ->
            entities.map { it.toCalendarEvent() }
        }
    }

    private fun EventEntity.toCalendarEvent(): CalendarEvent {
        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            date = LocalDate.parse(date),
            time = time?.let { LocalTime.parse(it, timeFormatter) },
            reminderMinutes = reminderMinutes,
            color = color,
        )
    }

    /**
     * 展开重复事件为指定日期范围内的实例。
     */
    private fun expandRecurringEvents(
        events: List<EventEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        for (entity in events) {
            val rule = entity.recurrenceRule ?: continue
            val eventStart = LocalDate.parse(entity.date)
            val eventTime = entity.time?.let { LocalTime.parse(it, timeFormatter) }

            var current = eventStart
            while (current <= endDate) {
                if (current >= startDate && matchesRule(current, eventStart, rule)) {
                    result.add(CalendarEvent(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        date = current,
                        time = eventTime,
                        reminderMinutes = entity.reminderMinutes,
                        color = entity.color,
                    ))
                }
                current = current.plusDays(1)
            }
        }
        return result
    }

    private fun matchesRule(date: LocalDate, eventStart: LocalDate, rule: String): Boolean {
        if (date.isBefore(eventStart)) return false
        return when {
            rule == "daily" -> true
            rule == "weekly" -> date.dayOfWeek == eventStart.dayOfWeek
            rule == "monthly" -> {
                // 处理月末情况：1月31日 → 2月取当月最后一天
                val targetDay = eventStart.dayOfMonth
                val lastDayOfMonth = date.lengthOfMonth()
                date.dayOfMonth == targetDay.coerceAtMost(lastDayOfMonth) &&
                    (targetDay <= lastDayOfMonth || date.dayOfMonth == lastDayOfMonth)
            }
            rule == "yearly" -> {
                // 处理闰年：2月29日 → 非闰年取2月28日
                val targetMonth = eventStart.monthValue
                val targetDay = eventStart.dayOfMonth
                if (date.monthValue != targetMonth) return false
                val lastDayOfMonth = date.lengthOfMonth()
                date.dayOfMonth == targetDay.coerceAtMost(lastDayOfMonth)
            }
            rule.startsWith("custom:") -> {
                val days = rule.removePrefix("custom:").split(",").mapNotNull { it.trim().toIntOrNull() }
                date.dayOfWeek.value in days  // 1=Monday ... 7=Sunday
            }
            else -> date == eventStart
        }
    }
}
