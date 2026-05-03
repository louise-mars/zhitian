# Android Studio 操作手册 — 知天

## 1. 环境准备

### 安装 Android Studio
- 下载：https://developer.android.com/studio
- 安装时勾选 Android SDK、Android Virtual Device
- 首次启动会自动下载 SDK（约 2-3GB）

### 检查 SDK 版本
- File → Settings → Languages & Frameworks → Android SDK
- 确保安装了 **API 35**（Android 15）
- SDK Tools 中确保安装了：
  - Android SDK Build-Tools
  - Android SDK Platform-Tools

## 2. 打开项目

### 从本地打开
- File → Open → 选择 `zhitian` 文件夹 → OK
- 等待 Gradle 同步完成（首次约 3-5 分钟，右下角进度条）

### 从 GitHub 克隆
- File → New → Project from Version Control
- URL: `https://github.com/louise-mars/zhitian.git`
- 点击 Clone

### 配置 API Keys
项目需要两个 API Key，配置在 `local.properties`（不提交到 Git）：

```properties
sdk.dir=你的SDK路径
QWEATHER_API_KEY=你的和风天气Key
```

- **和风天气 Key**：在 [和风天气开发者控制台](https://console.qweather.com/) 申请
- **高德定位 Key**：在 [高德开放平台](https://lbs.amap.com/) 申请，已配置在 AndroidManifest.xml 中

### Gradle 同步失败？
- File → Sync Project with Gradle Files（工具栏大象图标）
- 如果提示 JDK 版本不对：File → Settings → Build → Gradle → Gradle JDK → 选择 17
- 如果在公司网络需要代理：File → Settings → Appearance → System Settings → HTTP Proxy

## 3. 运行 App

### 连接真机（推荐）
1. 手机开启 **开发者选项**（设置 → 关于手机 → 连续点击版本号 7 次）
2. 开启 **USB 调试**
3. USB 连接电脑，手机弹出授权对话框 → 允许
4. Android Studio 顶部设备下拉框会显示你的手机型号

### 使用模拟器
1. Tools → Device Manager → Create Device
2. 选择 Pixel 7 → Next
3. 选择 API 35 系统镜像 → Download → Next → Finish
4. 点击 ▶ 启动模拟器

### 运行
- 顶部选择设备 → 点击绿色 ▶ 按钮（或 Shift+F10）
- 首次编译约 2-3 分钟，后续增量编译约 10-30 秒
- App 会自动安装到设备并启动

## 4. 项目结构导航

```
app/src/main/
├── java/com/weathercalendar/
│   ├── MainActivity.kt          ← App 入口
│   ├── WeatherCalendarApp.kt    ← Application 类
│   ├── data/                    ← 数据层
│   │   ├── local/               ← Room 数据库
│   │   ├── location/            ← 高德定位
│   │   ├── model/               ← 数据模型
│   │   ├── remote/              ← API 接口（和风天气）
│   │   └── repository/          ← 数据仓库
│   ├── di/                      ← Hilt 依赖注入
│   ├── notification/            ← 天气通知
│   ├── ui/                      ← UI 层
│   │   ├── calendar/            ← 日历页
│   │   ├── city/                ← 城市选择
│   │   ├── components/          ← 通用组件
│   │   ├── home/                ← 首页
│   │   ├── navigation/          ← 导航
│   │   ├── permission/          ← 权限请求
│   │   ├── settings/            ← 设置页
│   │   └── theme/               ← 主题/颜色
│   ├── util/                    ← 工具类
│   └── widget/                  ← 桌面 Widget
├── res/                         ← 资源文件
│   ├── drawable/                ← 图标
│   ├── layout/                  ← Widget 布局
│   ├── values/                  ← 字符串/颜色/主题
│   └── xml/                     ← Widget 配置
└── AndroidManifest.xml          ← App 配置
```

### 快速跳转
- **双击 Shift** → 搜索任何文件/类/方法
- **Ctrl+N** → 搜索类名
- **Ctrl+Shift+N** → 搜索文件名
- **Ctrl+B** → 跳转到定义
- **Alt+F7** → 查找所有引用

## 5. 常用操作

### 修改代码后运行
- 修改 → Ctrl+S 保存 → 点击 ▶ 运行
- 或使用 **Apply Changes**（闪电图标 ⚡）热更新，不重启 App

### 查看日志
- 底部 **Logcat** 标签页
- 过滤器输入 `com.weathercalendar` 只看本 App 日志
- 常用过滤：`tag:LocationService` 或 `tag:WeatherRepo` 或 `level:error`

### 查看布局预览
- 打开任何 `@Composable` 函数所在的文件
- 右侧面板 **Split** 或 **Design** 模式可以预览 UI
- 带 `@Preview` 注解的函数会自动显示预览

### 调试
- 在代码行号左侧点击设置断点（红色圆点）
- 点击 🐛 按钮（Debug 模式运行）
- 程序会在断点处暂停，可以查看变量值

## 6. 技术架构

### 数据源
| 服务 | 用途 | 备注 |
|------|------|------|
| 和风天气 | 天气数据 + 城市搜索 | 主力数据源，国内可达 |
| 高德定位 SDK | GPS + 基站 + WiFi 混合定位 | 国内秒定位，直接返回城市名 |
| Open-Meteo | 天气数据备用源 | 海外服务器，国内不可达，仅作 fallback |

### 定位策略
- 主力：高德定位 SDK（国内秒定位，精确到区县）
- 高德直接返回坐标 + 城市名 + 区县名，无需额外反向地理编码
- 详见 [LESSONS_LEARNED.md](./LESSONS_LEARNED.md) 中的 GPS 定位问题分析

### 缓存策略
- Room 数据库缓存天气数据，30 分钟内有效
- 启动时先显示缓存，后台静默刷新
- 非强制刷新时利用缓存，避免重复网络请求

## 7. 构建 Release 版本

详见 [RELEASE_GUIDE.md](./RELEASE_GUIDE.md)

## 8. 快捷键速查

| 操作 | 快捷键 |
|------|--------|
| 运行 | Shift+F10 |
| 调试 | Shift+F9 |
| 搜索所有 | 双击 Shift |
| 搜索类 | Ctrl+N |
| 搜索文件 | Ctrl+Shift+N |
| 跳转定义 | Ctrl+B |
| 查找引用 | Alt+F7 |
| 格式化代码 | Ctrl+Alt+L |
| 注释/取消注释 | Ctrl+/ |
| 重命名 | Shift+F6 |
| 提取方法 | Ctrl+Alt+M |
| 自动导入 | Alt+Enter |
