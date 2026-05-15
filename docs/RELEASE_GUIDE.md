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

## 八、功能清单（v1.3）

### 天气核心
- 15 天天气预报（和风天气 API）
- 空气质量指数（AQI + PM2.5/PM10，免费版不可用时自动隐藏）
- 8 种生活指数（运动/洗车/穿衣/紫外线/舒适度/感冒/空气/旅游）
- 天气预警（气象局实时数据，颜色边框醒目显示）
- 分钟级降雨预报
- 体感温度解释（风冷/湿热）
- 日/夜自动切换（基于日出日落）
- 今日宜忌（基于天气/温度/风速/季节的生活建议）

### 日程天气预警
- AlertEngine：15 天预报 × 日程交叉匹配
- 室内事件智能过滤（Zoom/Teams/线上会议等 20+ 关键词）
- 降水量阈值触发（>10mm，毛毛雨不打扰）
- 小时级精确匹配（今天的定时事件检查 ±1 小时天气）
- 灾害预警 × 日程关联（气象局预警自动关联今明两天事件）
- 本地推送通知（未来 3 天预警，每天最多 1 条）
- 温度阈值：≤0°C（冰冻）/ >35°C（高温）
- 预警卡片位于首页最醒目位置

### 日历与日程
- 日历事件管理（标题/描述/时间/颜色/重复规则/提醒）
- 重复事件（每天/每周/每月/每年，UI 下拉菜单选择）
- 事件提醒通知（AlarmManager 精确触发，设备重启自动恢复）
- 万年历（农历 + 节气 + 中外节日）
- 今日自动选中 + 脉冲动画

### 文化内容
- 每日诗词（1200 首，按天气/季节匹配，支持 3 年不重复）
- 左右滑动探索更多诗词（5 页 + 圆点指示器）
- 暖金色衬线字体突出显示
- 诗词收藏夹（"我的诗集"，设置页入口）
- 诗词桌面 Widget（每日自动刷新）
- 启动加载时居中显示诗词（仪式感）

### 视觉与动画
- 天气图标微动画（64dp Canvas：太阳脉冲/云漂移/雨滴/雪花/闪电）
- 背景渐变转场动画（700ms ease-in-out）
- 卡片入场动画（staggered slideIn + fadeIn）
- 温度大数字渐变色填充（72sp 极细字重）
- 动画降级管理（低电量/省电/低内存设备自动禁用）
- 尊重系统 Reduced Motion 无障碍设置
- 宫崎骏风格天气动画（丁达尔光束/手绘雨滴/萤火虫/季节花瓣）
- 骨架屏加载动画
- 温度趋势折线图（金黄+淡紫双线）
- 深色/浅色模式（跟随系统/手动切换，影响渐变背景）

### 其他
- 首页时段问候语（早安/上午好/午间/下午好/傍晚/晚上/夜深）
- 天气卡片图片分享（支持微信/微博，720×960 优化）
- 桌面天气 Widget（每小时自动刷新）
- 城市搜索与管理（搜索/添加/删除确认）
- 错误状态下保留导航入口
- 表单输入验证 + 旋转保持（rememberSaveable）
- 定位 fallback 链（GPS → 缓存 → 默认城市）
