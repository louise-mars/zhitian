package com.weathercalendar.data.repository

import com.weathercalendar.data.local.EventDao
import com.weathercalendar.data.local.EventEntity
import com.weathercalendar.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App 内事件仓库 — CRUD 操作，独立于系统日历。
 */
@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 获取指定日期范围内的事件 */
    suspend fun getEventsBetween(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> {
        val entities = eventDao.getBetween(startDate.toString(), endDate.toString())
        return entities.map { it.toCalendarEvent() }
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
        color: Long = 0xFF4CAF50,
    ): Long {
        val entity = EventEntity(
            title = title,
            date = date.toString(),
            time = time?.format(timeFormatter),
            color = color,
        )
        return eventDao.insert(entity)
    }

    /** 更新事件 */
    suspend fun updateEvent(
        id: Long,
        title: String,
        date: LocalDate,
        time: LocalTime? = null,
        color: Long = 0xFF4CAF50,
    ) {
        val entity = EventEntity(
            id = id,
            title = title,
            date = date.toString(),
            time = time?.format(timeFormatter),
            color = color,
        )
        eventDao.update(entity)
    }

    /** 删除事件 */
    suspend fun deleteEvent(id: Long) {
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
            date = LocalDate.parse(date),
            time = time?.let { LocalTime.parse(it, timeFormatter) },
            color = color,
        )
    }
}
