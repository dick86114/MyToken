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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
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
        logExportState: LogExportUiState = LogExportUiState.Idle,
        onExportLogs: () -> Unit = {},
        onShareLogs: () -> Unit = {},
        drawerMode: Boolean = false,
        onClose: (() -> Unit)? = null,
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
                    logExportState = logExportState,
                    onExportLogs = onExportLogs,
                    onShareLogs = onShareLogs,
                    drawerMode = drawerMode,
                    onClose = onClose,
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
        listOf("外观", "通知", "数据", "关于与更新").forEach { group ->
            composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText(group))
            composeRule.onNodeWithText(group).assertIsDisplayed()
        }
    }

    @Test
    fun switchInteractionPersists() {
        setContent()

        composeRule.onNodeWithContentDescription("打开应用时刷新").performClick()
        composeRule.waitUntil(5_000) { !refreshStore.state.value.openAppRefresh }

        assertFalse(refreshStore.state.value.openAppRefresh)
    }

    @Test
    fun dataEntryInvokesTransferCallback() {
        var opened = 0
        setContent(onOpenTransfer = { opened++ })

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("数据"))
        composeRule.onNodeWithText("从 Mac 导入").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun dataGroupExportsLogs() {
        var exportCalls = 0
        setContent(onExportLogs = { exportCalls++ })

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("数据"))
        composeRule.onNodeWithTag("settings_export_logs").performClick()

        assertEquals(1, exportCalls)
    }

    @Test
    fun drawerShowsCloseButton() {
        var closed = 0
        setContent(drawerMode = true, onClose = { closed++ })

        composeRule.onNodeWithContentDescription("关闭设置", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_close", useUnmergedTree = true).performClick()

        assertEquals(1, closed)
    }

    @Test
    @Config(qualifiers = "w343dp-h800dp")
    fun drawerSettingsSurvivesLargeFontAndGestureInsets() {
        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    androidx.compose.ui.platform.LocalDensity.current.density,
                    1.3f,
                ),
            ) {
                MaterialTheme {
                    SettingsScreen(
                        state = SettingsUiState(isLoading = false),
                        appVersion = "0.1.0",
                        onOpenAppRefreshChange = {},
                        onRetryOnFailureChange = {},
                        onThemeModeChange = {},
                        onOpenTransfer = {},
                        drawerMode = true,
                        onClose = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("settings_close", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_theme_cell").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_refresh_cell").performScrollTo().assertIsDisplayed()
        // 列表可滚动到底部的数据/关于分组，说明底部留白与手势区不遮挡内容。
        composeRule.onNodeWithTag("settings_drawer_content").performScrollToNode(hasText("关于与更新"))
        composeRule.onNodeWithText("关于与更新").assertIsDisplayed()
    }

    @Test
    fun longPressAboutVersionExportsLogs() {
        var exportCalls = 0
        setContent(onExportLogs = { exportCalls++ })

        expandAbout()
        composeRule.onNodeWithTag("settings_about_version")
            .performScrollTo()
            .performTouchInput { longClick() }

        assertEquals(1, exportCalls)
    }

    @Test
    fun exportedLogDialogOffersShare() {
        var shareCalls = 0
        setContent(
            logExportState = LogExportUiState.Done(
                fileName = "MyToken-logs-20261015-220000.zip",
                savedLocation = "下载/MyToken/MyToken-logs-20261015-220000.zip",
                sizeBytes = 2048,
            ),
            onShareLogs = { shareCalls++ },
        )

        composeRule.onNodeWithText("日志已导出").assertIsDisplayed()
        composeRule.onNodeWithText("分享").performClick()

        assertEquals(1, shareCalls)
    }

    @Test
    fun aboutSectionUsesIconsAndConciseCopy() {
        setContent()

        scrollToAbout()
        expandAbout(alreadyScrolled = true)
        composeRule.onNodeWithText("关于与更新").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_about_version").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("打开 GitHub").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("更新日志").assertExists()
        composeRule.onNodeWithText("更新源与令牌").assertExists()
        composeRule.onNodeWithText("当前版本修复内容").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("查看历史版本").assertDoesNotExist()
        composeRule.onNodeWithText("GitHub 令牌").assertDoesNotExist()
        composeRule.onNodeWithText("当前版本更新日志").assertDoesNotExist()
        composeRule.onNodeWithText("应用更新").assertDoesNotExist()
        composeRule.onNodeWithText("查看历史版本").assertDoesNotExist()
        composeRule.onNodeWithTag("check_updates_button").assertExists()
        composeRule.onNodeWithTag("update_channel_selector").assertDoesNotExist()

        // macOS-only settings must not appear on Android.
        composeRule.onNodeWithText("菜单栏").assertDoesNotExist()
        composeRule.onNodeWithText("登录项").assertDoesNotExist()
        assertTrue(displayStore.state.value.showDisabledCredentials)
    }

    @Test
    fun historyIconOpensReleaseHistoryDialog() {
        setContent()
        expandAbout()
        expandReleaseNotes()

        composeRule.onNodeWithContentDescription("查看历史版本")
            .performScrollTo()
            .performClick()

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
        expandAbout()

        composeRule.onNodeWithText("授权并安装").assertExists()
        composeRule.onNodeWithText("打开安装权限设置").assertDoesNotExist()
        composeRule.onNodeWithText("授权后继续安装").assertDoesNotExist()
        composeRule.onNodeWithTag("install_permission_button")
            .performScrollTo()
            .performClick()

        assertEquals(1, openedPermissionSettings)
    }

    @Test
    fun advancedSettingsAreCollapsedUntilRequested() {
        setContent()

        scrollToAbout()
        composeRule.onNodeWithTag("settings_about_toggle").assertIsDisplayed()
        composeRule.onNodeWithText("检测更新").assertDoesNotExist()
        composeRule.onNodeWithText("更新日志").assertDoesNotExist()
        composeRule.onNodeWithText("更新源与令牌").assertDoesNotExist()
    }

    @Test
    fun collapsedAboutSummarySurfacesAvailableUpdate() {
        setContent(
            updateState = AppUpdateUiState.Available(
                version = "0.2.0",
                releaseNotes = "新版本说明",
                downloadUrl = "https://example.com/MyToken.apk",
            ),
        )

        scrollToAbout()
        composeRule.onNodeWithText("MyToken 0.1.0 · 发现新版本 v0.2.0").assertIsDisplayed()
        composeRule.onNodeWithText("下载并安装").assertDoesNotExist()
    }

    @Test
    fun aboutAndUpdateDetailsDiscloseProgressively() {
        setContent()
        expandAbout()

        composeRule.onNodeWithText("检测更新").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("更新日志").assertExists()
        composeRule.onNodeWithText("更新源与令牌").assertExists()
        composeRule.onNodeWithText("GitHub 令牌").assertDoesNotExist()

        expandReleaseNotes()
        composeRule.onNodeWithText("当前版本修复内容").assertExists()
        composeRule.onNodeWithContentDescription("查看历史版本").assertExists()

        expandUpdateAdvanced()
        composeRule.onNodeWithText("GitHub 令牌").assertExists()
        composeRule.onNodeWithTag("update_channel_selector").assertExists()
    }

    private fun scrollToAbout() {
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("关于与更新"))
        composeRule.waitForIdle()
    }

    private fun expandAbout(alreadyScrolled: Boolean = false) {
        if (!alreadyScrolled) {
            scrollToAbout()
        }
        composeRule.onNodeWithTag("settings_about_toggle").performClick()
        composeRule.waitForIdle()
    }

    private fun expandReleaseNotes() {
        composeRule.onNodeWithTag("settings_release_notes_toggle")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun expandUpdateAdvanced() {
        composeRule.onNodeWithTag("settings_update_advanced_toggle")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }
}
