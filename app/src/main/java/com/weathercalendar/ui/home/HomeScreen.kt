package com.weathercalendar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.mock.MockData
import com.weathercalendar.data.model.AirQuality
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DayInfo
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.LifeIndex
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.model.WeatherDetails
import com.weathercalendar.data.model.WeatherWarning
import com.weathercalendar.data.repository.TemperatureUnit
import com.weathercalendar.ui.components.WeatherAnimationOverlay
import com.weathercalendar.ui.theme.WeatherCalendarTheme
import com.weathercalendar.ui.theme.WeatherColors
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cityName: String,
    dateText: String,
    lunarText: String,
    currentWeather: CurrentWeather,
    hourlyForecast: List<HourlyForecast>,
    threeDays: List<DayInfo>,
    weatherDetails: WeatherDetails,
    rainForecast: RainForecast? = null,
    warnings: List<WeatherWarning> = emptyList(),
    todayEvents: List<com.weathercalendar.data.model.CalendarEvent> = emptyList(),
    airQuality: AirQuality? = null,
    lifeIndices: List<LifeIndex> = emptyList(),
    isLoading: Boolean = false,
    fromCache: Boolean = false,
    error: String? = null,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    weatherAlerts: List<com.weathercalendar.domain.alert.WeatherAlert> = emptyList(),
    iconAnimationEnabled: Boolean = true,
    animationDegraded: Boolean = false,
    forceDarkGradient: Boolean? = null,
    onCityClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val pageCount = threeDays.size.coerceIn(1, 3)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val focusedDay = threeDays.getOrNull(pagerState.currentPage)
    val focusedCondition = focusedDay?.weather?.condition ?: currentWeather.condition
    val gradient = WeatherColors.gradientFor(focusedCondition, currentWeather.isDay, forceDarkGradient)
    val transitionDuration = if (animationDegraded) 200 else 700
    val animatedStart by animateColorAsState(gradient.start, tween(transitionDuration), label = "s")
    val animatedEnd by animateColorAsState(gradient.end, tween(transitionDuration), label = "e")
    val textColor = WeatherColors.textColorOn(focusedCondition, currentWeather.isDay)

    var fusionExpandedIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState.currentPage) { fusionExpandedIndex = pagerState.currentPage }

    // 折叠状态
    var showForecastDetail by remember { mutableStateOf(true) }
    var showMoreInfo by remember { mutableStateOf(true) }
    var refreshFeedback by remember { mutableStateOf(false) }

    // 刷新完成反馈
    LaunchedEffect(isLoading) {
        if (!isLoading && hourlyForecast.isNotEmpty() && !fromCache) {
            refreshFeedback = true
            kotlinx.coroutines.delay(2000)
            refreshFeedback = false
        }
    }

    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(animatedStart, animatedEnd))),
        ) {
            WeatherAnimationOverlay(condition = focusedCondition, isDay = currentWeather.isDay)

            when {
                isLoading && hourlyForecast.isEmpty() -> SkeletonScreen(textColor)
                error != null && hourlyForecast.isEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        // Show header even in error state so user can access city picker
                        HeaderBar(
                            cityName = cityName, dateText = dateText, lunarText = lunarText,
                            textColor = textColor, onCityClick = onCityClick,
                            onCalendarClick = onCalendarClick, onSettingsClick = onSettingsClick,
                            onShareClick = onShareClick,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                        ErrorContent(error, textColor, onRefresh)
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize().statusBarsPadding()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // ═══ 第一屏：核心信息（不滚动就能看到） ═══

                        HeaderBar(
                            cityName = cityName,
                            dateText = if (pagerState.currentPage == 0) dateText
                                else focusedDay?.date?.let {
                                    val dow = it.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.CHINESE)
                                    it.format(java.time.format.DateTimeFormatter.ofPattern("M月d日")) + " $dow"
                                } ?: dateText,
                            lunarText = if (pagerState.currentPage == 0) lunarText
                                else focusedDay?.lunarDate ?: lunarText,
                            textColor = textColor, onCityClick = onCityClick,
                            onCalendarClick = onCalendarClick, onSettingsClick = onSettingsClick,
                            onShareClick = onShareClick,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )

                        // 离线模式提示
                        if (fromCache && !isLoading) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "📡 离线模式，显示缓存数据",
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // 天气卡
                        if (threeDays.isNotEmpty()) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                                val day = threeDays[page]
                                CurrentWeatherCard(
                                    weather = if (page == 0) currentWeather else CurrentWeather(
                                        temperature = (day.weather.tempMax + day.weather.tempMin) / 2,
                                        feelsLike = (day.weather.tempMax + day.weather.tempMin) / 2,
                                        condition = day.weather.condition, isDay = currentWeather.isDay,
                                    ),
                                    textColor = textColor, tempUnit = tempUnit,
                                    iconAnimationEnabled = iconAnimationEnabled && page == 0,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            PageIndicator(pageCount, pagerState.currentPage, textColor)
                        } else {
                            CurrentWeatherCard(
                                weather = currentWeather, textColor = textColor, tempUnit = tempUnit,
                                iconAnimationEnabled = iconAnimationEnabled,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // 一句话智能建议（跟随当前页）
                        val focusedWeather = if (pagerState.currentPage == 0) currentWeather
                        else focusedDay?.let {
                            CurrentWeather(
                                temperature = (it.weather.tempMax + it.weather.tempMin) / 2,
                                feelsLike = (it.weather.tempMax + it.weather.tempMin) / 2,
                                condition = it.weather.condition,
                                isDay = currentWeather.isDay,
                            )
                        } ?: currentWeather

                        SmartAdviceCard(
                            currentWeather = focusedWeather,
                            todayInfo = focusedDay ?: threeDays.firstOrNull(),
                            rainForecast = if (pagerState.currentPage == 0) rainForecast else null,
                            textColor = textColor,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )

                        Spacer(Modifier.height(10.dp))

                        // 每日诗词
                        DailyPoetryCard(
                            date = focusedDay?.date ?: LocalDate.now(),
                            condition = focusedCondition,
                            textColor = textColor,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )

                        Spacer(Modifier.height(12.dp))

                        // ═══ 预警区域（最醒目位置） ═══

                        // 气象灾害预警（气象局官方，最高优先级）
                        if (pagerState.currentPage == 0 && warnings.isNotEmpty()) {
                            WeatherWarningCard(warnings = warnings, modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(Modifier.height(10.dp))
                        }

                        // 日程天气预警（日程×天气冲突）
                        if (weatherAlerts.isNotEmpty()) {
                            ScheduleWeatherAlertCard(
                                alerts = weatherAlerts,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        // ═══ 信息区域 ═══

                        // 小时预报（仅今天显示，明后天无小时数据）
                        if (pagerState.currentPage == 0 && hourlyForecast.isNotEmpty()) {
                            HourlyForecastRow(items = hourlyForecast, textColor = textColor, tempUnit = tempUnit)
                        }

                        // 当前页的日程
                        val focusedEvents = focusedDay?.events ?: emptyList()
                        if (focusedEvents.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            TodayEventsCard(
                                events = focusedEvents, textColor = textColor,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }

                        // ═══ 第二屏：降雨 + 15天预报 ═══

                        Spacer(Modifier.height(16.dp))

                        // 降雨预报（仅今天显示）
                        if (pagerState.currentPage == 0 && rainForecast != null) {
                            RainForecastCard(rainForecast = rainForecast, textColor = textColor,
                                modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(Modifier.height(12.dp))
                        }

                        // 7天预报（可折叠：默认只显示趋势图，点击展开列表）
                        if (threeDays.isNotEmpty()) {
                            CollapsibleSection(
                                title = "${threeDays.size}日预报",
                                textColor = textColor,
                                expanded = showForecastDetail,
                                onToggle = { showForecastDetail = !showForecastDetail },
                            ) {
                                TemperatureChart(days = threeDays, textColor = textColor)
                            }

                            AnimatedVisibility(
                                visible = showForecastDetail,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                DailyWeatherCalendarCard(
                                    days = threeDays, textColor = textColor,
                                    expandedIndex = fusionExpandedIndex,
                                    onExpandedChange = { fusionExpandedIndex = it },
                                    tempUnit = tempUnit,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // ═══ 第三屏：明日概览 + 更多信息 ═══

                        // 明日概览（仅在看今天时显示）
                        if (pagerState.currentPage == 0 && threeDays.size >= 2) {
                            TomorrowOverviewCard(tomorrowInfo = threeDays[1], textColor = textColor,
                                modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(Modifier.height(12.dp))
                        }

                        // 更多信息（可折叠：生活指数 + 日出日落 + 详情）
                        CollapsibleSection(
                            title = "更多信息",
                            textColor = textColor,
                            expanded = showMoreInfo,
                            onToggle = { showMoreInfo = !showMoreInfo },
                        )

                        AnimatedVisibility(
                            visible = showMoreInfo,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                if (airQuality != null) {
                                    AqiCard(
                                        airQuality = airQuality,
                                        textColor = textColor,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                LifeIndexCard(
                                    currentWeather = currentWeather, todayInfo = threeDays.firstOrNull(),
                                    windSpeed = weatherDetails.windSpeed, textColor = textColor,
                                    lifeIndices = lifeIndices,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                SunriseSunsetCard(
                                    sunrise = weatherDetails.sunrise, sunset = weatherDetails.sunset,
                                    textColor = textColor, modifier = Modifier.padding(horizontal = 20.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                ExpandableDetailsCard(
                                    details = weatherDetails, textColor = textColor,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }

            // 刷新成功反馈
            if (refreshFeedback) {
                Box(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        "✓ 已更新",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.85f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 可折叠区块标题
// ─────────────────────────────────────────────

@Composable
private fun CollapsibleSection(
    title: String,
    textColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title, color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = textColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
    content?.invoke()
}

// ─────────────────────────────────────────────

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, textColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) textColor else textColor.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun LoadingContent(textColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = textColor, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("正在获取天气数据...", color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorContent(error: String, textColor: Color, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("😔", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(error, color = textColor, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重试")
        }
    }
}

// ── Previews ──

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    WeatherCalendarTheme(dynamicColor = false) {
        HomeScreen(
            cityName = "北京", dateText = "4月16日 星期三", lunarText = "三月十九",
            currentWeather = MockData.currentWeather, hourlyForecast = MockData.hourlyForecast,
            threeDays = MockData.threeDays, weatherDetails = MockData.weatherDetails,
        )
    }
}
