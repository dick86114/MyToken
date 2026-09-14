# Android 平板与阔折叠适配 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 MyToken Android 客户端按窗口实际可用宽度，在手机、横屏设备、分屏、平板和阔折叠上使用正确的导航、列数和内容宽度。

**Architecture:** 在 `core-ui` 建立唯一的窗口尺寸类映射、列数计算、限宽容器和玻璃侧栏；根导航只切换底栏与侧栏，不改变现有 Pager、页面状态或返回栈；各 Feature 使用统一的布局模式调整列表、网格、详情和迁移页面。

**Tech Stack:** Kotlin 2.0.21、Jetpack Compose、Material 3、Compose BOM 2024.09.03、`androidx.compose.material3.adaptive:adaptive` 1.0.0、`androidx.window:window-core` 1.3.0、reorderable 3.1.0、Robolectric 4.13、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-09-14-android-adaptive-tablet-foldable-design.md`

## Global Constraints

- Android 最低支持 Android 10（API 29）。
- 断点固定为：`Compact < 600dp`、`600dp <= Medium < 840dp`、`Expanded >= 840dp`。
- 断点只由窗口实际可用宽度决定，不按设备型号、物理屏幕或折叠角度硬编码。
- `Compact` 使用现有底部导航；一级页面的 `Medium` 和 `Expanded` 使用左侧导航。
- 详情、编辑、迁移等非一级页面在所有宽度下继续隐藏主导航。
- 首页最大列数分别为 `1/2/3`；首页卡片最小宽度为 `260dp`。
- 凭证页在 `Expanded` 使用可拖拽排序的双列网格，排序仍是唯一线性顺序。
- 设置页在 `Medium` 和 `Expanded` 使用双列网格，“关于”横跨两列。
- 详情内容最大宽度为 `840dp`；编辑表单最大宽度为 `720dp`；空状态最大宽度为 `480dp`。
- 不改变 ViewModel、Repository、Room、DataStore、WorkManager、迁移协议和敏感数据存储规则。
- 不改变现有 `contentDescription` 语义和业务回调。
- 所有新增注释、测试名和提交信息使用中文。
- 每个任务先写失败测试，再写最小实现；任务结束必须运行对应测试并提交一次 Conventional Commit。
- Android 命令统一在 `android/` 目录执行。

---

## 文件地图

### `core-ui`

- Modify `android/core-ui/build.gradle.kts`
- Create `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayout.kt`
- Create `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveNavigation.kt`
- Modify `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt`
- Create `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayoutTest.kt`

### `app`

- Modify `android/app/src/main/kotlin/ai/routin/mytoken/MyTokenApp.kt`
- Create `android/app/src/test/kotlin/ai/routin/mytoken/MyTokenAppAdaptiveTest.kt`

### `feature-home`

- Modify `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/HomeScreen.kt`
- Modify `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt`
- Modify `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageMetricGrid.kt`
- Modify `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialDetailScreen.kt`
- Create `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenAdaptiveTest.kt`
- Create `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/CredentialDetailAdaptiveTest.kt`

### `feature-credentials`

- Modify `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialListScreen.kt`
- Modify `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorScreen.kt`
- Create `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialListAdaptiveTest.kt`
- Create `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorAdaptiveTest.kt`

### `feature-settings`

- Modify `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/SettingsScreen.kt`
- Create `android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/SettingsScreenAdaptiveTest.kt`

### `feature-transfer`

- Modify `android/feature-transfer/build.gradle.kts`
- Modify `android/feature-transfer/src/main/kotlin/ai/routin/mytoken/feature/transfer/TransferPreviewScreen.kt`
- Modify `android/feature-transfer/src/main/kotlin/ai/routin/mytoken/feature/transfer/TransferScannerScreen.kt`
- Create `android/feature-transfer/src/test/kotlin/ai/routin/mytoken/feature/transfer/TransferAdaptiveTest.kt`

### 文档与验收

- Modify `docs/android/README.md`
- Modify `docs/superpowers/specs/2026-09-14-android-adaptive-tablet-foldable-design.md`

---

### Task 1: 建立响应式尺寸与内容宽度基础

**Files:**
- Modify: `android/core-ui/build.gradle.kts`
- Create: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayout.kt`
- Create: `android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayoutTest.kt`

**Interfaces:**
- Consumes: `androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()`、`androidx.window.core.layout.WindowWidthSizeClass`。
- Produces:
  - `enum class MyTokenLayoutMode { Compact, Medium, Expanded }`
  - `@Composable fun rememberMyTokenLayoutMode(): MyTokenLayoutMode`
  - `fun MyTokenLayoutMode.maxColumns(compact: Int, medium: Int, expanded: Int): Int`
  - `fun adaptiveGridColumns(availableWidth: Dp, maxColumns: Int, minItemWidth: Dp, spacing: Dp): Int`
  - `@Composable fun MyTokenAdaptiveContent(maxWidth: Dp, modifier: Modifier = Modifier, horizontalPadding: Dp = 16.dp, content: @Composable BoxScope.() -> Unit)`

- [ ] **Step 1: 写入断点和列数失败测试**

创建 `AdaptiveLayoutTest.kt`：

```kotlin
package ai.routin.mytoken.core.ui

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun 窗口宽度映射到三档布局模式() {
        assertEquals(
            MyTokenLayoutMode.Compact,
            WindowSizeClass.compute(599f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Medium,
            WindowSizeClass.compute(600f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Medium,
            WindowSizeClass.compute(839f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Expanded,
            WindowSizeClass.compute(840f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
    }

    @Test
    fun 网格列数按可用宽度限制() {
        assertEquals(1, adaptiveGridColumns(484.dp, 2, 260.dp, 12.dp))
        assertEquals(2, adaptiveGridColumns(596.dp, 2, 260.dp, 12.dp))
        assertEquals(3, adaptiveGridColumns(808.dp, 3, 260.dp, 12.dp))
        assertEquals(2, adaptiveGridColumns(808.dp, 3, 400.dp, 12.dp))
    }

    @Test
    fun 布局模式为各页面提供列数上限() {
        assertEquals(1, MyTokenLayoutMode.Compact.maxColumns(1, 2, 3))
        assertEquals(2, MyTokenLayoutMode.Medium.maxColumns(1, 2, 3))
        assertEquals(3, MyTokenLayoutMode.Expanded.maxColumns(1, 2, 3))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :core-ui:testDebugUnitTest --tests "ai.routin.mytoken.core.ui.AdaptiveLayoutTest"
```

Expected: 编译失败，提示 `MyTokenLayoutMode`、`toMyTokenLayoutMode` 或 `adaptiveGridColumns` 不存在。

- [ ] **Step 3: 增加自适应依赖**

修改 `android/core-ui/build.gradle.kts`：

```kotlin
dependencies {
    api(platform("androidx.compose:compose-bom:2024.09.03"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.material3:material3")
    api("androidx.compose.material3.adaptive:adaptive")
    api("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 4: 实现尺寸模式、列数和限宽容器**

创建 `AdaptiveLayout.kt`：

```kotlin
package ai.routin.mytoken.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import kotlin.math.floor

@Immutable
enum class MyTokenLayoutMode {
    Compact,
    Medium,
    Expanded,
}

internal fun WindowWidthSizeClass.toMyTokenLayoutMode(): MyTokenLayoutMode = when (this) {
    WindowWidthSizeClass.COMPACT -> MyTokenLayoutMode.Compact
    WindowWidthSizeClass.MEDIUM -> MyTokenLayoutMode.Medium
    WindowWidthSizeClass.EXPANDED -> MyTokenLayoutMode.Expanded
    else -> MyTokenLayoutMode.Compact
}

fun MyTokenLayoutMode.maxColumns(
    compact: Int,
    medium: Int,
    expanded: Int,
): Int = when (this) {
    MyTokenLayoutMode.Compact -> compact
    MyTokenLayoutMode.Medium -> medium
    MyTokenLayoutMode.Expanded -> expanded
}

fun adaptiveGridColumns(
    availableWidth: Dp,
    maxColumns: Int,
    minItemWidth: Dp,
    spacing: Dp,
): Int {
    if (maxColumns <= 1 || availableWidth <= 0.dp || minItemWidth <= 0.dp) return 1
    val usable = availableWidth - spacing * (maxColumns - 1)
    val fitting = floor((usable / minItemWidth).toDouble()).toInt()
    return fitting.coerceIn(1, maxColumns)
}

@Composable
fun rememberMyTokenLayoutMode(): MyTokenLayoutMode =
    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass.toMyTokenLayoutMode()

@Composable
fun MyTokenAdaptiveContent(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize(),
            content = content,
        )
    }
}
```

说明：`adaptiveGridColumns` 先扣除列间距，再按最小项目宽度计算可放下几列；不足一列时回退为单列。

- [ ] **Step 5: 运行定向测试**

Run:

```bash
cd android
./gradlew :core-ui:testDebugUnitTest --tests "ai.routin.mytoken.core.ui.AdaptiveLayoutTest"
```

Expected: `AdaptiveLayoutTest` 全部 PASS。

- [ ] **Step 6: 提交**

```bash
git add android/core-ui/build.gradle.kts \
  android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayout.kt \
  android/core-ui/src/test/kotlin/ai/routin/mytoken/core/ui/AdaptiveLayoutTest.kt
git commit -m "feat(android): 建立响应式布局基础"
```

---

### Task 2: 增加玻璃侧栏并接入根导航

**Files:**
- Create: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveNavigation.kt`
- Modify: `android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt`
- Modify: `android/app/src/main/kotlin/ai/routin/mytoken/MyTokenApp.kt`
- Create: `android/app/src/test/kotlin/ai/routin/mytoken/MyTokenAppAdaptiveTest.kt`

**Interfaces:**
- Consumes: Task 1 的 `MyTokenLayoutMode`、现有 `GlassNavItem`、`LiquidGlassSurface`。
- Produces:
  - `@Composable fun LiquidGlassNavigationRail(items: List<GlassNavItem>, expanded: Boolean, modifier: Modifier = Modifier)`
  - `@Composable fun MyTokenNavigationScaffold(layoutMode: MyTokenLayoutMode, showNavigation: Boolean, items: List<GlassNavItem>, content: @Composable (PaddingValues) -> Unit)`
  - 稳定测试标识：`bottom_navigation`、`navigation_rail`、`navigation_rail_expanded`。

- [ ] **Step 1: 写入根导航失败测试**

创建 `MyTokenAppAdaptiveTest.kt`：

```kotlin
package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyTokenAppAdaptiveTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun 紧凑宽度使用底栏并隐藏侧栏() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_navigation").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 中等宽度使用紧凑侧栏并隐藏底栏() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 展开宽度使用展开侧栏并隐藏底栏() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("navigation_rail_expanded").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests "ai.routin.mytoken.MyTokenAppAdaptiveTest"
```

Expected: FAIL，找不到 `navigation_rail` 或 `navigation_rail_expanded`。

- [ ] **Step 3: 给现有底栏增加稳定测试标识**

在 `Components.kt` 的 `LiquidGlassBottomBar` 外层 `Box` 上增加：

```kotlin
Box(
    modifier = modifier
        .fillMaxWidth()
        .testTag("bottom_navigation"),
) {
```

新增导入：

```kotlin
import androidx.compose.ui.platform.testTag
```

- [ ] **Step 4: 实现玻璃侧栏和根导航壳**

创建 `AdaptiveNavigation.kt`：

```kotlin
package ai.routin.mytoken.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassNavigationRail(
    items: List<GlassNavItem>,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val railWidth = if (expanded) 160.dp else 72.dp
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Start + WindowInsetsSides.Vertical,
                )
            )
            .padding(8.dp)
            .testTag(if (expanded) "navigation_rail_expanded" else "navigation_rail"),
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .width(railWidth)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEach { item ->
                    val color = if (item.selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = item.onClick,
                            )
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (expanded) {
                            Arrangement.spacedBy(12.dp)
                        } else {
                            Arrangement.Center
                        },
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            tint = color,
                            modifier = Modifier.size(24.dp),
                        )
                        if (expanded) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyTokenNavigationScaffold(
    layoutMode: MyTokenLayoutMode,
    showNavigation: Boolean,
    items: List<GlassNavItem>,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNavigation && layoutMode == MyTokenLayoutMode.Compact) {
                LiquidGlassBottomBar(items = items)
            }
        },
    ) { padding ->
        if (showNavigation && layoutMode != MyTokenLayoutMode.Compact) {
            Row(modifier = Modifier.fillMaxSize()) {
                LiquidGlassNavigationRail(
                    items = items,
                    expanded = layoutMode == MyTokenLayoutMode.Expanded,
                )
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    content(padding)
                }
            }
        } else {
            content(padding)
        }
    }
}
```

`indication = null` 与现有底栏的 `ripple` 不同；如果编译器允许 `null`，直接使用；若当前 Compose 版本不接受，改用 `ripple(color = color)`，不要额外引入新依赖。

- [ ] **Step 5: 将 `MyTokenApp` 接入统一导航壳**

在 `MyTokenApp.kt` 中：

1. 删除根 `Scaffold` 的 `bottomBar` 分支。
2. 在 `selectTab` 定义之后、根导航壳之前加入：

```kotlin
val layoutMode = rememberMyTokenLayoutMode()
val isRootScreen = screen == AppScreen.Home ||
    screen == AppScreen.Credentials ||
    screen == AppScreen.Settings
val navigationItems = listOf(
    GlassNavItem("首页", Icons.Filled.Home, "首页", selectedTab == 0) { selectTab(0) },
    GlassNavItem("凭证", WalletCardsIcon, "凭证", selectedTab == 1) { selectTab(1) },
    GlassNavItem("设置", Icons.Filled.Settings, "设置", selectedTab == 2) { selectTab(2) },
)
```

3. 将原 `Scaffold(...) { padding -> ... }` 替换为：

```kotlin
MyTokenNavigationScaffold(
    layoutMode = layoutMode,
    showNavigation = isRootScreen,
    items = navigationItems,
) { padding ->
    Box(
        modifier = Modifier
            .padding(
                PaddingValues(
                    start = padding.calculateStartPadding(LayoutDirection.Ltr),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(LayoutDirection.Ltr),
                )
            )
            .fillMaxSize(),
    ) {
        // 保留现有 if (isRootScreen) HorizontalPager else when (screen) 内容。
    }
}
```

4. 本任务不要提前给 `HomeScreen`、`CredentialListScreen`、`SettingsScreen` 传新增参数；它们的 `layoutMode` 参数会在 Task 3 到 Task 5 中各自加入。

- [ ] **Step 6: 运行根导航测试并修复编译面**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest \
  --tests "ai.routin.mytoken.MyTokenAppTest" \
  --tests "ai.routin.mytoken.MyTokenAppAdaptiveTest"
```

Expected: 手机测试继续使用 `bottom_navigation`，`w700dp` 使用 `navigation_rail`，`w1000dp` 使用 `navigation_rail_expanded`，全部 PASS。

- [ ] **Step 7: 提交**

```bash
git add android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/AdaptiveNavigation.kt \
  android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt \
  android/app/src/main/kotlin/ai/routin/mytoken/MyTokenApp.kt \
  android/app/src/test/kotlin/ai/routin/mytoken/MyTokenAppAdaptiveTest.kt
git commit -m "feat(android): 适配平板与阔折叠导航"
```

---

### Task 3: 适配首页网格与详情限宽

**Files:**
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/HomeScreen.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/UsageMetricGrid.kt`
- Modify: `android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialDetailScreen.kt`
- Create: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/HomeScreenAdaptiveTest.kt`
- Create: `android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home/CredentialDetailAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenLayoutMode`、`rememberMyTokenLayoutMode()`、`adaptiveGridColumns()`、`MyTokenAdaptiveContent()`。
- Produces:
  - `HomeScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - `CredentialUsageCard(..., metricColumns: Int = 2)`
  - 稳定测试标识：`home_grid_1_columns`、`home_grid_2_columns`、`home_grid_3_columns`、`detail_content`。

- [ ] **Step 1: 写入首页列数失败测试**

在 `HomeScreenAdaptiveTest.kt` 中复用现有 `HomeScreenTest` 的卡片构造函数，并通过真实窗口尺寸类驱动布局：

```kotlin
package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.core.ui.rememberMyTokenLayoutMode
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent() {
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(
                            sampleCard("Routin 主账号", ai.routin.mytoken.domain.model.ProviderId.Routin),
                            sampleCard("DeepSeek 主账号", ai.routin.mytoken.domain.model.ProviderId.DeepSeek),
                            sampleCard("GLM 主账号", ai.routin.mytoken.domain.model.ProviderId.Glm),
                        ),
                        groups = emptyList(),
                        credentialCount = 3,
                    ),
                    layoutMode = rememberMyTokenLayoutMode(),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun 手机使用单列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_1_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 阔折叠或横屏使用双列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_2_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 平板使用三列首页() {
        setContent()
        composeRule.onNodeWithTag("home_grid_3_columns").assertExists()
    }
}
```

在测试类之后加入自包含的卡片夹具：

```kotlin
private fun sampleCard(name: String, provider: ProviderId): CredentialCardUi {
    val credential = Credential(
        id = UUID.randomUUID(),
        providerId = provider,
        credentialKind = CredentialKind.BearerApiKey,
        name = name,
    )
    val now = Instant.parse("2026-09-14T00:00:00Z")
    val metric = UsageMetric(
        id = "fiveHour",
        label = "5 小时",
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Normal,
    )
    return CredentialCardUi(
        credential = credential,
        status = RefreshStatus.Ready,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = now,
            metrics = listOf(metric),
            planName = "测试套餐",
        ),
        isStale = false,
        error = null,
        freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :feature-home:testDebugUnitTest --tests "ai.routin.mytoken.feature.home.HomeScreenAdaptiveTest"
```

Expected: 编译失败，提示 `HomeScreen` 没有 `layoutMode` 参数或找不到列数测试标识。

- [ ] **Step 3: 将首页列表改为按宽度计算的网格**

在 `HomeScreen.kt` 中：

1. 增加导入：

```kotlin
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import ai.routin.mytoken.core.ui.maxColumns
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.testTag
```

2. 增加参数：

```kotlin
layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
```

3. 将 `else -> Column` 中的筛选区和 `LazyColumn` 替换为：

```kotlin
else -> MyTokenAdaptiveContent(
    modifier = Modifier.fillMaxSize(),
    maxWidth = 1440.dp,
    horizontalPadding = 16.dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = adaptiveGridColumns(
            availableWidth = maxWidth,
            maxColumns = layoutMode.maxColumns(1, 2, 3),
            minItemWidth = 260.dp,
            spacing = 12.dp,
        )
        val metricColumns = if (columns == 1) 2 else 1
        Column(modifier = Modifier.fillMaxSize()) {
            if (layoutMode == MyTokenLayoutMode.Compact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProviderFilterChips(
                        selectedProvider = selectedProvider,
                        visibleProviders = visibleProviders,
                        onSelect = { selectedProvider = it },
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ProviderFilterChips(
                        selectedProvider = selectedProvider,
                        visibleProviders = visibleProviders,
                        onSelect = { selectedProvider = it },
                    )
                }
            }

            LazyVerticalGrid(
                state = rememberLazyGridState(),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_grid_${columns}_columns"),
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = if (layoutMode == MyTokenLayoutMode.Compact) 96.dp else 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(visibleCards, key = { it.credential.id }) { card ->
                    CredentialUsageCard(
                        card = card,
                        metricColumns = metricColumns,
                        onOpen = { onOpenCredential(card.credential.id) },
                        onRetry = { onRefreshCredential(card.credential.id) },
                    )
                }
            }
        }
    }
}
```

4. 把原筛选 chips 抽成 `ProviderFilterChips`，并提供：

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderFilterChips(
    selectedProvider: ProviderId?,
    visibleProviders: List<ProviderId>,
    onSelect: (ProviderId?) -> Unit,
) {
    FilterChip(
        selected = selectedProvider == null,
        onClick = { onSelect(null) },
        label = { Text("全部") },
        colors = glassFilterChipColors(selectedProvider == null),
        border = glassFilterChipBorder(selectedProvider == null),
    )
    visibleProviders.forEach { provider ->
        FilterChip(
            selected = selectedProvider == provider,
            onClick = {
                onSelect(if (selectedProvider == provider) null else provider)
            },
            label = { Text(ProviderCatalog.displayName(provider)) },
            colors = glassFilterChipColors(selectedProvider == provider),
            border = glassFilterChipBorder(selectedProvider == provider),
        )
    }
}
```

- [ ] **Step 4: 让卡片内指标在窄卡片中降为单列**

在 `CredentialUsageCard.kt` 中增加：

```kotlin
metricColumns: Int = 2,
```

并按以下方式传递：

```kotlin
when (card.credential.providerId) {
    ProviderId.Glm -> GLMMetrics(metrics, columns = metricColumns)
    ProviderId.NewAPI -> NewAPIMetrics(metrics, columns = metricColumns)
    ProviderId.Volcengine -> VolcengineMetrics(metrics, columns = metricColumns)
    ProviderId.CommandCode -> CommandCodeMetrics(metrics, columns = metricColumns)
    else -> UsageMetricGrid(metrics = metrics, columns = metricColumns)
}
```

在 `UsageMetricGrid.kt` 中为 `GLMMetrics`、`VolcengineMetrics`、`NewAPIMetrics`、`CommandCodeMetrics` 增加 `columns: Int = 2`，并把固定 `chunked(2)`、两列 `Row` 改为 `chunked(columns)` 和 `repeat(columns - row.size)`。`CommandCodeMetrics` 在 `columns == 1` 时按 5 小时、周、月、累计请求、购买剩余、赠送剩余的顺序纵向排列。

- [ ] **Step 5: 详情页增加居中限宽**

在 `CredentialDetailScreen.kt` 中：

1. 内容 `Column` 改为：

```kotlin
MyTokenAdaptiveContent(
    maxWidth = 840.dp,
    horizontalPadding = 16.dp,
    modifier = Modifier.padding(padding).fillMaxSize(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
            .testTag("detail_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 保留现有详情分区。
    }
}
```

2. 底部操作条改为居中限宽：

```kotlin
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 保留现有刷新、签到、编辑、删除按钮。
    }
}
```

- [ ] **Step 6: 写入详情限宽失败测试**

在 `CredentialDetailAdaptiveTest.kt` 中：

```kotlin
@Test
@Config(qualifiers = "w1000dp-h1100dp")
fun 平板详情限制最大阅读宽度() {
    setDetailContent()
    composeRule.onNodeWithTag("detail_content")
        .assertWidthIsAtMost(840.dp)
}
```

Run:

```bash
cd android
./gradlew :feature-home:testDebugUnitTest \
  --tests "ai.routin.mytoken.feature.home.HomeScreenAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.home.CredentialDetailAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.home.HomeScreenTest" \
  --tests "ai.routin.mytoken.feature.home.CommandCodeDetailScreenTest" \
  --tests "ai.routin.mytoken.feature.home.ModelChipDetailTest"
```

Expected: 全部 PASS；手机尺寸现有测试保持通过。

- [ ] **Step 7: 提交**

```bash
git add android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home \
  android/feature-home/src/test/kotlin/ai/routin/mytoken/feature/home
git commit -m "feat(android): 适配首页网格与详情宽度"
```

---

### Task 4: 适配凭证列表、拖拽网格与编辑表单

**Files:**
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialListScreen.kt`
- Modify: `android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorScreen.kt`
- Create: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialListAdaptiveTest.kt`
- Create: `android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials/CredentialEditorAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenLayoutMode`、`MyTokenAdaptiveContent()`、reorderable 3.1.0 的 `rememberReorderableLazyGridState()`。
- Produces:
  - `CredentialListScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - `CredentialEditorScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - 稳定测试标识：`credential_list`、`credential_grid`、`editor_form`。

- [ ] **Step 1: 写入凭证列表失败测试**

在 `CredentialListAdaptiveTest.kt` 中：

```kotlin
@Test
@Config(qualifiers = "w1000dp-h1100dp")
fun 平板使用可拖拽双列网格() {
    setContent()
    composeRule.onNodeWithTag("credential_grid").assertExists()
    composeRule.onNodeWithContentDescription("长按拖动排序").assertExists()
}
```

`setContent` 使用两个真实 `Credential` 和 `CredentialListScreen(layoutMode = MyTokenLayoutMode.Expanded, ...)`，避免依赖 Activity 当前尺寸。

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :feature-credentials:testDebugUnitTest --tests "ai.routin.mytoken.feature.credentials.CredentialListAdaptiveTest"
```

Expected: 编译失败，提示 `CredentialListScreen` 没有 `layoutMode` 参数或找不到 `credential_grid`。

- [ ] **Step 3: 实现 `Expanded` 双列拖拽网格**

在 `CredentialListScreen.kt` 中增加：

先补充导入：

```kotlin
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.testTag
import sh.calvin.reorderable.rememberReorderableLazyGridState
```

已有 `androidx.compose.foundation.lazy.itemsIndexed` 保持不变，网格使用 `gridItemsIndexed` 避免重名。

再增加参数：

```kotlin
layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
```

将列表内容拆分为共享行内容：

```kotlin
@Composable
private fun CredentialItemContent(
    credential: Credential,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    CredentialRow(
        credential = credential,
        providerName = ProviderNames.displayName(credential.providerId),
        elevation = if (isDragging) 8.dp else 0.dp,
        isDragging = isDragging,
        dragHandleModifier = dragHandleModifier,
        onToggleEnabled = onToggleEnabled,
        onEdit = onEdit,
        onRequestDelete = onRequestDelete,
    )
}
```

`Expanded` 分支：

```kotlin
if (layoutMode == MyTokenLayoutMode.Expanded) {
    val gridState = rememberLazyGridState()
    val hapticView = LocalView.current
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        onMove(from.index, to.index)
        hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    MyTokenAdaptiveContent(
        maxWidth = 960.dp,
        horizontalPadding = 0.dp,
        modifier = Modifier.padding(padding).fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize().testTag("credential_grid"),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItemsIndexed(state.items, key = { _, item -> item.id }) { _, credential ->
                ReorderableItem(reorderableState, key = credential.id) { isDragging ->
                    CredentialItemContent(
                        credential = credential,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            onDragStopped = {
                                hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            },
                        ),
                        onToggleEnabled = { onToggleEnabled(credential) },
                        onEdit = { onEditCredential(credential.id) },
                        onRequestDelete = { onRequestDelete(credential) },
                    )
                }
            }
        }
    }
}
```

`Compact` 和 `Medium` 分支保持列表，但将列表放入：

```kotlin
MyTokenAdaptiveContent(
    maxWidth = 960.dp,
    horizontalPadding = 0.dp,
    modifier = Modifier.padding(padding).fillMaxSize(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("credential_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 保留现有 ReorderableItem 列表逻辑。
    }
}
```

- [ ] **Step 4: 编辑页增加表单限宽**

在 `CredentialEditorScreen.kt` 中增加：

```kotlin
layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
```

将表单 `Column` 外层改为：

```kotlin
MyTokenAdaptiveContent(
    maxWidth = 720.dp,
    horizontalPadding = 16.dp,
    modifier = Modifier.padding(padding).fillMaxSize(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
            .testTag("editor_form"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 保留现有表单字段和按钮。
    }
}
```

- [ ] **Step 5: 运行凭证模块测试**

Run:

```bash
cd android
./gradlew :feature-credentials:testDebugUnitTest \
  --tests "ai.routin.mytoken.feature.credentials.CredentialListAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.credentials.CredentialEditorAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.credentials.CredentialListScreenTest" \
  --tests "ai.routin.mytoken.feature.credentials.CredentialEditorScreenTest" \
  --tests "ai.routin.mytoken.feature.credentials.CredentialListViewModelTest"
```

Expected: 全部 PASS；双列网格仍通过现有 `onMove` 路径更新线性排序。

- [ ] **Step 6: 提交**

```bash
git add android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials \
  android/feature-credentials/src/test/kotlin/ai/routin/mytoken/feature/credentials
git commit -m "feat(android): 适配凭证列表与编辑表单"
```

---

### Task 5: 适配设置页双列分区

**Files:**
- Modify: `android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/SettingsScreen.kt`
- Create: `android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/SettingsScreenAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenLayoutMode`、`MyTokenAdaptiveContent()`。
- Produces:
  - `SettingsScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - 稳定测试标识：`settings_list`、`settings_about_span`。

- [ ] **Step 1: 写入设置页双列失败测试**

在 `SettingsScreenAdaptiveTest.kt` 中：

```kotlin
@Test
@Config(qualifiers = "w700dp-h1100dp")
fun 阔折叠设置页使用双列并让关于横跨两列() {
    setContent(layoutMode = MyTokenLayoutMode.Medium)
    composeRule.onNodeWithTag("settings_theme_cell").assertExists()
    composeRule.onNodeWithTag("settings_refresh_cell").assertExists()
    composeRule.onNodeWithTag("settings_about_span").assertExists()
}
```

同时用 `getUnclippedBoundsInRoot()` 断言“主题”和“刷新”卡片的 `top` 接近相等，证明它们位于同一行。

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :feature-settings:testDebugUnitTest --tests "ai.routin.mytoken.feature.settings.SettingsScreenAdaptiveTest"
```

Expected: FAIL，找不到双列 cell 或 `settings_about_span`。

- [ ] **Step 3: 将 `LazyColumn` 替换为自适应 `LazyVerticalGrid`**

在 `SettingsScreen.kt` 中增加：

先补充导入：

```kotlin
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
```

再增加参数：

```kotlin
layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
```

将主列表替换为：

```kotlin
val columns = if (layoutMode == MyTokenLayoutMode.Compact) 1 else 2
MyTokenAdaptiveContent(
    maxWidth = 1200.dp,
    horizontalPadding = 16.dp,
    modifier = Modifier.padding(padding).fillMaxSize(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().testTag("settings_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "theme", span = { GridItemSpan(1) }) {
            Box(modifier = Modifier.testTag("settings_theme_cell")) {
                SectionCard(title = "主题") {
                    // 保留现有主题 chips。
                }
            }
        }
        item(key = "refresh", span = { GridItemSpan(1) }) {
            Box(modifier = Modifier.testTag("settings_refresh_cell")) {
                RefreshSettingsSection(
                    settings = state.refresh,
                    onAutoRefreshChange = onAutoRefreshChange,
                    onIntervalChange = onIntervalChange,
                    onWifiOnlyChange = onWifiOnlyChange,
                    onOpenAppRefreshChange = onOpenAppRefreshChange,
                    onRetryOnFailureChange = onRetryOnFailureChange,
                )
            }
        }
        item(key = "notification", span = { GridItemSpan(1) }) {
            NotificationSettingsSection(
                settings = state.notifications,
                permissionGranted = notificationPermissionGranted,
                onRequestPermission = onRequestNotificationPermission,
                onNotificationsEnabledChange = onNotificationsEnabledChange,
                onCredentialFailureAlertsChange = onCredentialFailureAlertsChange,
                onLowThresholdChange = onLowThresholdChange,
                onHighThresholdChange = onHighThresholdChange,
            )
        }
        item(key = "migration", span = { GridItemSpan(1) }) {
            SectionCard(title = "数据迁移") {
                // 保留现有迁移说明和按钮。
            }
        }
        item(
            key = "about",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Box(modifier = Modifier.testTag("settings_about_span")) {
                SectionCard(title = "关于") {
                    // 保留现有版本、更新日志和应用更新内容。
                }
            }
        }
    }
}
```

- [ ] **Step 4: 运行设置模块测试**

Run:

```bash
cd android
./gradlew :feature-settings:testDebugUnitTest \
  --tests "ai.routin.mytoken.feature.settings.SettingsScreenAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.settings.SettingsScreenTest" \
  --tests "ai.routin.mytoken.feature.settings.NotificationSettingsSectionTest"
```

Expected: 全部 PASS；`settings_list` 的 `performScrollToNode` 行为保持可用。

- [ ] **Step 5: 提交**

```bash
git add android/feature-settings/src/main/kotlin/ai/routin/mytoken/feature/settings/SettingsScreen.kt \
  android/feature-settings/src/test/kotlin/ai/routin/mytoken/feature/settings/SettingsScreenAdaptiveTest.kt
git commit -m "feat(android): 适配设置页双列布局"
```

---

### Task 6: 适配迁移扫码、预览与完成页

**Files:**
- Modify: `android/feature-transfer/build.gradle.kts`
- Modify: `android/feature-transfer/src/main/kotlin/ai/routin/mytoken/feature/transfer/TransferPreviewScreen.kt`
- Modify: `android/feature-transfer/src/main/kotlin/ai/routin/mytoken/feature/transfer/TransferScannerScreen.kt`
- Create: `android/feature-transfer/src/test/kotlin/ai/routin/mytoken/feature/transfer/TransferAdaptiveTest.kt`

**Interfaces:**
- Consumes: `MyTokenLayoutMode`、`MyTokenAdaptiveContent()`。
- Produces:
  - `TransferPreviewScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - `TransferCompletedScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - `TransferScannerScreen(..., layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact)`
  - 稳定测试标识：`transfer_preview_content`、`transfer_summary_pane`、`transfer_list_pane`、`transfer_camera_frame`、`transfer_completed_content`。

- [ ] **Step 1: 增加迁移模块 Compose 测试依赖**

修改 `android/feature-transfer/build.gradle.kts`：

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
testImplementation("org.robolectric:robolectric:4.13")
testImplementation("androidx.test:core-ktx:1.6.1")
testImplementation("androidx.test.ext:junit-ktx:1.2.1")
testImplementation("androidx.compose.ui:ui-test-junit4")
testImplementation("androidx.compose.ui:ui-test-manifest")
```

- [ ] **Step 2: 写入迁移页失败测试**

在 `TransferAdaptiveTest.kt` 中：

```kotlin
@Test
@Config(qualifiers = "w1000dp-h1100dp")
fun 平板导入预览使用左右双栏并限制总宽度() {
    setPreviewContent(layoutMode = MyTokenLayoutMode.Expanded)
    composeRule.onNodeWithTag("transfer_summary_pane").assertExists()
    composeRule.onNodeWithTag("transfer_list_pane").assertExists()
    composeRule.onNodeWithTag("transfer_preview_content")
        .assertWidthIsAtMost(960.dp)
}

@Test
@Config(qualifiers = "w1000dp-h1100dp")
fun 平板扫码取景保持稳定比例() {
    setScannerFrameForTest()
    composeRule.onNodeWithTag("transfer_camera_frame")
        .assertWidthIsAtMost(720.dp)
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```bash
cd android
./gradlew :feature-transfer:testDebugUnitTest --tests "ai.routin.mytoken.feature.transfer.TransferAdaptiveTest"
```

Expected: FAIL，找不到迁移页测试标识。

- [ ] **Step 4: 实现导入预览宽屏双栏**

在 `TransferPreviewScreen.kt` 中增加 `layoutMode` 参数，并用以下结构替换根 `Column`：

```kotlin
MyTokenAdaptiveContent(
    maxWidth = 960.dp,
    horizontalPadding = 16.dp,
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("transfer_preview_content"),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("确认导入", style = MaterialTheme.typography.headlineSmall)
        if (layoutMode == MyTokenLayoutMode.Expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.weight(0.4f).testTag("transfer_summary_pane")) {
                    TransferSummaryCard(state)
                }
                Box(modifier = Modifier.weight(0.6f).testTag("transfer_list_pane")) {
                    TransferItemList(state)
                }
            }
        } else {
            TransferSummaryCard(state)
            TransferItemList(state, modifier = Modifier.weight(1f))
        }
        TransferActionRow(state, onConfirm, onCancel)
    }
}
```

将现有摘要、列表和按钮抽成 `TransferSummaryCard`、`TransferItemList`、`TransferActionRow`，不改变回调参数。

- [ ] **Step 5: 实现扫码框和完成页限宽**

在 `TransferScannerScreen.kt` 中增加 `layoutMode` 参数，并让取景框使用：

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
    val frameModifier = if (layoutMode == MyTokenLayoutMode.Compact) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .aspectRatio(4f / 3f)
            .align(Alignment.Center)
            .testTag("transfer_camera_frame")
    }
    Box(modifier = frameModifier) {
        // 保留现有 CameraQrPreview 和扫描框。
    }
}
```

在 `TransferCompletedScreen.kt` 中将内容放入：

```kotlin
MyTokenAdaptiveContent(
    maxWidth = 480.dp,
    horizontalPadding = 24.dp,
    modifier = Modifier.fillMaxSize().testTag("transfer_completed_content"),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 保留现有完成文案和按钮。
    }
}
```

- [ ] **Step 6: 把布局模式从 `MyTokenApp` 传给迁移页面**

在 `MyTokenApp.kt` 中：

```kotlin
is TransferUiState.Idle -> TransferScannerScreen(
    isActive = true,
    layoutMode = layoutMode,
    onQrCodeScanned = transferViewModel::onQrCodeScanned,
    onCancel = { goBack() },
)
```

`TransferPreviewScreen` 和 `TransferCompletedScreen` 同样传入 `layoutMode = layoutMode`。

- [ ] **Step 7: 运行迁移相关测试**

Run:

```bash
cd android
./gradlew :feature-transfer:testDebugUnitTest \
  --tests "ai.routin.mytoken.feature.transfer.TransferAdaptiveTest" \
  --tests "ai.routin.mytoken.feature.transfer.TransferQrCodePayloadTest" \
  --tests "ai.routin.mytoken.feature.transfer.TransferViewModelTest"
```

Expected: 全部 PASS；扫码和解密逻辑测试不受 UI 布局改动影响。

- [ ] **Step 8: 提交**

```bash
git add android/feature-transfer/build.gradle.kts \
  android/feature-transfer/src/main/kotlin/ai/routin/mytoken/feature/transfer \
  android/feature-transfer/src/test/kotlin/ai/routin/mytoken/feature/transfer/TransferAdaptiveTest.kt
git commit -m "feat(android): 适配迁移页面大屏布局"
```

---

### Task 7: 全量回归、文档与真机验收说明

**Files:**
- Modify: `docs/android/README.md`
- Modify: `docs/superpowers/specs/2026-09-14-android-adaptive-tablet-foldable-design.md`
- Modify: 必要的测试文件，仅修复本次变更引起的回归。

**Interfaces:**
- Consumes: Task 1 到 Task 6 的全部实现。
- Produces: 可发布的 Debug APK、可编译的 AndroidTest APK、更新后的平板/阔折叠手工验收清单。

- [ ] **Step 1: 运行 Android 全量 JVM 测试**

Run:

```bash
cd android
./gradlew test
```

Expected: 所有模块 JVM/Robolectric 测试 PASS。

- [ ] **Step 2: 构建 Debug APK 和 AndroidTest APK**

Run:

```bash
cd android
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

Expected: 两个构建任务均成功，无 Kotlin 编译错误或资源错误。

- [ ] **Step 3: 更新 Android 手工验收清单**

在 `docs/android/README.md` 的“手工验收（待真机验证）”中追加：

```markdown
- [ ] 手机竖屏保持底部导航，首页、凭证和设置均为单列。
- [ ] 手机横屏切换到左侧导航，首页显示双列卡片。
- [ ] 平板横竖屏切换时保留当前标签、Pager 页面和详情返回栈。
- [ ] 阔折叠展开后首页显示三列或按最小宽度回退，凭证页可拖拽排序。
- [ ] 分屏窗口在 600dp 和 840dp 附近缩放时不出现底栏与侧栏同时显示。
- [ ] 大字体和深色主题下，导航、设置双列和迁移页面无文字重叠。
```

- [ ] **Step 4: 检查规范状态与实现一致性**

确认 `docs/superpowers/specs/2026-09-14-android-adaptive-tablet-foldable-design.md` 中的断点、列数、最大宽度和测试项与实现一致。若实现中调整了常量，只修改规范中对应数值，不扩大范围。

- [ ] **Step 5: 运行真机或模拟器验收**

在有设备或模拟器时运行：

```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

随后手工检查：

- Android 手机竖屏。
- Android 手机横屏。
- 平板竖屏和横屏。
- 阔折叠展开和折叠。
- 系统字体放大。
- 浅色和深色主题。

Expected: 导航切换、列表拖拽、Pager、表单、扫码和解密流程均保持可用。

- [ ] **Step 6: 检查工作区与差异**

Run:

```bash
git diff --check
git status --short
```

Expected: `git diff --check` 无输出；`git status` 只包含本任务文档变更。

- [ ] **Step 7: 提交文档与回归修复**

```bash
git add docs/android/README.md \
  docs/superpowers/specs/2026-09-14-android-adaptive-tablet-foldable-design.md
git commit -m "docs(android): 补充大屏适配验收说明"
```

---

## 完成标准

- Task 1 到 Task 7 均各自通过测试并形成独立提交。
- `./gradlew test`、`:app:assembleDebug`、`:app:assembleDebugAndroidTest` 全部成功。
- 手机、横屏、平板和阔折叠均按实际窗口宽度使用正确导航和列数。
- Pager、筛选、排序、表单、迁移和解密业务状态在宽度变化时不丢失。
- 详情、编辑、设置和迁移页面不再在平板宽度下横向拉伸。
- 规范、计划、实现常量和手工验收清单保持一致。
