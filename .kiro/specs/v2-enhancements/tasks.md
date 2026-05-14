# Implementation Plan: v2-enhancements

## Overview

Implement five sub-features for 知天 v2: Alert Engine, Poetry Widget, Icon Animator, Transition Controller, and Animation Degradation Manager. The implementation follows an incremental approach — data model changes first, then domain logic, then UI components, and finally integration/wiring.

## Tasks

- [ ] 1. Data model extensions and repository changes
  - [ ] 1.1 Add windSpeed field to DailyWeather model and update QWeatherRepository parsing
    - Add `windSpeed: Int = 0` field to `DailyWeather` data class in `data/model/Models.kt`
    - Update `QWeatherRepository.getWeather()` to parse `windSpeedDay` from `QWeatherDaily` into the new `DailyWeather.windSpeed` field
    - _Requirements: 1.3_

  - [ ] 1.2 Create WeatherAlert data model and BadWeatherCondition sealed class
    - Create `domain/alert/WeatherAlert.kt` with the `WeatherAlert` data class (eventName, eventDate, eventTime, weatherLabel, triggerMetric, suggestion, relativeDateText)
    - Create `BadWeatherCondition` sealed class with priority-ordered variants: Stormy(1), Snowy(2), Rainy(3), StrongWind(4), ExtremeHeat(5), ExtremeCold(6)
    - _Requirements: 1.2, 1.3_

  - [ ] 1.3 Extend HomeUiState with new fields
    - Add `weatherAlerts: List<WeatherAlert> = emptyList()` to `HomeUiState`
    - Add `animationDegraded: Boolean = false` to `HomeUiState`
    - Add `iconAnimationEnabled: Boolean = true` to `HomeUiState`
    - _Requirements: 2.1, 9.1_

- [ ] 2. Alert Engine domain logic
  - [ ] 2.1 Implement SuggestionGenerator
    - Create `domain/alert/SuggestionGenerator.kt` as an object with `getSuggestion(conditions: List<BadWeatherCondition>): String`
    - Implement priority-based selection: sort by priority, return suggestion for highest-priority condition
    - Map each condition to its predefined suggestion string (≤30 chars)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [ ]* 2.2 Write property test for suggestion mapping correctness
    - **Property 4: Suggestion mapping correctness**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

  - [ ]* 2.3 Write property test for suggestion priority selection
    - **Property 5: Suggestion priority selection**
    - **Validates: Requirements 3.7**

  - [ ]* 2.4 Write property test for suggestion length invariant
    - **Property 6: Suggestion length invariant**
    - **Validates: Requirements 3.8**

  - [ ] 2.5 Implement AlertEngine
    - Create `domain/alert/AlertEngine.kt` with `@Inject constructor(eventRepository, calendarRepository, suggestionGenerator)`
    - Implement `suspend fun generateAlerts(dailyForecasts: List<DailyWeather>): List<WeatherAlert>`
    - Query events from both EventRepository and CalendarRepository for next 15 days
    - Cross-reference event dates against daily forecasts to detect Bad_Weather conditions
    - Generate one alert per event on bad-weather days with: event name (truncated to 20 chars with ellipsis), relative date text, weather label, trigger metric, and suggestion
    - Sort alerts chronologically, cap at 5 entries
    - Handle errors gracefully: if one repository fails, use the other; if weather data unavailable, return empty list
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 2.2, 2.4_

  - [ ]* 2.6 Write property test for alert generation correctness
    - **Property 1: Alert generation produces alerts only for events on bad-weather days**
    - **Validates: Requirements 1.1, 1.3, 1.4, 1.5**

  - [ ]* 2.7 Write property test for alert field completeness
    - **Property 2: Alert field completeness**
    - **Validates: Requirements 1.2, 2.2**

  - [ ]* 2.8 Write property test for alert ordering and capping
    - **Property 3: Alert display ordering and capping**
    - **Validates: Requirements 2.4**

- [ ] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Animation Degradation Manager
  - [ ] 4.1 Implement AnimationDegradationManager
    - Create `domain/animation/AnimationDegradationManager.kt` as `@Singleton` with `@Inject constructor(@ApplicationContext context: Context)`
    - Define `DegradationState` data class with `iconAnimationEnabled: Boolean`, `transitionDuration: Int`, `reason: DegradationReason`
    - Expose `state: StateFlow<DegradationState>`
    - Implement `checkAndUpdate()` that reads BatteryManager level/scale and PowerManager.isPowerSaveMode
    - Apply hysteresis: disable at <15%, restore at >20% (15-20% maintains current state)
    - Power-saving mode: iconAnimationEnabled=false, transitionDuration=0
    - Low battery: iconAnimationEnabled=false, transitionDuration=200
    - Default to normal mode if system services unavailable
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ]* 4.2 Write property test for animation degradation state correctness
    - **Property 10: Animation degradation state correctness**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5**

- [ ] 5. Weather Icon Animator
  - [ ] 5.1 Implement WeatherIconAnimator composable
    - Create `ui/components/WeatherIconAnimator.kt`
    - Implement `@Composable fun WeatherIconAnimator(condition: WeatherCondition, enabled: Boolean, modifier: Modifier)` with 64dp fixed size
    - Use `rememberInfiniteTransition` for lifecycle-aware animation pause/resume
    - Implement per-condition animations: sunny (pulsing opacity 3-5s), cloudy (horizontal drift ≤20dp/s), rainy/drizzle (3-5 falling drops), snowy (3-5 floating flakes with sway), stormy (lightning flash 3-7s interval), foggy (≤3 flowing layers)
    - Cap particles: raindrops ≤5, snowflakes ≤5, cloud layers ≤3
    - When `enabled = false`, render static icon only (no animation)
    - Check system `ANIMATOR_DURATION_SCALE` for Reduced_Motion; if 0, force static
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [ ] 6. Weather Transition Controller
  - [ ] 6.1 Implement WeatherTransitionController (AnimatedWeatherBackground)
    - Create `ui/components/WeatherTransitionController.kt`
    - Implement `@Composable fun AnimatedWeatherBackground(condition: WeatherCondition, isDay: Boolean, degraded: Boolean, content: @Composable () -> Unit)`
    - Use `animateColorAsState` with `tween(durationMillis, easing = EaseInOut)` for both gradient start and end colors
    - Normal duration: 700ms; degraded (low battery): 200ms; power-saving: 0ms (instant)
    - Leverage `animateColorAsState` natural interruption handling for mid-transition changes
    - Use `WeatherColors.gradientFor(condition, isDay)` to get target gradient
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [ ] 7. Poetry Widget
  - [ ] 7.1 Create Poetry Widget Glance implementation
    - Create `widget/PoetryWidget.kt` extending `GlanceAppWidget`
    - Implement `provideGlance()` to render poetry content using Glance composables
    - Display verse text (color 0xFFF5E6C8), source attribution below in secondary style, weather icon (16dp, 60% opacity) in top-right
    - Dark background with 85% opacity (alpha 0xD9)
    - Read cached weather condition from SharedPreferences; fallback to SUNNY
    - Cache last successful poem in SharedPreferences as fallback
    - Add click action to launch MainActivity
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.5, 5.6_

  - [ ] 7.2 Create PoetryWidgetReceiver and register in AndroidManifest
    - Create `widget/PoetryWidgetReceiver.kt` extending `GlanceAppWidgetReceiver`
    - Create `res/xml/poetry_widget_info.xml` with widget metadata (updatePeriodMillis=86400000, minWidth/minHeight, previewImage)
    - Register receiver in AndroidManifest.xml with `APPWIDGET_UPDATE` intent filter and widget metadata
    - Register `ACTION_DATE_CHANGED` broadcast for daily refresh
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 7.3 Write property test for poetry daily uniqueness
    - **Property 7: Poetry daily uniqueness**
    - **Validates: Requirements 5.1**

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Integration and wiring
  - [ ] 9.1 Create ScheduleWeatherAlertCard composable
    - Create `ui/home/ScheduleWeatherAlertCard.kt`
    - Display alert text in format: "[相对日期]有[事件名]，预计[天气状况]，建议[行动建议]"
    - Use warning icon (⚠️), minimum 4dp elevation, 4.5:1 contrast ratio
    - Support vertical expand/shrink animation for show/hide transitions
    - Display max 5 alerts in chronological order
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 9.2 Wire AlertEngine into HomeViewModel
    - Inject `AlertEngine` into `HomeViewModel`
    - Call `alertEngine.generateAlerts(weatherData.daily)` after weather data loads in `renderWeatherData()`
    - Update `_uiState` with `weatherAlerts` field
    - _Requirements: 1.1, 2.1_

  - [ ] 9.3 Wire AnimationDegradationManager into HomeViewModel and HomeScreen
    - Inject `AnimationDegradationManager` into `HomeViewModel`
    - Call `checkAndUpdate()` in `loadData()` (on app resume)
    - Collect `degradationManager.state` and update `animationDegraded` and `iconAnimationEnabled` in `HomeUiState`
    - _Requirements: 9.5, 9.6_

  - [ ] 9.4 Integrate all new components into HomeScreen
    - Add `ScheduleWeatherAlertCard` above daily forecast section (show/hide based on `weatherAlerts`)
    - Replace static weather icon in `CurrentWeatherCard` with `WeatherIconAnimator` (pass `iconAnimationEnabled` from state)
    - Wrap HomeScreen content with `AnimatedWeatherBackground` (pass `animationDegraded` from state)
    - _Requirements: 2.1, 6.7, 8.1, 9.1_

  - [ ] 9.5 Register AnimationDegradationManager and AlertEngine in Hilt DI module
    - Add `@Provides` or `@Binds` entries in `di/AppModule.kt` for `AlertEngine`, `SuggestionGenerator`, and `AnimationDegradationManager`
    - Ensure proper scoping (`@Singleton` for degradation manager)
    - _Requirements: 1.1, 9.6_

- [ ] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using Kotest property testing
- Unit tests validate specific examples and edge cases
- The project currently has no test directory; property test tasks will need to set up `app/src/test/` with Kotest dependencies
- `QWeatherDaily.windSpeed` (aliased from `windSpeedDay`) already exists in the API model — only the domain model `DailyWeather` and the mapping in `QWeatherRepository` need updating
- `AnimationDegradationManager` is testable with extracted logic (battery level + power save → state), even though `BatteryManager`/`PowerManager` need mocking

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "4.2"] },
    { "id": 3, "tasks": ["2.6", "2.7", "2.8", "5.1", "6.1", "7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3"] },
    { "id": 5, "tasks": ["9.1", "9.2", "9.3", "9.5"] },
    { "id": 6, "tasks": ["9.4"] }
  ]
}
```
