# 最终修复报告

## 交付概况

- 日期：2026-08-10
- 分支：`feature/routin-usage-menubar`
- 工作树：`/Users/dickies/Documents/workspaces/routin/.worktrees/routin-usage-menubar`
- 目标：完成通知安全、投递生命周期、Key 脱敏、登录启动同步、菜单栏交互与 DMG 交付脚本的最终加固。

## 修复内容

### 1. 通知百分比安全

- `AlertEvaluator` 在生成提醒前拒绝 NaN、正负无穷、负数及超出 `Int` 安全范围的百分比。
- 异常百分比不会消耗提醒资格，也不会请求通知授权或调用系统发送器。
- 通知文案格式化增加安全兜底，避免 `Int(percent.rounded())` 在极端输入下溢出或崩溃。

### 2. 通知投递状态与并发协调

- 新增可注入的 `AlertDeliveryCoordinator`，保留同一进程内跨 evaluator 的并发去重、接管与失败回滚能力。
- evaluator 只在内存中预留待投递窗口；只有发送成功并调用 `finishDelivery` 后，才把 delivered 状态写入 `UserDefaults`。
- 授权失败、发送失败或进程在投递前退出时不会留下已发送假状态，模拟重启后仍可重新提醒。
- 成功发送后的窗口继续跨重启去重。

### 3. UsageStore 与通知投递解耦

- `refreshAll`、新增 Key 及编辑验证提交不再等待通知授权或系统发送完成。
- 网络结果和 UI 状态先提交，通知由独立任务异步投递。
- 投递前再次校验 Key 是否存在、通知开关与刷新代际；旧代际提醒、已删除 Key 的延迟提醒均被丢弃。
- 保留 Key 删除清理、刷新代际隔离和通知状态回滚行为。

### 4. 安全名称与 Key 元数据脱敏

- 新增 `KeyCredentialPolicy`，统一显示名称、Key 前缀、最短 payload 与安全后缀规则。
- 显示名称为空或以 `plan-` 开头时返回中文错误；`plan-` 后 payload 少于 4 位时拒绝保存。
- 不足 4 位的遗留后缀在设置页统一显示为 `plan-••••`，不回显任何旧内容。
- 菜单栏、用量列表、设置页和通知文案统一使用安全 `displayName`；遗留的 plan secret 名称不会进入通知正文。

### 5. 登录启动状态同步

- 新增 `LoginItemSettingSynchronizer`。
- 应用启动、设置窗口出现及应用回到前台时，均以系统实际 `isEnabled` 状态刷新设置。
- 系统返回 `requiresApproval` 或登录项被外部修改时，开关不会错误显示为开启。

### 6. 菜单栏与产品文案

- `MenuBarExtra` 增加 `.menuBarExtraStyle(.window)`，承载可交互的总览面板。
- 设置页和首次引导统一使用“5 小时”文案。

### 7. DMG 交付脚本安全

- `scripts/build-dmg.sh` 基于 `BASH_SOURCE` 解析仓库根目录，可从仓库外调用。
- `build` 为符号链接时直接拒绝清理；`dist`、`DerivedData`、`DMGStaging` 的规范路径必须位于仓库内。
- 增加 `ROUTIN_DMG_DRY_RUN=1`，可在不清理和不构建的情况下检查目标路径。
- DMG 同时包含 `Routin Usage.app` 与 `首次运行说明.md`。
- 新增 `DeliveryScriptTests`，覆盖仓库外调用与外指符号链接哨兵保护。

## TDD：RED / GREEN 记录

所有修复均先补充失败测试，再实现最小修复并运行回归测试。

| 范围 | RED：实现前的失败表现 | GREEN：实现后的覆盖 |
| --- | --- | --- |
| 异常百分比 | 非有限或越界百分比仍进入通知授权/发送路径，格式化存在整数转换风险 | `test异常百分比不会请求授权发送通知或消耗提醒资格` 通过 |
| 投递持久化 | evaluator 在真正发送前就持久化窗口，未完成投递在模拟重启后被错误去重 | `test未完成投递在进程重启后恢复通知资格`、`test成功投递后在进程重启仍保持去重` 通过 |
| 通知解耦 | 授权或发送挂起会阻塞刷新结果、新增 Key 提交 | `test刷新全部提交状态后不等待通知授权完成`、`test新增Key落库发布后不等待通知授权完成`、`test通知发送挂起时其他Key仍及时发布刷新结果` 通过 |
| 刷新代际 | 旧刷新在授权完成后仍可能发送已过时提醒 | `test旧代际通知授权完成时不会发送已过时高用量提醒` 通过 |
| Key 安全 | plan secret 可被当作名称保存，短 payload/短旧后缀会泄露元数据 | Key 编辑、仓储与设置脱敏测试通过 |
| 登录启动 | 设置只反映应用期望值，不能同步系统外部变更和待批准状态 | 启动同步、外部变更及 `requiresApproval` 测试通过 |
| 菜单栏与文案 | 菜单栏未声明 window 样式，产品文案仍有“五小时” | `ProjectBootstrapTests` 对 window 样式和“5 小时”文案断言通过 |
| DMG 脚本 | 依赖调用者当前目录，外指 `build` 符号链接存在误清理风险 | 两个 `DeliveryScriptTests` 均通过 |

刷新代际还做了突变确认：曾临时把 `shouldDeliverNotification` 门禁改为恒真，`test旧代际通知授权完成时不会发送已过时高用量提醒` 随即失败；恢复为 `return await self.shouldDeliverNotification(work)` 后测试重新通过。该突变未保留在交付代码中。

## 最终验证

### 完整测试

执行：

```bash
xcodegen generate
xcodebuild \
  -project RoutinUsage.xcodeproj \
  -scheme RoutinUsage \
  -destination 'platform=macOS' \
  -derivedDataPath build/DerivedData \
  test CODE_SIGNING_ALLOWED=NO
```

本轮结果：

```text
Executed 154 tests, with 0 failures (0 unexpected)
** TEST SUCCEEDED **
```

完整日志：`/tmp/routin-final-fix-full-test-4.log`

### 脚本语法与仓库外 dry-run

执行：

```bash
bash -n scripts/build-dmg.sh
cd /tmp
ROUTIN_DMG_DRY_RUN=1 \
  /Users/dickies/Documents/workspaces/routin/.worktrees/routin-usage-menubar/scripts/build-dmg.sh
```

结果：脚本语法检查通过，并从仓库外正确解析到当前工作树；三个待清理目录均位于该工作树的 `build` 目录内。

### Release 与 DMG

执行：

```bash
bash scripts/build-dmg.sh
hdiutil verify build/dist/Routin-Usage.dmg
```

本轮结果：

```text
** BUILD SUCCEEDED **
created: .../build/dist/Routin-Usage.dmg
hdiutil: verify: checksum of ".../build/dist/Routin-Usage.dmg" is VALID
```

只读挂载后确认包含：

```text
Routin Usage.app
首次运行说明.md
```

构建日志：`/tmp/routin-final-fix-dmg-build-2.log`

## UI 烟测限制

Release 应用曾成功启动并保持运行，说明可执行文件能够进入菜单栏应用生命周期；随后已安全结束该本地烟测进程。

当前执行环境是无可访问窗口的 headless 状态。Computer Use 对 LSUIElement 菜单栏应用调用 `sky.get_app_state` 时出现超时或 `noWindowsAvailable`，因此本报告不把菜单点击、弹窗交互或设置窗口切换描述为已完成的人工 UI 验证。对应行为由自动化测试、Release 构建与应用进程启动证据覆盖；如需点击式验收，应在有图形桌面会话的 macOS 环境进行。
