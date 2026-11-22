package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class CredentialListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCredentialRepository
    private lateinit var orderStore: FakeCredentialOrderStore
    private lateinit var viewModel: CredentialListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCredentialRepository()
        orderStore = FakeCredentialOrderStore()
        viewModel = CredentialListViewModel(repository, orderStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun advanceVm() {
        dispatcher.scheduler.advanceUntilIdle()
    }

    /** Polls [condition] while pumping both the VM scheduler and the Compose clock. */
    private fun waitUntilVm(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis) {
            advanceVm()
            composeRule.waitForIdle()
            condition()
        }
    }

    private fun credential(id: String, name: String, provider: ProviderId = ProviderId.Routin) =
        Credential(
            id = UUID.fromString(id.padStart(8, '0') + "-0000-0000-0000-000000000000"),
            providerId = provider,
            credentialKind = CredentialKind.BearerApiKey,
            name = name,
        )

    private fun setContent() {
        composeRule.setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                CredentialListScreen(
                    state = state,
                    onToggleEnabled = viewModel::toggleEnabled,
                    onMove = viewModel::move,
                    onEditCredential = {},
                    onRequestDelete = viewModel::requestDelete,
                    onDismissDelete = viewModel::dismissDelete,
                    onConfirmDelete = viewModel::confirmDelete,
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        advanceVm()
        composeRule.waitForIdle()
    }

    @Test
    fun rendersFlatRows() {
        repository.credentials.value = listOf(
            credential("00000001", "主力 Key"),
            credential("00000002", "备用 Key", provider = ProviderId.DeepSeek),
        )
        advanceVm()
        setContent()

        composeRule.onNodeWithText("主力 Key").assertIsDisplayed()
        composeRule.onNodeWithText("备用 Key").performScrollTo()
        composeRule.onNodeWithText("2 个凭证 · 2 个启用").assertIsDisplayed()
    }

    @Test
    fun enableSwitchShowsStateAndToggles() {
        val key = credential("00000001", "主力 Key")
        repository.credentials.value = listOf(key)
        repository.secrets.value = mapOf(key.id to CredentialSecret.BearerToken("plan-secret-8F2A"))
        advanceVm()
        setContent()

        composeRule.onNodeWithContentDescription("启用 主力 Key").assertIsOn()
        composeRule.onNodeWithContentDescription("启用 主力 Key").performClick()
        waitUntilVm { !repository.credentials.value.single().isEnabled }

        assertTrue(repository.savedSecrets.value.containsKey(key.id))
        assertEquals(
            CredentialSecret.BearerToken("plan-secret-8F2A"),
            repository.secrets.value[key.id],
        )
    }

    @Test
    fun deleteShowsConfirmationDialogWithLocalOnlyScopeAndDeletesOnConfirm() {
        val key = credential("00000001", "主力 Key")
        repository.credentials.value = listOf(key)
        advanceVm()
        setContent()

        composeRule.onNodeWithContentDescription("更多操作 主力 Key").performClick()
        composeRule.onNodeWithText("删除 主力 Key").performClick()
        waitUntilVm { viewModel.state.value.pendingDeletion != null }
        composeRule.onNodeWithText("删除凭证").assertIsDisplayed()
        composeRule.onNodeWithText("不会删除或修改供应商账户", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("删除").performClick()
        waitUntilVm { repository.deletedIds.value.isNotEmpty() }
        assertEquals(listOf(key.id), repository.deletedIds.value)
    }

    @Test
    fun deleteCancelKeepsCredential() {
        val key = credential("00000001", "主力 Key")
        repository.credentials.value = listOf(key)
        advanceVm()
        setContent()

        composeRule.onNodeWithContentDescription("更多操作 主力 Key").performClick()
        composeRule.onNodeWithText("删除 主力 Key").performClick()
        waitUntilVm { viewModel.state.value.pendingDeletion != null }
        composeRule.onNodeWithText("取消").performClick()
        waitUntilVm { viewModel.state.value.pendingDeletion == null }

        assertNull(viewModel.state.value.pendingDeletion)
        assertTrue(repository.deletedIds.value.isEmpty())
    }

    @Test
    fun emptyStateShowsImportAndManualAddEntries() {
        setContent()

        composeRule.onNodeWithText("还没有凭证").assertIsDisplayed()
        composeRule.onNodeWithText("从 Mac 导入").assertIsDisplayed()
        composeRule.onNodeWithText("手动添加").assertIsDisplayed()
    }

    @Test
    fun emptyStateUsesBrandMarkAndTwoActions() {
        setContent()

        composeRule.onNodeWithTag("credential_empty_mark", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("从 Mac 导入").assertIsDisplayed()
        composeRule.onNodeWithText("手动添加").assertIsDisplayed()
    }

    @Test
    fun disabledRowUsesNeutralLabelWithoutDimming() {
        val key = credential("00000001", "停用 Key").copy(isEnabled = false)
        repository.credentials.value = listOf(key)
        advanceVm()
        setContent()

        composeRule.onNodeWithText("已停用", useUnmergedTree = true).assertExists()
        val rowBounds = composeRule.onNodeWithTag("credential_row_${key.id}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "列表行高度应不小于 64dp（实际 ${rowBounds.bottom - rowBounds.top}）",
            rowBounds.bottom - rowBounds.top >= 64.dp,
        )
    }

    @Test
    fun rowsExposeProviderAndKind() {
        repository.credentials.value = listOf(credential("00000001", "主力 Key"))
        advanceVm()
        setContent()

        composeRule.onNodeWithText("Routin · Bearer Token", useUnmergedTree = true).assertExists()
    }

    @Test
    fun listShowsSectionHeaderAndPrivacyFooter() {
        repository.credentials.value = listOf(credential("00000001", "主力 Key"))
        advanceVm()
        setContent()

        composeRule.onNodeWithText("全部凭证").assertIsDisplayed()
        composeRule.onNodeWithText("长按拖动排序").assertIsDisplayed()
        composeRule.onNodeWithTag("credential_privacy_footer", useUnmergedTree = true).assertExists()
    }

    @Test
    fun editButtonIsAvailableAndRowShowsKind() {
        val key = credential("00000001", "主力 Key")
        repository.credentials.value = listOf(key)
        advanceVm()
        var edited: UUID? = null
        composeRule.setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                CredentialListScreen(
                    state = state,
                    onToggleEnabled = viewModel::toggleEnabled,
                    onMove = viewModel::move,
                    onEditCredential = { edited = it },
                    onRequestDelete = viewModel::requestDelete,
                    onDismissDelete = viewModel::dismissDelete,
                    onConfirmDelete = viewModel::confirmDelete,
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        advanceVm()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("更多操作 主力 Key").performClick()
        composeRule.onNodeWithText("编辑 主力 Key").performClick()
        assertEquals(key.id, edited)
        composeRule.onNodeWithText("Routin · Bearer Token", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tappingCredentialRowOpensEditor() {
        val key = credential("00000001", "主力 Key")
        repository.credentials.value = listOf(key)
        advanceVm()
        var edited: UUID? = null
        composeRule.setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                CredentialListScreen(
                    state = state,
                    onToggleEnabled = viewModel::toggleEnabled,
                    onMove = viewModel::move,
                    onEditCredential = { edited = it },
                    onRequestDelete = viewModel::requestDelete,
                    onDismissDelete = viewModel::dismissDelete,
                    onConfirmDelete = viewModel::confirmDelete,
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        advanceVm()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("主力 Key").performClick()
        assertEquals(key.id, edited)
    }

    @Test
    fun `抽屉模式不叠加状态栏内边距`() {
        val source = File(System.getProperty("user.dir"))
            .walkTopDown()
            .first { it.name == "CredentialListScreen.kt" }
            .readText()

        assertTrue(source.contains("contentWindowInsets = if (applyWindowInsets)"))
        assertTrue(source.contains("WindowInsets(0, 0, 0, 0)"))
    }
}
