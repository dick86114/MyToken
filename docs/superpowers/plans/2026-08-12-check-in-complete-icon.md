# 签到完成图标 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在菜单栏弹窗中以绿色实心图标表示当天已确认签到。

**Architecture:** 复用既有 `RoutinCheckInState`，仅在 `UsagePopoverView` 根据 `.alreadyCheckedIn` 选择图标和辅助功能文案；不改动签到会话、服务或设置页。

**Tech Stack:** Swift 5、SwiftUI、XCTest。

## Global Constraints

- 仅在 `.alreadyCheckedIn` 时显示绿色 `checkmark.circle.fill`。
- 登录中和签到中继续显示固定尺寸加载指示器并禁用按钮。
- 其他状态保持普通 `checkmark.circle`。
- 不新增持久化字段，不保存账号密码、验证码或 Cookie。

---

### Task 1: 签到完成图标

**Files:**
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**
- 消费：`RoutinCheckInState.alreadyCheckedIn`、`statusText`、`isBusy`。
- 产出：签到按钮在已签到状态的实心绿色图标与“今天已签到”辅助功能文案。

- [x] **Step 1: 编写失败静态测试**：在 `ProjectBootstrapTests` 断言 `UsagePopoverView` 包含 `.alreadyCheckedIn`、`checkmark.circle.fill`、`Color.green` 和“今天已签到”。
- [x] **Step 2: 运行测试确认失败**：

```bash
xcodegen generate
xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug -destination 'platform=macOS' CODE_SIGNING_ALLOWED=NO -only-testing:RoutinUsageTests/ProjectBootstrapTests test
```

- [x] **Step 3: 实现最小图标分支**：已签到显示 `checkmark.circle.fill` 和 `Color.green`，进行中保留 `ProgressView`，其余显示 `checkmark.circle`；更新帮助和辅助功能标签。
- [x] **Step 4: 运行测试确认通过**：执行 Step 2 的命令。
- [x] **Step 5: 运行完整测试**：`scripts/test.sh`。
- [ ] **Step 6: 提交**：

```bash
git add RoutinUsage/Views/UsagePopoverView.swift RoutinUsageTests/ProjectBootstrapTests.swift
git commit -m "feat: 标记已完成的 Routin 签到"
```
