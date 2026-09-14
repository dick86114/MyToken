package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CredentialListAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w1000dp-h1100dp")
    fun 平板使用可拖拽双列网格() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialListScreen(
                    state = CredentialListUiState(
                        isLoading = false,
                        items = listOf(
                            credential("主力 Key"),
                            credential("备用 Key"),
                        ),
                    ),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    onToggleEnabled = {},
                    onMove = { _, _ -> },
                    onEditCredential = {},
                    onRequestDelete = {},
                    onDismissDelete = {},
                    onConfirmDelete = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("credential_grid").assertExists()
        composeRule.onAllNodesWithContentDescription("长按拖动排序").onFirst().assertExists()
    }

    private fun credential(name: String) = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Routin,
        credentialKind = CredentialKind.BearerApiKey,
        name = name,
    )
}
