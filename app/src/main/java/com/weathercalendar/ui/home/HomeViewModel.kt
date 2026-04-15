package com.weathercalendar.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathercalendar.data.location.LocationService
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DayInfo
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.model.WeatherDetails
import com.weathercalendar.data.repository.CalendarRepository
import com.weathercalendar.data.repository.CityRepository
import com.weathercalendar.data.repository.EventRepository
import com.weathercalendar.data.repository.SavedCity
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.data.repository.UserPrefs
import com.weathercalendar.data.repository.UserPrefsRepository
import com.weathercalendar.data.repository.WeatherData
import com.weathercalendar.data.repository.WeatherRepository
import com.weathercalendar.util.LunarCalendar
import com.weathercalendar.widget.WeatherWidgetDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cityName: String = "定位中...",
    val dateText: String = "",
    val lunarText: String = "",
    val currentWeather: CurrentWeather = CurrentWeather(0, 0, WeatherCondition.SUNNY),
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val threeDays: List<DayInfo> = emptyList(),
    val weatherDetails: WeatherDetails = WeatherDetails(0, 0, "—", "—"),
    val rainForecast: RainForecast? = null,
    val fromCache: Boolean = false,
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val calendarRepository: CalendarRepository,
    private val cityRepository: CityRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val locationService: LocationService,
    private val eventRepository: EventRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val prefs get() =
        appContext.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)

    init {
        loadData()
    }

    /**
     * 两阶段加载：
     * 1. 瞬间：用历史坐标 + Room 缓存立即渲染（isLoading = false）
     * 2. 后台：GPS 定位 + API 刷新，完成后静默更新 UI
     */
    fun loadData() {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        val dateText = today.format(DateTimeFormatter.ofPattern("M月d日")) + " $dayOfWeek"
        val lunarText = LunarCalendar.getDisplayText(today)
        _uiState.update { it.copy(dateText = dateText, lunarText = lunarText, error = null) }

        // ── 阶段一：瞬间渲染缓存数据 ──
        viewModelScope.launch {
            val userPrefs = userPrefsRepository.prefs.first()
            val savedCity = cityRepository.selectedCity.first()

            // 确定快速坐标（不等 GPS）
            val quickLat: Double?
            val quickLon: Double?
            val quickCity: String?

            if (savedCity != null && savedCity.name.isNotEmpty()) {
                quickLat = savedCity.latitude
                quickLon = savedCity.longitude
                quickCity = savedCity.name
            } else {
                quickLat = prefs.getString("widget_city_lat", null)?.toDoubleOrNull()
                quickLon = prefs.getString("widget_city_lon", null)?.toDoubleOrNull()
                quickCity = prefs.getString("widget_city_name", null)
            }

            if (quickLat != null && quickLon != null && quickCity != null) {
                _uiState.update { it.copy(cityName = quickCity) }
                val cachedResult = weatherRepository.getWeather(quickLat, quickLon)
                cachedResult.getOrNull()?.let { weatherData ->
                    renderWeatherData(weatherData, today, userPrefs)
                    // 阶段一完成，UI 已经有内容了
                }
            }

            // ── 阶段二：后台静默刷新 ──
            val freshLat: Double
            val freshLon: Double
            val freshCity: String

            if (savedCity != null && savedCity.name.isNotEmpty()) {
                // 已选城市，不需要 GPS
                freshLat = savedCity.latitude
                freshLon = savedCity.longitude
                freshCity = savedCity.name
            } else if (userPrefs.useLocation) {
                // 后台 GPS 定位
                val locationResult = locationService.getCurrentLocation()
                val location = locationResult.getOrNull()
                if (location != null) {
                    freshLat = location.latitude
                    freshLon = location.longitude
                    freshCity = location.cityName
                } else if (quickLat != null && quickLon != null && quickCity != null) {
                    // GPS 失败，继续用历史位置
                    freshLat = quickLat
                    freshLon = quickLon
                    freshCity = quickCity
                } else {
                    freshLat = userPrefs.defaultCityLat
                    freshLon = userPrefs.defaultCityLon
                    freshCity = userPrefs.defaultCityName
                }
            } else {
                freshLat = userPrefs.defaultCityLat
                freshLon = userPrefs.defaultCityLon
                freshCity = userPrefs.defaultCityName
            }

            // 保存最新位置
            WeatherWidgetDataProvider.saveCityForWidget(appContext, freshCity, freshLat, freshLon)
            _uiState.update { it.copy(cityName = freshCity) }

            // 获取最新天气
            val weatherResult = weatherRepository.getWeather(freshLat, freshLon)
            val weatherData = weatherResult.getOrElse { e ->
                // 如果阶段一已经有数据，静默失败
                if (_uiState.value.hourlyForecast.isNotEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, error = "天气加载失败: ${e.message}")
                }
                return@launch
            }

            renderWeatherData(weatherData, today, userPrefs)
        }
    }

    private suspend fun renderWeatherData(
        weatherData: WeatherData,
        today: LocalDate,
        userPrefs: UserPrefs,
    ) {
        val endDate = today.plusDays(6)

        // 系统日历事件
        val eventsMap = try {
            calendarRepository.getEventsGroupedByDate(today, endDate)
        } catch (_: Exception) {
            emptyMap()
        }

        // App 内事件
        val appEventsMap = try {
            eventRepository.getEventsBetween(today, endDate).groupBy { it.date }
        } catch (_: Exception) {
            emptyMap()
        }

        // 合并事件
        val allDates = (eventsMap.keys + appEventsMap.keys).toSet()
        val mergedEventsMap = allDates.associateWith { date ->
            (eventsMap[date] ?: emptyList()) + (appEventsMap[date] ?: emptyList())
        }

        val days = weatherData.daily.take(7).map { daily ->
            val dayEvents = mergedEventsMap[daily.date] ?: emptyList()
            DayInfo(
                date = daily.date,
                weather = daily,
                events = dayEvents,
                lunarDate = LunarCalendar.getDisplayText(daily.date),
                lunarFestival = if (LunarCalendar.isFestival(daily.date))
                    LunarCalendar.getDisplayText(daily.date) else null,
            )
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                currentWeather = weatherData.current,
                hourlyForecast = weatherData.hourly,
                threeDays = days,
                weatherDetails = weatherData.details,
                rainForecast = weatherData.rainForecast,
                fromCache = weatherData.fromCache,
                tempUnit = userPrefs.temperatureUnit,
            )
        }
    }

    fun selectCity(city: SavedCity) {
        viewModelScope.launch {
            cityRepository.setSelectedCity(city)
            _uiState.update { it.copy(cityName = city.name) }
            loadData()
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch { loadData() }
    }
}
