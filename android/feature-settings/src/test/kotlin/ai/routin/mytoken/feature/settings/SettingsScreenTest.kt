package ai.routin.mytoken.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        onCheckForUpdates: () -> Unit = {},
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
            var themeMode by remember { mutableStateOf(displayStore.state.value.themeMode) }
            MaterialTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        isLoading = false,
                        refresh = refreshStore.state.value,
                        display = displayStore.state.value.copy(themeMode = themeMode),
                        update = UpdateSettings(mirrorBase = mirrorBase),
                    ),
                    appVersion = "0.1.0",
                    updateState = updateState,
                    releaseHistoryState = releaseHistoryState,
                    onOpenAppRefreshChange = { refreshStore.state.value = refreshStore.state.value.copy(openAppRefresh = it) },
                    onRetryOnFailureChange = { refreshStore.state.value = refreshStore.state.value.copy(retryOnFailure = it) },
                    onThemeModeChange = {
                        themeMode = it
                        displayStore.state.value = displayStore.state.value.copy(themeMode = it)
                    },
                    onOpenTransfer = onOpenTransfer,
                    onOpenInstallPermissionSettings = onOpenInstallPermissionSettings,
                    onCheckForUpdates = onCheckForUpdates,
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
        listOf("外观", "通知", "数据", "关于").forEach { group ->
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
    fun themeSelectorUsesThreePreviewTilesInOneRow() {
        setContent()

        val tags = listOf("theme_option_SYSTEM", "theme_option_LIGHT", "theme_option_DARK")
        val tops = tags.map { tag ->
            composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value
        }
        assertEquals("三个主题选项应在同一行", 1, tops.distinct().size)
        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
            composeRule.onNodeWithTag(
                tag.replace("theme_option_", "theme_scene_"),
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("theme_option_SYSTEM").assertIsSelected()
        composeRule.onNodeWithTag("theme_option_DARK").performClick().assertIsSelected()
        composeRule.onNodeWithTag("theme_option_SYSTEM").assertIsNotSelected()
    }

    @Test
    fun drawerShowsCloseButton() {
        var closed = 0
        setContent(drawerMode = true, onClose = { closed++ })

        composeRule.onNodeWithContentDescription("关闭设置", useUnmergedTree = true).assertIsDisplayed()
        val closeBounds = composeRule.onNodeWithTag("settings_close", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val titleBounds = composeRule.onNodeWithTag("settings_title", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "关闭按钮应位于标题左侧，实际 close.right=${closeBounds.right.value} title.left=${titleBounds.left.value}",
            closeBounds.right.value <= titleBounds.left.value,
        )
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
        composeRule.onNodeWithTag("settings_drawer_content").performScrollToNode(hasText("关于"))
        composeRule.onNodeWithText("关于").assertIsDisplayed()
    }

    @Test
    fun longPressAboutVersionExportsLogs() {
        var exportCalls = 0
        setContent(onExportLogs = { exportCalls++ })

        scrollToAbout()
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
    fun aboutSectionMatchesDesignRows() {
        setContent()

        scrollToAbout()
        composeRule.onNodeWithText("关于").assertIsDisplayed()
        composeRule.onNodeWithText("关于与更新").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_about_version").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("版本").assertIsDisplayed()
        composeRule.onNodeWithText("检查更新").assertIsDisplayed()
        composeRule.onNodeWithText("更新日志").assertExists()
        composeRule.onNodeWithText("更新源与令牌").assertExists()
        composeRule.onNodeWithContentDescription("打开 GitHub").assertDoesNotExist()
        composeRule.onNodeWithText("当前版本修复内容").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("查看历史版本").assertDoesNotExist()
        composeRule.onNodeWithText("GitHub 令牌").assertDoesNotExist()
        composeRule.onNodeWithTag("check_updates_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_check_updates").assertExists()
        composeRule.onNodeWithTag("update_channel_selector").assertDoesNotExist()

        // macOS-only settings must not appear on Android.
        composeRule.onNodeWithText("菜单栏").assertDoesNotExist()
        composeRule.onNodeWithText("登录项").assertDoesNotExist()
        assertTrue(displayStore.state.value.showDisabledCredentials)
    }

    @Test
    fun settingsProfileUsesProductLogo() {
        setContent()

        val logo = composeRule.onNodeWithTag("settings_brand_logo", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertEquals("品牌 logo 应保持 44dp", 44f, (logo.bottom - logo.top).value, 1f)
        composeRule.onNodeWithText("M").assertDoesNotExist()
    }

    @Test
    fun settingsRowsAndSectionsUseDesignRhythm() {
        setContent()

        // 设计稿为开关行补了说明文案；两行行高不再显得空旷。
        composeRule.onNodeWithText("启动后立即拉取一次用量").assertIsDisplayed()
        composeRule.onNodeWithText("网络波动时静默重试一次").assertIsDisplayed()
        composeRule.onNodeWithText("Key 失效或权限不足时通知").assertIsDisplayed()

        val header = composeRule.onNodeWithTag("settings_header").getUnclippedBoundsInRoot()
        val profile = composeRule.onNodeWithTag("settings_profile").getUnclippedBoundsInRoot()
        val headerGap = profile.top.value - header.bottom.value
        assertTrue("资料卡应贴近页头，实际 ${headerGap}dp", headerGap <= 8f)

        val appearance = composeRule.onNodeWithText("外观").getUnclippedBoundsInRoot()
        val groupGap = appearance.top.value - profile.bottom.value
        assertTrue("资料卡与首个分组间距应≤18dp，实际 ${groupGap}dp", groupGap <= 18f)

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("通知"))
        val refresh = composeRule.onNodeWithTag("settings_refresh_cell").getUnclippedBoundsInRoot()
        val notification = composeRule.onNodeWithText("通知").getUnclippedBoundsInRoot()
        val sectionGap = notification.top.value - refresh.bottom.value
        assertTrue("设置分组间距应≤18dp，实际 ${sectionGap}dp", sectionGap <= 18f)
    }

    @Test
    fun aboutCheckUpdateRowStartsCheck() {
        var checks = 0
        setContent(onCheckForUpdates = { checks++ })

        scrollToAbout()
        composeRule.onNodeWithTag("settings_check_updates")
            .performScrollTo()
            .performClick()

        assertEquals(1, checks)
    }

    @Test
    fun historyIconOpensReleaseHistoryDialog() {
        setContent()
        scrollToAbout()
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
        scrollToAbout()

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
        composeRule.onNodeWithTag("settings_release_notes_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_update_advanced_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("update_channel_selector").assertDoesNotExist()
        composeRule.onNodeWithText("GitHub 令牌").assertDoesNotExist()
    }

    @Test
    fun aboutRowSurfacesAvailableUpdate() {
        setContent(
            updateState = AppUpdateUiState.Available(
                version = "0.2.0",
                releaseNotes = "新版本说明",
                downloadUrl = "https://example.com/MyToken.apk",
            ),
        )

        scrollToAbout()
        composeRule.onNodeWithText("发现 v0.2.0").assertIsDisplayed()
        composeRule.onNodeWithText("下载并安装").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aboutAndUpdateDetailsDiscloseProgressively() {
        setContent()
        scrollToAbout()

        composeRule.onNodeWithTag("settings_check_updates").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("关于"))
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
