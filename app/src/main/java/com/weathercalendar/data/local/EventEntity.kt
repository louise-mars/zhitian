package com.weathercalendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App 内日历事件实体 — 用户自建事件，独立于系统日历。
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",   // 详细描述/行动
    val date: String,               // ISO 格式 "2026-04-16"
    val time: String? = null,       // "HH:mm" 格式，null = 全天事件
    val reminderMinutes: Int? = null, // 提前提醒分钟数，null = 不提醒
    val recurrenceRule: String? = null, // "none"/"daily"/"weekly"/"monthly"/"yearly"/"custom:1,3,5"
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis(),
)
