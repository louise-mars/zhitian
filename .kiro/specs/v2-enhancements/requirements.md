# Requirements Document

## Introduction

知天 (Zhitian) v2 增强功能批次，包含四项特性：日程天气预警、Widget 诗词模式、天气图标微动画、天气转场动画。这些功能围绕"轻量、内敛、高效"的工具型 App 核心理念，在不增加用户认知负担的前提下提升信息密度和视觉品质。

## Glossary

- **Alert_Engine**: 日程天气预警引擎，负责将未来日程与天气预报进行交叉匹配并生成预警卡片数据
- **HomeScreen**: 应用主界面，展示当前天气、预报、诗词、日程等核心信息
- **Poetry_Widget**: 诗词模式桌面小组件，使用 Jetpack Glance 实现，展示每日诗词
- **Weather_Widget**: 已有的天气桌面小组件（WeatherWidgetReceiver + WeatherWidget）
- **Icon_Animator**: 天气图标微动画组件，使用 Compose Canvas 绘制轻量无限动画
- **Transition_Controller**: 天气转场动画控制器，管理 HomeScreen 背景渐变色的平滑过渡
- **Bad_Weather**: 恶劣天气条件集合，包括：雨（RAINY）、大雨/暴雨、雷暴（STORMY）、雪（SNOWY）、极端高温（>35°C）、极端低温（<-10°C）、强风（>6级/风速>39km/h）
- **EventRepository**: App 内事件仓库（Room 数据库），提供 CRUD 和日期范围查询
- **CalendarRepository**: 系统日历仓库，通过 ContentResolver 读取系统日历事件
- **QWeatherRepository**: 和风天气数据仓库，提供 15 天预报、逐小时预报、预警等数据
- **DailyPoetry**: 每日诗词工具类，根据日期和天气条件返回匹配的古诗词
- **WeatherCondition**: 天气状态枚举（SUNNY, PARTLY_CLOUDY, CLOUDY, FOGGY, DRIZZLE, RAINY, SNOWY, STORMY）
- **WeatherColors**: 天气渐变色工具，根据 WeatherCondition 和昼夜状态返回背景渐变色
- **CurrentWeatherCard**: HomeScreen 中展示当前天气的核心卡片组件
- **Reduced_Motion**: Android 系统无障碍设置中的"减少动画"选项

## Requirements

### Requirement 1: 日程天气预警数据匹配

**User Story:** As a user with upcoming calendar events, I want to be warned when bad weather is forecast on event days, so that I can prepare or adjust plans in advance.

#### Acceptance Criteria

1. WHEN the App is opened or resumed to the foreground, THE Alert_Engine SHALL query events from EventRepository and CalendarRepository for the next 15 days (starting from today) and cross-reference each event's date against the 15-day daily forecast from QWeatherRepository
2. WHEN an event day matches a Bad_Weather condition, THE Alert_Engine SHALL generate an alert containing: the event name, the event date, the forecast weather condition label (e.g. "雨", "雷暴", "雪"), the triggering metric (e.g. "最高温 37°C"), and a suggestion referencing the weather condition
3. THE Alert_Engine SHALL classify the following conditions as Bad_Weather: WeatherCondition RAINY, WeatherCondition STORMY, WeatherCondition SNOWY, daily precipitation exceeding 10mm, daily maximum temperature exceeding 35°C, daily minimum temperature at or below 0°C, and daily maximum wind speed exceeding 39 km/h
4. WHEN multiple events exist on the same bad-weather day, THE Alert_Engine SHALL generate one alert per event, each containing that event's specific name and the shared weather condition for that day
5. WHEN no events coincide with bad weather in the next 15 days, THE Alert_Engine SHALL produce an empty alert list
6. IF the weather forecast data is unavailable (network failure or API error), THEN THE Alert_Engine SHALL skip alert generation and produce an empty alert list without crashing
7. IF calendar data is unavailable (permission denied or read failure), THEN THE Alert_Engine SHALL skip alert generation for the unavailable source and still process events from the other available repository
8. THE Alert_Engine SHALL filter out indoor events by matching event titles against a set of indoor keywords (Zoom, Teams, 线上, 腾讯会议, 钉钉, 飞书, 居家, etc.) and SHALL NOT generate alerts for matched events
9. WHEN the event is on today's date and has a specific time, THE Alert_Engine SHALL check the hourly forecast for ±1 hour around the event time instead of using the daily forecast
10. WHEN active weather warnings exist from the meteorological bureau, THE Alert_Engine SHALL generate alerts for all outdoor events on today and tomorrow that do not match indoor keywords
11. WHEN alerts are generated for tomorrow or the day after, THE WeatherAlertNotifier SHALL send a local OS notification (maximum 1 per day) to proactively inform the user

### Requirement 2: 日程天气预警卡片展示

**User Story:** As a user viewing the home screen, I want to see weather alerts for my upcoming events prominently, so that I notice them before it is too late to act.

#### Acceptance Criteria

1. WHEN the Alert_Engine produces one or more alerts, THE HomeScreen SHALL display a Schedule_Weather_Alert card above the daily forecast section
2. THE Schedule_Weather_Alert card SHALL display text in the format: "[相对日期]有[事件名]，预计[天气状况]，建议[行动建议]", where [相对日期] is the relative day reference (e.g., "今天", "明天", "后天", or "N天后" for events more than 2 days away), and [事件名] is truncated to a maximum of 20 characters with an ellipsis appended if exceeded
3. WHEN the Alert_Engine produces an empty alert list, THE HomeScreen SHALL hide the Schedule_Weather_Alert card
4. WHEN multiple alerts exist, THE HomeScreen SHALL display a maximum of 5 alert cards in chronological order by event date, with alerts on the same date ordered by event time (earlier first, all-day events before timed events)
5. THE Schedule_Weather_Alert card SHALL use a warning icon (⚠️) and a background with a minimum elevation of 4dp that provides at least 4.5:1 contrast ratio between the card text and the card background
6. WHEN the alert list transitions between empty and non-empty states, THE HomeScreen SHALL animate the Schedule_Weather_Alert card appearance and disappearance using a vertical expand/shrink animation

### Requirement 3: 日程天气预警建议生成

**User Story:** As a user receiving a weather alert, I want actionable suggestions tailored to the weather type, so that I know what to do.

#### Acceptance Criteria

1. WHEN the Bad_Weather condition is WeatherCondition.RAINY or WeatherCondition.DRIZZLE, THE Alert_Engine SHALL suggest "建议带伞或调整出行时间"
2. WHEN the Bad_Weather condition is WeatherCondition.STORMY, THE Alert_Engine SHALL suggest "建议改期或选择室内场所"
3. WHEN the Bad_Weather condition is WeatherCondition.SNOWY, THE Alert_Engine SHALL suggest "注意保暖和路面结冰"
4. WHEN the Bad_Weather condition is extreme heat (daily maximum temperature > 35°C), THE Alert_Engine SHALL suggest "注意防暑降温，避免长时间户外活动"
5. WHEN the Bad_Weather condition is extreme cold (daily minimum temperature < -10°C), THE Alert_Engine SHALL suggest "注意防寒保暖"
6. WHEN the Bad_Weather condition is strong wind (daily maximum wind speed > 6级 / 39 km/h), THE Alert_Engine SHALL suggest "注意防风，避免高空作业或户外活动"
7. WHEN multiple Bad_Weather conditions apply to the same event day, THE Alert_Engine SHALL select the suggestion corresponding to the highest-severity condition using the priority order: STORMY > SNOWY > RAINY/DRIZZLE > strong wind > extreme heat > extreme cold
8. THE Alert_Engine SHALL return each suggestion as a plain-text string with a maximum length of 30 characters

### Requirement 4: Poetry Widget 内容展示

**User Story:** As a user who appreciates Chinese poetry, I want a home screen widget that shows a daily poetry verse, so that I can enjoy literature throughout the day without opening the app.

#### Acceptance Criteria

1. THE Poetry_Widget SHALL display one line of poetry verse obtained from DailyPoetry for the current date and weather condition, truncated with ellipsis if the verse exceeds the available widget width
2. THE Poetry_Widget SHALL display the source (poem title and author) of the poetry below the verse in a secondary text style
3. THE Poetry_Widget SHALL display the weather condition icon (from WeatherCondition.icon) in the top-right corner of the widget at 16dp size and 60% opacity
4. THE Poetry_Widget SHALL use text color 0xFFF5E6C8 on a dark background with 85% opacity (alpha 0xD9)
5. WHEN the user taps the Poetry_Widget, THE Poetry_Widget SHALL launch the main application (MainActivity)
6. IF weather condition data is unavailable, THEN THE Poetry_Widget SHALL display a verse using WeatherCondition.SUNNY as the default condition
7. WHEN a new calendar day begins or the widget receives a system update broadcast, THE Poetry_Widget SHALL refresh the displayed verse for the new date and current weather condition

### Requirement 5: Poetry Widget 刷新与共存

**User Story:** As a user managing my home screen, I want the poetry widget to refresh daily and coexist with the weather widget, so that I can choose which widgets to display.

#### Acceptance Criteria

1. THE Poetry_Widget SHALL refresh its content once per day at midnight (00:00 local device time) to display a poem that is different from the previous day's poem, selected deterministically based on the current date
2. THE Poetry_Widget SHALL operate as an independent widget variant that can be added to or removed from the home screen without affecting the existing Weather_Widget's functionality or state
3. THE Poetry_Widget SHALL register a separate GlanceAppWidgetReceiver with its own widget metadata XML, distinct from the WeatherWidgetReceiver
4. WHEN the device date changes, THE Poetry_Widget SHALL update the displayed verse and source attribution to match the new date within 60 seconds of the widget becoming active
5. THE Poetry_Widget SHALL display the poem verse text and its source attribution (poem title and author)
6. IF the Poetry_Widget fails to retrieve a poem for the current date, THEN THE Poetry_Widget SHALL display the most recently successfully loaded poem content

### Requirement 6: 天气图标微动画渲染

**User Story:** As a user viewing current weather, I want the weather icon to have subtle animations, so that the interface feels alive and informative without being distracting.

#### Acceptance Criteria

1. THE Icon_Animator SHALL render animated weather icons using Compose Canvas drawing APIs without external animation libraries
2. WHEN the current weather is sunny, THE Icon_Animator SHALL animate sun rays with a pulsing opacity effect (cycle duration 3-5 seconds)
3. WHEN the current weather is cloudy or partly cloudy, THE Icon_Animator SHALL animate clouds with a slow horizontal drift (traversal speed ≤ 20dp/second)
4. WHEN the current weather is rainy or drizzle, THE Icon_Animator SHALL animate 3-5 raindrops falling downward within the icon bounds
5. WHEN the current weather is snowy, THE Icon_Animator SHALL animate 3-5 snowflakes floating downward with lateral sway within the icon bounds
6. WHEN the current weather is stormy, THE Icon_Animator SHALL animate a periodic lightning flash effect (flash interval 3-7 seconds, flash duration ≤ 100ms)
7. THE Icon_Animator SHALL apply animations only to the main weather icon in CurrentWeatherCard on HomeScreen, with a fixed icon size of 64dp × 64dp

### Requirement 7: 天气图标微动画性能与无障碍

**User Story:** As a user on a low-end device or with accessibility needs, I want weather animations to be lightweight and respect my system settings, so that the app remains performant and accessible.

#### Acceptance Criteria

1. THE Icon_Animator SHALL use infinite animations with a frame interval no shorter than 16ms (60fps cap)
2. WHEN the app moves to the background, THE Icon_Animator SHALL pause all running animations and release Canvas draw resources
3. WHEN the app returns to the foreground, THE Icon_Animator SHALL resume animations from their paused state without restarting from the initial frame
4. WHEN the system Reduced_Motion accessibility setting is enabled or toggled while the app is in the foreground, THE Icon_Animator SHALL immediately cease all motion and display a static weather icon representing the current weather condition
5. THE Icon_Animator SHALL limit total draw operations to no more than 20 per frame for the icon animation and maintain a per-frame rendering time below 8ms
6. THE Icon_Animator SHALL cap particle counts per weather type within the icon: raindrops ≤ 5, snowflakes ≤ 5, cloud layers ≤ 3

### Requirement 8: 天气转场动画

**User Story:** As a user switching cities or refreshing weather, I want the background gradient to transition smoothly, so that the visual change feels polished rather than jarring.

#### Acceptance Criteria

1. WHEN the weather condition changes due to city switching, THE Transition_Controller SHALL crossfade the HomeScreen background gradient from the current displayed color to the new target color over the configured transition duration
2. WHEN the weather condition changes due to data refresh, THE Transition_Controller SHALL crossfade the HomeScreen background gradient from the current displayed color to the new target color over the configured transition duration
3. WHEN the day/night state changes, THE Transition_Controller SHALL crossfade the HomeScreen background gradient from the current displayed color to the night gradient (when transitioning to night) or the weather-condition-appropriate day gradient (when transitioning to day)
4. THE Transition_Controller SHALL complete each gradient transition within 600 to 800 milliseconds using an ease-in-out timing curve
5. THE Transition_Controller SHALL animate both the gradient start color and gradient end color simultaneously using Compose animateColorAsState or equivalent color interpolation API
6. IF a new weather condition change occurs while a gradient transition is already in progress, THEN THE Transition_Controller SHALL begin the new transition from the current interpolated color values at the moment of interruption, without resetting to the previous target or origin colors

### Requirement 9: 动画降级策略

**User Story:** As a user on a device with low battery or limited performance, I want animations to automatically degrade, so that the app conserves resources and remains responsive.

#### Acceptance Criteria

1. WHEN the device battery level drops below 15%, THE Icon_Animator SHALL disable all animations and display static icons
2. WHEN the device battery level drops below 15%, THE Transition_Controller SHALL reduce transition duration to 200ms
3. WHILE the system power-saving mode is active, THE Icon_Animator SHALL disable all animations and display static icons
4. WHILE the system power-saving mode is active, THE Transition_Controller SHALL use instant color change without animation
5. WHEN the device battery level rises above 20% AND power-saving mode is not active, THE Icon_Animator and Transition_Controller SHALL restore full animation behavior
6. THE animation degradation state SHALL be checked via BatteryManager and PowerManager system services, polled on each app resume event
