package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageShareDialogUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        layoutMode: MyTokenLayoutMode,
        metrics: List<UsageMetric> = emptyList(),
    ) {
        val credential = Credential(
            id = UUID.fromString("12345678-0000-0000-0000-000000000000"),
            providerId = ProviderId.Routin,
            credentialKind = CredentialKind.BearerApiKey,
            name = "主力账户",
        )
        val card = CredentialCardUi(
            credential = credential,
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = credential.id,
                fetchedAt = Instant.parse("2026-09-20T08:30:00Z"),
                metrics = metrics,
                planName = "成长版",
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
        composeRule.setContent {
            MyTokenTheme(darkTheme = false) {
                UsageShareDialog(
                    card = card,
                    onDismiss = {},
                    layoutMode = layoutMode,
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun 手机操作保持单行且不再显示微信入口() {
        setContent(MyTokenLayoutMode.Compact)
        composeRule.onNodeWithTag("share_actions_row").assertExists()
        composeRule.onNodeWithTag("share_settings_pane").assertDoesNotExist()
        composeRule.onNodeWithText("微信").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun 平板使用预览和设置左右布局() {
        setContent(MyTokenLayoutMode.Expanded)
        composeRule.onNodeWithTag("share_wide_preview").assertExists()
        composeRule.onNodeWithTag("share_settings_pane").assertExists()
        composeRule.onNodeWithTag("share_actions_row").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun 模板风格四选项单行上图下字且不显示模式说明() {
        setContent(MyTokenLayoutMode.Expanded)

        val tops = UsageShareTemplate.entries.map { template ->
            composeRule.onNodeWithText(template.title, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .top
                .value
        }
        assertEquals("四个模板应排在同一行", 1, tops.distinct().size)
        UsageShareTemplate.entries.forEach { template ->
            composeRule.onNodeWithTag("share_template_${template.name}", useUnmergedTree = true)
                .assertExists()
        }
        composeRule.onAllNodesWithText("浅色票根模式", useUnmergedTree = true)
            .assertCountEquals(0)

        composeRule.onNodeWithTag("share_template_Dark", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
            .assertIsSelected()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1000dp")
    fun 展示字段改为胶囊默认一行可展开收起且点击切换() {
        setContent(
            MyTokenLayoutMode.Expanded,
            metrics = (1..20).map { progress("metric-$it", "指标 $it") },
        )

        composeRule.onNodeWithTag("share_field_chip_0", useUnmergedTree = true)
            .assertIsOn()
            .performSemanticsAction(SemanticsActions.OnClick)
            .assertIsOff()
            .performSemanticsAction(SemanticsActions.OnClick)
            .assertIsOn()

        val collapsedHeight = composeRule.onNodeWithTag("share_field_flow", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("快照水印与防伪").assertExists()
        val expandedHeight = composeRule.onNodeWithTag("share_field_flow", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .let { it.bottom - it.top }
        assertTrue("展开后字段应占多行（折叠 $collapsedHeight，展开 $expandedHeight）", expandedHeight > collapsedHeight)

        composeRule.onNodeWithTag("share_field_collapse", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun 复制保存分享收进顶部图标且预览位于其下() {
        setContent(MyTokenLayoutMode.Compact)

        val actions = composeRule.onNodeWithTag("share_actions_row", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithTag("share_preview_pane", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("操作按钮应在预览上方", actions.bottom <= preview.top)

        listOf("share_action_copy", "share_action_save", "share_action_share").forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
        composeRule.onNodeWithContentDescription("复制分享图").assertExists()
        composeRule.onNodeWithContentDescription("保存分享图").assertExists()
        composeRule.onNodeWithContentDescription("分享用量").assertExists()
        listOf("复制", "保存").forEach { label ->
            composeRule.onNodeWithText(label).assertDoesNotExist()
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun 四个以内字段直接展示不需要更多入口() {
        setContent(MyTokenLayoutMode.Expanded)
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("快照水印与防伪").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun 字段胶囊展开后按票面顺序全部可见() {
        setContent(
            MyTokenLayoutMode.Expanded,
            metrics = (1..5).map { progress("metric-$it", "指标 $it") },
        )
        composeRule.onNodeWithText("可用状态徽章").assertExists()
        composeRule.onNodeWithTag("share_field_more", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("快照水印与防伪").assertExists()
    }

    private fun progress(id: String, label: String) = UsageMetric(
        id = id,
        label = label,
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Normal,
    )
}
