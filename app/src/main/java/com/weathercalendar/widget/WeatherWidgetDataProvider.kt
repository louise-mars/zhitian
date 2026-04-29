package com.weathercalendar.widget

import android.content.Context
import androidx.room.Room
import com.weathercalendar.data.local.AppDatabase
import com.weathercalendar.data.local.WeatherEntity
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.remote.OpenMeteoResponse
import com.weathercalendar.data.remote.WeatherCodeMapper
import com.weathercalendar.util.LunarCalendar
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Widget 数据获取器 — 读 Room 缓存，不直接调 API。
 * App 负责写缓存，Widget 只读。数据源统一，省流量。
 */
object WeatherWidgetDataProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 复用数据库实例，避免每次 Widget 刷新都重新创建
    @Volatile
    private var dbInstance: AppDatabase? = null

    private fun getDatabase(context: Context): AppDatabase {
        return dbInstance ?: synchronized(this) {
            dbInstance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "weather_calendar.db",
            ).fallbackToDestructiveMigration().build().also { dbInstance = it }
        }
    }

    suspend fun fetchData(context: Context): WeatherWidgetData {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)
        val dateText = today.format(DateTimeFormatter.ofPattern("M月d日")) + " $dayOfWeek"
        val lunarText = LunarCalendar.getDisplayText(today)

        val cityInfo = readSavedCity(context)
        val cityName = cityInfo.first
        val lat = cityInfo.second
        val lon = cityInfo.third

        return try {
            val db = getDatabase(context)
            val cacheKey = WeatherEntity.key(lat, lon)
            val cached = db.weatherDao().get(cacheKey)

            if (cached != null) {
                val response = json.decodeFromString<OpenMeteoResponse>(cached.responseJson)
                val current = response.current
                    ?: return fallbackData(dateText, lunarText, cityName)
                val condition = WeatherCodeMapper.fromWmoCode(current.weatherCode)
                val gradient = conditionToGradient(condition)

                // 明日天气摘要
                val tomorrowSummary = buildTomorrowSummary(response, today)

                WeatherWidgetData(
                    temperature = current.temperature.toInt(),
                    weatherIcon = condition.icon,
                    weatherLabel = condition.label,
                    cityName = cityName,
                    dateText = dateText,
                    lunarText = lunarText,
                    tomorrowSummary = tomorrowSummary,
                    gradientStart = gradient.first,
                    gradientEnd = gradient.second,
                )
            } else {
                fallbackData(dateText, lunarText, cityName)
            }
        } catch (_: Exception) {
            fallbackData(dateText, lunarText, cityName)
        }
    }

    private fun buildTomorrowSummary(response: OpenMeteoResponse, today: LocalDate): String? {
        val daily = response.daily ?: return null
        val tomorrowStr = today.plusDays(1).toString()
        val index = daily.time.indexOf(tomorrowStr)
        if (index < 0) return null
        val code = daily.weatherCode.getOrNull(index) ?: return null
        val condition = WeatherCodeMapper.fromWmoCode(code)
        val min = daily.temperatureMin.getOrNull(index)?.toInt() ?: return null
        val max = daily.temperatureMax.getOrNull(index)?.toInt() ?: return null
        return "明天 ${condition.icon} ${condition.label} $min°~$max°"
    }

    private fun fallbackData(dateText: String, lunarText: String, cityName: String) =
        WeatherWidgetData(
            dateText = dateText,
            lunarText = lunarText,
            cityName = cityName,
            weatherLabel = "打开App刷新",
            weatherIcon = "—",
        )

    private fun readSavedCity(context: Context): Triple<String, Double, Double> {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
            val name = prefs.getString("widget_city_name", null)
            val lat = prefs.getString("widget_city_lat", null)?.toDoubleOrNull()
            val lon = prefs.getString("widget_city_lon", null)?.toDoubleOrNull()
            if (name != null && lat != null && lon != null) {
                Triple(name, lat, lon)
            } else {
                Triple("北京", 39.9042, 116.4074)
            }
        } catch (_: Exception) {
            Triple("北京", 39.9042, 116.4074)
        }
    }

    fun saveCityForWidget(context: Context, name: String, lat: Double, lon: Double) {
        context.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
            .edit()
            .putString("widget_city_name", name)
            .putString("widget_city_lat", lat.toString())
            .putString("widget_city_lon", lon.toString())
            .apply()
    }

    private fun conditionToGradient(condition: WeatherCondition): Pair<Long, Long> {
        return when (condition) {
            WeatherCondition.SUNNY, WeatherCondition.PARTLY_CLOUDY -> 0xFF4FACFE to 0xFF00F2FE
            WeatherCondition.CLOUDY -> 0xFF89A0B0 to 0xFF546E7A
            WeatherCondition.FOGGY -> 0xFFB8C6D0 to 0xFF6E8898
            WeatherCondition.DRIZZLE, WeatherCondition.RAINY -> 0xFF5F9EA0 to 0xFF2F4F4F
            WeatherCondition.SNOWY -> 0xFFD4E4F1 to 0xFF8EAFC2
            WeatherCondition.STORMY -> 0xFF3D4E5C to 0xFF1A252F
        }
    }
}
