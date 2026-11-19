package ai.routin.mytoken

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    fun 紧凑宽度使用首页双角入口() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("home_open_credentials").assertIsDisplayed()
        composeRule.onNodeWithTag("home_open_settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w700dp-h1100dp")
    fun 中等宽度仍保留双侧抽屉导航() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("home_open_credentials").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_start").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 展开宽度仍通过右角打开设置抽屉() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edge_drawer_end").assertIsDisplayed()
    }
}
