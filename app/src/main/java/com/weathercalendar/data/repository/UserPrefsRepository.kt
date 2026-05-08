package com.weathercalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class TemperatureUnit(val label: String, val symbol: String) {
    CELSIUS("摄氏度", "°C"),
    FAHRENHEIT("华氏度", "°F"),
}

enum class ThemeMode(val label: String) {
    FOLLOW_SYSTEM("跟随系统"),
    ALWAYS_LIGHT("浅色"),
    ALWAYS_DARK("深色"),
}

data class UserPrefs(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val useLocation: Boolean = true,
    val weatherNotification: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val defaultCityName: String = "北京",
    val defaultCityLat: Double = 39.9042,
    val defaultCityLon: Double = 116.4074,
)

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.userPrefsStore

    companion object {
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_USE_LOCATION = booleanPreferencesKey("use_location")
        private val KEY_WEATHER_NOTIFICATION = booleanPreferencesKey("weather_notification")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DEFAULT_CITY = stringPreferencesKey("default_city")
        private val KEY_DEFAULT_LAT = stringPreferencesKey("default_lat")
        private val KEY_DEFAULT_LON = stringPreferencesKey("default_lon")
    }

    val prefs: Flow<UserPrefs> = store.data.map { p ->
        UserPrefs(
            temperatureUnit = try {
                TemperatureUnit.valueOf(p[KEY_TEMP_UNIT] ?: "CELSIUS")
            } catch (_: Exception) {
                TemperatureUnit.CELSIUS
            },
            useLocation = p[KEY_USE_LOCATION] ?: true,
            weatherNotification = p[KEY_WEATHER_NOTIFICATION] ?: false,
            themeMode = try {
                ThemeMode.valueOf(p[KEY_THEME_MODE] ?: "FOLLOW_SYSTEM")
            } catch (_: Exception) { ThemeMode.FOLLOW_SYSTEM },
            defaultCityName = p[KEY_DEFAULT_CITY] ?: "北京",
            defaultCityLat = p[KEY_DEFAULT_LAT]?.toDoubleOrNull() ?: 39.9042,
            defaultCityLon = p[KEY_DEFAULT_LON]?.toDoubleOrNull() ?: 116.4074,
        )
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        store.edit { it[KEY_TEMP_UNIT] = unit.name }
    }

    suspend fun setUseLocation(enabled: Boolean) {
        store.edit { it[KEY_USE_LOCATION] = enabled }
    }

    suspend fun setWeatherNotification(enabled: Boolean) {
        store.edit { it[KEY_WEATHER_NOTIFICATION] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDefaultCity(name: String, lat: Double, lon: Double) {
        store.edit {
            it[KEY_DEFAULT_CITY] = name
            it[KEY_DEFAULT_LAT] = lat.toString()
            it[KEY_DEFAULT_LON] = lon.toString()
        }
    }
}
