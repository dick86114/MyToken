# 时区测试确定性修复报告

## 问题与根因

GitHub Actions 的 UTC runner 中，`UsageFormatterTests` 将 `NSTimeZone.default` 改为 UTC+8 后，`TimeZone.current` 仍可能保持 UTC，导致同一绝对时间被格式化为 `06:00` 而非测试期望的 `14:00`。测试依赖进程全局时区状态，无法保证跨运行环境一致。

本机以 `TZ=UTC` 运行现有定向测试时仍会通过，说明本机 Foundation 会受 `NSTimeZone.default` 影响，不能稳定复现 CI 的 Foundation 差异；但失败 CI 的症状与生产代码读取 `.current`、测试修改全局默认时区之间的耦合一致。

## 修复

- 为 `UsageFormatter.resetTime`、`UsageFormatter.fullDateTime` 和同类完整窗口日期函数 `UsageFormatter.windowEndDescription` 增加 `timeZone: TimeZone = .current` 参数。
- 保持生产调用不变，默认仍使用当前时区。
- 测试改为显式传入 UTC+8，且删除 `NSTimeZone.default` 的读写；绝对倒计时测试也不再修改全局时区。

## TDD 证据

先改测试为传入 `timeZone`。在未实现参数时，定向测试编译失败，并报告两处 `extra argument 'timeZone' in call`，分别来自 `resetTime` 和 `fullDateTime`。随后实现最小 API 注入。

## 验证

2026-08-11 在 `TZ=UTC` 环境执行：

- `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test -only-testing:RoutinUsageTests/UsageFormatterTests CODE_SIGNING_ALLOWED=NO`
  - 29 个测试通过，0 个失败。
- `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -destination 'platform=macOS' test CODE_SIGNING_ALLOWED=NO`
  - 195 个测试通过，0 个失败。
