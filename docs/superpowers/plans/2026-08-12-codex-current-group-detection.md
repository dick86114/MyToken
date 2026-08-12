# Codex 当前分组检测 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为每个本地 plan Key 手动探测并显示其本次 Codex 请求的实际 Routin 分组。

**Architecture:** 新增独立的探测记录仓库、网络探测客户端和网页会话读取器。`CodexGroupDetectionService` 串行协调登录、账号指纹校验、探测请求及日志匹配；`AppEnvironment` 负责将其接入现有 Key 生命周期和 SwiftUI 视图。网页认证继续仅存在于现有 `WKWebView` 默认数据仓库中。

**Tech Stack:** Swift 6、SwiftUI、Observation、Foundation、CryptoKit、WebKit、XCTest。

## Global Constraints

- 仅由用户手动触发探测；后台刷新不得发送付费请求。
- 不读取、导出或持久化 Cookie、网页登录令牌、邮箱原文、完整 Key、探测 UUID、请求体或响应体。
- 探测日志必须以本次唯一 `User-Agent` 精确匹配，不得以最新普通日志推断。
- 已绑定 Key 的账号指纹不匹配时必须在发送请求前终止。
- 首次绑定没有匹配日志时只能报告未找到记录或超时，不能断言账号不匹配。
- 分组倍率始终来自当前 `UsageSnapshot.groupMultipliers`；历史检测结果只保存分组名称和检测时间。
- 所有项目注释和用户可见文案使用中文。

---

### Task 1: 探测记录、账号指纹和仓库

**Files:**
- Create: `RoutinUsage/GroupDetection/CodexGroupDetectionRecord.swift`
- Create: `RoutinUsage/GroupDetection/CodexGroupDetectionRepository.swift`
- Create: `RoutinUsageTests/CodexGroupDetectionRepositoryTests.swift`

**Interfaces:**
- Produces `CodexGroupDetectionRecord`、`CodexGroupDetectionStoring`、`CodexGroupDetectionRepository`。
- `load(for:) throws -> CodexGroupDetectionRecord?`、`save(_:) throws`、`delete(for:) throws`。
- `RoutinAccountIdentity.make(email:displayName:) -> RoutinAccountIdentity` 只公开 SHA-256 摘要和显示名。

- [ ] **Step 1: 写失败测试**：验证邮箱大小写和首尾空白得到同一摘要，保存数据不包含邮箱原文，记录可按 Key 隔离读写删除。
- [ ] **Step 2: 运行测试确认失败**：`xcodebuild test -scheme RoutinUsage -only-testing:RoutinUsageTests/CodexGroupDetectionRepositoryTests`。
- [ ] **Step 3: 实现最小模型与 UserDefaults 仓库**：使用 `CryptoKit.SHA256`、单个 JSON 字典键和损坏数据回退为空。
- [ ] **Step 4: 运行 Task 1 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 保存 Codex 分组检测结果`。

### Task 2: Codex 探测请求客户端

**Files:**
- Create: `RoutinUsage/GroupDetection/CodexGroupProbeClient.swift`
- Create: `RoutinUsageTests/CodexGroupProbeClientTests.swift`

**Interfaces:**
- Produces `CodexGroupProbing`，其方法为 `probe(apiKey:marker:) async throws`。
- `CodexGroupProbeRequestMarker` 持有一次性 UUID 和探测开始时间；其 `userAgent` 为 `MyRoutin-Group-Probe/<UUID>`。
- `CodexGroupProbeError` 区分 `invalidKey`、`modelUnavailable`、`network`、`server(statusCode:)` 与 `invalidResponse`。

- [ ] **Step 1: 写失败测试**：验证 Responses 端点、Bearer 头、唯一 User-Agent、JSON 最小体、15 秒超时，以及 401 映射。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 实现最小客户端**：固定单一低成本 Codex 候选模型，除明确模型不可用外不自动重试。
- [ ] **Step 4: 运行 Task 2 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 添加 Codex 分组探测请求`。

### Task 3: 网页会话账号和日志读取

**Files:**
- Modify: `RoutinUsage/CheckIn/RoutinWebSession.swift`
- Create: `RoutinUsage/GroupDetection/RoutinGroupDetectionWebSession.swift`
- Create: `RoutinUsageTests/RoutinGroupDetectionWebSessionTests.swift`

**Interfaces:**
- Produces `RoutinGroupDetectionWebSessionManaging`。
- `readCurrentAccountIdentity() async throws -> RoutinAccountIdentity`。
- `findGroupName(marker:startedAt:) async throws -> String`。
- 失败类型区分 `needsLogin`、`accountUnavailable`、`logTimeout`、`pageChanged` 和 `ambiguousLog`。

- [ ] **Step 1: 写失败解析测试**：账户页只读邮箱/用户名提取，日志表 User-Agent 精确匹配，分组提取，缺列与多条匹配处理。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 实现语义解析纯函数和 WebKit 适配器**：适配器复用现有 `RoutinWebSession.webView`，仅在登录完成后调用。
- [ ] **Step 4: 运行 Task 3 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 读取 Routin 账号与请求日志`。

### Task 4: 串行检测状态服务

**Files:**
- Create: `RoutinUsage/GroupDetection/CodexGroupDetectionService.swift`
- Create: `RoutinUsageTests/CodexGroupDetectionServiceTests.swift`

**Interfaces:**
- Produces `CodexGroupDetectionState` 和 `CodexGroupDetectionService`。
- `start(keyID:secret:snapshot:) async`、`didFinishLogin() async`、`cancel(keyID:)`、`clearRecord(for:)`。
- 服务依赖 Task 1-3 接口；状态按 Key 存储，全局仅允许一个活动任务。

- [ ] **Step 1: 写失败测试**：未登录恢复、已绑定账号不同阻止请求、成功保存、首次未找到日志不误报、并发点击只请求一次、取消不保存。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 实现状态机**：先读账号，再检查既有摘要，再发起探测和日志读取；日志分组不在快照时保存记录但标记未点亮。
- [ ] **Step 4: 运行 Task 4 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 编排 Codex 分组检测流程`。

### Task 5: 应用生命周期与登录窗口接入

**Files:**
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsage/Usage/UsageStore.swift`
- Modify: `RoutinUsageTests/AppLifecycleTests.swift`

**Interfaces:**
- `AppEnvironment` 暴露 `codexGroupDetection`、`startCodexGroupDetection(for:)`、`clearCodexGroupDetection(for:)`。
- Key 密文更新、删除时调用 `clearCodexGroupDetection(for:)`；Key 别名更新不清除。
- 现有网页登录完成回调同时恢复签到和分组检测。

- [ ] **Step 1: 写失败测试**：Key 更新和删除清理检测记录；网页登录完成会恢复待处理分组检测。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 最小接入环境、Key 生命周期和窗口回调**。
- [ ] **Step 4: 运行 Task 5 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 接入分组检测生命周期`。

### Task 6: 菜单栏与设置界面

**Files:**
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsage/Usage/UsageFormatter.swift`
- Modify: `RoutinUsageTests/UsageFormatterTests.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- `UsageFormatter.groupMultiplierSegments(_:highlightedGroupName:)` 生成逐项显示模型。
- `UsageRowView` 接收检测状态和开始检测闭包。
- 设置页显示关联账号并提供解除关联入口。

- [ ] **Step 1: 写失败测试**：分组完整文本精确高亮、倍率变化仍采用当前值、无匹配不高亮、视图含固定尺寸探测控件和无障碍标签。
- [ ] **Step 2: 运行测试确认失败**。
- [ ] **Step 3: 最小实现逐项文本、定位图标、进度、状态信息和设置页操作**。
- [ ] **Step 4: 运行 Task 6 测试确认通过**。
- [ ] **Step 5: 提交**：`feat: 显示 Codex 当前分组`。

### Task 7: 全量验证和人工验证准备

**Files:**
- Modify: `docs/首次运行说明.md`
- Modify: `docs/superpowers/specs/2026-08-12-codex-current-group-detection-design.md`

- [ ] **Step 1: 补充用户可见说明**：手动探测会产生少量费用，首次使用要求登录，检测结果只对应 Codex 请求。
- [ ] **Step 2: 运行全部测试**：`./scripts/test.sh`。
- [ ] **Step 3: 运行构建**：`xcodebuild build -scheme RoutinUsage -configuration Debug`。
- [ ] **Step 4: 静态复核**：搜索敏感字段，确认不把完整 Key、邮箱、Cookie、UUID、请求或响应正文写入持久化和诊断。
- [ ] **Step 5: 提交**：`docs: 说明 Codex 分组检测`。

## 自检

- Task 1 覆盖持久化、摘要和 Key 隔离。
- Task 2 覆盖请求成本边界和不可重试规则。
- Task 3 覆盖网页登录态、账号和日志语义解析。
- Task 4 覆盖串行状态机与账号归属结论边界。
- Task 5 覆盖现有 Key 与登录生命周期。
- Task 6 覆盖绿色文本、固定控件、无障碍和解除关联。
- Task 7 覆盖用户说明、全量测试、构建和敏感数据复核。
