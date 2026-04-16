package com.weathercalendar.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathercalendar.data.remote.GeocodingResult
import com.weathercalendar.data.repository.CityRepository
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.data.repository.UserPrefs
import com.weathercalendar.data.repository.UserPrefsRepository
import com.weathercalendar.notification.WeatherNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val cityRepository: CityRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val uiState: StateFlow<UserPrefs> = userPrefsRepository.prefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs(),
        )

    private val _citySearchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val citySearchResults: StateFlow<List<GeocodingResult>> = _citySearchResults.asStateFlow()

    private var searchJob: Job? = null

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { userPrefsRepository.setTemperatureUnit(unit) }
    }

    fun setUseLocation(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setUseLocation(enabled) }
    }

    fun setWeatherNotification(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setWeatherNotification(enabled)
            appContext.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("weather_notification", enabled)
                .apply()
            if (enabled) {
                WeatherNotificationWorker.enqueue(appContext)
            } else {
                WeatherNotificationWorker.cancel(appContext)
            }
        }
    }

    fun searchCity(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _citySearchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // 防抖
            val results = cityRepository.searchCities(query).getOrDefault(emptyList())
            _citySearchResults.value = results
        }
    }

    fun setDefaultCity(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            userPrefsRepository.setDefaultCity(name, lat, lon)
            _citySearchResults.value = emptyList()
        }
    }
}
