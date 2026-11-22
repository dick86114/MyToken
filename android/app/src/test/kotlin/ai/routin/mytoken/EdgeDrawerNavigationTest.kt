package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
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
    fun 抽屉手柄竖条居中于面板边框与页边距之间() {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_open_credentials").performClick()
        composeRule.waitForIdle()
        val startDrawer = composeRule.onNodeWithTag("edge_drawer_start").getUnclippedBoundsInRoot()
        val startBar = composeRule
            .onNodeWithTag("edge_drawer_handle_bar_start", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val startCenter = (startBar.left.value + startBar.right.value) / 2f
        assertEquals(
            "左侧抽屉手柄应在 16dp 页边距中居中",
            8f,
            startDrawer.right.value - startCenter,
            1f,
        )

        composeRule.onNodeWithTag("edge_drawer_scrim_start")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitForIdle()
        val endDrawer = composeRule.onNodeWithTag("edge_drawer_end").getUnclippedBoundsInRoot()
        val endBar = composeRule
            .onNodeWithTag("edge_drawer_handle_bar_end", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val endCenter = (endBar.left.value + endBar.right.value) / 2f
        assertEquals(
            "右侧抽屉手柄应在 16dp 页边距中居中",
            8f,
            endCenter - endDrawer.left.value,
            1f,
        )
    }

    @Test
    fun 主页左滑不会打开抽屉() {
        composeRule.waitForIdle()

        swipeAcrossHome(left = true)

        composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        composeRule.onAllNodesWithTag("edge_drawer_end").fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun 主页右滑不会打开抽屉() {
        composeRule.waitForIdle()

        swipeAcrossHome(left = false)

        composeRule.onAllNodesWithTag("edge_drawer_start").fetchSemanticsNodes().isEmpty()
        composeRule.onAllNodesWithTag("edge_drawer_end").fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun 按钮反复开合抽屉后首页仍然可以点击() {
        composeRule.waitForIdle()

        repeat(3) {
            composeRule.onNodeWithTag("home_open_credentials").performClick()
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithTag("edge_drawer_start")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("edge_drawer_scrim_start")
                .performSemanticsAction(SemanticsActions.OnClick)
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

    private fun swipeAcrossHome(left: Boolean) {
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            val y = height * 0.4f
            if (left) {
                down(Offset(width * 0.8f, y))
                moveTo(Offset(width * 0.2f, y), delayMillis = 180)
            } else {
                down(Offset(width * 0.2f, y))
                moveTo(Offset(width * 0.8f, y), delayMillis = 180)
            }
            up()
        }
    }
}
