# Implementation Tasks — UX Enhancements V2

## Phase 1: Data Layer (API + Models + DB)

- [x] 1.1 Add `weather15d` endpoint to QWeatherApi.kt
- [x] 1.2 Add `AirQuality` and `LifeIndex` data classes to Models.kt
- [x] 1.3 Extend `WeatherData` with `airQuality: AirQuality?` and `lifeIndices: List<LifeIndex>`
- [x] 1.4 Add `recurrenceRule: String?` field to EventEntity.kt
- [x] 1.5 Create MIGRATION_3_4 in AppDatabase.kt (ALTER TABLE events ADD COLUMN recurrenceRule TEXT)
- [x] 1.6 Add `theme_mode` key to UserPrefsRepository.kt
- [x] 1.7 Update AppModule.kt to include MIGRATION_3_4

## Phase 2: Repository Layer

- [x] 2.1 Update QWeatherRepository to call weather15d (fallback to weather7d)
- [x] 2.2 Map QWeather airNow response to AirQuality data class in QWeatherRepository
- [x] 2.3 Map QWeather indices response to List<LifeIndex> in QWeatherRepository
- [x] 2.4 Propagate AirQuality and lifeIndices through WeatherData in WeatherRepository
- [x] 2.5 Implement `expandRecurringEvents()` in EventRepository
- [x] 2.6 Update `getEventsBetween()` to expand recurring events
- [x] 2.7 Implement recurring event delete logic (UI phase)

## Phase 3: Widget Auto-Refresh

- [x] 3.1 Create WidgetRefreshWorker.kt (CoroutineWorker, fetch weather, update widget)
- [x] 3.2 Register WidgetRefreshWorker as PeriodicWorkRequest (1 hour, network required)
- [x] 3.3 Trigger immediate widget update after HomeViewModel refreshes weather data
- [x] 3.4 Enqueue WidgetRefreshWorker on app startup (WeatherCalendarApp.kt)

## Phase 4: UI — Quick Wins

- [x] 4.1 Feels-like explanation in CurrentWeatherCard (3°C threshold + cause text)
- [x] 4.2 Event color picker in CalendarScreen DayDetailContent (8 colors + checkmark)
- [x] 4.3 City swipe-to-delete in CityPickerSheet (SwipeToDismiss + red background)
- [x] 4.4 Pass event color through add/update callbacks and display in event list

## Phase 5: UI — AQI & Life Indices

- [x] 5.1 Create AqiCard.kt composable (circular indicator + value + category + PM2.5/PM10)
- [x] 5.2 Update LifeIndexCard.kt to display QWeather indices (grid layout, 8 items)
- [x] 5.3 Add AqiCard to HomeScreen "更多信息" section
- [x] 5.4 Update HomeUiState to include airQuality and lifeIndices
- [x] 5.5 Pass AQI/indices from HomeViewModel.renderWeatherData to UI state

## Phase 6: UI — Temperature Chart Enhancement

- [x] 6.1 Increase TemperatureChart height to 180dp
- [x] 6.2 Wrap chart in horizontalScroll for 15-day data
- [x] 6.3 Add date labels below each data point
- [x] 6.4 Add weather emoji icons above high temperature line
- [x] 6.5 Implement tap-to-show tooltip (Popup with date/temp/condition)

## Phase 7: UI — Skeleton Screen

- [x] 7.1 Create ShimmerEffect composable (animated gradient brush)
- [x] 7.2 Create SkeletonScreen composable (placeholder shapes matching HomeScreen layout)
- [x] 7.3 Replace LoadingContent in HomeScreen with SkeletonScreen
- [x] 7.4 Add crossfade animation from skeleton to real content

## Phase 8: UI — Dark Mode

- [x] 8.1 Update WeatherCalendarTheme to read theme_mode preference
- [x] 8.2 Implement follow_system / always_light / always_dark logic
- [x] 8.3 Add "主题" section to SettingsScreen (3 radio options)
- [x] 8.4 Define dark color variants for GlassCard, SettingsScreen, CityPickerSheet
- [x] 8.5 Ensure CalendarScreen bottom sheet respects dark mode

## Phase 9: UI — Recurring Events

- [x] 9.1 Add recurrence selector UI in DayDetailContent (chips)
- [x] 9.2 Add custom weekday picker (7 toggleable day buttons)
- [x] 9.3 Pass recurrenceRule through onAddEvent/onUpdateEvent callbacks
- [x] 9.4 Update CalendarViewModel to pass recurrenceRule to EventRepository
- [ ] 9.5 Add delete confirmation dialog for recurring events (仅此次/此后所有/全部)

## Phase 10: UI — Weather Card Image Sharing

- [ ] 10.1 Create generateWeatherBitmap() function (Canvas drawing)
- [ ] 10.2 Add FileProvider configuration for sharing cached images
- [ ] 10.3 Update share button handler to generate image and invoke share sheet
- [ ] 10.4 Add fallback to text sharing on Bitmap generation failure
- [ ] 10.5 Add "知天" branding watermark to generated image

## Phase 11: Integration & Polish

- [x] 11.1 Update HomeScreen pager to show up to 15 days in DailyWeatherCalendarCard
- [x] 11.2 Remove `.take(7)` limit in HomeViewModel.renderWeatherData
- [ ] 11.3 Verify all 12 features work together end-to-end
- [ ] 11.4 Build and test on device
