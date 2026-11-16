package ai.routin.mytoken.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.platform.testTag
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class LoadedReleaseNotesManualRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 已加载状态仍显示手动获取当前版本日志按钮() {
        val refreshCount = AtomicInteger()
        val releases = listOf(
            AppReleaseHistoryItem(
                version = "0.1.0",
                releaseNotes = "当前版本修复内容",
                releaseUrl = "https://example.com/0.1.0",
                publishedAt = null,
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsUiState(isLoading = false),
                    appVersion = "0.1.0",
                    releaseHistoryState = AppReleaseHistoryUiState.Loaded(releases),
                    onOpenAppRefreshChange = {},
                    onRetryOnFailureChange = {},
                    onThemeModeChange = {},
                    onOpenTransfer = {},
                    onLoadReleaseHistory = { refreshCount.incrementAndGet() },
                )
            }
        }

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasText("关于与更新"))
        composeRule.onNodeWithTag("settings_about_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_release_notes_toggle")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("获取当前版本日志")
            .performScrollTo()
            .performClick()

        assertEquals(1, refreshCount.get())
    }
}
