package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WideHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var density by mutableStateOf(UsageCardDensity.COMPACT)
    private var densityChanges = 0

    private fun cards(used: List<Double> = listOf(20.0, 20.0, 20.0)): List<CredentialCardUi> =
        listOf(
            sampleCard("Routin 主账号", ProviderId.Routin, used = used[0]),
            sampleCard("GLM 主账号", ProviderId.Glm, used = used[1]),
            sampleCard("DeepSeek 主账号", ProviderId.DeepSeek, used = used[2]),
        )

    private fun setContent(
        layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Expanded,
        used: List<Double> = listOf(20.0, 20.0, 20.0),
        initialDensity: UsageCardDensity = UsageCardDensity.COMPACT,
        hasUpdate: Boolean = false,
    ): List<CredentialCardUi> {
        density = initialDensity
        densityChanges = 0
        val all = cards(used)
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = all,
                        groups = listOf(
                            ProviderGroupUi(ProviderId.Routin, "Routin", false, listOf(all[0])),
                            ProviderGroupUi(ProviderId.Glm, "智谱 GLM", false, listOf(all[1])),
                            ProviderGroupUi(ProviderId.DeepSeek, "DeepSeek", false, listOf(all[2])),
                        ),
                        credentialCount = all.size,
                    ),
                    layoutMode = layoutMode,
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        densityChanges += 1
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                    hasUpdateAvailable = hasUpdate,
                    updateVersion = if (hasUpdate) "6.2.0" else null,
                    onConfirmUpdate = {},
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        return all
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 大屏使用左轨报告与手机同款分页卡片区() {
        setContent()
        composeRule.onNodeWithTag("home_wide_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_report").assertIsDisplayed()
        composeRule.onNodeWithTag("home_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("home_page_active").assertExists()
        composeRule.onNodeWithTag("home_grid_6_columns").assertExists()
        // 大屏不再出现“全部供应商”筛选行，也不再放大屏更新条。
        composeRule.onNodeWithTag("home_provider_toolbar").assertDoesNotExist()
        composeRule.onNodeWithTag("home_toolbar").assertDoesNotExist()
        composeRule.onNodeWithTag("home_update_banner").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 全部供应商图标是二乘二网格且品牌下方有分隔线() {
        setContent()
        composeRule.onNodeWithTag("home_wide_rail_all_icon", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag("home_wide_rail_all_grid_icon", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag("home_wide_rail_all_logo_routin", useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag("home_wide_rail_brand_divider", useUnmergedTree = true)
            .assertExists()
        val bounds = composeRule.onNodeWithTag("home_wide_rail_all_icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(30.dp, bounds.right - bounds.left)
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 无告警时报告收成八十短票并显示全部凭证正常() {
        setContent()
        composeRule.onNodeWithText("全部 3 个凭证当前用量正常").assertIsDisplayed()
        val height = composeRule.onNodeWithTag("home_wide_report")
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        assertEquals(80.dp, height)
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 有告警时报告一百一十高并展示告警条目() {
        val all = setContent(used = listOf(20.0, 90.0, 96.0))
        composeRule.onNodeWithTag("health_finding_${all[1].credential.id}").assertExists()
        composeRule.onNodeWithTag("health_finding_${all[2].credential.id}").assertExists()
        val height = composeRule.onNodeWithTag("home_wide_report")
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        assertEquals(110.dp, height)
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun 四比三横屏报告仍横排且密度横排位于报告下方右侧() {
        val all = setContent(used = listOf(20.0, 90.0, 96.0))
        val report = composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot()
        val toggle = composeRule.onNodeWithTag("usage_density_toggle").getUnclippedBoundsInRoot()
        assertEquals(110.dp, report.bottom - report.top)
        assertTrue(
            "密度切换器应在报告下方，report.bottom=${report.bottom} toggle.top=${toggle.top}",
            toggle.top.value >= report.bottom.value,
        )
        assertEquals("密度切换器始终横排", 32.dp, toggle.bottom - toggle.top)
        assertEquals("密度切换器右对齐", report.right, toggle.right)
        composeRule.onNodeWithTag("home_grid_5_columns").assertExists()
        composeRule.onNodeWithTag("health_finding_${all[1].credential.id}").assertExists()
    }

    @Test
    @Config(qualifiers = "w768dp-h1024dp")
    fun 竖屏报告在上控制条在下() {
        setContent(layoutMode = MyTokenLayoutMode.Medium)
        val report = composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot()
        val toggle = composeRule.onNodeWithTag("usage_density_toggle").getUnclippedBoundsInRoot()
        assertTrue(
            "竖屏控制条应在报告下方，report.bottom=${report.bottom} toggle.top=${toggle.top}",
            toggle.top.value >= report.bottom.value,
        )
        composeRule.onNodeWithTag("home_grid_4_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 有更新时箭头在左轨刷新上方且报告内无重复提示() {
        setContent(hasUpdate = true)
        composeRule.onNodeWithTag("home_update_banner").assertDoesNotExist()
        composeRule.onNodeWithTag("home_wide_update_hint").assertDoesNotExist()

        val update = composeRule.onNodeWithTag("home_wide_rail_update", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val refresh = composeRule.onNodeWithTag("home_wide_rail_refresh", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "更新箭头应在刷新按钮上方，update.bottom=${update.bottom} refresh.top=${refresh.top}",
            update.bottom.value <= refresh.top.value + 1f,
        )
        val toggle = composeRule.onNodeWithTag("usage_density_toggle").getUnclippedBoundsInRoot()
        assertEquals("密度切换始终横排（高 32dp）", 32.dp, toggle.bottom - toggle.top)

        composeRule.onNodeWithTag("home_wide_rail_update", useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag("home_update_dialog").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 密度切换器驱动分页并在落页后写回模式() {
        setContent()
        composeRule.onNodeWithTag("home_grid_6_columns").assertExists()
        composeRule.onNodeWithTag("usage_density_option_full").performClick()
        composeRule.waitForIdle()

        assertEquals(UsageCardDensity.FULL, density)
        assertEquals(1, densityChanges)
        composeRule.onNodeWithTag("home_grid_3_columns").assertExists()
        composeRule.onNodeWithTag("home_grid_6_columns").assertDoesNotExist()

        composeRule.onNodeWithTag("usage_density_option_compact").performClick()
        composeRule.waitForIdle()
        assertEquals(UsageCardDensity.COMPACT, density)
        assertEquals(2, densityChanges)
        composeRule.onNodeWithTag("home_grid_6_columns").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 卡片宽度以手机竖屏卡宽为基准且不超百分之十二拉伸() {
        val all = setContent()
        val compactWidth = composeRule
            .onNodeWithTag("credential_card_${all[0].credential.id}")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertTrue(
            "1280 简洁页卡宽 $compactWidth 应接近 176dp",
            compactWidth.value >= 176f * 0.9f && compactWidth.value <= 176f * 1.12f + 0.1f,
        )

        composeRule.onNodeWithTag("usage_density_option_full").performClick()
        composeRule.waitForIdle()
        val fullWidth = composeRule
            .onNodeWithTag("credential_card_${all[0].credential.id}")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertTrue(
            "1280 完整页卡宽 $fullWidth 应接近 360dp",
            fullWidth.value >= 360f * 0.9f && fullWidth.value <= 360f * 1.12f + 0.1f,
        )
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 上滑网格后报告跟随收起() {
        // 18 张卡确保列表可滚，头部有足够的收起空间。
        val all = (1..6).flatMap { cards() }
        density = UsageCardDensity.COMPACT
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = all,
                        groups = emptyList(),
                        credentialCount = all.size,
                    ),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        val before = composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("home_grid_6_columns").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_scroll_edge").assertExists()
        val after = composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot()
        assertTrue(
            "报告应随上滑收起，before.top=${before.top} after.top=${after.top}",
            after.top.value < before.top.value,
        )
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 点卡片打开与设置同宽浮层且首页不变形() {
        val all = setContent()
        val cardTag = "credential_card_${all[0].credential.id}"
        val gridBefore = composeRule.onNodeWithTag("home_grid_6_columns").getUnclippedBoundsInRoot()
        val reportBefore = composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(420.dp, width)
        // 浮层是覆盖层：首页不缩窄、不位移。
        assertEquals(
            gridBefore,
            composeRule.onNodeWithTag("home_grid_6_columns").getUnclippedBoundsInRoot(),
        )
        assertEquals(
            reportBefore,
            composeRule.onNodeWithTag("home_wide_report").getUnclippedBoundsInRoot(),
        )

        composeRule.onNodeWithTag("home_wide_detail_close").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()

        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h1024dp")
    fun 方形窗口详情同样使用四百二十宽() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(420.dp, width)
    }

    @Test
    @Config(qualifiers = "w768dp-h1024dp")
    fun 竖屏折叠详情与设置抽屉同宽四百二十() {
        val all = setContent(layoutMode = MyTokenLayoutMode.Medium)
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(420.dp, width)
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 左轨筛选只影响中间卡片报告仍覆盖全部() {
        val all = setContent(used = listOf(20.0, 90.0, 96.0))
        composeRule.onNodeWithTag("home_wide_rail_provider_glm").performClick()
        composeRule.onNodeWithTag("credential_card_${all[1].credential.id}").assertExists()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").assertDoesNotExist()
        composeRule.onNodeWithTag("credential_card_${all[2].credential.id}").assertDoesNotExist()
        // 报告口径仍是全部凭证：两条告警仍在。
        composeRule.onNodeWithTag("health_finding_${all[1].credential.id}").assertExists()
        composeRule.onNodeWithTag("health_finding_${all[2].credential.id}").assertExists()

        composeRule.onNodeWithTag("home_wide_rail_all").performClick()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 侧栏详情保留详情页全部区块与操作() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("detail_hero").assertExists()
        composeRule.onNodeWithTag("detail_windows").assertExists()
        composeRule.onNodeWithTag("detail_facts").assertExists()
        composeRule.onNodeWithTag("detail_config").assertExists()
        composeRule.onNodeWithTag("detail_share_icon").assertExists()
        composeRule.onNodeWithTag("detail_delete").assertExists()
        composeRule.onNodeWithTag("detail_edit").assertExists()
        composeRule.onNodeWithTag("detail_refresh_now").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 旋转重建后仍保留打开的详情() {
        val all = cards()
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = all,
                        groups = emptyList(),
                        credentialCount = all.size,
                    ),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    usageCardDensity = UsageCardDensity.COMPACT,
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertExists()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("home_wide_detail").assertExists()
        composeRule.onNodeWithTag("home_wide_detail")
            .assertContentDescriptionContains(all[0].credential.name, substring = true)
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 折叠回手机宽度时把侧栏详情交给整页详情() {
        val all = cards()
        var layoutMode by mutableStateOf(MyTokenLayoutMode.Expanded)
        var openedCredentialId: java.util.UUID? = null
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = all,
                        groups = emptyList(),
                        credentialCount = all.size,
                    ),
                    layoutMode = layoutMode,
                    usageCardDensity = UsageCardDensity.COMPACT,
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = { openedCredentialId = it },
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertExists()

        composeRule.runOnIdle { layoutMode = MyTokenLayoutMode.Compact }
        composeRule.waitForIdle()

        assertEquals(all[0].credential.id, openedCredentialId)
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
        composeRule.onNodeWithTag("home_pager").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 点击浮层外遮罩关闭详情() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_detail_scrim").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_detail_scrim").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 下拉详情顶部手柄关闭浮层() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_detail_handle").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y + 160.dp.toPx()),
                durationMillis = 400,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun 右滑详情浮层关闭() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_detail").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
    }
}
