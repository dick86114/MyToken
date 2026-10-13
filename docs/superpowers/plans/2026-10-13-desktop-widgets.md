# 桌面小组件重做实施计划

> **执行状态（2026-10-13）：** Task 1-10 已完成；Task 11 的自动化测试与 Release 构建已完成。真实 macOS 桌面拖放截图和 Android 真机截图因缺少签名产物/设备会话未执行，详见 `docs/superpowers/verification/2026-10-13-desktop-widgets-release.md`。

> **给代理执行者：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 实现 macOS 与 Android 的 MyToken 总览/单凭证桌面小组件，支持四种视觉风格、按套餐动态选指标、共享安全快照和真实 Release 验收。

**架构：** 主应用负责凭证访问与供应商刷新；macOS WidgetKit extension 只读 App Group 中不含密钥的快照，Android Glance 只读 Room 缓存与本地组件配置。两端各自实现同一套指标排序与展示契约，用共享 fixture 保证行为一致。

**技术栈：** Swift 5 / SwiftUI / WidgetKit / AppIntents / App Group；Kotlin / Compose / Glance / Room / WorkManager；XcodeGen 与 Gradle。

**设计文档：** [2026-10-13-desktop-widgets-design.md](../specs/2026-10-13-desktop-widgets-design.md)

## 全局约束

- 不在日志、测试夹具、快照或提交内容中暴露 API Key、Cookie、Token、Key 后缀。
- macOS 部署目标为 14.0，WidgetKit extension 不做供应商网络请求。
- Android `minSdk = 29`、`targetSdk = 34`；尺寸判断必须使用小组件实际可用宽高，不按机型分支。
- 视觉风格固定为：`自动 / 聚焦环 / 扫描带 / 信号格`；默认 `自动`。
- 抽象尺寸固定为：紧凑、标准、扩展；单凭证指标数量分别不超过 2、4、6。
- 组件内标题、指标名、数值和时间必须单行显示；标题与指标名空间不足时省略，不得换行、重叠或溢出。
- 次要指标自动选择；用户只能固定单凭证组件的主指标。
- macOS 测试命令：`scripts/test.sh`；Android 测试命令：`./android/gradlew -p android test`。
- UI 修复必须通过 Release 构建和真实桌面/设备截图验证，不能只以编译通过作为完成标准。
- 执行时从 `master` 创建 `codex/desktop-widgets` 分支；每个任务独立提交。

## 审查重点

1. **长名称与大数字**：长凭证名、长套餐名、完整分组 Token、多行时间文案不得导致换行或溢出；由 Task 1、8、10 的布局策略与布局测试覆盖。
2. **固定指标缺失**：用户固定的主指标不在本次快照中时必须显示“该指标暂不可用”，并保留其它真实指标；由 Task 1、6 的排序测试覆盖。
3. **旧 Android 小组件迁移**：现有 `credential_<widgetId>` 选择不能丢失，旧组件必须无缝变成新的单凭证组件；由 Task 6 的迁移测试覆盖。
4. **并发重复请求**：组件手动刷新、周期刷新、前台刷新并发时，同一凭证只能发生一次上游请求；由 Task 7 的 in-flight 合并测试覆盖。
5. **共享快照损坏或泄露**：macOS App Group 读写出错要降级到旧快照，写入内容不得含凭证材料；由 Task 2 的存储与安全测试覆盖。

---

### Task 1: 建立跨端展示契约与 Swift 排序器

**Files:**

- Create: `shared/widget-contracts/widget-cases.json`
- Create: `RoutinUsage/WidgetShared/WidgetSnapshotModels.swift`
- Create: `RoutinUsage/WidgetShared/WidgetMetricRanker.swift`
- Create: `RoutinUsage/WidgetShared/WidgetPresentationBuilder.swift`
- Test: `RoutinUsageTests/WidgetMetricRankerTests.swift`
- Test: `RoutinUsageTests/WidgetPresentationBuilderTests.swift`
- Modify: `project.yml`

**Interfaces:**

- Produces Swift 类型：`WidgetSnapshotBundle`、`WidgetSnapshotCredential`、`WidgetSnapshotMetric`、`WidgetDisplayStyle`、`WidgetSizeKind`、`WidgetPresentation`、`WidgetMetricPresentation`、`WidgetMetricSelection`。
- Produces Swift 入口：
  - `WidgetMetricRanker.rank(providerID: String, metrics: [WidgetSnapshotMetric], fixedMetricID: String?, capacity: Int) -> WidgetMetricSelection`
  - `WidgetPresentationBuilder.makeCredentialPresentation(credential: WidgetSnapshotCredential, style: WidgetDisplayStyle, size: WidgetSizeKind) -> WidgetPresentation`
  - `WidgetPresentationBuilder.makeOverviewPresentation(credentials: [WidgetSnapshotCredential], style: WidgetDisplayStyle, size: WidgetSizeKind) -> WidgetPresentation`
- Produces 共享 fixture：`shared/widget-contracts/widget-cases.json`，Task 6 复用。

- [ ] **Step 1: 写失败测试**

新增测试：

```swift
func test_ranker_preserves_provider_metric_priority()
func test_ranker_reports_missing_fixed_metric_without_dropping_other_metrics()
func test_builder_limits_metrics_by_size_kind()
func test_builder_resolves_auto_style_by_size_and_component_kind()
```

断言必须包含：

- Command Code 顺序：`five-hour`、`weekly`、`credit-progress`、`request-count`、`purchased-remaining`、`free-remaining`。
- DeepSeek 主指标为 `balance`，不得出现周期订阅模板。
- NewAPI 的 `quota-progress` 优先，Token 指标与费用指标语义去重。
- Xiaomi 保留原始指标顺序，输出 `3,323,413` 这类完整分组数字。
- 紧凑、标准、扩展容量分别为 2、4、6。
- 固定 `five-hour` 时 `fixedMetricUnavailable = false`；固定不存在的 `not-present` 时 `fixedMetricUnavailable = true` 且其它指标仍被返回。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/WidgetMetricRankerTests`

Expected: FAIL，原因是新类型和入口不存在。

- [ ] **Step 3: 实现契约模型与排序器**

排序策略：

1. 固定指标存在时优先。
2. 固定指标不存在时保留 `fixedMetricUnavailable` 标记。
3. 未固定时使用供应商 ID 优先表。
4. 未知供应商回退到快照原始顺序。
5. 次要指标按语义去重。

`widget-cases.json` 至少包含 Routin、DeepSeek、GLM、Volcengine、NewAPI、Command Code、Xiaomi MiMo、OpenCode Go。

- [ ] **Step 4: 将 fixture 加入测试资源**

修改 `project.yml` 的 `RoutinUsageTests` 资源列表，加入 `shared/widget-contracts`，执行 `xcodegen generate`。

- [ ] **Step 5: 运行测试确认通过**

Run: `scripts/test.sh`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add shared/widget-contracts/widget-cases.json RoutinUsage/WidgetShared RoutinUsageTests/WidgetMetricRankerTests.swift RoutinUsageTests/WidgetPresentationBuilderTests.swift project.yml RoutinUsage.xcodeproj
git commit -m "feat: 建立桌面小组件展示契约"
```

### Task 2: 实现 macOS 安全快照桥

**Files:**

- Create: `RoutinUsage/WidgetShared/WidgetSnapshotStore.swift`
- Create: `RoutinUsage/WidgetShared/WidgetSnapshotSynchronizer.swift`
- Create: `RoutinUsage/WidgetShared/WidgetReloading.swift`
- Modify: `RoutinUsage/Usage/UsageStore.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Test: `RoutinUsageTests/WidgetSnapshotStoreTests.swift`
- Test: `RoutinUsageTests/UsageStoreWidgetSyncTests.swift`

**Interfaces:**

- Consumes: Task 1 的 `WidgetSnapshotBundle`。
- Produces:
  - `WidgetSnapshotStore(directory: URL, filename: String = "widget-snapshot-v1.json")`
  - `WidgetSnapshotStore.load() throws -> WidgetSnapshotBundle?`
  - `WidgetSnapshotStore.save(_ bundle: WidgetSnapshotBundle) throws`
  - `@MainActor WidgetSnapshotSynchronizer(store: WidgetSnapshotStore, reloader: any WidgetReloading)`
  - `WidgetSnapshotSynchronizer.publish(states: [UUID: KeyUsageState], orderedKeyIDs: [UUID])`
  - `protocol WidgetReloading: Sendable { func reloadAllTimelines() }`
- `UsageStore` 新增可选依赖：`widgetPublisher: (@MainActor ([UUID: KeyUsageState], [UUID]) -> Void)?`。

- [ ] **Step 1: 写失败测试**

测试：

```swift
func test_snapshot_store_round_trips_and_replaces_atomically()
func test_snapshot_store_ignores_corrupt_file_and_keeps_previous_snapshot_available()
func test_usage_store_publishes_after_refresh_and_configuration_changes()
func test_widget_snapshot_does_not_contain_credential_material()
```

断言：

- 保存后再读取，`WidgetSnapshotBundle` 相等。
- 正式文件损坏时能尝试 `.previous`，不产生致命错误。
- 批量刷新、单凭证刷新、启用/停用、删除后都会调用 publisher。
- 序列化结果不包含测试 secret `sk-test-secret-value`、不包含 `planKey.`、不包含 `keySuffix` 字段。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/WidgetSnapshotStoreTests -only-testing:RoutinUsageTests/UsageStoreWidgetSyncTests`

Expected: FAIL，原因是存储与同步器不存在。

- [ ] **Step 3: 实现快照存储**

要求：

- 使用指定目录，不依赖 `UserDefaults` 保存快照。
- 保存到临时文件后原子替换正式文件。
- 替换前保留 `.previous` 文件。
- 读取损坏的正式文件时尝试 `.previous`。
- 文件名固定为 `widget-snapshot-v1.json`。

- [ ] **Step 4: 接入 `UsageStore`**

在批量刷新完成、单凭证刷新完成、`applyValidatedSnapshot`、启用/停用、删除、`reloadConfigurations` 后调用统一私有 `publishWidgetSnapshot()`。发布失败只记录事件名 `widget_snapshot_write_failed`，不得记录凭证值。

- [ ] **Step 5: 接入 live 环境**

在 `AppEnvironment.live()`：

- App Group ID 使用 `cc.idickies.mytoken.widgets`。
- 通过 `FileManager.containerURL(forSecurityApplicationGroupIdentifier:)` 取共享目录。
- 注入 `WidgetCenterReloader`，内部调用 `WidgetCenter.shared.reloadAllTimelines()`。
- 共享目录不可用时记录 `widget_group_unavailable`，主应用刷新不受影响。

- [ ] **Step 6: 运行测试确认通过**

Run: `scripts/test.sh`

Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add RoutinUsage/WidgetShared RoutinUsage/Usage/UsageStore.swift RoutinUsage/App/AppEnvironment.swift RoutinUsageTests/WidgetSnapshotStoreTests.swift RoutinUsageTests/UsageStoreWidgetSyncTests.swift
git commit -m "feat: 桥接 macOS 小组件安全快照"
```

### Task 3: 新增 macOS WidgetKit 目标与单凭证组件

**Files:**

- Create: `RoutinUsageWidget/RoutinUsageWidgetBundle.swift`
- Create: `RoutinUsageWidget/MyTokenCredentialWidget.swift`
- Create: `RoutinUsageWidget/Views/WidgetGraphicViews.swift`
- Create: `RoutinUsageWidget/WidgetSnapshotProvider.swift`
- Create: `RoutinUsage/RoutinUsage.entitlements`
- Create: `RoutinUsageWidget/RoutinUsageWidget.entitlements`
- Modify: `project.yml`
- Modify: `RoutinUsage/App/RoutinUsageApp.swift`
- Modify: `RoutinUsage/App/AppEnvironment.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`
- Test: `RoutinUsageTests/WidgetDeepLinkTests.swift`

**Interfaces:**

- Consumes: Task 1、2。
- Produces:
  - Widget target：`RoutinUsageWidget`
  - Bundle ID：`cc.idickies.mytoken.widget`
  - App Group：`cc.idickies.mytoken.widgets`
  - URL scheme：`mytoken`
  - deep link：`mytoken://credential/<UUID>?refresh=1`
  - `AppEnvironment.openCredentialDeepLink(_ url: URL) -> Bool`

- [ ] **Step 1: 写失败测试**

测试：

```swift
func test_project_defines_embedded_widget_extension_and_matching_app_group()
func test_credential_deep_link_parses_uuid_and_requests_refresh()
func test_invalid_credential_deep_link_is_rejected()
```

断言：

- 主 App 与 widget extension 的 entitlements 都包含同一 App Group。
- widget extension 的 extension point 为 `com.apple.widgetkit-extension`。
- 合法 UUID 返回 true 并触发对应凭证刷新。
- 非法 UUID、错误 host 或错误 scheme 返回 false。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/ProjectBootstrapTests -only-testing:RoutinUsageTests/WidgetDeepLinkTests`

Expected: FAIL，原因是 widget target 与 deep link 不存在。

- [ ] **Step 3: 配置 XcodeGen target**

在 `project.yml` 新增 `RoutinUsageWidget`：

- `type: app-extension`
- `platform: macOS`
- sources：`RoutinUsageWidget` 与 `RoutinUsage/WidgetShared`
- bundle ID：`cc.idickies.mytoken.widget`
- deployment target：14.0
- extension point：`com.apple.widgetkit-extension`
- 主 App target 依赖并嵌入 extension。
- `RoutinUsageTests` 同时依赖 `RoutinUsage` 与 `RoutinUsageWidget`，让后续 AppIntent 和 Timeline 测试能直接编译 extension 内类型。

两个 entitlements 都声明 App Group。执行 `xcodegen generate`。

- [ ] **Step 4: 实现单凭证 Widget**

要求：

- `TimelineProvider` 只从 App Group 读取 `WidgetSnapshotBundle`。
- 支持 `systemSmall`、`systemMedium`、`systemLarge`。
- 先实现默认 `自动` 风格。
- 聚焦环、扫描带、信号格共用图形组件。
- 点击组件打开 `mytoken://credential/<UUID>?refresh=1`。
- 快照缺失时显示“打开应用添加凭证”。

- [ ] **Step 5: 注册 URL scheme 并处理打开**

- `RoutinUsage` 注册 `mytoken` scheme。
- `RoutinUsageApp` 使用 `onOpenURL` 调用 `AppEnvironment.openCredentialDeepLink`。
- 打开后定位对应凭证并触发刷新。

- [ ] **Step 6: 运行构建与测试**

Run: `scripts/test.sh`

Expected: PASS，且 App 包包含 `Contents/PlugIns/RoutinUsageWidget.appex`。

- [ ] **Step 7: Commit**

```bash
git add RoutinUsageWidget RoutinUsage/RoutinUsage.entitlements project.yml RoutinUsage.xcodeproj RoutinUsage/App RoutinUsageTests/ProjectBootstrapTests.swift RoutinUsageTests/WidgetDeepLinkTests.swift
git commit -m "feat: 新增 macOS 单凭证小组件"
```

### Task 4: 实现 macOS 组件配置与总览组件

**Files:**

- Create: `RoutinUsageWidget/Intents/CredentialEntity.swift`
- Create: `RoutinUsageWidget/Intents/PrimaryMetricEntity.swift`
- Create: `RoutinUsageWidget/Intents/WidgetDisplayStyleIntentEnum.swift`
- Create: `RoutinUsageWidget/Intents/OverviewSourceIntentEnum.swift`
- Create: `RoutinUsageWidget/Intents/CredentialWidgetConfigurationIntent.swift`
- Create: `RoutinUsageWidget/Intents/OverviewWidgetConfigurationIntent.swift`
- Create: `RoutinUsageWidget/MyTokenOverviewWidget.swift`
- Modify: `RoutinUsageWidget/RoutinUsageWidgetBundle.swift`
- Test: `RoutinUsageTests/WidgetIntentQueryTests.swift`
- Test: `RoutinUsageTests/ProjectBootstrapTests.swift`

**Interfaces:**

- Consumes: Task 1、2、3。
- Produces:
  - `CredentialEntity: AppEntity`
  - `PrimaryMetricEntity: AppEntity`
  - `WidgetDisplayStyleIntentEnum: AppEnum`
  - `OverviewSourceIntentEnum: AppEnum`
  - `CredentialWidgetConfigurationIntent: WidgetConfigurationIntent`
  - `OverviewWidgetConfigurationIntent: WidgetConfigurationIntent`
  - Widget kind：`MyTokenCredentialWidget`、`MyTokenOverviewWidget`

- [ ] **Step 1: 写失败测试**

测试：

```swift
func test_credential_entity_query_excludes_disabled_and_missing_credentials()
func test_primary_metric_entity_uses_snapshot_and_falls_back_to_empty()
func test_intent_enums_expose_exact_display_styles()
```

断言：

- `WidgetDisplayStyleIntentEnum` 恰好包含自动、聚焦环、扫描带、信号格。
- 总览来源恰好包含全部启用、手动选择。
- 停用凭证不出现在可选列表。
- 快照缺失时主指标列表为空但查询不崩溃。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/WidgetIntentQueryTests -only-testing:RoutinUsageTests/ProjectBootstrapTests`

Expected: FAIL，原因是 AppIntent 类型不存在。

- [ ] **Step 3: 实现 AppIntent 配置**

要求：

- 单凭证配置：凭证、视觉风格、主指标。
- 总览配置：总览来源、手动凭证集合、视觉风格。
- 默认值为自动、全部启用、主指标自动。
- 手动集合超过 6 个时只保留前 6 个。
- Intent 查询只读取共享快照，不访问主应用凭证存储。

- [ ] **Step 4: 实现总览 Widget**

要求：

- 标准 3 个凭证，扩展 5-6 个。
- 风险排序：认证失败、指标不可用、即将过期、高占用、低余额、用户排序。
- 点击单行打开对应凭证。
- 总体点击空白区打开主应用。

- [ ] **Step 5: 运行测试**

Run: `scripts/test.sh`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add RoutinUsageWidget RoutinUsageTests/WidgetIntentQueryTests.swift RoutinUsageTests/ProjectBootstrapTests.swift project.yml RoutinUsage.xcodeproj
git commit -m "feat: 支持 macOS 小组件配置与总览"
```

### Task 5: 完善 macOS 状态、Timeline 与视觉验收

**Files:**

- Create: `RoutinUsageWidget/WidgetTimelineEntryFactory.swift`
- Modify: `RoutinUsageWidget/MyTokenCredentialWidget.swift`
- Modify: `RoutinUsageWidget/MyTokenOverviewWidget.swift`
- Modify: `RoutinUsageWidget/Views/WidgetGraphicViews.swift`
- Test: `RoutinUsageTests/WidgetTimelineEntryFactoryTests.swift`
- Test: `RoutinUsageTests/WidgetPresentationStateTests.swift`

**Interfaces:**

- Consumes: Task 1 的 `WidgetPresentation`。
- Produces: `WidgetTimelineEntryFactory.makeEntries(snapshot: WidgetSnapshotBundle?, now: Date) -> [WidgetTimelineEntry]`

- [ ] **Step 1: 写失败测试**

测试：

```swift
func test_timeline_adds_reset_expiry_and_stale_entries_without_fabricating_usage()
func test_presentation_states_render_distinct_status_copy()
func test_long_names_and_values_remain_single_line()
```

断言：

- entry 时间来自 `windowEnd`、`subscriptionEndAt`、缓存过期阈值。
- 用量值在后续 entry 中保持不变。
- 首次加载、失败有缓存、认证失败、停用、删除、无指标、固定指标缺失各有唯一状态文案。
- 布局策略对长名称返回省略策略，不返回换行策略。

- [ ] **Step 2: 运行测试确认失败**

Run: `xcodebuild test -project RoutinUsage.xcodeproj -scheme RoutinUsage -only-testing:RoutinUsageTests/WidgetTimelineEntryFactoryTests -only-testing:RoutinUsageTests/WidgetPresentationStateTests`

Expected: FAIL。

- [ ] **Step 3: 实现 Timeline 与状态 UI**

要求：

- 首次加载显示骨架，不显示 0%。
- 失败但有缓存显示上次成功数据和失败分类。
- 认证失败提供明确操作提示。
- 停用降低不透明度。
- 删除后单凭证提示重新配置，总览移除该项。
- 过期用时间徽标，不用高占用红色。

- [ ] **Step 4: 运行完整测试**

Run: `scripts/test.sh`

Expected: PASS。

- [ ] **Step 5: 构建 Release DMG**

Run: `scripts/build-dmg.sh`

Expected: DMG 构建成功，App 包含 widget extension。

- [ ] **Step 6: 真实桌面验收**

安装 Release App，在 macOS 桌面添加 Small/Medium/Large 单凭证与总览组件，覆盖浅色、深色、长名称、多供应商和异常状态，保存截图并确认无换行、无重叠、无溢出，点击跳转正确。

- [ ] **Step 7: Commit**

```bash
git add RoutinUsageWidget RoutinUsageTests/WidgetTimelineEntryFactoryTests.swift RoutinUsageTests/WidgetPresentationStateTests.swift
git commit -m "feat: 完善 macOS 小组件状态与时间线"
```

### Task 6: 建立 Android 配置模型与展示排序器

**Files:**

- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetConfig.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetConfigStore.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetPresentationModels.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetMetricRanker.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetPresentationBuilder.kt`
- Modify: `android/app/build.gradle.kts`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetConfigStoreTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetMetricRankerTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetPresentationBuilderTest.kt`

**Interfaces:**

- Consumes: `shared/widget-contracts/widget-cases.json`、现有 `Credential` 与 `UsageSnapshot`。
- Produces:
  - `enum WidgetKind { Credential, Overview }`
  - `enum WidgetDisplayStyle { Auto, FocusRing, ScanBand, SignalTiles }`
  - `enum WidgetOverviewSource { AllEnabled, Manual }`
  - `enum WidgetSizeKind { Compact, Standard, Expanded }`
  - `data class WidgetConfig`
  - `data class WidgetPresentation`
  - `WidgetConfigStore.load(appWidgetId: Int, kind: WidgetKind): WidgetConfig`
  - `WidgetConfigStore.save(appWidgetId: Int, config: WidgetConfig)`
  - `WidgetConfigStore.migrateLegacySelection(appWidgetId: Int)`
  - `WidgetConfigStore.remove(appWidgetId: Int)`
  - `WidgetConfigStore.requiredCredentialIds(): Set<UUID>`

- [ ] **Step 1: 写失败测试**

测试：

```kotlin
fun `legacy credential selection migrates without losing widget id`()
fun `manual overview is capped at six credentials and preserves order`()
fun `ranker matches shared provider contracts`()
fun `missing fixed metric is reported while real metrics remain`()
fun `builder limits metrics and resolves auto style`()
```

Android 测试必须读取与 Swift 相同的 `widget-cases.json`，断言输出与 Task 1 的供应商顺序、主指标和格式一致。

- [ ] **Step 2: 运行测试确认失败**

Run: `./android/gradlew -p android :app:testDebugUnitTest --tests "ai.routin.mytoken.widget.*"`

Expected: FAIL，原因是新类型不存在。

- [ ] **Step 3: 实现配置存储与迁移**

要求：

- v2 key：`config_<appWidgetId>`。
- 读取 v2 前先检查旧 key `credential_<appWidgetId>`。
- 旧值迁移为 `WidgetKind.Credential + WidgetDisplayStyle.Auto + primaryMetricID = null`。
- 迁移后保留旧 key 作为回滚兼容，不删除。
- v2 解析失败时回退为自动单凭证配置。

- [ ] **Step 4: 实现排序与展示模型**

要求与 Swift Task 1 一致：固定主指标优先、供应商稳定主指标优先、次要指标语义去重、Xiaomi 使用完整分组数字、三档容量为 2/4/6。

- [ ] **Step 5: 运行测试确认通过**

Run: `./android/gradlew -p android :app:testDebugUnitTest --tests "ai.routin.mytoken.widget.*"`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/ai/routin/mytoken/widget android/app/src/test/kotlin/ai/routin/mytoken/widget android/app/build.gradle.kts
git commit -m "feat: 建立 Android 小组件展示契约"
```

### Task 7: 合并 Android 刷新请求并计算组件凭证并集

**Files:**

- Modify: `android/domain/src/main/kotlin/ai/routin/mytoken/domain/usage/RefreshCredentialsUseCase.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetRefreshWorker.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetRefresher.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetProvider.kt`
- Test: `android/domain/src/test/kotlin/ai/routin/mytoken/domain/usage/RefreshCredentialsUseCaseTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetRefreshWorkerTest.kt`

**Interfaces:**

- Consumes: Task 6 的 `WidgetConfigStore.requiredCredentialIds()`。
- Produces:
  - `RefreshCredentialsUseCase.refresh(credential: Credential)` 保持签名，但同一实例内按凭证合并 in-flight 请求。
  - `WidgetRefresher.enqueue(context: Context)` 使用唯一任务名 `mytoken-widget-refresh-all`。
  - `WidgetRefreshWorker` 刷新组件凭证并集并更新全部组件。

- [ ] **Step 1: 写失败测试**

测试：

```kotlin
fun `concurrent refresh of same credential invokes provider once and shares result`()
fun `widget refresh unions credentials instead of refreshing per widget`()
fun `manual widget refresh updates every installed widget after one pass`()
```

断言：

- 同一 `credential.id` 并发调用两次，fake provider 的 `fetchCount == 1`。
- 两个单凭证组件与一个总览组件有重叠凭证时，上游请求 ID 集合去重。
- 手动刷新立即进入 Loading 状态，完成后所有组件重新渲染。

- [ ] **Step 2: 运行测试确认失败**

Run: `./android/gradlew -p android :domain:testDebugUnitTest :app:testDebugUnitTest`

Expected: FAIL。

- [ ] **Step 3: 实现按凭证 in-flight 合并**

在 `RefreshCredentialsUseCase` 内维护互斥保护的 `MutableMap<UUID, Job>`；后来调用等待已有 Job，不重新调用 provider。请求失败时移除 in-flight 记录，允许下次重试。

- [ ] **Step 4: 改造 Widget 刷新任务**

要求：

- 删除按 `appWidgetId` 拆分的 unique work name。
- 全局唯一任务名固定为 `mytoken-widget-refresh-all`。
- Worker 读取两个 provider 的全部 installed widget ID。
- 通过 `WidgetConfigStore.requiredCredentialIds()` 得到并集。
- 刷新完成后更新单凭证与总览组件。
- 保留网络约束和现有刷新偏好。

- [ ] **Step 5: 运行测试确认通过**

Run: `./android/gradlew -p android :domain:testDebugUnitTest :app:testDebugUnitTest`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add android/domain/src/main/kotlin/ai/routin/mytoken/domain/usage/RefreshCredentialsUseCase.kt android/domain/src/test/kotlin/ai/routin/mytoken/domain/usage/RefreshCredentialsUseCaseTest.kt android/app/src/main/kotlin/ai/routin/mytoken/widget android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetRefreshWorkerTest.kt
git commit -m "fix: 合并 Android 小组件刷新请求"
```

### Task 8: 用 Glance 实现 Android 单凭证组件

**Files:**

- Modify: `android/app/build.gradle.kts`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/MyTokenGlanceWidget.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialGlanceWidget.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetLayoutPolicy.kt`
- Create: `android/app/src/main/res/xml/credential_widget_info_v2.xml`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetProvider.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetLayoutPolicyTest.kt`

**Interfaces:**

- Consumes: Task 6、7。
- Produces:
  - `abstract class MyTokenGlanceWidget : GlanceAppWidget`
  - `class CredentialGlanceWidget : MyTokenGlanceWidget`
  - `class CredentialWidgetProvider : GlanceAppWidgetReceiver<CredentialGlanceWidget>`
  - `WidgetLayoutPolicy.kindFor(widthDp: Int, heightDp: Int): WidgetSizeKind`

- [ ] **Step 1: 写失败测试**

测试：

```kotlin
fun `compact standard and expanded map from available widget dimensions`()
fun `all text roles use single line and ellipsize labels`()
fun `existing credential provider remains receiver for legacy widget ids`()
```

断言：

- `widthDp < 180` 为 Compact。
- `widthDp in 180..299` 为 Standard。
- `widthDp >= 300` 为 Expanded。
- 高度不足 110dp 时最多降为 Compact。
- 值和时间不换行，标题与指标名省略。

- [ ] **Step 2: 运行测试确认失败**

Run: `./android/gradlew -p android :app:testDebugUnitTest --tests "ai.routin.mytoken.widget.WidgetLayoutPolicyTest"`

Expected: FAIL。

- [ ] **Step 3: 添加 Glance 依赖并实现组件**

依赖版本使用当前项目确认可解析的 `androidx.glance:glance-appwidget` 稳定版本。实现要求：

- `CredentialWidgetProvider` 继承 `GlanceAppWidgetReceiver`，保留类名和 receiver 声明，兼容已安装旧组件。
- `CredentialGlanceWidget` 使用 responsive size 集合覆盖紧凑、标准、扩展。
- 支持四种视觉风格和自动切换。
- 点击卡片打开对应凭证详情。
- 点击刷新调用全局唯一刷新任务。
- 首次加载显示骨架。

- [ ] **Step 4: 更新 widget metadata**

`credential_widget_info_v2.xml`：

- `minWidth = 110dp`
- `minHeight = 110dp`
- `targetCellWidth = 2`
- `targetCellHeight = 2`
- `resizeMode = horizontal|vertical`
- `widgetFeatures = reconfigurable`

Manifest 继续注册 `CredentialWidgetProvider`，metadata 指向 v2 XML。

- [ ] **Step 5: 运行测试与 Debug 构建**

Run: `./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add android/app/build.gradle.kts android/app/src/main/kotlin/ai/routin/mytoken/widget android/app/src/main/res/xml/credential_widget_info_v2.xml android/app/src/main/AndroidManifest.xml android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetLayoutPolicyTest.kt
git commit -m "feat: 重做 Android 单凭证小组件"
```

### Task 9: 实现 Android 总览组件与配置界面

**Files:**

- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/OverviewGlanceWidget.kt`
- Create: `android/app/src/main/kotlin/ai/routin/mytoken/widget/OverviewWidgetProvider.kt`
- Create: `android/app/src/main/res/xml/overview_widget_info.xml`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetConfigActivity.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetUpdater.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetConfigurationUiStateTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/OverviewWidgetConfigMigrationTest.kt`

**Interfaces:**

- Consumes: Task 6、7、8。
- Produces:
  - `class OverviewGlanceWidget : MyTokenGlanceWidget`
  - `class OverviewWidgetProvider : GlanceAppWidgetReceiver<OverviewGlanceWidget>`
  - `data class WidgetConfigurationUiState`
  - `resolveConfigurationUiState(config: WidgetConfig, credentials: List<Credential>, snapshots: Map<UUID, UsageSnapshot?>): WidgetConfigurationUiState`

- [ ] **Step 1: 写失败测试**

测试：

```kotlin
fun `overview configuration defaults to all enabled and auto style`()
fun `manual selection preserves order and caps at six`()
fun `configuration ui exposes only approved options`()
fun `overview renderer receives risk sorted presentations`()
```

断言：

- 配置项只包含凭证/来源、视觉风格、单凭证主指标。
- 不出现次要指标排序、颜色主题或独立刷新频率配置。
- 停用凭证从“全部启用”结果中移除。
- 认证失败与不可用指标排在普通高占用之前。

- [ ] **Step 2: 运行测试确认失败**

Run: `./android/gradlew -p android :app:testDebugUnitTest --tests "ai.routin.mytoken.widget.WidgetConfigurationUiStateTest" --tests "ai.routin.mytoken.widget.OverviewWidgetConfigMigrationTest"`

Expected: FAIL。

- [ ] **Step 3: 实现配置 Activity**

要求：

- 顶部显示当前尺寸风格的实时预览。
- 单凭证配置：凭证、视觉风格、主指标。
- 总览配置：来源、手动凭证集合、视觉风格。
- 保存后立即更新组件。
- 保留编辑已有组件时的原 widget ID。
- 空凭证时显示“打开应用添加凭证”。

- [ ] **Step 4: 实现总览组件**

要求：

- 标准 3 个凭证，扩展 5-6 个。
- 每行一个主指标。
- 点击行打开对应凭证。
- 删除凭证自动移除。
- 手动刷新触发全局任务。

- [ ] **Step 5: 更新 Manifest 与 updater**

注册 `OverviewWidgetProvider`，让 `AppBackgroundRefreshDelegate` 与 `WidgetRefreshWorker` 同时更新单凭证和总览组件。

- [ ] **Step 6: 运行测试与构建**

Run: `./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/ai/routin/mytoken/widget android/app/src/main/res/xml/overview_widget_info.xml android/app/src/main/AndroidManifest.xml android/app/src/test/kotlin/ai/routin/mytoken/widget
git commit -m "feat: 支持 Android 总览小组件配置"
```

### Task 10: Android 状态降级、旧渲染清理与布局硬化

**Files:**

- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialGlanceWidget.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/OverviewGlanceWidget.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/widget/WidgetPresentationBuilder.kt`
- Delete: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetRenderer.kt`
- Delete: `android/app/src/main/kotlin/ai/routin/mytoken/widget/CredentialWidgetViewsService.kt`
- Delete: `android/app/src/main/res/layout/credential_widget.xml`
- Delete: `android/app/src/main/res/layout/widget_metric_item.xml`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetPresentationStateTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/widget/WidgetTextPolicyTest.kt`

**Interfaces:**

- Consumes: Task 6、8、9。
- Produces: 完整 Android 状态降级与单行文本策略，并移除不再使用的 RemoteViews 渲染路径。

- [ ] **Step 1: 写失败测试**

测试：

```kotlin
fun `widget states map to distinct copy and visual severity`()
fun `long provider plan credential and metric names stay single line`()
fun `full grouped token numbers are not compacted for xiaomi`()
fun `stale data uses timestamp badge instead of high usage color`()
```

断言覆盖：无凭证、首次加载、失败有缓存、认证失败、停用、删除、无指标、固定指标缺失、数据过期。

- [ ] **Step 2: 运行测试确认失败**

Run: `./android/gradlew -p android :app:testDebugUnitTest --tests "ai.routin.mytoken.widget.WidgetPresentationStateTest" --tests "ai.routin.mytoken.widget.WidgetTextPolicyTest"`

Expected: FAIL。

- [ ] **Step 3: 实现状态与文本策略**

要求：

- 首次加载不显示假 0%。
- 失败但有缓存显示更新时间。
- 停用组件降低透明度。
- 认证失败给出重新登录/更换凭证提示。
- 固定指标缺失显示“该指标暂不可用”。
- 过期数据用时间徽标，不用高占用红色。

- [ ] **Step 4: 删除旧 RemoteViews 专用文件**

仅当 `CredentialWidgetProvider` 已稳定运行 Glance 路径且测试通过后删除。Manifest 中移除不再使用的 `CredentialWidgetViewsService`。

- [ ] **Step 5: 运行完整 Android 测试**

Run: `./android/gradlew -p android test`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main android/app/src/test
git commit -m "refactor: 清理 Android 小组件旧渲染路径"
```

### Task 11: 跨端 Release 验收与发布准备

**Files:**

- Modify: `docs/superpowers/plans/2026-10-13-desktop-widgets.md`
- Create: `docs/superpowers/verification/2026-10-13-desktop-widgets-release.md`

**Interfaces:**

- Consumes: 所有前置任务。
- Produces: 可审查的 Release 验收记录。

- [ ] **Step 1: 运行 macOS 完整测试**

Run: `scripts/test.sh`

Expected: PASS。

- [ ] **Step 2: 构建 macOS Release DMG**

Run: `scripts/build-dmg.sh`

Expected: DMG 构建成功，App 内包含 widget extension。

- [ ] **Step 3: macOS 真实桌面截图验收**

添加 Small/Medium/Large 单凭证与总览组件，覆盖浅色、深色、长凭证名、长套餐名、Routin、DeepSeek、Command Code、Xiaomi MiMo、正常、高占用、失败有缓存、固定指标缺失。保存截图并确认无换行、无重叠、无溢出，点击跳转正确。

- [ ] **Step 4: 运行 Android 完整测试**

Run: `./android/gradlew -p android test`

Expected: PASS。

- [ ] **Step 5: 构建 Android Release**

Run: `./android/gradlew -p android :app:assembleRelease`

Expected: Release APK 构建成功。

- [ ] **Step 6: Android 真实设备或模拟器验收**

安装 Release 包，添加 2x2 单凭证、4x2 单凭证、4x2 总览、4x3 或更大单凭证、4x3 或更大总览。覆盖浅色、深色、系统字体放大、旧版本升级保留旧组件选择、手动刷新去重。保存截图并确认无换行、无重叠、无溢出。

- [ ] **Step 7: 记录验收结果**

在 `docs/superpowers/verification/2026-10-13-desktop-widgets-release.md` 记录命令与结果、构建产物路径、截图路径、已覆盖供应商与状态、未覆盖项和原因。

- [ ] **Step 8: Commit**

```bash
git add docs/superpowers/plans/2026-10-13-desktop-widgets.md docs/superpowers/verification/2026-10-13-desktop-widgets-release.md
git commit -m "docs: 记录桌面小组件发布验收"
```

## 执行顺序与发布切分

1. **契约与数据安全**：Task 1-2。
2. **macOS 可发布切片**：Task 3-5。
3. **Android 契约与刷新安全**：Task 6-7。
4. **Android 可发布切片**：Task 8-10。
5. **整体发布**：Task 11。

Task 5 与 Task 10 分别是 macOS、Android 的发布门槛。若需要拆 PR，建议按上述五个阶段拆分，不要把 macOS extension、Android Glance 重写和刷新去重塞进同一个巨型 PR。
