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
import com.weathercalendar.widget.WidgetRefreshWorker
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
    val airQuality: com.weathercalendar.data.model.AirQuality? = null,
    val lifeIndices: List<com.weathercalendar.data.model.LifeIndex> = emptyList(),
    val todayEvents: List<com.weathercalendar.data.model.CalendarEvent> = emptyList(),
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

    /** 标记阶段二（网络刷新）是否已完成，防止阶段一覆盖新数据 */
    @Volatile
    private var freshDataLoaded = false

    /** 当前加载任务，防止重复刷新 */
    private var loadJob: kotlinx.coroutines.Job? = null

    private val sp get() =
        appContext.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)

    init {
        // 监听温度单位变化，实时更新 UI
        viewModelScope.launch {
            userPrefsRepository.prefs.collect { prefs ->
                _uiState.update { it.copy(tempUnit = prefs.temperatureUnit) }
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        // 如果已经在加载中且不是强制刷新，不重复触发
        if (!forceRefresh && loadJob?.isActive == true) return

        // 强制刷新时才取消正在进行的任务
        if (forceRefresh) {
            loadJob?.cancel()
        }

        freshDataLoaded = false
        loadJob = viewModelScope.launch {
            // 立即更新日期（纯计算，零延迟）
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            val dateText = today.format(DateTimeFormatter.ofPattern("M月d日")) + " $dayOfWeek"
            val lunarText = LunarCalendar.getDisplayText(today)
            _uiState.update { it.copy(dateText = dateText, lunarText = lunarText, error = null, isLoading = true) }

            // 读一次 prefs，传给两个阶段
            val userPrefs = userPrefsRepository.prefs.first()

            // ── 阶段一：瞬间渲染（独立协程，不等任何网络） ──
            launch {
                loadCachedData(today, userPrefs)
            }

            // ── 阶段二：定位 + 网络刷新 ──
            launch {
                refreshFreshData(today, userPrefs, forceRefresh)
            }
        }
    }

    /**
     * 阶段一：纯本地操作，零网络。
     * SharedPreferences 读坐标 → Room 读缓存 → 立即渲染。
     * 如果没有缓存位置，不做任何事（等阶段二 GPS 定位）。
     */
    private suspend fun loadCachedData(today: LocalDate, userPrefs: UserPrefs) {

        // 只用已有的缓存位置，不使用硬编码默认值
        val cachedLat = sp.getString("widget_city_lat", null)?.toDoubleOrNull()
        val cachedLon = sp.getString("widget_city_lon", null)?.toDoubleOrNull()
        val cachedCity = sp.getString("widget_city_name", null)

        if (cachedLat == null || cachedLon == null || cachedCity == null) {
            // 没有缓存位置，等阶段二 GPS 定位
            return
        }

        // 只有在阶段二尚未完成时才用缓存数据
        if (freshDataLoaded) return

        _uiState.update { it.copy(cityName = cachedCity) }

        // getWeather 在缓存未过期时只读 Room，零网络
        val result = weatherRepository.getWeather(cachedLat, cachedLon)
        result.getOrNull()?.let { data ->
            if (!freshDataLoaded) {
                renderWeatherData(data, today, userPrefs)
            }
        }
    }

    /**
     * 阶段二：定位 + 天气请求。
     * 非强制刷新时，如果缓存有效则跳过网络请求。
     */
    private suspend fun refreshFreshData(today: LocalDate, userPrefs: UserPrefs, forceRefresh: Boolean) {

        // 尝试定位（高德 SDK，通常 1-3 秒）
        val locResult = locationService.getCurrentLocation()
        val loc = locResult.getOrNull()

        if (loc == null) {
            val failReason = locResult.exceptionOrNull()?.message ?: "未知原因"
            android.util.Log.w("HomeViewModel", "定位失败: $failReason")
            // 如果有缓存位置，用缓存；否则用默认城市
            val cachedLat = sp.getString("widget_city_lat", null)?.toDoubleOrNull()
            val cachedLon = sp.getString("widget_city_lon", null)?.toDoubleOrNull()
            val cachedCity = sp.getString("widget_city_name", null)

            val lat: Double
            val lon: Double
            val city: String

            if (cachedLat != null && cachedLon != null && cachedCity != null) {
                lat = cachedLat; lon = cachedLon; city = cachedCity
            } else {
                // 使用默认城市（设置中可配置）
                lat = userPrefs.defaultCityLat
                lon = userPrefs.defaultCityLon
                city = userPrefs.defaultCityName
            }

            _uiState.update { it.copy(cityName = city) }
            val result = weatherRepository.getWeather(lat, lon, forceRefresh = forceRefresh)
            result.getOrNull()?.let { data ->
                freshDataLoaded = true
                renderWeatherData(data, today, userPrefs)
            } ?: _uiState.update { it.copy(isLoading = false, error = "天气加载失败，请检查网络") }
            return
        }

        // 定位成功
        val lat = loc.latitude
        val lon = loc.longitude
        val city = loc.cityName
        WeatherWidgetDataProvider.saveCityForWidget(appContext, city, lat, lon)
        _uiState.update { it.copy(cityName = city) }

        // 请求天气（非强制刷新时利用缓存）
        val result = weatherRepository.getWeather(lat, lon, forceRefresh = forceRefresh)
        val data = result.getOrElse { e ->
            if (_uiState.value.hourlyForecast.isNotEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
            val errorMsg = when (e) {
                is java.io.IOException -> "网络连接失败"
                else -> "天气加载失败"
            }
            _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            return
        }

        freshDataLoaded = true
        renderWeatherData(data, today, userPrefs)

        // 触发 Widget 刷新
        WidgetRefreshWorker.enqueue(appContext)
    }

    private suspend fun renderWeatherData(
        weatherData: WeatherData,
        today: LocalDate,
        userPrefs: UserPrefs,
    ) {
        val endDate = today.plusDays(14)

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

        val days = weatherData.daily.map { daily ->
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
                airQuality = weatherData.airQuality,
                lifeIndices = weatherData.lifeIndices,
                todayEvents = mergedEventsMap[today] ?: emptyList(),
                fromCache = weatherData.fromCache,
                tempUnit = userPrefs.temperatureUnit,
            )
        }
    }

    fun selectCity(city: SavedCity) {
        viewModelScope.launch {
            cityRepository.setSelectedCity(city)
            // 临时显示该城市天气，下次刷新仍回到 GPS
            _uiState.update { it.copy(cityName = city.name, isLoading = true) }
            val userPrefs = userPrefsRepository.prefs.first()
            val today = LocalDate.now()
            WeatherWidgetDataProvider.saveCityForWidget(appContext, city.name, city.latitude, city.longitude)
            val result = weatherRepository.getWeather(city.latitude, city.longitude, forceRefresh = true)
            result.getOrNull()?.let { data ->
                renderWeatherData(data, today, userPrefs)
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            loadData()
        }
    }
}
