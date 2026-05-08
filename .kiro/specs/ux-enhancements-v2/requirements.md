# Requirements Document

## Introduction

知天 (zhitian) 天气日历 App 的第二轮体验增强，涵盖 12 项改进，对标墨迹天气、彩云天气和 Apple Weather 的核心功能。改进范围包括：扩展天气预报天数、展示生活指数与空气质量数据、日历事件重复规则与颜色自定义、Widget 自动刷新、城市管理、加载体验优化、温度趋势图增强、深色模式跟随系统、天气卡片图片分享、以及体感温度解释。

## Glossary

- **App**: 知天天气日历 Android 应用
- **QWeather_API**: 和风天气 RESTful API 服务，提供天气数据
- **Home_Screen**: App 主页面，展示当前天气、小时预报、多日预报等信息
- **Temperature_Chart**: 多日温度趋势折线图组件
- **Life_Index_Card**: 生活指数展示卡片组件
- **AQI_Display**: 空气质量指数展示组件
- **Event_Repository**: 日历事件数据仓库，负责事件 CRUD 操作
- **Event_Entity**: Room 数据库中的日历事件实体
- **Recurrence_Rule**: 事件重复规则，定义事件的重复模式
- **Widget**: App 桌面小组件，使用 Glance 框架实现
- **Widget_Worker**: 使用 WorkManager 定时刷新 Widget 数据的后台任务
- **City_Picker_Sheet**: 城市选择底部弹窗组件
- **Skeleton_Screen**: 骨架屏加载占位 UI，模拟真实布局结构
- **Weather_Card_Image**: 用于分享的天气信息 Bitmap 图片
- **Feels_Like_Explanation**: 体感温度与实际温度差异的文字解释
- **Event_Color**: 日历事件的自定义颜色标识
- **Dark_Mode**: 深色主题模式
- **User_Prefs_Repository**: 用户偏好设置数据仓库

## Requirements

### Requirement 1: 15-Day Weather Forecast

**User Story:** As a user, I want to see a 15-day weather forecast, so that I can plan activities further in advance.

#### Acceptance Criteria

1. WHEN the user opens the Home_Screen, THE App SHALL request daily forecast data from the QWeather_API `weather/15d` endpoint instead of the `weather/7d` endpoint
2. WHEN the QWeather_API returns a successful 15-day forecast response, THE Home_Screen SHALL display all 15 days in the daily forecast list
3. WHEN the QWeather_API `weather/15d` endpoint returns an error, THE App SHALL fall back to the `weather/7d` endpoint and display 7 days of forecast data
4. THE Temperature_Chart SHALL render data points for all available forecast days (up to 15)
5. WHEN the forecast contains more than 7 days, THE Temperature_Chart SHALL be horizontally scrollable to reveal additional days

### Requirement 2: Life Indices Display from QWeather API

**User Story:** As a user, I want to see detailed life indices from the weather service, so that I can make informed daily decisions.

#### Acceptance Criteria

1. WHEN the QWeather_API `indices/1d` endpoint returns a successful response, THE Life_Index_Card SHALL display the following indices: exercise (运动), car wash (洗车), clothing (穿衣), UV (紫外线), comfort (舒适度), cold risk (感冒), air pollution (空气污染), and travel (旅游)
2. THE Life_Index_Card SHALL display each index with its name, category level, and descriptive text from the QWeather_API response
3. IF the QWeather_API `indices/1d` endpoint returns an error, THEN THE Life_Index_Card SHALL fall back to the locally computed indices based on temperature, wind speed, and weather condition

### Requirement 3: Air Quality Index Display

**User Story:** As a user, I want to see detailed air quality information, so that I can decide whether outdoor activities are safe.

#### Acceptance Criteria

1. WHEN the QWeather_API `airNow` endpoint returns a successful response, THE AQI_Display SHALL show the AQI numeric value, the category label (优/良/轻度污染/中度污染/重度污染/严重污染), and PM2.5 and PM10 concentration values
2. THE AQI_Display SHALL use a color indicator that corresponds to the AQI category (green for 优, yellow for 良, orange for 轻度污染, red for 中度污染, purple for 重度污染, maroon for 严重污染)
3. THE AQI_Display SHALL be positioned within the Home_Screen weather details section
4. IF the QWeather_API `airNow` endpoint returns an error, THEN THE AQI_Display SHALL show a placeholder text "暂无数据"

### Requirement 4: Recurring Calendar Events

**User Story:** As a user, I want to create recurring events, so that I do not need to manually add repeated events.

#### Acceptance Criteria

1. THE Event_Entity SHALL store a Recurrence_Rule field that supports the following repeat types: none, daily, weekly, monthly, yearly, and custom weekdays
2. WHEN a user creates or edits an event, THE App SHALL present a repeat frequency selector with options: 不重复, 每天, 每周, 每月, 每年, and 自定义 (custom weekdays)
3. WHEN the user selects 自定义, THE App SHALL display a weekday picker allowing selection of one or more days of the week (周一 through 周日)
4. WHEN the Event_Repository queries events for a date range, THE Event_Repository SHALL expand recurring events into individual occurrences for each matching date within the queried range
5. WHEN a user deletes a recurring event, THE App SHALL prompt the user to choose between deleting only the selected occurrence, all future occurrences, or all occurrences
6. THE Recurrence_Rule SHALL be serialized as a string in the format `type:params` (e.g., `weekly:`, `custom:1,3,5` for Monday, Wednesday, Friday)

### Requirement 5: Widget Auto-Refresh

**User Story:** As a user, I want the home screen widget to show up-to-date weather, so that I can glance at current conditions without opening the app.

#### Acceptance Criteria

1. THE Widget_Worker SHALL execute a periodic refresh task at a minimum interval of 1 hour using WorkManager PeriodicWorkRequest
2. WHEN the Widget_Worker executes, THE Widget_Worker SHALL fetch fresh weather data from the QWeather_API and update the Widget display
3. THE Widget_Worker SHALL only execute when a network connection is available, using WorkManager network constraint
4. WHEN the Widget_Worker fails to fetch data from the QWeather_API, THE Widget SHALL continue displaying the most recent cached data
5. WHEN the user opens the App and weather data is refreshed, THE App SHALL trigger an immediate Widget update in addition to the periodic schedule

### Requirement 6: City Delete from Picker

**User Story:** As a user, I want to remove saved cities from my list, so that I can keep my city list organized.

#### Acceptance Criteria

1. WHEN a saved city item in the City_Picker_Sheet is swiped left, THE City_Picker_Sheet SHALL reveal a delete action button
2. WHEN the user taps the delete action button, THE App SHALL remove the city from the saved cities list and persist the change
3. THE City_Picker_Sheet SHALL NOT allow deletion of the current GPS-located city (isCurrentLocation = true)
4. WHEN a city is deleted, THE City_Picker_Sheet SHALL animate the item removal with a slide-out animation

### Requirement 7: Skeleton Loading Screen

**User Story:** As a user, I want to see a structured placeholder during initial load, so that I understand the layout before data arrives.

#### Acceptance Criteria

1. WHILE the App is loading weather data for the first time (no cached data available), THE Home_Screen SHALL display a Skeleton_Screen instead of a spinner
2. THE Skeleton_Screen SHALL replicate the layout structure of the Home_Screen including placeholder shapes for: the header bar, current weather card, hourly forecast row, and daily forecast section
3. THE Skeleton_Screen SHALL display a shimmer animation effect that sweeps across placeholder shapes from left to right
4. WHEN weather data becomes available, THE Home_Screen SHALL transition from the Skeleton_Screen to the actual content with a fade-in animation

### Requirement 8: Enhanced Temperature Trend Chart

**User Story:** As a user, I want a larger and more interactive temperature chart, so that I can clearly see temperature trends across multiple days.

#### Acceptance Criteria

1. THE Temperature_Chart SHALL have a minimum height of 180dp (increased from the current 120dp)
2. WHEN the forecast contains more than 7 days, THE Temperature_Chart SHALL be horizontally scrollable within a fixed-height container
3. THE Temperature_Chart SHALL display date labels (day of month) below each data point
4. WHEN the user taps a data point on the Temperature_Chart, THE Temperature_Chart SHALL display a tooltip showing the date, high temperature, low temperature, and weather condition for that day
5. THE Temperature_Chart SHALL display weather condition icons above the high temperature line for each day

### Requirement 9: Dark Mode Auto-Switch

**User Story:** As a user, I want the app theme to follow my system dark/light mode setting, so that the app matches my device appearance preference.

#### Acceptance Criteria

1. THE App SHALL follow the system-level dark/light mode setting by default
2. WHEN the system dark mode setting changes while the App is running, THE App SHALL update its theme within the same session without requiring a restart
3. THE User_Prefs_Repository SHALL store a theme preference with options: follow_system, always_light, always_dark
4. WHEN the user selects a manual theme override in settings, THE App SHALL use the selected theme regardless of the system setting
5. WHILE in dark mode, THE App SHALL apply dark-variant colors to all UI cards, text, and icons that are not part of the weather gradient background

### Requirement 10: Weather Card Image Sharing

**User Story:** As a user, I want to share a styled weather card image, so that my friends can see the weather information in a visually appealing format.

#### Acceptance Criteria

1. WHEN the user taps the share button on the Home_Screen, THE App SHALL generate a Weather_Card_Image as a Bitmap containing: city name, date, current temperature, weather condition icon, high/low temperatures, and the "知天" branding
2. THE Weather_Card_Image SHALL use a gradient background matching the current weather condition and time of day
3. WHEN the Weather_Card_Image is generated, THE App SHALL invoke the system share sheet with the image as a `image/png` content URI
4. THE Weather_Card_Image SHALL have a resolution of at least 1080 pixels wide and maintain a 3:4 aspect ratio
5. IF the Bitmap generation fails due to memory constraints, THEN THE App SHALL fall back to sharing plain text using the existing text sharing function

### Requirement 11: Feels-Like Temperature Explanation

**User Story:** As a user, I want to understand why the feels-like temperature differs from the actual temperature, so that I can dress appropriately.

#### Acceptance Criteria

1. WHEN the absolute difference between the actual temperature and the feels-like temperature is 3°C or greater, THE Current_Weather_Card SHALL display a Feels_Like_Explanation text below the feels-like temperature
2. THE Feels_Like_Explanation SHALL describe the primary cause of the difference using one of the following messages: "风大体感偏冷" (wind chill), "湿度高体感闷热" (humidity), "日照强体感偏热" (solar radiation), or "综合因素" (combined factors)
3. WHEN the feels-like temperature is lower than the actual temperature by 3°C or more, THE Feels_Like_Explanation SHALL indicate a cold-biased cause (wind or humidity in cold conditions)
4. WHEN the feels-like temperature is higher than the actual temperature by 3°C or more, THE Feels_Like_Explanation SHALL indicate a heat-biased cause (humidity or solar radiation)

### Requirement 12: Event Color Picker

**User Story:** As a user, I want to assign custom colors to my calendar events, so that I can visually distinguish different types of events.

#### Acceptance Criteria

1. WHEN the user creates or edits an event, THE App SHALL display an Event_Color picker with at least 8 predefined color options
2. THE Event_Color picker SHALL show the currently selected color with a check mark indicator
3. THE Event_Entity SHALL persist the selected Event_Color value as a Long (ARGB format) in the Room database
4. WHEN events are displayed in the calendar view and the Home_Screen events card, THE App SHALL use the event's assigned Event_Color for the event indicator dot and text accent
5. THE App SHALL use a default color of green (0xFF4CAF50) when no color is explicitly selected by the user
