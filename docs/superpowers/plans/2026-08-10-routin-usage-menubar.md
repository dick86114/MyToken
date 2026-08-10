# Routin 用量菜单栏应用实施计划

> **供智能代理执行：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项实施。本计划使用复选框跟踪进度。

**目标：** 构建一款原生 macOS 菜单栏应用，通过固定 Routin API 安全监控多个 plan Key 的周期额度或 Token 资源包用量。

**架构：** 使用 SwiftUI `MenuBarExtra` 和独立设置场景构建界面，以 `UsageStore` 作为主线程单一状态源。Keychain、网络、映射、缓存、刷新调度和通知分别通过小型协议隔离，便于使用测试替身验证多 Key 部分失败、离线恢复和阈值去重。

**技术栈：** Swift 6、SwiftUI、Foundation、Security、UserNotifications、ServiceManagement、XCTest、XcodeGen、Xcode 26。

## 全局约束

- 最低目标系统为 macOS 14。
- API 固定为 `GET https://api.routin.ai/plan/v1/usage`，使用 `Authorization: Bearer <plan-key>`。
- 界面、代码注释、提交信息和交付文档全部使用中文。
- 接口 `daily*` 字段在界面中统一称为“5 小时”，不得显示为“日”或“每日”。
- 真实 Key 只能存入 macOS Keychain，不得写入偏好设置、缓存、日志、通知或测试快照。
- 菜单栏只显示当前 Key 的整数百分比；周期订阅支持 5 小时/周切换，资源包固定显示 Token 总额度。
- 自动刷新间隔仅允许 1、5、15、30 分钟，默认 5 分钟。
- 通知默认阈值为 80% 和 95%，低阈值必须小于高阈值。
- 应用默认无 Dock 图标，不引入第三方运行时依赖，不实现历史趋势、同步、自动更新或遥测。
- 交付未签名 `.app` 和 `.dmg`，不配置 App Store、公证或 Developer ID。

---

## 文件结构

```text
project.yml                                      # XcodeGen 工程定义
RoutinUsage/
  App/
    RoutinUsageApp.swift                         # 场景声明与依赖装配
    AppEnvironment.swift                         # 生产依赖容器
  Models/
    KeyConfiguration.swift                       # 非敏感 Key 元数据
    UsageDTO.swift                               # 接口传输模型
    UsageSnapshot.swift                          # 统一领域用量模型
    AppSettings.swift                            # 刷新、显示、通知偏好
  Security/
    KeychainStore.swift                          # Keychain 读写封装
  Persistence/
    KeyRepository.swift                          # Key 元数据持久化
    UsageCache.swift                             # 最后成功用量缓存
  Networking/
    UsageAPIClient.swift                         # 固定接口请求与错误映射
  Usage/
    UsageMapper.swift                            # DTO 到领域模型转换
    UsageFormatter.swift                         # 金额、Token、菜单栏文案格式化
    UsageStore.swift                             # 多 Key 状态协调
  Scheduling/
    RefreshScheduler.swift                       # 定时、改频率和唤醒补刷
  Notifications/
    AlertManager.swift                           # 阈值评估、去重和通知发送
  System/
    LoginItemManager.swift                       # 登录时启动
  Views/
    MenuBarLabelView.swift                       # 菜单栏百分比
    UsagePopoverView.swift                       # 紧凑总览面板
    UsageRowView.swift                           # 单 Key 进度行
    SettingsView.swift                           # 独立设置窗口
    KeyEditorView.swift                          # Key 新增/编辑表单
    OnboardingView.swift                         # 首次启动引导
RoutinUsageTests/                                # 与生产模块同名的 XCTest 文件
scripts/build-dmg.sh                             # 未签名 app 与 DMG 构建脚本
docs/首次运行说明.md                              # 私有分发安装说明
```

## 任务 1：建立可构建、可测试的菜单栏工程

**文件：**

- 创建：`project.yml`
- 创建：`RoutinUsage/App/RoutinUsageApp.swift`
- 创建：`RoutinUsageTests/ProjectBootstrapTests.swift`

**接口：**

- 产出：`RoutinUsage` macOS 应用目标、`RoutinUsageTests` 测试目标和共享 scheme。
- 产出：后续任务统一使用 `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test CODE_SIGNING_ALLOWED=NO`。

- [ ] **步骤 1：写入工程定义和失败的引导测试**

`project.yml` 明确以下核心配置：

```yaml
name: RoutinUsage
options:
  deploymentTarget:
    macOS: "14.0"
settings:
  base:
    SWIFT_VERSION: "6.0"
    PRODUCT_BUNDLE_IDENTIFIER: ai.routin.usage-monitor
    GENERATE_INFOPLIST_FILE: YES
    INFOPLIST_KEY_LSUIElement: YES
targets:
  RoutinUsage:
    type: application
    platform: macOS
    sources: [RoutinUsage]
    settings:
      base:
        PRODUCT_NAME: "Routin Usage"
  RoutinUsageTests:
    type: bundle.unit-test
    platform: macOS
    sources: [RoutinUsageTests]
    dependencies:
      - target: RoutinUsage
schemes:
  RoutinUsage:
    build:
      targets:
        RoutinUsage: all
        RoutinUsageTests: [test]
    test:
      targets: [RoutinUsageTests]
```

`ProjectBootstrapTests.swift` 先引用尚不存在的应用标识：

```swift
import XCTest
@testable import RoutinUsage

final class ProjectBootstrapTests: XCTestCase {
    func test应用标识稳定() {
        XCTAssertEqual(RoutinUsageApp.applicationName, "Routin Usage")
    }
}
```

- [ ] **步骤 2：生成工程并确认测试失败**

运行：

```bash
xcodegen generate
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `RoutinUsageApp` 或 `applicationName` 不存在。

- [ ] **步骤 3：实现最小菜单栏入口**

```swift
import SwiftUI

@main
struct RoutinUsageApp: App {
    static let applicationName = "Routin Usage"

    var body: some Scene {
        MenuBarExtra("…") {
            Text("尚未配置 Key")
        }
        Settings {
            Text("设置")
                .frame(width: 520, height: 420)
        }
    }
}
```

- [ ] **步骤 4：重新生成工程并确认通过**

运行任务 1 步骤 2 的两条命令。预期：`** TEST SUCCEEDED **`，应用构建产物为无 Dock 图标的 agent 应用。

- [ ] **步骤 5：提交工程骨架**

```bash
git add project.yml RoutinUsage RoutinUsageTests
git commit -m "构建: 初始化 macOS 菜单栏工程"
```

## 任务 2：实现订阅领域模型、接口模型和用量映射

**文件：**

- 创建：`RoutinUsage/Models/UsageDTO.swift`
- 创建：`RoutinUsage/Models/UsageSnapshot.swift`
- 创建：`RoutinUsage/Usage/UsageMapper.swift`
- 测试：`RoutinUsageTests/UsageMapperTests.swift`

**接口：**

- 产出：`UsageResponseDTO: Decodable & Sendable`，所有接口业务字段均为可选值。
- 产出：`UsageSnapshot: Codable & Equatable & Sendable`。
- 产出：`UsageDimension.fiveHour`、`.weekly`、`.token` 用于额度与通知；`DisplayDimension.fiveHour`、`.weekly` 用于菜单栏偏好。
- 产出：`UsageMapper.map(_ dto: UsageResponseDTO, fetchedAt: Date) throws -> UsageSnapshot`。
- 产出：`UsageMetric.percent: Double` 保留真实百分比，不在领域层截断到 100。

- [ ] **步骤 1：为周期订阅、资源包和边界值写失败测试**

测试必须直接构造 DTO，并断言：

```swift
func test周期订阅把Daily映射为五小时窗口() throws {
    let dto = UsageResponseDTO(
        planName: "Pro", type: 1,
        dailyLimitUsd: 10, weeklyLimitUsd: 50,
        dailyUsedUsd: 6.8, weeklyUsedUsd: 21,
        dailyRemainingUsd: 3.2, weeklyRemainingUsd: 29,
        dayWindowEndAt: "2026-08-10T14:00:00Z",
        weekWindowEndAt: "2026-08-15T00:00:00Z",
        totalTokens: nil, consumedTokens: nil, remainingTokens: nil,
        allowedModels: ["gpt-4.1"]
    )
    let result = try UsageMapper().map(dto, fetchedAt: Date(timeIntervalSince1970: 100))
    XCTAssertEqual(result.kind, .periodic)
    XCTAssertEqual(result.fiveHour?.percent, 68, accuracy: 0.001)
    XCTAssertEqual(result.weekly?.percent, 42, accuracy: 0.001)
}

func test资源包映射总Token使用率() throws {
    let dto = UsageResponseDTO(
        planName: "资源包", type: 2,
        dailyLimitUsd: nil, weeklyLimitUsd: nil,
        dailyUsedUsd: nil, weeklyUsedUsd: nil,
        dailyRemainingUsd: nil, weeklyRemainingUsd: nil,
        dayWindowEndAt: nil, weekWindowEndAt: nil,
        totalTokens: 10_000_000, consumedTokens: 9_200_000,
        remainingTokens: 800_000, allowedModels: []
    )
    let result = try UsageMapper().map(dto, fetchedAt: .distantPast)
    XCTAssertEqual(result.kind, .tokenPack)
    XCTAssertEqual(result.token?.percent, 92, accuracy: 0.001)
}
```

另写测试覆盖零额度抛出 `.invalidLimit`、120% 超额不截断、缺失 `remainingTokens` 时计算剩余量、未知 `type` 时按有效非零额度判断订阅类型。

- [ ] **步骤 2：运行映射测试并确认失败**

运行：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageMapperTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 DTO、快照和映射器类型不存在。

- [ ] **步骤 3：实现最小领域类型与映射规则**

领域模型使用以下固定形状：

```swift
enum UsageKind: String, Codable, Equatable, Sendable { case periodic, tokenPack }
enum UsageUnit: String, Codable, Equatable, Sendable { case usd, token }
enum UsageDimension: String, Codable, Equatable, Sendable { case fiveHour, weekly, token }
enum DisplayDimension: String, Codable, Equatable, Sendable { case fiveHour, weekly }

struct UsageMetric: Codable, Equatable, Sendable {
    let used: Decimal
    let limit: Decimal
    let remaining: Decimal
    let percent: Double
    let unit: UsageUnit
    let windowEnd: Date?
}

struct UsageSnapshot: Codable, Equatable, Sendable {
    let planName: String
    let kind: UsageKind
    let fiveHour: UsageMetric?
    let weekly: UsageMetric?
    let token: UsageMetric?
    let allowedModels: [String]
    let fetchedAt: Date
}
```

`UsageMapper` 使用 `NSDecimalNumber(decimal:)` 计算百分比。若 `totalTokens > 0` 且周期额度无效，判定为资源包；若周期额度有效且 `type == 1`，判定为周期订阅；两类字段同时有效且 `type != 1` 时判定为资源包。窗口时间使用 `ISO8601DateFormatter` 解析。

- [ ] **步骤 4：运行映射测试和全量测试**

先运行步骤 2 的命令，再运行全量测试命令。预期：全部显示 `** TEST SUCCEEDED **`。

- [ ] **步骤 5：提交领域模型**

```bash
git add RoutinUsage/Models RoutinUsage/Usage/UsageMapper.swift RoutinUsageTests/UsageMapperTests.swift
git commit -m "功能: 增加订阅用量模型与映射"
```

## 任务 3：实现 Keychain 和非敏感 Key 配置仓库

**文件：**

- 创建：`RoutinUsage/Models/KeyConfiguration.swift`
- 创建：`RoutinUsage/Security/KeychainStore.swift`
- 创建：`RoutinUsage/Persistence/KeyRepository.swift`
- 测试：`RoutinUsageTests/KeyRepositoryTests.swift`
- 测试：`RoutinUsageTests/KeychainStoreTests.swift`

**接口：**

- 产出：`KeyConfiguration(id:name:keySuffix:sortOrder:)`，模型不包含真实 Key。
- 产出：`KeychainStoring` 的 `save(_:for:)`、`read(for:)`、`delete(for:)`。
- 产出：`KeyRepository.list()`、`add(name:secret:)`、`update(id:name:secret:)`、`delete(id:)`、`move(fromOffsets:toOffset:)`。

- [ ] **步骤 1：写 Key 不落入偏好设置的失败测试**

使用独立 `UserDefaults(suiteName:)` 和内存 Keychain 测试替身，验证添加 `plan-secret-8F2A` 后：

```swift
let saved = try repository.add(name: "主账号", secret: "plan-secret-8F2A")
XCTAssertEqual(saved.name, "主账号")
XCTAssertEqual(saved.keySuffix, "8F2A")
XCTAssertFalse(String(data: defaults.data(forKey: "keyConfigurations")!, encoding: .utf8)!.contains("plan-secret"))
XCTAssertEqual(try keychain.read(for: saved.id), "plan-secret-8F2A")
```

另写测试验证名称去除首尾空白、空名称和不以 `plan-` 开头的 Key 被拒绝、排序持久化、删除同步清除 Keychain。

- [ ] **步骤 2：运行仓库测试并确认失败**

运行：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/KeyRepositoryTests -only-testing:RoutinUsageTests/KeychainStoreTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `KeyRepository` 和 `KeychainStoring` 不存在。

- [ ] **步骤 3：实现 Keychain 与仓库**

协议固定为：

```swift
protocol KeychainStoring: Sendable {
    func save(_ secret: String, for id: UUID) throws
    func read(for id: UUID) throws -> String?
    func delete(for id: UUID) throws
}
```

生产实现以 bundle identifier 作为 `kSecAttrService`，UUID 字符串作为 `kSecAttrAccount`；更新时先 `SecItemUpdate`，找不到再 `SecItemAdd`。`KeyRepository` 只将 `[KeyConfiguration]` 编码进 `UserDefaults`，Key 后四位通过 `String(secret.suffix(4))` 计算。

- [ ] **步骤 4：运行目标测试和全量测试**

预期：Key 仓库测试与全部既有测试通过，并确认失败输出中不包含测试密钥全文。

- [ ] **步骤 5：提交安全存储**

```bash
git add RoutinUsage/Models/KeyConfiguration.swift RoutinUsage/Security RoutinUsage/Persistence/KeyRepository.swift RoutinUsageTests/KeyRepositoryTests.swift RoutinUsageTests/KeychainStoreTests.swift
git commit -m "功能: 安全保存多 Key 配置"
```

## 任务 4：实现固定用量接口客户端

**文件：**

- 创建：`RoutinUsage/Networking/UsageAPIClient.swift`
- 测试：`RoutinUsageTests/UsageAPIClientTests.swift`
- 测试支持：`RoutinUsageTests/Support/URLProtocolStub.swift`

**接口：**

- 消费：`UsageMapper.map(_:fetchedAt:)`。
- 产出：`UsageFetching.fetchUsage(apiKey:now:) async throws -> UsageSnapshot?`。
- 产出：`UsageAPIError.invalidKey`、`.transport`、`.invalidResponse`、`.server(statusCode:)`。

- [ ] **步骤 1：写请求、空响应和错误映射失败测试**

使用注入 `URLSession` 的 `URLProtocolStub`，验证：

```swift
let result = try await client.fetchUsage(apiKey: "plan-test", now: fixedDate)
XCTAssertEqual(URLProtocolStub.lastRequest?.url?.absoluteString, "https://api.routin.ai/plan/v1/usage")
XCTAssertEqual(URLProtocolStub.lastRequest?.httpMethod, "GET")
XCTAssertEqual(URLProtocolStub.lastRequest?.value(forHTTPHeaderField: "Authorization"), "Bearer plan-test")
XCTAssertEqual(result?.planName, "Pro")
```

分别返回 JSON `null`、401 `{"error":"invalid_api_key"}`、500 和损坏 JSON，断言结果为 `nil`、`.invalidKey`、`.server(statusCode: 500)` 和 `.invalidResponse`。

- [ ] **步骤 2：运行 API 客户端测试并确认失败**

运行：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageAPIClientTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `UsageAPIClient` 不存在。

- [ ] **步骤 3：实现固定 URL 和状态映射**

```swift
protocol UsageFetching: Sendable {
    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot?
}

struct UsageAPIClient: UsageFetching {
    static let endpoint = URL(string: "https://api.routin.ai/plan/v1/usage")!
    let session: URLSession
    let mapper: UsageMapper
}
```

请求设置 15 秒超时和 `Accept: application/json`。先判断 HTTP 状态，再将精确字节序列 `null` 解读为无订阅；其余成功数据解码为 `UsageResponseDTO` 并映射。错误类型只保存状态码和通用类别，不保存响应正文。

- [ ] **步骤 4：运行客户端测试和全量测试**

预期：所有请求结构与错误映射断言通过。

- [ ] **步骤 5：提交接口客户端**

```bash
git add RoutinUsage/Networking RoutinUsageTests/UsageAPIClientTests.swift RoutinUsageTests/Support
git commit -m "功能: 接入固定订阅用量接口"
```

## 任务 5：实现最后成功数据缓存和过期判断

**文件：**

- 创建：`RoutinUsage/Persistence/UsageCache.swift`
- 测试：`RoutinUsageTests/UsageCacheTests.swift`

**接口：**

- 产出：`UsageCaching.load(for:)`、`save(_:for:)`、`delete(for:)`。
- 产出：`UsageFreshness.isStale(lastSuccess:now:refreshMinutes:) -> Bool`。

- [ ] **步骤 1：写缓存隔离和过期规则失败测试**

测试两个 UUID 的快照互不覆盖；持久化 JSON 不包含 `plan-`；删除只删除目标 UUID。过期规则断言：刷新为 1 分钟时 4 分 59 秒仍新鲜、5 分钟过期；刷新为 15 分钟时 29 分 59 秒仍新鲜、30 分钟过期。

- [ ] **步骤 2：运行缓存测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageCacheTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `UsageCaching` 和 `UsageFreshness` 不存在。

- [ ] **步骤 3：实现 Codable 缓存与过期函数**

```swift
protocol UsageCaching: Sendable {
    func load(for keyID: UUID) throws -> UsageSnapshot?
    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws
    func delete(for keyID: UUID) throws
}

enum UsageFreshness {
    static func isStale(lastSuccess: Date, now: Date, refreshMinutes: Int) -> Bool {
        let threshold = max(300, TimeInterval(refreshMinutes * 120))
        return now.timeIntervalSince(lastSuccess) >= threshold
    }
}
```

生产缓存使用专用 `UserDefaults` 键保存 `[UUID: UsageSnapshot]` 的编码结果，不持久化错误正文和请求材料。

- [ ] **步骤 4：运行缓存测试和全量测试**

预期：缓存往返、隔离、删除与过期边界全部通过。

- [ ] **步骤 5：提交缓存实现**

```bash
git add RoutinUsage/Persistence/UsageCache.swift RoutinUsageTests/UsageCacheTests.swift
git commit -m "功能: 缓存最后成功用量"
```

## 任务 6：实现通知阈值、窗口去重和系统发送适配器

**文件：**

- 创建：`RoutinUsage/Notifications/AlertManager.swift`
- 测试：`RoutinUsageTests/AlertManagerTests.swift`

**接口：**

- 产出：`AlertThresholds(low:high:)`，仅接受 1...100 且 low < high。
- 产出：`AlertEvaluator.evaluate(key:snapshot:thresholds:) -> [UsageAlert]`。
- 产出：`NotificationSending.requestAuthorization()` 与 `send(_:)`。

- [ ] **步骤 1：写阈值跨越和去重失败测试**

覆盖以下精确行为：79% 无通知；80% 产生低阈值通知；相同窗口再次 81% 不重复；从 79% 直接到 96% 只产生高阈值通知；新 `windowEnd` 恢复通知资格；资源包降到阈值以下后再次升高可重新通知；关闭通知时不调用发送器。

关键断言：

```swift
let alerts = evaluator.evaluate(key: key, snapshot: snapshot(percent: 96), thresholds: .init(low: 80, high: 95))
XCTAssertEqual(alerts.map(\.level), [.high])
XCTAssertEqual(alerts.first?.keyName, "主账号")
XCTAssertEqual(alerts.first?.dimension, .fiveHour)
```

- [ ] **步骤 2：运行通知测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/AlertManagerTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示通知领域类型不存在。

- [ ] **步骤 3：实现纯评估器和 UserNotifications 适配器**

用 `AlertWindowKey(keyID:dimension:windowIdentifier:threshold:)` 持久化已触发集合。周期窗口标识取 `windowEnd.timeIntervalSince1970`；资源包标识固定为 `token-pack`，当百分比回落到对应阈值以下时清除该阈值记录。

系统通知标题固定为“Routin 用量预警”，正文格式为“主账号 · 5 小时用量已达 96%，窗口将在 14:00 重置”。资源包正文不包含重置时间。

- [ ] **步骤 4：运行通知测试和全量测试**

预期：跨越、去重、重置和降幅恢复全部通过。

- [ ] **步骤 5：提交通知模块**

```bash
git add RoutinUsage/Notifications RoutinUsageTests/AlertManagerTests.swift
git commit -m "功能: 增加额度阈值通知"
```

## 任务 7：实现应用设置、刷新调度和登录时启动

**文件：**

- 创建：`RoutinUsage/Models/AppSettings.swift`
- 创建：`RoutinUsage/Scheduling/RefreshScheduler.swift`
- 创建：`RoutinUsage/System/LoginItemManager.swift`
- 测试：`RoutinUsageTests/AppSettingsTests.swift`
- 测试：`RoutinUsageTests/RefreshSchedulerTests.swift`

**接口：**

- 消费：任务 2 定义的 `DisplayDimension.fiveHour` 与 `.weekly`。
- 产出：`AppSettings.refreshMinutes`、`displayDimension`、`notificationsEnabled`、`thresholds`、`launchAtLogin`。
- 产出：`RefreshScheduler.start(minutes:onTick:)`、`reschedule(minutes:)`、`handleWake()`、`stop()`。
- 产出：`LoginItemManaging.setEnabled(_:) throws`。

- [ ] **步骤 1：写设置校验和调度替身失败测试**

断言默认值为 5 分钟、五小时维度、通知开启、80/95 阈值和登录启动关闭；刷新间隔只接受 `[1, 5, 15, 30]`。用 `TimerScheduling` spy 断言 `start(minutes: 5)` 安排 300 秒、改为 15 分钟会取消旧任务并安排 900 秒、连续两个 wake 事件只触发一次补刷。

- [ ] **步骤 2：运行设置和调度测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/AppSettingsTests -only-testing:RoutinUsageTests/RefreshSchedulerTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示设置和调度类型不存在。

- [ ] **步骤 3：实现设置、可注入定时器与系统适配器**

```swift
protocol TimerScheduling: Sendable {
    func schedule(every seconds: TimeInterval, action: @escaping @Sendable () -> Void) -> any CancellableTimer
}

protocol LoginItemManaging: Sendable {
    var isEnabled: Bool { get }
    func setEnabled(_ enabled: Bool) throws
}
```

生产定时器使用主运行循环 `Timer`；唤醒事件监听 `NSWorkspace.didWakeNotification`，以 2 秒去抖。登录启动使用 `SMAppService.mainApp.register()` 和 `unregister()`。设置持久化只保存枚举、整数、布尔值和阈值。

- [ ] **步骤 4：运行目标测试和全量测试**

预期：设置边界、重调度、取消和唤醒去抖测试全部通过。

- [ ] **步骤 5：提交系统行为模块**

```bash
git add RoutinUsage/Models/AppSettings.swift RoutinUsage/Scheduling RoutinUsage/System RoutinUsageTests/AppSettingsTests.swift RoutinUsageTests/RefreshSchedulerTests.swift
git commit -m "功能: 增加刷新调度与登录启动"
```

## 任务 8：实现多 Key UsageStore 状态协调

**文件：**

- 创建：`RoutinUsage/Usage/UsageStore.swift`
- 测试：`RoutinUsageTests/UsageStoreTests.swift`
- 测试支持：`RoutinUsageTests/Support/UsageFakes.swift`

**接口：**

- 消费：`KeyRepository`、`KeychainStoring`、`UsageFetching`、`UsageCaching`、`AlertEvaluator` 和 `NotificationSending`。
- 产出：`KeyUsageState`，包含配置、快照、最后成功时间、刷新状态和展示错误。
- 产出：`UsageStore.refreshAll()`、`refresh(keyID:)`、`selectKey(_:)`、`addValidatedKey(name:secret:)`、`deleteKey(_:)`。

- [ ] **步骤 1：写多 Key 部分成功和缓存恢复失败测试**

至少覆盖：

```swift
func test刷新全部时单个Key失败不阻塞其他Key() async {
    await store.refreshAll()
    XCTAssertEqual(store.state(for: successID)?.snapshot?.planName, "Pro")
    XCTAssertEqual(store.state(for: failureID)?.error, .network)
    XCTAssertFalse(store.isRefreshing)
}
```

另测首次加载恢复缓存、成功请求覆盖缓存、失败请求保留缓存并标记过期、`nil` 响应显示无订阅、401 显示 Key 无效、重复点击刷新不会为同一 Key 创建第二个请求、选择 Key 持久化、删除 Key 清除密钥/缓存/通知状态、验证失败不保存新 Key。

- [ ] **步骤 2：运行 UsageStore 测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageStoreTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `UsageStore` 不存在。

- [ ] **步骤 3：实现主线程状态源和结构化并发刷新**

```swift
@MainActor
@Observable
final class UsageStore {
    private(set) var states: [UUID: KeyUsageState] = [:]
    private(set) var orderedKeyIDs: [UUID] = []
    private(set) var selectedKeyID: UUID?
    private(set) var isRefreshing = false

    func refreshAll() async
    func refresh(keyID: UUID) async
    func selectKey(_ id: UUID)
    func addValidatedKey(name: String, secret: String) async throws
    func deleteKey(_ id: UUID) throws
}
```

刷新前在主线程读取 Key 元数据和密钥，随后用 `withTaskGroup` 并发请求。每个子任务返回 `(UUID, Result<UsageSnapshot?, UsageAPIError>)`，主线程逐项合并结果。通知只评估成功快照；缓存只在成功时覆盖。

- [ ] **步骤 4：运行 UsageStore 测试和全量测试**

预期：所有部分失败、缓存、选择、删除和验证场景通过；Thread Sanitizer 手工运行不报告状态数据竞争。

- [ ] **步骤 5：提交状态协调层**

```bash
git add RoutinUsage/Usage/UsageStore.swift RoutinUsageTests/UsageStoreTests.swift RoutinUsageTests/Support/UsageFakes.swift
git commit -m "功能: 协调多 Key 用量刷新状态"
```

## 任务 9：实现格式化、菜单栏标签和紧凑总览面板

**文件：**

- 创建：`RoutinUsage/Usage/UsageFormatter.swift`
- 创建：`RoutinUsage/Views/MenuBarLabelView.swift`
- 创建：`RoutinUsage/Views/UsageRowView.swift`
- 创建：`RoutinUsage/Views/UsagePopoverView.swift`
- 测试：`RoutinUsageTests/UsageFormatterTests.swift`

**接口：**

- 产出：`UsageFormatter.menuBarText(state:dimension:) -> String`。
- 产出：`UsageFormatter.amount(_ metric:)`、`remaining(_:)`、`resetTime(_:)`。
- 消费：`UsageStore` 和 `AppSettings`。

- [ ] **步骤 1：写菜单栏和数值文案失败测试**

断言 67.5% 显示 `68%`；首次加载显示 `…`；无订阅显示 `--`；无缓存错误显示 `!`；资源包在周维度仍取 Token 百分比；USD 显示 `$6.80 / $10.00`；Token 显示 `9.2M / 10M`；重置时间按当前时区显示 `14:00`。

- [ ] **步骤 2：运行格式化测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageFormatterTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `UsageFormatter` 不存在。

- [ ] **步骤 3：实现格式化器和 SwiftUI 面板**

`UsagePopoverView` 宽度固定 360 点，高度按内容增长、最大 520 点；Key 列表超过可用高度时滚动。顶部使用 `Picker` 的 segmented 样式切换五小时/周并提供刷新按钮。`UsageRowView` 使用 `ProgressView(value: min(max(percent, 0), 100), total: 100)`，按 80/95 阈值着色，当前 Key 使用实心圆标记；整行按钮调用 `store.selectKey(id)`。

底部显示最后刷新时间，并提供“设置”和“退出 Routin Usage”操作。为加载、空配置、无订阅、缓存过期和请求错误分别提供可访问性标签。菜单栏只渲染 `UsageFormatter.menuBarText`，悬停帮助包含当前 Key 名称和最后更新时间。

- [ ] **步骤 4：运行格式化测试、全量测试和构建**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test CODE_SIGNING_ALLOWED=NO
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug build CODE_SIGNING_ALLOWED=NO
```

预期：测试和构建成功；运行应用后弹窗能显示预览或测试注入的三类状态。

- [ ] **步骤 5：提交菜单界面**

```bash
git add RoutinUsage/Usage/UsageFormatter.swift RoutinUsage/Views/MenuBarLabelView.swift RoutinUsage/Views/UsageRowView.swift RoutinUsage/Views/UsagePopoverView.swift RoutinUsageTests/UsageFormatterTests.swift
git commit -m "功能: 实现菜单栏用量总览"
```

## 任务 10：实现设置窗口、Key 编辑和首次启动引导

**文件：**

- 创建：`RoutinUsage/Views/SettingsView.swift`
- 创建：`RoutinUsage/Views/KeyEditorView.swift`
- 创建：`RoutinUsage/Views/OnboardingView.swift`
- 测试：`RoutinUsageTests/KeyEditorValidationTests.swift`

**接口：**

- 消费：`UsageStore.addValidatedKey(name:secret:)` 和 Key 管理接口。
- 消费并修改：`AppSettings` 的显示、刷新、通知、阈值和登录启动属性。
- 产出：`KeyEditorValidation.validate(name:secret:) throws`。

- [ ] **步骤 1：写编辑表单校验失败测试**

断言空名称、空 Key、不以 `plan-` 开头、低阈值不小于高阈值时返回中文错误；合法名称会去除首尾空白，合法 Key 保持原值。断言网络验证失败后表单中的名称和 Key 不被清空。

- [ ] **步骤 2：运行表单校验测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/KeyEditorValidationTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `KeyEditorValidation` 不存在。

- [ ] **步骤 3：实现设置、编辑和引导界面**

设置窗口最小尺寸 520×420。Key 列表显示名称和 `plan-••••8F2A`，支持添加、编辑、删除确认和拖动排序。编辑表单保存时显示进度并禁用重复提交；401 显示“Key 无效”，无订阅的成功响应允许保存并提示“当前没有可用订阅”，网络错误保留输入并允许重试。

刷新间隔使用固定选项；维度只提供“五小时”和“周”；通知关闭时阈值控件禁用；登录启动切换失败时回滚开关并显示系统错误。没有 Key 时，应用启动后自动打开 `OnboardingView`；首个 Key 保存成功后关闭引导。

- [ ] **步骤 4：运行目标测试和全量构建**

预期：校验测试通过；设置窗口可从菜单面板打开，表单键盘导航和 VoiceOver 标签完整。

- [ ] **步骤 5：提交设置与引导**

```bash
git add RoutinUsage/Views/SettingsView.swift RoutinUsage/Views/KeyEditorView.swift RoutinUsage/Views/OnboardingView.swift RoutinUsageTests/KeyEditorValidationTests.swift
git commit -m "功能: 增加 Key 设置与首次引导"
```

## 任务 11：装配生产依赖与完整应用生命周期

**文件：**

- 创建：`RoutinUsage/App/AppEnvironment.swift`
- 修改：`RoutinUsage/App/RoutinUsageApp.swift`
- 测试：`RoutinUsageTests/AppLifecycleTests.swift`

**接口：**

- 产出：`AppEnvironment.live()` 创建唯一生产依赖图。
- 消费：此前任务的所有服务、状态和视图。

- [ ] **步骤 1：写启动、改设置和唤醒流程失败测试**

用依赖替身验证：启动立即调用一次 `refreshAll()`；定时器 tick 再次刷新；修改刷新间隔调用 `reschedule`；唤醒事件触发补刷；开启通知时只请求一次授权；没有 Key 时请求打开引导，已有 Key 时不打开。

- [ ] **步骤 2：运行生命周期测试并确认失败**

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/AppLifecycleTests CODE_SIGNING_ALLOWED=NO
```

预期：编译失败，提示 `AppEnvironment` 或生命周期协调器不存在。

- [ ] **步骤 3：实现依赖容器并替换临时入口**

`AppEnvironment.live()` 创建共享 `UserDefaults`、`KeychainStore`、`KeyRepository`、`UsageCache`、`UsageAPIClient`、`AlertEvaluator`、通知发送器、`RefreshScheduler`、`LoginItemManager` 和 `UsageStore`。`RoutinUsageApp` 的 `MenuBarExtra` label 使用 `MenuBarLabelView`，content 使用 `UsagePopoverView`，`Settings` 使用 `SettingsView`。

应用启动任务依次加载配置与缓存、立即刷新、启动调度器；终止时取消定时器。设置周期变化后菜单栏立即重新计算，不额外发请求；切换当前 Key 后立即使用已有数据，无数据时只刷新该 Key。

- [ ] **步骤 4：运行完整测试并手工走通主流程**

运行：

```bash
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' -derivedDataPath build/DerivedData test CODE_SIGNING_ALLOWED=NO
open "$PWD/build/DerivedData/Build/Products/Debug/Routin Usage.app"
```

手工验证添加两个测试 Key、切换当前 Key、周期切换、手动刷新、错误行隔离、设置窗口和退出入口。

- [ ] **步骤 5：提交应用集成**

```bash
git add RoutinUsage/App RoutinUsageTests/AppLifecycleTests.swift
git commit -m "功能: 完成菜单栏应用生命周期集成"
```

## 任务 12：实现未签名 DMG 交付、安装说明和最终验收

**文件：**

- 创建：`scripts/build-dmg.sh`
- 创建：`docs/首次运行说明.md`
- 修改：`.gitignore`

**接口：**

- 产出：`build/dist/Routin Usage.app`。
- 产出：`build/dist/Routin-Usage.dmg`。

- [ ] **步骤 1：先验证交付脚本尚不存在**

运行：

```bash
test -x scripts/build-dmg.sh
```

预期：退出码非零。

- [ ] **步骤 2：编写确定性的未签名构建脚本**

脚本使用 `set -euo pipefail`，将构建目录固定为仓库内 `build/DerivedData`，执行：

```bash
xcodegen generate
xcodebuild \
  -project RoutinUsage.xcodeproj \
  -scheme RoutinUsage \
  -configuration Release \
  -derivedDataPath build/DerivedData \
  CODE_SIGNING_ALLOWED=NO \
  clean build
ditto "build/DerivedData/Build/Products/Release/Routin Usage.app" "build/dist/Routin Usage.app"
hdiutil create -volname "Routin Usage" -srcfolder "build/dist/Routin Usage.app" -ov -format UDZO "build/dist/Routin-Usage.dmg"
```

脚本开始时只清理明确的 `build/dist` 和 `build/DerivedData` 路径，并在清理前校验当前目录包含 `project.yml`。将 `build/` 和 `RoutinUsage.xcodeproj/` 加入 `.gitignore`。
清理完成后先执行 `mkdir -p build/dist`，再运行 `ditto` 和 `hdiutil`，确保全新仓库中目标目录存在。

- [ ] **步骤 3：编写中文首次运行说明**

文档包含：拖入“应用程序”；首次被 Gatekeeper 阻止后进入“系统设置 → 隐私与安全性”；选择“仍要打开”；再次确认；Key 只保存在本机 Keychain；卸载应用不会自动删除 Keychain 项，需在应用内删除 Key 后再卸载。

- [ ] **步骤 4：执行全量验收**

运行：

```bash
xcodegen generate
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test CODE_SIGNING_ALLOWED=NO
bash scripts/build-dmg.sh
test -d "build/dist/Routin Usage.app"
test -f "build/dist/Routin-Usage.dmg"
hdiutil verify "build/dist/Routin-Usage.dmg"
git status --short
```

预期：测试成功；Release 构建成功；两个产物存在；DMG 校验返回 `verified successfully`；Git 状态只包含计划内未提交文件。

- [ ] **步骤 5：执行手工验收清单**

在当前 Mac 和另一台未配置开发证书的 Mac 上验证：首次允许流程、深浅色菜单栏、10 个 Key 滚动列表、周期与资源包混排、部分网络失败、离线缓存、系统休眠恢复、80%/95% 通知去重、登录时启动、删除 Key 后 Keychain 清理。

- [ ] **步骤 6：提交交付能力**

```bash
git add .gitignore scripts/build-dmg.sh docs/首次运行说明.md
git commit -m "构建: 增加私有 DMG 交付流程"
```

- [ ] **步骤 7：记录最终状态**

运行：

```bash
git status --short
git log --oneline --decorate -12
```

预期：工作区干净，历史包含本计划的十二个独立提交，最终 DMG 位于 `build/dist/Routin-Usage.dmg`。
