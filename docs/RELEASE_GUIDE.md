# 发布指南 — 知天

## 一、前置条件

### API Keys
确保 `local.properties` 中配置了：
```properties
QWEATHER_API_KEY=你的和风天气Key
```

高德定位 Key 已硬编码在 `AndroidManifest.xml` 中（发布时需要用 release 签名的 SHA1 重新申请）。

### 签名密钥
Release 版本需要签名密钥。首次生成：

```bash
keytool -genkey -v -keystore zhitian-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias zhitian
```

将 `zhitian-release.jks` 保存在安全位置，**不要提交到 Git**。

## 二、配置签名

在项目根目录创建 `keystore.properties`（已加入 .gitignore）：

```properties
storeFile=../zhitian-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=zhitian
keyPassword=YOUR_KEY_PASSWORD
```

## 三、构建

### Debug APK（测试用）
```bash
./gradlew :app:assembleDebug
```
输出：`app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew :app:assembleRelease
```
输出：`app/build/outputs/apk/release/app-release.apk`

### AAB（上架 Google Play 用）
```bash
./gradlew bundleRelease
```
输出：`app/build/outputs/bundle/release/app-release.aab`

## 四、发布前检查清单

- [ ] 递增 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`
- [ ] 和风天气 API Key 有效（免费版每天 1000 次调用）
- [ ] 高德定位 Key 对应 release 签名的 SHA1（需要在高德控制台添加）
- [ ] ProGuard 规则正确（`proguard-rules.pro`）
- [ ] 在真机上测试 release 版本的定位和天气功能

## 五、高德定位 Key 注意事项

高德 Key 绑定了应用包名 + SHA1 签名。Debug 和 Release 签名不同，需要：

1. 获取 release 签名的 SHA1：
```bash
keytool -list -v -keystore zhitian-release.jks -alias zhitian
```

2. 在[高德控制台](https://console.amap.com/)添加 release SHA1

3. 或者创建两个 Key（debug 和 release），通过 `AndroidManifest.xml` 的 placeholder 区分

## 六、版本号规范

- `versionCode`：每次发布递增（1, 2, 3...）
- `versionName`：语义化版本（1.0.0, 1.1.0, 2.0.0）

## 七、数据安全声明（Google Play 要求）

| 问题 | 回答 |
|------|------|
| 是否收集用户数据？ | 是 — 位置数据（精确位置） |
| 是否共享用户数据？ | 是 — 与高德（定位）和和风天气（天气查询/城市搜索）共享位置坐标 |
| 数据是否加密传输？ | 是（HTTPS） |
| 用户能否请求删除数据？ | 是（清除应用数据即可） |
| 是否面向儿童？ | 否 |

## 八、功能清单（v1.1）

- 15 天天气预报（和风天气 API）
- 空气质量指数（AQI + PM2.5/PM10）
- 8 种生活指数（运动/洗车/穿衣/紫外线/舒适度/感冒/空气/旅游）
- 日历事件管理（标题/描述/时间/颜色/重复规则/提醒）
- 重复事件（每天/每周/每月/每年/自定义星期）
- 事件提醒通知
- 天气预警
- 分钟级降雨预报
- 每日诗词（根据天气/季节匹配，400+ 首，点击展开完整全文）
- 天气卡片图片分享（带 fallback 到文本分享）
- 桌面 Widget（每小时自动刷新）
- 深色模式（跟随系统/手动切换）
- 骨架屏加载动画
- 温度趋势折线图（金黄+淡紫双线，可点击查看详情）
- 城市搜索与管理（搜索/添加/删除确认）
- 体感温度解释（风冷/湿热）
- 日/夜自动切换（基于日出日落）
- 错误状态下保留导航入口
- 表单输入验证（标题长度/时间格式）
- 表单状态旋转保持（rememberSaveable）
