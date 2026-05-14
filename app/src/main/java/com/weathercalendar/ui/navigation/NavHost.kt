package com.weathercalendar.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weathercalendar.data.repository.SavedCity
import com.weathercalendar.ui.calendar.CalendarScreen
import com.weathercalendar.ui.calendar.CalendarViewModel
import com.weathercalendar.ui.city.CityPickerSheet
import com.weathercalendar.ui.city.CityViewModel
import com.weathercalendar.ui.home.HomeScreen
import com.weathercalendar.ui.home.HomeViewModel
import com.weathercalendar.ui.home.shareWeatherImage
import com.weathercalendar.ui.home.shareWeatherText
import com.weathercalendar.ui.permission.PermissionScreen
import com.weathercalendar.ui.settings.SettingsScreen

object Routes {
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
}

@Composable
fun WeatherCalendarNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showCityPicker by remember { mutableStateOf(false) }

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    val startDestination = if (hasLocationPermission) Routes.HOME else Routes.PERMISSION
    val homeViewModel: HomeViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ── 权限请求页 ──
        composable(Routes.PERMISSION) {
            val navigateToHome = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.PERMISSION) { inclusive = true }
                }
            }
            PermissionScreen(
                onAllGranted = navigateToHome,
                onSkipLocation = navigateToHome, // fallback 到默认城市
            )
        }

        // ── 首页 ──
        composable(Routes.HOME) {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            // 首次进入 HOME 时加载数据（确保权限已授予）
            LaunchedEffect(Unit) {
                homeViewModel.loadData()
            }

            HomeScreen(
                cityName = uiState.cityName,
                dateText = uiState.dateText,
                lunarText = uiState.lunarText,
                currentWeather = uiState.currentWeather,
                hourlyForecast = uiState.hourlyForecast,
                threeDays = uiState.threeDays,
                weatherDetails = uiState.weatherDetails,
                rainForecast = uiState.rainForecast,
                warnings = uiState.warnings,
                todayEvents = uiState.todayEvents,
                airQuality = uiState.airQuality,
                lifeIndices = uiState.lifeIndices,
                isLoading = uiState.isLoading,
                fromCache = uiState.fromCache,
                error = uiState.error,
                tempUnit = uiState.tempUnit,
                weatherAlerts = uiState.weatherAlerts,
                iconAnimationEnabled = uiState.iconAnimationEnabled,
                animationDegraded = uiState.animationDegraded,
                forceDarkGradient = uiState.forceDarkGradient,
                onCityClick = { showCityPicker = true },
                onCalendarClick = { navController.navigate(Routes.CALENDAR) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onShareClick = {
                    try {
                        shareWeatherImage(
                            context = context,
                            cityName = uiState.cityName,
                            dateText = uiState.dateText,
                            currentWeather = uiState.currentWeather,
                            lunarText = uiState.lunarText,
                        )
                    } catch (_: Throwable) {  // Catches both Exception and Error (OOM)
                        shareWeatherText(
                            context = context,
                            cityName = uiState.cityName,
                            dateText = uiState.dateText,
                            currentWeather = uiState.currentWeather,
                            lunarText = uiState.lunarText,
                        )
                    }
                },
                onRefresh = { homeViewModel.loadData(forceRefresh = true) },
            )
        }

        // ── 日历页 ──
        composable(Routes.CALENDAR) {
            val viewModel: CalendarViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CalendarScreen(
                monthLabel = uiState.monthLabel,
                days = uiState.days,
                firstDayOfWeek = uiState.firstDayOfWeek,
                todayEvents = uiState.eventsMap,
                todayWeather = uiState.weatherMap,
                onBack = { navController.popBackStack() },
                onPrevMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onAddEvent = { date, title, description, time, reminder, color, recurrence -> viewModel.addEvent(title, date, time, description, reminder, color, recurrence) },
                onDeleteEvent = { eventId -> viewModel.deleteEvent(eventId) },
                onUpdateEvent = { id, date, title, description, time, reminder, color, recurrence -> viewModel.updateEvent(id, title, date, time, description, reminder, color, recurrence) },
            )
        }

        // ── 设置页 ──
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }

    // ── 城市选择器 BottomSheet ──
    if (showCityPicker) {
        val cityViewModel: CityViewModel = hiltViewModel()
        val cityState by cityViewModel.uiState.collectAsStateWithLifecycle()

        val allCities = buildList {
            cityState.currentLocationCity?.let { add(it) }
            addAll(cityState.savedCities)
        }

        CityPickerSheet(
            cities = allCities,
            currentCityName = homeViewModel.uiState.value.cityName,
            searchResults = cityState.searchResults,
            isSearching = cityState.isSearching,
            searchError = cityState.searchError,
            onSearch = { cityViewModel.search(it) },
            onCitySelected = { city ->
                if (city.isCurrentLocation) {
                    // 选择"当前定位"时，重新 GPS 定位
                    homeViewModel.useCurrentLocation()
                } else {
                    homeViewModel.selectCity(
                        SavedCity(
                            name = city.name,
                            latitude = city.latitude,
                            longitude = city.longitude,
                        )
                    )
                }
                showCityPicker = false
            },
            onAddCity = { cityViewModel.addCity(it) },
            onDeleteCity = { cityViewModel.removeCity(it) },
            onDismiss = { showCityPicker = false },
        )
    }
}
