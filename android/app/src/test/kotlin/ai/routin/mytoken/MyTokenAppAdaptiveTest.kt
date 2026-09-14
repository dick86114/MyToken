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
