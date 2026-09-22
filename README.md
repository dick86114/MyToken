# MyToken

MyToken 是一个 macOS 菜单栏用量监控工具，用于在本地查看多个大模型供应商的用量、余额、剩余额度和重置时间。

## 功能

- 菜单栏最多显示 5 个选中凭证的多指标图标（供应商短码 + 竖向进度条），支持自定义颜色规则。
- 左键打开用量弹窗：简洁 / 完整双模式玻璃卡片，首字母头像、环形用量图（2 项横排、3 项竖排）、余额大金额与充足 / 偏低 / 不足状态徽章，深浅色主题自适应。
- 高占用一目了然：≥80% 显示「即将耗尽」，50%–80% 显示「用量偏高」；刷新中卡片边框播放流星动效。
- 刷新失败时头像右上角显示红色徽章，点击查看失败详情并一键重试。
- 弹窗内百分比统一四舍五入为整数。
- 支持 Routin、DeepSeek、GLM Coding Plan、火山方舟个人 Agent Plan/Coding Plan、New API、Command Code 和小米 MiMo。
- 小米 MiMo 可分别查询 API 按量余额/消费或 Token Plan 订阅 Credits，额度与余额不会混算。
- 小米 MiMo API 卡片按“账户余额、累计消费、现金余额、赠送余额”和“历史消耗、输出、命中缓存、未命中缓存”两行展示，Token 数值使用千分位格式。
- 右键菜单支持切换账号、打开设置、检查更新和退出应用。
- 支持从右键菜单或设置页提交问题，自动生成脱敏日志并打开 GitHub Issue 页面。
- 支持 5 小时和周用量维度，以及别名、别名加竖向进度条等显示样式。
- 支持 50% 和 80% 用量阈值通知，并可自定义阈值。
- 支持按 1、5、15 或 30 分钟自动刷新，以及登录时自动启动。
- 从 GitHub Release 检查并安装应用更新。
- 设置窗口支持缩放，界面使用 macOS 液态玻璃风格。

## 系统要求

- macOS 14.0 或更高版本。
- 本地构建需要 Xcode 26 或更高版本、macOS 26 SDK 或更高版本和 XcodeGen。

```bash
brew install xcodegen
```

## Android 客户端

MyToken 提供 Android 伴侣应用（**要求 Android 10+**）：通过"迁移到 Android"
扫码把 Mac 上的供应商凭证端到端加密迁移到手机，即可在 Android 上查看用量、
余额与重置时间，接收用量提醒并完成 Routin 每日签到。

- 构建与模块结构：见 `docs/android/README.md`。
- 迁移前置条件与失败排查：见 `docs/android/transfer-troubleshooting.md`。
- 跨端迁移协议：见 `shared/transfer-schema/wire-contract.md`。

```bash
cd android
./gradlew test                 # 全模块 JVM 单元测试
./gradlew :app:assembleDebug   # 构建 Debug APK
```

Android 端凭证密钥经 Android Keystore 加密保存在本机（不进入云备份）；
迁移过程密钥仅以密文经局域网点对点传输。

## 安装使用

1. 下载对应版本的安装包（例如 `MyToken-1.2.0-arm64.dmg`），或从 GitHub Release 下载正式版本。
2. 打开 DMG，将 `MyToken` 拖入“应用程序”文件夹。
3. 启动应用，在设置中选择供应商并手动添加 API Key 或 Access Key/SecretAccessKey。
   小米 MiMo 当前需要从官方控制台复制网页 Cookie 或 `api-platform_serviceToken` 值，并选择 API 按量或 Token Plan。
4. 点击菜单栏中的 MyToken 图标查看用量。

首次运行的未签名版本可能会被 macOS Gatekeeper 拦截。请前往“系统设置 → 隐私与安全性”，选择“仍要打开”，然后确认启动应用。

### 菜单栏操作

- 左键点击：打开用量弹窗。
- 右键点击：打开应用菜单。
- 弹窗右上角「设置」：打开独立设置窗口。

### 供应商凭证与本地数据

凭证配置和秘密材料保存在当前 macOS 用户的应用本地存储中，不会写入应用包，也不会提交到仓库。设置页会按供应商分组管理多个独立凭证；每个凭证可以单独启用、停用、刷新、显示和复制。

菜单栏指标最多选择 4 个凭证。具有明确额度上限的供应商显示真实用量进度；只有余额接口的供应商（例如 DeepSeek）显示余额健康状态，不显示伪造的用量百分比。

升级过程中如果检测到上一轮测试版本遗留的 Keychain 项，应用只会尝试一次回迁到本地存储，成功后不再访问 Keychain。

小米 MiMo 用量查询使用用户手动提供的网页 Cookie 或 `api-platform_serviceToken`。该值作为秘密凭证保存在本地存储中，仅用于直接请求小米官方平台，不会写入诊断日志。

卸载应用不会自动删除本地配置。如需彻底清除数据，请先在设置中删除所有凭证，再卸载应用。

### 问题反馈与日志

更新检查、下载、安装和自动重启失败会记录到本地诊断日志。点击右键菜单或设置页中的“提交问题”，应用会生成包含版本、系统信息和最近脱敏日志的 GitHub Issue 草稿；提交前请在浏览器中再次确认正文不包含不应公开的内容。

日志文件位于：

    ~/Library/Logs/MyToken/app.log

## 本地开发

生成 Xcode 工程：

```bash
xcodegen generate
```

运行完整测试：

```bash
scripts/test.sh
```

直接使用 Xcode 命令行运行测试：

```bash
xcodegen generate
xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage
```

构建未签名 DMG：

```bash
scripts/build-dmg.sh
```

产物位于 `build/dist/MyToken.dmg`。构建脚本会先校验 Xcode 26+ 和 macOS 26+ SDK。

## CI 与发布

每次 push 和 Pull Request 都会运行 `.github/workflows/ci.yml`，使用 Xcode 26.3，执行完整测试并上传 DMG 构建产物。

`android/` 目录的变更会触发 `.github/workflows/android-ci.yml`：全模块 JVM
单元测试与 instrumented 测试编译验证；`connectedAndroidTest` 需要配置
self-hosted runner（带 KVM 或真机）。

发布版本时，在 GitHub Actions 中手动运行“发布版本”工作流，填写三段式版本号（例如 `1.2.0`）和 Markdown 发布说明。工作流会创建 `v1.2.0` 标签、生成单一 GitHub Release（Latest）并上传 `MyToken.dmg` 与 `MyToken-版本-架构.dmg`。

## 项目结构

```text
RoutinUsage/       macOS 应用源码
RoutinUsageTests/  macOS 单元测试
android/           Android 客户端（多模块，见 docs/android/README.md）
shared/            跨端共享 schema 与 fixture
scripts/           测试、DMG 构建和 Xcode 版本校验脚本
docs/              首次运行说明与 Android 文档
project.yml        XcodeGen 工程配置
```

## 许可证

当前仓库未声明开源许可证。未经项目维护者许可，请不要将代码或构建产物用于再发布。
