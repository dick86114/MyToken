package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h1100dp")
class CredentialEditorAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 平板编辑表单限制最大宽度() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialEditorScreen(
                    state = CredentialEditorUiState(isLoading = false),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    onBack = {},
                    onProviderChange = {},
                    onNameChange = {},
                    onApiKeyChange = {},
                    onAccessKeyIDChange = {},
                    onSecretAccessKeyChange = {},
                    onRegionChange = {},
                    onBaseURLChange = {},
                    onUserIDChange = {},
                    onWebsiteURLChange = {},
                    onToggleSecretVisible = {},
                    onTestConnection = {},
                    onSave = {},
                )
            }
        }

        val bounds = composeRule.onNodeWithTag("editor_form").getUnclippedBoundsInRoot()
        assertTrue(bounds.width <= 720.dp)
    }
}
