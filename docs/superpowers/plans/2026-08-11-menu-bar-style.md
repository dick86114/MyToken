# 菜单栏显示样式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MyRoutin 增加可持久化的三档菜单栏显示样式，并实现“别名 + 竖形进度条”。

**Architecture:** `AppSettings` 持有 `MenuBarStyle` 并通过 UserDefaults 持久化；`UsageFormatter` 负责别名截断、文本样式和有效指标判定；`MenuBarLabelView` 负责文本与自绘竖形胶囊柱；`SettingsView` 提供 Picker。异常状态继续复用现有状态文本。

**Tech Stack:** Swift 6、SwiftUI、Observation、XCTest、XcodeGen。

## Global Constraints

- macOS 最低版本保持 14.0。
- 默认样式保持“仅百分比”。
- 别名最多显示 6 个字符，超出保留前 5 个字符并追加 `…`。
- 竖形进度条约 7pt × 18pt，从底部向上填充；低于 80% 绿色、80% 至 94% 黄色、95% 及以上红色。
- 周期订阅服从 5 小时 / 周设置；Token 资源包继续显示 Token 指标。
- 不新增第三方依赖；代码和测试文案使用中文。

---

### Task 1: 菜单栏样式模型与设置持久化

**Files:**
- Create: `RoutinUsage/Models/MenuBarStyle.swift`
- Modify: `RoutinUsage/Models/AppSettings.swift`
- Test: `RoutinUsageTests/AppSettingsTests.swift`

**Interfaces:**
- `MenuBarStyle: String, CaseIterable, Codable, Equatable, Sendable`，包含 `.percent`、`.aliasVerticalBar`、`.aliasPercent`。
- `AppSettings.menuBarStyle: MenuBarStyle` 写入 `menuBarStyle` UserDefaults；未知值和缺失值回退 `.percent`。

- [ ] **Step 1: 写失败测试**
  - 在 `AppSettingsTests` 测试新实例默认 `.percent`。
  - 依次写入 `.aliasVerticalBar`、`.aliasPercent`，新实例读取到相同值。
  - 直接写入字符串 `broken` 后，新实例回退 `.percent`。
- [ ] **Step 2: 运行测试确认失败**
  - 运行 `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/AppSettingsTests test`。
  - 预期因 `MenuBarStyle` 和 `menuBarStyle` 尚不存在而编译或断言失败。
- [ ] **Step 3: 写最小实现**
  - 创建枚举及中文标题，在 `AppSettings` 增加属性、didSet 和读取逻辑。
- [ ] **Step 4: 运行同一测试命令确认通过**
- [ ] **Step 5: 提交**
  - `git add RoutinUsage/Models/MenuBarStyle.swift RoutinUsage/Models/AppSettings.swift RoutinUsageTests/AppSettingsTests.swift && git commit -m "feat: 增加菜单栏样式设置"`

### Task 2: 菜单栏文本与别名格式化

**Files:**
- Modify: `RoutinUsage/Usage/UsageFormatter.swift`
- Test: `RoutinUsageTests/UsageFormatterTests.swift`

**Interfaces:**
- `UsageFormatter.truncatedMenuBarAlias(_:) -> String`：短别名原样返回，超长返回前 5 个字符加 `…`。
- 新增 `menuBarText(state:dimension:style:)`；`.percent` 和 `.aliasPercent` 返回文本，`.aliasVerticalBar` 在有效指标时返回截断别名，异常状态返回现有 `--`、`…`、`!`。
- 保留无 style 参数的旧 `menuBarText`，内部按 `.percent` 调用。

- [ ] **Step 1: 写失败测试**
  - 测试别名截断为 6 个字符。
  - 测试 `.aliasPercent` 返回“主账号 68%”。
  - 测试 `.aliasVerticalBar` 有效时返回“主账号”，加载时返回“…”；Token 资源包仍返回 Token 百分比。
- [ ] **Step 2: 运行 `xcodebuild ... -only-testing:RoutinUsageTests/UsageFormatterTests test` 确认失败**
- [ ] **Step 3: 写最小实现**
  - 复用现有 `metric(in:dimension:)` 和 `percentText`，仅增加样式分支与别名截断。
- [ ] **Step 4: 重跑同一测试命令确认通过**
- [ ] **Step 5: 提交**
  - `git add RoutinUsage/Usage/UsageFormatter.swift RoutinUsageTests/UsageFormatterTests.swift && git commit -m "feat: 格式化菜单栏别名样式"`

### Task 3: SwiftUI 菜单栏竖形进度条与设置入口

**Files:**
- Modify: `RoutinUsage/Views/MenuBarLabelView.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `MenuBarLabelView` 根据 `settings.menuBarStyle` 选择文本或 `Text(alias) + VerticalUsageBar`。
- `VerticalUsageBar` 是 SwiftUI 视图，输入 `percent: Double`、`color: Color`，将百分比限制在 0...100，用 `GeometryReader` 和 `Capsule` 自绘约 7pt × 18pt 的底部填充柱。
- 设置页“菜单栏显示”区域增加 `Picker("显示样式", selection: $settings.menuBarStyle)`，包含三个枚举 tag。

- [ ] **Step 1: 写失败测试**
  - `ProjectBootstrapTests` 读取两个源文件，断言设置页包含“显示样式”、`aliasVerticalBar`、`aliasPercent`，菜单栏视图包含 `VerticalUsageBar` 和 `settings.menuBarStyle`。
- [ ] **Step 2: 运行 `xcodebuild ... -only-testing:RoutinUsageTests/ProjectBootstrapTests test` 确认失败**
- [ ] **Step 3: 写最小实现**
  - 增加 Picker、竖条视图、风险颜色；无有效指标时沿用文本状态。为控件提供完整别名和百分比的可访问性标签。
- [ ] **Step 4: 重跑同一测试命令确认通过**
- [ ] **Step 5: 提交**
  - `git add RoutinUsage/Views/MenuBarLabelView.swift RoutinUsage/Views/SettingsView.swift RoutinUsageTests/ProjectBootstrapTests.swift && git commit -m "feat: 增加菜单栏竖形进度条样式"`

### Task 4: 全量验证与交付

**Files:** 无；仅验证实现与构建脚本。

- [ ] **Step 1: 运行全量测试**
  - `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO test`，确认所有测试通过。
- [ ] **Step 2: 构建并校验 DMG**
  - 运行 `scripts/build-dmg.sh`，再运行 `hdiutil verify build/dist/MyRoutin.dmg`，确认 Release Universal App 和 DMG 校验成功。
- [ ] **Step 3: 差异检查并推送**
  - 运行 `git diff --check`、`git status --short --branch`、`git push`；确认无未提交源码改动并触发 GitHub CI。
