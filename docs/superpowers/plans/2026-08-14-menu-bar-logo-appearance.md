# 菜单栏 Logo 外观适配实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 让菜单栏 Logo 轮廓在浅色菜单栏背景使用黑色、深色菜单栏背景使用白色，同时保留用量进度的绿色、橙色和红色。

**架构：** `MenuBarLogoUsageIcon` 增加基于 `NSAppearance` 的轮廓颜色判定，状态栏按钮传入自身的 `effectiveAppearance`，因为它比应用全局外观更接近菜单栏实际显示环境。`StatusBarController` 监听应用有效外观变化并刷新按钮图片。

**技术栈：** Swift、AppKit、XCTest、XcodeGen。

## 任务

### 任务 1：外观颜色回归测试

**文件：**
- 新增：`RoutinUsageTests/MenuBarLabelViewTests.swift`

- [x] 测试 `.aqua` 返回黑色轮廓。
- [x] 测试 `.darkAqua` 返回白色轮廓。

### 任务 2：实现自适应轮廓并接入状态栏刷新

**文件：**
- 修改：`RoutinUsage/Views/MenuBarLabelView.swift`
- 修改：`RoutinUsage/App/StatusBarController.swift`

- [x] 新增外观到轮廓颜色的纯函数，并在 Logo 合成层使用该颜色。
- [x] 菜单栏 Logo 图片接收状态栏按钮的有效外观。
- [x] 监听 `NSApplication.effectiveAppearance` 的变化，变化后重绘状态栏按钮。
- [x] KVO 观察由 `NSKeyValueObservation` 持有，控制器释放时自动失效。

### 任务 3：验证

- [x] 运行 `scripts/test.sh`，315 项测试全部通过。
- [x] 检查 `git diff --check` 和工作区差异，确认只包含本需求文件。
