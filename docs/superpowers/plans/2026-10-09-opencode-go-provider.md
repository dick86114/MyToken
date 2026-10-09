# OpenCode Go Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 OpenCode Go / Go Plus 账户级用量、额度和续费/到期监控。

**Architecture:** 新增独立的 OpenCode provider adapter，通过 Console service account Bearer Key 调用内部 `GET /api/go/status`，并映射到现有 `UsageSnapshot` 指标模型。macOS、Android 和共享传输契约同步注册 `opencode`。官网只更新支持列表与模拟展示，不参与数据请求。

**Tech Stack:** Swift / SwiftUI / URLSession，Kotlin / Compose / HttpURLConnection，JSON Schema，React 网站。

**Spec:** `docs/superpowers/specs/2026-10-09-opencode-go-provider-design.md`

## Global Constraints

- Provider ID 固定为 `opencode`，Credential kind 固定为 `bearerAPIKey`。
- Console 状态端点固定为 `https://opencode.ai/console/api/go/status`。
- 金额单位为 micro cents，`1 USD = 100_000_000 micro cents`。
- 指标 ID 固定为 `fiveHour`、`weekly`、`monthly`。
- 卡片头部使用订阅生命周期时间；指标重置时间只属于各自指标。
- 内部接口响应变化必须转换为具体供应商错误，不允许崩溃或输出凭证。
- 注释、用户可见文案和文档使用中文。

## Review Focus

- 顶层 `null` 或缺少 `access.meters` 时应提示未找到有效订阅，而不是显示 0 用量。
- `cancelAtPeriodEnd` 为真时头部必须显示“到期”，否则显示“续费”。
- micro cents 解析必须保留精度，不能用二进制浮点直接换算。
- 403 需要提示 Key 缺少 Console 权限，区别于普通网络失败。
- 旧传输备份遇到未知 provider 仍应维持既有拒绝行为，新增 provider 后新旧备份分别可验证。

---

### Task 1: 共享契约与 macOS数据适配器

**Files:**
- Modify: `RoutinUsage/Providers/ProviderModels.swift`
- Modify: `RoutinUsage/Providers/UsageProvider.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Create: `RoutinUsage/Providers/OpenCodeUsageModels.swift`
- Create: `RoutinUsage/Providers/OpenCodeUsageProvider.swift`
- Modify: `shared/provider-contracts/provider-capabilities.json`
- Modify: `shared/transfer-schema/transfer-schema-v1.json`
- Modify: `shared/transfer-schema/wire-contract.md`
- Test: `RoutinUsageTests/OpenCodeUsageProviderTests.swift`
- Test: `RoutinUsageTests/ProviderModelsTests.swift`
- Test: `RoutinUsageTests/TransferSchemaTests.swift`

**Interfaces:**
- Produces: `ProviderID.opencode`
- Produces: `OpenCodeUsageProvider(session: URLSession = .shared, baseURL: URL = OpenCodeUsageProvider.defaultBaseURL)`
- Produces: `OpenCodeGoStatus` response model and internal micro-cent decimal conversion
- Consumes: existing `UsageSnapshot`, `NormalizedUsageMetric`, and `UsageProviderError`

- [ ] **Step 1: 写失败测试**

新增 `OpenCodeUsageProviderTests`：

```swift
func testFetchUsageMapsGoStatusToSnapshot() async throws
func testFetchUsageClampsRemainingAndMapsMonthlyWindow() async throws
func testFetchUsesBearerAuthorizationWithoutPrintingCredential() async throws
func testNullStatusThrowsProviderMessage() async throws
func testHTTPStatusMapsToDiagnosticErrors() async throws
```

断言三个指标、美元金额、剩余、百分比、窗口时间、订阅时间和请求头。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/OpenCodeUsageProviderTests`

Expected: FAIL，原因是 `OpenCodeUsageProvider` 与 `ProviderID.opencode` 不存在。

- [ ] **Step 3: 实现最小 macOS适配器**

先新增响应模型与 provider，再注册 descriptor 和 `AppEnvironment`。JSON 解码使用 `Decimal` 或整数除法换算，不使用 `Double` 承载 micro cents。

- [ ] **Step 4: 更新共享契约测试并确认失败**

在 Provider Models 与 Transfer Schema 测试中加入 `opencode`、short code、能力声明和 schema provider enum。

Run: targeted ProviderModelsTests / TransferSchemaTests

Expected: 先因 JSON 与 Swift enum 未同步出现失败。

- [ ] **Step 5: 同步共享契约并让 macOS目标测试通过**

更新 provider capabilities、transfer schema 和 wire contract；运行 Task 1 全部目标测试。

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add RoutinUsage RoutinUsageTests shared
git commit -m "feat: 接入 OpenCode Go 用量查询"
```

### Task 2: macOS凭证与展示策略

**Files:**
- Modify: `RoutinUsage/Views/CredentialEditorView.swift`
- Modify: `RoutinUsage/Views/UsageMetricPresentation.swift`
- Modify: `RoutinUsage/Views/NormalizedUsageMetricGrid.swift`
- Modify: `RoutinUsage/Views/UsageRowView.swift`
- Modify: `RoutinUsage/Views/Settings/CredentialDetailsView.swift`
- Test: `RoutinUsageTests/UsagePresentationPolicyTests.swift`
- Test: `RoutinUsageTests/ProviderRoutingTests.swift`

**Interfaces:**
- Consumes: `ProviderID.opencode`
- Consumes: metric IDs `fiveHour`、`weekly`、`monthly`
- Produces: OpenCode credential form and three-window display policy

- [ ] **Step 1: 写失败 UI策略测试**

新增断言：

```swift
func testOpenCodeCompactPolicyShowsAllThreeWindows()
func testOpenCodeMetricsUseDollarFormattingAndResetTime()
func testOpenCodeRoutesToUsageProvider()
```

- [ ] **Step 2: 运行确认失败**

Run: targeted usage presentation and routing tests.

Expected: FAIL，因为展示策略尚未包含 OpenCode。

- [ ] **Step 3: 实现凭证表单与展示策略**

OpenCode 只展示 API Key 字段；进度指标按 5 小时、周、月排序；订阅时间来自 `subscriptionEndAt`。

- [ ] **Step 4: 运行 macOS测试**

Run: `scripts/test.sh`

Expected: PASS，0 failures。

- [ ] **Step 5: Commit**

```bash
git add RoutinUsage RoutinUsageTests
git commit -m "feat: 完善 OpenCode macOS 展示"
```

### Task 3: Android领域模型与数据适配器

**Files:**
- Modify: `android/domain/src/main/kotlin/ai/routin/mytoken/domain/model/Credential.kt`
- Modify: `android/provider/src/main/kotlin/ai/routin/mytoken/provider/ProviderRegistry.kt`
- Create: `android/provider/src/main/kotlin/ai/routin/mytoken/provider/opencode/OpenCodeUsageProvider.kt`
- Test: `android/domain/src/test/kotlin/ai/routin/mytoken/domain/model/CredentialSecretCodecTest.kt`
- Test: `android/provider/src/test/kotlin/ai/routin/mytoken/provider/OpenCodeUsageProviderTest.kt`

**Interfaces:**
- Produces: `ProviderId.OpenCode`
- Produces: `OpenCodeUsageProvider(transport: ProviderHttpTransport, clock: Clock)`
- Consumes: shared transfer schema and Android `UsageSnapshot`

- [ ] **Step 1: 写失败测试**

测试覆盖：

```kotlin
fun `maps go status to three usage metrics`()
fun `maps go plus product and lifecycle dates`()
fun `rejects null status with provider message`()
fun `maps http errors to diagnostic exceptions`()
fun `credential codec accepts opencode bearer token`()
```

- [ ] **Step 2: 运行确认失败**

Run: `./android/gradlew -p android :domain:test :provider:test --tests "ai.routin.mytoken.provider.OpenCodeUsageProviderTest"`

Expected: FAIL，原因是 provider 和 enum 不存在。

- [ ] **Step 3: 实现领域模型与 provider**

使用 `BigDecimal` 解析 micro cents；Bearer Key 存入 `CredentialSecret.BearerToken`。

- [ ] **Step 4: 运行 Android provider/domain测试**

Run: `./android/gradlew -p android :domain:test :provider:test`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add android
git commit -m "feat: 接入 Android OpenCode 用量查询"
```

### Task 4: Android界面与迁移

**Files:**
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorValidation.kt`
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorViewModel.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/ProviderCatalog.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageMetricGrid.kt`
- Test: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorValidationTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/OpenCodeUsageLayoutTest.kt`

**Interfaces:**
- Consumes: `ProviderId.OpenCode`
- Produces: OpenCode API Key 表单校验、卡片主题、三窗口布局和生命周期文案

- [ ] **Step 1: 写失败测试**

覆盖 API Key 必填、OpenCode 显示名、三指标顺序、卡片头部使用 `subscriptionEndAt`。

- [ ] **Step 2: 运行确认失败**

Run: targeted feature tests.

Expected: FAIL。

- [ ] **Step 3: 实现界面逻辑**

不新增 provider 专属硬编码布局文件，优先复用现有 metric grid 和状态逻辑。

- [ ] **Step 4: 运行 Android全量测试**

Run: `./android/gradlew -p android test`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add android
git commit -m "feat: 完善 Android OpenCode 展示"
```

### Task 5: 官网、文档与最终验证

**Files:**
- Modify: `website/src/components/ProviderMatrixSection.tsx`
- Modify: `website/src/components/SettingsModal.tsx`
- Modify: `website/src/components/HeroSection.tsx`
- Modify: `website/src/components/Footer.tsx`
- Modify: `website/src/components/SecuritySection.tsx`
- Modify: `website/src/data/mockData.ts`
- Modify: `website/src/types.ts`
- Modify: `README.md`

**Interfaces:**
- Consumes: provider display name `OpenCode`
- Produces: provider short code `OC` and public support copy

- [ ] **Step 1: 更新官网类型与支持列表测试**

如网站无测试框架，以 `pnpm lint` 和 `pnpm build` 作为验证；类型中 provider union 必须包含 `OC`。

- [ ] **Step 2: 实现官网与 README**

说明需要 Console service account API Key，并明确监控的是账户级 Go / Go Plus 状态。

- [ ] **Step 3: 网站验证**

Run: `pnpm lint && pnpm build`

Expected: PASS。

- [ ] **Step 4: 全端验证**

Run:

```bash
scripts/test.sh
./android/gradlew -p android test
pnpm lint && pnpm build
```

Expected: 全部 PASS。

- [ ] **Step 5: Commit**

```bash
git add website README.md
git commit -m "docs: 更新 OpenCode 支持说明"
```

### Task 6: Release级人工验证与收尾

**Files:**
- No production files unless a verified defect requires TDD fix.

**Interfaces:**
- Consumes: all prior tasks.

- [ ] **Step 1: 构建 macOS Release DMG**

Run: `scripts/build-dmg.sh`

Expected: build succeeds。

- [ ] **Step 2: 用真实凭证做无泄漏探测**

由用户提供本机环境变量中的 service account Key，只输出 HTTP 状态和脱敏字段摘要；凭证不进入日志、fixture 或提交。

- [ ] **Step 3: 若真实接口拒绝 service key，停止发布路径并记录结论**

Expected: 不伪装为已支持；保留代码但明确认证限制，等待 OAuth Device Flow 设计。

- [ ] **Step 4: 最终 diff审查**

Run: `git diff --stat master...HEAD` and inspect changed files.

Expected: 只包含 OpenCode相关修改；`output/` 不进入提交。
