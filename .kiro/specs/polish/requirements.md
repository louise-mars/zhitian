# Requirements — 天气日历 App

## R1: 天气首页
- 显示当前天气（温度、体感温度、天气状况、天气图标）
- 小时预报（24 小时滚动列表，当前时间高亮）
- 7 日预报（每日天气、温度范围、日历事件融合）
- 详细气象指标（湿度、风速、UV 指数、空气质量）
- 天气驱动渐变背景（晴天蓝、雨天青灰、雪天冰蓝等 8 种天气状态）
- 背景渐变随聚焦日期平滑过渡（animateColorAsState, 500ms）
- HorizontalPager 支持左右滑动切换聚焦日（今天/明天/后天）
- 下拉刷新（PullToRefreshBox）

## R2: 天气动态粒子动画
- Canvas 粒子系统，叠在渐变背景之上、内容之下
- 晴天：双层光晕呼吸 + 6 条旋转光线
- 多云：3 层视差云漂移（远景慢/近景快）
- 雨天：100 滴雨滴（小雨 50 滴），独立速度/透明度/粗细
- 雪天：70 片雪花缓慢飘落 + 左右摆动
- 雷暴：密集雨 + 随机双闪电（2-5 秒间隔）
- 雾天：6 层雾带缓慢横向漂移
- 性能约束：雨滴 ≤ 120，云层 ≤ 7，~60fps

## R3: 日历月视图
- iOS 级极简设计：日期格子只显示数字 + 天气小图标
- 农历/节气/事件详情收到底部浮层（ModalBottomSheet）
- 三种日期状态：普通（白色数字）、今天（白色实心圆 + 深色数字）、选中（半透明蓝圆 + scale 1.1 弹性动画）
- 节气/节日在格子中显示红色小字
- 左右滑动切月（detectHorizontalDragGestures + AnimatedContent 滑入滑出动画）
- 箭头按钮切月
- 天气联动：选中日期 → 背景渐变平滑过渡 + 粒子动画切换
- 月份数据缓存（ConcurrentHashMap），坐标缓存，避免重复网络请求

## R4: 反向地理编码（城市名显示）
- 三级 fallback：Android Geocoder → Nominatim API → 中国城市坐标查表（48 个主要城市）
- 离线查表确保在无网络/无 Google 服务时也能显示城市名

## R5: 城市管理
- Open-Meteo Geocoding API 城市搜索（中文）
- 收藏城市列表（DataStore 持久化）
- 手动选择城市 / GPS 自动定位
- 城市切换后同步给 Widget

## R6: 桌面 Widget（Jetpack Glance）
- 显示：天气图标 + 温度 + 城市 + 日期 + 农历 + 明日天气摘要
- 背景色跟随天气变化
- 点击跳转 App
- 数据源：读 Room 缓存（和 App 共享同一数据库），不直接调 API
- WorkManager 每小时定时刷新，仅在有网络时执行

## R7: 天气通知
- 两种通知：明天有雨/雪提醒、温差 > 10° 提醒
- WorkManager 每 6 小时检查一次，读 Room 缓存判断
- 设置页提供开关（默认关闭，符合 Google Play 政策）
- Android 13+ 运行时请求 POST_NOTIFICATIONS 权限
- 开关联动 Worker 启停

## R8: 空气质量
- Open-Meteo Air Quality API（European AQI）
- 显示在详情卡：优/良/轻度/中度/重度/严重
- 请求失败不影响主流程（fallback 显示 "—"）

## R9: 设置
- 温度单位切换（°C / °F）
- GPS 定位开关
- 天气变化通知开关
- 默认城市设置
- 关于信息

## R10: 农历与节气
- 查表法农历计算（1900-2100 年）
- 24 节气（近似固定日期）
- 农历节日（春节、元宵、端午、中秋等）
- 日历格子中节气/节日红色高亮

## R11: 数据缓存策略
- Room 缓存天气 API 响应（JSON 字段）
- 缓存有效期 30 分钟
- 缓存过期 → 请求 API → 更新缓存
- API 失败 + 有旧缓存 → 返回旧数据（标记 fromCache）
- 自动清理 24 小时以上的旧缓存

## R12: 性能与电量
- 启动时只定位一次，不持续追踪
- Widget Worker 有网络约束
- 粒子动画数量受控
- 所有网络请求 try/catch + fallback
- ProGuard 规则覆盖 Retrofit、kotlinx.serialization、Glance、WorkManager、Room

## R13: 深色模式
- Material3 主题支持深色模式
- 天气页使用自定义渐变（不受系统主题影响）
- 设置页自动适配系统深色模式
