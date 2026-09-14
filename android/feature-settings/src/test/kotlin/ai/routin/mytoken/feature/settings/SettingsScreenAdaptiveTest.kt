package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w700dp-h1100dp")
class SettingsScreenAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 阔折叠设置页使用双列并让关于横跨两列() {
        composeRule.setContent {
            MyTokenTheme {
                SettingsScreen(
                    state = SettingsUiState(isLoading = false),
                    appVersion = "0.1.0",
                    layoutMode = MyTokenLayoutMode.Medium,
                    onAutoRefreshChange = {},
                    onIntervalChange = {},
                    onWifiOnlyChange = {},
                    onOpenAppRefreshChange = {},
                    onRetryOnFailureChange = {},
                    onThemeModeChange = {},
                    onOpenTransfer = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings_theme_cell").assertExists()
        composeRule.onNodeWithTag("settings_refresh_cell").assertExists()
        composeRule.onNodeWithTag("settings_about_span").assertExists()

        val themeTop = composeRule.onNodeWithTag("settings_theme_cell")
            .getUnclippedBoundsInRoot().top
        val refreshTop = composeRule.onNodeWithTag("settings_refresh_cell")
            .getUnclippedBoundsInRoot().top
        assertEquals(themeTop, refreshTop)
    }
}
