package com.weathercalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weathercalendar.ui.navigation.WeatherCalendarNavHost
import com.weathercalendar.ui.settings.SettingsViewModel
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val userPrefs by settingsViewModel.uiState.collectAsStateWithLifecycle()

            WeatherCalendarTheme(themeMode = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherCalendarNavHost()
                }
            }
        }
    }
}
