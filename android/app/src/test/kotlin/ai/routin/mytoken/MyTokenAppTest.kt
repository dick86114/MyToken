package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 冒烟测试：真实 Activity 通过首页两角入口打开左右工具抽屉。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class MyTokenAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cornerEntriesOpenCredentialAndSettingsDrawers() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()

        composeRule.onNodeWithTag("home_open_credentials").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_start").assertIsDisplayed()
        composeRule.onNodeWithTag("credential_drawer_content").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_drawer_scrim_start")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_start").assertDoesNotExist()

        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_drawer_content").assertIsDisplayed()

        // 低频设置以摘要形式保留，展开后才出现具体操作。
        composeRule.onNodeWithTag("settings_drawer_content").performScrollToNode(
            hasText("数据迁移"),
        )
        composeRule.onNodeWithText("扫码导入").assertIsDisplayed()
    }

    @Test
    fun 从凭证抽屉进入编辑器后返回不会残留旧抽屉() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_open_credentials").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("credential_add_button")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_open_settings").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("edge_drawer_start").assertDoesNotExist()
        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("edge_drawer_end").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
    }
}
