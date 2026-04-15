# Tasks — 天气日历 App

## Phase 1: 核心天气功能（已完成）
- [x] 1.1 Open-Meteo 天气 API 集成（当前天气 + 小时预报 + 每日预报）
- [x] 1.2 Room 缓存策略（30 分钟过期，JSON 字段存储）
- [x] 1.3 GPS 定位（FusedLocationProvider，启动时获取一次）
- [x] 1.4 城市搜索（Open-Meteo Geocoding API，中文）
- [x] 1.5 城市收藏与切换（DataStore 持久化）
- [x] 1.6 温度单位切换（°C / °F，TemperatureConverter 工具类）
- [x] 1.7 下拉刷新（PullToRefreshBox）

## Phase 2: 首页 UI（已完成）
- [x] 2.1 天气驱动渐变背景（8 种天气状态，animateColorAsState 过渡）
- [x] 2.2 CurrentWeatherCard（GlassCard，72sp Light 温度，阴影浮起）
- [x] 2.3 HourlyForecastRow（当前时间高亮，alpha 0.35 + elevation 8dp）
- [x] 2.4 DailyWeatherCalendarCard（7 日预报，可展开日历事件）
- [x] 2.5 ExpandableDetailsCard（湿度、风速、UV、空气质量）
- [x] 2.6 HorizontalPager 日期切换（3 页，渐变动画 + 页面指示器）
- [x] 2.7 HeaderBar（城市名、日期、农历、导航按钮）

## Phase 3: 天气粒子动画（已完成）
- [x] 3.1 WeatherAnimationOverlay 架构（WeatherCondition → Effect 组合）
- [x] 3.2 晴天效果（双层光晕呼吸 + 旋转光线）
- [x] 3.3 多云效果（3 层视差云漂移，7 朵云）
- [x] 3.4 雨天效果（100 滴雨滴，独立速度/透明度/粗细）
- [x] 3.5 雪天效果（70 片雪花，左右摆动）
- [x] 3.6 雷暴效果（120 滴密集雨 + 双闪电）
- [x] 3.7 雾天效果（6 层雾带漂移）

## Phase 4: 日历页（已完成）
- [x] 4.1 iOS 级极简日历网格（数字 + 天气小图标，去掉农历堆叠）
- [x] 4.2 今天高亮（白色实心圆 + 深色数字）
- [x] 4.3 选中态（半透明蓝圆 + scale 1.1 弹性动画）
- [x] 4.4 底部浮层详情（深色半透明背景，农历 + 天气 GlassCard + 事件列表）
- [x] 4.5 左右滑动切月（detectHorizontalDragGestures + AnimatedContent）
- [x] 4.6 天气联动背景（选中日期 → 渐变过渡 + 粒子切换）
- [x] 4.7 月份数据缓存 + 坐标缓存（避免重复网络请求）

## Phase 5: 农历与节气（已完成）
- [x] 5.1 查表法农历计算（LunarCalendar，1900-2100 年）
- [x] 5.2 24 节气（近似固定日期）
- [x] 5.3 农历节日（春节、元宵、端午、中秋等）
- [x] 5.4 日历格子节气/节日红色高亮

## Phase 6: 反向地理编码（已完成）
- [x] 6.1 Nominatim API 集成（OpenStreetMap 反向地理编码）
- [x] 6.2 Android Geocoder 作为首选
- [x] 6.3 中国城市坐标查表（48 个主要城市，离线 fallback）
- [x] 6.4 三级 fallback 链

## Phase 7: 桌面 Widget（已完成）
- [x] 7.1 Jetpack Glance Widget UI（温度 + 图标 + 城市 + 日期 + 农历）
- [x] 7.2 Widget 读 Room 缓存（不直接调 API，数据源统一）
- [x] 7.3 明日天气摘要（底部显示"明天 🌧️ 雨 13°~17°"）
- [x] 7.4 WeatherWidgetReceiver 注册（AndroidManifest）
- [x] 7.5 WeatherWidgetWorker 定时刷新（每小时，网络约束）
- [x] 7.6 点击跳转 App（actionStartActivity）
- [x] 7.7 城市信息同步（HomeViewModel → SharedPreferences → Widget）

## Phase 8: 天气通知（已完成）
- [x] 8.1 NotificationChannel 注册（weather_alerts）
- [x] 8.2 WeatherNotificationWorker（每 6 小时检查，读 Room 缓存）
- [x] 8.3 雨/雪提醒通知
- [x] 8.4 温差 > 10° 提醒通知
- [x] 8.5 设置页通知开关（默认关闭）
- [x] 8.6 Android 13+ POST_NOTIFICATIONS 运行时权限请求
- [x] 8.7 开关联动 Worker 启停

## Phase 9: 空气质量（已完成）
- [x] 9.1 AirQualityApi 集成（Open-Meteo European AQI）
- [x] 9.2 AQI → 中文等级映射（优/良/轻度/中度/重度/严重）
- [x] 9.3 集成到 WeatherRepository（独立请求，失败不影响主流程）
- [x] 9.4 ExpandableDetailsCard 显示空气质量

## Phase 10: 7 日预报扩展（已完成）
- [x] 10.1 HomeViewModel 数据范围 3 天 → 7 天
- [x] 10.2 日历事件查询范围 2 天 → 6 天
- [x] 10.3 首页 section 标签"近三天" → "7日预报"

## Phase 11: 构建与发布准备（已完成）
- [x] 11.1 ProGuard 规则（Retrofit、kotlinx.serialization、Glance、WorkManager、Room、Nominatim）
- [x] 11.2 自适应 App 图标（太阳 + 日历网格）
- [x] 11.3 深色模式支持（Material3 主题 + 自定义渐变）
- [x] 11.4 POST_NOTIFICATIONS 权限声明

## Phase 12: UI 视觉统一（已完成）
- [x] 12.1 GlassCard 统一阴影（elevation 8dp，ambientColor 0.1f）
- [x] 12.2 圆角统一 20dp
- [x] 12.3 间距体系（8dp 小 / 16dp 标准 / 24dp 大）
- [x] 12.4 天气渐变色升级（高饱和度，晴天 #4FACFE → #00F2FE）
- [x] 12.5 温度字体强化（72sp Light + letterSpacing）
- [x] 12.6 设置页可滚动

## 待做（后续版本）
- [ ]* 多语言支持（字符串资源抽取）
- [ ]* 分享天气卡片（截图分享到社交媒体）
- [ ]* 日历事件写入（添加日程功能）
- [ ]* 小尺寸 Widget（2x2）
- [ ]* 日程 + 天气联动通知（"10:00 有会议 + 下雨 → 建议提前出门"）
