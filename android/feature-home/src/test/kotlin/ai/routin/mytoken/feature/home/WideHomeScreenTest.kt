package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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

    private fun cards(used: List<Double> = listOf(20.0, 20.0, 20.0)): List<CredentialCardUi> =
        listOf(
            sampleCard("Routin 主账号", ProviderId.Routin, used = used[0]),
            sampleCard("GLM 主账号", ProviderId.Glm, used = used[1]),
            sampleCard("DeepSeek 主账号", ProviderId.DeepSeek, used = used[2]),
        )

    private fun setContent(
        layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Expanded,
        used: List<Double> = listOf(20.0, 20.0, 20.0),
    ): List<CredentialCardUi> {
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
                    usageCardDensity = UsageCardDensity.COMPACT,
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
    fun 三栏包含左轨报告分区行与瀑布流且不再使用分页() {
        setContent()
        composeRule.onNodeWithTag("home_wide_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wide_report").assertIsDisplayed()
        composeRule.onNodeWithTag("home_provider_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("home_grid_4_columns").assertExists()
        composeRule.onNodeWithTag("home_pager").assertDoesNotExist()
        composeRule.onNodeWithTag("home_toolbar").assertDoesNotExist()
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
    @Config(qualifiers = "w1280dp-h720dp")
    fun 点卡片打开三百八十宽详情再次点击或关闭按钮收起() {
        val all = setContent()
        val cardTag = "credential_card_${all[0].credential.id}"
        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(380.dp, width)
        composeRule.onNodeWithTag("home_wide_detail_close").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()

        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag(cardTag).performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h1024dp")
    fun 方形窗口详情使用三百六十宽() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(360.dp, width)
    }

    @Test
    @Config(qualifiers = "w768dp-h1024dp")
    fun 竖屏折叠详情使用三百四十宽() {
        val all = setContent(layoutMode = MyTokenLayoutMode.Medium)
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        val width = composeRule.onNodeWithTag("home_wide_detail")
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }
        assertEquals(340.dp, width)
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
    fun 点另一张卡片直接替换详情内容() {
        val all = setContent()
        composeRule.onNodeWithTag("credential_card_${all[0].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("credential_card_${all[1].credential.id}").performClick()
        composeRule.onNodeWithTag("home_wide_detail")
            .assertContentDescriptionContains("GLM 主账号", substring = true)
        composeRule.onAllNodesWithTag("home_wide_detail").assertCountEquals(1)
    }
}
