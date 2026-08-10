# 本地 Key、窗口记忆与用量倒计时 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 plan Key 改为 App 本地存储，增加设置窗口尺寸记忆和临时明文查看，并修正弹窗的窗口倒计时、分组倍率和订阅时间展示。

**Architecture:** 用本地 Key 存储实现替换 Keychain 依赖，保持 `KeyRepository` 的增删改接口不变；设置窗口通过窗口代理监听尺寸并持久化宽高；接口模型将窗口结束时间、分组名称/倍率和 `startAt`/`endAt` 映射为 `UsageSnapshot`；弹窗只消费格式化后的剩余时长和分组倍率，不再显示允许模型清单。

**Tech Stack:** Swift 6、SwiftUI、AppKit、Observation、UserDefaults、XCTest、XcodeGen。

## Global Constraints

- macOS 最低版本保持 14.0。
- plan Key 直接保存在 App 本地偏好设置中，接受本地明文存储风险；不请求或使用系统钥匙串。
- Key 列表默认显示掩码；眼睛图标显示完整 Key 的状态只在当前设置窗口内有效，关闭窗口自动隐藏。
- 设置窗口允许拖动边角调整大小，记住上一次宽高，并保留最小宽高限制。
- 弹窗不显示允许模型清单；设置页 Key 详情可以继续显示。
- 5 小时剩余时长使用 `dayWindowEndAt - 当前时间`，周剩余时长使用 `weekWindowEndAt - 当前时间`；格式分别如 `3h45m`、`4d3h45m`，结束显示“已结束”，缺失显示“—”。
- 分组倍率按 `groupNames` 与 `groupMultipliers` 同索引配对，显示如 `Codex ×1、Codex Pro ×2`。
- 订阅开始/结束分别映射 `startAt`、`endAt`；时间按绝对 Date 计算，展示使用当前时区。
- 所有新增注释、测试和界面文案使用中文，不新增第三方依赖。

---

### Task 1: 本地 Key 存储替换

**Files:**
- Create: `RoutinUsage/Security/LocalKeyStore.swift`
- Modify: `RoutinUsage/Persistence/KeyRepository.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/Usage/UsageStore.swift`
- Test: `RoutinUsageTests/LocalKeyStoreTests.swift`
- Test: `RoutinUsageTests/KeyRepositoryTests.swift`

**Interfaces:**
- `LocalKeyStoring` 提供 `save(_:for:) throws`、`read(for:) throws -> String?`、`delete(for:) throws`。
- `KeyRepository` 保留现有增删改列表 API，但依赖 `LocalKeyStoring`；持久化 Key 与配置元数据均使用本地 UserDefaults。

- [ ] **Step 1: 写失败测试**：测试本地 Key 新增、读取、覆盖、删除和不存在读取为空；更新仓储测试断言完整 Key 写入本地存储而不是 Keychain。
- [ ] **Step 2: 运行相关测试确认失败**：先运行 `xcodebuild ... -only-testing:RoutinUsageTests/LocalKeyStoreTests -only-testing:RoutinUsageTests/KeyRepositoryTests test`，预期新接口不存在或旧 Keychain 断言失败。
- [ ] **Step 3: 写最小实现**：实现本地存储、替换依赖注入和生产环境构造；移除运行期 Keychain 请求与清理调用，但保留无关的旧测试文件不纳入生产目标。
- [ ] **Step 4: 重跑相关测试确认通过**。
- [ ] **Step 5: 提交**：`git add RoutinUsage/Security RoutinUsage/Persistence/KeyRepository.swift RoutinUsage/App/AppEnvironment.swift RoutinUsage/Usage/UsageStore.swift RoutinUsageTests && git commit -m "feat: 将plan Key改为本地存储"`。

### Task 2: 设置窗口尺寸记忆与 Key 临时查看

**Files:**
- Create: `RoutinUsage/Views/WindowFramePersistence.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Test: `RoutinUsageTests/AppSettingsTests.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `WindowFramePersistence` 负责读取/保存设置窗口宽高并设置最小尺寸。
- `SettingsView` 使用 `@State` 保存当前展示 Key 的 UUID 集合；眼睛按钮切换显示/隐藏，不写入 UserDefaults。

- [ ] **Step 1: 写失败测试**：测试窗口宽高默认值、保存后重载；源代码测试确认设置页包含眼睛按钮和窗口代理；测试关闭/重建视图时不读取查看状态。
- [ ] **Step 2: 运行相关测试确认失败**。
- [ ] **Step 3: 写最小实现**：通过 `NSViewRepresentable` 获取设置窗口，监听 `NSWindow.didResizeNotification`，保存宽高并在出现时恢复；Key 行增加 `eye` / `eye.slash` 按钮显示本地完整 Key。
- [ ] **Step 4: 重跑相关测试确认通过**。
- [ ] **Step 5: 提交**：`git add RoutinUsage/Views RoutinUsageTests && git commit -m "feat: 记忆设置窗口尺寸并支持临时查看Key"`。

### Task 3: 用量字段、窗口结束时间与分组倍率映射

**Files:**
- Modify: `RoutinUsage/Models/UsageDTO.swift`
- Modify: `RoutinUsage/Models/UsageSnapshot.swift`
- Modify: `RoutinUsage/Usage/UsageMapper.swift`
- Test: `RoutinUsageTests/UsageMapperTests.swift`
- Test: `RoutinUsageTests/UsageCacheTests.swift`

**Interfaces:**
- `UsageSnapshot` 保存 `dayWindowEndAt`、`weekWindowEndAt` 对应的五小时/周窗口结束 Date，保存 `groupNames`、`groupMultipliers` 的配对结果，并使用 `subscriptionStartAt`/`subscriptionEndAt` 对应 `startAt`/`endAt`。
- 兼容旧缓存：新增字段缺失时为 nil 或空数组，不影响解码。

- [ ] **Step 1: 写失败测试**：fixture 使用 `dayWindowEndAt`、`weekWindowEndAt`、`groupNames`、`groupMultipliers`、`startAt`、`endAt`，断言映射结果和数组配对。
- [ ] **Step 2: 运行 `xcodebuild ... -only-testing:RoutinUsageTests/UsageMapperTests test` 确认失败**。
- [ ] **Step 3: 写最小实现**：增加 DTO 字段、Mapper 解析和 Snapshot 可选字段；时间解码为绝对 Date，不在 Mapper 内做本地时区偏移。
- [ ] **Step 4: 重跑 Mapper 与缓存测试确认通过**。
- [ ] **Step 5: 提交**：`git add RoutinUsage/Models RoutinUsage/Usage RoutinUsageTests && git commit -m "feat: 映射窗口时间与分组倍率"`。

### Task 4: 弹窗剩余时长和分组倍率展示

**Files:**
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Usage/UsageFormatter.swift`
- Test: `RoutinUsageTests/UsageFormatterTests.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `UsageFormatter.remainingDurationText(until:now:) -> String` 输出天、小时、分钟；结束返回“已结束”；缺失由调用方显示“—”。
- `UsageRowView.details` 显示“5 小时剩余”“周剩余”“分组倍率”，不再渲染“允许模型”。

- [ ] **Step 1: 写失败测试**：测试 `3h45m`、`4d3h45m`、零/负时长“已结束”、跨时区绝对时间计算；源代码测试确认弹窗详情移除 allowedModels。
- [ ] **Step 2: 运行相关测试确认失败**。
- [ ] **Step 3: 写最小实现**：用 `Date` 差值和整分钟格式化，按配对分组数组渲染倍率，移除弹窗模型列表。
- [ ] **Step 4: 重跑相关测试确认通过**。
- [ ] **Step 5: 提交**：`git add RoutinUsage/Views/UsageRowView.swift RoutinUsage/Usage/UsageFormatter.swift RoutinUsageTests && git commit -m "feat: 调整弹窗倒计时与分组倍率"`。

### Task 5: 全量验证与交付

**Files:** 无；仅验证已有实现和构建脚本。

- [ ] **Step 1: 运行全量测试**：`xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO test`。
- [ ] **Step 2: 构建 DMG 并校验**：运行 `scripts/build-dmg.sh` 和 `hdiutil verify build/dist/MyRoutin.dmg`。
- [ ] **Step 3: 检查差异并推送**：运行 `git diff --check`、`git status --short --branch`、`git push`，确认 GitHub CI 已触发。
