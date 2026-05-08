# Technical Design — UX Enhancements V2

## Overview

本设计文档覆盖 12 项体验增强的技术实现方案。按层次组织：数据层 → 业务层 → UI 层。

---

## 1. Data Layer Changes

### 1.1 QWeatherApi — 新增 15 天预报端点

```kotlin
// QWeatherApi.kt — 新增
@GET("v7/weather/15d")
suspend fun weather15d(
    @Query("location") location: String,
    @Query("key") key: String = API_KEY,
): QWeatherDailyResponse
```

### 1.2 WeatherData — 扩展 AQI 和生活指数

```kotlin
// Models.kt — 新增
data class AirQuality(
    val aqi: Int,
    val category: String,   // "优"/"良"/...
    val pm2p5: String,
    val pm10: String,
)

data class LifeIndex(
    val type: String,       // "1"=运动 "2"=洗车 ...
    val name: String,       // "运动指数"
    val category: String,   // "适宜"
    val text: String,       // 详细描述
)

// WeatherData 扩展
data class WeatherData(
    // ... existing fields ...
    val airQuality: AirQuality? = null,
    val lifeIndices: List<LifeIndex> = emptyList(),
)
```

### 1.3 EventEntity — 新增重复规则和颜色字段

```kotlin
// EventEntity.kt — 修改
@Entity(tableName = "events")
data class EventEntity(
    // ... existing fields ...
    val recurrenceRule: String? = null,  // "none"/"daily"/"weekly"/"monthly"/"yearly"/"custom:1,3,5"
    val color: Long = 0xFF4CAF50,       // 已有，无需修改
)
```

**Database Migration v3 → v4:**
```sql
ALTER TABLE events ADD COLUMN recurrenceRule TEXT
```

### 1.4 UserPrefsRepository — 新增主题偏好

```kotlin
// 新增 key
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")  // "follow_system"/"always_light"/"always_dark"
```

---

## 2. Repository Layer Changes

### 2.1 QWeatherRepository — 15 天预报 + AQI + 生活指数

```kotlin
// 修改 getWeather():
// - weather7d → weather15d (fallback to 7d on error)
// - airNow 结果映射到 AirQuality data class
// - indices 结果映射到 List<LifeIndex>
// - 全部并行请求 (已有 coroutineScope + async 模式)
```

### 2.2 EventRepository — 重复事件展开

```kotlin
// 新增方法:
fun expandRecurringEvents(events: List<EventEntity>, startDate: LocalDate, endDate: LocalDate): List<CalendarEvent>

// 逻辑:
// 1. 遍历所有 events
// 2. 如果 recurrenceRule == null 或 "none" → 直接转换
// 3. 如果 "daily" → 生成 startDate..endDate 范围内每天一个实例
// 4. 如果 "weekly" → 生成同一星期几的实例
// 5. 如果 "monthly" → 生成同一日期的实例
// 6. 如果 "yearly" → 生成同一月日的实例
// 7. 如果 "custom:1,3,5" → 生成指定星期几的实例
```

### 2.3 Widget Refresh Worker

```kotlin
// 新建 WidgetRefreshWorker.kt
class WidgetRefreshWorker : CoroutineWorker {
    // 1. 读取 SP 中的城市坐标
    // 2. 调用 QWeatherApi.weatherNow()
    // 3. 更新 Room 缓存
    // 4. 调用 WeatherWidget.update() 刷新 Glance widget
}

// 注册: PeriodicWorkRequest, 1 hour, NetworkType.CONNECTED
```

---

## 3. UI Layer Changes

### 3.1 Skeleton Loading Screen (Req 7)

```
新建 SkeletonScreen.kt:
- 使用 Brush.linearGradient + InfiniteTransition 实现 shimmer
- 布局: 圆角矩形占位 (header 40dp, weather card 160dp, hourly row 80dp, forecast 200dp)
- 替换 HomeScreen 中的 LoadingContent()
```

### 3.2 Enhanced Temperature Chart (Req 8)

```
修改 TemperatureChart.kt:
- height: 120dp → 180dp
- 包裹在 horizontalScroll() 中
- 每个数据点下方显示日期 (d日)
- 每个数据点上方显示天气 emoji
- 点击数据点显示 tooltip (Popup)
- 宽度: max(screenWidth, days.size * 56.dp)
```

### 3.3 AQI Display Card (Req 3)

```
新建 AqiCard.kt:
- 圆形进度指示器 (AQI 0-500)
- 颜色映射: 优=绿, 良=黄, 轻度=橙, 中度=红, 重度=紫, 严重=褐
- 显示: AQI 数值 + 类别 + PM2.5 + PM10
- 放在 ExpandableDetailsCard 旁边或替换 airQuality 文字
```

### 3.4 Life Indices Card (Req 2)

```
修改 LifeIndexCard.kt:
- 从 QWeather indices API 获取真实数据
- Grid 布局: 2 列 × 4 行
- 每项: emoji + 名称 + 类别等级
- 点击展开详细描述
```

### 3.5 City Delete (Req 6)

```
修改 CityPickerSheet.kt:
- SwipeToDismiss 包裹 CityRow (仅 savedCities)
- 左滑显示红色删除背景
- 删除时调用 cityViewModel.removeCity(city)
- GPS 城市不可删除
```

### 3.6 Dark Mode (Req 9)

```
修改 Theme.kt:
- WeatherCalendarTheme 读取 theme preference
- 使用 isSystemInDarkTheme() 作为默认
- 设置页新增"主题"选项: 跟随系统/浅色/深色
- GlassCard, SettingsScreen 等非天气背景区域适配暗色
```

### 3.7 Weather Card Image Sharing (Req 10)

```
修改 ShareWeatherCard.kt:
- 新增 generateWeatherBitmap(): Bitmap
- 使用 Canvas 绘制: 渐变背景 + 城市 + 日期 + 温度 + 天气图标 + 品牌
- 保存到 cache dir → FileProvider URI → share intent (image/png)
- 失败时 fallback 到现有文本分享
```

### 3.8 Feels-Like Explanation (Req 11)

```
修改 CurrentWeatherCard.kt:
- 计算 abs(temperature - feelsLike)
- >= 3°C 时显示解释文字
- feelsLike < temperature: "风大体感偏冷" (windSpeed > 20) 或 "湿度高体感偏冷"
- feelsLike > temperature: "湿度高体感闷热" (humidity > 70) 或 "日照强体感偏热"
- 其他: "综合因素"
```

### 3.9 Event Color Picker (Req 12)

```
修改 CalendarScreen.kt DayDetailContent:
- 新增 ColorPicker composable: Row of 8 circles
- 颜色: 绿/蓝/红/橙/紫/粉/青/黄
- 选中显示 ✓
- 传递 color 到 onAddEvent/onUpdateEvent
```

### 3.10 Recurring Events UI (Req 4)

```
修改 CalendarScreen.kt DayDetailContent:
- 新增重复选择器: Row of chips (不重复/每天/每周/每月/每年/自定义)
- 自定义: 7 个星期按钮 (可多选)
- 删除时弹出 AlertDialog: 仅此次/此后所有/全部
```

---

## 4. Migration Plan

1. Database v3 → v4: 添加 `recurrenceRule` 字段
2. 向后兼容: 旧事件 recurrenceRule = null 视为不重复
3. WeatherData 新字段有默认值，不影响缓存解析

---

## 5. File Change Summary

| File | Change Type | Requirement |
|------|-------------|-------------|
| QWeatherApi.kt | 新增 weather15d | R1 |
| QWeatherRepository.kt | 15d + AQI + indices 映射 | R1, R2, R3 |
| WeatherRepository.kt | 传递 AQI/indices | R2, R3 |
| Models.kt | 新增 AirQuality, LifeIndex | R2, R3 |
| EventEntity.kt | 新增 recurrenceRule | R4 |
| AppDatabase.kt | MIGRATION_3_4 | R4 |
| EventRepository.kt | expandRecurringEvents | R4 |
| WidgetRefreshWorker.kt | 新建 | R5 |
| CityPickerSheet.kt | SwipeToDismiss | R6 |
| HomeScreen.kt | Skeleton + AQI + indices | R7, R2, R3 |
| TemperatureChart.kt | 增大 + 滚动 + tooltip | R8 |
| Theme.kt | 暗色模式 | R9 |
| UserPrefsRepository.kt | theme_mode | R9 |
| SettingsScreen.kt | 主题选项 | R9 |
| ShareWeatherCard.kt | Bitmap 生成 | R10 |
| CurrentWeatherCard.kt | 体感解释 | R11 |
| CalendarScreen.kt | 颜色选择 + 重复规则 | R12, R4 |
