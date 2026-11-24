package ai.routin.mytoken

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The Settings page only surfaces the system-notification row while permission is
 * missing. Granting/revoking (what a POST_NOTIFICATIONS change causes on real devices)
 * and returning to the app (no activity recreation) must update it reactively.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class MainActivityNotificationPermissionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun toggleNotifications(activity: MainActivity, enabled: Boolean) {
        val manager = activity.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(enabled)
        // Real devices also drop the runtime permission when notifications are disabled.
        if (enabled) {
            shadowOf(activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            shadowOf(activity).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun relaunchThroughLifecycle() {
        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    private fun openSettingsAndScrollTo(text: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_open_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_drawer_content").performScrollToNode(
            hasText(text),
        )
    }

    @Test
    fun systemNotificationRowOnlyVisibleWhileNotGranted() {
        composeRule.activityRule.scenario.onActivity { toggleNotifications(it, enabled = true) }
        openSettingsAndScrollTo("凭证失效提醒")
        composeRule.onNodeWithText("系统通知").assertDoesNotExist()
        composeRule.onNodeWithText("已允许").assertDoesNotExist()

        // Simulate revocation in system settings + return to the app.
        composeRule.activityRule.scenario.onActivity { toggleNotifications(it, enabled = false) }
        relaunchThroughLifecycle()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_drawer_content")
            .performScrollToNode(hasText("系统通知"))
        composeRule.onNodeWithText("系统通知").assertIsDisplayed()
        composeRule.onNodeWithText("未授予").assertIsDisplayed()

        // Re-granting hides the row again on the next resume.
        composeRule.activityRule.scenario.onActivity { toggleNotifications(it, enabled = true) }
        relaunchThroughLifecycle()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("系统通知").assertDoesNotExist()
        composeRule.onNodeWithText("未授予").assertDoesNotExist()
    }
}
