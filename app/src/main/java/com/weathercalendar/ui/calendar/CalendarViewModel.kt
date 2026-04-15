package com.weathercalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathercalendar.data.location.LocationService
import com.weathercalendar.data.model.CalendarDayCell
import com.weathercalendar.data.model.CalendarEvent
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.repository.CalendarRepository
import com.weathercalendar.data.repository.CityRepository
import com.weathercalendar.data.repository.WeatherRepository
import com.weathercalendar.util.LunarCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val currentMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDayCell> = emptyList(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val eventsMap: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val weatherMap: Map<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>> = emptyMap(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val calendarRepository: CalendarRepository,
    private val cityRepository: CityRepository,
    private val locationService: LocationService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
    }

    fun changeMonth(delta: Int) {
        val newMonth = _uiState.value.currentMonth.plusMonths(delta.toLong())
        loadMonth(newMonth)
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentMonth = month) }

            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            val today = LocalDate.now()

            // 月份标签
            val monthLabel = "${month.year}年${month.monthValue}月"

            // 日历事件
            val eventsMap = try {
                calendarRepository.getEventsGroupedByDate(startDate, endDate)
            } catch (_: Exception) {
                emptyMap()
            }

            // 天气数据（仅预报范围内的日期）
            val weatherMap = mutableMapOf<LocalDate, Pair<WeatherCondition, Pair<Int, Int>>>()
            try {
                val savedCity = cityRepository.selectedCity.first()
                val lat: Double
                val lon: Double
                if (savedCity != null && savedCity.name.isNotEmpty()) {
                    lat = savedCity.latitude
                    lon = savedCity.longitude
                } else {
                    val loc = locationService.getCurrentLocation().getOrNull()
                    lat = loc?.latitude ?: 0.0
                    lon = loc?.longitude ?: 0.0
                }
                if (lat != 0.0 || lon != 0.0) {
                    val weatherData = weatherRepository.getWeather(lat, lon).getOrNull()
                    weatherData?.daily?.forEach { daily ->
                        weatherMap[daily.date] = daily.condition to (daily.tempMin to daily.tempMax)
                    }
                }
            } catch (_: Exception) {
                // 天气加载失败不影响日历显示
            }

            // 构建日期格子
            val firstDayOfWeek = startDate.dayOfWeek
            val days = (1..endDate.dayOfMonth).map { day ->
                val date = month.atDay(day)
                val hasEvents = eventsMap.containsKey(date)
                val weatherIcon = weatherMap[date]?.first?.icon

                CalendarDayCell(
                    date = date,
                    lunarText = LunarCalendar.getDisplayText(date),
                    isLunarFestival = LunarCalendar.isFestival(date) || LunarCalendar.isSolarTerm(date),
                    weatherIcon = weatherIcon,
                    hasEvents = hasEvents,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthLabel = monthLabel,
                    days = days,
                    firstDayOfWeek = firstDayOfWeek,
                    eventsMap = eventsMap,
                    weatherMap = weatherMap,
                )
            }
        }
    }
}
