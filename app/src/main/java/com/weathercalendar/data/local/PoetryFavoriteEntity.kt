package com.weathercalendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 诗词收藏实体 — 用户收藏的古诗词。
 */
@Entity(tableName = "poetry_favorites")
data class PoetryFavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val verse: String,          // 精华一句
    val source: String,         // 出处（诗名·作者）
    val fullText: String = "",  // 完整诗词
    val collectedAt: Long = System.currentTimeMillis(),
)
