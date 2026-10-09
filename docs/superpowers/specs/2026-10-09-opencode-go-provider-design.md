# OpenCode Go 供应商设计

## 目标

在 MyToken 中新增 OpenCode 供应商，用于查询 OpenCode Go 与 Go Plus 订阅的账户级 5 小时、周、月用量，以及续费/到期状态。该数据必须覆盖同一订阅在多台设备上的消耗，不使用本机 OpenCode session 统计。

## 数据来源与边界

- 数据端点：`GET https://opencode.ai/console/api/go/status`。
- 凭证：OpenCode Console service account API Key，使用 `Authorization: Bearer <key>`。
- 该端点当前属于 Console 内部 API，官方公开兼容 API 未承诺稳定性；实现必须隔离在 OpenCode adapter 内，响应结构变化要映射为可诊断的供应商错误。
- Go / Go Plus 的模型推理 API Key 通常不具备 Console 管理权限；如果用户输入推理 Key，应通过 401/403 错误提示创建 service account Key，而不是误判为网络问题。
- 不实现 OAuth Device Flow 作为第一期方案；若 service account Key 无法访问内部状态端点，后续再单独设计登录授权。

## 响应建模

接口核心字段映射如下：

- `product`: `go` 显示为 `OpenCode Go`，`go-plus` 显示为 `OpenCode Go Plus`。
- `access.startsAt` / `access.endsAt`: 订阅开始与续费/结束时间。
- `access.cancelAtPeriodEnd`: 为真时卡片文案使用“到期”，否则使用“续费”。
- `access.meters.fiveHour`: 5 小时滚动窗口。
- `access.meters.week`: 周窗口。
- `access.meters.month`: 月窗口。
- 每个_meter_包含 `startsAt`、`resetsAt`、`limitMicroCents`、`usedMicroCents`。
- `renewalPending`、`renewalAuthorizationRequired`、`renewalRetryAt`、`renewalStopReason`: 用于状态提示；第一期不自动处理支付。

金额字段为 micro cents 字符串或可解析数值，1 美元等于 `100_000_000` micro cents。剩余额度计算为 `limit - used`，不得为负数。

## 指标展示

OpenCode 供应商提供三个进度指标：

1. `fiveHour`：5 小时
2. `weekly`：周
3. `monthly`：月

每个指标展示已用、上限、剩余、百分比和重置时间。卡片头部展示订阅生命周期时间：正常续费显示“续费 yyyy-MM-dd HH:mm”，取消续订后显示“到期 yyyy-MM-dd HH:mm”。支付处理中或需要重新授权时，状态区展示对应提示，不覆盖三个用量指标。

## 凭证与迁移

- Provider ID 使用 `opencode`。
- Credential kind 使用 `bearerAPIKey`。
- 凭证编辑器只需要一个秘密字段：`OpenCode Console API Key`。
- 不新增 metadata 字段；固定 Console 基础地址，避免用户配置错误。
- macOS 与 Android 传输 schema 同步允许 `opencode`，旧备份继续可导入。

## 错误处理

- 400: 提示 OpenCode 返回无法处理的请求。
- 401: 提示 Key 无效或未登录 Console。
- 403: 提示 Key 没有 Console 状态权限，建议创建带读取权限的 service account Key。
- 404: 提示未找到订阅或接口路径已变化。
- 429: 请求过于频繁。
- 5xx: OpenCode 服务暂时不可用。
- 顶层状态为 `null` 或缺少 `access` / `meters`: 提示未找到有效 Go 订阅。
- 解码失败: 提示 OpenCode 接口结构可能已变化。

所有错误与日志只记录状态码、错误类型和脱敏消息，绝不输出 API Key 或 Authorization 头。

## 验收标准

- macOS 与 Android 都能添加 OpenCode 凭证并刷新账户级 Go / Go Plus 状态。
- 三个窗口的 used / limit / remaining / percent / reset time 与 fixture 一致。
- 到期或续费时间进入卡片头部，而非误用指标重置时间。
- macOS / Android 之间可传输 OpenCode 凭证与快照。
- 官网支持列表和模拟数据包含 OpenCode。
- 单元测试覆盖响应映射、错误映射、schema 同步和 UI 展示策略。
