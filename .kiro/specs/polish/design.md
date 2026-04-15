# Design — 天气日历 App

## 1. 整体架构

```
App
├── UI (Jetpack Compose)
│   ├── HomeScreen — 天气首页（HorizontalPager + 渐变背景 + 粒子动画）
│   ├── CalendarScreen — 日历月视图（滑动切月 + 天气联动）
│   ├── SettingsScreen — 设置页
│   ├── PermissionScreen — 权限请求页
│   ├── CityPickerSheet — 城市搜索 BottomSheet
│   └── Components
│       ├── GlassCard — 毛玻璃卡片（半透明 + 阴影）
│       └── WeatherAnimationOverlay — 天气粒子动画层
├── ViewModel (StateFlow)
│   ├── HomeViewModel — 首页数据（天气 + 日历事件 + 城市）
│   ├── CalendarViewModel — 日历数据（月份 + 天气 + 事件）
│   ├── CityViewModel — 城市搜索与管理
│   └── SettingsViewModel — 用户偏好
├── Repository
│   ├── WeatherRepository — 天气数据（API + Room 缓存 + AQI）
│   ├── CalendarRepository — 系统日历事件
│   ├── CityRepository — 城市搜索与收藏（DataStore）
│   └── UserPrefsRepository — 用户设置（DataStore）
├── Data
│   ├── Remote — API 接口
│   │   ├── WeatherApi — Open-Meteo 天气
│   │   ├── GeocodingApi — Open-Meteo 城市搜索
│   │   ├── NominatimApi — OpenStreetMap 反向地理编码
│   │   └── AirQualityApi — Open-Meteo 空气质量
│   ├── Local — Room 数据库
│   │   ├── AppDatabase
│   │   ├── WeatherDao
│   │   └── WeatherEntity（JSON 缓存）
│   └── Location — GPS 定位 + 反向地理编码
├── Widget (Jetpack Glance)
│   ├── WeatherWidget — Widget UI
│   ├── WeatherWidgetReceiver — 系统回调
│   ├── WeatherWidgetDataProvider — 数据获取（读 Room 缓存）
│   └── WeatherWidgetWorker — 定时刷新（WorkManager）
├── Notification
│   └── WeatherNotificationWorker — 天气变化通知（WorkManager）
└── DI (Hilt)
    └── AppModule — 依赖注入配置
```

## 2. 数据流

### 天气数据流
```
App 启动 → HomeViewModel.loadData()
    ↓
确定坐标：已选城市 > GPS 定位 > 默认城市
    ↓
WeatherRepository.getWeather(lat, lon)
    ├── 读 Room 缓存（< 30 分钟 → 直接返回）
    ├── 缓存过期 → 请求 Open-Meteo API → 写入 Room
    └── 同时请求 AirQualityApi（失败不影响主流程）
    ↓
HomeUiState 更新 → UI 重组
    ↓
同步城市信息给 Widget（SharedPreferences）
```

### Widget 数据流
```
WorkManager 每小时触发 → WeatherWidgetWorker
    ↓
WeatherWidgetDataProvider.fetchData()
    ├── 读 SharedPreferences 获取城市坐标
    └── 读 Room 缓存获取天气数据（不调 API）
    ↓
WeatherWidget.update() → Glance 渲染
```

### 通知数据流
```
WorkManager 每 6 小时触发 → WeatherNotificationWorker
    ↓
检查用户是否开启通知（SharedPreferences）
    ↓
读 Room 缓存 → 解析明日天气
    ├── 雨/雪 → 发送"记得带伞"通知
    └── 温差 > 10° → 发送"注意穿衣"通知
```

## 3. UI 设计

### 首页布局
```
┌─────────────────────────────┐
│ [城市名 ▾]    [📅] [⚙️]    │  HeaderBar
│ 4月15日 星期三 · 廿八        │
├─────────────────────────────┤
│                             │
│      ☀️                     │  HorizontalPager (3 页)
│     20°                     │  CurrentWeatherCard
│      晴                     │  (GlassCard, alpha=0.18)
│  体感 20°  适合外出          │
│                             │
│         ● ○ ○               │  PageIndicator
├─────────────────────────────┤
│ 小时预报                     │
│ [现在][14:00][15:00]...     │  HourlyForecastRow (LazyRow)
├─────────────────────────────┤
│ 7日预报                      │
│ 今天  4/15  ☀️ 10°~23°     │  DailyWeatherCalendarCard
│ 明天  4/16  🌧️ 13°~17°     │  (GlassCard, 可展开事件)
│ ...                         │
├─────────────────────────────┤
│ 详细信息  ▾                  │  ExpandableDetailsCard
│ 湿度 65%  风速 12km/h       │
│ UV 中等   空气 良            │
└─────────────────────────────┘
```

### 日历布局
```
┌─────────────────────────────┐
│ ← [◀ 2026年4月 ▶]          │  CalendarTopBar
├─────────────────────────────┤
│ 周一 周二 周三 ...  周日     │  WeekdayHeader
├─────────────────────────────┤
│       1    2    3    4    5 │
│       ☀️   ☀️              │  MonthGrid
│  6  [7]   8    9   10  ... │  (滑动切月手势)
│      小暑                   │  (今天=白色圆, 选中=蓝色圆)
│ ...                         │
├─────────────────────────────┤
│ ── 底部浮层（选中日期）──    │  ModalBottomSheet
│ 4月15日  星期三              │  (深色半透明背景)
│ 农历 廿八                   │
│ ┌─────────────────────┐    │
│ │ ☀️  晴  10°~23°     │    │  GlassCard (天气)
│ │     适合外出         │    │
│ └─────────────────────┘    │
│ 日程                        │
│ ● 10:00  Team Meeting      │
│ ● 18:00  Gym               │
└─────────────────────────────┘
```

### Widget 布局
```
┌──────────────────────────────┐
│ ☀️  20°           北京       │
│                 4月15日 周三  │
│                 农历 廿八     │
│ 晴    明天 🌧️ 雨 13°~17°   │
└──────────────────────────────┘
```

## 4. 天气渐变色映射

| 天气状态 | 首页渐变 | 日历渐变 |
|---------|---------|---------|
| 晴天 | #4FACFE → #00F2FE | #4FACFE → #00C6FB |
| 多云 | #89A0B0 → #546E7A | #BDC3C7 → #2C3E50 |
| 雨天 | #5F9EA0 → #2F4F4F | #5F9EA0 → #2F4F4F |
| 雪天 | #E8F0FE → #B8D4E8 | #D4E4F1 → #8EAFC2 |
| 雷暴 | #424242 → #212121 | #3D4E5C → #1A252F |
| 雾天 | #CFD8DC → #90A4AE | #B8C6D0 → #6E8898 |
| 夜间 | #1A237E → #0D47A1 | #141E30 → #243B55 |

## 5. 反向地理编码策略

```
GPS 坐标 (lat, lon)
    ↓
1. Android Geocoder（需要 Google Play Services）
    ↓ 失败
2. Nominatim API（OpenStreetMap，需要网络）
    ↓ 失败
3. 中国城市坐标查表（48 个主要城市，离线可用）
    ↓
返回最近城市名
```

## 6. 技术栈

| 层 | 技术 |
|---|------|
| UI | Jetpack Compose + Material3 |
| 导航 | Navigation Compose |
| DI | Hilt |
| 网络 | Retrofit + kotlinx.serialization |
| 缓存 | Room |
| 偏好 | DataStore Preferences |
| 定位 | Google Play Services Location |
| Widget | Jetpack Glance |
| 后台任务 | WorkManager |
| 构建 | Gradle KTS + Version Catalog |
| 混淆 | R8 + ProGuard 规则 |

## 7. API 端点

| API | 用途 | Base URL |
|-----|------|----------|
| Open-Meteo Forecast | 天气预报 | api.open-meteo.com |
| Open-Meteo Geocoding | 城市搜索 | geocoding-api.open-meteo.com |
| Open-Meteo Air Quality | 空气质量 | air-quality-api.open-meteo.com |
| Nominatim | 反向地理编码 | nominatim.openstreetmap.org |
