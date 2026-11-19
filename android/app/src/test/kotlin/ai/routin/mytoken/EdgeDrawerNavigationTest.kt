package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class EdgeDrawerNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun 左右抽屉取代底部导航并占据大部分内容宽度() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()

        composeRule.onNodeWithTag("home_open_credentials").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_start").assertIsDisplayed()

        val rootWidth = composeRule.onRoot().fetchSemanticsNode().size.width
        val drawerWidth = composeRule.onNodeWithTag("edge_drawer_start")
            .fetchSemanticsNode()
            .size
            .width
        assertTrue(drawerWidth > rootWidth / 2)
        assertTrue(drawerWidth < rootWidth)

        composeRule.onNodeWithTag("edge_drawer_scrim_start")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_start").assertDoesNotExist()

        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
        composeRule.onNodeWithTag("edge_drawer_scrim_end")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("edge_drawer_end").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_end").assertDoesNotExist()
    }

    @Test
    fun 主页中部右滑可以打开凭证抽屉() {
        composeRule.waitForIdle()

        composeRule.onRoot().performTouchInput {
            val y = height / 2f
            down(Offset(width * 0.42f, y))
            moveTo(Offset(width * 0.88f, y), delayMillis = 180)
            up()
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_start").assertIsDisplayed()
    }

    @Test
    fun 主页中部左滑可以打开设置抽屉() {
        composeRule.waitForIdle()

        composeRule.onRoot().performTouchInput {
            val y = height / 2f
            down(Offset(width * 0.58f, y))
            moveTo(Offset(width * 0.12f, y), delayMillis = 180)
            up()
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("edge_drawer_end").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
    }

    @Test
    fun 抽屉反复开合后首页仍然可以点击() {
        composeRule.waitForIdle()

        repeat(3) {
            composeRule.onRoot().performTouchInput {
                val y = height / 2f
                down(Offset(width * 0.42f, y))
                moveTo(Offset(width * 0.88f, y), delayMillis = 140)
                up()
            }
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithTag("edge_drawer_start")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("edge_drawer_start").performTouchInput {
                val y = height / 2f
                down(Offset(width * 0.82f, y))
                moveTo(Offset(width * 0.08f, y), delayMillis = 140)
                up()
            }
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithTag("edge_drawer_start")
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }

        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("edge_drawer_end")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
    }
}
