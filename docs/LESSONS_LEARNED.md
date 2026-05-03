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
