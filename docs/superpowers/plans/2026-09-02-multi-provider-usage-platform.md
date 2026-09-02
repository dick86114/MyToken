# 多供应商本地用量平台实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 MyRoutin 改造成支持 Routin、DeepSeek、GLM 和火山方舟个人 Agent/Coding Plan 的本地多供应商用量统计工具。

**Architecture:** 采用通用核心 + 供应商适配器。凭证、刷新、缓存、告警和菜单栏展示由通用层负责；每个供应商实现独立的凭证 schema、认证、请求、解析和扩展字段。多个凭证始终独立，不跨凭证求和。

**Tech Stack:** Swift 5、SwiftUI、Observation、Foundation、URLSession、Security.framework Keychain、XCTest、XcodeGen、macOS 14+。

**Spec:** `docs/superpowers/specs/2026-09-02-multi-provider-usage-platform-design.md`

## Global Constraints

- 第一阶段只支持 macOS 本地使用，不引入服务端同步。
- 只支持手动录入 API Key、Access Key、Secret Key 等凭证，不做 OAuth 和网页登录授权。
- 同一供应商下的多个凭证完全独立，不跨凭证求和。
- 菜单栏最多选择并显示 4 个凭证的指标单元，默认最多 2 个。
- 没有明确上限的余额不得伪装成百分比用量。
- 新增供应商从第一天起只使用 Keychain；旧 Routin 明文密钥必须迁移。
- 所有请求日志不得包含密钥、Authorization、原始响应和账户敏感字段。
- 每个任务完成后运行针对性测试，并创建一个小提交。

## 文件地图

- Create `RoutinUsage/Providers/ProviderModels.swift`: 供应商描述、能力和凭证 schema。
- Create `RoutinUsage/Providers/UsageProvider.swift`: 适配器协议、统一错误和注册表。
- Create `RoutinUsage/Providers/*UsageProvider.swift`: 各供应商适配器。
- Modify `RoutinUsage/Models/KeyConfiguration.swift`: 演进为通用凭证配置并保留解码迁移。
- Modify `RoutinUsage/Security/LocalKeyStore.swift`: 增加 Keychain 实现和旧值迁移。
- Modify `RoutinUsage/Models/UsageSnapshot.swift`: 改为通用指标数组并兼容旧 Routin 缓存。
- Modify `RoutinUsage/Usage/UsageStore.swift`: 依赖 ProviderRegistry，按凭证调度刷新和告警。
- Modify `RoutinUsage/Persistence/UsageCache.swift`: 增加 schemaVersion 和 providerID。
- Modify `RoutinUsage/Views/SettingsView.swift`, `KeyEditorView.swift`: 供应商分组和动态凭证表单。
- Modify `RoutinUsage/Views/UsagePopoverView.swift`, `UsageRowView.swift`: 供应商分组、能力驱动的详情。
- Modify `RoutinUsage/App/StatusBarController.swift`, `MenuBarLabelView.swift`: 1～4 个竖向菜单栏指标单元。
- Modify `RoutinUsage/App/AppEnvironment.swift`: 注入 ProviderRegistry，隔离 Routin 专属服务。

### Task 1: 建立通用模型和安全迁移基线

**Files:**
- Create: `RoutinUsage/Providers/ProviderModels.swift`
- Create: `RoutinUsage/Providers/UsageProvider.swift`
- Modify: `RoutinUsage/Models/KeyConfiguration.swift`
- Modify: `RoutinUsage/Security/LocalKeyStore.swift`
- Test: `RoutinUsageTests/ProviderModelsTests.swift`
- Test: `RoutinUsageTests/KeychainMigrationTests.swift`

**Interfaces:**
- Produce `ProviderID`, `CredentialKind`, `ProviderCapability`, `ProviderDescriptor`, `CredentialConfiguration`, `ProviderCredential`。
- Produce `UsageProvider`, `UsageProviderError`, `ProviderRegistry`。
- Produce `KeychainSecretStore: LocalKeyStoring` and `KeychainMigration.migrate旧Routin密钥()`。
- `UsageProvider.validate(_:) async throws -> UsageSnapshot?` 用于新增/更新凭证验证；`fetchUsage(_:now:) async throws -> UsageSnapshot?` 用于周期刷新，允许供应商返回无订阅或无数据状态。

- [ ] **Step 1: Write failing tests**
  - 验证四个首期供应商 descriptor 的 ID、简称、凭证类型和能力集合。
  - 验证凭证配置编码/解码保留 UUID、供应商、名称、排序、启用状态。
  - 验证旧 `planKey.<UUID>` 可迁移到 Keychain，并在成功后删除旧 UserDefaults 值。
  - 验证迁移失败时旧值仍保留。
- [ ] **Step 2: Run tests to verify failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/ProviderModelsTests -only-testing:RoutinUsageTests/KeychainMigrationTests`
  - Expected: 新测试因类型和迁移入口不存在而失败。
- [ ] **Step 3: Implement minimal models and Keychain store**
  - 使用 `Security.SecItemAdd/SecItemCopyMatching/SecItemUpdate/SecItemDelete`。
  - Keychain service 固定为 `ai.routin.usage-monitor.credentials`，account 使用凭证 UUID。
  - 旧 `KeyConfiguration` 解码时默认 `providerID = routin`、`credentialKind = bearerAPIKey`。
- [ ] **Step 4: Run focused tests**
  - Expected: ProviderModelsTests 和 KeychainMigrationTests 全部通过。
- [ ] **Step 5: Commit**
  - `git add RoutinUsage/Providers RoutinUsage/Models/KeyConfiguration.swift RoutinUsage/Security/LocalKeyStore.swift RoutinUsageTests/ProviderModelsTests.swift RoutinUsageTests/KeychainMigrationTests.swift`
  - `git commit -m "feat: 建立供应商模型与密钥迁移基础"`

### Task 2: 通用快照、缓存和 Store 调度

**Files:**
- Modify: `RoutinUsage/Models/UsageSnapshot.swift`
- Modify: `RoutinUsage/Persistence/UsageCache.swift`
- Modify: `RoutinUsage/Usage/UsageStore.swift`
- Modify: `RoutinUsage/Notifications/AlertManager.swift`
- Test: `RoutinUsageTests/UsageSnapshotTests.swift`
- Test: `RoutinUsageTests/UsageCacheTests.swift`
- Test: `RoutinUsageTests/UsageStoreTests.swift`

**Interfaces:**
- Produce `UsageMetricPresentation`、`UsageMetric` 新构造器，并将通用快照统一命名为 `UsageSnapshot`。
- `UsageCaching` 增加按 credentialID 读写 schemaVersion 的行为，但保留现有协议调用形式。
- `UsageStore` 改为持有 `ProviderRegistry`，按 `CredentialConfiguration.providerID` 选择适配器。

- [ ] **Step 1: Write failing tests**
  - 测试 progress、balance、status、value 四种指标的编码/解码。
  - 测试旧 Routin 快照能转换为通用指标数组。
  - 测试一个凭证失败时保留该凭证旧缓存，不影响其他凭证成功刷新。
  - 测试同供应商两个凭证的快照不会聚合。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/UsageSnapshotTests -only-testing:RoutinUsageTests/UsageCacheTests -only-testing:RoutinUsageTests/UsageStoreTests`
- [ ] **Step 3: Implement normalized snapshot and cache envelope**
  - 缓存 envelope 字段为 `schemaVersion`、`providerID`、`credentialID`、`snapshot`。
  - 解码失败只清理损坏条目，不清理全部供应商缓存。
  - 错误状态统一映射为 `invalidCredential`、`unauthorized`、`rateLimited`、`transport`、`timeout`、`invalidResponse`、`providerUnavailable`、`staleData`。
- [ ] **Step 4: Implement provider-based Store scheduling**
  - 保留现有并发刷新、取消、过期、通知 generation 和 selected credential 行为。
  - 增加适配器的最小间隔和串行策略检查。
- [ ] **Step 5: Run tests and commit**
  - Expected: 旧 UsageStoreTests 及新增测试全部通过。
  - `git commit -m "refactor: 抽象通用用量快照缓存和刷新调度"`

### Task 3: Routin 适配器迁移和专属服务隔离

**Files:**
- Create: `RoutinUsage/Providers/RoutinUsageProvider.swift`
- Modify: `RoutinUsage/Networking/UsageAPIClient.swift`
- Modify: `RoutinUsage/Usage/UsageMapper.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Test: `RoutinUsageTests/RoutinUsageProviderTests.swift`
- Modify: `RoutinUsageTests/UsageAPIClientTests.swift`, `UsageMapperTests.swift`

**Interfaces:**
- `RoutinUsageProvider` 实现 `UsageProvider`，内部复用现有 DTO 和 Mapper；当接口返回 `null` 时返回 `nil` 快照。
- Routin 专属签到、网页登录、Codex 分组检测通过 `RoutinExtensions` 组合对象注入，不由通用 Store 调用。

- [ ] **Step 1: Write failing adapter compatibility tests**
  - 现有周期额度、Token Pack、分组倍率和 null 响应 fixture 必须保持同样快照结果。
  - 无效 Key、401、非 2xx、解析失败映射到统一错误。
- [ ] **Step 2: Run tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/RoutinUsageProviderTests -only-testing:RoutinUsageTests/UsageAPIClientTests -only-testing:RoutinUsageTests/UsageMapperTests`
- [ ] **Step 3: Implement adapter and wire registry**
  - 将 endpoint、Bearer Header、DTO 解码和 Mapper 调用收进 `RoutinUsageProvider`。
  - 保持旧 `UsageAPIClient` 作为兼容 facade，避免一次性改动测试面。
- [ ] **Step 4: Move Routin-only dependencies behind extension container**
  - `AppEnvironment` 只暴露通用 provider registry 和可选 `RoutinExtensions`。
  - 非 Routin 凭证不创建签到和 Codex 分组检测任务。
- [ ] **Step 5: Run full tests and commit**
  - `scripts/test.sh`
  - `git commit -m "refactor: 将 Routin 接入通用供应商协议"`

### Task 4: DeepSeek 余额适配器

**Files:**
- Create: `RoutinUsage/Providers/DeepSeekUsageProvider.swift`
- Modify: `RoutinUsage/Providers/ProviderModels.swift`
- Test: `RoutinUsageTests/DeepSeekUsageProviderTests.swift`
- Test fixture: `RoutinUsageTests/Fixtures/deepseek-balance-success.json`

**Interfaces:**
- `DeepSeekUsageProvider` 接受 `apiKey`，返回 balance、赠金、充值余额和 availability 指标。
- `providerDetails` 使用 `DeepSeekUsageDetails { currency, isAvailable }`。

- [ ] **Step 1: Write failing fixture tests**
  - 成功响应转换为 `presentation = .balance`。
  - 余额为 0、账户不可用、401、限流和结构缺失分别得到明确错误或状态。
  - 快照不得产生 `limit`、`used` 或百分比字段。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/DeepSeekUsageProviderTests`
- [ ] **Step 3: Implement URLSession client and parser**
  - 使用独立 request builder，Authorization 只存在内存中的 URLRequest。
  - 解析金额为 Decimal，保留货币单位。
- [ ] **Step 4: Implement balance health evaluator**
  - 无阈值按余额为 0/可用状态判断；有阈值按绝对金额判断黄/红状态。
- [ ] **Step 5: Run tests and commit**
  - `git commit -m "feat: 接入 DeepSeek 余额统计"`

### Task 5: GLM Coding Plan 适配器

**Files:**
- Create: `RoutinUsage/Providers/GLMUsageProvider.swift`
- Create: `RoutinUsage/Providers/GLMUsageModels.swift`
- Test: `RoutinUsageTests/GLMUsageProviderTests.swift`
- Test fixture: `RoutinUsageTests/Fixtures/glm-usage-success.json`

**Interfaces:**
- `GLMUsageProvider` 返回配额窗口、模型用量、工具/MCP 用量。
- `GLMUsageDetails` 使用类型化数组，不使用 `[String: Any]`。

- [ ] **Step 1: Write failing fixture tests**
  - 覆盖 quota limit、model usage、tool usage 三类响应。
  - 覆盖字段缺失、空数组、认证失败、限流、接口结构变化。
  - 验证缺失的明细字段不会丢弃其他可用指标。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/GLMUsageProviderTests`
- [ ] **Step 3: Implement endpoint/version handling**
  - 将产品接口路径集中在适配器常量中。
  - 对响应字段使用可选解析，成功解析的指标先返回。
- [ ] **Step 4: Implement stale fallback contract**
  - 解析失败返回统一 `invalidResponse`，由 Store 保留上次成功快照并标记过期。
- [ ] **Step 5: Run tests and commit**
  - `git commit -m "feat: 接入 GLM Coding Plan 用量"`

### Task 6: 火山方舟个人 Agent/Coding Plan 适配器

**Files:**
- Create: `RoutinUsage/Providers/VolcenginePlanUsageProvider.swift`
- Create: `RoutinUsage/Providers/VolcengineSigning.swift`
- Create: `RoutinUsage/Providers/VolcengineUsageModels.swift`
- Test: `RoutinUsageTests/VolcenginePlanUsageProviderTests.swift`
- Test: `RoutinUsageTests/VolcengineSigningTests.swift`
- Test fixture: `RoutinUsageTests/Fixtures/volcengine-seat-usage-success.json`

**Interfaces:**
- `VolcengineCredential` 包含 accessKeyID、secretAccessKey、region。
- `VolcengineRequestSigner.sign(method:url:headers:body:credential:)` 只返回签名后的 Header。
- `VolcenginePlanUsageProvider` 返回计划类型、席位信息、额度窗口和重置时间。

- [ ] **Step 1: Write failing signing and parser tests**
  - 固定时间、请求体和凭证下签名结果必须稳定。
  - Agent Plan、Coding Plan、认证失败、限流、时钟偏差和字段缺失均有 fixture。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/VolcengineSigningTests -only-testing:RoutinUsageTests/VolcenginePlanUsageProviderTests`
- [ ] **Step 3: Implement signer and request builder**
  - Action、Version、区域、时间戳和 payload hash 均在适配器内部生成。
  - 日志中只保留 request ID、HTTP 状态和耗时。
- [ ] **Step 4: Implement plan detection and normalized metrics**
  - Agent Plan/Coding Plan 映射到统一 `planLabel`，不创建两个供应商 ID。
- [ ] **Step 5: Run tests and commit**
  - `git commit -m "feat: 接入火山方舟个人计划用量"`

### Task 7: 通用凭证管理和供应商分组设置页

**Files:**
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Modify: `RoutinUsage/Views/KeyEditorView.swift`
- Modify: `RoutinUsage/Views/OnboardingView.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Test: `RoutinUsageTests/CredentialEditorValidationTests.swift`
- Test: `RoutinUsageTests/SettingsProviderGroupingTests.swift`

**Interfaces:**
- `CredentialEditorModel` 根据 `ProviderDescriptor.credentialSchemas` 生成动态表单。
- `AppEnvironment.addValidatedCredential` 和 `updateValidatedCredential` 替代只接受 Routin secret 的入口。

- [ ] **Step 1: Write failing UI/state tests**
  - 供应商分组、组内排序、启用/停用和删除只影响当前凭证。
  - Routin、DeepSeek、GLM、火山的字段校验不同。
  - 验证失败不保存新密钥、不覆盖旧快照。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/CredentialEditorValidationTests -only-testing:RoutinUsageTests/SettingsProviderGroupingTests`
- [ ] **Step 3: Implement grouped settings model**
  - 以 providerID 分组，sortOrder 在供应商内部计算。
  - 编辑表单先 validate，再写 Keychain 和配置。
- [ ] **Step 4: Preserve Routin-only controls**
  - 仅 Routin 凭证显示签到和 Codex 分组检测入口。
- [ ] **Step 5: Run tests and commit**
  - `git commit -m "feat: 增加供应商分组和通用凭证设置"`

### Task 8: 用量弹窗和多凭证详情

**Files:**
- Modify: `RoutinUsage/Views/UsagePopoverView.swift`
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/UsageMetricPresentation.swift`
- Create: `RoutinUsage/Views/ProviderGroupView.swift`
- Create: `RoutinUsage/Views/UsageMetricView.swift`
- Test: `RoutinUsageTests/ProviderGroupViewTests.swift`
- Test: `RoutinUsageTests/UsageMetricPresentationTests.swift`

**Interfaces:**
- `ProviderGroupView` 接收供应商 descriptor 和凭证状态列表，不接收合计用量。
- `UsageMetricView` 根据 `UsageMetric.presentation` 渲染 progress、balance、status 或 value。

- [ ] **Step 1: Write failing presentation tests**
  - progress 显示百分比和窗口；balance 不显示百分比；status 显示状态色。
  - 同供应商多凭证只显示独立行，不出现合计。
  - 失败、过期、刷新中状态优先级固定。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/ProviderGroupViewTests -only-testing:RoutinUsageTests/UsageMetricPresentationTests`
- [ ] **Step 3: Implement provider groups and capability-driven details**
  - 详情区只渲染适配器声明支持的指标区块。
  - DeepSeek 展示余额、赠金、充值余额和说明文案。
- [ ] **Step 4: Run tests and commit**
  - `git commit -m "feat: 增加供应商分组用量弹窗"`

### Task 9: 菜单栏 1～4 个竖向指标单元

**Files:**
- Modify: `RoutinUsage/App/StatusBarController.swift`
- Modify: `RoutinUsage/Views/MenuBarLabelView.swift`
- Modify: `RoutinUsage/Models/AppSettings.swift`
- Modify: `RoutinUsage/Views/SettingsView.swift`
- Test: `RoutinUsageTests/MenuBarLabelViewTests.swift`
- Test: `RoutinUsageTests/MenuBarSelectionTests.swift`

**Interfaces:**
- `AppSettings.selectedCredentialIDs: [UUID]`，写入 UserDefaults 并限制 1～4 个。
- `MenuBarIndicatorModel` 包含 shortCode、presentation、healthState、accessibilityLabel。
- `MenuBarLabelView` 固定单元尺寸，最多渲染四个 `VerticalUsageIndicator`。

- [ ] **Step 1: Write failing layout/state tests**
  - 1、2、3、4 个选择项均可编码、恢复和渲染。
  - 第 5 个选择被拒绝；停用/删除当前项后自动补位。
  - progress 从底部向上填充；balance 不显示百分比；失败和过期使用灰/红状态。
  - 无论缩写长度为 2 或 3，单元宽高保持固定。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/MenuBarLabelViewTests -only-testing:RoutinUsageTests/MenuBarSelectionTests`
- [ ] **Step 3: Implement vertical indicator rendering**
  - 每个单元左侧纵向排列供应商缩写，右侧使用竖向胶囊。
  - 余额型只展示健康色和底部状态标记，不伪造比例。
  - accessibilityLabel 提供供应商、凭证名称、金额/百分比和更新时间。
- [ ] **Step 4: Add settings selection UI**
  - 在供应商分组凭证行增加菜单栏显示开关和已选数量提示。
- [ ] **Step 5: Run tests and commit**
  - `git commit -m "feat: 支持菜单栏多凭证竖向用量指标"`

### Task 10: AppEnvironment 集成、通知和刷新策略

**Files:**
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsage/Scheduling/RefreshScheduler.swift`
- Modify: `RoutinUsage/Notifications/AlertManager.swift`
- Test: `RoutinUsageTests/AppEnvironmentTests.swift`
- Test: `RoutinUsageTests/RefreshSchedulerTests.swift`

**Interfaces:**
- `AppEnvironment.live()` 创建 descriptors、providers、registry、Keychain store 和迁移器。
- `AlertEvaluator` 接受 progress 百分比、balance 金额和 status 三种阈值输入。

- [ ] **Step 1: Write failing integration tests**
  - 启动时完成旧 Routin 密钥迁移，再恢复配置、缓存和菜单栏选择。
  - 自动刷新按凭证调用对应 provider。
  - Routin 专属 scheduler 不影响其他供应商。
  - DeepSeek 低余额和 GLM/火山高用量分别触发正确通知。
- [ ] **Step 2: Run focused tests and confirm failure**
  - Run: `xcodegen generate && xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/AppEnvironmentTests -only-testing:RoutinUsageTests/RefreshSchedulerTests`
- [ ] **Step 3: Wire live dependency graph**
  - 删除 `AppEnvironment` 对单一 `apiClient` 的硬依赖，改为 registry。
  - 保留 Noop provider 以支持单元测试和不可用环境。
- [ ] **Step 4: Run full test suite and commit**
  - `scripts/test.sh`
  - `git commit -m "refactor: 集成多供应商刷新告警和应用环境"`

### Task 11: 迁移验证、文档和发布前检查

**Files:**
- Modify: `README.md`
- Modify: `docs/首次运行说明.md`
- Modify: `project.yml`
- Modify: `.gitignore`
- Test: `RoutinUsageTests/MigrationRegressionTests.swift`

- [ ] **Step 1: Add migration regression tests**
  - 覆盖旧 Routin 配置、旧快照、旧 selectedKeyID、旧明文 key 和重复启动。
- [ ] **Step 2: Update user documentation**
  - 说明供应商分组、1～4 个菜单栏选择、DeepSeek 余额语义、Keychain 存储和失败降级。
  - 移除“只支持 plan Key”的表述，保留 Routin 专属能力说明。
- [ ] **Step 3: Verify project resources**
  - 在 `project.yml` 测试资源同步脚本中加入新 Swift 文件和 fixture 目录。
- [ ] **Step 4: Run verification commands**
  - `git diff --check`
  - `scripts/test.sh`
  - `xcodebuild -project RoutinUsage.xcodeproj -scheme RoutinUsage -configuration Debug build`
  - `scripts/build-dmg.sh`
- [ ] **Step 5: Commit final documentation and release checks**
  - `git commit -m "docs: 更新多供应商用量平台说明"`

## 实施顺序和检查点

1. Task 1 完成后确认 Keychain 迁移不会破坏现有 Routin Key。
2. Task 2 完成后确认旧缓存、Store 并发刷新和通知行为无回归。
3. Task 3 完成后确认 Routin 功能全部通过，再开始新增供应商。
4. Task 4～6 每接入一家供应商就单独完成 fixture、错误处理和适配器测试。
5. Task 7～9 完成后进行真实窄窗口检查，重点确认菜单栏图标不是弹窗布局、最多 4 个单元且无重叠。
6. Task 10～11 完成后才进行完整构建、DMG 和升级迁移验证。

## 需要真实凭证验证的范围

- GLM 产品用量接口的当前响应结构和个人计划权限。
- 火山方舟个人 Agent/Coding Plan 的签名字段、区域和返回字段。
- DeepSeek 余额接口金额字段和币种展示。

真实验证只用于开发环境，不把真实密钥、原始响应或账户标识写入仓库、fixture 或日志。
