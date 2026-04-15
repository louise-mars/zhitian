package com.weathercalendar.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.data.repository.UserPrefs
import com.weathercalendar.data.repository.UserPrefsRepository
import com.weathercalendar.notification.WeatherNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val uiState: StateFlow<UserPrefs> = userPrefsRepository.prefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPrefs(),
        )

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { userPrefsRepository.setTemperatureUnit(unit) }
    }

    fun setUseLocation(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setUseLocation(enabled) }
    }

    fun setWeatherNotification(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setWeatherNotification(enabled)
            // 同步到 SharedPreferences 供 Worker 读取
            appContext.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("weather_notification", enabled)
                .apply()
            // 开启/关闭 Worker
            if (enabled) {
                WeatherNotificationWorker.enqueue(appContext)
            } else {
                WeatherNotificationWorker.cancel(appContext)
            }
        }
    }
}
