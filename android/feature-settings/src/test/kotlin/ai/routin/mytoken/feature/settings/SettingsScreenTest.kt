package ai.routin.mytoken.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.platform.testTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val refreshStore = FakeRefreshSettingsStore()
    private val displayStore = FakeDisplaySettingsStore()

    private fun setContent(
        onOpenTransfer: () -> Unit = {},
        updateState: AppUpdateUiState = AppUpdateUiState.Idle,
        mirrorBase: String = "",
        onOpenInstallPermissionSettings: () -> Unit = {},
    ) {
        val releaseHistoryState = AppReleaseHistoryUiState.Loaded(
            listOf(
                AppReleaseHistoryItem(
                    version = "0.1.0",
                    releaseNotes = "当前版本修复内容",
                    releaseUrl = "https://example.com/0.1.0",
                    publishedAt = "2026-09-11T00:00:00Z",
                ),
                AppReleaseHistoryItem(
                    version = "0.0.9",
                    releaseNotes = "历史版本修复内容",
                    releaseUrl = "https://example.com/0.0.9",
                    publishedAt = "2026-09-01T00:00:00Z",
                ),
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        isLoading = false,
                        refresh = refreshStore.state.value,
                        display = displayStore.state.value,
                        update = UpdateSettings(mirrorBase = mirrorBase),
                    ),
                    appVersion = "0.1.0",
                    updateState = updateState,
                    releaseHistoryState = releaseHistoryState,
                    onOpenAppRefreshChange = { refreshStore.state.value = refreshStore.state.value.copy(openAppRefresh = it) },
                    onRetryOnFailureChange = { refreshStore.state.value = refreshStore.state.value.copy(retryOnFailure = it) },
                    onThemeModeChange = { displayStore.state.value = displayStore.state.value.copy(themeMode = it) },
                    onOpenTransfer = onOpenTransfer,
                    onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                )
            }
        }
    }

    @Test
    fun rendersRefreshAndDisplaySections() {
        setContent()

        composeRule.onNodeWithText("刷新").assertIsDisplayed()
        composeRule.onNodeWithText("自动刷新").assertDoesNotExist()
        composeRule.onNodeWithText("刷新频率（分钟）").assertDoesNotExist()
        composeRule.onNodeWithText("仅 Wi-Fi 下刷新").assertDoesNotExist()
        composeRule.onNodeWithText("打开应用时刷新").assertIsDisplayed()
        composeRule.onNodeWithText("失败后自动重试").assertIsDisplayed()
    }

    @Test
    fun switchInteractionPersists() {
        setContent()

        composeRule.onNodeWithContentDescription("打开应用时刷新").performClick()
        composeRule.waitUntil(5_000) { !refreshStore.state.value.openAppRefresh }

        assertFalse(refreshStore.state.value.openAppRefresh)
    }

    @Test
    fun migrationEntryInvokesCallback() {
        var opened = 0
        setContent(onOpenTransfer = { opened++ })

        composeRule.onNodeWithText("从 Mac 导入").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("从 Mac 导入").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun aboutSectionUsesIconsAndConciseCopy() {
        setContent()

        scrollToAbout()
        composeRule.onNodeWithText("关于").assertIsDisplayed()
        composeRule.onNodeWithText("MyToken 0.1.0", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("当前版本修复内容").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("打开 GitHub").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("查看历史版本").assertIsDisplayed()
        composeRule.onNodeWithText("更新日志").assertIsDisplayed()
        composeRule.onNodeWithText("当前版本更新日志").assertDoesNotExist()
        composeRule.onNodeWithText("应用更新").assertDoesNotExist()
        composeRule.onNodeWithText("查看历史版本").assertDoesNotExist()
        composeRule.onNodeWithTag("update_channel_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("check_updates_button").assertIsDisplayed()

        // macOS-only settings must not appear on Android.
        composeRule.onNodeWithText("菜单栏").assertDoesNotExist()
        composeRule.onNodeWithText("登录项").assertDoesNotExist()
        assertTrue(displayStore.state.value.showDisabledCredentials)
    }

    @Test
    fun historyIconOpensReleaseHistoryDialog() {
        setContent()
        scrollToAbout()

        composeRule.onNodeWithContentDescription("查看历史版本").performClick()

        composeRule.onNodeWithText("历史版本").assertIsDisplayed()
        composeRule.onNodeWithText("历史版本修复内容").assertIsDisplayed()
    }

    @Test
    fun installPermissionUsesSinglePrimaryAction() {
        var openedPermissionSettings = 0
        setContent(
            updateState = AppUpdateUiState.NeedsInstallPermission("0.2.0"),
            onOpenInstallPermissionSettings = { openedPermissionSettings++ },
        )
        scrollToAbout()

        composeRule.onNodeWithText("授权并安装").assertIsDisplayed()
        composeRule.onNodeWithText("打开安装权限设置").assertDoesNotExist()
        composeRule.onNodeWithText("授权后继续安装").assertDoesNotExist()
        composeRule.onNodeWithTag("install_permission_button").performClick()

        assertEquals(1, openedPermissionSettings)
    }

    private fun scrollToAbout() {
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("关于"))
        composeRule.waitForIdle()
    }
}
