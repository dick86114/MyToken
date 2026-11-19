package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageResetCard
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
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
import kotlin.math.abs
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

    private fun token(id: String, label: String, value: Double) = UsageMetric(
        id = id,
        label = label,
        value = BigDecimal.valueOf(value),
        unit = UsageMetricUnit.Token,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.UsedQuota,
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

    private fun glmProgressMetric(
        id: String,
        label: String,
        resetCards: Int,
    ) = UsageMetric(
        id = id,
        label = label,
        used = BigDecimal.valueOf(42),
        limit = BigDecimal.valueOf(100),
        remaining = BigDecimal.valueOf(58),
        unit = UsageMetricUnit.Token,
        windowEnd = now.plusSeconds(3_600),
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        healthState = UsageMetricHealthState.Normal,
        resetCards = (1..resetCards).map {
            UsageResetCard(Instant.parse("2026-11-07T09:42:45Z"))
        },
    )

    @Test
    fun newApiTokenColumnsAreCenteredInTheirCells() {
        composeRule.setContent {
            MaterialTheme {
                NewAPIMetrics(
                    metrics = listOf(
                        token("today-token", "今日 Token", 14_873_680.0),
                        token("one-day-token", "近 24 小时 Token", 15_123_456.0),
                        token("seven-day-token", "近 7 天 Token", 139_735_349.0),
                        token("thirty-day-token", "近 30 天 Token", 1_100_848_403.0),
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        fun centerRatio(label: String): Float {
            val bounds = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            return (bounds.left + bounds.right) / 2f / (rootBounds.right - rootBounds.left)
        }

        assertEquals(0.25f, centerRatio("今日 Token"), 0.015f)
        assertEquals(0.25f, centerRatio("14,873,680"), 0.015f)
        assertEquals(0.75f, centerRatio("近 24 小时 Token"), 0.015f)
        assertEquals(0.75f, centerRatio("15,123,456"), 0.015f)
        assertEquals(0.25f, centerRatio("近 7 天 Token"), 0.015f)
        assertEquals(0.75f, centerRatio("近 30 天 Token"), 0.015f)
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun compactGlmCardShowsResetCardCountsAndOpensDetailList() {
        val credential = credential("GLM 重置卡", ProviderId.Glm)
        val testCard = card(
            credential,
            metrics = listOf(
                glmProgressMetric("five-hour", "5 小时用量", resetCards = 2),
                glmProgressMetric("weekly", "每周用量", resetCards = 1),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithContentDescription("2 张重置卡").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 张重置卡").assertIsDisplayed()
        composeRule.onNodeWithText("5 小时").assertIsDisplayed()
        composeRule.onNodeWithText("周").assertIsDisplayed()
        composeRule.onNodeWithText("5 小时用量").assertDoesNotExist()
        composeRule.onNodeWithText("每周用量").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("2 张重置卡").performClick()
        composeRule.onNodeWithText("重置卡到期时间").assertIsDisplayed()
        composeRule
            .onAllNodesWithText(
                formatResetCardExpiry(Instant.parse("2026-11-07T09:42:45Z")),
                substring = true,
                useUnmergedTree = true,
            )
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullGlmCardShowsResetCardCountBelowRemainingTime() {
        val credential = credential("GLM 完整模式", ProviderId.Glm)
        val testCard = card(
            credential,
            metrics = listOf(
                glmProgressMetric("five-hour", "5 小时用量", resetCards = 2),
                glmProgressMetric("weekly", "每周用量", resetCards = 1),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(testCard, onOpen = {}, onRetry = {})
            }
        }

        composeRule.onAllNodesWithText("2 张重置卡").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("1 张重置卡").onFirst().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun compactXiaomiBalanceShowsTopUpAndGiftBreakdown() {
        val xiaomi = credential("小米 API 按量", ProviderId.Xiaomi)
        val testCard = card(
            xiaomi,
            metrics = listOf(
                balance(12.5).copy(id = "account-balance", label = "账户余额"),
                balance(10.0).copy(id = "cash-balance", label = "现金余额"),
                balance(2.5).copy(id = "gift-balance", label = "赠送余额"),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("¥12.50").assertIsDisplayed()
        composeRule.onNodeWithText("充值余额").assertIsDisplayed()
        composeRule.onNodeWithText("¥10.00").assertIsDisplayed()
        composeRule.onNodeWithText("赠送余额").assertIsDisplayed()
        composeRule.onNodeWithText("¥2.50").assertIsDisplayed()
        val totalBounds = composeRule.onNodeWithText("¥12.50", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val giftBounds = composeRule.onNodeWithText("赠送余额", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "total=$totalBounds gift=$giftBounds",
            giftBounds.left.value > totalBounds.right.value,
        )
    }

    @Test
    fun compactStaleCardUsesPendingRefreshLabel() {
        val item = credential("待刷新卡片")
        val staleCard = card(
            item,
            metrics = listOf(progress(12.0, 100.0)),
            isStale = true,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    staleCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("待刷新").assertIsDisplayed()
        composeRule.onNodeWithText("数据过期").assertDoesNotExist()
    }

    @Test
    fun compactThreeRingsStayInOneRow() {
        val item = credential("三周期", ProviderId.OpenCode)
        val threeMetrics = listOf(
            progress(10.0, 100.0).copy(id = "fiveHour", label = "5 小时"),
            progress(20.0, 100.0).copy(id = "weekly", label = "周"),
            progress(30.0, 100.0).copy(id = "monthly", label = "月"),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(item, metrics = threeMetrics),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        val first = composeRule.onNodeWithTag(
            "compact_ring_tile_fiveHour",
            useUnmergedTree = true,
        )
            .getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithTag(
            "compact_ring_tile_weekly",
            useUnmergedTree = true,
        )
            .getUnclippedBoundsInRoot()
        val third = composeRule.onNodeWithTag(
            "compact_ring_tile_monthly",
            useUnmergedTree = true,
        )
            .getUnclippedBoundsInRoot()
        assertEquals(first.top.value, second.top.value, 0.5f)
        assertEquals(second.top.value, third.top.value, 0.5f)
        assertTrue(first.left.value < second.left.value)
        assertTrue(second.left.value < third.left.value)
    }

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
        composeRule.onAllNodesWithText("已用", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("剩余", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun highUsageCardShowsResetTimeInsteadOfEmotionalCopy() {
        val resetAt = now.plusSeconds(3_600)
        fun urgentMetric(id: String, label: String) = UsageMetric(
            id = id,
            label = label,
            used = BigDecimal.valueOf(95),
            limit = BigDecimal.valueOf(100),
            remaining = BigDecimal.valueOf(5),
            unit = UsageMetricUnit.Currency,
            windowEnd = resetAt,
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            currencyCode = "USD",
            healthState = UsageMetricHealthState.Normal,
        )
        val credential = credential("高用量")
        val testCard = card(
            credential,
            metrics = listOf(
                urgentMetric("five-hour", "5 小时"),
                urgentMetric("weekly", "周"),
                urgentMetric("monthly", "月"),
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("即将耗尽", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("用量偏高", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onAllNodesWithText(formatResetTime(resetAt), substring = true, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun highUsageCardHeaderShowsSubscriptionExpiry() {
        val resetAt = now.plusSeconds(3_600)
        val credential = credential("高用量")
        val testCard = card(
            credential,
            metrics = listOf(progress(used = 95.0, limit = 100.0).copy(id = "five-hour", windowEnd = resetAt)),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("到期 ${formatSubscriptionTime(now.plusSeconds(86_400))}")
            .assertIsDisplayed()
        val alias = composeRule.onNodeWithText("高用量", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val expiry = composeRule.onNodeWithText(
            "到期 ${formatSubscriptionTime(now.plusSeconds(86_400))}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(expiry.top.value >= alias.bottom.value - 1f)
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullDeepSeekCardShowsAllAccountFieldsIncludingStatusAndTopUp() {
        val credential = credential("DeepSeek 余额", ProviderId.DeepSeek)
        val total = UsageMetric(
            id = "balance",
            label = "余额",
            value = BigDecimal("38.40"),
            unit = UsageMetricUnit.Currency,
            presentation = UsageMetricPresentation.Balance,
            semantic = UsageMetricSemantic.Balance,
            currencyCode = "CNY",
            healthState = UsageMetricHealthState.Normal,
        )
        val gift = total.copy(
            id = "grantedBalance",
            label = "赠金余额",
            value = BigDecimal("5.00"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val toppedUp = total.copy(
            id = "toppedUpBalance",
            label = "充值余额",
            value = BigDecimal("33.40"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val availability = total.copy(
            id = "availability",
            label = "账户状态",
            value = BigDecimal.ONE,
            unit = UsageMetricUnit.Boolean_,
            presentation = UsageMetricPresentation.Status,
            semantic = UsageMetricSemantic.Status,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(total, gift, toppedUp, availability),
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        // 完整模式需要把接口返回的字段全部展示：赠金、充值余额、账户状态缺一不可。
        composeRule.onNodeWithText("赠金").assertIsDisplayed()
        composeRule.onNodeWithText("¥5.00").assertIsDisplayed()
        composeRule.onNodeWithText("充值").assertIsDisplayed()
        composeRule.onNodeWithText("¥33.40").assertIsDisplayed()
        composeRule.onNodeWithText("账户状态").assertIsDisplayed()
        composeRule.onNodeWithText("可用").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun balanceStripShowsResetTimeInsteadOfHealthCopy() {
        val resetAt = now.plusSeconds(86_400)
        val credential = credential("余额账户", ProviderId.DeepSeek)
        val balanceMetric = UsageMetric(
            id = "balance",
            label = "余额",
            value = BigDecimal.valueOf(38.42),
            unit = UsageMetricUnit.Currency,
            windowEnd = resetAt,
            presentation = UsageMetricPresentation.Balance,
            semantic = UsageMetricSemantic.Balance,
            currencyCode = "CNY",
            healthState = UsageMetricHealthState.Normal,
        )
        val testCard = card(credential, metrics = listOf(balanceMetric))
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("余额账户").assertExists()
        composeRule.onNodeWithText("充足", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("偏低", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("不足", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onAllNodesWithText("重置", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertExists()
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

        listOf("5 小时窗口", "本周用量", "本月用量", "累计请求").forEach {
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
        composeRule.onNodeWithText("累计请求", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("9.7K 次", useUnmergedTree = true).assertIsDisplayed()
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
        val progressLabel = composeRule.onNodeWithText("5 小时窗口", useUnmergedTree = true)
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

        composeRule.onNodeWithTag("provider_filter_item_routin").performClick()
        composeRule.onNodeWithText("备用").assertDoesNotExist()
        composeRule.onNodeWithTag("provider_filter_item_routin", useUnmergedTree = true)
            .assertIsSelected()
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
    fun 点击账户健康短条会定位到对应凭证卡片() {
        val cards = (1..5).map { index ->
            val item = credential("健康定位 $index")
            card(item, metrics = listOf(progress(index * 10.0, 100.0)))
        }
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("health_segment_4").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${cards[4].credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("credential_card_${cards[4].credential.id}")
            .assertIsDisplayed()
    }

    @Test
    fun 点击账户健康短条会把中间凭证卡片定位到屏幕中心() {
        val cards = (1..5).map { index ->
            val item = credential("健康居中 $index")
            card(item, metrics = listOf(progress(index * 10.0, 100.0)))
        }
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithTag("health_segment_2").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${cards[2].credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitForIdle()

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val card = composeRule
            .onNodeWithTag("credential_card_${cards[2].credential.id}")
            .getUnclippedBoundsInRoot()
        val rootCenter = (root.top.value + root.bottom.value) / 2f
        val cardCenter = (card.top.value + card.bottom.value) / 2f
        assertTrue("卡片中心应贴近屏幕中心，实际偏差 ${abs(cardCenter - rootCenter)}dp", abs(cardCenter - rootCenter) < 40f)
    }

    @Test
    fun 简洁完整分段控件暴露当前模式并可切换() {
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        var toggleCount = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("模式切换"))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        toggleCount += 1
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("usage_density_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("usage_density_option_compact").assertIsSelected()
        composeRule.onNodeWithTag("usage_density_option_full").performClick()
        composeRule.onNodeWithTag("usage_density_option_full").assertIsSelected()
        assertEquals(1, toggleCount)
    }

    @Test
    fun 有可用更新时设置入口显示提示点() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("更新提示"))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    hasUpdateAvailable = true,
                )
            }
        }

        composeRule.onNodeWithTag("home_update_badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("打开设置，有可用更新").assertIsDisplayed()
    }

    @Test
    fun 有可用更新时首页顶部显示更新提醒条() {
        var openedSettings = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("更新横幅"))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    onOpenSettings = { openedSettings++ },
                    hasUpdateAvailable = true,
                    updateVersion = "6.1.0",
                )
            }
        }

        composeRule.onNodeWithTag("home_update_banner").assertIsDisplayed()
        composeRule.onNodeWithText("发现新版本 6.1.0").assertIsDisplayed()
        composeRule.onNodeWithTag("home_update_banner").performClick()
        assertEquals(1, openedSettings)
    }

    @Test
    fun 无可用更新时不显示更新提醒条() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("无更新"))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    hasUpdateAvailable = false,
                )
            }
        }

        composeRule.onNodeWithTag("home_update_banner").assertDoesNotExist()
    }

    @Test
    fun 账户健康说明使用分组排版() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("健康说明"))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithContentDescription("查看账户健康 说明").performClick()
        composeRule.onNodeWithTag("my_token_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("颜色").assertIsDisplayed()
        composeRule.onNodeWithText("判定阈值").assertIsDisplayed()
        composeRule.onNodeWithText("80% 标红 · 50% 标橙").assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onNodeWithTag("my_token_dialog").assertDoesNotExist()
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
        // 头像承载在 44dp 触摸区内、按钮 30dp，中心对齐后顶部差约 7dp。
        assertTrue(kotlin.math.abs(failureBounds.top.value - refreshBounds.top.value) < 10f)
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
    fun credentialAvatarUsesProviderLogoImage() {
        val credential = credential("主账户", ProviderId.Glm)
        val usageCard = card(
            credential,
            metrics = listOf(progress(14.0, 100.0)),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(usageCard, onOpen = {}, onRetry = {})
            }
        }

        composeRule.onNodeWithTag(
            "credential_avatar_${credential.id}",
            useUnmergedTree = true,
        ).assertExists()
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
