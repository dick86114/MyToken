# 签到完成图标设计

## 目标

让菜单栏弹窗中的 Routin 签到入口在当天已经确认签到后提供一眼可见的完成反馈。

## 交互规则

- `RoutinCheckInState.alreadyCheckedIn`：显示绿色实心 `checkmark.circle.fill`，悬停和辅助功能文案为“今天已签到”。
- `RoutinCheckInState.checkingIn` 或 `loggingIn`：显示固定尺寸加载指示器，按钮禁用。
- 其他状态：显示现有空心 `checkmark.circle`，可继续触发签到。
- `succeeded` 保持普通图标，因为抽奖已提交成功但页面结果不一定能等同于当天状态已稳定刷新；后续再次探测到 `alreadyCheckedIn` 时才切换完成样式。

## 范围与验证

- 仅修改 `UsagePopoverView` 的图标、颜色、提示和辅助功能标签，不改变签到服务、网页登录会话或设置页逻辑。
- 添加静态视图接入测试，覆盖绿色实心图标和“今天已签到”文案。
- 运行相关测试与完整测试脚本，确保不影响既有菜单栏交互。
