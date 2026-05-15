package com.weathercalendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoetryFavoriteDao {

    @Query("SELECT * FROM poetry_favorites ORDER BY collectedAt DESC")
    fun observeAll(): Flow<List<PoetryFavoriteEntity>>

    @Query("SELECT * FROM poetry_favorites ORDER BY collectedAt DESC")
    suspend fun getAll(): List<PoetryFavoriteEntity>

    @Query("SELECT COUNT(*) FROM poetry_favorites WHERE verse = :verse AND source = :source")
    suspend fun countByVerseAndSource(verse: String, source: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PoetryFavoriteEntity): Long

    @Delete
    suspend fun delete(entity: PoetryFavoriteEntity)

    @Query("DELETE FROM poetry_favorites WHERE id = :id")
    suspend fun deleteById(id: Long)
}
