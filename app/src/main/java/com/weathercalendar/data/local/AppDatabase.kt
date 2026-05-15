package com.weathercalendar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WeatherEntity::class, EventEntity::class, PoetryFavoriteEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun eventDao(): EventDao
    abstract fun poetryFavoriteDao(): PoetryFavoriteDao

    companion object {
        /** v1 → v2: 添加 events 表 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT,
                        color INTEGER NOT NULL DEFAULT 4283215696,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /** v2 → v3: 添加 description 和 reminderMinutes 字段 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE events ADD COLUMN reminderMinutes INTEGER")
            }
        }

        /** v3 → v4: 添加 recurrenceRule 字段 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN recurrenceRule TEXT")
            }
        }

        /** v4 → v5: 添加 poetry_favorites 表 */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS poetry_favorites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        verse TEXT NOT NULL,
                        source TEXT NOT NULL,
                        fullText TEXT NOT NULL DEFAULT '',
                        collectedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
