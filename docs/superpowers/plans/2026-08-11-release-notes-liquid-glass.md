# MyRoutin 发布更新日志与液态玻璃 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MyRoutin 增加必填 Markdown 发布日志、每六小时更新检查、完整更新日志展示、明亮液态玻璃外观和更紧凑的菜单栏弹窗。

**Architecture:** GitHub Release 的 `body` 是更新日志唯一来源，经 `GitHubUpdateService` 映射为 `AppUpdate.notes`。独立 `UpdateCheckScheduler` 由 `AppEnvironment` 持有，避免与用量刷新耦合。`LiquidGlassSurface` 统一封装 macOS 26+ 原生玻璃和 macOS 14–25 Material 回退。

**Tech Stack:** Swift 5、SwiftUI、Observation、XCTest、GitHub Actions、XcodeGen。

## Global Constraints

- 最低部署版本保持 macOS 14.0；macOS 26+ 才调用原生液态玻璃 API。
- 不新增第三方依赖；代码、测试、注释均使用中文。
- 发布日志为必填 Markdown，标题固定 `MyRoutin v<version>`，禁用自动生成 Release Notes。
- 启动时检查一次更新，之后每 6 小时检查；停止环境后不得继续检查。
- 完整更新日志仅在设置页显示；菜单栏只显示发现更新与安装动作。
- 设置窗口的 Tab、分区、列表行、开关、Picker、按钮和进度条均接入玻璃外观，且保留原生交互与危险色。
- 别名与竖条必须拆分渲染：文字使用原生 `Text` 自动适配菜单栏深浅背景，竖条保留独立彩色非模板图像；不得采样壁纸像素决定文字颜色。

---

### Task 1: 手动 Release 必填 Markdown 更新日志

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:** `workflow_dispatch.inputs.release_notes` 为必填字符串；`action-gh-release` 的 `body` 取该输入并设置 `generate_release_notes: false`。

- [ ] **Step 1: 写失败测试**

```swift
func test发布工作流要求手动Markdown更新日志() throws {
    let workflow = try sourceText(at: ".github/workflows/release.yml")
    XCTAssertTrue(workflow.contains("release_notes:"))
    XCTAssertTrue(workflow.contains("body: ${{ inputs.release_notes }}"))
    XCTAssertTrue(workflow.contains("generate_release_notes: false"))
}
```

- [ ] **Step 2: 运行失败测试**：运行 `scripts/test.sh`；预期当前 `notes` 与自动说明设置使新测试失败。
- [ ] **Step 3: 写最小实现**：将 `notes` 替换为 `release_notes`（`required: true`），将 Release 参数改为 `body: ${{ inputs.release_notes }}` 和 `generate_release_notes: false`。
- [ ] **Step 4: 重跑 `scripts/test.sh` 确认通过。**
- [ ] **Step 5: 提交。** 提交 `.github/workflows/release.yml` 与 `RoutinUsageTests/ProjectBootstrapTests.swift`，提交信息：`feat: 发布时要求填写更新日志`。

### Task 2: 六小时更新检查调度

**Files:**
- Create: `RoutinUsage/Updates/UpdateCheckScheduler.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsageTests/AppLifecycleTests.swift`

**Interfaces:**
- `@MainActor protocol UpdateCheckScheduling: AnyObject { func start(onTick: @escaping @Sendable () -> Void); func stop() }`。
- `UpdateCheckScheduler(interval: TimeInterval = 21_600)` 使用可取消任务循环等待 interval 后执行回调。
- `AppEnvironment` 注入 `updateCheckScheduler`；`live()` 创建 `UpdateCheckScheduler()`；`start()` 启动立即检查和调度；`stop()` 停止调度。

- [ ] **Step 1: 写失败测试。** 在 `AppLifecycleTests` 新增 `LifecycleUpdateService` 和 `LifecycleUpdateSchedulerSpy`，覆盖以下行为：

```swift
func test启动立即检查更新并启动六小时调度() async throws {
    let context = try makeContext()
    let service = LifecycleUpdateService(result: .none)
    let scheduler = LifecycleUpdateSchedulerSpy()
    let environment = context.makeEnvironment(
        updateService: service,
        updateCheckScheduler: scheduler
    )
    await environment.start()
    await 等待条件 { await service.checkCount == 1 }
    XCTAssertEqual(scheduler.startCount, 1)
}
```

另加一例：`fireTick()` 后检查次数为 2，`stop()` 后 `stopCount == 1`；再加一例：已有 `.available` 时下一次后台检查抛 `UpdateServiceError.unavailable`，状态仍为 `.available`。

- [ ] **Step 2: 运行 `scripts/test.sh` 确认失败。**
- [ ] **Step 3: 写最小实现。** 任务循环捕获取消且取消后不回调；`checkForUpdates()` 的错误路径在当前状态为 `.available` 时保留已有可安装更新。
- [ ] **Step 4: 重跑 `scripts/test.sh` 确认通过。**
- [ ] **Step 5: 提交。** 提交调度器、环境和生命周期测试，提交信息：`feat: 定时检查应用更新`。

### Task 3: 设置页当前版本与完整更新日志

**Files:**
- Create: `RoutinUsage/Views/UpdateNotesView.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`
- Modify: `project.yml`

**Interfaces:** `UpdateNotesView(notes: String)` 空日志显示“此版本未提供更新日志”；有日志时 `AttributedString(markdown:)` 渲染，解析失败回退纯文本。设置页始终显示当前 `CFBundleShortVersionString`，发现新版时在标题下显示完整 `UpdateNotesView`。

- [ ] **Step 1: 写失败测试。** 新增两个 `ProjectBootstrapTests`：设置页包含“当前版本”、`UpdateNotesView(notes: update.notes)` 且不再含 `.lineLimit(3)`；`UpdateNotesView` 源码含 `AttributedString(markdown:` 与“此版本未提供更新日志”。
- [ ] **Step 2: 运行 `scripts/test.sh` 确认失败。**
- [ ] **Step 3: 写最小实现。** 使用以下逻辑，外加适当字体与辅助功能标签：

```swift
if notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
    Text("此版本未提供更新日志")
} else if let markdown = try? AttributedString(markdown: notes) {
    Text(markdown)
} else {
    Text(notes)
}
```

在 `project.yml` 同步 `UpdateNotesView.swift` 到测试资源，扩展 `ProjectBootstrapTests.sourceText(at:)` 映射。

- [ ] **Step 4: 重跑 `scripts/test.sh` 确认通过。**
- [ ] **Step 5: 提交。** 提交视图、设置页、工程规格与测试，提交信息：`feat: 在设置显示版本与更新日志`。

### Task 4: 明亮液态玻璃外观层

**Files:**
- Create: `RoutinUsage/Views/LiquidGlassSurface.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/OnboardingView.swift`
- Modify: `RoutinUsage/Views/MenuBarLabelView.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`
- Modify: `project.yml`

**Interfaces:** `View.liquidGlassSurface(cornerRadius: CGFloat = 16)` 与 `View.liquidGlassWindowBackground()`。macOS 26+ 分支使用当前 SDK 原生玻璃修饰符；macOS 14–25 使用 `.regularMaterial`、亮色描边、浅阴影。

- [ ] **Step 1: 写失败测试。** 静态测试断言设置页和弹窗调用 `liquidGlassWindowBackground`，辅助层含 `if #available(macOS 26.0` 与 `regularMaterial`。
- [ ] **Step 2: 运行 `scripts/test.sh` 确认失败。**
- [ ] **Step 3: 写最小实现。** 将统一玻璃表面应用于设置 Tab、分区、Key 详情、软件更新卡片、开关、Picker、按钮、进度条、弹窗工具栏/行和引导卡片。菜单栏状态项不绘制玻璃背景；将别名与竖条拆成原生 `Text(alias + " · ")` 和仅绘制彩色竖条的独立图像，让系统自动处理文字的深浅对比；删除操作保留红色语义。
- [ ] **Step 4: 重跑 `scripts/test.sh` 确认通过。**
- [ ] **Step 5: 提交。** 提交辅助层、四个界面、工程规格与测试，提交信息：`feat: 增加液态玻璃界面风格`。

### Task 5: 压缩菜单栏弹窗详情布局

**Files:**
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:** `details` 的 `TimelineView` 先输出包含两个倒计时的 `HStack`，中间 `Spacer(minLength: 8)`；倍率随后使用 `HStack { Spacer(); Text(UsageFormatter.groupMultiplierText(...)) }` 右对齐。

- [ ] **Step 1: 写失败测试。** 静态测试同时断言 `detailLine("5 小时剩余"`、`detailLine("周剩余"`、`Spacer(minLength: 8)` 和 `UsageFormatter.groupMultiplierText(groupMultipliers)`。
- [ ] **Step 2: 运行 `scripts/test.sh` 确认失败。**
- [ ] **Step 3: 写最小实现。** 两个倒计时从垂直堆叠改为同一行两端对齐；倍率移至进度条下方单独右对齐；每分钟刷新与 `—` 回退不变，Token 资源包不产生空白周期行。
- [ ] **Step 4: 重跑 `scripts/test.sh` 确认通过。**
- [ ] **Step 5: 提交。** 提交行视图与测试，提交信息：`refactor: 压缩菜单栏用量详情布局`。

### Task 6: 完整验证与交付

**Files:** 无新增业务文件。

- [ ] **Step 1: 运行完整回归。** 运行 `scripts/test.sh`，确认所有 XCTest 通过，脚本退出后测试偏好 plist 和 `myroutin-tests.*` 均为 0。
- [ ] **Step 2: 构建并校验 DMG。** 运行 `scripts/build-dmg.sh` 与 `hdiutil verify build/dist/MyRoutin.dmg`；确认正式 App 不包含 XCTest 插件。
- [ ] **Step 3: 检查、提交和推送。** 运行 `git diff --check`、`git status --short --branch` 和 `git push origin master`，确认 CI 开始构建 DMG。
