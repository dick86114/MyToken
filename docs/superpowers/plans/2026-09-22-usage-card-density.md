# 凭证卡片简洁 / 完整模式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给 macOS 弹层凭证卡加上全局简洁 / 完整模式：新装默认简洁，升级用户维持完整，各供应商按设计说明显示写死的简洁字段。

**Architecture:** 用 `UsageCardDensity` 作为唯一全局设置，存在 `AppSettings` 和配置备份里。弹层顶栏方案 A 分段和设置 → 通用共用该值。`UsageCardDensityPolicy` 按供应商给出简洁模式要显示的 metricID；卡片视图按密度选择完整布局或简洁布局。额度简洁格复用进度条，只保留百分比、条和重置时刻。

**Tech Stack:** Swift 5、SwiftUI、AppKit、XCTest、XcodeGen、`scripts/test.sh`

**Spec:** [docs/superpowers/specs/2026-09-22-usage-card-density-design.md](../specs/2026-09-22-usage-card-density-design.md)

## Global Constraints

- 永远使用中文：注释、测试名、提交说明、设置文案。
- 前端/工程命令用 pnpm 的约定不适用于本 macOS 目标；测试用 XcodeGen + xcodebuild，禁止改 Android / 官网 / 传输 schema。
- 开关文案固定为 `简洁` 和 `完整`；说明文案固定为 `简洁模式只保留各供应商最常用的用量信息。`
- UserDefaults 键名固定为 `usageCardDensity`；枚举 rawValue 固定为 `compact` / `full`。
- 升级判定键（缺密度键时）：`refreshMinutes`、`displayOrder.v1`、`selectedCredentialIDs`、`availableCredentialIDs`、`credentialUsagePreferences.v1` 任一存在即视为升级，设为 `full`。
- 凭证详情页永远完整；弹层卡片和凭证管理列表卡片跟随全局密度。
- 分组倍率从 `UsageRowView` 卡片头和无障碍文案中去掉；分享图和凭证详情页可保留。
- 新 Swift 文件放进现有目录即可，`project.yml` 已按文件夹收录源码，不必改工程清单。
- 单测命令（每个失败/通过步骤都用这一套，只换 `-only-testing`）：

```bash
xcodegen generate && xcodebuild test \
  -project RoutinUsage.xcodeproj \
  -scheme RoutinUsage \
  -configuration Debug \
  -destination 'platform=macOS' \
  -derivedDataPath .build/test-derived \
  PRODUCT_BUNDLE_IDENTIFIER=ai.routin.mytoken.tests \
  CODE_SIGNING_ALLOWED=NO \
  -only-testing:RoutinUsageTests/<Class>/<test>
```

## Review Focus

- UserDefaults 里 `usageCardDensity` 是非法字符串、同时又有 `refreshMinutes`：按升级用户落成完整，不能当成新装简洁。对应 Task 1。
- 旧备份 JSON 没有 `usageCardDensity`：导入后为完整。对应 Task 2。
- 小米套餐简洁只显示套餐总量进度条，即使 `plan-total` 带 `windowEnd` 也不显示重置时刻。对应 Task 3 和 Task 6。
- 弹层为简洁时，凭证详情页仍是完整指标（Command Code 请求次数、小米 Token 拆分都在）。对应 Task 6。
- 分享图仍可带分组倍率，弹层卡片两种密度都不能显示「默认分组 ×2」。对应 Task 5。

## File Structure

- Create: `RoutinUsage/Models/UsageCardDensity.swift` — 密度枚举和中文标题
- Create: `RoutinUsage/Views/UsageCardDensityPolicy.swift` — 供应商 → 简洁 metricID / 是否显示重置
- Create: `RoutinUsage/Views/UsageCardDensitySegmentedControl.swift` — 弹层方案 A 胶囊分段
- Modify: `RoutinUsage/Models/AppSettings.swift` — 读写、新装/升级默认、备份往返
- Modify: `RoutinUsage/Configuration/ConfigurationBackup.swift` — 备份字段，缺省 `full`
- Modify: `RoutinUsage/Views/NormalizedUsageMetricGrid.swift` — `resetTimeOnly`
- Modify: `RoutinUsage/Views/UsageRowView.swift` — 密度入参、去分组倍率、简洁藏订阅日期
- Modify: `RoutinUsage/Views/ProviderUsageMetricSections.swift` — 小米 / GLM / 火山 / New API 简洁布局
- Modify: `RoutinUsage/Views/CommandCodeUsageMetricsView.swift` — 弹层简洁三窗口
- Modify: `RoutinUsage/Views/UsagePopoverView.swift` — 顶栏分段、把密度传给卡片
- Modify: `RoutinUsage/Views/Settings/CredentialManagementView.swift` — 列表卡跟随密度
- Modify: `RoutinUsage/Views/Settings/GeneralSettingsView.swift` — 通用页「卡片显示」
- Test: `RoutinUsageTests/AppSettingsTests.swift`
- Test: `RoutinUsageTests/ConfigurationBackupTests.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`
- Test: `RoutinUsageTests/GeneralSettingsViewTests.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`

---

### Task 1: 密度设置与新装/升级默认值

**Files:**
- Create: `RoutinUsage/Models/UsageCardDensity.swift`
- Modify: `RoutinUsage/Models/AppSettings.swift`
- Test: `RoutinUsageTests/AppSettingsTests.swift`

**Interfaces:**
- Consumes: 现有 `AppSettings(defaults:)` 初始化顺序和 `Keys`
- Produces: `enum UsageCardDensity: String, Codable, Equatable, Sendable, CaseIterable { case compact, full }`，`title` 为 `简洁`/`完整`；`AppSettings.usageCardDensity: UsageCardDensity`；UserDefaults 键 `usageCardDensity`

- [ ] **Step 1: 写失败测试**

在 `AppSettingsTests` 的 `test全新设置使用产品默认值` 增加：

```swift
XCTAssertEqual(settings.usageCardDensity, .compact)
```

并新增：

```swift
func test全新安装默认简洁并写回密度键() throws {
    let context = try makeContext()
    defer { context.cleanUp() }

    let settings = AppSettings(defaults: context.defaults)

    XCTAssertEqual(settings.usageCardDensity, .compact)
    XCTAssertEqual(
        context.defaults.string(forKey: "usageCardDensity"),
        UsageCardDensity.compact.rawValue
    )
    XCTAssertEqual(
        AppSettings(defaults: context.defaults).usageCardDensity,
        .compact
    )
}

func test升级用户缺少密度键时维持完整() throws {
    let context = try makeContext()
    defer { context.cleanUp() }
    context.defaults.set(5, forKey: "refreshMinutes")

    let settings = AppSettings(defaults: context.defaults)

    XCTAssertEqual(settings.usageCardDensity, .full)
    XCTAssertEqual(
        context.defaults.string(forKey: "usageCardDensity"),
        UsageCardDensity.full.rawValue
    )
}

func test非法密度键且已有旧设置时回落完整() throws {
    let context = try makeContext()
    defer { context.cleanUp() }
    context.defaults.set(5, forKey: "refreshMinutes")
    context.defaults.set("dense", forKey: "usageCardDensity")

    XCTAssertEqual(AppSettings(defaults: context.defaults).usageCardDensity, .full)
}

func test卡片密度可持久化并重新载入() throws {
    let context = try makeContext()
    defer { context.cleanUp() }
    let settings = AppSettings(defaults: context.defaults)

    settings.usageCardDensity = .full

    XCTAssertEqual(AppSettings(defaults: context.defaults).usageCardDensity, .full)
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: 上述单测命令，`-only-testing:RoutinUsageTests/AppSettingsTests/test全新安装默认简洁并写回密度键`

Expected: FAIL，`UsageCardDensity` 未定义。

- [ ] **Step 3: 最小实现**

`RoutinUsage/Models/UsageCardDensity.swift`：

```swift
import Foundation

enum UsageCardDensity: String, Codable, Equatable, Sendable, CaseIterable {
    case compact
    case full

    var title: String {
        switch self {
        case .compact: return "简洁"
        case .full: return "完整"
        }
    }
}
```

在 `AppSettings` 增加属性（`didSet` 写入 `Keys.usageCardDensity`）。`Keys` 增加 `usageCardDensity = "usageCardDensity"`。

`init(defaults:)` 里在读取完 `refreshMinutes` / `displayOrder` / 凭证偏好之后、不要依赖 `didSet`（init 里不会触发），按下面顺序赋值并 `defaults.set`：

1. 若 `defaults.string(forKey: Keys.usageCardDensity).flatMap(UsageCardDensity.init(rawValue:))` 有值，用它。
2. 否则若 `refreshMinutes`、`displayOrder.v1`、`selectedCredentialIDs`、`availableCredentialIDs`、`credentialUsagePreferences.v1` 任一 `object(forKey:)` 非 nil，设为 `.full` 并写回。
3. 否则设为 `.compact` 并写回。

非法字符串走 2/3，不能当成已配置。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/AppSettingsTests`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Models/UsageCardDensity.swift RoutinUsage/Models/AppSettings.swift RoutinUsageTests/AppSettingsTests.swift
git commit -m "feat: 新增卡片简洁完整密度设置"
```

---

### Task 2: 配置备份往返

**Files:**
- Modify: `RoutinUsage/Configuration/ConfigurationBackup.swift`
- Modify: `RoutinUsage/Models/AppSettings.swift`（`backupSettings` / `applyBackup`）
- Test: `RoutinUsageTests/ConfigurationBackupTests.swift`
- Test: `RoutinUsageTests/AppSettingsTests.swift`

**Interfaces:**
- Consumes: `UsageCardDensity`
- Produces: `ConfigurationBackupSettings.usageCardDensity`；解码缺字段时为 `.full`；`AppSettings.backupSettings` / `applyBackup` 往返该字段

- [ ] **Step 1: 写失败测试**

```swift
func test配置备份往返保留卡片密度() throws {
    let context = try makeContext()
    defer { context.cleanUp() }
    let settings = AppSettings(defaults: context.defaults)
    settings.usageCardDensity = .full

    let restored = AppSettings(defaults: context.defaults)
    restored.applyBackup(settings.backupSettings)

    XCTAssertEqual(restored.usageCardDensity, .full)
}

func test旧备份缺少卡片密度时导入为完整() throws {
    let context = try makeContext()
    defer { context.cleanUp() }
    let settings = AppSettings(defaults: context.defaults)
    var backup = settings.backupSettings
    let data = try JSONEncoder().encode(backup)
    var object = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
    object.removeValue(forKey: "usageCardDensity")
    let legacyData = try JSONSerialization.data(withJSONObject: object)

    backup = try JSONDecoder().decode(ConfigurationBackupSettings.self, from: legacyData)
    settings.applyBackup(backup)

    XCTAssertEqual(backup.usageCardDensity, .full)
    XCTAssertEqual(settings.usageCardDensity, .full)
}
```

`test配置备份往返保留凭证和偏好` 里顺手断言 `decoded.settings.usageCardDensity` 等于导出值。

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/AppSettingsTests/test旧备份缺少卡片密度时导入为完整`

Expected: FAIL，`ConfigurationBackupSettings` 没有该字段。

- [ ] **Step 3: 最小实现**

`ConfigurationBackupSettings` 增加 `var usageCardDensity: UsageCardDensity`，`CodingKeys` 增加对应 case。`init` 参数补上。`init(from:)` 用 `decodeIfPresent`，缺省 `.full`。

`AppSettings.backupSettings` 传入当前值；`applyBackup` 赋值 `usageCardDensity = backup.usageCardDensity`。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/AppSettingsTests` 和 `-only-testing:RoutinUsageTests/ConfigurationBackupTests`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Configuration/ConfigurationBackup.swift RoutinUsage/Models/AppSettings.swift RoutinUsageTests/AppSettingsTests.swift RoutinUsageTests/ConfigurationBackupTests.swift
git commit -m "feat: 配置备份纳入卡片密度"
```

---

### Task 3: 供应商简洁字段策略

**Files:**
- Create: `RoutinUsage/Views/UsageCardDensityPolicy.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`

**Interfaces:**
- Consumes: `UsageCardDensity`、`ProviderID`、凭证 `metadata`
- Produces:

```swift
struct UsageCardCompactSpec: Equatable {
    var metricIDs: [String]
    var showsResetTime: Bool
}

enum UsageCardDensityPolicy {
    static func compactSpec(
        providerID: ProviderID,
        metadata: [String: String]
    ) -> UsageCardCompactSpec
}
```

完整模式不走该函数，由视图直接渲染现有布局。

- [ ] **Step 1: 写失败测试**

```swift
func test简洁模式按供应商返回写死字段() {
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .routin, metadata: [:]).metricIDs,
        ["fiveHour", "weekly"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(
            providerID: .routin,
            metadata: ["usageKind": "tokenPack"]
        ).metricIDs,
        ["token"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .deepseek, metadata: [:]).metricIDs,
        ["balance"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(
            providerID: .xiaomi,
            metadata: ["usageKind": "api"]
        ).metricIDs,
        ["account-balance"]
    )
    let xiaomiPlan = UsageCardDensityPolicy.compactSpec(
        providerID: .xiaomi,
        metadata: ["usageKind": "plan"]
    )
    XCTAssertEqual(xiaomiPlan.metricIDs, ["plan-total"])
    XCTAssertFalse(xiaomiPlan.showsResetTime)
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .glm, metadata: [:]).metricIDs,
        ["five-hour", "weekly"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .volcengine, metadata: [:]).metricIDs,
        ["fiveHour", "weekly", "monthly"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .newAPI, metadata: [:]).metricIDs,
        ["today-token", "one-day-token", "seven-day-token", "thirty-day-token"]
    )
    XCTAssertEqual(
        UsageCardDensityPolicy.compactSpec(providerID: .commandCode, metadata: [:]).metricIDs,
        ["five-hour", "weekly", "credit-progress"]
    )
    XCTAssertTrue(
        UsageCardDensityPolicy.compactSpec(providerID: .glm, metadata: [:]).showsResetTime
    )
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test简洁模式按供应商返回写死字段`

Expected: FAIL，`UsageCardDensityPolicy` 未定义。

- [ ] **Step 3: 最小实现**

按上表写 `switch providerID`。小米缺 `usageKind` 时按 `api`。除小米套餐外 `showsResetTime == true`。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test简洁模式按供应商返回写死字段`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Views/UsageCardDensityPolicy.swift RoutinUsageTests/UsagePresentationPolicyTests.swift
git commit -m "feat: 增加各供应商简洁模式字段策略"
```

---

### Task 4: 简洁进度格只保留重置时刻

**Files:**
- Modify: `RoutinUsage/Views/NormalizedUsageMetricGrid.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`

**Interfaces:**
- Consumes: 现有 `NormalizedUsageMetricResetStyle`
- Produces: `NormalizedUsageMetricResetStyle.resetTimeOnly`：显示 `重置 HH:mm` / `重置 MM-dd HH:mm`，不显示剩余时长，不显示已用/限额

- [ ] **Step 1: 写失败测试**

扩展 `test通用用量卡片使用Routin小字号并完整单独显示重置时间`：完整路径仍断言 `.relativeDuration` 和 `remainingDurationText`。新增：

```swift
func test简洁进度格只渲染重置时刻() throws {
    let source = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/NormalizedUsageMetricGrid.swift"),
        encoding: .utf8
    )
    XCTAssertTrue(source.contains("case .resetTimeOnly"))
    XCTAssertTrue(source.contains("UsageFormatter.resetTime(windowEnd, now: now)"))
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test简洁进度格只渲染重置时刻`

Expected: FAIL，没有 `.resetTimeOnly`。

- [ ] **Step 3: 最小实现**

```swift
enum NormalizedUsageMetricResetStyle {
    case fullDateTime
    case relativeDuration
    case resetTimeOnly
}
```

在 `progressCell` 的 `windowEnd` 分支增加 `.resetTimeOnly`：只画一行 `重置 \(UsageFormatter.resetTime(windowEnd, now: now))`。简洁调用方同时传 `showsAmountDetails: false`。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test简洁进度格只渲染重置时刻`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Views/NormalizedUsageMetricGrid.swift RoutinUsageTests/UsagePresentationPolicyTests.swift
git commit -m "feat: 简洁进度格只显示重置时刻"
```

---

### Task 5: 卡片头去掉分组倍率并接入密度

**Files:**
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/Settings/CredentialManagementView.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`

**Interfaces:**
- Consumes: `UsageCardDensity`
- Produces: `UsageRowView.density: UsageCardDensity = .full`；`headerView` 不再调用 `groupMultiplierText`；`subscriptionPeriodDetails` 仅 `density == .full` 时显示；无障碍文案不再追加分组倍率

- [ ] **Step 1: 写失败测试**

改 `ProjectBootstrapTests.test弹窗将分组倍率置于百分比下方并右对齐`：断言 `header` **不包含** `groupMultiplierText(currentGroupMultiplier)`。

改 `test弹窗倒计时每分钟刷新并将分组倍率合并为一行`：`UsageRowView` 仍可引用 `UsageFormatter.groupMultiplierText` 于无障碍之外则删掉该断言；改为断言无障碍 `label` **不** `details.append(UsageFormatter.groupMultiplierText`。

新增源码测试：

```swift
func test弹窗卡片按密度渲染并默认完整以免详情页误伤() throws {
    let row = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/UsageRowView.swift"),
        encoding: .utf8
    )
    let popover = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
        encoding: .utf8
    )
    XCTAssertTrue(row.contains("var density: UsageCardDensity = .full"))
    XCTAssertTrue(popover.contains("density: settings.usageCardDensity"))
    XCTAssertFalse(row.contains("groupMultiplierText(currentGroupMultiplier)"))
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test弹窗卡片按密度渲染并默认完整以免详情页误伤`

Expected: FAIL。

- [ ] **Step 3: 最小实现**

`UsageRowView` 增加 `var density: UsageCardDensity = .full`。

删除头部 `if let currentGroupMultiplier { groupMultiplierText(...) }`。`subscriptionPeriodDetails` 外包 `if density == .full`。`UsageRowAccessibility.label` 去掉 `groupMultipliers` 拼接。

`periodicContent` / `metricContent`：当 `density == .compact` 只保留标题、百分比、进度条、`重置` 一行，去掉 `amount`、`剩余` 数量、剩余时长。

`normalizedMetricsContent` 把 `density` 传给各供应商视图（下一任务接住；本任务可先只改签名默认 `.full`，避免编译失败则同步加参数默认值）。

`UsagePopoverView` 和 `CredentialManagementView` 的 `UsageRowView(...)` 传入 `density: environment.settings.usageCardDensity` 或 `settings.usageCardDensity`。

分享构建不要动。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/ProjectBootstrapTests` 和上述新测试。

Expected: PASS。`UsageShareCardTests` 里分组倍率断言仍通过。

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Views/UsageRowView.swift RoutinUsage/Views/UsagePopoverView.swift RoutinUsage/Views/Settings/CredentialManagementView.swift RoutinUsageTests/ProjectBootstrapTests.swift RoutinUsageTests/UsagePresentationPolicyTests.swift
git commit -m "feat: 弹层卡片接入密度并移除分组倍率"
```

---

### Task 6: 各供应商简洁布局

**Files:**
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/ProviderUsageMetricSections.swift`
- Modify: `RoutinUsage/Views/CommandCodeUsageMetricsView.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`
- Test: `RoutinUsageTests/CredentialDetailsViewTests.swift`（若已有源码断言，确认详情页仍不传 `density: .compact`）

**Interfaces:**
- Consumes: `UsageCardDensity`、`UsageCardDensityPolicy.compactSpec`、`.resetTimeOnly`
- Produces: 弹层 `density == .compact` 时各供应商只渲染策略表中的字段；`CredentialDetailsView` 继续调用不带密度或 `density: .full` 的视图

- [ ] **Step 1: 写失败测试**

```swift
func test供应商简洁视图按策略过滤字段() throws {
    let sections = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/ProviderUsageMetricSections.swift"),
        encoding: .utf8
    )
    let command = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/CommandCodeUsageMetricsView.swift"),
        encoding: .utf8
    )
    let details = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/Settings/CredentialDetailsView.swift"),
        encoding: .utf8
    )
    XCTAssertTrue(sections.contains("var density: UsageCardDensity = .full"))
    XCTAssertTrue(command.contains("var density: UsageCardDensity = .full"))
    XCTAssertTrue(sections.contains("UsageCardDensityPolicy.compactSpec("))
    XCTAssertFalse(details.contains("density: .compact"))
    XCTAssertTrue(command.contains("density == .compact"))
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests/test供应商简洁视图按策略过滤字段`

Expected: FAIL。

- [ ] **Step 3: 最小实现**

各专用视图增加 `var density: UsageCardDensity = .full`。`UsageRowView.normalizedMetricsContent` 传入 `density`。

简洁分支：

- 小米 API：只画 `account-balance` 余额格。
- 小米套餐：`NormalizedUsageMetricGrid` 只留 `plan-total`，`showsAmountDetails: false`，`resetTimeStyle` 任意但 `showsResetTime == false` 时不渲染重置（给 grid 增加 `showsResetTime: Bool = true`，小米套餐简洁传 `false`）。
- GLM：只留 `five-hour`、`weekly`，`showsAmountDetails: false`，`.resetTimeOnly`。
- 火山 Agent / Coding：只留 `fiveHour`、`weekly`、`monthly`；月度仍独占一行；细节行去掉，重置用 `.resetTimeOnly`。
- New API：只画今日 / 24 小时 / 7 天 / 30 天 Token 的 2×2 数字格，不画额度卡和请求活动。
- Command Code：`density == .compact` 时只画 5 小时、周、月三块进度，使用简洁明细（无已用/剩余/剩余时长），不要 `requestCount`。`displayMode: .details` 且 `density == .full` 保持现状。
- DeepSeek / Routin 走 `NormalizedUsageMetricGrid`：按策略过滤 metricID，简洁用 `.resetTimeOnly` 且 `showsAmountDetails: false`。

`CredentialDetailsView` 不传 density，默认完整。

- [ ] **Step 4: 跑测试确认通过**

Run: `-only-testing:RoutinUsageTests/UsagePresentationPolicyTests` 和 `-only-testing:RoutinUsageTests/CredentialDetailsViewTests`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Views/UsageRowView.swift RoutinUsage/Views/ProviderUsageMetricSections.swift RoutinUsage/Views/CommandCodeUsageMetricsView.swift RoutinUsage/Views/NormalizedUsageMetricGrid.swift RoutinUsageTests/UsagePresentationPolicyTests.swift
git commit -m "feat: 按供应商实现简洁卡片布局"
```

---

### Task 7: 弹层方案 A 开关和通用设置项

**Files:**
- Create: `RoutinUsage/Views/UsageCardDensitySegmentedControl.swift`
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/Settings/GeneralSettingsView.swift`
- Test: `RoutinUsageTests/GeneralSettingsViewTests.swift`
- Test: `RoutinUsageTests/UsagePopoverLayoutTests.swift` 或 `UsagePresentationPolicyTests.swift`

**Interfaces:**
- Consumes: `Binding<UsageCardDensity>`、`UsageCardDensity.title`
- Produces: 顶栏设置按钮左侧 30pt 胶囊分段；通用页「卡片显示」绑定同一 `settings.usageCardDensity`

- [ ] **Step 1: 写失败测试**

`GeneralSettingsViewTests` 增加：

```swift
func test通用页提供卡片简洁完整显示() throws {
    let source = try TestSourceReader.read([
        "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
    ])
    XCTAssertTrue(source.contains("卡片显示"))
    XCTAssertTrue(source.contains("简洁模式只保留各供应商最常用的用量信息。"))
    XCTAssertTrue(source.contains("settings.usageCardDensity"))
}
```

弹层源码测试：

```swift
func test弹窗顶栏设置左侧有简洁完整分段() throws {
    let popover = try String(
        contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
        encoding: .utf8
    )
    XCTAssertTrue(popover.contains("UsageCardDensitySegmentedControl"))
    XCTAssertTrue(popover.contains("openSettings()"))
    let toolbar = popover.range(of: "var toolbar: some View")!
    let toolbarSlice = popover[toolbar.lowerBound...]
    let segmentedIndex = toolbarSlice.range(of: "UsageCardDensitySegmentedControl")!.lowerBound
    let gearIndex = toolbarSlice.range(of: "openSettings()")!.lowerBound
    XCTAssertTrue(segmentedIndex < gearIndex)
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `-only-testing:RoutinUsageTests/GeneralSettingsViewTests/test通用页提供卡片简洁完整显示`

Expected: FAIL。

- [ ] **Step 3: 最小实现**

`UsageCardDensitySegmentedControl`：高度 30，圆角 8，浅填充 + 描边对齐 `GlassIconButton`。内部两个按钮 `简洁` / `完整`，选中段白底。绑定 `UsageCardDensity`。无障碍标签「卡片显示」。

`UsagePopoverView.toolbar` 右侧 `HStack(spacing: 8)`：分段控件，然后设置齿轮。

`GeneralSettingsView` 在通知和颜色规则之间加 `displaySection`：左侧 `settingLabel(title: "卡片显示", message: "简洁模式只保留各供应商最常用的用量信息。")`，右侧同一分段或 `Picker` 绑定 `$settings.usageCardDensity`。

- [ ] **Step 4: 跑相关测试并跑全量**

Run: 新测试，以及 `scripts/test.sh`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage/Views/UsageCardDensitySegmentedControl.swift RoutinUsage/Views/UsagePopoverView.swift RoutinUsage/Views/Settings/GeneralSettingsView.swift RoutinUsageTests/GeneralSettingsViewTests.swift RoutinUsageTests/UsagePresentationPolicyTests.swift RoutinUsageTests/UsagePopoverLayoutTests.swift
git commit -m "feat: 弹层和通用设置加入简洁完整开关"
```

---

## Self-Review

1. Spec coverage: 第 2 节开关与默认值 → Task 1/7；第 6 节顶栏方案 A 和分组倍率 → Task 5/7；第 7 节供应商字段 → Task 3/6；第 8 节备份与升级键 → Task 1/2；第 9 节渲染与详情页完整 → Task 4/5/6；第 11 节不做 Android → 全任务未改 `android/`。
2. Placeholder scan: 无 TBD / 「类似 Task N」。
3. Type consistency: 全程 `UsageCardDensity`、`compactSpec(providerID:metadata:)`、`resetTimeOnly`。
4. Review Focus 五条均已落到 Task 1/2/3/5/6 的测试。
