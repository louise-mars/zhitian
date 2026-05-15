package com.weathercalendar.data.repository

import com.weathercalendar.data.local.PoetryFavoriteDao
import com.weathercalendar.data.local.PoetryFavoriteEntity
import com.weathercalendar.util.DailyPoetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class FavoritePoetry(
    val id: Long,
    val verse: String,
    val source: String,
    val fullText: String,
    val collectedAt: Long,
)

/**
 * 诗词收藏仓库 — 管理用户收藏的古诗词。
 */
@Singleton
class PoetryRepository @Inject constructor(
    private val dao: PoetryFavoriteDao,
) {
    /** 观察所有收藏（按收藏时间倒序） */
    fun observeFavorites(): Flow<List<FavoritePoetry>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toFavoritePoetry() }
        }
    }

    /** 检查某首诗是否已收藏 */
    suspend fun isFavorite(poetry: DailyPoetry.Poetry): Boolean {
        return dao.countByVerseAndSource(poetry.verse, poetry.source) > 0
    }

    /** 收藏一首诗 */
    suspend fun addFavorite(poetry: DailyPoetry.Poetry) {
        dao.insert(
            PoetryFavoriteEntity(
                verse = poetry.verse,
                source = poetry.source,
                fullText = poetry.fullText,
            )
        )
    }

    /** 取消收藏 */
    suspend fun removeFavorite(id: Long) {
        dao.deleteById(id)
    }

    /** 按 verse + source 取消收藏 */
    suspend fun removeFavoriteByContent(poetry: DailyPoetry.Poetry) {
        val all = dao.getAll()
        val match = all.find { it.verse == poetry.verse && it.source == poetry.source }
        if (match != null) {
            dao.deleteById(match.id)
        }
    }

    private fun PoetryFavoriteEntity.toFavoritePoetry() = FavoritePoetry(
        id = id,
        verse = verse,
        source = source,
        fullText = fullText,
        collectedAt = collectedAt,
    )
}
