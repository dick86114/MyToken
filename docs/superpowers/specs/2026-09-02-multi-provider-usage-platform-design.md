# 多供应商本地用量统计平台设计

## 1. 背景与目标

当前 MyRoutin 的用量链路围绕 Routin 设计：单一 API 客户端、`plan-` Key、固定的 5 小时/周额度模型，以及 Routin 签到和 Codex 分组检测。

本次改造目标是在保持 macOS 菜单栏应用形态的前提下，将核心能力扩展为本地多供应商用量统计平台，首期支持：

- Routin：保留现有完整能力，并迁移到通用供应商接口。
- DeepSeek：余额、赠金余额、充值余额和可用状态。
- GLM / Z.ai Coding Plan：配额窗口、模型用量、工具/MCP 用量。
- 火山方舟个人 Agent Plan 和 Coding Plan：个人席位信息、套餐类型、额度、窗口用量和重置时间。

明确约束：

- 第一阶段只支持 macOS 本地使用，不引入服务端同步。
- 只支持手动录入 API Key、Access Key、Secret Key 等凭证，不做 OAuth 和网页登录授权。
- 同一供应商下的多个凭证完全独立，不跨凭证求和。
- 菜单栏最多选择并显示 4 个凭证的指标单元。
- 不把没有明确上限的余额伪装成百分比用量。

## 2. 非目标

- 不做团队协作、云端账号同步和跨设备同步。
- 不做企业版火山 Agent Plan 多席位查询。
- 不承诺 DeepSeek 的日/周精确消耗量，除非官方提供稳定的聚合用量接口。
- 不在首期引入动态插件、脚本沙箱或第三方扩展市场。

## 3. 总体架构

采用“通用核心 + 供应商适配器”结构：

```text
ProviderRegistry
  -> UsageProvider
       -> validate(credential)
       -> fetchUsage(credential, now)
  -> NormalizedUsageSnapshot
  -> UsageStore
  -> Cache / Alert / Menu Bar / Settings
```

通用核心只依赖协议和标准化模型，不判断供应商名称。供应商差异由适配器处理：认证、请求、限流、响应解析、能力声明和供应商扩展字段。

建议的核心协议：

```swift
protocol UsageProvider: Sendable {
    var descriptor: ProviderDescriptor { get }
    func validate(_ credential: ProviderCredential) async throws -> UsageSnapshot
    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot
}
```

## 4. 数据模型

### 4.1 供应商

```text
ProviderDescriptor
  id
  displayName
  shortCode
  iconName
  capabilities
  credentialSchemas
```

首期供应商 ID 固定为 `routin`、`deepseek`、`glm`、`volcengine`。`shortCode` 用于菜单栏竖向缩写，例如 `DS`、`GLM`、`VOL`、`ROU`。

### 4.2 凭证配置

将现有 `KeyConfiguration` 演进为通用 `CredentialConfiguration`：

```text
CredentialConfiguration
  id: UUID
  providerID: String
  name: String
  credentialKind: String
  metadata: [String: String]
  sortOrder: Int
  isEnabled: Bool
  secretReference: String
```

`metadata` 只保存展示和请求所需的非秘密配置，例如区域、计划类型提示、币种和余额预警值。首期秘密材料继续通过应用本地存储访问，不调用系统钥匙串。

推荐凭证类型：

- Routin：`bearerAPIKey`
- DeepSeek：`apiKey`
- GLM：`apiKey`
- 火山个人版：`accessKeyPair`，包含 AccessKey ID、SecretAccessKey 和可选区域

### 4.3 通用用量模型

不再把 `UsageSnapshot` 固定为 `fiveHour`、`weekly`、`token` 三个字段，改为指标数组：

```text
UsageSnapshot
  providerID
  credentialID
  accountLabel
  planLabel
  metrics: [UsageMetric]
  providerDetails
  fetchedAt
  status
  subscriptionStartAt
  subscriptionEndAt
```

```text
UsageMetric
  id
  label
  used: Decimal?
  limit: Decimal?
  remaining: Decimal?
  value: Decimal?
  unit: token / currency / request / boolean / text
  windowStart: Date?
  windowEnd: Date?
  presentation: progress / balance / status / value
```

展示规则：

- `progress`：有明确上限，显示已用百分比和剩余量。
- `balance`：只有余额，不显示百分比，用余额健康状态表达风险。
- `status`：只有可用性或连接状态。
- `value`：展示一个不适合进度条的数值。

供应商扩展字段采用类型化结构，而不是 `[String: Any]`，避免缓存和测试失去类型安全：

- Routin：分组倍率、Codex 当前分组。
- GLM：模型用量、工具/MCP 用量、配额窗口信息。
- 火山：席位类型、计划类型、重置规则。
- DeepSeek：余额明细、币种、可用状态。

## 5. 供应商适配器

### 5.1 Routin

将现有 `UsageAPIClient`、`UsageMapper` 拆成 `RoutinUsageProvider`，保留现有响应兼容、额度判断、分组倍率和缓存迁移逻辑。Routin 专属的签到、网页登录、Codex 分组探测移动到独立的 Routin 扩展服务，不进入通用 `UsageStore`。

### 5.2 DeepSeek

首期只建模余额类指标：余额、赠金余额、充值余额、可用状态和更新时间。适配器不生成虚假的 `used / limit / percent`。

余额健康状态：

- 绿色：余额大于预警阈值且账户可用。
- 黄色：余额低于用户设置的预警阈值。
- 红色：余额为 0、账户不可用或认证失败。
- 灰色：无缓存、请求失败或数据过期。

如果没有设置预警阈值，只根据余额是否为 0 和接口可用状态判断。

### 5.3 GLM

适配器负责查询配额窗口、模型用量和工具/MCP 用量，统一转换为 `progress` 或 `value` 指标。由于产品用量接口可能发生版本变化，必须：

- 对缺失字段采用可选解析，不因单个明细字段缺失而丢弃整个快照。
- 在解析失败时保留上一次成功快照并标记数据过期。
- 区分认证失败、限流、网络错误和响应结构变化。
- 不记录原始响应正文。

### 5.4 火山方舟个人 Agent/Coding Plan

统一由 `VolcenginePlanUsageProvider` 实现，通过返回信息识别 Agent Plan 或 Coding Plan。Action、Version、区域、签名和时间戳只在适配器内部处理。

支持的手动凭证字段：

- AccessKey ID
- SecretAccessKey
- 区域（提供默认值，也允许修改）

适配器必须覆盖个人席位查询、套餐类型、额度窗口、重置时间和认证错误。企业版席位列表不纳入首期模型。

## 6. 分组与界面

### 6.1 设置页

设置导航新增“供应商与凭证”：

```text
供应商与凭证
├── Routin       2 个凭证
├── DeepSeek     3 个凭证
├── GLM          1 个凭证
└── 火山方舟     2 个凭证
```

每个供应商内部独立排序。每个凭证支持启用/停用、编辑名称、替换密钥、立即刷新、删除和余额预警值配置。

新增凭证流程：

```text
选择供应商
→ 选择凭证类型
→ 输入显示名称和秘密字段
→ 适配器验证
→ 验证成功后写入应用本地存储和元数据
→ 保存首个快照
```

验证失败不覆盖旧凭证和旧缓存。

### 6.2 用量弹窗

弹窗按供应商分组，每组显示供应商图标、名称、凭证数量和连接状态。组内每行显示凭证名、计划类型、主指标、更新时间和错误状态。

不显示供应商合计用量。供应商标题上的状态只表示该组是否存在失败或过期凭证。

详情区按能力显示配额、余额、模型明细、工具明细、订阅信息和 Routin 专属信息。

### 6.3 菜单栏图标

菜单栏图标显示用户选中的 1～4 个凭证，每个凭证对应一个固定宽度的竖向单元：

```text
左侧：供应商缩写纵向排列
右侧：竖向胶囊指示器
```

规则：

- 进度型指标：胶囊从底部向上填充，表示真实使用率。
- 余额型指标：胶囊不表达百分比，只使用余额健康颜色和底部状态标记。
- 状态型指标：显示连接状态色。
- 默认最多 2 个，设置中允许 1～4 个。
- 超过 4 个不允许选择；停用或删除当前选中项时自动补位。
- 每个单元固定宽度和高度，不因供应商名称变化而抖动。
- 可访问性标签提供完整文本，例如“DeepSeek，余额 12.36 元，状态正常”。

选中项存储为凭证 UUID 数组，而不是单个供应商 ID。迁移旧版本时将原有当前 Key 转换成包含一个凭证的选择数组。

## 7. 缓存、刷新与告警

### 7.1 缓存

缓存键改为 `credentialID`，快照内同时保存 `providerID`。缓存文件增加 `schemaVersion`，支持旧版 Routin 快照迁移。

请求失败时保留最近一次成功快照，并在 UI 显示：

- 最后成功时间。
- 当前数据已过期。
- 最近一次错误类别。

### 7.2 刷新

保留现有 1、5、15、30 分钟全局刷新设置，并为适配器增加能力：

- 最小请求间隔。
- 是否支持并发刷新。
- 是否需要串行请求。

`UsageStore` 只负责调度凭证，不负责供应商请求细节。刷新任务按凭证并发执行，但同一凭证禁止重复请求。

### 7.3 告警

告警统一抽象为指标阈值：

- 进度型：按百分比阈值，例如 50% 和 80%。
- 余额型：按绝对金额阈值。
- 状态型：按不可用、认证失败或数据过期。

告警正文必须包含供应商名和凭证显示名，不包含任何秘密字段和完整响应。

## 8. 安全与数据迁移

当前 `LocalKeyStore` 使用 `UserDefaults` 保存本地凭证。由于上一轮实现曾短暂写入 Keychain，升级时提供一次性回迁：

1. 启动时读取旧 `planKey.<UUID>` 和遗留 Keychain 项。
2. 遗留 Keychain 项复制回 `UserDefaults` 本地存储，成功后删除对应 Keychain 项。
3. 迁移完成后写入一次性标记，后续启动不再访问 Keychain。
4. 回迁失败时保留原值并在下一次启动重试，不破坏现有 Routin 使用。

配置迁移：

- 旧 `KeyConfiguration` 映射为 `providerID = routin` 的 `CredentialConfiguration`。
- 保留原 UUID、名称、排序、启用状态和后缀元数据。
- 旧 `UsageSnapshot` 映射为 Routin 的通用指标数组。
- 旧的 `selectedKeyID` 映射为单元素 `selectedCredentialIDs`。
- 旧 Routin Codex 分组记录继续按原 UUID 关联。

## 9. 错误处理与可观测性

统一错误分类：

```text
invalidCredential
unauthorized
rateLimited
transport
timeout
invalidResponse
providerUnavailable
staleData
```

所有供应商请求日志只记录供应商 ID、凭证 UUID、错误分类、HTTP 状态、耗时和重试次数。禁止记录 API Key、AK/SK、Authorization、原始响应和账户敏感字段。

## 10. 测试策略

### 核心模型

- 指标单位、窗口、百分比和余额状态计算。
- 能力缺失时的动态展示条件。
- 多供应商、多凭证排序和选中项迁移。

### 供应商适配器

- 每家供应商至少覆盖成功、认证失败、限流、网络失败、结构变化和空数据。
- 使用脱敏 JSON fixture，不使用真实密钥。
- Routin 复用并扩展现有 Mapper/API 测试。

### 存储与迁移

- 遗留 Keychain 到 UserDefaults 的一次性回迁、重复启动和失败重试。
- 旧配置、旧缓存、旧 selectedKeyID 的兼容读取。
- 删除凭证时只删除自己的秘密和缓存。

### UI 与菜单栏

- 1～4 个选中凭证的布局快照。
- 供应商分组、凭证停用、删除、自动补位。
- DeepSeek 余额型指示器不出现百分比。
- 窄菜单栏下单元固定尺寸、不发生文字重叠。

## 11. 分阶段实施计划

### 阶段 0：接口和安全基础

- 建立 Provider Registry、通用凭证模型和能力模型。
- 增加 Keychain 存储及 Routin 明文迁移。
- 增加 schemaVersion 和旧缓存迁移。

### 阶段 1：Routin 通用化

- 将现有 API Client/Mapper 接入 `RoutinUsageProvider`。
- 保持现有 Routin UI、告警和刷新行为。
- 将签到、网页登录、Codex 分组检测隔离为 Routin 扩展。

### 阶段 2：DeepSeek

- 接入余额查询。
- 增加余额指标、余额阈值和余额型菜单栏单元。
- 补齐无用量上限时的说明和错误降级。

### 阶段 3：GLM

- 接入配额、模型和工具/MCP 明细。
- 增加结构变化保护、缓存保留和产品接口版本记录。

### 阶段 4：火山个人计划

- 接入个人 Agent Plan 和 Coding Plan。
- 增加 AK/SK、区域、签名、计划类型和窗口用量解析。

### 阶段 5：多选菜单栏与完整 UI

- 增加 1～4 个凭证选择器。
- 完成竖向菜单栏图标和多单元布局。
- 完成供应商分组弹窗、设置页和可访问性文本。

## 12. 验收标准

- 现有 Routin Key、缓存、通知和 Routin 专属功能不回归。
- 可以在一个供应商下保存并独立刷新多个凭证。
- 多个凭证不会被错误合计。
- 菜单栏可以选择 1～4 个凭证并稳定显示竖向单元。
- DeepSeek 不显示伪造的用量百分比，只显示余额健康状态和详情金额。
- 任意供应商请求失败时不清空最近一次成功数据。
- 新增凭证和更新凭证的明文秘密不再写入 UserDefaults。
- 所有供应商适配器均有脱敏 fixture 测试和错误路径测试。

## 13. 风险

- GLM Coding Plan 查询接口可能随产品版本变化，需要把解析器和 fixture 版本化。
- DeepSeek 余额接口无法替代精确历史用量统计，产品文案必须明确“余额监控”而非“用量统计”。
- 火山签名请求对区域、时间戳和字段要求敏感，需要在真实凭证验证阶段补齐 API fixture。
- Keychain 迁移需要覆盖升级、重复启动、用户拒绝访问和异常中断场景。
