# MyRoutin

MyRoutin 是一个 macOS 菜单栏用量监控工具，用于查看 Routin plan 订阅的用量、剩余额度和重置时间。

## 功能

- 菜单栏显示当前 Key 的用量摘要和风险颜色。
- 左键打开用量弹窗，查看多个 Key 的详细用量、剩余时间和分组倍率。
- 右键菜单支持切换账号、打开设置、检查更新和退出应用。
- 支持从右键菜单或设置页提交问题，自动生成脱敏日志并打开 GitHub Issue 页面。
- 支持 5 小时和周用量维度，以及别名、别名加竖向进度条等显示样式。
- 支持 50% 和 80% 用量阈值通知，并可自定义阈值。
- 支持按 1、5、15 或 30 分钟自动刷新，以及登录时自动启动。
- 从 GitHub Release 检查并安装应用更新。
- 设置窗口支持缩放，界面使用 macOS 液态玻璃风格。

## 系统要求

- macOS 14.0 或更高版本。
- 本地构建需要 Xcode 26、macOS 26 SDK 和 XcodeGen。

```bash
brew install xcodegen
```

## 安装使用

1. 下载 `MyRoutin.dmg`，或从 GitHub Release 下载正式版本。
2. 打开 DMG，将 `MyRoutin` 拖入“应用程序”文件夹。
3. 启动应用，在设置中添加有效的 `plan-` Key。
4. 点击菜单栏中的 MyRoutin 图标查看用量。

首次运行的未签名版本可能会被 macOS Gatekeeper 拦截。请前往“系统设置 → 隐私与安全性”，选择“仍要打开”，然后确认启动应用。

### 菜单栏操作

- 左键点击：打开用量弹窗。
- 右键点击：打开应用菜单。
- 点击用量行：切换当前菜单栏使用的 Key。
- 弹窗中的“设置”：打开独立设置窗口。

### Key 与本地数据

Key 保存在当前 macOS 用户的本地应用偏好设置中，以明文形式保存，不会写入应用包，也不会提交到仓库。界面默认只显示安全后缀；请求用量时，Key 会通过 HTTPS `Bearer` 请求发送到 Routin 用量接口。

卸载应用不会自动删除本地配置。如需彻底清除数据，请先在设置中删除所有 Key，再卸载应用。

### 问题反馈与日志

更新检查、下载、安装和自动重启失败会记录到本地诊断日志。点击右键菜单或设置页中的“提交问题”，应用会生成包含版本、系统信息和最近脱敏日志的 GitHub Issue 草稿；提交前请在浏览器中再次确认正文不包含不应公开的内容。

日志文件位于：

    ~/Library/Logs/MyRoutin/app.log

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

产物位于 `build/dist/MyRoutin.dmg`。构建脚本会先校验 Xcode 26 和 macOS 26 SDK。

## CI 与发布

每次 push 和 Pull Request 都会运行 `.github/workflows/ci.yml`，使用 Xcode 26.3，执行完整测试并上传 DMG 构建产物。

发布版本时，在 GitHub Actions 中手动运行“发布版本”工作流，填写三段式版本号（例如 `1.2.0`）和 Markdown 发布说明。工作流会创建 `v1.2.0` 标签、生成 GitHub Release 并上传 DMG。

## 项目结构

```text
RoutinUsage/       应用源码
RoutinUsageTests/  单元测试
scripts/           测试、DMG 构建和 Xcode 版本校验脚本
docs/              首次运行说明
project.yml        XcodeGen 工程配置
```

## 许可证

当前仓库未声明开源许可证。未经项目维护者许可，请不要将代码或构建产物用于再发布。
