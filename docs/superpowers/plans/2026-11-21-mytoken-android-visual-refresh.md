# MyToken Android 视觉重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已确认的“安静的仪表”设计，把 Android 客户端的首页、卡片系统、凭证列表、凭证编辑、设置和详情页统一到一套 token、一个卡片骨架和一种状态语言，且不改变 provider、数据层、迁移协议与安全路径。

**Architecture:** 先在 `:core-ui` 建立颜色/间距/圆角/尺寸/字体 token，并把现有玻璃组件机械重命名为 `MyToken*`、改为实色表面；随后按页面推进：首页 chrome → 卡片骨架 → 完整模式内容 → 凭证列表 → 编辑器 → 设置 → 详情 → 动效与无障碍收尾。每个任务独立可编译、可测试、可回滚；测试与实现同一次提交更新。

**Tech Stack:** Kotlin 1.9 级别（项目现有配置）、Jetpack Compose（BOM 2024.09.03）、Material 3、Robolectric 4.13、Compose UI Test、Gradle Kotlin DSL。

**Spec:** `docs/superpowers/specs/2026-11-21-mytoken-android-visual-refresh-design.md`

设计稿：`docs/design/home-hifi.html`、`docs/design/cards-spec.html`、`docs/design/pages.html`。实现以 spec 的文字规格为准；设计稿用于对照视觉。

## Global Constraints

- 平台：minSdk 29，compileSdk 34，targetSdk 34，JDK 17，Compose BOM 2024.09.03；不新增第三方 UI 依赖。
- 用户文案保持中文；不新增英文长句；数字使用 tabular numerals（`fontFeatureSettings = "tnum"`）。
- 颜色只从 `MyTokenThemeTokens.colors` 取；`core-ui` 之外禁止 `Color(0x...)`；卡片/输入框禁止 `surface.copy(alpha = ...)`。
- 字号只从 `MyTokenType` 取；业务文件禁止内联 `fontSize =` / `letterSpacing =`；中文最小 11.5sp，`micro` 只用于拉丁单位与数字。
- 卡片：`surface` 实色 + 1px `line` + 20dp 圆角，无阴影；卡片内部不画横线/竖线，用间距与小标题分组。
- 浮层（sheet / menu / dialog）：`surface` + 阴影 + scrim；sheet 顶部 24dp 圆角；只有 `SurfaceRole.Modal` 允许阴影。
- 弧线：270°，从 135° 起顺时针，3.5dp 圆头；简洁 52dp、完整 64dp、详情 88dp；无渐变、无发光。
- 触控区 ≥ 48dp；头部视觉高度 44dp，图标使用 48dp 触控盒（实际布局 48dp）；简洁/完整切换为 44dp；列表行 64dp；开关视觉 46×28dp、触控区 48dp；页面边距 16dp。
- 简洁/完整两种密度都保留；默认密度行为不变：DataStore 为空 → 简洁；有旧设置但无密度字段 → 完整；用户选择后持久化。
- 抽屉：保留现有 edge drawer；宽度 `min(屏宽 × 0.88, 420dp)`；内容 1 列；底部加 `navigationBarsPadding`；设置页头部用关闭“×”，不做全屏路由。
- 不改 `UsageProvider`、`UsageSnapshot`、`UsageMetric`、provider 适配器、`shared/` 契约、迁移 wire format、小组件布局。
- 不做历史趋势、折线图或用量预测；不新增设置项、凭证类型或指标字段。
- 不改安全路径：Android secrets 仍走 Keystore；日志/诊断仍脱敏；任何密钥不得出现在 UI、日志或测试夹具。
- 现有 testTag 语义默认保留；确需变更时同一提交内更新所有断言。
- 每个任务结束运行 `cd android && ./gradlew test`；提交使用 Conventional Commits；从 `master` 建分支，不直接推默认分支。

### 颜色 token（精确值）

| Token | 浅色 | 深色 |
| --- | --- | --- |
| `canvas` | `#EEF0F2` | `#0D0F12` |
| `surface` | `#FFFFFF` | `#16181C` |
| `subtle` | `#F6F7F9` | `#1D2025` |
| `line` | `#E5E7EB` | `#292D33` |
| `ink` | `#17181A` | `#F5F6F7` |
| `muted` | `#6B7076` | `#9BA1A8` |
| `actionFill` | `#2F6BFF` | `#7AA2FF` |
| `actionText` | `#1F5AE0` | `#9DBBFF` |
| `onAction` | `#FFFFFF` | `#061326` |
| `positiveFill` / `positiveText` | `#30C48D` / `#14805A` | `#35D399` / `#5BE0AC` |
| `warningFill` / `warningText` | `#F5A524` / `#9A5B00` | `#FFB340` / `#FFC266` |
| `criticalFill` / `criticalText` | `#E5484D` / `#C0272D` | `#FF6369` / `#FF8A8E` |
| `cat1/2/3` | `#2B2D31` / `#8A8F96` / `#C9CDD3` | `#E8EAED` / `#9BA1A8` / `#4A4F56` |
| `track` | `#E9EBEE` | `#2A2E34` |
| `statusDotNormal` | `#A3A9B0` | `#6B7076` |

### 字体 token（精确值）

| Token | 字号 / 行高 | 字重 |
| --- | --- | --- |
| `display` | 32 / 36 | 750 |
| `displayCompact` | 24 / 28 | 750 |
| `title` | 22 / 28 | 700 |
| `section` | 16 / 22 | 650 |
| `body` | 14 / 21 | 400 |
| `label` | 12.5 / 18 | 500 |
| `caption` | 11.5 / 16 | 400 |
| `micro` | 11 / 15 | 600 |

`display` / `displayCompact` 使用 `tnum` 与负字距（-0.8sp / -0.6sp）；中文 token 字距为 0。

## Review Focus

1. **390dp 屏幕 + 1.3× 字体**：简洁模式必须退回 1 列，卡片名称、数字、重置时间不得被裁切。由 Task 4 的 `fontScale13FallsBackToSingleColumn` 与 Task 5 的 `longNameAtLargeFontScaleDoesNotClip` 覆盖。
2. **刷新失败但仍有上次成功数据**：必须保留数字、状态显示“更新失败”、只在该卡出现重试，不得清空或闪烁。由 Task 5 的 `failedKeepsCachedSnapshotAndMapsToFailed` 覆盖。
3. **`limit = 0` / `percent = null` / 零余额**：不得出现 `NaN`、`Infinity`、空数值；显示 `—` 或 `0%`。由 Task 5 的 `zeroLimitReturnsNullPercentAndDashGauge` 覆盖。
4. **多币种余额（3 组）与零值构成段**：构成条权重不得除零、零值段应省略或显示 0，所有币种都能读到。由 Task 6 的 `multiCurrencyCompositionHandlesZeroSegments` 覆盖。
5. **343dp 抽屉 + 1.3× 字体 + 手势导航条**：设置页关闭按钮可见、行不被裁切、底部操作在系统导航条之上。由 Task 9 的 `drawerSettingsSurvivesLargeFontAndGestureInsets` 覆盖。

## File Structure

| 模块 | 新建 | 修改 | 责任 |
| --- | --- | --- | --- |
| `core-ui` | `MyTokenTokens.kt`、`MyTokenSpacing.kt`、`MyTokenMotion.kt`、`MyTokenTokensTest.kt`、`MyTokenNamingTest.kt`、`MyTokenComponentsTest.kt`、`MyTokenTypographyTest.kt` | `MyTokenTheme.kt`、`MyTokenVisualPolicy.kt`、`MyTokenTypography.kt`、`Components.kt`、`Help.kt`、`EdgeDrawer.kt`、`ProviderLogoPlate.kt`、`build.gradle.kts` | token 单一来源、基础组件、无障碍基础 |
| `feature-home` | `HomeChrome.kt`、`HomeScrollStates.kt`、`UsageCardSkeleton.kt`、`CredentialDetailHero.kt`、`UsageCardHealthTest.kt`、`CredentialUsageSurfaceStyleTest.kt` | `HomeScreen.kt`、`CredentialUsageCard.kt`、`UsageRing.kt`、`CompactUsageCardPresentation.kt`、`FullSplitUsageContent.kt`、`UsageMetricGrid.kt`、`CredentialDetailScreen.kt`、`AllowedModelsSection.kt`、`ProviderCatalog.kt`、既有测试 | 首页、卡片骨架、完整模式、详情页 |
| `feature-credentials` | — | `CredentialListScreen.kt`、`CredentialEditorScreen.kt`、既有测试 | 凭证列表、编辑器 |
| `feature-settings` | — | `SettingsScreen.kt`、`RefreshSettingsScreen.kt`、`NotificationSettingsScreen.kt`、既有测试 | 设置分组与抽屉关闭 |
| `app` | — | `MyTokenApp.kt`、`MyTokenAppTest.kt` | 滚动状态、抽屉关闭接线 |
| `feature-transfer` | — | 仅随 P0 重命名 | 保持行为不变 |

## Task 1: 建立设计 token 与主题映射（P0）

**Files:**
- Create: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenTokens.kt`
- Create: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenSpacing.kt`
- Create: `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/MyTokenTokensTest.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenTheme.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenVisualPolicy.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt:277-311`
- Modify: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/AndroidVisualRegressionTest.kt:50-82`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `@Immutable data class MyTokenColors(...)` with exactly the fields listed in Global Constraints.
  - `internal val MyTokenLightColors: MyTokenColors`, `internal val MyTokenDarkColors: MyTokenColors`, `internal val LocalMyTokenColors: ProvidableCompositionLocal<MyTokenColors>`.
  - `object MyTokenThemeTokens { val colors: MyTokenColors @Composable @ReadOnlyComposable get() }`.
  - `object MyTokenSpacing { xxs=2.dp; xs=4.dp; sm=8.dp; md=12.dp; lg=16.dp; xl=24.dp; xxl=32.dp; page=16.dp; cardCompact=14.dp; cardFull=16.dp; gridCompact=8.dp; gridFull=12.dp; section=24.dp; settingsSection=20.dp }`.
  - `object MyTokenRadius { card=20.dp; inner=12.dp; row=18.dp; sheetTop=24.dp; pill=999.dp }`.
  - `object MyTokenSize { touch=48.dp; header=44.dp; listRow=64.dp; ringCompact=52.dp; ringFull=64.dp; ringHero=88.dp; healthBar=4.dp; switchWidth=46.dp; switchHeight=28.dp; settingsRowMin=44.dp }`.
  - `MyTokenVisualPolicy.Material` becomes `enum class Material { Solid, Overlay }`; `materialFor(Modal) = Overlay`, all other roles `Solid`; `allowsShadow(role) = role == SurfaceRole.Modal`.

- [ ] **Step 1: Write the failing token test**

```kotlin
class MyTokenTokensTest {
    @Test fun `light tokens match approved design`() {
        assertEquals(Color(0xFFEEF0F2), MyTokenLightColors.canvas)
        assertEquals(Color(0xFFFFFFFF), MyTokenLightColors.surface)
        assertEquals(Color(0xFFF6F7F9), MyTokenLightColors.subtle)
        assertEquals(Color(0xFFE5E7EB), MyTokenLightColors.line)
        assertEquals(Color(0xFF17181A), MyTokenLightColors.ink)
        assertEquals(Color(0xFF6B7076), MyTokenLightColors.muted)
        assertEquals(Color(0xFF2F6BFF), MyTokenLightColors.actionFill)
        assertEquals(Color(0xFF14805A), MyTokenLightColors.positiveText)
        assertEquals(Color(0xFFE5484D), MyTokenLightColors.criticalFill)
        assertEquals(Color(0xFFE9EBEE), MyTokenLightColors.track)
        assertEquals(Color(0xFFA3A9B0), MyTokenLightColors.statusDotNormal)
    }

    @Test fun `dark onAction is dark ink not white`() {
        assertEquals(Color(0xFF061326), MyTokenDarkColors.onAction)
        assertEquals(Color(0xFF7AA2FF), MyTokenDarkColors.actionFill)
        assertEquals(Color(0xFF16181C), MyTokenDarkColors.surface)
    }

    @Test fun `spacing and radius match the approved scale`() {
        assertEquals(20.dp, MyTokenRadius.card)
        assertEquals(12.dp, MyTokenRadius.inner)
        assertEquals(18.dp, MyTokenRadius.row)
        assertEquals(48.dp, MyTokenSize.touch)
        assertEquals(44.dp, MyTokenSize.header)
        assertEquals(4.dp, MyTokenSize.healthBar)
        assertEquals(52.dp, MyTokenSize.ringCompact)
        assertEquals(88.dp, MyTokenSize.ringHero)
    }

    @Test fun `only modal surfaces may cast shadow`() {
        assertEquals(MyTokenVisualPolicy.Material.Solid, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Card))
        assertEquals(MyTokenVisualPolicy.Material.Solid, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Window))
        assertEquals(MyTokenVisualPolicy.Material.Overlay, MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Card))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd android && ./gradlew :core-ui:test --tests "ai.routin.mytoken.core.ui.MyTokenTokensTest"`
Expected: FAIL — unresolved `MyTokenLightColors` / `MyTokenDarkColors` / `MyTokenRadius`.

- [ ] **Step 3: Implement tokens, spacing, policy, and theme mapping**

`MyTokenColors` holds the 20 fields exactly as named in Global Constraints. `MyTokenSpacing`/`MyTokenRadius`/`MyTokenSize` are plain objects.

`MyTokenTheme.kt`:
- Light `lightColorScheme(primary = actionFill, onPrimary = onAction, background = canvas, surface = surface, surfaceVariant = subtle, onSurface = ink, onSurfaceVariant = muted, outline = line, outlineVariant = line, secondary = positiveFill, tertiary = warningFill, error = criticalFill, errorContainer = subtle, onError = onAction)`.
- Dark uses the dark tokens; `onPrimary = #061326`.
- Wrap content in `CompositionLocalProvider(LocalMyTokenColors provides colors, LocalOverscrollConfiguration provides null)`.
- Keep dynamic color off.

`MyTokenVisualPolicy`:
- Keep `outerRadiusDp = MyTokenRadius.card`, `innerRadiusDp = MyTokenRadius.inner`, `buttonRadiusDp = MyTokenRadius.inner` for source compatibility.
- Replace `WindowGlass/ModalGlass` with `Solid/Overlay` and update `Components.kt`’s only material comparison to `material == Material.Overlay`; keep the rest of the surface implementation for Task 2.

`Components.kt` in this task only needs to compile with the new enum; do not rewrite the surface yet.

Keep `object MyTokenPalette` temporarily as a `@Deprecated` compatibility shim whose values now point at the new tokens, using fills for the old single-value slots (`brandLight = actionFill`, `positiveLight = positiveFill`, `positiveDark = positiveFill dark`, `warning = warningFill`, `critical = criticalFill`, `lightCanvas = canvas`, `lightSurface = surface`, `lightElevated = subtle`, `lightText = ink`, `lightMutedText = muted`, and the dark equivalents). Page tasks migrate text to `positiveText` / `warningText` / `criticalText`; Task 11 deletes the shim.

- [ ] **Step 4: Update the visual regression assertions**

In `AndroidVisualRegressionTest.kt`:
- `方案A材质契约只允许模态投影`: assert `materialFor(Window) == Solid`, `materialFor(Card) == Solid`, `materialFor(Modal) == Overlay`, `allowsShadow(Modal)` true, others false.
- `方案A主题锁定品牌蓝与状态色`: light primary `0xFF2F6BFF`, dark primary `0xFF7AA2FF`, light secondary `0xFF30C48D`, dark error `0xFFFF6369`, dark onPrimary `0xFF061326`.

- [ ] **Step 5: Run tests**

Run:
```bash
cd android
./gradlew :core-ui:test --tests "ai.routin.mytoken.core.ui.MyTokenTokensTest"
./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.AndroidVisualRegressionTest"
./gradlew test
```
Expected: PASS. `./gradlew test` proves no module broke on the enum change.

- [ ] **Step 6: Commit**

```bash
git add android/core-ui android/feature-home/src/test
git commit -m "feat(android): 建立视觉 token 与主题映射"
```

---

## Task 2: 重命名玻璃组件并改为实色表面（P0）

**Files:**
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Help.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Dialogs.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/EdgeDrawer.kt:285`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/ProviderLogoPlate.kt`
- Modify: `android/core-ui/build.gradle.kts`
- Modify (rename usages only): all files reported by `grep -rl 'GlassButton\|GlassSwitch\|GlassTextField\|GlassHelpButton\|LiquidGlassSurface\|glassFilterChip' android --include='*.kt'`
- Create: `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/MyTokenNamingTest.kt`
- Create: `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/MyTokenComponentsTest.kt`

**Interfaces:**
- Consumes: Task 1 tokens (`MyTokenThemeTokens.colors`, `MyTokenRadius`, `MyTokenSize`).
- Produces:
  - `enum class MyTokenButtonTone { Primary, Neutral, Destructive }`.
  - `@Composable fun MyTokenButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: MyTokenButtonTone = MyTokenButtonTone.Neutral, enabled: Boolean = true, icon: ImageVector? = null)`.
  - `@Composable fun MyTokenSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, contentDescription: String? = null)`.
  - `@Composable fun MyTokenTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, contentPadding: Dp = 6.dp, singleLine: Boolean = true, visualTransformation: VisualTransformation = VisualTransformation.None, trailingIcon: (@Composable () -> Unit)? = null, supportingText: String? = null)` — `contentPadding` is kept so `UsageShareDialog` compiles unchanged.
  - `@Composable fun MyTokenSurface(modifier: Modifier = Modifier, surfaceRole: MyTokenVisualPolicy.SurfaceRole = MyTokenVisualPolicy.SurfaceRole.Control, shape: RoundedCornerShape = RoundedCornerShape(MyTokenRadius.card), contentAlignment: Alignment = Alignment.TopStart, content: @Composable BoxScope.() -> Unit)`.
  - `@Composable fun MyTokenSection(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)`.
  - `@Composable fun MyTokenRow(icon: ImageVector?, title: String, supportingText: String? = null, value: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier)`.
  - `@Composable fun MyTokenSegmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)`.
  - `enum class MyTokenStatusTone { Neutral, Positive, Warning, Critical }`; `@Composable fun MyTokenStatus(text: String, tone: MyTokenStatusTone, modifier: Modifier = Modifier)`.
  - `@Composable fun myTokenFilterChipColors(selected: Boolean)`, `@Composable fun myTokenFilterChipBorder(selected: Boolean)`.
  - `@Composable fun MyTokenHelpButton(...)` (renamed, behavior unchanged).

- [ ] **Step 1: Write the failing naming and component tests**

`MyTokenNamingTest` scans `File(System.getProperty("user.dir")).parentFile` (i.e. `android/`) for `src/main/kotlin/**/*.kt` and asserts none of these substrings exist: `GlassButton`, `GlassButtonTone`, `GlassSwitch`, `GlassTextField`, `GlassHelpButton`, `LiquidGlassSurface`, `glassFilterChipColors`, `glassFilterChipBorder`.

`MyTokenComponentsTest` (Robolectric, `@Config(sdk = [34], qualifiers = "w400dp-h900dp")`):
```kotlin
@Test fun switchExposesRoleAndTogglesOff() {
    var checked = true
    composeRule.setContent { MyTokenTheme { MyTokenSwitch(checked, { checked = it }, contentDescription = "启用 A") } }
    composeRule.onNodeWithContentDescription("启用 A").assertIsOn().performClick()
    assertFalse(checked)
}

@Test fun segmentedSelectsSecondOption() {
    var selected = 0
    composeRule.setContent { MyTokenTheme { MyTokenSegmented(listOf("跟随系统", "浅色", "深色"), selected, { selected = it }) } }
    composeRule.onNodeWithText("浅色").performClick()
    assertEquals(1, selected)
}

@Test fun surfaceUsesSolidBackgroundAndHairline() {
    val source = File(System.getProperty("user.dir")).walkTopDown().first { it.name == "Components.kt" }.readText()
    assertFalse(source.contains("Brush.verticalGradient"))
    assertFalse(source.contains("copy(alpha = 0.62f)"))
    assertTrue(source.contains("border(1.dp"))
}
```

Add to `core-ui/build.gradle.kts`:
```kotlin
testOptions { unitTests { isIncludeAndroidResources = true } }
dependencies {
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :core-ui:test --tests "ai.routin.mytoken.core.ui.MyTokenNamingTest" --tests "ai.routin.mytoken.core.ui.MyTokenComponentsTest"`
Expected: FAIL — `Glass*` still present, `MyTokenSegmented` unresolved.

- [ ] **Step 3: Perform the mechanical rename**

```bash
cd android
grep -rl 'GlassButtonTone\|GlassButton\|GlassSwitch\|GlassTextField\|GlassHelpButton\|LiquidGlassSurface\|glassFilterChipColors\|glassFilterChipBorder' . --include='*.kt' | while read -r f; do
  perl -pi -e 's/GlassButtonTone/MyTokenButtonTone/g; s/GlassButton/MyTokenButton/g; s/GlassSwitch/MyTokenSwitch/g; s/GlassTextField/MyTokenTextField/g; s/GlassHelpButton/MyTokenHelpButton/g; s/LiquidGlassSurface/MyTokenSurface/g; s/glassFilterChipColors/myTokenFilterChipColors/g; s/glassFilterChipBorder/myTokenFilterChipBorder/g;' "$f"
done
grep -rn 'Glass\|glassFilterChip\|LiquidGlass' --include='*.kt' . || true
```
The final grep must print nothing.

- [ ] **Step 4: Restyle the core components**

In `Components.kt`:
- `MyTokenSurface`: choose `surface` for `Window/Card/Modal`, `subtle` for `Control/Metric`; draw `background(color)` then, for non-Window roles, `border(1.dp, line, shape)`; for `Modal` draw `shadow(24.dp)`; no Brush, no alpha.
- `MyTokenButton`: min height 48dp, 12dp radius, horizontal padding 20dp; Primary `actionFill/onAction`, Neutral `subtle/ink`, Destructive `criticalFill` with white (`onAction` in dark); disabled content 45% alpha is allowed (it is text/icon, not a surface).
- `MyTokenSwitch`: visual track 46×28; wrap it in a 48×48dp touch box (the `toggleable` lives on the outer box); checked track `actionFill`, unchecked `track`; thumb `surface`.
- `MyTokenTextField`: `Column { Text(label, style = MyTokenType.label, color = muted); OutlinedTextField(label = null, container = subtle, focused/unfocused border = action/line, shape = inner, min height 48); supportingText caption }`. The `modifier` (and testTag) stays on the `OutlinedTextField`.
- `MyTokenSection`: `MyTokenSurface(surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card)` + 16dp padding + section title + 12dp column spacing.
- `SectionCard(title, modifier, supportingText, content)` delegates to `MyTokenSection` (no alpha, no elevation); `SettingRow(title, supportingText, trailing)` delegates to a 44dp-min `MyTokenRow`. Keep both signatures so `RefreshSettingsScreen` / `NotificationSettingsScreen` compile unchanged.
- `MyTokenRow`: min height 44dp (48dp when clickable), icon 18dp muted, title `body`, supporting `caption` muted, value `body` muted, trailing slot.
- `MyTokenSegmented`: 44dp height, `subtle` track + 4dp padding, equal weights, selected pill `surface` + `line` border + `actionText` label, unselected `muted`; `Role.RadioButton` and `selected` semantics.
- `MyTokenStatus`: 6dp dot + `caption` text; Neutral uses `statusDotNormal/muted`; Positive uses `positiveFill/positiveText`; Warning `warningFill/warningText`; Critical `criticalFill/criticalText`.
- `myTokenFilterChipColors`: container `subtle`, label `muted`, selected container `ink`, selected label `surface`; border `line` (selected transparent). No alpha.
- `ProviderLogoPlate`: white plate + 1px `line` in both themes; remove the 94% alpha; content padding unchanged.
- `Help.kt`: rename `GlassToneColors` → `ButtonToneColors`; help icon keeps 17dp visual but gets a 48dp touch target; help markers use `positiveText` / `warningText` / `criticalText`, never fill colors as text.
- `Dialogs.kt`: dialog container `surface` + 1px `line` + shadow (Modal only); replace accent-tinted icon backgrounds/`accent.copy(alpha = 0.12f)` plates with `subtle` + `line`; keep text/action semantics and the `my_token_dialog` tag.
- `EdgeDrawer.kt`: only renamed identifiers; drawer stays `SurfaceRole.Modal`.

- [ ] **Step 5: Run tests**

Run:
```bash
cd android
./gradlew :core-ui:test
./gradlew test
```
Expected: PASS. Fix any feature test that referenced the old names.

- [ ] **Step 6: Commit**

```bash
git add android
git commit -m "refactor(android): 重命名玻璃组件并改为实色表面"
```

---

## Task 3: 收敛字号层级并启用等宽数字（P0）

**Files:**
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenTypography.kt`
- Create: `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/MyTokenTypographyTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `MyTokenType.display` 32/36/750, `displayCompact` 24/28/750, `title` 22/28/700, `section` 16/22/650, `body` 14/21/400, `label` 12.5/18/500, `caption` 11.5/16/400, `micro` 11/15/600.
  - Numeric/layout variants (so feature code never needs inline sizes): `detailTitle` 20/26/700, `listTitle` 15/19/600, `ringCompact` 14/18/700 + tnum, `ringFull` 16/20/700 + tnum, `percentMark` 9/12/600 (symbol only), `detailRemaining` 18/24/650 + tnum, `numericValue` 18/24/650 + tnum.
  - `display` / `displayCompact` and every numeric variant include `fontFeatureSettings = "tnum"`; letterSpacing `-0.8sp` / `-0.6sp` for display sizes, `-0.3sp` for ring sizes, `0` for the rest.
  - Old names (`pageTitle`, `sectionTitle`, `cardTitle`, `heroNumber`, `balanceNumber`, `metricLabel`, …) remain as `@Deprecated` aliases with their current values so P0 compiles; page tasks migrate away; Task 11 removes them.

- [ ] **Step 1: Write the failing typography test**

```kotlin
class MyTokenTypographyTest {
    @Test fun `seven levels match the approved scale`() {
        assertEquals(32.sp, MyTokenType.display.fontSize)
        assertEquals(36.sp, MyTokenType.display.lineHeight)
        assertEquals(FontWeight(750), MyTokenType.display.fontWeight)
        assertEquals(24.sp, MyTokenType.displayCompact.fontSize)
        assertEquals(22.sp, MyTokenType.title.fontSize)
        assertEquals(16.sp, MyTokenType.section.fontSize)
        assertEquals(14.sp, MyTokenType.body.fontSize)
        assertEquals(12.5.sp, MyTokenType.label.fontSize)
        assertEquals(11.5.sp, MyTokenType.caption.fontSize)
        assertEquals(11.sp, MyTokenType.micro.fontSize)
        assertEquals(20.sp, MyTokenType.detailTitle.fontSize)
        assertEquals(15.sp, MyTokenType.listTitle.fontSize)
        assertEquals(14.sp, MyTokenType.ringCompact.fontSize)
        assertEquals(16.sp, MyTokenType.ringFull.fontSize)
        assertEquals(18.sp, MyTokenType.detailRemaining.fontSize)
    }

    @Test fun `numbers are tabular`() {
        assertEquals("tnum", MyTokenType.display.fontFeatureSettings)
        assertEquals("tnum", MyTokenType.displayCompact.fontFeatureSettings)
        assertEquals((-0.8f).sp, MyTokenType.display.letterSpacing)
    }

    @Test fun `chinese tokens do not use negative tracking`() {
        listOf(MyTokenType.title, MyTokenType.section, MyTokenType.body, MyTokenType.label, MyTokenType.caption)
            .forEach { assertEquals(0.sp, it.letterSpacing) }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd android && ./gradlew :core-ui:test --tests "ai.routin.mytoken.core.ui.MyTokenTypographyTest"`
Expected: FAIL — `display` unresolved.

- [ ] **Step 3: Implement the seven tokens and deprecated aliases**

Add the exact tokens. Keep the old aliases mapping to their current sizes:
`pageTitle = 30/34/700`, `sectionTitle = 16/22/600`, `cardTitle = 16/20/600`, `cardTitleLong`, `cardTitleDense`, `heroNumber`, `balanceNumber`, `metricLabel`, `metricLabelTight`, `metricValue`, `metricValueStrong`, `metricCaption`, `factLabel`, `factValue`, `chip`, `badge`, `sync`. Mark each `@Deprecated("改用 MyTokenType 的七级 token")`.

Update `MyTokenTypography` Material mapping: `headlineLarge = title`, `headlineMedium = title`, `headlineSmall = section`, `titleLarge = section`, `titleMedium = section`, `titleSmall = label`, `bodyLarge = body`, `bodyMedium = body`, `bodySmall = caption`, `labelLarge = label`, `labelMedium = label`, `labelSmall = micro`.

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :core-ui:test --tests "ai.routin.mytoken.core.ui.MyTokenTypographyTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/core-ui
git commit -m "feat(android): 收敛字号层级并启用等宽数字"
```

---

## Task 4: 重构首页 chrome、状态行与网格（P1）

**Files:**
- Create: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/HomeChrome.kt`
- Create: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/HomeScrollStates.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/HomeScreen.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/MyTokenApp.kt:96,276-320`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenAdaptiveTest.kt`

**Interfaces:**
- Consumes: Task 1–3 tokens/components; existing `CredentialCardUi`, `ProviderId`, `UsageCardDensity`.
- Produces:
  - `@Stable class HomeScrollStates(val compact: LazyGridState = LazyGridState(), val full: LazyListState = LazyListState())`.
  - `@Composable fun rememberHomeScrollStates(): HomeScrollStates`.
  - `HomeScreen(..., homeScrollStates: HomeScrollStates = rememberHomeScrollStates())` replacing `gridState: LazyStaggeredGridState`.
  - `internal enum class HomeHealthTone { Normal, Warning, Critical }`; `internal fun homeHealthTone(card: CredentialCardUi): HomeHealthTone`; `internal fun homeHealthLabel(tone: HomeHealthTone): String`. Mapping: failed/unavailable/critical → Critical; warning → Warning; stale/loading/normal → Normal (stale is neutral, not “需注意”).
  - `internal data class HealthRulerEntry(val credentialId: UUID, val name: String, val tone: HomeHealthTone)`.
  - `@Composable internal fun HomeToolbar(credentialCount: Int, density: UsageCardDensity, onToggleDensity: () -> Unit, onOpenCredentials: () -> Unit, onOpenSettings: () -> Unit, hasUpdateAvailable: Boolean, modifier: Modifier = Modifier)`.
  - `@Composable internal fun HomeStatusRow(total: Int, attention: Int, syncText: String, selectedProvider: ProviderId?, providers: List<ProviderId>, counts: Map<ProviderId, Int>, onSelect: (ProviderId?) -> Unit, modifier: Modifier = Modifier)`.
  - `@Composable internal fun HealthRuler(entries: List<HealthRulerEntry>, onLocate: (UUID) -> Unit, modifier: Modifier = Modifier)`.

**Tag contract:** keep `home_toolbar`, `home_open_credentials`, `home_open_settings`, `home_update_badge`, `usage_density_toggle`, `usage_density_option_compact`, `usage_density_option_full`, `home_update_banner`, `home_update_banner_icon`, `home_pull_refresh`, `provider_filter_menu`, `provider_filter_item_${rawValue}`, `health_summary`, `health_segment_$index`, `credential_card_${id}`. New: `home_status_row`, `home_scroll_edge`.

- [ ] **Step 1: Update/write the failing home tests**

In `HomeScreenAdaptiveTest`, always pass `usageCardDensity = UsageCardDensity.COMPACT` except one full-mode test, and assert:
- 400dp → `home_grid_2_columns`
- 700dp → `home_grid_3_columns`
- 1000dp → `home_grid_4_columns`
- full mode 400dp → `home_list_full`; full mode 700dp → `home_grid_2_columns`? Use a separate tag `home_list_2_columns` for the full grid, or keep `home_grid_2_columns`; choose `home_grid_2_columns` for full ≥700 and `home_list_full` for 1 column. Tests assert accordingly.

Add:
```kotlin
@Test @Config(qualifiers = "w390dp-h1100dp")
fun fontScale13FallsBackToSingleColumn() {
    composeRule.setContent {
        MyTokenTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                HomeScreen(/* compact, 3 cards */)
            }
        }
    }
    composeRule.onNodeWithTag("home_grid_1_columns").assertExists()
}

@Test
fun statusRowShowsAttentionCountAndFilterMenu() {
    // 2 warning cards + 1 normal
    composeRule.onNodeWithText("2 个需注意", substring = true).assertIsDisplayed()
    composeRule.onNodeWithTag("provider_filter_menu").performClick()
    composeRule.onNodeWithTag("provider_filter_item_deepseek").assertExists()
}
```

In `HomeScreenTest`:
- `顶部导航固顶且离开首页后保留滚动位置`: replace `LazyStaggeredGridState` with `rememberHomeScrollStates()`; scroll `home_list_full`; assert `homeScrollStates.full.firstVisibleItemIndex > 0` and preserved.
- `filtersProvidersAndHidesDisabledCredentials`: click `provider_filter_menu` before `provider_filter_item_routin`.
- `credentialCardKeepsNarrowMetricLayoutOnWideScreens`: pass `usageCardDensity = COMPACT`, assert `home_grid_4_columns`.
- Delete `账户健康说明使用分组排版` (help entry removed); replace with `状态行显示需注意数量`.
- Keep `首页顶部使用单行工具条且账户健康保持紧凑`; `health_summary` now wraps status row + ruler and must stay < 100dp.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd android && ./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.HomeScreenTest" --tests "ai.routin.mytoken.feature.home.HomeScreenAdaptiveTest"`
Expected: FAIL — `HomeScrollStates` unresolved; grid tags differ.

- [ ] **Step 3: Implement `HomeScrollStates` and the locate helper**

`HomeScrollStates` holds `compact: LazyGridState` and `full: LazyListState`. `locateCard`:
1. choose state by density;
2. `animateScrollToItem(index, -viewportHeight / 3)`;
3. wait one frame, read the visible item at `index`, compute `delta = itemCenter - (viewportCenter - centerBiasPx)`;
4. if `abs(delta) > 1f`, `state.scroll { animateBy(delta) }` with 240ms tween.
Expose `internal suspend fun locateCard(states: HomeScrollStates, density: UsageCardDensity, index: Int, centerBiasPx: Float)`.

- [ ] **Step 4: Implement `HomeChrome.kt` and restructure `HomeScreen.kt`**

`HomeToolbar`:
- 44dp height; left list icon 48dp touch (20dp icon, muted), center `DensitySwitch` (text-only: two 44dp-tall options, label 12.5sp, selected `actionText` + 2dp underline; 240ms underline animation; no filled track), right settings icon 48dp touch + 7dp `warningFill` update dot.
- Content is not wrapped in 78% surface circles.

`HomeStatusRow`:
- 34dp min height; left text `${total} 个账户 · ${attention} 个需注意 · ${syncText}` (`attention` uses `warningText`, rest `muted`); if attention == 0 use `全部正常`.
- Right pill `全部供应商 ▾` or `${displayName} ▾`, 30dp visual height inside a 48dp touch box, `subtle` container, `line` border, `label` style; opens `DropdownMenu` with “全部 N” + each provider + count. Preserve tag semantics.

`HealthRuler`:
- The status row (34dp) and the 4dp ruler share one 48dp block: text line on top, 4dp bar at the bottom with 4dp gaps. The segment hit boxes are 48dp tall columns overlaying the block by x-range, so the visual stays 4dp while the touch target is 48dp.
- Draw the segment hit boxes first and the status row content above them; the filter pill is the only clickable node in the status row, so it wins hit-testing while taps on plain status text fall through to the ruler.
- Normal segment `statusDotNormal`, warning `warningFill`, critical `criticalFill`; each segment `weight(1f)` with contentDescription `${name}，${label}`; tag `health_segment_$index`.
- `health_summary` sits on the 48dp block; remove the help button and the old provider-logo segment row.

`HomeUpdateBanner` becomes a 28dp single row: 16dp star (2.4s breathing unless animators disabled), text “发现新版本 X”, trailing “去更新” in `actionText`; no radial gradient, no white overlay. Keep tags.

`HomeScreen`:
- Toolbar + optional update hint + the combined 48dp status/ruler block are fixed above the scroll container (not scrolling items); `PullToRefreshBox` fills the rest. This keeps the first card roughly 80dp earlier than the old header + health card + filter stack.
- Compact: `LazyVerticalGrid(columns = Fixed(columns))`; columns = 1 if fontScale ≥ 1.3, else use `LocalConfiguration.current.screenWidthDp`: 2 (<600), 3 (600–839), 4 (≥840). Tag `home_grid_${columns}_columns`.
- Full: `LazyColumn` for 1 column (`home_list_full`) or `LazyVerticalGrid(Fixed(2))` when `screenWidthDp >= 700` (`home_grid_2_columns`).
- Content padding top 12, bottom 24; compact spacing 8, full 12.
- Add a top `home_scroll_edge` 1dp `line` + 12dp fade when `firstVisibleItemIndex > 0 || offset > 0`.
- Use `homeScrollStates`; remove the staggered-grid imports and `LazyStaggeredGridState` parameter.

`MyTokenApp.kt`: replace `rememberLazyStaggeredGridState()` with `rememberHomeScrollStates()` and pass `homeScrollStates`.

- [ ] **Step 5: Run tests**

Run:
```bash
cd android
./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.HomeScreenTest" --tests "ai.routin.mytoken.feature.home.HomeScreenAdaptiveTest"
./gradlew :app:test --tests "ai.routin.mytoken.MyTokenAppAdaptiveTest" --tests "ai.routin.mytoken.EdgeDrawerNavigationTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add android
git commit -m "feat(android): 重构首页头部、状态行与网格"
```

---

## Task 5: 统一卡片骨架、状态与两种密度（P1）

**Files:**
- Create: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageCardSkeleton.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageRing.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CompactUsageCardPresentation.kt`
- Create: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/UsageCardHealthTest.kt`
- Rename: `CredentialUsageGlassStyleTest.kt` → `CredentialUsageSurfaceStyleTest.kt` (same package)
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenTest.kt`

**Interfaces:**
- Consumes: Task 1–4 tokens/components, `UsageCardDensity`, `UsageMetric`, `CredentialCardUi`.
- Produces (all `internal` in `feature.home`):
  - `enum class UsageCardHealth { Normal, Warning, Critical, Stale, Failed, Loading, Disabled }`.
  - `fun usageCardHealth(card: CredentialCardUi): UsageCardHealth`.
  - `fun usageCardStatusText(health: UsageCardHealth): String?` (`"正常"`, `"接近上限"`, `"已达上限"`, `"待刷新"`, `"更新失败"`, `"刷新中"`, `"已停用"`).
  - `fun usageCardStatusTone(health: UsageCardHealth): MyTokenStatusTone`.
  - `@Composable fun UsageArcGauge(percent: Double?, tone: UsageMetricTone, diameter: Dp)` — 270° arc from 135°, 3.5dp round cap, track `track`, accent from tone (`Normal → positiveFill`, `Warning → warningFill`, `Critical → criticalFill`), center number `MyTokenType.ringCompact` (52dp) / `ringFull` (64dp) + `percentMark`; percent null shows `—`.
  - `@Composable fun UsageCardFrame(density: UsageCardDensity, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`.
  - `@Composable fun UsageCardIdentity(card: CredentialCardUi, density: UsageCardDensity, health: UsageCardHealth, onRetry: () -> Unit)`.
  - `@Composable fun UsageCardWindowRow(metric: UsageMetric, dense: Boolean, modifier: Modifier = Modifier)`; the row container carries `testTag("compact_window_row_${metric.id}")` in compact mode.
  - `@Composable fun UsageCardFacts(rows: List<Pair<String, String>>, modifier: Modifier = Modifier)`.
  - `@Composable fun UsageCardFooter(text: String, showChevron: Boolean)`.
  - `UsageMetricTone` stays in `CompactUsageCardPresentation.kt` as `enum class UsageMetricTone { Normal, Warning, Critical }`.

**Status rules:** normal ring `positiveFill`, status text neutral; warning ring `warningFill`, text `warningText`; critical ring `criticalFill`, text `criticalText`; stale/failed ring `track`; loading skeleton; disabled neutral. Status dot 6dp, always with text.

**Compact:** 14dp padding, logo 24, name 13sp label, meta 11sp caption, ring 52, secondary windows as mini rows (3dp bar) for every secondary metric, footer 11.5sp. Refresh icon hidden except `Failed`. Balance: label 11.5, amount 24sp `displayCompact`, 5dp composition bar, legend 11.5, then model count.

**Full:** 16dp padding, logo 28, name 15.5sp section, meta 11.5sp caption, left 40% ring 64 / right 60% window rows, facts only here, footer freshness + chevron. Refresh icon always visible (48dp touch).

**No internal dividers:** remove `HorizontalDivider` and the 1dp vertical boxes from all card bodies; use 14dp row spacing, 18dp column gap, section labels.

- [ ] **Step 1: Write the failing health/edge tests**

`UsageCardHealthTest`:
```kotlin
@Test fun warningAndCriticalOutrankNormal()
@Test fun failedKeepsCachedSnapshotAndMapsToFailed()
@Test fun zeroLimitReturnsNullPercentAndDashGauge()
@Test fun unavailableMetricIsCritical()
```

In `HomeScreenTest`:
- Replace `compactThreeRingsStayInOneRow` with `compactThreeWindowsUsesOneRingAndMiniRows`: assert `compact_ring_primary` exists once, `compact_window_row_weekly` and `compact_window_row_monthly` exist, `compact_ring_tile_weekly` does not.
- Rename `compactValueCardUsesHairlineTwoByTwoGrid` → `compactValueGridUsesSpacingNotDividers`; keep the 2×2 bounds assertions, assert source has no `HorizontalDivider`.
- Add `longNameAtLargeFontScaleDoesNotClip`: name 30 chars + fontScale 1.3; assert name node exists and no node has visual overflow for the amount/ring percent.
- Keep `failedCardShowsErrorStaleDataAndRetry` (update status text to `更新失败` only).
- `compactGlmCardShowsResetCardCountsAndOpensDetailList`: keep text/reset assertions; labels become `5 小时` / `周`.

`CredentialUsageSurfaceStyleTest`:
```kotlin
assertFalse(source.contains("HorizontalDivider"))
assertFalse(source.contains("surface.copy(alpha"))
assertFalse(source.contains("StatusPill("))
assertTrue(ringSource.contains("ArcStartAngle = 135f"))
assertTrue(ringSource.contains("ArcSweepAngle = 270f"))
assertTrue(source.contains("UsageArcGauge"))
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.UsageCardHealthTest" --tests "ai.routin.mytoken.feature.home.HomeScreenTest" --tests "ai.routin.mytoken.feature.home.CredentialUsageSurfaceStyleTest"`
Expected: FAIL — `UsageArcGauge` / `usageCardHealth` unresolved; old `compact_ring_tile_*` tags.

- [ ] **Step 3: Implement the skeleton and rewrite the card**

`UsageCardSkeleton.kt`:
- Move `cardHealth` logic from `CredentialUsageCard.kt` into `usageCardHealth`; failed wins over snapshot health, disabled wins over failed, loading before stale.
- `UsageCardFrame` uses `MyTokenSurface(surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card)`, 20dp radius, 1dp line, 14/16dp padding, `Card`-equivalent click with `Role.Button`.
- Identity: left logo plate 24/28 (7dp inner radius), name single line 13/15.5 (long names ellipsize once; meta stays single line 11/11.5 with `compactMeta` from planName/expiry), right `MyTokenStatus` + retry icon (48dp touch, 15dp icon, tag `credential_refresh_${id}`). Keep `credential_avatar_${id}` on the logo and `credential_failure_${id}` on the status area when `health == Failed`; clicking the failure area opens the existing failure dialog, and its bounds must stay left of the retry button.
- Footer: compact text `usageCardFooterText(card, health)`; full `card.freshness.text` + chevron.
- Facts: label `caption` muted + value `caption` ink, rows spaced 6dp, no divider.

`UsageRing.kt`: replace `UsageRing` + `CompactArcGauge` with one `UsageArcGauge`; keep `UsageMetricTone` in `CompactUsageCardPresentation.kt`. Constants `ArcStartAngle = 135f`, `ArcSweepAngle = 270f`; no `useCenter` fill, no glow. The compact ring container carries `testTag("compact_ring_primary")`.
- Loading state: static 3-row skeleton using `track` blocks (no shimmer); when animators are enabled it may breathe at 1.6s, otherwise static.
- Compact equal-height rule: at 2+ columns use a fixed `compactCardHeight = 196.dp` with `Spacer(weight(1f))` before the footer so rows align; when compact falls back to 1 column (fontScale ≥ 1.3×) use wrap content. Long names stay single-line ellipsis and must not push the footer.
- Currency amount: render the symbol at `0.6em` of the numeric size via `buildAnnotatedString` + `SpanStyle(fontSize = ... * 0.6f)`, baseline-aligned; the numeric span keeps `tnum`.

`CompactUsageCardPresentation.arrangement`: `Balance`, `RingWithWindows`, `ValueTiles` (remove `HorizontalRings`/`VerticalRings`). Every progress card uses the first metric as the ring and all remaining metrics as mini rows.

`CredentialUsageCard.kt`:
- Header → `UsageCardIdentity`.
- Compact body:
  - Balance: `CompactBalanceLayout` (24sp amount, composition bar cat1/cat2, legend, model count).
  - RingWithWindows: `Row { UsageArcGauge(52) + label/reset; Column { window rows } }` — for 1 metric show ring + reset; for 2–3 show ring + secondary mini rows.
  - ValueTiles: 2-column grid without dividers (9dp vertical padding, 10dp column gap); labels use `caption`, values use `MyTokenType.numericValue`, suffixes use `micro`.
- Full body: keep `FullSplitUsageContent` behind `FullUsageCardBody` (Task 6 rewrites internals); footer uses `UsageCardFooter`.
- Remove `CardPalette`, `StatusPill`, `CircleIconButton` alpha backgrounds, `HorizontalDivider`, vertical divider boxes.

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.UsageCardHealthTest" --tests "ai.routin.mytoken.feature.home.HomeScreenTest" --tests "ai.routin.mytoken.feature.home.CredentialUsageSurfaceStyleTest" --tests "ai.routin.mytoken.feature.home.AndroidVisualRegressionTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/feature-home
git commit -m "feat(android): 统一用量卡片骨架与状态表达"
```

---

## Task 6: 重构完整模式分栏与指标网格（P1）

**Files:**
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/FullSplitUsageContent.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageMetricGrid.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/ProviderCatalog.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/ResetCardsMenuStyleTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/ProviderCatalogTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/OpenCodeUsageLayoutTest.kt`

**Interfaces:**
- Consumes: Task 5 skeleton/tokens.
- Produces:
  - `FullSplitUsageContent(card, metrics)` keeps its signature and all provider branches.
  - `UsageProgressBar(percent, color, modifier, height: Dp = 4.dp)`.
  - Delete `ProviderCatalog.accentColor` (unused in main; colors now come only from tokens).

- [ ] **Step 1: Add the failing composition/layout tests**

In `HomeScreenTest`:
```kotlin
@Test @Config(qualifiers = "w400dp-h1100dp")
fun multiCurrencyCompositionHandlesZeroSegments() {
    // 3 balances (CNY/USD/EUR), one zero granted segment
    // assert all currency amounts are displayed, "可用模型" count == 1, no crash
}
```
Assert `onAllNodesWithText("可用模型").fetchSemanticsNodes().size == 1` and each currency amount exists.

Update `ResetCardsMenuStyleTest` expectations: container `MyTokenThemeTokens.colors.surface` (or a helper returning it), border `colors.line`, radius `MyTokenRadius.inner`, width 210, row padding 12, no `HorizontalDivider`, value text `MyTokenType.micro`, tabular digits via `FontFamily.Monospace` (monospace digits are tabular).

Update `ProviderCatalogTest` / `OpenCodeUsageLayoutTest`: delete the `accentColor` distinctness tests (accent colors are no longer part of the design); keep logo mapping, display name, metric order, lifecycle tests.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.HomeScreenTest" --tests "ai.routin.mytoken.feature.home.ResetCardsMenuStyleTest"`
Expected: FAIL — multi-currency test missing/behavior; style test expects removed dividers.

- [ ] **Step 3: Rewrite `FullSplitUsageContent.kt`**

- `FullSplitLayout`: delete `drawBehind` vertical line; `Row(horizontalArrangement = Arrangement.spacedBy(18.dp))` with `weight(0.4f)` / `weight(0.6f)`; when `maxWidth < 280.dp` or `LocalDensity.fontScale >= 1.3f`, stack `Column` with 14dp spacing and no divider.
- `FullQuotaFocus`: label `label` muted + reset chip; percent `heroNumber` → `MyTokenType.displayCompact`; bar 4dp; remaining/reset `caption`.
- `FullWindowRow`: label `label`, percent `caption`/`label`, bar 3dp; remaining/reset `caption`; no row divider.
- `FullFacts`: remove `HorizontalDivider`; rows spaced 6dp; positive value uses `positiveText` only for subscription status, not for normal numbers.
- Balance composition: use `cat1` for topped-up/cash, `cat2` for granted/gift; legends use the same colors. Token composition: input `cat1`, output `cat2`, cache `cat3`.
- `FullCommandCodeSummary`: remove divider; 14dp top spacing; “额度构成” section label `caption` muted; values `caption` ink.
- `FullXiaomiUsage`: remove divider; 24dp section spacing; token legend 5dp dots with cat colors.
- `FullNewApiSplit`: keep window rows; realtime line separators removed, use 16dp gaps.
- `FullAccountStatus`: use `MyTokenStatus` semantics (dot + `caption`), not 9.5sp badge.

- [ ] **Step 4: Rewrite `UsageMetricGrid.kt` styles**

- `UsageProgressBar`: default height 4dp, track `colors.track`, fill given color, radius = height/2.
- `UsageMetricGrid`, `GLMMetrics`, `VolcengineMetrics`, `NewAPIMetrics`, `CommandCodeMetrics`, `XiaomiAPIMetrics`: replace inline font sizes/letterSpacing with `MyTokenType` tokens; remove card-internal `HorizontalDivider`s; use 14dp row spacing/18dp column gap.
- `ResetCardsBadge` menu: container `surface`, border `line`, radius `inner`, shadow allowed (overlay), 210dp width, 12dp padding, rows spaced 8dp, no dividers; expiry/date value `micro` with `tnum`.
- `StatusColors`: reduce to semantic accessors returning `MyTokenThemeTokens.colors` fills/texts; remove `Color(0xFF6F7782)` and any `MyTokenPalette` usage.
- `ProviderCatalog`: delete `accentColor`.

- [ ] **Step 5: Run tests**

Run:
```bash
cd android
./gradlew :feature-home:test
./gradlew test
```
Expected: PASS. Update any text assertion that changed only where the design explicitly changes copy.

- [ ] **Step 6: Commit**

```bash
git add android/feature-home
git commit -m "feat(android): 重构完整模式分栏与指标网格"
```

---

## Task 7: 重构凭证列表页（P2）

**Files:**
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialListScreen.kt`
- Test: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialListScreenTest.kt`
- Test: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialListAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenSurface`, `MyTokenSwitch`, `MyTokenStatus`, tokens.
- Produces: `CredentialListScreen` signature unchanged (`state`, callbacks, `layoutMode`, `drawerMode`).

**Tag contract:** keep `credential_list_content`, `credential_drawer_content`, `credential_add_button`, `credential_import_button`, `credential_enabled_summary`, `credential_more_${id}`. Add `credential_row_${id}`, `credential_disabled_${id}`, `credential_privacy_footer`.

- [ ] **Step 1: Update/write failing list tests**

- `rendersFlatRows`: assert `"2 个凭证 · 2 个启用"` instead of `"已启用 2 / 2"`.
- Add `disabledRowUsesNeutralLabelWithoutDimming`: disabled credential → `已停用` visible; row bounds height ≥ 64dp.
- Add `rowsExposeProviderAndKind`: text `Routin · Bearer Token` (or the localized kind label) exists once per row.
- Rename `editButtonIsAvailableAndKindDescriptionIsHidden` to `editButtonIsAvailableAndRowShowsKind`; drop the `assertCountEquals(0)` on `Bearer Token` because the design now shows provider · kind on each row. Keep the “编辑 主力 Key” menu assertion.
- Update `emptyStateShowsImportAndManualAddEntries`: `暂无凭证` → `还没有凭证`; keep `从 Mac 导入` / `添加凭证`.
- Add `emptyStateUsesBrandMarkAndTwoActions`: brand mark semantics + both action buttons remain reachable.
- `CredentialListAdaptiveTest` keeps the expanded 2-column grid assertion at 1000dp; add `drawerModeAlwaysUsesSingleColumn` at 1000dp with `drawerMode = true` and assert `credential_grid` absent.
- Keep delete/enable/tap tests and tags.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-credentials:test --tests "ai.routin.mytoken.feature.credentials.CredentialListScreenTest" --tests "ai.routin.mytoken.feature.credentials.CredentialListAdaptiveTest"`
Expected: FAIL — subtitle text, `已停用` label, drawer single-column behavior.

- [ ] **Step 3: Implement the list redesign**

- Header: title `MyTokenType.title`; subtitle `${total} 个凭证 · ${enabled} 个启用` (`label`, muted); right side QR icon 44dp touch and an “添加” pill (34dp visual height, wrapped in a 48dp touch area; `actionFill` text, `subtle` container, `line` border, plus icon) — tags preserved.
- Section line between header and list: `全部凭证` (`section`) + trailing `长按拖动排序` (`caption` muted), 16dp top / 8dp bottom.
- Row (`MyTokenSurface(surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card, shape = RoundedCornerShape(MyTokenRadius.row))`, `heightIn(min = MyTokenSize.listRow)`): 12dp horizontal padding; logo plate 36dp (9dp radius); name `MyTokenType.listTitle` + secondary line `${providerName} · ${kindLabel}` `caption` muted. Add `private fun kindLabel(kind: CredentialKind): String` in the file, mapping `BearerApiKey → "Bearer Token"`, `ApiKey → "API Key"`, `AccessKeyPair → "Access Key Pair"`. If disabled, a neutral `已停用` chip (`caption`, `subtle`, `line`) next to the name; right `MyTokenSwitch`; more button 48dp touch (`credential_more_${id}`), icon 18dp.
- Row whole area click → edit; switch → enable; more menu → 编辑 / 删除; long-press drag with 1.02 scale + haptic (keep existing reorderable).
- Empty state: brand mark 54dp + `还没有凭证` + primary “从 Mac 导入” + secondary “手动添加”.
- Footer: `凭证只保存在本机` caption centered, `navigationBarsPadding()` + 16dp bottom; tag `credential_privacy_footer`.
- Drawer: `drawerMode = true` forces 1 column, transparent background, bottom padding 24 + `navigationBarsPadding`, no back/close (scrim and swipe close).
- Delete dialog copy unchanged (“只影响本机数据”).

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :feature-credentials:test --tests "ai.routin.mytoken.feature.credentials.CredentialListScreenTest" --tests "ai.routin.mytoken.feature.credentials.CredentialListAdaptiveTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/feature-credentials
git commit -m "feat(android): 重构凭证列表页"
```

---

## Task 8: 重构新增/编辑凭证页（P2）

**Files:**
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorScreen.kt`
- Test: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorScreenTest.kt`
- Test: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenTextField`, `MyTokenButton`, `MyTokenSurface`, `MyTokenRow`, tokens.
- Produces: `CredentialEditorScreen` signature unchanged except a new optional `onDismissProviderSheet: (() -> Unit)? = null` is **not** needed; sheet state is internal.

**Tag contract:** keep `editor_form`, `editor_name`, `editor_api_key`, `editor_access_key_id`, `editor_secret_access_key`, `editor_region`, `editor_base_url`, `editor_user_id`, `editor_website_url`. Add `editor_provider_row`, `editor_provider_sheet`, `editor_provider_tile_${rawValue}`, `editor_test_result`.

- [ ] **Step 1: Update/write failing editor tests**

- `newEditorShowsProviderRowInsteadOfChips`: `editor_provider_row` exists; `Routin` tile only after opening the sheet.
- `providerSheetListsAllProvidersAndSelectionChangesFields`:
  - click `editor_provider_row`; assert `editor_provider_sheet` exists;
  - click `editor_provider_tile_volcengine`; assert sheet closes and `Access Key ID`, `Secret Access Key`, `区域（默认 cn-beijing）` appear.
- `connectionSuccessShowsSuccessMessage`: assert `连接成功` (no `✓`), and `editor_test_result` is displayed.
- `failedConnectionTestDisplaysFailureAndSaveStillPossible`: keep failure copy; assert `editor_test_result` exists.
- Keep validation/masking/save tests; update provider selection flow where needed.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-credentials:test --tests "ai.routin.mytoken.feature.credentials.CredentialEditorScreenTest"`
Expected: FAIL — provider row/sheet missing, success copy changed.

- [ ] **Step 3: Implement the editor redesign**

- Navigation: 44dp visual top bar with back in a 48dp touch box, inline title `新增凭证` / `编辑凭证` (`section`), no separate page title in the body.
- Intro: `选择供应商，填写最少的字段。` caption muted (new only).
- Provider row: `MyTokenSurface(surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card)` 64dp, logo 40dp, name `body` semibold, kind `caption` muted, “更换” `actionText`, chevron; click opens `ModalBottomSheet` (24dp top radius, grabber, 取消).
- Sheet: 2 columns × 4 rows of provider tiles (logo 26, name `label`, kind `caption`; never 10.5sp Chinese); selected tile 1px `actionFill` border + check icon; tag per tile. Selecting calls `onProviderChange` and closes.
- Edit mode: show `供应商 · 凭证类型` static line; provider not changeable.
- Form sections: `基本信息` (名称), `凭证` (per-provider fields), `更多（可选）` (官网地址), `安全说明` (`凭证加密保存在本机 Keystore，不会上传`). Keep the existing field logic exactly: Routin Plan Key; DeepSeek/GLM API Key; Volcengine Access Key ID + Secret Access Key + optional region (default cn-beijing); New API base URL + user ID + access token; Xiaomi usage kind + sk-/tp- key; Command Code/OpenCode console API key.
- Fields: `MyTokenTextField` with label above; 48dp input; secret masked with eye toggle; Xiaomi usage-kind segmented control (`API 按量` / `Token Plan`) using `MyTokenSegmented`.
- Validation: `validationError` under the credentials section, `caption` + `criticalText`; no banner/toast.
- Test result: line above the bottom actions (`editor_test_result`): 6dp dot + sentence; success `positiveText`, failure `criticalText`; keep “测试失败仍可保存…” helper.
- Bottom bar: `测试连接` neutral + `保存` primary, each 48dp, `navigationBarsPadding`; loading text unchanged (`测试中…` / `保存中…`).

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :feature-credentials:test --tests "ai.routin.mytoken.feature.credentials.CredentialEditorScreenTest" --tests "ai.routin.mytoken.feature.credentials.CredentialEditorAdaptiveTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/feature-credentials
git commit -m "feat(android): 重构凭证编辑页"
```

---

## Task 9: 重构设置页并加入抽屉关闭（P3）

**Files:**
- Modify: `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/SettingsScreen.kt`
- Modify: `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/RefreshSettingsScreen.kt`
- Modify: `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/NotificationSettingsScreen.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/MyTokenApp.kt:245-273`
- Test: `android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/SettingsScreenTest.kt`
- Test: `android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/SettingsScreenAdaptiveTest.kt`
- Test: `android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/NotificationSettingsSectionTest.kt`
- Test: `android/app/src/test/kotlin/ai/routin/mytoken/MyTokenAppTest.kt`

**Interfaces:**
- Consumes: `MyTokenRow`, `MyTokenSegmented`, `MyTokenStatus`, `MyTokenSection`, tokens.
- Produces: `SettingsScreen(..., onClose: (() -> Unit)? = null)`; `onClose` renders a 44dp `关闭设置` icon button (tag `settings_close`) when `drawerMode` and non-null. `MyTokenApp` passes `onClose = { drawerSide = null }`.

**Tag contract:** keep `settings_list`, `settings_drawer_content`, `settings_theme_cell`, `settings_refresh_cell`, `settings_migration_cell`, `settings_about_span`, `settings_about_toggle`, `settings_release_notes_toggle`, `settings_update_advanced_toggle`, `settings_about_version`, `settings_privacy_help`, `github_button`, `check_updates_button`, `download_update_button`, `install_permission_button`, `update_channel_selector`, `github_token_*`, `release_notes_refresh_button`, `release_history_button`. Remove `settings_migration_toggle` (the Data group is a direct row, not a disclosure). Add `settings_close`, `settings_permission_status`, `settings_export_logs`.

- [ ] **Step 1: Update/write failing settings tests**

- `SettingsScreenTest`:
  - `rendersRefreshAndDisplaySections`: keep 刷新/打开应用时刷新/失败后自动重试; add 外观/通知/数据/关于与更新.
  - `migrationEntryInvokesCallback` → `dataEntryInvokesTransferCallback`: replace the `expandDataMigration()` helper with `settings_list.performScrollToNode(hasText("数据"))`, then click “从 Mac 导入”; assert callback once (no expand step, no `settings_migration_toggle`).
  - `advancedSettingsAreCollapsedUntilRequested`: “从 Mac 导入” is now visible; remove that assertion, keep about details collapsed.
  - Add `drawerShowsCloseButton`: `drawerMode = true, onClose = { closed++ }`; `settings_close` displayed; click increments.
  - Add `dataGroupExportsLogs`: click `settings_export_logs`, assert `onExportLogs` called once.
  - Add `drawerSettingsSurvivesLargeFontAndGestureInsets`: `@Config(qualifiers = "w343dp-h800dp")` + fontScale 1.3; `settings_close` displayed; theme/refresh rows not clipped; `settings_list` bottom padding > 0.
- `NotificationSettingsSectionTest`: replace `系统通知权限：已允许` with `已允许`; replace `系统通知权限：未授予，提醒将静默` with `未授予`; keep `去授权`.
- `SettingsScreenAdaptiveTest`: keep `settings_theme_cell` / `settings_refresh_cell` / `settings_migration_cell` / `settings_about_span`; delete the `settings_migration_toggle` assertion; ensure theme/refresh remain in the same grid row.
- `MyTokenAppTest`: scroll to `数据` and assert `从 Mac 导入` instead of `数据迁移`/`扫码导入`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-settings:test --tests "ai.routin.mytoken.feature.settings.SettingsScreenTest" --tests "ai.routin.mytoken.feature.settings.NotificationSettingsSectionTest" --tests "ai.routin.mytoken.feature.settings.SettingsScreenAdaptiveTest"`
Expected: FAIL — group names, close button, permission copy.

- [ ] **Step 3: Implement the settings redesign**

- Header: 44dp row: title `设置` (`title`) + `settings_close` (48dp touch, close icon, contentDescription `关闭设置`) when drawer; then profile card: brand mark 36, `MyToken` + version, `凭证只在本机` caption, help entry.
- Groups in order: 外观 / 刷新 / 通知 / 数据 / 关于与更新.
- 外观: `MyTokenSegmented(["跟随系统","浅色","深色"], ...)`; no icon chips; 44dp.
- 刷新: two switch rows (`打开应用时刷新`, `失败后自动重试`) with `MyTokenRow` min 44dp; no dividers.
- 通知: switch row + permission row; granted: neutral `已允许`; denied: `warningFill` dot + `未授予` + `去授权` text button (48dp touch); tag `settings_permission_status`.
- 数据: two `MyTokenRow`s. `从 Mac 导入` (icon, supporting `扫码接收 Mac 凭证`, chevron, click `onOpenTransfer`) and `导出诊断日志` (tag `settings_export_logs`, supporting `生成已脱敏的本机日志`, chevron, click `onExportLogs`). `settings_migration_cell` stays on the group; the long-press-version shortcut remains.
- 关于与更新: version row (long-press export preserved, tag `settings_about_version`), check-update row/status, release-notes disclosure, update-source disclosure; keep all update states and buttons. Up-to-date status uses `muted`; an available version uses `actionText` + 6dp dot, matching the home update hint.
- Remove all `HorizontalDivider` and `surfaceVariant.copy(alpha = 0.62f)` surfaces; use `MyTokenSection` (surface + line) and 20dp section spacing.
- Bottom `navigationBarsPadding()`; drawer 1 column, non-drawer 2 columns at Medium+ (unchanged).
- `MyTokenApp.kt`: pass `onClose = { drawerSide = null }`.

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :feature-settings:test
./gradlew :app:test --tests "ai.routin.mytoken.MyTokenAppTest" --tests "ai.routin.mytoken.EdgeDrawerNavigationTest"
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android
git commit -m "feat(android): 重构设置页并加入抽屉关闭"
```

---

## Task 10: 重构卡片详情页（P3）

**Files:**
- Create: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialDetailHero.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialDetailScreen.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/AllowedModelsSection.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/CommandCodeDetailScreenTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/ModelChipDetailTest.kt`
- Test: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/CredentialDetailAdaptiveTest.kt`
- Test: create `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/CredentialDetailScreenTest.kt`

**Interfaces:**
- Consumes: `UsageArcGauge`, `MyTokenStatus`, `MyTokenSection`, `MyTokenRow`, `AllowedModelsSection`, tokens.
- Produces:
  - `CredentialDetailScreen(card: CredentialCardUi?, lowThresholdPercent: Int? = null, layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact, nextRefreshAt: Instant? = null, onBack, onRefresh, onEdit, onDelete)`.
  - `@Composable internal fun DetailHero(card: CredentialCardUi, nextRefreshAt: Instant? = null)`.
  - `@Composable internal fun DetailWindowsSection(card: CredentialCardUi)`.
  - `@Composable internal fun DetailFactsSection(card: CredentialCardUi)`.
  - `@Composable internal fun DetailConfigSection(metadata: Map<CredentialMetadataKey, String>, providerId: ProviderId, credentialKind: CredentialKind, lowThresholdPercent: Int?)`.

- [ ] **Step 1: Write/update failing detail tests**

- `CommandCodeDetailScreenTest`: section titles change to `全部窗口`, `套餐与订阅`, `账户与模型`, `配置`; keep individual values (`5 小时`, `周`, `月`, `购买剩余`, `赠送剩余`, `累计请求`, `有效`, `周期订阅`).
- `ModelChipDetailTest`: keep model chip assertions; `自动续费`/`Running` remain.
- Add:
```kotlin
@Test fun heroShowsUpdateTimeOnceAndNoDataTimeRow()
@Test fun failedDetailKeepsDataAndShowsRetry()
@Test fun balanceDetailUsesCompositionAndGroupsCurrencies()
@Test fun detailBottomActionsPutRefreshOnTheRightAsPrimary()
```
- `CredentialDetailAdaptiveTest` keeps 840dp width.
- Tag contract: `detail_content`, `detail_hero`, `detail_windows`, `detail_facts`, `detail_models`, `detail_config`, `detail_refresh_now`, `detail_edit`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.CommandCodeDetailScreenTest" --tests "ai.routin.mytoken.feature.home.CredentialDetailScreenTest"`
Expected: FAIL — section titles/tags missing.

- [ ] **Step 3: Implement the detail redesign**

Navigation: 44dp visual top bar, back/share/more in 48dp touch boxes, title `详情` (`section`) (delete + confirm unchanged).

Hero (`MyTokenSurface(surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card)`, 16dp padding):
- Row: logo 48 (13dp radius) + name `MyTokenType.detailTitle` + `${provider} · ${plan}` caption muted + `MyTokenStatus`.
- 88dp ring + primary window label + percent + `剩余 X` in `MyTokenType.detailRemaining` + reset text + reset-card chip (expanded dates inline, 48dp touch).
- Footer: `${HH:mm} 更新`; append `· 下次刷新 ${HH:mm}` only when `nextRefreshAt != null`. No “数据时间” row elsewhere.
- Balance providers: hero uses 32sp amount + composition bar (cat1/cat2) + legend + account status; secondary currencies repeat the same structure in the windows section.
- Failed: keep cached values; ring/bar use `track`; footer `warning/critical` line `更新失败 · 上次成功 HH:mm` + `重试` action.

`DetailWindowsSection`: title `全部窗口`; for each progress metric a row: label, percent, 5dp bar, `已用/总量`, `剩余`, reset time, reset-card full expiry (inline). No dividers; 14dp row spacing.

`DetailFactsSection`: `套餐与订阅` 2-column fact grid: 套餐 / 类型 / 状态 / 计费 / 开始 / 结束; for CommandCode also keep `额度构成` (月度/购买/赠送) and `累计请求` so `购买剩余` / `赠送剩余` / `累计请求` remain readable, using cat1/cat2/cat3 dots. `账户与模型`: group multipliers + `AllowedModelsSection`; `配置`: website link (clickable actionText), credential type, low threshold if present. No duplicate update time.

Bottom bar: `编辑` neutral (weight 1) + `立即刷新` primary (weight 1.5), 48dp, `navigationBarsPadding`.

Delete the old private `DetailHero`, `UsageMetricsSection`, `PlanDetailsSection`, `MetadataSection`, `DetailRow`, and `DetailColumn` from `CredentialDetailScreen.kt` once the new sections compile; keep `formatFullTime`, `credentialKindLabel`, `planName`, `usageType`, `subscriptionStatus`, `metadataLabel`, `metadataValue` (or move them to the new file unchanged).

`AllowedModelsSection`: chips use `subtle` container + `line` border + `ink`/`muted` text; copied state `actionText` + check; 8dp gaps; no per-chip alpha tint. Delete `ModelIdChipPalette` and `ModelIdChipPaletteTest` (no replacement; model chips are no longer per-ID tinted).

- [ ] **Step 4: Run tests**

Run:
```bash
cd android
./gradlew :feature-home:test
./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/feature-home
git commit -m "feat(android): 重构凭证详情页"
```

---

## Task 11: 动效、无障碍、视觉契约与收尾（P4）

**Files:**
- Create: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenMotion.kt`
- Create: `android/app/src/test/kotlin/ai/routin/mytoken/DesignTokenLintTest.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/MyTokenTypography.kt` (remove deprecated aliases)
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageRing.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialDetailHero.kt`
- Modify: `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/SettingsScreen.kt`
- Modify: `docs/superpowers/specs/2026-11-21-mytoken-android-visual-refresh-design.md` (add `## 17. 实现备注` with deviations)

**Interfaces:**
- Consumes: everything above.
- Produces:
  - `object MyTokenMotion { const val pressMillis = 100; const val modeSwitchMillis = 320; const val ringMillis = 500; const val locateMillis = 300; const val starMillis = 2400; fun ringDurationMillis(enabled: Boolean): Int; fun locateDurationMillis(enabled: Boolean): Int; fun modeSwitchDurationMillis(enabled: Boolean): Int }` — each returns `0` when `enabled` is false.
  - `@Composable fun rememberMyTokenMotionEnabled(): Boolean` — uses `ValueAnimator.areAnimatorsEnabled()`.
  - `@Composable fun Modifier.myTokenPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.99f): Modifier` (cards 0.99, buttons 0.97).

- [ ] **Step 1: Write failing motion/lint/accessibility tests**

`DesignTokenLintTest` (app module, plain JUnit; `user.dir` is `android/app`, so scan `File(user.dir).parentFile`, i.e. `android/`):
- `noInlineColorsOutsideCoreUi`: no `Color(0x` outside `core-ui` and `app/**/widget/**`.
- `noInlineFontSizeOrTrackingInFeatureCode`: no `fontSize =` / `letterSpacing =` in `feature-*/src/main`.
- `noAlphaCardSurfaces`: no `MaterialTheme.colorScheme.surface.copy(alpha` in `feature-*/src/main`.
- `noGlassIdentifiers`: no `GlassButton|GlassSwitch|GlassTextField|GlassHelpButton|LiquidGlassSurface` anywhere in `src/main`.
- `noLegacyPalette`: no `MyTokenPalette` references under `android/**/src/main`.

Add Compose tests:
- `UsageCardHealthTest` extended for `rememberMyTokenMotionEnabled`? Motion policy pure:
```kotlin
@Test fun motionDurationsCollapseWhenDisabled() {
    assertEquals(500, MyTokenMotion.ringDurationMillis(enabled = true))
    assertEquals(0, MyTokenMotion.ringDurationMillis(enabled = false))
}
```
- `HomeScreenTest`: `touchTargetsAreAtLeast48` — bounds for `credential_refresh_${id}`, `provider_filter_menu`, `health_segment_0`, `home_open_credentials`, `home_open_settings` ≥ 48dp (or ≥44dp for density options; list exact allowed exceptions).
- `CredentialDetailScreenTest`: `detailShareAndMoreAre48`.
- Card semantics test: `onNodeWithContentDescription("主账号，正常，42%", substring = true)` exists.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew :app:test --tests "ai.routin.mytoken.DesignTokenLintTest" && ./gradlew :feature-home:test --tests "ai.routin.mytoken.feature.home.HomeScreenTest"`
Expected: FAIL — inline colors/font sizes remain, tags not 48dp, motion helper unresolved.

- [ ] **Step 3: Apply motion and accessibility**

- `MyTokenMotion`: durations as above; `fun ringDurationMillis(enabled: Boolean) = if (enabled) ringMillis else 0`; same for locate/mode switch.
- `UsageArcGauge`: animate percent from old to new with `animateFloatAsState(animationSpec = tween(if (enabled) 500 else 0, easing = FastOutSlowInEasing))`.
- Mode switch: underline 240ms; card reflow spring(damping 1, response 0.35) — wrap grid content in `AnimatedContent` keyed by density with the 320ms spec, cancellable.
- Locate highlight: 1.02 scale + 4dp lift, 300ms, no bounce (replace the current keyframes bounce).
- Press: cards `.99`, buttons `.97` via `myTokenPressScale`; ripple retained.
- Reduced motion: when `rememberMyTokenMotionEnabled()` is false, only fades; disable star breathing.
- Icons: prefer Material outlined/rounded glyphs at 18–20dp, tinted `ink` or `muted`; no colored icons except provider logos; chevrons and status dots are not standalone semantic elements.
- TalkBack: card contentDescription `${name}，${statusText}，${primaryText}`; health segments `${name}，${label}`; reset chips `${count} 张重置卡` (existing); switches keep `contentDescription`; filter pill `供应商筛选，当前全部`; detail share/more names.
- Dynamic font: verify compact 1-column fallback (Task 4) and full split stacking (Task 6) still pass at 1.3×.

- [ ] **Step 4: Remove deprecated type aliases and remaining inline styles**

- Search `MyTokenPalette`; replace every reference with `MyTokenThemeTokens.colors` (Tasks 5/6/10 should already have removed them) and delete the compatibility object from `MyTokenVisualPolicy.kt`.
- Search `MyTokenType.pageTitle|sectionTitle|cardTitle|heroNumber|balanceNumber|metricLabel|metricCaption|factLabel|factValue|badge|sync`; replace each with the closest new token:
  - pageTitle → title
  - sectionTitle/cardTitle → section
  - heroNumber/balanceNumber → display or displayCompact
  - metricLabel/factLabel → label
  - metricValue → label
  - metricCaption/factValue → caption
  - badge/sync → micro or caption (Latin/digits only for micro)
- Delete the deprecated aliases from `MyTokenTypography.kt`.
- Remove `ProviderCatalog.accentColor` tests if not already removed; remove `ModelIdChipPalette` if no longer referenced.

- [ ] **Step 5: Run the full suite and build**

Run:
```bash
cd android
./gradlew test
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
```
Expected: PASS.

- [ ] **Step 6: Manual device acceptance**

On a 390dp phone (OriginOS/vivo or Xiaomi), light and dark:
- Home: 6 credentials covering all 8 archetypes; compact and full; pull-to-refresh; health ruler locate; filter menu; update hint.
- Font scale 1.3×: compact 1 column, full split stacks, no clipped text.
- Reduce motion: no breathing star, only fades.
- TalkBack: card summary, ruler segments, switches, reset chips, filter.
- Credential drawer: list 1 column, drag reorder, enable/disable, delete.
- Editor: provider sheet, fields per provider, test result inline, save.
- Settings drawer: close ×, theme switching, permission status, export logs, check update.
- Detail: all windows, reset cards inline, facts, models copy, share, delete, refresh.
- Visual review: screenshot at 100%, then grayscale and 10%-scale/blur-10px versions; the 3 most important numbers must still be readable; hue count ≤ 3; no translucent surface stacked on another.
- Security: grep logcat for the test secret; must be absent; diagnostics remain redacted.

- [ ] **Step 7: Update the spec with implementation notes and commit**

Add `## 17. 实现备注` to the spec:
- `nextRefreshAt` is only rendered when supplied; current app state has no scheduled refresh time, so detail shows update time only.
- Health help entry removed; status row + ruler carry the explanation.
- §5.2 wording updated: `LiquidGlassSurface` is not only changed to a solid implementation but renamed to `MyTokenSurface` in P0, matching §12.1 and §15.2.
- Any provider-specific layout deviation discovered in P1.

```bash
git add android docs/superpowers/specs
git commit -m "chore(android): 收尾动效、无障碍与视觉契约检查"
```

---

## 验收对照（Definition of Done）

- [ ] `./gradlew test`、`:app:assembleDebug`、`:app:assembleDebugAndroidTest` 全绿。
- [ ] 首页独立色相 ≤ 3；正常状态没有绿色文字/徽章；颜色只出现在弧线、细条、构成条和异常句。
- [ ] 任一屏字号 ≤ 6 种；没有 9–10.5sp 的中文；所有数字 `tnum`。
- [ ] 没有半透明表面叠半透明表面；卡片有 1px 边界；卡片内部没有分割线。
- [ ] 8 类内容原型 × 2 种密度 × 6 种状态都能渲染；provider 排序规则不变。
- [ ] 列表启停/排序/删除、编辑器字段/测试连接、设置全部可回写、详情分享/删除/刷新/编辑均可用。
- [ ] 明暗、1.3× 字体、减少动态、TalkBack、48dp 触控全部通过。
- [ ] 未改动 `shared/`、provider 协议、迁移格式、Keystore/日志脱敏路径；测试夹具中没有真实密钥。
