# 知天 — 天气×日程×诗词

> 不只是看天气，更是懂生活。

**知天**是一个将天气预报、日程管理与中国传统文化融为一体的 Android 智能生活助手。

[![Release](https://img.shields.io/github/v/tag/louise-mars/zhitian?label=version&color=blue)](https://github.com/louise-mars/zhitian/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Poetry](https://img.shields.io/badge/诗词库-1200首-gold)]()
[![API](https://img.shields.io/badge/天气-和风天气-blue)]()

---

## ✨ 为什么选择知天

| 别人的天气 App | 知天 |
|---|---|
| "明天有雨" | "明天有雨，而且你有户外会议，建议带伞" |
| 打开看温度，关掉 | 打开看温度，看诗，看日程冲突 |
| 开屏广告 3 秒 | 开屏一句诗 |
| 数据上传云端 | 数据留在你手机里 |

**核心差异化：日程天气预警引擎 — 市场上没有第二个天气 App 能做到。**

---

## 🚀 核心功能

### 🌤️ 精准天气
- 15 天预报 · 分钟级降雨 · 8 种生活指数
- 体感温度解释（风冷/湿热）
- 气象局预警实时推送（红/橙/黄/蓝色醒目显示）

### ⚠️ 日程天气预警（独家）
- 自动检测未来 15 天日程与恶劣天气冲突
- 智能过滤：Zoom/线上会议不打扰
- 精确到小时：上午会议不会因为晚上暴雨而误报
- 主动推送：未来 3 天有冲突时发送系统通知

### 📖 每日诗词
- **1200 首**唐诗宋词，按天气/季节智能匹配
- 左右滑动探索更多 · 点击展开全文 · ❤️ 收藏
- 桌面 Widget 诗词模式 · 3 年不重复

### 📅 万年历
- 农历 + 24 节气 + 中外节日
- 日程管理：重复事件 / 颜色分类 / 精确提醒
- 今日宜忌（基于天气的生活建议）

### 🎨 东方美学
- 宫崎骏风格天气动画（丁达尔光束/手绘雨滴/萤火虫）
- 天气图标微动画（太阳脉冲/云漂移/雪花飘落）
- 背景渐变 700ms 平滑过渡
- 低电量/省电模式自动降级

---

## 📱 截图

> *（安装后截图补充）*

---

## 🔒 隐私承诺

- ❌ 无广告
- ❌ 无追踪
- ❌ 无后台偷跑
- ✅ 数据全部本地存储
- ✅ 开源可审计

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | UI 框架 |
| Hilt | 依赖注入 |
| Room | 本地数据库 |
| Jetpack Glance | 桌面 Widget |
| 和风天气 API | 天气数据（中国区最佳） |
| 高德定位 SDK | GPS 定位（中国区可用） |
| AlarmManager | 精确事件提醒 |
| Canvas | 天气动画 |

---

## 📦 构建

```bash
# 1. 克隆仓库
git clone https://github.com/louise-mars/zhitian.git
cd zhitian

# 2. 配置 API Key
echo "QWEATHER_API_KEY=你的和风天气Key" >> local.properties

# 3. 构建 Debug APK
./gradlew :app:assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

> 需要 Android Studio + JDK 17+。详见 [发布指南](docs/RELEASE_GUIDE.md)。

---

## 📚 文档

| 文档 | 内容 |
|------|------|
| [产品定位与架构](docs/PRODUCT_VISION.md) | 定位、差异化、架构设计、使用指南 |
| [发布指南](docs/RELEASE_GUIDE.md) | 构建、签名、发布流程 |
| [更新日志](docs/CHANGELOG.md) | 版本历史 |
| [开发笔记](docs/LESSONS_LEARNED.md) | 20 条经验教训 |
| [广告文案](docs/AD_COPY.md) | 6 个版本的推广文案 |
| [隐私政策](docs/PRIVACY_POLICY.md) | 数据收集说明 |

---

## 🗺️ 路线图

- [x] v1.0 — 基础天气+日历+诗词
- [x] v1.1 — 15天预报、生活指数、Widget、动画
- [x] v1.2 — 日程天气预警引擎、图标微动画、转场动画
- [x] **v1.3** — 诗词收藏夹、今日宜忌、1200首诗词、UI打磨 ← 当前
- [ ] v2.0 — 一键心情打卡、分享海报、月末回顾

---

## 📄 License

MIT License. 诗词内容来自公共领域古典文学作品。

---

*知天，知时节，知冷暖，知进退。*
