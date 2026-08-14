# MyRoutin 原生界面重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 将菜单栏弹窗、设置窗口、首次引导和 Key 编辑统一为清晰、可扫描的 macOS 原生工具界面。

**架构：** 新增共享用量呈现组件，替换弹窗和设置页中的系统染色进度条。设置窗口从顶部标签改为 `NavigationSplitView`，账户页以可展开的账户概览替代长文本堆叠。业务接口、状态对象和所有持久化格式保持不变。

**技术栈：** Swift 5、SwiftUI、AppKit、XCTest、XcodeGen。

## 全局约束

- 保持 macOS 14 最低部署版本。
- 不新增第三方依赖、网络请求、权限或持久化字段。
- 保留 Key 排序、选择、编辑、删除、签到、更新与 Codex 分组检测行为。
- 所有新增可点击图标提供辅助功能标签和提示。

---

### 任务 1：共享用量呈现

**文件：**
- 新增：`RoutinUsage/Views/UsageMetricPresentation.swift`
- 修改：`RoutinUsage/Views/UsageRowView.swift`
- 修改：`RoutinUsageTests/UsageFormatterTests.swift`

- [ ] 写出用量颜色与 0 至 100 百分比限制的失败测试。
- [ ] 实现风险颜色、百分比限制和细胶囊进度条。
- [ ] 将菜单栏弹窗接入共享进度条。
- [ ] 运行相关测试确认通过。

### 任务 2：设置窗口与账户概览

**文件：**
- 修改：`RoutinUsage/Views/SettingsView.swift`
- 修改：`RoutinUsageTests/ProjectBootstrapTests.swift`

- [ ] 写出原生侧栏、账户概览与共享进度条的失败测试。
- [ ] 以 `NavigationSplitView` 替换顶部标签，并提升默认窗口尺寸。
- [ ] 以图形化账户概览和详情展开区重构 Key 行，保留排序与所有操作。
- [ ] 运行相关测试确认通过。

### 任务 3：弹窗和引导统一

**文件：**
- 修改：`RoutinUsage/Views/UsagePopoverView.swift`
- 修改：`RoutinUsage/Views/OnboardingView.swift`
- 修改：`RoutinUsage/Views/KeyEditorView.swift`
- 修改：`RoutinUsageTests/ProjectBootstrapTests.swift`

- [ ] 写出品牌引导与图标操作的失败测试。
- [ ] 收紧菜单栏弹窗的工具栏与页脚层级。
- [ ] 使用品牌图重构首次引导，优化 Key 编辑标题和按钮层级。
- [ ] 运行相关测试确认通过。

### 任务 4：验证与实际检查

**文件：**
- 修改：必要的回归测试文件。

- [ ] 运行 `scripts/test.sh`。
- [ ] 运行 Debug 构建并启动应用。
- [ ] 通过 Computer Use 检查设置窗口三个入口与账户概览的深色界面。
- [ ] 检查 `git diff --check` 与工作区状态。
