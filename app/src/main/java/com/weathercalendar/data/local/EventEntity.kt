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
    val date: String,       // ISO 格式 "2026-04-16"
    val time: String? = null, // "HH:mm" 格式，null = 全天事件
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis(),
)
