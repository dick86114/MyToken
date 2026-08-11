# 更新日志与问题提交 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为更新失败提供脱敏本地日志，并从菜单栏和设置页打开带日志的 GitHub 新 Issue 页面。

**Architecture:** 新增可注入的 `AppLogWriting` 日志接口；`AppLogStore` 负责文件写入、轮转和脱敏，`IssueReporter` 负责环境信息、日志截断和 Issue URL 编码。更新服务与应用环境通过接口记录关键阶段，界面入口只调用应用环境的统一提交方法。

**Tech Stack:** Swift 5、AppKit、SwiftUI、Foundation `URLComponents`/`XMLParser`、XCTest。

## Global Constraints

- 不内置 GitHub Token，不直接调用创建 Issue API。
- 不读取或上传 KeyRepository 中保存的秘密值。
- Issue 正文只包含有限长度的最近脱敏日志。
- 保持 macOS 14.0 最低版本和 arm64/x86_64 Universal 架构。
- 前端和生成文档使用中文，不新增第三方依赖。

---

### Task 1: 本地日志与 Issue URL 组件

**Files:**
- Create: `RoutinUsage/Diagnostics/AppLogStore.swift`
- Create: `RoutinUsage/Diagnostics/IssueReporter.swift`
- Test: `RoutinUsageTests/AppLogStoreTests.swift`
- Test: `RoutinUsageTests/IssueReporterTests.swift`

**Interfaces:**
- `protocol AppLogWriting: Sendable { func log(level:event:details:) async }`
- `actor AppLogStore: AppLogWriting { init(fileURL:maxBytes:); func recentText(maxCharacters:) async -> String }`
- `struct NoopAppLogWriter: AppLogWriting`
- `struct IssueReportContext: Sendable`
- `enum IssueReporter { static func makeIssueURL(context:) -> URL? }`

- [ ] **Step 1: 编写失败测试**，覆盖日志脱敏、大小轮转、最近日志读取和 Issue URL 的标题、正文、环境信息及长度限制。
- [ ] **Step 2: 运行定向测试确认失败**：
  `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/AppLogStoreTests -only-testing:RoutinUsageTests/IssueReporterTests test`
- [ ] **Step 3: 实现 `AppLogStore`**：使用 JSONL 或可读文本追加日志；写入前替换 `plan-` 凭据、敏感查询参数和超长详情；超过 128 KiB 时只保留末尾内容；文件失败时通过 `Logger` 记录但不抛出业务错误。
- [ ] **Step 4: 实现 `IssueReporter`**：使用 `URLComponents` 构造 `https://github.com/dick86114/MyRoutin/issues/new`，正文包含版本、系统版本、架构、更新状态和最多 6000 字符日志。
- [ ] **Step 5: 运行定向测试确认通过**。
- [ ] **Step 6: 提交组件**：`git add RoutinUsage/Diagnostics RoutinUsageTests/AppLogStoreTests.swift RoutinUsageTests/IssueReporterTests.swift && git commit -m "feat: 增加脱敏日志与 Issue 报告组件"`。

### Task 2: 接入更新链路日志

**Files:**
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/Updates/GitHubUpdateService.swift`
- Modify: `RoutinUsageTests/AppLifecycleTests.swift`
- Modify: `RoutinUsageTests/GitHubUpdateServiceTests.swift`

**Interfaces:**
- `AppEnvironment` 注入 `any AppLogWriting`，生产环境使用 `AppLogStore.shared`，测试默认使用 `NoopAppLogWriter`。
- `GitHubUpdateService` 接收同一日志接口，记录请求阶段和 HTTP 状态。
- `UpdateInstaller.install` 接收可选日志接口，记录自动启动成功或失败。

- [ ] **Step 1: 编写失败测试**，验证检查失败、API 限流回退、下载失败、安装失败和重启失败会写入事件。
- [ ] **Step 2: 运行相关测试确认失败**。
- [ ] **Step 3: 接入日志**：保留现有用户可见的通用错误文案，同时记录错误类型、HTTP 状态码、失败阶段和重试信息；不写入响应正文、请求正文或 Key。
- [ ] **Step 4: 运行相关测试确认通过**。
- [ ] **Step 5: 提交更新链路**：`git add RoutinUsage/App/AppEnvironment.swift RoutinUsage/Updates/GitHubUpdateService.swift RoutinUsageTests/AppLifecycleTests.swift RoutinUsageTests/GitHubUpdateServiceTests.swift && git commit -m "feat: 记录更新失败诊断日志"`。

### Task 3: 菜单栏与设置页入口

**Files:**
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsage/App/StatusBarController.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `AppEnvironment.openIssueReport() async` 读取日志、生成 Issue URL 并调用 `NSWorkspace.shared.open`。
- `SettingsView` 新增 `SubmitIssueReport` 回调。

- [ ] **Step 1: 编写失败静态接入测试**，要求右键菜单和设置页均存在“提交问题”，且应用场景传入统一回调。
- [ ] **Step 2: 运行静态测试确认失败**。
- [ ] **Step 3: 实现入口**：右键菜单增加“提交问题”；设置页软件更新区域增加按钮；打开失败时显示中文提示，不影响菜单栏常驻。
- [ ] **Step 4: 运行静态测试确认通过**。
- [ ] **Step 5: 提交界面接入**：`git add RoutinUsage/App/RoutinUsageApp.swift RoutinUsage/App/StatusBarController.swift RoutinUsage/Views/SettingsView.swift RoutinUsageTests/ProjectBootstrapTests.swift && git commit -m "feat: 增加问题提交入口"`。

### Task 4: 全量验证与发布说明

**Files:**
- Modify: `README.md`
- Modify: `docs/首次运行说明.md`

- [ ] **Step 1: 补充用户说明**：解释日志只包含脱敏诊断信息，提交 Issue 前用户仍需确认内容。
- [ ] **Step 2: 运行全量测试**：`scripts/test.sh`，预期全部通过。
- [ ] **Step 3: 构建 DMG**：`scripts/build-dmg.sh`，预期生成 `build/dist/MyRoutin.dmg`。
- [ ] **Step 4: 检查 DMG**：确认版本、最低系统版本、Universal 架构、日志入口静态接入和 `git diff --check`。
- [ ] **Step 5: 提交文档与验证结果**：`git add README.md docs/首次运行说明.md && git commit -m "docs: 补充问题反馈说明"`。
