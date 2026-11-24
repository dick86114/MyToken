package ai.routin.mytoken.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric Compose rendering of the 通知 settings section. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class NotificationSettingsSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var failureAlerts: Boolean? = null
    private var notificationsEnabled: Boolean? = null
    private var savedThresholds: Pair<Int, Int>? = null

    private fun setContent(
        settings: NotificationSettings = NotificationSettings(),
        permissionGranted: Boolean = true,
    ) {
        composeRule.setContent {
            MaterialTheme {
                NotificationSettingsSection(
                    settings = settings,
                    permissionGranted = permissionGranted,
                    onRequestPermission = {},
                    onCredentialFailureAlertsChange = { failureAlerts = it },
                    onNotificationsEnabledChange = { notificationsEnabled = it },
                    onThresholdsChange = { low, high -> savedThresholds = low to high },
                )
            }
        }
    }

    private fun setRange(low: Float, high: Float) {
        val hasSetProgress = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        composeRule.onAllNodes(hasSetProgress)[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(low) }
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetProgress)[1]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(high) }
        composeRule.waitForIdle()
    }

    private fun openThresholdDialog() {
        composeRule.onNodeWithTag("settings_alert_threshold_edit", useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    private fun saveThresholdDialog() {
        composeRule.onNodeWithTag("settings_threshold_save", useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun rendersThresholdSummaryAndEditEntry() {
        setContent()

        composeRule.onNodeWithText("通知").assertIsDisplayed()
        composeRule.onNodeWithText("启用通知").assertIsDisplayed()
        composeRule.onNodeWithText("启用提醒").assertDoesNotExist()
        composeRule.onNodeWithText("用量提醒阈值").assertIsDisplayed()
        // 收起时小字直接展示当前阈值范围。
        composeRule.onNodeWithText("同时影响首页状态与通知").assertDoesNotExist()
        composeRule.onNodeWithText("低 80% · 高 95%").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_alert_threshold_edit").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_alert_threshold_range").assertDoesNotExist()
        composeRule.onNodeWithText("凭证失效提醒").assertIsDisplayed()
        // 已授权时系统通知行整体隐藏。
        composeRule.onNodeWithText("系统通知").assertDoesNotExist()
        composeRule.onNodeWithText("已允许").assertDoesNotExist()
        composeRule.onNodeWithText("去授权").assertDoesNotExist()

        openThresholdDialog()
        composeRule.onNodeWithTag("settings_threshold_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("同时影响首页状态与通知").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_alert_threshold_range").assertIsDisplayed()
        composeRule.onNodeWithText("保存").assertIsDisplayed()
    }

    @Test
    fun grantedPermissionHidesSystemNotificationEntry() {
        setContent(permissionGranted = true)

        composeRule.onNodeWithText("系统通知").assertDoesNotExist()
        composeRule.onNodeWithText("已允许").assertDoesNotExist()
        composeRule.onNodeWithText("去授权").assertDoesNotExist()
    }

    @Test
    fun deniedPermissionShowsStatusAndAuthorizationEntry() {
        var requested = 0
        composeRule.setContent {
            MaterialTheme {
                NotificationSettingsSection(
                    settings = NotificationSettings(),
                    permissionGranted = false,
                    onRequestPermission = { requested++ },
                    onCredentialFailureAlertsChange = {},
                )
            }
        }

        composeRule.onNodeWithText("系统通知").assertIsDisplayed()
        composeRule.onNodeWithText("未授予").assertIsDisplayed()
        composeRule.onNodeWithText("去授权").performClick()
        assertEquals(1, requested)
    }

    @Test
    fun togglesEmitChanges() {
        setContent()

        composeRule.onNodeWithContentDescription("凭证失效提醒").performClick()
        assertFalse(failureAlerts!!)

        composeRule.onNodeWithContentDescription("启用通知").performClick()
        assertFalse(notificationsEnabled!!)
    }

    @Test
    fun rangeSliderUpdatesDraftAndSavesOnConfirm() {
        setContent()
        openThresholdDialog()

        setRange(low = 70f, high = 95f)
        composeRule.onNodeWithText("低 70% · 高 95%").assertIsDisplayed()
        assertNull(savedThresholds)

        saveThresholdDialog()
        assertEquals(70 to 95, savedThresholds)
        composeRule.onNodeWithText("低 70% · 高 95%").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_threshold_dialog").assertDoesNotExist()
    }

    @Test
    fun cancelKeepsSavedThresholds() {
        setContent()
        openThresholdDialog()

        setRange(low = 70f, high = 90f)
        composeRule.onNodeWithTag("settings_threshold_cancel", useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()

        assertNull(savedThresholds)
        composeRule.onNodeWithText("低 80% · 高 95%").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_threshold_dialog").assertDoesNotExist()
    }

    @Test
    fun rangeSliderKeepsAtLeastOnePointBetweenThumbs() {
        setContent()
        openThresholdDialog()

        // Drag the low thumb onto the high thumb; the range normalizes to 94/95.
        setRange(low = 95f, high = 95f)
        composeRule.onNodeWithText("低 94% · 高 95%").assertIsDisplayed()

        saveThresholdDialog()
        assertEquals(94 to 95, savedThresholds)
    }

    @Test
    fun masterSwitchOffHidesThresholdSettings() {
        setContent(settings = NotificationSettings(notificationsEnabled = false))

        composeRule.onNodeWithContentDescription("启用通知").assertIsEnabled()
        composeRule.onNodeWithContentDescription("凭证失效提醒").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_alert_threshold_range").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_alert_threshold_edit").assertDoesNotExist()
        composeRule.onNodeWithText("用量提醒阈值").assertDoesNotExist()
        composeRule.onNodeWithText("低 80% · 高 95%").assertDoesNotExist()
        assertNull(savedThresholds)
    }
}
