package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1100dp")
class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val now = Instant.parse("2026-09-07T12:00:00Z")

    private fun credential(name: String, provider: ProviderId = ProviderId.Routin) = Credential(
        id = UUID.randomUUID(),
        providerId = provider,
        credentialKind = CredentialKind.BearerApiKey,
        name = name,
    )

    private fun progress(used: Double, limit: Double) = UsageMetric(
        id = "fiveHour",
        label = "5 小时",
        used = BigDecimal.valueOf(used),
        limit = BigDecimal.valueOf(limit),
        remaining = BigDecimal.valueOf(limit - used),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Unknown,
    )

    private fun balance(value: Double) = UsageMetric(
        id = "balance",
        label = "余额",
        value = BigDecimal.valueOf(value),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Balance,
        semantic = UsageMetricSemantic.Balance,
        currencyCode = "CNY",
        healthState = UsageMetricHealthState.Normal,
    )

    private fun card(
        credential: Credential,
        status: RefreshStatus = RefreshStatus.Ready,
        metrics: List<UsageMetric> = emptyList(),
        isStale: Boolean = false,
        error: AppError? = null,
    ) = CredentialCardUi(
        credential = credential,
        status = status,
        snapshot = UsageSnapshot(credential.id, now, metrics, "成长版", now.minusSeconds(86400), now.plusSeconds(86400)),
        isStale = isStale,
        error = error,
        freshness = if (isStale) Freshness(FreshnessLevel.EXPIRED, "数据已过期") else Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )

    private fun group(provider: ProviderId, cards: List<CredentialCardUi>) = ProviderGroupUi(
        providerId = provider,
        displayName = ProviderCatalog.displayName(provider),
        isCollapsed = false,
        cards = cards,
    )

    @Test
    fun showsStandardCardWithMacStyleFields() {
        val credential = credential("熠")
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential, metrics = listOf(progress(42.0, 100.0)))),
                        groups = listOf(group(ProviderId.Routin, listOf(card(credential, metrics = listOf(progress(42.0, 100.0)))))),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithText("熠").assertIsDisplayed()
        composeRule.onNodeWithText("Routin · 成长版").assertIsDisplayed()
        composeRule.onAllNodesWithText("已用", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("剩余", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun commandCodeCardUsesRequestedMetricLayout() {
        val command = credential("Command Code", ProviderId.CommandCode)
        fun metric(
            id: String,
            label: String,
            used: Double? = null,
            limit: Double? = null,
            remaining: Double? = null,
            value: Double? = null,
            unit: UsageMetricUnit = UsageMetricUnit.Currency,
        ) = UsageMetric(
            id = id,
            label = label,
            used = used?.toBigDecimal(),
            limit = limit?.toBigDecimal(),
            remaining = remaining?.toBigDecimal(),
            value = value?.toBigDecimal(),
            unit = unit,
            presentation = if (used != null) UsageMetricPresentation.Progress else UsageMetricPresentation.Value,
            semantic = if (used != null) UsageMetricSemantic.UsedQuota else UsageMetricSemantic.Value,
            currencyCode = if (unit == UsageMetricUnit.Currency) "$" else null,
            healthState = UsageMetricHealthState.Normal,
        )
        val metrics = listOf(
            metric("five-hour", "5 小时", used = 2.45, limit = 14.0, remaining = 11.55),
            metric("weekly", "周", used = 2.45, limit = 35.0, remaining = 32.55),
            metric("credit-progress", "月", used = 24.11, limit = 70.0, remaining = 45.89),
            metric("purchased-remaining", "购买剩余", value = 0.0),
            metric("free-remaining", "赠送剩余", value = 0.0),
            metric("request-count", "累计请求", value = 9700.0, unit = UsageMetricUnit.Request),
        )
        val commandCodeCard = CredentialCardUi(
            credential = command,
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = command.id,
                fetchedAt = now,
                metrics = metrics,
                planName = "GOAT",
                statusText = "有效",
                usageKind = "periodic",
                allowedModels = listOf("claude-sonnet-5"),
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(commandCodeCard, onOpen = {}, onRetry = {})
            }
        }

        listOf("5 小时", "周", "月", "累计请求").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeRule.onNodeWithText("额度单位：美元（USD）").assertDoesNotExist()
        composeRule.onNodeWithText("已用 \$24.11").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 \$45.89").assertIsDisplayed()
        composeRule.onNodeWithText("购买剩余").assertDoesNotExist()
        composeRule.onNodeWithText("赠送剩余").assertDoesNotExist()
        val requestTextResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        composeRule.onNodeWithText("9.7K 次", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(requestTextResults)
            }
        assertEquals(1, requestTextResults.single().lineCount)
        val requestLabel = composeRule.onNodeWithText("累计请求", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val requestValue = composeRule.onNodeWithText("9.7K 次", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val monthly = composeRule.onNodeWithText("月", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs(requestLabel.top.value - requestValue.top.value) < 1f)
        assertTrue(monthly.left < requestLabel.left)
        assertTrue(requestLabel.left < requestValue.left)
    }

    @Test
    @Config(qualifiers = "w1200dp-h800dp")
    fun credentialCardKeepsNarrowMetricLayoutOnWideScreens() {
        val credential = credential("宽屏主账号", ProviderId.Routin)
        val card = card(
            credential,
            metrics = listOf(progress(12.0, 60.0), balance(38.42)),
        )

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card),
                        groups = listOf(group(ProviderId.Routin, listOf(card))),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    layoutMode = MyTokenLayoutMode.Expanded,
                )
            }
        }

        composeRule.onNodeWithTag("home_grid_3_columns").assertExists()
        val progressLabel = composeRule.onNodeWithText("5 小时", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val balanceLabel = composeRule.onNodeWithText("账户余额", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertTrue(balanceLabel.left.value > progressLabel.left.value)
    }

    @Test
    fun filtersProvidersAndHidesDisabledCredentials() {
        val enabled = credential("启用", ProviderId.Routin)
        val disabled = credential("停用", ProviderId.DeepSeek)
        val enabledDeepSeek = credential("备用", ProviderId.DeepSeek)
        val cards = listOf(
            card(enabled, metrics = listOf(balance(20.0))),
            card(enabledDeepSeek, metrics = listOf(balance(10.0))),
            card(disabled, status = RefreshStatus.Disabled),
        )
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = listOf(
                            group(ProviderId.Routin, listOf(card(enabled, metrics = listOf(balance(20.0))))),
                            group(
                                ProviderId.DeepSeek,
                                listOf(
                                    card(enabledDeepSeek, metrics = listOf(balance(10.0))),
                                    card(disabled, status = RefreshStatus.Disabled),
                                ),
                            ),
                        ),
                        credentialCount = 3,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithText("启用").assertIsDisplayed()
        composeRule.onNodeWithText("停用").assertDoesNotExist()

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_item_routin").performClick()
        composeRule.onNodeWithText("备用").assertDoesNotExist()
        composeRule.onNodeWithText("2 个凭证").assertDoesNotExist()
        composeRule.onNodeWithText("1 个凭证").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_filter_selected", useUnmergedTree = true)
            .assertTextContains("供应商：Routin")
    }

    @Test
    fun pullRefreshIsOnlyAttachedWhenCredentialCardsExist() {
        val credential = credential("首页")
        var state by mutableStateOf(HomeUiState(isLoading = false))
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = state,
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithTag("home_pull_refresh").assertDoesNotExist()

        state = HomeUiState(
            isLoading = false,
            cards = listOf(card(credential, metrics = listOf(balance(20.0)))),
            groups = listOf(group(ProviderId.Routin, listOf(card(credential, metrics = listOf(balance(20.0)))))),
            credentialCount = 1,
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_pull_refresh").assertExists()
    }

    @Test
    fun failedCardShowsErrorStaleDataAndRetry() {
        val credential = credential("失败")
        var refreshed = false
        var retried = false
        val failedCard = card(
            credential,
            status = RefreshStatus.Failed,
            metrics = listOf(balance(20.0)),
            isStale = true,
            error = AppError.Authentication("凭证无效"),
        )
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(failedCard),
                        groups = listOf(
                            group(
                                ProviderId.Routin,
                                listOf(
                                    card(
                                        credential,
                                        status = RefreshStatus.Failed,
                                        metrics = listOf(balance(20.0)),
                                        isStale = true,
                                        error = AppError.Authentication("凭证无效"),
                                    )
                                )
                            )
                        ),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = { refreshed = true },
                    onRetryCredential = { retried = true },
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }
        composeRule.onNodeWithTag(
            "credential_refresh_${credential.id}",
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.OnClick)
        val refreshBounds = composeRule.onNodeWithTag(
            "credential_refresh_${credential.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val failureBounds = composeRule.onNodeWithTag(
            "credential_failure_${credential.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(failureBounds.left < refreshBounds.left)
        // 失败徽章改挂头像右上角后，头像(30dp)与按钮(30dp)顶对齐，容差放宽到 6dp。
        assertTrue(kotlin.math.abs(failureBounds.top.value - refreshBounds.top.value) < 6f)
        assertTrue(refreshBounds.right <= composeRule.onRoot().getUnclippedBoundsInRoot().right)
        assertEquals(true, refreshed)
        composeRule.onNodeWithTag(
            "credential_failure_${credential.id}",
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("刷新失败").assertIsDisplayed()
        composeRule.onNodeWithText("凭证无效").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun loadingCardKeepsRefreshAffordanceVisible() {
        val credential = credential("加载中")
        val loadingCard = card(
            credential,
            status = RefreshStatus.Loading,
            metrics = emptyList(),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(loadingCard, onOpen = {}, onRetry = {})
            }
        }

        composeRule.onNodeWithTag(
            "credential_refresh_${credential.id}",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            "刷新 加载中",
            useUnmergedTree = true,
        ).assertExists()
    }
}
