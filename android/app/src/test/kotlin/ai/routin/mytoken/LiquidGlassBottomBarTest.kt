package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class LiquidGlassBottomBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun 浮动底栏不占满整行并显示选中高亮() {
        composeRule.waitForIdle()

        val rootWidth = composeRule.onRoot().fetchSemanticsNode().size.width
        val navigationWidth = composeRule.onNodeWithTag("bottom_navigation")
            .fetchSemanticsNode()
            .size
            .width

        composeRule.onNodeWithTag("bottom_navigation_highlight").assertIsDisplayed()
        composeRule.onAllNodesWithTag("bottom_navigation_highlight").assertCountEquals(1)

        assertTrue(navigationWidth > 0)
        assertTrue(navigationWidth < (rootWidth * 0.85f).toInt())
    }
}
