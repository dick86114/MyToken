# 最终审查修复报告

日期：2026-08-11

## 修复范围

本次仅处理最终审查列出的五项问题：

1. 发布工作流显式选择 Xcode 26，并以可执行脚本校验 Xcode 与 macOS SDK 主版本。
2. 应用启动后立即开始首次更新检查和六小时调度，不再依赖首次用量刷新或通知授权完成。
3. `AppEnvironment` 持有实际更新检查任务；`stop()` 与环境释放会取消任务，并以代际标识阻止取消后返回的过期结果提交状态。
4. `UsageRowView` 在同一个 `TimelineView` 时间上下文中生成视觉倒计时和 VoiceOver 标签；周期套餐朗读两个倒计时与倍率，Token 资源包只朗读倍率。
5. `UpdateNotesView` 的辅助功能标签包含 Markdown 转换后的可读正文；空正文朗读“此版本未提供更新日志”。

## 实现摘要

### Xcode 26 发布环境

- `.github/workflows/release.yml` 为发布 Job 设置：
  - `DEVELOPER_DIR=/Applications/Xcode_26.3.app/Contents/Developer`
- 新增 `scripts/verify-xcode-26.sh`：
  - 执行 `xcodebuild -version`，拒绝非 Xcode 26。
  - 执行 `xcrun --sdk macosx --show-sdk-version`，拒绝非 macOS 26 SDK。
- `scripts/build-dmg.sh` 在生成工程和构建前执行上述校验。
- 新增脚本行为测试，使用假的 `xcodebuild`/`xcrun` 验证接受 Xcode 26、拒绝 Xcode 16。

### 更新检查生命周期

- `start()` 在首次 `await store.refreshAll()` 之前启动六小时调度并创建首次检查任务。
- 所有首次、周期和手动检查都汇聚到 `updateCheckTask`，避免未保存的实际网络任务。
- `stop()` 取消调度器和活动检查任务，并恢复检查前状态。
- `deinit` 取消活动检查任务。
- `updateCheckGeneration` 使忽略取消、延迟返回的旧请求无法提交 `.available`、`.idle` 或 `.failed` 状态。
- 新增回归测试覆盖：
  - 首次用量刷新挂起时，更新检查和调度已启动。
  - 停止时取消首次检查并拒绝过期成功结果。
  - 周期 tick 发出后停止会取消正在执行的检查。
  - 环境释放会取消正在执行的检查。

### VoiceOver

- `UsageRowAccessibility.label` 新增当前时间参数，并按套餐类型组合倒计时与倍率。
- `UsageRowView` 的外层 `TimelineView` 同时驱动视觉文本和辅助功能标签每分钟更新。
- `UpdateNotesAccessibility.label` 将每行 Markdown 转为无标记可读文本并保留段落分隔；空白正文返回明确空状态。

## TDD 记录

先添加回归测试并观察失败：

- `UpdateNotesAccessibility` 尚不存在，测试编译失败。
- 原有用量行标签缺少两个倒计时与倍率，行为断言失败。
- 首次用量刷新挂起时，旧实现的更新检查计数和调度启动计数仍为 0。
- 旧实现停止或释放环境后，挂起更新检查未收到取消。
- 发布工作流缺少 Xcode 26 选择与可执行校验脚本。

完成最小实现后，针对性测试结果：

```text
Executed 96 tests, with 0 failures
** TEST SUCCEEDED **
```

## 完整验证

本机工具链：

```text
Xcode 26.6
Build version 17F113
macOS SDK 26.5
```

完整测试：

```text
$ scripts/test.sh
Executed 228 tests, with 0 failures
** TEST SUCCEEDED **
```

DMG 构建：

```text
$ scripts/build-dmg.sh
已验证 Xcode 26.6，macOS SDK 26.5。
** BUILD SUCCEEDED **
created: build/dist/MyRoutin.dmg
```

镜像校验：

```text
$ hdiutil verify build/dist/MyRoutin.dmg
hdiutil: verify: checksum of "build/dist/MyRoutin.dmg" is VALID
```

产物：`build/dist/MyRoutin.dmg`，大小约 6.2 MB。
