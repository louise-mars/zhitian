# Android Studio 操作手册 — 天气日历

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
  - Google Play services

## 2. 打开项目

### 从本地打开
- File → Open → 选择 `Weather-calendar` 文件夹 → OK
- 等待 Gradle 同步完成（首次约 3-5 分钟，右下角进度条）

### 从 GitHub 克隆
- File → New → Project from Version Control
- URL: `https://github.com/louise-mars/zhitian.git`
- 点击 Clone

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
│   │   ├── location/            ← GPS 定位
│   │   ├── model/               ← 数据模型
│   │   ├── remote/              ← API 接口
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
- 常用过滤：`tag:WeatherRepository` 或 `level:error`

### 查看布局预览
- 打开任何 `@Composable` 函数所在的文件
- 右侧面板 **Split** 或 **Design** 模式可以预览 UI
- 带 `@Preview` 注解的函数会自动显示预览

### 调试
- 在代码行号左侧点击设置断点（红色圆点）
- 点击 🐛 按钮（Debug 模式运行）
- 程序会在断点处暂停，可以查看变量值

## 6. 构建 Release 版本

### 生成签名密钥（首次）
```bash
keytool -genkey -v -keystore weather-calendar.jks -keyalg RSA -keysize 2048 -validity 10000 -alias weather
```
按提示输入密码和信息。

### 配置签名
1. 在项目根目录创建 `keystore.properties`：
```
storeFile=weather-calendar.jks
storePassword=你的密码
keyAlias=weather
keyPassword=你的密码
```

2. `app/build.gradle.kts` 中添加：
```kotlin
android {
    signingConfigs {
        create("release") {
            val props = java.util.Properties()
            props.load(rootProject.file("keystore.properties").inputStream())
            storeFile = file(props["storeFile"] as String)
            storePassword = props["storePassword"] as String
            keyAlias = props["keyAlias"] as String
            keyPassword = props["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 构建 APK
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- 输出路径：`app/build/outputs/apk/release/app-release.apk`

### 构建 AAB（上架 Play Store 用）
- Build → Build Bundle(s) / APK(s) → Build Bundle(s)
- 输出路径：`app/build/outputs/bundle/release/app-release.aab`

## 7. 常见问题

### Gradle 同步失败
- 检查网络（公司网络可能需要代理）
- File → Invalidate Caches → Invalidate and Restart

### 编译错误 "Cannot resolve symbol"
- Build → Clean Project → Rebuild Project
- 或 File → Sync Project with Gradle Files

### 模拟器太慢
- 确保开启了 Intel HAXM 或 Hyper-V（Settings → SDK Tools → Intel x86 Emulator Accelerator）
- 或直接用真机调试

### App 闪退
- 查看 Logcat 中的红色错误日志
- 搜索 `FATAL EXCEPTION` 找到崩溃堆栈

### Widget 不显示
- 长按桌面 → 小组件 → 找到"天气日历"
- 如果看不到，重新安装 App

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
| 撤销 | Ctrl+Z |
| 重做 | Ctrl+Shift+Z |
