package com.weathercalendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE date = :date ORDER BY time ASC")
    suspend fun getByDate(date: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, time ASC")
    suspend fun getBetween(startDate: String, endDate: String): List<EventEntity>

    @Query("SELECT DISTINCT date FROM events WHERE date >= :startDate AND date <= :endDate")
    suspend fun getDatesWithEvents(startDate: String, endDate: String): List<String>

    @Query("SELECT * FROM events ORDER BY date ASC, time ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
