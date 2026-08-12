# Routin 每日签到 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 MyRoutin 中以 Routin 官方网页登录会话完成每日签到，同时不保存账号密码、验证码或 Cookie。

**Architecture:** 新增可替换的签到会话服务和 WebKit 窗口承载层。服务负责签到状态、重复操作互斥、登录恢复和网站数据清理；窗口层仅负责显示官方登录/签到页面并向服务报告经过白名单校验的导航和页面结果。设置页与菜单栏弹窗只调用 `AppEnvironment` 暴露的统一命令。

**Tech Stack:** Swift 5、SwiftUI、AppKit、WebKit、Foundation、XCTest、XcodeGen。

## Global Constraints

- 最低系统版本保持 macOS 14.0，不新增第三方依赖。
- 应用不新增 Routin 用户名、密码、验证码、Token 或 Cookie 的设置项。
- 不把网页登录凭据写入 `UserDefaults`、`LocalKeyStore`、用量缓存或诊断日志。
- 只加载 `https://routin.ai` 及经过实际登录流程验证后才允许的身份提供商域名；未知外链交给默认浏览器。
- 不猜测或硬编码未验证的私有签到 HTTP 接口、Cookie 名称或 CSRF 参数。
- 外部页面结构变化时安全失败：不伪造成功，保留用户打开官方页面确认的路径。
- 项目注释、测试名、用户文案和文档使用中文；使用 `pnpm` 而非 `npm`。

---

## 文件结构

- `RoutinUsage/CheckIn/RoutinCheckInState.swift`：签到状态、可展示文案和结果类型。
- `RoutinUsage/CheckIn/RoutinCheckInService.swift`：可注入的签到会话协议、状态机、重复请求互斥和网站数据清除命令。
- `RoutinUsage/CheckIn/RoutinWebSession.swift`：WebKit 会话配置、允许导航规则、登录识别、签到页面结果提取和受限 JavaScript 调用。
- `RoutinUsage/CheckIn/RoutinCheckInWindow.swift`：SwiftUI/AppKit 桥接的 WebView 窗口，只显示服务当前请求的官方页面。
- `RoutinUsage/App/AppEnvironment.swift`：持有签到服务并提供 `startRoutinCheckIn()`、`beginRoutinLogin()`、`signOutRoutin()`。
- `RoutinUsage/App/RoutinUsageApp.swift`：注册独立的签到网页登录窗口，并把服务回调传入设置页。
- `RoutinUsage/App/StatusBarController.swift`：将签到服务状态传给弹窗，并订阅状态变化。
- `RoutinUsage/Views/UsagePopoverView.swift`：工具栏增加无文字图标式签到按钮与状态提示。
- `RoutinUsage/Views/SettingsView.swift`：通知与系统页增加 Routin 签到设置区域，不出现账号密码输入。
- `RoutinUsageTests/RoutinCheckInStateTests.swift`：状态文字、终态和重复操作规则。
- `RoutinUsageTests/RoutinCheckInServiceTests.swift`：服务状态转换、登录后续签、登出数据清理和错误分支。
- `RoutinUsageTests/RoutinWebSessionTests.swift`：导航域名白名单、登录重定向检测和页面结果解析。
- `RoutinUsageTests/ProjectBootstrapTests.swift`：菜单栏、设置页、应用环境的静态接入契约。
- `README.md`、`docs/首次运行说明.md`：说明网页登录会话范围、重新登录与登出行为。

---

### Task 1: 签到状态模型与服务状态机

**Files:**
- Create: `RoutinUsage/CheckIn/RoutinCheckInState.swift`
- Create: `RoutinUsage/CheckIn/RoutinCheckInService.swift`
- Test: `RoutinUsageTests/RoutinCheckInStateTests.swift`
- Test: `RoutinUsageTests/RoutinCheckInServiceTests.swift`

**Interfaces:**
- `enum RoutinCheckInState: Equatable, Sendable { case idle; case needsLogin; case loggingIn; case checkingIn; case succeeded; case alreadyCheckedIn; case failed(RoutinCheckInFailure) }`
- `enum RoutinCheckInFailure: Equatable, Sendable { case network; case pageChanged; case cancelled }`
- `enum RoutinCheckInAction: Sendable { case login; case checkIn }`
- `protocol RoutinWebSessionManaging: Sendable { func hasAuthenticatedSession() async -> Bool; func performCheckIn() async throws -> RoutinCheckInOutcome; func clearRoutinWebsiteData() async }`
- `enum RoutinCheckInOutcome: Equatable, Sendable { case succeeded; case alreadyCheckedIn; case needsLogin; case cannotConfirm }`
- `@MainActor @Observable final class RoutinCheckInService { var state: RoutinCheckInState; func startCheckIn() async; func beginLogin() async; func didFinishLogin() async; func signOut() async }`

- [ ] **Step 1: 编写失败测试**，在 `RoutinCheckInStateTests.swift` 断言每种状态的用户文案和终态规则；在 `RoutinCheckInServiceTests.swift` 使用 `RoutinWebSessionManaging` 假实现覆盖以下流程：

```swift
func test未登录签到会进入登录状态() async {
    let session = RoutinWebSessionFake(isAuthenticated: false)
    let service = await MainActor.run { RoutinCheckInService(session: session) }

    await service.startCheckIn()

    await MainActor.run {
        XCTAssertEqual(service.state, .needsLogin)
    }
}

func test登录成功后会续接原签到请求() async {
    let session = RoutinWebSessionFake(isAuthenticated: false, outcome: .succeeded)
    let service = await MainActor.run { RoutinCheckInService(session: session) }

    await service.startCheckIn()
    await session.setAuthenticated(true)
    await service.didFinishLogin()

    await MainActor.run {
        XCTAssertEqual(service.state, .succeeded)
    }
}
```

- [ ] **Step 2: 运行定向测试确认失败**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/RoutinCheckInStateTests -only-testing:RoutinUsageTests/RoutinCheckInServiceTests test
```

预期：因签到状态类型和服务尚不存在而编译失败。

- [ ] **Step 3: 实现最小状态模型与服务**。在 `RoutinCheckInState` 添加中文展示文案、`isBusy` 和 `isTerminalResult`；在 `RoutinCheckInService` 保存 `pendingAction`，用单个 `Task` 或 `isBusy` 防止重复签到，在 `startCheckIn()` 中依次处理 `needsLogin`、`succeeded`、`alreadyCheckedIn`、`cannotConfirm` 和网络异常。实现登出时先取消未完成任务，再调用 `clearRoutinWebsiteData()` 并将状态设为 `.idle`。

```swift
func startCheckIn() async {
    guard !state.isBusy else { return }
    pendingAction = .checkIn
    guard await session.hasAuthenticatedSession() else {
        state = .needsLogin
        return
    }
    await performPendingAction()
}
```

- [ ] **Step 4: 补齐失败分支测试**。分别验证重复点击只调用一次 `performCheckIn()`、`alreadyCheckedIn` 映射正确、`cannotConfirm` 映射 `.failed(.pageChanged)`、传输错误映射 `.failed(.network)`，以及登出只调用站点数据清理而不接触 KeyRepository。

- [ ] **Step 5: 运行定向测试确认通过**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/RoutinCheckInStateTests -only-testing:RoutinUsageTests/RoutinCheckInServiceTests test
```

- [ ] **Step 6: 提交状态机**：

```bash
git add RoutinUsage/CheckIn/RoutinCheckInState.swift RoutinUsage/CheckIn/RoutinCheckInService.swift RoutinUsageTests/RoutinCheckInStateTests.swift RoutinUsageTests/RoutinCheckInServiceTests.swift
git commit -m "feat: 增加 Routin 签到状态机"
```

### Task 2: WebKit 会话、导航限制与页面结果识别

**Files:**
- Create: `RoutinUsage/CheckIn/RoutinWebSession.swift`
- Test: `RoutinUsageTests/RoutinWebSessionTests.swift`

**Interfaces:**
- `@MainActor final class RoutinWebSession: NSObject, RoutinWebSessionManaging`
- `static let lotteryURL = URL(string: "https://routin.ai/dashboard/lottery")!`
- `static func navigationDecision(for url: URL, approvedIdentityHosts: Set<String>) -> RoutinNavigationDecision`
- `enum RoutinNavigationDecision: Equatable { case allowInWebView; case openExternally; case cancel }`
- `static func pageOutcome(from title: String?, bodyText: String) -> RoutinCheckInOutcome?`
- `func loadLoginPage() async`
- `func performCheckIn() async throws -> RoutinCheckInOutcome`

- [ ] **Step 1: 在开发者工具中验证外部契约并记录证据**。手工访问 `https://routin.ai/dashboard/lottery`，使用一个授权测试账号完成登录，记录：最终登录页 URL、所有顶级重定向域名、签到按钮的稳定文本或可访问性标识、已签到和成功的可见文案。将域名和可识别结果写入测试常量；不得记录账号、Cookie、请求头或响应体。

- [ ] **Step 2: 编写失败测试**，覆盖导航白名单和结果解析：

```swift
func test官方页面和已验证身份域名允许在WebView内打开() {
    XCTAssertEqual(
        RoutinWebSession.navigationDecision(
            for: URL(string: "https://routin.ai/dashboard/lottery")!,
            approvedIdentityHosts: ["accounts.example-verified.test"]
        ),
        .allowInWebView
    )
}

func test未知外部链接交给默认浏览器() {
    XCTAssertEqual(
        RoutinWebSession.navigationDecision(
            for: URL(string: "https://example.invalid/help")!,
            approvedIdentityHosts: []
        ),
        .openExternally
    )
}

func test已签到页面解析为alreadyCheckedIn() {
    XCTAssertEqual(
        RoutinWebSession.pageOutcome(from: "每日签到", bodyText: "今日已签到，明天再来"),
        .alreadyCheckedIn
    )
}
```

- [ ] **Step 3: 运行定向测试确认失败**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/RoutinWebSessionTests test
```

预期：`RoutinWebSession` 尚不存在而编译失败。

- [ ] **Step 4: 实现受限 WebKit 会话**。使用非持久化以外的默认 `WKWebsiteDataStore.default()` 保存系统管理的站点会话；将 JavaScript、弹窗和导航委托收束在该类；仅对 `https` 的 `routin.ai`、子域名和步骤 1 已验证的身份域名返回 `.allowInWebView`。将未知 HTTP(S) URL 交给 `NSWorkspace.shared.open`，将非 HTTP(S) URL 取消。页面识别采用步骤 1 得到的稳定文本，解析不出结果时返回 `.cannotConfirm`。

```swift
static func navigationDecision(
    for url: URL,
    approvedIdentityHosts: Set<String>
) -> RoutinNavigationDecision {
    guard url.scheme?.lowercased() == "https", let host = url.host?.lowercased() else {
        return .cancel
    }
    if host == "routin.ai" || host.hasSuffix(".routin.ai") || approvedIdentityHosts.contains(host) {
        return .allowInWebView
    }
    return .openExternally
}
```

- [ ] **Step 5: 实现签页面驱动流程**。页面加载后，先读取标题和 `document.body.innerText`；仅当步骤 1 证实存在稳定的签到控件时，用固定的 CSS 选择器或可访问性标签定位并点击一次。点击后重新读取页面文本并映射结果。任何 JavaScript 异常、超时或未匹配结果都返回 `.cannotConfirm`，不直接调用私有接口。

- [ ] **Step 6: 补齐定向测试并确认通过**。验证 `http`、`file`、自定义 scheme 都被取消；`routin.ai.evil.example` 不被视为官方域名；成功、已签到和未知页面分别映射 `.succeeded`、`.alreadyCheckedIn`、`nil`。

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/RoutinWebSessionTests test
```

- [ ] **Step 7: 提交 WebKit 会话层**：

```bash
git add RoutinUsage/CheckIn/RoutinWebSession.swift RoutinUsageTests/RoutinWebSessionTests.swift
git commit -m "feat: 增加受限 Routin 网页签到会话"
```

### Task 3: 网页窗口与应用环境接入

**Files:**
- Create: `RoutinUsage/CheckIn/RoutinCheckInWindow.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsage/App/StatusBarController.swift`
- Modify: `RoutinUsageTests/AppLifecycleTests.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `struct RoutinCheckInWindow: View { @Bindable var service: RoutinCheckInService; let session: RoutinWebSession }`
- `AppEnvironment` 新增 `let routinCheckIn: RoutinCheckInService` 与 `func startRoutinCheckIn() async`、`func beginRoutinLogin() async`、`func signOutRoutin() async`。
- `RoutinUsageApp` 新增 `Window("Routin 签到", id: "routin-check-in")`。
- `UsagePopoverView` 新增 `checkInState`、`startCheckIn` 和 `openCheckInWindow` 参数。

- [ ] **Step 1: 编写失败静态接入测试**。在 `ProjectBootstrapTests` 断言：

```swift
XCTAssertTrue(app.contains("Window(\"Routin 签到\", id: \"routin-check-in\")"))
XCTAssertTrue(appEnvironment.contains("func startRoutinCheckIn() async"))
XCTAssertTrue(statusBarController.contains("environment.routinCheckIn"))
XCTAssertTrue(popover.contains("startCheckIn"))
```

在 `AppLifecycleTests` 使用可注入的会话假实现，断言 `AppEnvironment.startRoutinCheckIn()` 将服务置为 `.needsLogin` 或 `.checkingIn`，但不改变 `store.orderedKeyIDs` 与 KeyRepository 内容。

- [ ] **Step 2: 运行相关测试确认失败**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/AppLifecycleTests -only-testing:RoutinUsageTests/ProjectBootstrapTests test
```

- [ ] **Step 3: 接入 `AppEnvironment`**。在 `live()` 中创建 `RoutinWebSession` 与 `RoutinCheckInService`，生产环境通过同一实例驱动窗口。初始化器接受可选 `RoutinCheckInService`，测试默认使用无副作用假实现。`startRoutinCheckIn()` 只调用签到服务；当其状态为 `.needsLogin` 时通过 `NotificationCenter` 请求打开 `routin-check-in` 窗口，绝不读取 `KeyRepository`、`LocalKeyStore` 或日志内容。

- [ ] **Step 4: 实现 `RoutinCheckInWindow`**。用 `NSViewRepresentable` 装配 `WKWebView`，窗口打开时根据服务状态加载登录页或签到页；用户关闭窗口而服务仍在 `.loggingIn` 时将状态变为 `.failed(.cancelled)`。窗口尺寸设置为最小宽 520、高 640，支持登录页正常滚动，不添加账号密码表单。

- [ ] **Step 5: 注册窗口和状态观察**。在 `RoutinUsageApp` 注册独立窗口并在 `StatusPopoverContent` 监听打开签到窗口通知；在 `StatusBarController.observeEnvironment()` 读取 `environment.routinCheckIn.state`，确保 UI 状态变化刷新菜单栏内容。

- [ ] **Step 6: 运行相关测试确认通过**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/AppLifecycleTests -only-testing:RoutinUsageTests/ProjectBootstrapTests test
```

- [ ] **Step 7: 提交应用接入**：

```bash
git add RoutinUsage/CheckIn/RoutinCheckInWindow.swift RoutinUsage/App/AppEnvironment.swift RoutinUsage/App/RoutinUsageApp.swift RoutinUsage/App/StatusBarController.swift RoutinUsageTests/AppLifecycleTests.swift RoutinUsageTests/ProjectBootstrapTests.swift
git commit -m "feat: 接入 Routin 签到网页登录窗口"
```

### Task 4: 弹窗和设置页操作界面

**Files:**
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `UsagePopoverView` 新增 `typealias StartRoutinCheckIn = @MainActor () async -> Void`、`let checkInState: RoutinCheckInState`、`let startRoutinCheckIn: StartRoutinCheckIn`。
- `SettingsView` 新增 `typealias StartRoutinCheckIn = @MainActor () async -> Void`、`typealias BeginRoutinLogin = @MainActor () async -> Void`、`typealias SignOutRoutin = @MainActor () async -> Void`、`let routinCheckInState: RoutinCheckInState`。

- [ ] **Step 1: 编写失败静态接入测试**，断言弹窗工具栏使用签到图标按钮而不是普通文本矩形按钮，并且设置页包含“Routin 签到”“登录 Routin”“重新登录”“退出登录”及状态文案，但不包含 `SecureField("Routin`、`TextField("账号`、`TextField("密码`、`UserDefaults` 或 Cookie 输入。

- [ ] **Step 2: 运行静态接入测试确认失败**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/ProjectBootstrapTests test
```

- [ ] **Step 3: 实现菜单栏弹窗签到入口**。在 `UsagePopoverView.toolbar` 的刷新按钮旁加入 `checkmark.circle` 图标按钮，点击后调用 `Task { await startRoutinCheckIn() }`；`checkInState.isBusy` 时显示固定尺寸 `ProgressView` 并禁用按钮。使用 `.help("Routin 签到")` 和完整 accessibility label。工具栏下方或 footer 以紧凑文字展示结果/失败状态，失败时提供“打开签到页面”图标操作而不声称已签到。

- [ ] **Step 4: 实现设置页签到区域**。在“通知与系统”页增加 `Section("Routin 签到")`：

```swift
LabeledContent("登录状态") {
    Text(routinCheckInState.statusText)
}
Button("立即签到") { Task { await startRoutinCheckIn() } }
Button(routinCheckInState.requiresLogin ? "登录 Routin" : "重新登录") {
    Task { await beginRoutinLogin() }
}
Button("退出登录", role: .destructive) {
    Task { await signOutRoutin() }
}
.disabled(routinCheckInState.isBusy || routinCheckInState.requiresLogin)
```

根据状态隐藏不适用的命令：未登录时不显示退出登录，登录中和签到中禁用命令，成功和已签到显示最终状态。所有控件复用 `.liquidGlassButton()`，保持现有设置页宽度与表单风格。

- [ ] **Step 5: 在应用场景完整传参**。从 `RoutinUsageApp` 向 `SettingsView` 注入签到状态与三个 `AppEnvironment` 方法，从 `StatusPopoverContent` 向 `UsagePopoverView` 注入同一服务的状态与回调；不要创建第二个服务实例。

- [ ] **Step 6: 运行静态接入测试确认通过**：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/ProjectBootstrapTests test
```

- [ ] **Step 7: 提交界面**：

```bash
git add RoutinUsage/Views/UsagePopoverView.swift RoutinUsage/Views/SettingsView.swift RoutinUsage/App/RoutinUsageApp.swift RoutinUsageTests/ProjectBootstrapTests.swift
git commit -m "feat: 增加 Routin 签到界面入口"
```

### Task 5: 文档、全量验证与人工网页登录验证

**Files:**
- Modify: `README.md`
- Modify: `docs/首次运行说明.md`

- [ ] **Step 1: 更新用户文档**。在 README 功能清单和“Key 与本地数据”部分明确：签到使用应用内 Routin 官方网页登录会话；应用不保存账号密码、验证码或 Cookie；可在设置中重新登录或退出登录，退出仅清除 Routin 网页站点数据且不影响 plan Key。首次运行说明增加登录窗口、会话失效和无法确认签到结果时打开官方页面检查的说明。

- [ ] **Step 2: 生成工程并运行全量单元测试**：

```bash
xcodegen generate
scripts/test.sh
```

预期：所有现有测试和新增签到测试通过。

- [ ] **Step 3: 运行静态质量检查**：

```bash
git diff --check
rg -n "Routin.*(密码|Cookie)|SecureField\(\"Routin|TextField\(\"账号" RoutinUsage README.md docs || true
```

预期：没有将凭据输入或 Cookie 存储引入源码；只有文档中的“不保存”说明可匹配敏感词。

- [ ] **Step 4: 使用授权测试账号执行人工验证**。从未登录状态点击菜单栏签到，确认官方登录页在 App 内打开；完成登录后确认本次签到自动恢复；再次点击确认复用会话；验证“今天已签到”显示；在设置页退出登录后确认签到再次要求登录。记录执行日期、可见结果和未完成项；不记录账号、Cookie、令牌或网络请求内容。

- [ ] **Step 5: 构建 DMG**：

```bash
scripts/build-dmg.sh
```

预期：生成 `build/dist/MyRoutin.dmg`；若本机缺少 Xcode 26 或 SDK，保留构建日志并在结果中明确说明阻塞原因。

- [ ] **Step 6: 提交文档与验证结果**：

```bash
git add README.md docs/首次运行说明.md
git commit -m "docs: 补充 Routin 签到使用说明"
```

## 自检记录

- 规格中的会话安全、状态反馈、双入口、登出范围、导航限制、外部页面变更安全失败和测试要求，分别由 Task 1 至 Task 5 覆盖。
- 外部站点的实际身份域名、稳定按钮标识和成功文本在 Task 2 的实施前验证；计划不把它们伪装为已知事实。
- 所有在后续任务使用的类型和方法均在“接口”段首次定义；没有未决占位内容或含糊的测试指令。
