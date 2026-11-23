package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.alerts.AlertThresholds
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.platform.testTag
import ai.routin.mytoken.domain.model.ProviderId
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class CredentialEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: StaticCredentialRepository
    private lateinit var providers: Map<ai.routin.mytoken.domain.model.ProviderId, ai.routin.mytoken.domain.usage.UsageProvider>
    private lateinit var alertThresholdStore: FakeCredentialAlertThresholdStore
    private lateinit var viewModel: CredentialEditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = StaticCredentialRepository()
        providers = mapOf(
            ai.routin.mytoken.domain.model.ProviderId.Routin to
                FakeUsageProvider(ai.routin.mytoken.domain.model.ProviderId.Routin),
        )
        alertThresholdStore = FakeCredentialAlertThresholdStore()
        viewModel = CredentialEditorViewModel(
            repository = repository,
            providers = providers,
            alertThresholdStore = alertThresholdStore,
            credentialId = null,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Polls [condition] while pumping the VM's test-scheduler and the Compose clock. */
    private fun waitUntilVm(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
            condition()
        }
    }

    private fun setSliderProgress(index: Int, value: Float) {
        val hasSetProgress = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        composeRule.onAllNodes(hasSetProgress)[index]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
        composeRule.waitForIdle()
    }

    private fun setContent() {
        composeRule.setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                CredentialEditorScreen(
                    state = state,
                    onBack = {},
                    onProviderChange = viewModel::setProviderId,
                    onNameChange = viewModel::setName,
                    onApiKeyChange = viewModel::setApiKey,
                    onAccessKeyIDChange = viewModel::setAccessKeyID,
                    onSecretAccessKeyChange = viewModel::setSecretAccessKey,
                    onRegionChange = viewModel::setRegion,
                    onBaseURLChange = viewModel::setBaseURL,
                    onUserIDChange = viewModel::setUserID,
                    onXiaomiUsageKindChange = viewModel::setXiaomiUsageKind,
                    onWebsiteURLChange = viewModel::setWebsiteURL,
                    onBalanceWarningThresholdChange = viewModel::setBalanceWarningThreshold,
                    onCustomAlertThresholdChange = viewModel::setCustomAlertThresholdEnabled,
                    onAlertThresholdRangeChange = viewModel::setAlertThresholdRange,
                    onToggleSecretVisible = viewModel::toggleSecretVisible,
                    onTestConnection = viewModel::testConnection,
                    onSave = viewModel::save,
                )
            }
        }
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    @Test
    fun balanceWarningSliderIsShownOnlyForBalanceProvidersAndWritesIntegers() {
        setContent()
        composeRule.onNodeWithTag("editor_balance_warning_threshold").assertDoesNotExist()

        viewModel.setProviderId(ProviderId.DeepSeek)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        setSliderProgress(
            index = 0,
            value = BalanceWarningThreshold.sliderModel("20").selectedIndex.toFloat(),
        )
        assertEquals("20", viewModel.state.value.balanceWarningThreshold)
        composeRule.onNodeWithText("余额低于 20 时提醒").assertExists()

        setSliderProgress(index = 0, value = 0f)
        assertEquals("", viewModel.state.value.balanceWarningThreshold)
        composeRule.onNodeWithText("不提醒").assertExists()

        viewModel.setProviderId(ProviderId.Xiaomi)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        viewModel.setXiaomiUsageKind("plan")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold").assertDoesNotExist()

        viewModel.setProviderId(ProviderId.OpenCode)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold").assertDoesNotExist()

        viewModel.setProviderId(ProviderId.Glm)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold").assertDoesNotExist()
    }

    @Test
    fun alertThresholdSectionIsShownOnlyForPercentMetricProviders() {
        setContent()
        composeRule.onNodeWithTag("editor_alert_threshold_global", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("跟随全局 80% / 95%").assertExists()
        composeRule.onNodeWithText("同时影响此凭证的首页状态与通知").assertExists()
        composeRule.onNodeWithTag("editor_alert_threshold_range").assertDoesNotExist()

        viewModel.setProviderId(ProviderId.DeepSeek)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_global").assertDoesNotExist()

        viewModel.setProviderId(ProviderId.Xiaomi)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_global").assertDoesNotExist()

        viewModel.setXiaomiUsageKind("plan")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_global", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        viewModel.setProviderId(ProviderId.Glm)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("自定义提醒阈值")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun customThresholdToggleRevealsSliderAndSavesOverride() {
        setContent()

        composeRule.onNodeWithTag("editor_name").performTextInput("主账号")
        composeRule.onNodeWithTag("editor_api_key").performTextInput("plan-main-8F2A")
        composeRule.onNodeWithContentDescription("自定义提醒阈值")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_alert_threshold_range", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        setSliderProgress(index = 0, value = 70f)
        setSliderProgress(index = 1, value = 90f)

        assertEquals(70, viewModel.state.value.alertThresholdLowPercent)
        assertEquals(90, viewModel.state.value.alertThresholdHighPercent)
        composeRule.onNodeWithText("低 70% · 高 90%").assertExists()

        composeRule.onNodeWithText("保存").performClick()
        waitUntilVm { viewModel.state.value.saveCompleted }
        val saved = repository.saved.value.single().first
        assertEquals(AlertThresholds(70, 90), alertThresholdStore.state.value[saved.id])
    }

    @Test
    fun customThresholdSwitchOffReturnsToGlobalHint() {
        setContent()

        composeRule.onNodeWithContentDescription("自定义提醒阈值")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_range", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("自定义提醒阈值")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_range").assertDoesNotExist()
        composeRule.onNodeWithTag("editor_alert_threshold_global", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun newEditorShowsProviderRowInsteadOfChips() {
        setContent()

        composeRule.onNodeWithText("新增凭证").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_provider_row", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("更换", useUnmergedTree = true).assertExists()
        // 供应商列表只在打开 sheet 后出现
        composeRule.onNodeWithTag("editor_provider_tile_routin", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("名称").assertIsDisplayed()
        composeRule.onNodeWithText("API Key（Bearer Token）", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("测试连接").assertIsDisplayed()
        composeRule.onNodeWithText("保存").assertIsDisplayed()
    }

    @Test
    fun providerSheetListsAllProvidersAndSelectionChangesFields() {
        setContent()

        composeRule.onNodeWithTag("editor_provider_row").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_provider_sheet", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("editor_provider_tile_volcengine", useUnmergedTree = true).assertExists()

        // Robolectric 无法把触摸事件派发到 ModalBottomSheet 的 dialog 窗口，
        // 直接调用语义点击动作来验证选择行为。
        composeRule.onNodeWithTag("editor_provider_tile_volcengine")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntilVm { viewModel.state.value.providerId == ProviderId.Volcengine }
        composeRule.waitForIdle()

        // Robolectric 不会销毁 ModalBottomSheet 的 dialog 语义节点，
        // 因此用“选择后供应商行与字段已切换”证明选择生效且面板将要关闭。
        composeRule.onNodeWithText("Access Key ID").assertIsDisplayed()
        composeRule.onNodeWithText("Secret Access Key").assertIsDisplayed()
        composeRule.onNodeWithText("区域（默认 cn-beijing）").assertIsDisplayed()
    }

    @Test
    fun savingInvalidFormShowsChineseValidationErrorAndKeepsInput() {
        setContent()

        composeRule.onNodeWithTag("editor_name").performTextInput("主账号")
        composeRule.onNodeWithTag("editor_api_key").performTextInput("sk-invalid")
        composeRule.onNodeWithText("保存").performClick()
        waitUntilVm { viewModel.state.value.validationError != null }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Key 必须以 plan- 开头").assertIsDisplayed()
        assertFalse(viewModel.state.value.saveCompleted)
        assertTrue(repository.saved.value.isEmpty())
    }

    @Test
    fun secretFieldIsMaskedByDefaultAndVisibilityToggleSwitchesIcon() {
        setContent()

        composeRule.onNodeWithContentDescription("显示密钥").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("显示密钥").performClick()
        composeRule.waitForIdle()

        assertTrue(viewModel.state.value.isSecretVisible)
        composeRule.onNodeWithContentDescription("隐藏密钥").assertIsDisplayed()
    }

    @Test
    fun successfulSaveMarksCompleted() {
        setContent()

        composeRule.onNodeWithTag("editor_name").performTextInput("主账号")
        composeRule.onNodeWithTag("editor_api_key").performTextInput("plan-main-8F2A")
        composeRule.onNodeWithText("保存").performClick()
        waitUntilVm { viewModel.state.value.saveCompleted }
        composeRule.waitForIdle()

        assertTrue(viewModel.state.value.saveCompleted)
        assertEquals("主账号", repository.saved.value.single().first.name)
    }

    @Test
    fun failedConnectionTestDisplaysFailureAndSaveStillPossible() {
        val provider = providers.values.single() as FakeUsageProvider
        provider.enqueue(
            Result.failure(
                ai.routin.mytoken.domain.usage.UsageProviderException.Unauthorized(),
            ),
        )
        setContent()

        composeRule.onNodeWithTag("editor_name").performTextInput("主账号")
        composeRule.onNodeWithTag("editor_api_key").performTextInput("plan-main-8F2A")
        composeRule.onNodeWithText("测试连接").performClick()
        waitUntilVm { viewModel.state.value.testResultMessage != null }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_test_result", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("凭证无效或没有该供应商权限", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("测试失败仍可保存，失败原因不会丢失已填内容", substring = true).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("保存").performClick()
        waitUntilVm { viewModel.state.value.saveCompleted }
        assertTrue(viewModel.state.value.saveCompleted)
        assertEquals(1, repository.saved.value.size)
    }

    @Test
    fun connectionSuccessShowsSuccessMessage() {
        val provider = providers.values.single() as FakeUsageProvider
        provider.enqueue(
            Result.success(
                ai.routin.mytoken.domain.model.UsageSnapshot(
                    credentialId = java.util.UUID.randomUUID(),
                    fetchedAt = java.time.Instant.parse("2026-09-07T12:00:00Z"),
                    metrics = emptyList(),
                ),
            ),
        )
        setContent()

        composeRule.onNodeWithTag("editor_name").performTextInput("主账号")
        composeRule.onNodeWithTag("editor_api_key").performTextInput("plan-main-8F2A")
        composeRule.onNodeWithText("测试连接").performClick()
        waitUntilVm { viewModel.state.value.testResultMessage != null }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("editor_test_result", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("连接成功", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("✓", substring = true).assertDoesNotExist()
    }

    @Test
    fun draggingCustomThresholdSliderDoesNotScrollEditorUntilRelease() {
        setContent()
        composeRule.onNodeWithContentDescription("自定义提醒阈值")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_alert_threshold_range", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        fun sectionTop() = composeRule
            .onNodeWithText("提醒阈值（可选）", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .value

        val topBeforeDrag = sectionTop()
        composeRule.onNodeWithContentDescription(
            "用量提醒阈值 低值",
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            moveBy(Offset(-80f, -140f))
            moveBy(Offset(-40f, -140f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(
            "拖动每凭证阈值滑块不应带动编辑页滚动",
            topBeforeDrag,
            sectionTop(),
            0.5f,
        )
        assertTrue(viewModel.state.value.alertThresholdLowPercent < 80)

        composeRule.onNodeWithText("提醒阈值（可选）", useUnmergedTree = true)
            .performScrollTo()
        val topBeforeSwipe = sectionTop()
        composeRule.onNodeWithText("提醒阈值（可选）", useUnmergedTree = true)
            .performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertTrue(
            "松手后从非滑块区域拖动应恢复编辑页滚动",
            sectionTop() > topBeforeSwipe,
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h600dp")
    fun draggingBalanceSliderDoesNotScrollEditorUntilRelease() {
        setContent()
        viewModel.setProviderId(ProviderId.DeepSeek)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("editor_balance_warning_threshold", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        fun balanceLabelTop() = composeRule
            .onNodeWithText("低余额预警值（可选）", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .value

        val topBeforeDrag = balanceLabelTop()
        composeRule.onNodeWithContentDescription(
            "低余额预警值 当前值",
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            moveBy(Offset(140f, 0f))
            moveBy(Offset(0f, -140f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(
            "拖动低余额滑块不应带动编辑页滚动",
            topBeforeDrag,
            balanceLabelTop(),
            0.5f,
        )
        assertTrue(viewModel.state.value.balanceWarningThreshold.isNotEmpty())

        composeRule.onNodeWithText("官网地址（可选）", useUnmergedTree = true)
            .performScrollTo()
        fun moreSectionTop() = composeRule
            .onNodeWithText("更多（可选）", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .value
        val topBeforeSwipe = moreSectionTop()
        composeRule.onNodeWithText("更多（可选）", useUnmergedTree = true)
            .performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertTrue(
            "松手后从非滑块区域拖动应恢复编辑页滚动",
            moreSectionTop() > topBeforeSwipe,
        )
    }
}
