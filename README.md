# Huoleme_Win

活了么软件Windows版本

本仓库只包含 Windows 桌面端源码，Android 端不在本仓库。仓库根目录即 Gradle 工程，克隆后可直接构建。

「活了么」Android 版的 Windows 桌面迁移版本，采用 Kotlin + Compose Multiplatform Desktop。所有数据只保存在本机，**不依赖任何云服务器**。

## 技术栈

- Kotlin 2.4.10 / JVM 21
- Compose Multiplatform Desktop 1.12.2（Material 3）
- kotlinx.serialization（本地 JSON）、kotlinx-coroutines-swing
- Gradle Kotlin DSL + Gradle Wrapper
- `jpackage` 目录版分发（exe 启动器 + 精简 JRE，非单文件 exe）
- 数据文件：`%APPDATA%\Huoleme\data.json`

不使用 Android SDK、`android.*`、SharedPreferences、Toast、Activity 等 Android 平台依赖。

## 主要功能

- 每日打卡、连续天数、最后打卡时间（精确到秒）、本地打卡档案
- 5 套配色（蓝 / 红 / 绿 / 橙 / 自定义）+ 深色 / 浅色 / 跟随系统 + 5 档字号
- 自定义颜色：选中「自定义」后才显示，默认折叠；展开后为红 / 绿 / 蓝滑块 + 实时预览 + HEX 精确输入
- 生存遗言、生日与「已生存时间」
- JSON 导入 / 导出，兼容 Android 版备份
- Windows 系统托盘：打开、快速打卡、退出；关闭窗口默认驻留托盘
- 每日定时提醒：到点若未打卡则发送 Windows 通知，每天最多一次
- **开机自启动**：登录后自动在托盘后台运行，不显示主界面（见下节）
- 窗口加宽 / 最大化时自动切换为左侧 NavigationRail，内容区加宽居中
- 首次启动引导、单实例锁、开发者二级页面（标题连点 7 次 + 密码）
- 清晰字体渲染：显式指定系统中文字体族与真实字重、整数 sp 字号、ClearType / DPI 参数
- 15 个单元测试覆盖数据持久化、JSON 备份、颜色解析与 RGB 往返、自启动设置兼容

明确已移除：GPS / IP 定位、摇一摇、广告逻辑、Google / iOS 主题、SOS 求救信标、**Windows 任务计划程序与 headless 提醒窗口**。本项目不做安装包签名。

## 开机自启动

应用默认开启「设置 → 通知提醒 → 开机自动启动」，首次启动即写入当前用户的注册表启动项，无需管理员权限：

```
HKCU\Software\Microsoft\Windows\CurrentVersion\Run
  Huoleme = "D:\...\活了么.exe" autostart
```

- 参数使用普通词 `autostart`（jpackage 的 Windows 启动器会吞掉 `--xxx` 参数）。
- 带该参数启动时**只安装托盘，主窗口保持隐藏**；点击托盘图标或菜单「打开活了么」才显示界面。
- 托盘、提醒循环与自启动同步都运行在 `application` 作用域，因此窗口隐藏时提醒依然有效。
- 每次启动会用当前 exe 路径重写启动项，移动 / 重命名目录后不会指向失效路径。写入优先用 `reg.exe`；若被组策略 / 安全软件禁用，则自动回退到隐藏的 PowerShell `New-ItemProperty`，并做写后校验。
- 关闭开关会移除该注册表项；`gradlew run` 开发模式不会写入注册表。
- 系统托盘不可用时（极少见），开机自启动会兜底显示主窗口，避免"启动了却找不到"。

提醒逻辑因此不再需要计划任务：应用开机常驻托盘，由运行中的主程序在设定时间发送通知。

## 通知与托盘

- 关闭窗口默认隐藏到系统托盘，不会退出；设置页可关闭该行为。
- 托盘菜单：`打开活了么` / `快速打卡` / `退出活了么`。回调与窗口清理都延迟到事件派发结束，退出带 2.5 秒兜底。
- 托盘菜单使用 `Microsoft YaHei UI`（缺失时回退系统 CJK 字体），并按 DPI 缩放字号。
- 重复启动只保留第一个实例；单实例锁位于 `%LOCALAPPDATA%\Huoleme`。
- 若收不到通知，请在 Windows 设置 → 系统 → 通知 中允许「活了么」，并检查专注助手。
- 开发者二级页面提供「发送测试通知」以及记录回填 / 撤销 / 清空等本地调试功能。

## 字体清晰度与资源占用

字体模糊的三个来源已分别处理：

1. `FontFamily.Default` 在 Windows 会回退到拉丁字体、并对中文标题做伪粗体 → 改为按系统可用字体显式构造 `Microsoft YaHei UI` 的 Regular / Medium / SemiBold / Bold 四个真实字重。
2. 小数 `fontScale` 会产生非整数像素字号 → 字号统一为整数 sp，缩放时四舍五入。
3. 未声明 DPI / 文本抗锯齿 → 启动参数加 `-Dsun.java2d.dpiaware=true`、`-Dawt.useSystemAAFontSettings=lcd`、`-Dswing.aatext=true`，Swing 组件按 DPI 缩放字号。

体积与内存：

- 只依赖 `material-icons-core`，不再打包 37 MB 的 `material-icons-extended`。
- `createSlimRuntime` 用 jlink 以 `--compress=zip-6` 重建最小 JRE（约 30 MB 小于 Compose 默认 runtime）。
- 启动器限制 `-Xms32m -Xmx384m -XX:+UseSerialGC`，缩小元空间 / 代码缓存与协程线程池；提醒循环由 20 秒改为 60 秒轮询。
- 打包时跳过空的兼容占位 jar。

## 运行

要求：完整 JDK 21+（含 `jpackage.exe` / `jlink.exe`）。

```powershell
$env:JAVA_HOME = "C:\path\to\full-jdk-21"
.\gradlew.bat run
```

首次构建需要联网下载 Gradle 与 Maven 依赖；Wrapper 已指向可访问的镜像。

## 构建与分发

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat packagePortable
```

目录版输出（**不是单文件 exe**，需与 `app\`、`runtime\` 一起分发）：

```
build\dist\活了么\
  活了么.exe
  app\
  runtime\
```

`build\output\活了么\` 是历史版本，打包任务不会改动它。若 `build\dist` 被运行中的实例占用，可指定其他输出目录：

```powershell
.\gradlew.bat packagePortable -PdistDir=dist-new
```

`test` 覆盖 JSON 备份导出 / 导入（含 Android 数组格式）、颜色代码与 RGB 往返、Roaming 数据兼容、自启动设置的持久化与旧字段忽略。

## 数据位置

```
%APPDATA%\Huoleme\data.json
```

- `first_launch_done`：是否完成首次启动引导
- `settings`：主题、自定义色、深色模式、字号、生日、遗言、通知时间、动画、托盘行为、`auto_start_enabled`
- `records`：打卡记录（`date` / `timestamp` / `note` / `location`）
- `last_notified_date`：最近一次提醒日期

```json
{
  "schema_version": 1,
  "app": "活了么",
  "version": "1.3.6",
  "first_launch_done": false,
  "settings": {
    "theme_mode": "CLASSIC_BLUE",
    "notification_hour": 20,
    "notification_minute": 0,
    "minimize_to_tray": true,
    "auto_start_enabled": true
  },
  "records": [
    { "date": "2026-11-07", "timestamp": 1794043165865, "note": "", "location": "" }
  ],
  "last_notified_date": ""
}
```

写入采用临时文件 + 原子替换。旧版 `records.json` 与 `java.util.prefs` 设置会在首次启动自动迁移；旧备份里的 `background_reminder_enabled` 会被忽略（未知键兼容）。Windows 端不采集位置，但保留 `location` 字段以兼容 Android 备份。

## 目录结构

```
src/main/kotlin/com/huoleme/app/
  Main.kt                       入口、autostart 参数、托盘 / 提醒 / 自启动的应用级生命周期
  App.kt                       Scaffold / 顶部栏 / 底部导航或左侧 NavigationRail / 二级页面路由
  AppInfo.kt / Strings.kt      应用信息与中文文案
  SingleInstance.kt            全局单实例文件锁
  OnboardingScreen.kt          首次启动引导
  DeveloperPasswordPage.kt     开发者密码二级页面
  DeveloperPage.kt             开发者工具二级页面
  DashboardPage.kt             主页
  HistoryPage.kt               打卡记录
  SettingsPage.kt              设置 / 通知 / 自启动 / 生日 / 遗言 / 备份 / 关于
  desktop/AutoStart.kt         HKCU Run 自启动注册 / 查询 / 移除
  desktop/DesktopTray.kt       Windows 托盘与通知
  theme/                       配色、主题、字体族与字号
  data/                        Roaming 数据、记录模型、设置、应用状态、备份解析
  ui/                          触摸滚动、生日编辑器、颜色代码、字体、文件选择
src/test/kotlin/com/huoleme/app/
  data/AppStatePersistenceTest.kt
  data/AutoStartSettingsTest.kt
  data/BackupServiceTest.kt
  data/RoamingStoreTest.kt
  ui/ColorCodeTest.kt
src/main/resources/
  icon.png / icon.ico          窗口与托盘图标
```