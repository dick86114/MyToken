package ai.routin.mytoken.core.ui

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp")
class MyTokenComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchExposesRoleAndTogglesOff() {
        var checked = true
        composeRule.setContent {
            MyTokenTheme {
                MyTokenSwitch(checked, { checked = it }, contentDescription = "启用 A")
            }
        }

        composeRule.onNodeWithContentDescription("启用 A").assertIsOn().performClick()
        assertFalse(checked)
    }

    @Test
    fun segmentedSelectsSecondOption() {
        var selected = 0
        composeRule.setContent {
            MyTokenTheme {
                MyTokenSegmented(
                    options = listOf("跟随系统", "浅色", "深色"),
                    selectedIndex = selected,
                    onSelect = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("浅色").performClick()
        assertEquals(1, selected)
    }

    @Test
    fun switchUsesReportYellowAndDarkThumb() {
        val source = File(System.getProperty("user.dir"))
            .walkTopDown()
            .first { it.name == "Components.kt" }
            .readText()

        assertTrue(source.contains("if (checked) colors.reportTicket else colors.track"))
        assertTrue(source.contains("if (checked) colors.reportTicketInk else colors.surface"))
    }

    @Test
    fun surfaceUsesSolidBackgroundAndHairline() {
        val source = File(System.getProperty("user.dir"))
            .walkTopDown()
            .first { it.name == "Components.kt" }
            .readText()

        assertFalse(source.contains("Brush.verticalGradient"))
        assertFalse(source.contains("copy(alpha = 0.62f)"))
        assertFalse(source.contains("copy(alpha = 0.72f)"))
        assertFalse(source.contains("copy(alpha = 0.78f)"))
        assertTrue(source.contains("border(1.dp"))
        assertTrue(source.contains("MyTokenThemeTokens.colors"))
    }
}
