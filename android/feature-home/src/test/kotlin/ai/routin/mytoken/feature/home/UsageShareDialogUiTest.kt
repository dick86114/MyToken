package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import java.time.Instant
import java.util.UUID
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

    private fun setContent(layoutMode: MyTokenLayoutMode) {
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
                metrics = emptyList(),
                planName = "成长版",
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
        composeRule.setContent {
            MyTokenTheme {
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
    fun 字段开关按票面顺序排列且整卡可点击() {
        setContent(MyTokenLayoutMode.Expanded)
        val expected = listOf(
            "可用状态徽章",
            "套餐规格",
            "订阅周期/到期",
            "周期剩余",
            "附加备注框",
            "分组倍率",
            "快照水印与防伪",
        )
        composeRule.onNodeWithText(expected.first()).assertExists()
        composeRule.onNodeWithText(expected.last()).assertExists()
    }
}
