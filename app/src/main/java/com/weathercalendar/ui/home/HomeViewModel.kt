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
import com.weathercalendar.data.model.WeatherWarning
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
    val warnings: List<WeatherWarning> = emptyList(),
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

    private val sp get() =
        appContext.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)

    init {
        loadData()
    }

    fun loadData() {
        // 立即更新日期（纯计算，零延迟）
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        val dateText = today.format(DateTimeFormatter.ofPattern("M月d日")) + " $dayOfWeek"
        val lunarText = LunarCalendar.getDisplayText(today)
        _uiState.update { it.copy(dateText = dateText, lunarText = lunarText, error = null) }

        // ── 阶段一：瞬间渲染（独立协程，不等任何网络） ──
        viewModelScope.launch {
            loadCachedData(today)
        }

        // ── 阶段二：后台静默刷新（独立协程，和阶段一并行） ──
        viewModelScope.launch {
            refreshFreshData(today)
        }
    }

    /**
     * 阶段一：纯本地操作，零网络。
     * SharedPreferences 读坐标 → Room 读缓存 → 立即渲染。
     */
    private suspend fun loadCachedData(today: LocalDate) {
        val userPrefs = userPrefsRepository.prefs.first()
        val savedCity = cityRepository.selectedCity.first()

        val lat: Double
        val lon: Double
        val city: String

        if (savedCity != null && savedCity.name.isNotEmpty()) {
            lat = savedCity.latitude
            lon = savedCity.longitude
            city = savedCity.name
        } else {
            lat = sp.getString("widget_city_lat", null)?.toDoubleOrNull() ?: return
            lon = sp.getString("widget_city_lon", null)?.toDoubleOrNull() ?: return
            city = sp.getString("widget_city_name", null) ?: return
        }

        _uiState.update { it.copy(cityName = city) }

        // getWeather 在缓存未过期时只读 Room，零网络
        val result = weatherRepository.getWeather(lat, lon)
        result.getOrNull()?.let { data ->
            renderWeatherData(data, today, userPrefs)
        }
    }

    /**
     * 阶段二：网络刷新。GPS + API，完成后静默更新 UI。
     * 和阶段一并行执行，不阻塞 UI。
     */
    private suspend fun refreshFreshData(today: LocalDate) {
        val userPrefs = userPrefsRepository.prefs.first()
        val savedCity = cityRepository.selectedCity.first()

        val lat: Double
        val lon: Double
        val city: String

        if (savedCity != null && savedCity.name.isNotEmpty()) {
            lat = savedCity.latitude
            lon = savedCity.longitude
            city = savedCity.name
        } else if (userPrefs.useLocation) {
            // GPS 定位（可能耗时几秒，但不阻塞 UI）
            val locResult = locationService.getCurrentLocation()
            val loc = locResult.getOrNull()
            if (loc != null) {
                lat = loc.latitude
                lon = loc.longitude
                city = loc.cityName
            } else {
                // GPS 失败，用历史位置或默认
                lat = sp.getString("widget_city_lat", null)?.toDoubleOrNull() ?: userPrefs.defaultCityLat
                lon = sp.getString("widget_city_lon", null)?.toDoubleOrNull() ?: userPrefs.defaultCityLon
                city = sp.getString("widget_city_name", null) ?: userPrefs.defaultCityName
            }
        } else {
            lat = userPrefs.defaultCityLat
            lon = userPrefs.defaultCityLon
            city = userPrefs.defaultCityName
        }

        // 保存位置
        WeatherWidgetDataProvider.saveCityForWidget(appContext, city, lat, lon)
        _uiState.update { it.copy(cityName = city) }

        // 强制刷新天气（即使缓存未过期，阶段二也会触发网络请求）
        val result = weatherRepository.getWeather(lat, lon)
        val data = result.getOrElse { e ->
            // 阶段一已经有数据了，静默失败
            if (_uiState.value.hourlyForecast.isNotEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
            _uiState.update { it.copy(isLoading = false, error = "天气加载失败: ${e.message}") }
            return
        }

        renderWeatherData(data, today, userPrefs)
    }

    private suspend fun renderWeatherData(
        weatherData: WeatherData,
        today: LocalDate,
        userPrefs: UserPrefs,
    ) {
        val endDate = today.plusDays(6)

        val eventsMap = try {
            calendarRepository.getEventsGroupedByDate(today, endDate)
        } catch (_: Exception) {
            emptyMap()
        }

        val appEventsMap = try {
            eventRepository.getEventsBetween(today, endDate).groupBy { it.date }
        } catch (_: Exception) {
            emptyMap()
        }

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
                warnings = weatherData.warnings,
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
