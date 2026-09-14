package ai.routin.mytoken.feature.transfer

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h1100dp")
class TransferAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 平板导入预览使用左右双栏并限制总宽度() {
        composeRule.setContent {
            MyTokenTheme {
                TransferPreviewScreen(
                    state = TransferUiState.PreviewReady(
                        preview = ImportPreview(
                            providerCount = 2,
                            credentialCount = 3,
                            sensitiveItemCount = 3,
                        ),
                        items = listOf(
                            TransferImportItem(
                                credentialId = UUID.randomUUID(),
                                providerId = "routin",
                                credentialKind = "bearerAPIKey",
                                name = "主力 Key",
                                hasSecret = true,
                                conflictsWithExisting = false,
                            ),
                        ),
                        exportedAt = "2026-09-14T00:00:00Z",
                    ),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithTag("transfer_summary_pane").assertExists()
        composeRule.onNodeWithTag("transfer_list_pane").assertExists()
        val bounds = composeRule.onNodeWithTag("transfer_preview_content").getUnclippedBoundsInRoot()
        assertTrue(bounds.width <= 960.dp)
    }

    @Test
    fun 平板完成页限制最大宽度() {
        composeRule.setContent {
            MyTokenTheme {
                TransferCompletedScreen(
                    summary = ImportSummary(
                        importedCount = 2,
                        overwrittenCount = 0,
                        skippedCount = 1,
                        skippedWithoutSecretCount = 0,
                    ),
                    layoutMode = MyTokenLayoutMode.Expanded,
                    onDone = {},
                )
            }
        }

        val bounds = composeRule.onNodeWithTag("transfer_completed_content").getUnclippedBoundsInRoot()
        assertTrue(bounds.width <= 480.dp)
    }
}
