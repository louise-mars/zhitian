# Lessons Learned — 知天开发笔记

## 1. 中国大陆 GPS 定位问题

### 问题现象

在中国大陆使用 Google Pixel 9 手机时，app 无法获取 GPS 定位，始终显示错误的城市（如"石家庄"或"北京"），而同一设备上 12306、高德地图等国内 app 能正常秒定位。

### 根本原因

**Google Play Services 的 FusedLocationProviderClient 在中国大陆不可用。**

Android 的定位体系分两层：
1. **Google Play Services（FusedLocationProvider）**：整合 GPS + WiFi + 基站 + 传感器，需要连接 Google 服务器获取 WiFi/基站数据库。在中国大陆，Google 服务器不可达，导致：
   - `lastLocation` 返回 null（无缓存）
   - `getCurrentLocation` 返回 null（无法通过网络辅助定位）
   - `requestLocationUpdates` 超时无回调（GPS 冷启动在室内需要 1-2 分钟，而 WiFi/基站定位完全不可用）

2. **Android 原生 LocationManager**：
   - `GPS_PROVIDER`：纯 GPS 卫星定位，室外可用但冷启动慢（30-120 秒）
   - `NETWORK_PROVIDER`：基站/WiFi 定位，**依赖 Google 服务器解析基站 ID → 坐标**，在中国同样不可用

### 为什么国内 app 能正常定位

12306、高德地图、百度地图等国内 app 集成了**高德定位 SDK** 或**百度定位 SDK**。这些 SDK：
- 拥有国内完整的基站 + WiFi 热点数据库（不依赖 Google）
- 通过基站/WiFi 信号即可在室内秒定位（精度到街道级别）
- 同时支持 GPS 卫星定位作为精度增强
- 服务器在国内，延迟低

### 解决方案

**集成高德定位 SDK（AMap Location SDK）。**

步骤：
1. 在[高德开放平台](https://lbs.amap.com/)注册，创建应用
2. 获取 Android Key（需要填写包名 + SHA1 签名）
3. 添加依赖：`implementation("com.amap.api:location:latest.integration")`
4. 在 `AndroidManifest.xml` 中声明 Key
5. 调用 `AMapLocationClient` 获取位置

关键代码：
```kotlin
val client = AMapLocationClient(context)
val option = AMapLocationClientOption().apply {
    locationMode = AMapLocationMode.Hight_Accuracy
    isOnceLocation = true
    isNeedAddress = true  // 直接返回城市名，无需反向地理编码
}
client.setLocationOption(option)
client.setLocationListener { location ->
    if (location.errorCode == 0) {
        // location.latitude, location.longitude
        // location.city → "张家口市"
        // location.district → "宣化区"
    }
}
client.startLocation()
```

### 额外收获

高德 SDK 的 `isNeedAddress = true` 会直接返回城市名和区县名，**不需要单独做反向地理编码**。这比之前尝试的 Nominatim、和风 GeoAPI 等方案都简单可靠。

### 注意事项

- 高德 Key 绑定包名 + SHA1 签名，debug 和 release 签名不同，需要分别配置
- 必须调用隐私合规接口：`AMapLocationClient.updatePrivacyShow()` 和 `updatePrivacyAgree()`
- 高德 SDK 的 Maven 仓库需要添加阿里云镜像：`maven { url = uri("https://maven.aliyun.com/repository/public") }`
- `play-services-location` 依赖可以移除（不再需要）

---

## 2. 中国大陆网络可达性问题

### 问题现象

以下海外 API 在中国大陆网络环境下完全不可达：
- `api.open-meteo.com`（天气数据）
- `nominatim.openstreetmap.org`（反向地理编码）
- `geocoding-api.open-meteo.com`（城市搜索）
- `air-quality-api.open-meteo.com`（空气质量）
- `ip-api.com` / `ipapi.co` / `ipwho.is`（IP 定位）

### 解决方案

所有核心功能改用国内可达的服务：
- **天气数据**：和风天气（QWeather），专属域名 `*.re.qweatherapi.com`
- **城市搜索**：和风天气 `v7/geo/city/lookup` 接口
- **定位 + 反向地理编码**：高德定位 SDK（直接返回城市名）
- **Open-Meteo**：保留为 fallback，但不阻塞主流程

### 架构教训

面向中国大陆用户的 app，**不能依赖任何海外 API 作为主数据源**。应该：
1. 主数据源用国内服务（和风天气、高德等）
2. 海外服务只作为 fallback，且必须设置短超时，不能阻塞 UI
3. OkHttp 超时设置要合理（国内服务 8-10 秒足够）

---

## 3. 和风天气 API 域名变更

### 问题现象

和风天气在 2026 年改为每个项目分配独立的 API Host（如 `k678m2r6e6.re.qweatherapi.com`），旧的 `devapi.qweather.com` 不再可用。

### 解决方案

在和风天气开发者控制台的项目设置中查看 "API Host"，将其配置到代码中。不同项目的域名不同，不能硬编码通用域名。

---

## 4. API Key 安全管理

### 问题

API Key 最初硬编码在 `build.gradle.kts` 中并提交到了 GitHub 公开仓库，导致 Key 泄露。

### 解决方案

- 从 `local.properties` 读取 Key（该文件在 `.gitignore` 中）
- 高德 Key 在 `AndroidManifest.xml` 中声明（APK 可反编译提取，但 Key 绑定了包名+SHA1，他人无法使用）
- 旧的泄露 Key 应在控制台中作废并重新申请

---

## 5. ViewModel init 与权限时序

### 问题

`HomeViewModel` 在 `init` 中调用 `loadData()`，但此时用户可能还在权限请求页面，定位权限尚未授予，导致定位失败。

### 解决方案

不在 `init` 中加载数据，改为在 HOME composable 的 `LaunchedEffect(Unit)` 中触发——此时用户一定已经通过了权限页面。

```kotlin
composable(Routes.HOME) {
    LaunchedEffect(Unit) {
        homeViewModel.loadData()
    }
    // ...
}
```


---

## 6. 和风天气 Geo API 路径

### 问题

城市搜索功能不工作——API 返回 404。

### 根因

和风天气的 Geo API 路径不是 `/v7/geo/city/lookup`，而是 `/geo/v2/city/lookup`。新版项目专属域名下，天气 API 和 Geo API 的路径前缀不同。

### 解决方案

```kotlin
@GET("geo/v2/city/lookup")  // 不是 v7/geo/city/lookup
suspend fun cityLookup(...)
```

### 教训

和风天气的 API 文档可能不完全准确，最好直接在浏览器中测试 URL 确认路径格式。

---

## 7. BottomSheet 中的手势冲突

### 问题

在 `ModalBottomSheet` 内部使用 `SwipeToDismissBox`（左滑删除），滑动手势被 BottomSheet 的拖拽手势拦截，导致删除功能无法触发。

### 解决方案

放弃滑动删除，改为行内删除按钮（🗑️ 图标）。在 BottomSheet 等有手势冲突的容器中，避免使用需要水平滑动的交互模式。

---

## 8. 温度折线图可见性

### 问题

温度折线图在蓝色天气背景上几乎不可见——低温线用了浅蓝色（`#4FC3F7`），和背景色几乎相同。

### 解决方案

- 高温线：金黄色（`#FFD54F`）
- 低温线：淡紫色（`#CE93D8`）
- 线条加粗到 4f，数据点加大到 5f

### 教训

选择图表颜色时必须考虑实际背景色。天气 app 的背景是动态渐变的，图表颜色需要在所有天气条件下都有足够对比度。

---

## 9. Room 数据库多实例风险

### 问题

`WeatherWidgetDataProvider` 和 `WeatherNotificationWorker` 各自创建独立的 Room 数据库实例，且 migration 配置不一致。Widget 使用了 `fallbackToDestructiveMigration()`，可能在 DB 版本升级时删除所有用户数据。

### 解决方案

所有数据库实例必须包含完整的 migration 链：
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
```

绝不使用 `fallbackToDestructiveMigration()`——它会在 migration 缺失时静默删除所有数据。

---

## 10. 重复事件的月末处理

### 问题

1月31日创建的"每月重复"事件，在2月（只有28/29天）不会出现——因为 `date.dayOfMonth == 31` 在2月永远不成立。

### 解决方案

```kotlin
rule == "monthly" -> {
    val targetDay = eventStart.dayOfMonth
    val lastDayOfMonth = date.lengthOfMonth()
    date.dayOfMonth == targetDay.coerceAtMost(lastDayOfMonth)
}
```

如果目标日期超过当月天数，取当月最后一天。同理处理闰年2月29日的年重复事件。


---

## 11. 错误状态下的用户逃生路径

### 问题

当天气加载失败时（网络错误、API 故障），错误页面只显示"重试"按钮。HeaderBar（包含城市选择器入口）不渲染，用户被锁死在重试循环中，无法手动选择城市。

### 解决方案

错误状态下也渲染 HeaderBar，让用户可以：
- 点击城市名打开城市选择器，手动选择一个城市
- 访问设置页
- 访问日历页

### 教训

任何错误/空状态页面都必须保留导航入口。用户不应该被"锁死"在任何状态中。

---

## 12. Throwable vs Exception 的区别

### 问题

分享天气图片时，`Bitmap.createBitmap(1080, 1440)` 在低内存设备上可能抛出 `OutOfMemoryError`。代码中 `catch (Exception)` 无法捕获 `Error`（OOM 是 Error 的子类），导致 app 崩溃。

### 解决方案

```kotlin
try {
    shareWeatherImage(...)
} catch (_: Throwable) {  // 捕获 Exception + Error
    shareWeatherText(...)  // fallback
}
```

### 教训

涉及大内存分配（Bitmap、大数组）的操作，catch 块应该用 `Throwable` 而不是 `Exception`。

---

## 13. rememberSaveable vs remember

### 问题

日历事件表单中所有输入状态（标题、描述、时间）使用 `remember`，在屏幕旋转时全部丢失。用户正在输入一个长描述，旋转手机后所有内容消失。

### 解决方案

对于用户输入的文本字段，使用 `rememberSaveable` 代替 `remember`：
```kotlin
var newTitle by rememberSaveable { mutableStateOf("") }
```

`rememberSaveable` 会在配置变更（旋转）和进程恢复时保留状态。

### 注意

`rememberSaveable` 只支持可序列化类型（String、Int、Boolean 等）。复杂对象需要自定义 Saver。

---

## 14. BottomSheet 内的手势冲突

### 问题

在 `ModalBottomSheet` 内部使用 `SwipeToDismissBox`（左滑删除城市），BottomSheet 的垂直拖拽手势会拦截水平滑动，导致删除功能完全无法触发。

### 解决方案

放弃滑动删除，改为行内删除按钮（🗑️ 图标）+ 确认对话框。在有手势冲突的容器中，避免使用需要特定方向滑动的交互。

### 适用场景

任何嵌套在可拖拽容器（BottomSheet、Drawer、ViewPager）中的交互，都应该避免依赖滑动手势。改用点击操作。


---

## 15. 和风天气免费版 API 权限限制

### 问题

空气质量 API (`/v7/air/now`) 返回 403 Forbidden："No permission to request this data"。

### 根因

和风天气免费开发版订阅不包含空气质量数据权限。这不是代码 bug，是 API 套餐限制。

### 解决方案

当 API 返回 403 或数据为 null 时，**直接隐藏 AQI 卡片**，而不是显示"暂无数据"。用户不需要看到一个永远无数据的空卡片。

```kotlin
if (airQuality != null) {
    AqiCard(airQuality = airQuality, ...)
}
```

### 教训

集成第三方 API 时，必须确认免费版包含哪些端点。不要假设所有文档中列出的 API 都可用。对于付费功能，UI 应该优雅降级（隐藏）而不是显示错误状态。
