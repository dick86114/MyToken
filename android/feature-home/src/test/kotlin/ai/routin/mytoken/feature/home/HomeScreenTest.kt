package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenTheme
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsNotSelected
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** 两页都会提前组合；测试只取当前页的节点，避免隐藏页重复命中。 */
    private fun activeNode(tag: String) = composeRule
        .onAllNodesWithTag(tag, useUnmergedTree = true)
        .filterToOne(hasAnyAncestor(hasTestTag("home_page_active")))

    private fun activeText(text: String) = composeRule
        .onAllNodesWithText(text, useUnmergedTree = true)
        .filterToOne(hasAnyAncestor(hasTestTag("home_page_active")))

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

    private fun balanceMetric(
        id: String,
        value: Double,
        currency: String,
        topped: Double,
        granted: Double,
    ) = UsageMetric(
        id = id,
        label = "余额",
        value = BigDecimal.valueOf(value),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Balance,
        semantic = UsageMetricSemantic.Balance,
        currencyCode = currency,
        healthState = UsageMetricHealthState.Normal,
    ).let { balance ->
        val extras = buildList {
            if (topped > 0) add(balance.copy(id = "toppedUpBalance-${currency}", label = "充值余额", value = BigDecimal.valueOf(topped)))
            if (granted > 0) add(balance.copy(id = "grantedBalance-${currency}", label = "赠金余额", value = BigDecimal.valueOf(granted)))
        }
        listOf(balance) + extras
    }

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
        allowedModels: List<String> = emptyList(),
        statusText: String? = null,
        usageKind: String? = null,
        billingMode: String? = null,
        planName: String = "成长版",
        subscriptionEndAt: Instant? = now.plusSeconds(86400),
    ) = CredentialCardUi(
        credential = credential,
        status = status,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = now,
            metrics = metrics,
            planName = planName,
            subscriptionStartAt = now.minusSeconds(86400),
            subscriptionEndAt = subscriptionEndAt,
            statusText = statusText,
            usageKind = usageKind,
            billingMode = billingMode,
            allowedModels = allowedModels,
        ),
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
        composeRule.onNodeWithContentDescription("1 张重置卡").assertDoesNotExist()
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
    fun fullGlmCardShowsOnlyPrimaryResetCardChip() {
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

        composeRule.onNodeWithContentDescription("2 张重置卡").assertExists()
        composeRule.onNodeWithContentDescription("1 张重置卡").assertDoesNotExist()
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
        composeRule.onNodeWithText("含赠金 ¥2.50", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("现金 ¥10.00").assertDoesNotExist()
        composeRule.onNodeWithText("赠送 ¥2.50").assertDoesNotExist()
        composeRule.onNodeWithText("余额构成").assertDoesNotExist()
        composeRule.onNodeWithText("可用模型").assertDoesNotExist()
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

        composeRule.onNodeWithTag("card_footer_left", useUnmergedTree = true)
            .assertTextEquals("${formatResetTime(now)} 更新")
        composeRule.onNodeWithText("待刷新").assertDoesNotExist()
        composeRule.onNodeWithText("数据过期").assertDoesNotExist()
    }

    @Test
    fun compactThreeWindowsUsesOneRingAndMiniRows() {
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

        composeRule.onAllNodesWithTag("compact_ring_primary", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule.onNodeWithTag("compact_window_row_weekly", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("compact_window_row_monthly", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("compact_ring_tile_weekly", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h900dp")
    fun compactValueGridUsesSpacingNotDividers() {
        val credential = credential("New API 统计", ProviderId.NewAPI)
        val metrics = listOf(
            token("today-token", "今日 Token", 14_873_680.0),
            token("one-day-token", "近 24 小时 Token", 15_123_456.0),
            token("seven-day-token", "近 7 天 Token", 139_735_349.0),
            token("thirty-day-token", "近 30 天 Token", 1_100_848_403.0),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(credential, metrics = metrics),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        listOf("今日 Token", "近 24 小时 Token", "近 7 天 Token", "近 30 天 Token")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("14.87").assertIsDisplayed()
        composeRule.onNodeWithText("15.12").assertIsDisplayed()
        composeRule.onNodeWithText("139.74").assertIsDisplayed()
        composeRule.onNodeWithText("1.1").assertIsDisplayed()

        val first = composeRule.onNodeWithText("今日 Token", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithText("近 24 小时 Token", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val third = composeRule.onNodeWithText("近 7 天 Token", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(first.top.value, second.top.value, 0.5f)
        assertTrue(second.left.value > first.left.value)
        assertTrue(third.top.value > first.top.value)

        val source = java.io.File(System.getProperty("user.dir"))
            .walkTopDown()
            .first { it.name == "CredentialUsageCard.kt" }
            .readText()
        assertFalse("卡片内部不应再有分割线", source.contains("HorizontalDivider"))
    }

    /**
     * Robolectric 的文本度量不随 fontScale 变化（每个字形固定约 1px），因此不能用
     * hasVisualOverflow / 文本节点宽度判断裁切。这里改为断言真正受 fontScale 影响的几何：
     * 弧线直径随字号放大、身份行的名称列仍占据卡片大部分宽度、圆心数字落在弧线范围内。
     */
    @Test
    @Config(qualifiers = "w390dp-h1100dp")
    fun longNameAtLargeFontScaleKeepsGaugeAndNameColumn() {
        val longName = "非常长的凭证名称用于验证大字体下不会被裁切或溢出"
        val item = credential(longName)
        val fontScale = 1.3f
        composeRule.setContent {
            MaterialTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        androidx.compose.ui.platform.LocalDensity.current.density,
                        fontScale,
                    ),
                ) {
                    CredentialUsageCard(
                        card(item, metrics = listOf(progress(42.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                    )
                }
            }
        }

        composeRule.onNodeWithText(longName, substring = true, useUnmergedTree = true).assertExists()

        val cardBounds = composeRule.onNodeWithTag(
            "credential_card_${item.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val contentWidth = cardBounds.right - cardBounds.left - 28.dp
        val gaugeBounds = composeRule.onNodeWithTag(
            "compact_ring_primary",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val gaugeWidth = gaugeBounds.right - gaugeBounds.left
        val nameBounds = composeRule.onNodeWithTag(
            "credential_identity_${item.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val nameWidth = nameBounds.right - nameBounds.left

        assertTrue(
            "弧线直径应随字号放大（实际 $gaugeWidth），否则圆心数字会被 52dp 裁切",
            gaugeWidth >= (52f * fontScale - 1f).dp,
        )
        assertTrue("弧线不应溢出卡片内容宽度", gaugeWidth <= contentWidth)
        assertTrue(
            "长名称下名称列仍应占卡片内容宽度过半（实际 $nameWidth / $contentWidth）",
            nameWidth > contentWidth / 2,
        )

        listOf("42", "%").forEach { text ->
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .forEach { node ->
                    val nodeBounds = node.boundsInRoot
                    assertTrue(
                        "数字 $text 应落在弧线范围内",
                        nodeBounds.left >= gaugeBounds.left.value && nodeBounds.right <= gaugeBounds.right.value,
                    )
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult]
                        ?.action?.invoke(layouts)
                    assertEquals(
                        "数字 $text 应按放大后的字号排版",
                        fontScale,
                        layouts.single().layoutInput.density.fontScale,
                        0.001f,
                    )
                }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun loadingCardWithoutDataShowsStaticSkeleton() {
        val item = credential("空数据凭证")
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(item, status = RefreshStatus.Loading, metrics = emptyList()),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithTag("card_skeleton", useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithTag("compact_ring_primary", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("刷新中", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("暂无用量数据", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun refreshingCardWithDataKeepsContent() {
        val item = credential("有数据凭证")
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(item, status = RefreshStatus.Loading, metrics = listOf(progress(42.0, 100.0))),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onAllNodesWithTag("card_skeleton", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag("compact_ring_primary", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("card_footer_left", useUnmergedTree = true)
            .assertTextEquals("${formatResetTime(now)} 更新")
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun multiCurrencyCompositionHandlesZeroSegments() {
        val item = credential("多币种账户", provider = ProviderId.DeepSeek)
        val balances = listOf(
            balanceMetric(id = "balance-CNY", value = 120.0, currency = "CNY", topped = 80.0, granted = 40.0),
            balanceMetric(id = "balance-USD", value = 50.0, currency = "USD", topped = 50.0, granted = 0.0),
            balanceMetric(id = "balance-EUR", value = 30.0, currency = "EUR", topped = 0.0, granted = 30.0),
        ).flatten()
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(item, metrics = balances),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        // 三个币种的总额度数字都要出现（¥120.00 / $50.00 / €30.00）
        listOf("¥120.00", "$50.00", "€30.00").forEach { amount ->
            assertTrue(
                "缺少币种金额 $amount",
                composeRule.onAllNodesWithText(amount, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
        // 完整余额卡不再平铺账户事实。
        composeRule.onNodeWithText("可用模型", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun touchTargetsAreAtLeast48() {
        val credential = credential("主账号")
        val item = card(credential, metrics = listOf(progress(42.0, 100.0)))
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(item),
                        groups = listOf(group(ProviderId.Routin, listOf(item))),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    onOpenCredentials = {},
                    onOpenSettings = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        // 顶部导航、供应商筛选与报告刷新都保持 48dp 触控。
        mapOf(
            "credential_refresh_${credential.id}" to 48.dp,
            "provider_filter_menu" to 48.dp,
            "report_refresh" to 48.dp,
            "home_open_credentials" to 48.dp,
            "home_open_settings" to 48.dp,
            "usage_density_option_compact" to 48.dp,
            "usage_density_option_full" to 48.dp,
        ).forEach { (tag, minSize) ->
            val bounds = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val height = bounds.bottom - bounds.top
            val width = bounds.right - bounds.left
            assertTrue("触控目标 $tag 高度应 ≥$minSize（实际 $height）", height >= minSize)
            assertTrue("触控目标 $tag 宽度应 ≥48dp（实际 $width）", width >= 48.dp)
        }
    }

    @Test
    fun cardExposesTalkBackSummary() {
        val credential = credential("主账号")
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = card(credential, metrics = listOf(progress(42.0, 100.0))),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithContentDescription("主账号，正常，42%", substring = true)
            .assertExists()
    }

    @Test
    fun 简洁卡正常状态不显示圆点只有报警才显示() {
        val normal = credential("简洁正常")
        val warning = credential("简洁报警")
        composeRule.setContent {
            MyTokenTheme {
                Column {
                    CredentialUsageCard(
                        card = card(normal, metrics = listOf(progress(20.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                    )
                    CredentialUsageCard(
                        card = card(warning, metrics = listOf(progress(90.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                    )
                }
            }
        }

        composeRule.onAllNodesWithTag("credential_status_dot_${normal.id}", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("credential_status_dot_${warning.id}", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun 完整卡正常状态不显示圆点只有报警才显示() {
        val normal = credential("完整正常")
        val warning = credential("完整报警")
        composeRule.setContent {
            MyTokenTheme {
                Column {
                    CredentialUsageCard(
                        card = card(normal, metrics = listOf(progress(20.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.FULL,
                    )
                    CredentialUsageCard(
                        card = card(warning, metrics = listOf(progress(90.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.FULL,
                    )
                }
            }
        }

        composeRule.onAllNodesWithTag("credential_status_dot_${normal.id}", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("credential_status_dot_${warning.id}", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun filterMenuKeepsAllCountAfterFiltering() {
        val routinCard = card(credential("启用", ProviderId.Routin), metrics = listOf(balance(20.0)))
        val deepSeekCard = card(credential("备用", ProviderId.DeepSeek), metrics = listOf(balance(10.0)))
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(routinCard, deepSeekCard),
                        groups = listOf(
                            group(ProviderId.Routin, listOf(routinCard)),
                            group(ProviderId.DeepSeek, listOf(deepSeekCard)),
                        ),
                        credentialCount = 2,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_sheet", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_filter_item_routin")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // 过滤后“全部”仍应代表所有启用凭证，而不是当前视图。
        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithText("全部供应商", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("2 个凭证", useUnmergedTree = true).assertExists()
    }

    @Test
    fun resetCardChipMatchesDesignGeometry() {
        val credential = credential("重置卡账户")
        val metric = progress(42.0, 100.0).copy(
            resetCards = listOf(
                ai.routin.mytoken.domain.model.UsageResetCard(java.time.Instant.parse("2026-09-30T00:00:00Z")),
            ),
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = card(credential, metrics = listOf(metric)),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        val chip = composeRule.onNodeWithTag("reset_chip", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        // 设计稿简洁 chip：17dp 高、数字居中，紧贴窗口标签。
        assertEquals("chip 视觉高度应为 17dp", 17f, (chip.bottom - chip.top).value, 1f)
        val label = composeRule.onNodeWithText("5 小时", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val gap = chip.left.value - label.right.value
        assertTrue("chip 应紧贴标签，实际间隙 ${gap}dp", gap in 0f..8f)
        val touch = composeRule.onNodeWithContentDescription("1 张重置卡", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "chip 可点击高度应 ≥24dp（实际 ${touch.bottom - touch.top}）",
            touch.bottom - touch.top >= 24.dp,
        )
    }

    @Test
    fun fullPrimaryResetChipSitsNextToItsLabel() {
        val credential = credential("完整重置卡", ProviderId.Glm)
        val testCard = card(
            credential,
            metrics = listOf(
                glmProgressMetric("five-hour", "5 小时用量", resetCards = 2),
                glmProgressMetric("weekly", "每周用量", resetCards = 1),
            ),
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        val chips = composeRule.onAllNodesWithTag("reset_chip", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertEquals("完整卡只保留主指标重置 chip", 1, chips.size)
        val primaryLabel = composeRule.onNodeWithText("5 小时用量", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "主指标 chip 应在标签右侧同一行",
            chips[0].left >= primaryLabel.right.value - 1f &&
                chips[0].top < primaryLabel.bottom.value,
        )
        composeRule.onNodeWithContentDescription("1 张重置卡").assertDoesNotExist()
    }

    @Test
    fun commandCodeCompactCardOmitsBalanceRow() {
        val credential = credential("Command 简洁", ProviderId.CommandCode)
        val monthlyBalance = UsageMetric(
            id = "monthly-remaining",
            label = "月度余额",
            value = BigDecimal("12.30"),
            unit = UsageMetricUnit.Currency,
            presentation = UsageMetricPresentation.Balance,
            semantic = UsageMetricSemantic.Balance,
            currencyCode = "USD",
            healthState = UsageMetricHealthState.Normal,
        )
        val testCard = card(
            credential,
            metrics = listOf(
                progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
                progress(30.0, 100.0).copy(id = "credit-progress", label = "月额度"),
                monthlyBalance,
            ),
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithTag("compact_window_row_credit-progress", useUnmergedTree = true)
            .assertIsDisplayed()
        assertTrue(
            "CommandCode 简洁卡不应再显示余额行",
            composeRule.onAllNodesWithText("余额", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun compactBalanceOmitsTitleCompositionAndModelsButShowsGift() {
        val credential = credential("DeepSeek 简洁余额", ProviderId.DeepSeek)
        val total = balance(38.40).copy(id = "balance")
        val toppedUp = total.copy(
            id = "toppedUpBalance",
            label = "充值余额",
            value = BigDecimal("33.40"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val granted = total.copy(
            id = "grantedBalance",
            label = "赠金余额",
            value = BigDecimal("5.00"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = card(
                        credential,
                        metrics = listOf(total, toppedUp, granted),
                        planName = "API 余额",
                        allowedModels = listOf("deepseek-v4-flash", "deepseek-v4-pro"),
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithText("¥38.40").assertIsDisplayed()
        composeRule.onNodeWithText("含赠金 ¥5.00", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("账户余额 · CNY").assertDoesNotExist()
        composeRule.onNodeWithText("余额构成").assertDoesNotExist()
        composeRule.onNodeWithText("可用模型").assertDoesNotExist()
        composeRule.onAllNodesWithTag("compact_balance_legend", useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("card_footer_left", useUnmergedTree = true)
            .assertTextEquals("${formatResetTime(now)} 更新")
    }

    @Test
    fun compactPlanCardOmitsRemainingRowAndShowsUpdateTime() {
        val credential = credential("套餐简洁")
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = card(
                        credential,
                        metrics = listOf(progress(20.0, 100.0)),
                        usageKind = "plan",
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onAllNodesWithText("剩余", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag("card_footer_left", useUnmergedTree = true)
            .assertTextEquals("${formatResetTime(now)} 更新")
    }

    @Test
    fun fullBalanceCardShowsCompositionLegendWithoutFacts() {
        val credential = credential("DeepSeek 完整余额", ProviderId.DeepSeek)
        val total = balance(38.40).copy(id = "balance")
        val toppedUp = total.copy(
            id = "toppedUpBalance",
            label = "充值余额",
            value = BigDecimal("33.40"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val granted = total.copy(
            id = "grantedBalance",
            label = "赠金余额",
            value = BigDecimal("5.00"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = card(
                        credential,
                        metrics = listOf(total, toppedUp, granted),
                        planName = "API 余额",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("余额构成").assertIsDisplayed()
        composeRule.onNodeWithTag("full_balance_legend_row", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("full_balance_facts", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun providerLogoKeepsDesignSize() {
        val compactCredential = credential("Logo 简洁")
        val fullCredential = credential("Logo 完整")
        composeRule.setContent {
            MyTokenTheme {
                Column {
                    CredentialUsageCard(
                        card = card(compactCredential, metrics = listOf(progress(42.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                    )
                    CredentialUsageCard(
                        card = card(fullCredential, metrics = listOf(progress(42.0, 100.0))),
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.FULL,
                    )
                }
            }
        }

        fun avatarHeight(id: UUID): Float = composeRule
            .onNodeWithTag("credential_avatar_$id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .let { it.bottom.value - it.top.value }

        assertEquals("简洁卡 logo 按设计稿 24dp", 24f, avatarHeight(compactCredential.id), 1f)
        assertEquals("完整卡 logo 按设计稿 28dp", 28f, avatarHeight(fullCredential.id), 1f)
    }

    @Test
    fun 报告与工具条之后紧跟卡片区域() {
        val credential = credential("间距")
        val usage = card(credential, metrics = listOf(progress(42.0, 100.0)))
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(usage),
                        groups = listOf(group(ProviderId.Routin, listOf(usage))),
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

        val reportBottom = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
            .bottom
        val toolbarTop = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
            .top
        assertEquals("报告与供应商工具条之间不留空隙", reportBottom.value, toolbarTop.value, 1f)

        val toolbarBottom = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        val cardTop = activeNode("credential_card_${credential.id}")
            .getUnclippedBoundsInRoot()
            .top
        val gap = cardTop.value - toolbarBottom.value
        assertTrue("工具条与首张卡片间距应保持在 8dp 内，实际 ${gap}dp", gap <= 8f)
    }

    @Test
    fun compactCardsAreNotPinnedToFixedHeight() {
        val credential = credential("单指标账户", ProviderId.Glm)
        val usage = card(
            credential,
            metrics = listOf(progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时")),
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialUsageCard(
                    card = usage,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        val bounds = composeRule.onNodeWithTag(
            "credential_card_${credential.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        // 高度由内容决定：单行内容应明显低于旧的 288dp 固定高度。
        assertTrue("简洁卡高度应随内容收缩，实际 $height", height < 280.dp)
    }

    @Test
    fun locateScrollUsesMotionPolicy() {
        fun read(name: String) = java.io.File(
            System.getProperty("user.dir") ?: "",
        ).resolve("src/main/kotlin/ai/routin/mytoken/feature/home/$name")
            .let {
                if (it.isFile) it
                else java.io.File("android/feature-home").resolve("src/main/kotlin/ai/routin/mytoken/feature/home/$name")
            }
            .readText()

        assertTrue(read("HomeScreen.kt").contains("MyTokenMotion.locateDurationMillis"))
        assertTrue(!read("HomeScrollStates.kt").contains("tween(240)"))
    }

    @Test
    fun compactCardsShareTopRhythmAndAdaptHeight() {
        val glm = card(
            credential("GLM 节奏", ProviderId.Glm),
            metrics = listOf(
                progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
            ),
        )
        val command = card(
            credential("Command 节奏", ProviderId.CommandCode),
            metrics = listOf(
                progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
                progress(30.0, 100.0).copy(id = "credit-progress", label = "月额度"),
            ),
        )
        composeRule.setContent {
            MyTokenTheme {
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CredentialUsageCard(
                        card = glm,
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                    )
                    CredentialUsageCard(
                        card = command,
                        onOpen = {},
                        onRetry = {},
                        usageCardDensity = UsageCardDensity.COMPACT,
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                    )
                }
            }
        }

        val cardA = composeRule.onNodeWithTag(
            "credential_card_${glm.credential.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val cardB = composeRule.onNodeWithTag(
            "credential_card_${command.credential.id}",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertEquals("卡片顶边应一致", cardA.top.value, cardB.top.value, 1f)

        val rings = composeRule.onAllNodesWithTag("compact_ring_primary", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertEquals("弧线数量", 2, rings.size)
        assertEquals(
            "弧线中心应在同一水平线",
            rings[0].center.y,
            rings[1].center.y,
            1f,
        )

        assertTrue(
            "卡片高度应随内容自适应：多一行窗口的卡更高",
            cardB.bottom - cardB.top > cardA.bottom - cardA.top,
        )

        val footers = composeRule.onAllNodesWithTag("card_footer_left", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertEquals("页脚数量", 2, footers.size)
        assertTrue("页脚应跟随内容，而不是被固定高度撑开", footers[0].top < footers[1].top)
    }

    @Test
    fun 简洁模式卡片按瀑布流排列() {
        val short = card(
            credential("瀑布短卡", ProviderId.Glm),
            metrics = listOf(
                progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
            ),
        )
        val tall = card(
            credential("瀑布长卡", ProviderId.CommandCode),
            metrics = listOf(
                progress(42.0, 100.0).copy(id = "five-hour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
                progress(30.0, 100.0).copy(id = "credit-progress", label = "月额度"),
            ),
        )
        val third = card(
            credential("瀑布第三张"),
            metrics = listOf(progress(15.0, 100.0)),
        )
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(short, tall, third),
                        groups = listOf(
                            group(ProviderId.Glm, listOf(short)),
                            group(ProviderId.CommandCode, listOf(tall)),
                        ),
                        credentialCount = 3,
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
        composeRule.waitForIdle()

        fun bounds(id: UUID) = activeNode("credential_card_$id").getUnclippedBoundsInRoot()

        val shortBounds = bounds(short.credential.id)
        val tallBounds = bounds(tall.credential.id)
        val thirdBounds = bounds(third.credential.id)

        assertEquals("两列卡片顶边应对齐", shortBounds.top.value, tallBounds.top.value, 1f)
        assertTrue(
            "第三张应落在较矮的一列下方，而不是另起一行",
            thirdBounds.top < tallBounds.bottom,
        )
        assertTrue(
            "瀑布流不允许卡片重叠",
            thirdBounds.top >= shortBounds.bottom,
        )
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
        activeText("熠").assertIsDisplayed()
        activeText("5 小时额度").assertIsDisplayed()
        activeText("42%").assertIsDisplayed()
    }

    @Test
    fun 首页顶部使用品牌导航与琥珀报告() {
        val cards = (1..7).map { index ->
            card(credential("健康 $index"), metrics = listOf(progress(20.0 + index, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = listOf(group(ProviderId.Routin, cards)),
                        credentialCount = cards.size,
                        lastUpdatedText = "刚刚同步",
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithText("账户用量").assertDoesNotExist()
        composeRule.onNodeWithTag("home_wordmark", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_brand", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("home_health_report").assertIsDisplayed()
        composeRule.onNodeWithTag("home_provider_toolbar").assertIsDisplayed()

        val toolbarBounds = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
        val toolbarHeight = (toolbarBounds.bottom - toolbarBounds.top).value
        assertTrue("顶部导航应为 60dp，实际 $toolbarHeight", toolbarHeight in 58f..64f)

        val reportBounds = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        val reportHeight = (reportBounds.bottom - reportBounds.top).value
        assertTrue("无风险项时报告应保持紧凑，实际 $reportHeight", reportHeight < 260f)

        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val toggleBounds = composeRule.onNodeWithTag("usage_density_toggle")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "密度滑块应位于供应商工具条右侧",
            (toggleBounds.left + toggleBounds.right) / 2f >
                (rootBounds.left + rootBounds.right) / 2f,
        )
        assertEquals("密度滑块宽度按设计稿 104dp", 104f, toggleBounds.right.value - toggleBounds.left.value, 1.5f)
    }

    @Test
    fun 顶部导航固顶且离开首页后保留滚动位置() {
        val cards = (1..18).map { index ->
            card(
                credential("滚动 $index"),
                metrics = listOf(progress(index.toDouble(), 100.0)),
            )
        }
        val homeScrollStates = HomeScrollStates()
        var showHome by mutableStateOf(true)
        composeRule.setContent {
            MyTokenTheme {
                if (showHome) {
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
                        homeScrollStates = homeScrollStates,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_toolbar").assertIsDisplayed()
        val toolbarBefore = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag("home_list_full").performScrollToIndex(8)
        composeRule.waitForIdle()
        val toolbarAfter = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
        assertEquals(toolbarBefore.top.value, toolbarAfter.top.value, 0.5f)

        val scrollIndex = homeScrollStates.full.firstVisibleItemIndex
        assertTrue("首页应已滚动", scrollIndex > 0)
        composeRule.runOnIdle { showHome = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { showHome = true }
        composeRule.waitForIdle()

        assertEquals(scrollIndex, homeScrollStates.full.firstVisibleItemIndex)
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
        composeRule.onAllNodesWithText("后重置", substring = true, useUnmergedTree = true)
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

        // 设计稿简洁卡把到期/重置放进页脚两段式，而不是标题下的 meta。
        composeRule.onNodeWithText("成长版").assertIsDisplayed()
        composeRule.onNodeWithText("后重置", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullDeepSeekCardUsesBalanceHeroWithoutAccountFacts() {
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
                        allowedModels = listOf("deepseek-v4-flash", "deepseek-v4-pro"),
                        planName = "API 余额",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("账户余额 · CNY").assertIsDisplayed()
        val balanceTitleResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        composeRule.onNodeWithText("账户余额 · CNY", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(balanceTitleResults)
            }
        assertTrue(
            "余额标题不应在完整模式中被截断",
            !balanceTitleResults.single().hasVisualOverflow,
        )
        composeRule.onNodeWithText("¥38.40").assertIsDisplayed()
        composeRule.onNodeWithText("账户可用").assertDoesNotExist()
        composeRule.onNodeWithText("充值 ¥33.40").assertIsDisplayed()
        composeRule.onNodeWithText("赠金 ¥5.00").assertIsDisplayed()
        composeRule.onNodeWithText("可用模型").assertDoesNotExist()
        composeRule.onNodeWithText("2 个").assertDoesNotExist()
        composeRule.onNodeWithText("deepseek-v4-flash").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullDeepSeekCardGroupsCurrenciesAndShowsAccountStatusOnce() {
        val credential = credential("DeepSeek 双币种", ProviderId.DeepSeek)
        val cny = balance(110.0).copy(id = "balance-CNY", label = "余额（CNY）")
        val usd = cny.copy(
            id = "balance-USD",
            label = "余额（USD）",
            value = BigDecimal("23.86"),
            currencyCode = "USD",
        )
        val availability = cny.copy(
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
                        metrics = listOf(
                            cny,
                            cny.copy(
                                id = "toppedUpBalance-CNY",
                                label = "充值余额（CNY）",
                                value = BigDecimal("100.00"),
                                presentation = UsageMetricPresentation.Value,
                                semantic = UsageMetricSemantic.Value,
                            ),
                            cny.copy(
                                id = "grantedBalance-CNY",
                                label = "赠金余额（CNY）",
                                value = BigDecimal("10.00"),
                                presentation = UsageMetricPresentation.Value,
                                semantic = UsageMetricSemantic.Value,
                            ),
                            usd,
                            usd.copy(
                                id = "toppedUpBalance-USD",
                                label = "充值余额（USD）",
                                value = BigDecimal("18.86"),
                                presentation = UsageMetricPresentation.Value,
                                semantic = UsageMetricSemantic.Value,
                            ),
                            usd.copy(
                                id = "grantedBalance-USD",
                                label = "赠金余额（USD）",
                                value = BigDecimal("5.00"),
                                presentation = UsageMetricPresentation.Value,
                                semantic = UsageMetricSemantic.Value,
                            ),
                            availability,
                        ),
                        allowedModels = listOf("deepseek-v4-flash", "deepseek-v4-pro"),
                        planName = "API 余额",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("¥110.00").assertIsDisplayed()
        composeRule.onNodeWithText("$23.86").assertIsDisplayed()
        composeRule.onNodeWithText("USD 账户").assertIsDisplayed()
        composeRule.onNodeWithText("充值 $18.86 · 赠金 $5.00").assertIsDisplayed()
        composeRule.onNodeWithText("可用模型").assertDoesNotExist()
        composeRule.onNodeWithText("2 个").assertDoesNotExist()
        composeRule.onNodeWithText("账户可用").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullDeepSeekUnavailableAccountDropsInlineStatusRow() {
        val credential = credential("DeepSeek 不可用", ProviderId.DeepSeek)
        val total = balance(38.40).copy(id = "balance", label = "余额")
        val availability = total.copy(
            id = "availability",
            label = "账户状态",
            value = BigDecimal.ZERO,
            unit = UsageMetricUnit.Boolean_,
            presentation = UsageMetricPresentation.Status,
            semantic = UsageMetricSemantic.Status,
            healthState = UsageMetricHealthState.Unavailable,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(total, availability),
                        allowedModels = listOf("deepseek-v4-flash"),
                        planName = "API 余额",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("账户不可用").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullXiaomiApiCardShowsUsageBreakdownWithoutDuplicateBalances() {
        val credential = credential("小米 MiMo API", ProviderId.Xiaomi)
        val accountBalance = balance(12.50).copy(
            id = "account-balance",
            label = "账户余额",
        )
        val cash = accountBalance.copy(
            id = "cash-balance",
            label = "现金余额",
            value = BigDecimal("10.00"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val gift = accountBalance.copy(
            id = "gift-balance",
            label = "赠送余额",
            value = BigDecimal("2.50"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val consumption = accountBalance.copy(
            id = "total-consumption",
            label = "近 31 天消费",
            value = BigDecimal("2.00"),
            presentation = UsageMetricPresentation.Value,
            semantic = UsageMetricSemantic.Value,
        )
        val models = listOf(
            "mimo-v2.5-pro",
            "mimo-v2.5",
            "mimo-v2.5-asr",
            "mimo-v2.5-tts",
            "mimo-v2.5-tts-voicedesign",
            "mimo-v2.5-tts-voiceclone",
        )

        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(
                            accountBalance,
                            cash,
                            gift,
                            consumption,
                            token("total-tokens", "近 31 天消耗", 1500.0),
                            token("input-tokens", "未命中缓存", 900.0),
                            token("output-tokens", "输出", 300.0),
                            token("cache-tokens", "命中缓存", 300.0),
                        ),
                        allowedModels = models,
                        statusText = "可用",
                        usageKind = "api",
                        billingMode = "按量计费",
                        planName = "API 按量",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("¥12.50").assertIsDisplayed()
        composeRule.onNodeWithText("账户可用").assertDoesNotExist()
        composeRule.onNodeWithText("现金 ¥10.00").assertIsDisplayed()
        composeRule.onNodeWithText("赠送 ¥2.50").assertIsDisplayed()
        composeRule.onNodeWithText("近 31 天").assertIsDisplayed()
        composeRule.onNodeWithText("按量计费").assertDoesNotExist()
        composeRule.onNodeWithText("¥2.00").assertIsDisplayed()
        composeRule.onNodeWithText("1,500").assertIsDisplayed()
        composeRule.onNodeWithText("未命中 900").assertIsDisplayed()
        composeRule.onNodeWithText("输出 300").assertIsDisplayed()
        composeRule.onNodeWithText("缓存 300").assertIsDisplayed()
        composeRule.onNodeWithText("可用模型").assertDoesNotExist()
        composeRule.onNodeWithText("6 个").assertDoesNotExist()
        composeRule.onNodeWithText("mimo-v2.5-pro").assertDoesNotExist()
        assertEquals(
            1,
            composeRule.onAllNodesWithText("现金 ¥10.00").fetchSemanticsNodes().size,
        )
        assertEquals(
            1,
            composeRule.onAllNodesWithText("赠送 ¥2.50").fetchSemanticsNodes().size,
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun compactBalanceShowsAmountWithoutHealthCopyOrResetPill() {
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
        composeRule.onNodeWithText("账户余额 · CNY").assertDoesNotExist()
        composeRule.onNodeWithText("¥38.42").assertIsDisplayed()
        composeRule.onNodeWithTag("card_footer_left", useUnmergedTree = true)
            .assertTextEquals("${formatResetTime(now)} 更新")
        composeRule.onNodeWithText("充足", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("偏低", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("不足", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("重置", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h1200dp")
    fun commandCodeFullCardShowsPeriodLedgerOnly() {
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
            metric("monthly-remaining", "月度剩余", value = 34.42),
            metric("purchased-remaining", "购买剩余", value = 0.0),
            metric("free-remaining", "赠送剩余", value = 0.0),
            metric("period-spent", "周期消费", value = 35.54),
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

        listOf("5 小时额度", "本周额度", "本月额度").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeRule.onNodeWithText("额度单位：美元（USD）").assertDoesNotExist()
        composeRule.onNodeWithText("18%").assertIsDisplayed()
        composeRule.onNodeWithText("7%").assertIsDisplayed()
        composeRule.onNodeWithText("34%").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 \$11.55").assertDoesNotExist()
        composeRule.onNodeWithText("累计请求", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("9.7K 次", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("额度构成").assertDoesNotExist()
        listOf("月度 \$34.42", "购买 \$0.00", "赠送 \$0.00").forEach { legend ->
            composeRule.onNodeWithText(legend, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullGlmCardShowsPeriodLedgerWithoutActivityFacts() {
        val credential = credential("GLM 完整模式", ProviderId.Glm)
        fun valueMetric(id: String, label: String, value: Double, unit: UsageMetricUnit) =
            UsageMetric(
                id = id,
                label = label,
                value = BigDecimal.valueOf(value),
                unit = unit,
                presentation = UsageMetricPresentation.Value,
                semantic = UsageMetricSemantic.Value,
                healthState = UsageMetricHealthState.Normal,
            )
        val testCard = card(
            credential,
            metrics = listOf(
                glmProgressMetric("five-hour", "5 小时用量", resetCards = 2),
                glmProgressMetric("weekly", "每周用量", resetCards = 1),
                valueMetric("model-calls", "模型调用量", 22.0, UsageMetricUnit.Request),
                UsageMetric(
                    id = "zcode-mcp",
                    label = "MCP 调用量",
                    used = BigDecimal("461"),
                    limit = BigDecimal("1000"),
                    remaining = BigDecimal("539"),
                    value = BigDecimal("461"),
                    unit = UsageMetricUnit.Request,
                    presentation = UsageMetricPresentation.Value,
                    semantic = UsageMetricSemantic.UsedQuota,
                    healthState = UsageMetricHealthState.Normal,
                ),
                valueMetric(
                    "activity-total-tokens",
                    "累计 Token 数",
                    1_890_000_000.0,
                    UsageMetricUnit.Token,
                ),
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    testCard,
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("5 小时用量").assertIsDisplayed()
        composeRule.onNodeWithText("本周用量").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 张重置卡").assertExists()
        composeRule.onNodeWithContentDescription("1 张重置卡").assertDoesNotExist()
        composeRule.onNodeWithText("模型调用").assertDoesNotExist()
        composeRule.onNodeWithText("22 次").assertDoesNotExist()
        composeRule.onNodeWithText("MCP 调用").assertDoesNotExist()
        composeRule.onNodeWithText("累计 Tokens").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullNewApiCardShowsTokenFactsWithoutCosts() {
        val credential = credential("New API 统计", ProviderId.NewAPI)
        val quota = progress(3.5, 14.0).copy(
            id = "quota-progress",
            label = "账户额度",
            remaining = BigDecimal("10.50"),
            currencyCode = "CNY",
        )
        val today = token("today-token", "今日 Token", 14_873_680.0)
        val todayCost = token("today-token-cost", "今日消费", 1.4)
            .copy(unit = UsageMetricUnit.Currency, currencyCode = "CNY")
        val oneDay = token("one-day-token", "近 24 小时 Token", 15_123_456.0)
        val oneDayCost = token("one-day-token-cost", "近 24 小时消费", 1.42)
            .copy(unit = UsageMetricUnit.Currency, currencyCode = "CNY")
        val sevenDay = token("seven-day-token", "近 7 天 Token", 139_735_349.0)
        val sevenDayCost = token("seven-day-token-cost", "近 7 天消费", 13.1)
            .copy(unit = UsageMetricUnit.Currency, currencyCode = "CNY")
        val thirtyDay = token("thirty-day-token", "近 30 天 Token", 1_100_848_403.0)
        val thirtyDayCost = token("thirty-day-token-cost", "近 30 天消费", 103.2)
            .copy(unit = UsageMetricUnit.Currency, currencyCode = "CNY")

        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(
                            quota,
                            today,
                            todayCost,
                            oneDay,
                            oneDayCost,
                            sevenDay,
                            sevenDayCost,
                            thirtyDay,
                            thirtyDayCost,
                            token("rpm", "近 60 秒 RPM", 3.0)
                                .copy(unit = UsageMetricUnit.Request),
                            token("tpm", "近 60 秒 TPM", 456.0),
                            token("request-count", "账户累计请求", 42.0)
                                .copy(unit = UsageMetricUnit.Request),
                        ),
                        planName = "New API · vip",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("账户额度").assertIsDisplayed()
        composeRule.onNodeWithText("25%").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 ¥10.50").assertDoesNotExist()
        listOf("今日 Token", "24 小时", "7 天", "30 天").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onNodeWithText("14.87M tokens").assertIsDisplayed()
        composeRule.onNodeWithText("15.12M tokens").assertIsDisplayed()
        composeRule.onNodeWithText("1.1B tokens").assertIsDisplayed()
        composeRule.onNodeWithText("¥1.40").assertDoesNotExist()
        composeRule.onNodeWithText("¥13.10").assertDoesNotExist()
        composeRule.onNodeWithText("RPM").assertIsDisplayed()
        composeRule.onNodeWithText("TPM").assertIsDisplayed()
        composeRule.onNodeWithText("42 次").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullMimoTokenPlanShowsCreditsAndSubscriptionStatus() {
        val credential = credential("MiMo Plan", ProviderId.Xiaomi)
        val models = listOf("m1", "m2", "m3", "m4", "m5", "m6")
        val plan = UsageMetric(
            id = "plan-total",
            label = "套餐总量",
            used = BigDecimal("5000"),
            limit = BigDecimal("20000"),
            remaining = BigDecimal("15000"),
            unit = UsageMetricUnit.Token,
            windowEnd = now.plusSeconds(86_400 * 30),
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            healthState = UsageMetricHealthState.Normal,
        )
        val status = UsageMetric(
            id = "plan-status",
            label = "订阅状态",
            value = BigDecimal.ONE,
            unit = UsageMetricUnit.Boolean_,
            presentation = UsageMetricPresentation.Status,
            semantic = UsageMetricSemantic.Status,
            healthState = UsageMetricHealthState.Normal,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(plan, status),
                        allowedModels = models,
                        statusText = "有效",
                        usageKind = "plan",
                        billingMode = "自动续费",
                        planName = "Pro 月度套餐",
                        subscriptionEndAt = now.plusSeconds(86_400 * 30),
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("套餐").assertIsDisplayed()
        composeRule.onNodeWithText("25%").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 15,000 credits").assertDoesNotExist()
        composeRule.onNodeWithText("套餐总量").assertIsDisplayed()
        composeRule.onNodeWithText("20,000 credits").assertIsDisplayed()
        composeRule.onNodeWithText("已使用").assertIsDisplayed()
        composeRule.onNodeWithText("订阅状态").assertIsDisplayed()
        composeRule.onNodeWithText("有效").assertIsDisplayed()
        composeRule.onNodeWithText("可用模型").assertIsDisplayed()
        composeRule.onNodeWithText("6 个").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullRoutinTokenPackShowsTotalsAndModelCount() {
        val credential = credential("Routin Token Pack", ProviderId.Routin)
        val tokenPack = UsageMetric(
            id = "token",
            label = "Token",
            used = BigDecimal("320000000"),
            limit = BigDecimal("1000000000"),
            remaining = BigDecimal("680000000"),
            unit = UsageMetricUnit.Token,
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            healthState = UsageMetricHealthState.Normal,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(tokenPack),
                        allowedModels = listOf("a", "b", "c"),
                        usageKind = "tokenPack",
                        planName = "Pro",
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("Token 套餐").assertIsDisplayed()
        composeRule.onNodeWithText("32%").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 680M tokens").assertDoesNotExist()
        composeRule.onNodeWithText("总额度").assertIsDisplayed()
        composeRule.onNodeWithText("1B tokens").assertIsDisplayed()
        composeRule.onNodeWithText("已使用").assertIsDisplayed()
        composeRule.onNodeWithText("320M tokens").assertIsDisplayed()
        composeRule.onNodeWithText("可用模型").assertIsDisplayed()
        composeRule.onNodeWithText("3 个").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullOpenCodeCardKeepsThreeWindowHierarchy() {
        val credential = credential("OpenCode Go", ProviderId.OpenCode)
        val metrics = listOf(
            progress(6.0, 30.0).copy(id = "fiveHour", label = "5 小时"),
            progress(15.0, 75.0).copy(id = "weekly", label = "周"),
            progress(45.0, 150.0).copy(id = "monthly", label = "月"),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = metrics,
                        planName = "OpenCode Go",
                        statusText = "正常续费",
                        usageKind = "periodic",
                        billingMode = "自动续费",
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("5 小时额度").assertIsDisplayed()
        composeRule.onNodeWithText("本周用量").assertIsDisplayed()
        composeRule.onNodeWithText("本月用量").assertIsDisplayed()
        composeRule.onAllNodesWithText("20%").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("30%").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullQuotaSplitPlacesSecondaryWindowToTheRight() {
        val credential = credential("双栏配额", ProviderId.Routin)
        val metrics = listOf(
            progress(20.0, 100.0).copy(id = "fiveHour", label = "5 小时"),
            progress(10.0, 100.0).copy(id = "weekly", label = "周"),
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(credential, metrics = metrics),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        val primary = composeRule.onNodeWithText("5 小时额度", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val secondary = composeRule.onNodeWithText("本周用量", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(secondary.left.value > primary.right.value)
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp")
    fun fullVolcengineCardFallsBackToTheOnlyAvailableWindow() {
        val credential = credential("火山方舟", ProviderId.Volcengine)
        val weekly = UsageMetric(
            id = "weekly",
            label = "近一周用量",
            used = BigDecimal("3252.2867"),
            limit = BigDecimal("35000"),
            remaining = BigDecimal("31747.7133"),
            unit = UsageMetricUnit.Request,
            windowEnd = now.plusSeconds(86_400),
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            healthState = UsageMetricHealthState.Normal,
        )
        composeRule.setContent {
            MaterialTheme {
                CredentialUsageCard(
                    card(
                        credential,
                        metrics = listOf(weekly),
                        planName = "Max Plan",
                        subscriptionEndAt = null,
                    ),
                    onOpen = {},
                    onRetry = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithText("本周用量").assertIsDisplayed()
        composeRule.onNodeWithText("9%").assertIsDisplayed()
        composeRule.onNodeWithText("暂无用量数据").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1200dp-h800dp")
    fun credentialCardKeepsNarrowMetricLayoutOnWideScreens() {
        val credential = credential("宽屏主账号", ProviderId.Routin)
        val card = card(
            credential,
            metrics = listOf(
                progress(12.0, 60.0).copy(id = "fiveHour", label = "5 小时"),
                progress(20.0, 100.0).copy(id = "weekly", label = "周"),
            ),
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
                    usageCardDensity = UsageCardDensity.COMPACT,
                )
            }
        }

        composeRule.onNodeWithTag("home_grid_4_columns").assertExists()
        composeRule.onNodeWithText("5 小时", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("周", useUnmergedTree = true).assertIsDisplayed()
        val cardBounds = activeNode("credential_card_${credential.id}")
            .getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val availableWidth = rootBounds.right.value - rootBounds.left.value
        assertTrue(
            "wide screen should still use a narrow card",
            cardBounds.right.value - cardBounds.left.value < availableWidth * 0.6f,
        )
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
        activeText("启用").assertIsDisplayed()
        composeRule.onNodeWithText("停用").assertDoesNotExist()

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_item_routin")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("备用").assertDoesNotExist()
        composeRule.onNodeWithTag("provider_filter_menu").assertIsSelected()
        composeRule.onNodeWithText("Routin").assertIsDisplayed()
    }

    @Test
    fun 从页面顶部下拉在报告上方显示刷新指示器并触发刷新() {
        val cards = (1..3).map { index ->
            card(credential("下拉 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var refreshCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = { refreshCalls += 1 },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            down(Offset(width / 2f, 20f))
            moveTo(Offset(width / 2f, 460f), delayMillis = 220)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_pull_indicator", useUnmergedTree = true)
            .assertIsDisplayed()
        val indicatorBounds = composeRule
            .onNodeWithTag("home_pull_indicator", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val reportBounds = composeRule
            .onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "刷新指示器应出现在报告上方，实际 indicator.bottom=${indicatorBounds.bottom.value} report.top=${reportBounds.top.value}",
            indicatorBounds.bottom.value <= reportBounds.top.value + 1f,
        )

        composeRule.onNodeWithTag("home_list_full").performTouchInput { up() }
        composeRule.waitUntil(5_000) { refreshCalls > 0 }
        assertEquals(1, refreshCalls)
    }

    @Test
    fun 刷新未结束时上滑会立即收起顶部刷新图标() {
        val cards = (1..3).map { index ->
            card(credential("刷新收起 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var refreshing by mutableStateOf(false)
        var refreshCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                        isRefreshingAll = refreshing,
                    ),
                    onRefreshAll = {
                        refreshCalls += 1
                        refreshing = true
                    },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            down(Offset(width / 2f, 20f))
            moveTo(Offset(width / 2f, 460f), delayMillis = 220)
            up()
        }
        composeRule.waitUntil(5_000) { refreshing && refreshCalls == 1 }
        composeRule.onNodeWithTag("home_pull_indicator", useUnmergedTree = true)
            .assertIsDisplayed()

        // 刷新还在进行时立刻上滑：指示器必须收起，但不能取消已经在跑的刷新。
        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeUp(startY = height * 0.92f, endY = height * 0.08f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertTrue("刷新不应被上滑取消", refreshing)
        composeRule.onNodeWithTag("home_pull_indicator", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun 从列表下方一路滑到顶部不会触发刷新() {
        val cards = (1..8).map { index ->
            card(credential("长列表 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var refreshCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = { refreshCalls += 1 },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_list_full").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeUp(startY = height * 0.92f, endY = height * 0.08f, durationMillis = 500)
        }
        composeRule.waitForIdle()
        assertEquals(0, refreshCalls)
    }

    @Test
    fun 同一次手势上滑再回拉不会触发刷新() {
        val cards = (1..3).map { index ->
            card(credential("回拉 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var refreshCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = { refreshCalls += 1 },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_health_report").performTouchInput {
            down(Offset(width / 2f, height / 2f))
            moveBy(Offset(0f, -180f))
            moveBy(Offset(0f, 260f))
            moveBy(Offset(0f, 260f))
            moveBy(Offset(0f, 260f))
            up()
        }
        composeRule.waitForIdle()
        assertEquals(0, refreshCalls)
    }

    @Test
    fun 非顶部开始的手势回到顶部后不会顺带触发刷新() {
        val cards = (1..12).map { index ->
            card(credential("回顶 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var refreshCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                    ),
                    onRefreshAll = { refreshCalls += 1 },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_list_full").performScrollToIndex(4)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeDown(startY = height * 0.15f, endY = height * 0.92f, durationMillis = 90)
        }
        composeRule.waitForIdle()
        // 回到顶部后必须重新起手下拉，而不是沿用刚才从列表中部开始的手势。
        composeRule.onNodeWithTag("home_list_full").performScrollToIndex(0)
        composeRule.waitForIdle()
        assertEquals(0, refreshCalls)

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            down(Offset(width / 2f, 20f))
            moveTo(Offset(width / 2f, 460f), delayMillis = 220)
            up()
        }
        composeRule.waitUntil(5_000) { refreshCalls == 1 }
    }

    @Test
    fun 点击报告风险项会定位到对应凭证卡片() {
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

        composeRule.onNodeWithTag("health_finding_${cards[4].credential.id}").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${cards[4].credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("credential_card_${cards[4].credential.id}")
            .assertIsDisplayed()
    }

    @Test
    fun 简洁模式报告风险项可定位到瀑布流中的凭证卡片() {
        val cards = (1..20).map { index ->
            val item = credential("瀑布定位 $index")
            val used = if (index == 20) 90.0 else 10.0 + index
            card(item, metrics = listOf(progress(used, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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

        composeRule.onNodeWithTag("health_finding_${cards[19].credential.id}").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${cards[19].credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("credential_card_${cards[19].credential.id}")
            .assertIsDisplayed()
    }

    @Test
    fun 点击报告风险项会把中间凭证卡片定位到屏幕中心() {
        val cards = (1..7).map { index ->
            val item = credential("健康居中 $index")
            val used = if (index == 4) 90.0 else index * 10.0
            card(item, metrics = listOf(progress(used, 100.0)))
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("health_finding_${cards[3].credential.id}").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${cards[3].credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitForIdle()

        // 定位由程序化滚动驱动，头部也必须同步收起，不能继续盖住居中的卡片。
        composeRule.onNodeWithTag("home_nav_summary", useUnmergedTree = true)
            .assertIsDisplayed()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val card = activeNode("credential_card_${cards[3].credential.id}")
            .getUnclippedBoundsInRoot()
        val rootCenter = (root.top.value + root.bottom.value) / 2f
        val cardCenter = (card.top.value + card.bottom.value) / 2f
        assertTrue(
            "卡片中心应贴近屏幕中心，实际偏差 ${abs(cardCenter - rootCenter)}dp",
            abs(cardCenter - rootCenter) < 16f,
        )
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
    fun 横向拖动卡片区切换模式并在松手后停靠() {
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("滑动手势"))),
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
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        // 按住并拖到一半：页面应已经跟着手指平移，而不是松手后才切换。
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            down(Offset(width * 0.8f, height * 0.4f))
            moveTo(Offset(width * 0.5f, height * 0.4f), delayMillis = 150)
        }
        composeRule.waitForIdle()
        val draggedBounds = composeRule.onNodeWithTag("home_grid_2_columns")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "左滑过程中简洁页应跟随手势左移，实际 left=${draggedBounds.left}",
            draggedBounds.left.value < -10f,
        )
        composeRule.onNodeWithTag("home_list_full").assertExists()
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            moveTo(Offset(width * 0.2f, height * 0.4f), delayMillis = 100)
            up()
        }
        composeRule.waitUntil(5_000) { density == UsageCardDensity.FULL }
        assertEquals(UsageCardDensity.FULL, density)

        composeRule.onNodeWithTag("home_pager").performTouchInput {
            down(Offset(width * 0.2f, height * 0.4f))
            moveTo(Offset(width * 0.8f, height * 0.4f), delayMillis = 150)
            up()
        }
        composeRule.waitUntil(5_000) { density == UsageCardDensity.COMPACT }
        assertEquals(UsageCardDensity.COMPACT, density)
    }

    @Test
    fun 快速反向滑动不会被上一次模式切换拉回() {
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = (1..6).map { card(credential("反向滑动 $it"), metrics = listOf(progress(20.0, 100.0))) },
                        groups = emptyList(),
                        credentialCount = 6,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        // 先左滑，在分页尚未完全停稳时立刻右滑，最终必须停在简洁页。
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 90)
        }
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            swipeRight(startX = width * 0.1f, endX = width * 0.9f, durationMillis = 90)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(5_000) {
            runCatching { activeNode("home_grid_2_columns").fetchSemanticsNode() }.isSuccess
        }
        assertEquals(UsageCardDensity.COMPACT, density)
    }

    @Test
    fun 用户滑动后过期的密度回调不会把分页抢回去() {
        var density by mutableStateOf(UsageCardDensity.FULL)
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = (1..6).map { card(credential("过期回调 $it"), metrics = listOf(progress(20.0, 100.0))) },
                        groups = emptyList(),
                        credentialCount = 6,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        density = if (density == UsageCardDensity.FULL) {
                            UsageCardDensity.COMPACT
                        } else {
                            UsageCardDensity.FULL
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("home_pager").performTouchInput {
            swipeRight(startX = width * 0.1f, endX = width * 0.9f, durationMillis = 180)
        }
        composeRule.waitUntil(5_000) {
            runCatching { activeNode("home_grid_2_columns").fetchSemanticsNode() }.isSuccess
        }

        // 模拟上一次模式写入晚到：用户已经滑到简洁页，旧的 FULL 回调不能把页面抢回去。
        composeRule.runOnIdle { density = UsageCardDensity.FULL }
        composeRule.waitForIdle()
        assertTrue(
            "过期的密度回调不应把页面从简洁页拉回完整页",
            runCatching { activeNode("home_grid_2_columns").fetchSemanticsNode() }.isSuccess,
        )
    }

    @Test
    fun 用户左滑后过期的密度回调不会把分页抢回去() {
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = (1..6).map { card(credential("左滑过期 $it"), metrics = listOf(progress(20.0, 100.0))) },
                        groups = emptyList(),
                        credentialCount = 6,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("home_pager").performTouchInput {
            swipeLeft(startX = width * 0.9f, endX = width * 0.1f, durationMillis = 180)
        }
        composeRule.waitUntil(5_000) {
            runCatching { activeNode("home_list_full").fetchSemanticsNode() }.isSuccess
        }

        // 旧的 COMPACT 回调不能把已经左滑到的完整页抢回简洁页。
        composeRule.runOnIdle { density = UsageCardDensity.COMPACT }
        composeRule.waitForIdle()
        assertTrue(
            "过期的密度回调不应把页面从完整页拉回简洁页",
            runCatching { activeNode("home_list_full").fetchSemanticsNode() }.isSuccess,
        )
    }

    @Test
    fun 切换密度后头部与目标页顶部对齐() {
        val cards = (1..12).map { index ->
            card(credential("密度对齐 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = density,
                    onToggleUsageCardDensity = {
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("home_grid_2_columns").performTouchInput {
            swipeUp(startY = height * 0.92f, endY = height * 0.08f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            swipeLeft(startX = width * 0.85f, endX = width * 0.15f, durationMillis = 180)
        }
        composeRule.waitUntil(5_000) {
            runCatching { activeNode("home_list_full").fetchSemanticsNode() }.isSuccess
        }
        composeRule.waitForIdle()

        val toolbarBottom = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        val firstTop = activeNode("credential_card_${cards[0].credential.id}")
            .getUnclippedBoundsInRoot()
            .top
        assertTrue(
            "切换后目标页在顶部时，首张卡片应紧贴工具条，实际 gap=${firstTop.value - toolbarBottom.value}dp",
            abs(firstTop.value - toolbarBottom.value) <= 3f,
        )
    }

    @Test
    fun 报告风险项点击回调携带凭证() {
        val warning = card(credential("风险"), metrics = listOf(progress(90.0, 100.0)))
        var clicked: UUID? = null
        composeRule.setContent {
            MyTokenTheme {
                HomeHealthReport(
                    cards = listOf(warning),
                    syncText = "刚刚同步",
                    isRefreshing = false,
                    onRefresh = {},
                    onLocate = { clicked = it },
                )
            }
        }

        composeRule.onNodeWithTag("health_finding_${warning.credential.id}").performClick()
        assertEquals(warning.credential.id, clicked)
    }

    @Test
    fun 顶部展示产品品牌入口与琥珀报告() {
        val cards = listOf(
            card(credential("正常账号"), metrics = listOf(progress(20.0, 100.0))),
            card(credential("接近上限"), metrics = listOf(progress(60.0, 100.0))),
            card(credential("超限账号"), metrics = listOf(progress(90.0, 100.0))),
        )
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = emptyList(),
                        credentialCount = cards.size,
                        lastUpdatedText = "刚刚同步",
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("home_wordmark", useUnmergedTree = true).assertTextEquals("MyToken")
        composeRule.onNodeWithTag("home_health_report").assertIsDisplayed()
        composeRule.onNodeWithTag("report_attention_count").assertTextEquals("2")
        composeRule.onNodeWithTag("report_tail").assertTextEquals("接近上限")
        composeRule.onNodeWithTag("report_dial")
            .assertContentDescriptionContains("3 个凭证，1 个正常，2 个需注意")
        composeRule.onNodeWithTag("health_finding_${cards[1].credential.id}").assertExists()
        composeRule.onNodeWithTag("health_finding_${cards[2].credential.id}").assertExists()
        composeRule.onNodeWithTag("health_finding_${cards[0].credential.id}").assertDoesNotExist()
        composeRule.onNodeWithText("最近同步", substring = true).assertIsDisplayed()
    }

    @Test
    fun 报告为空时只保留同步行() {
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("全部正常"), metrics = listOf(progress(20.0, 100.0)))),
                        groups = emptyList(),
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

        composeRule.onNodeWithTag("report_tail").assertTextEquals("用量正常")
        composeRule.onNodeWithText("全部凭证的当前用量正常。", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("最近同步", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("report_refresh").assertIsDisplayed()
    }

    @Test
    fun 上滑后固定导航展示用量摘要且供应商工具条不再固顶() {
        val cards = (1..20).map { index ->
            card(credential("上滑 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        val navBottom = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        val toolbarBefore = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
        val reportBefore = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "初始时报告应在工具条上方，实际 nav=${navBottom.value} toolbar=${toolbarBefore.top.value}",
            toolbarBefore.top.value > navBottom.value + 50f,
        )
        assertTrue(
            "初始时报告应完整可见，实际底部 ${reportBefore.bottom.value}",
            reportBefore.bottom.value > navBottom.value + 100f,
        )

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeUp(startY = height * 0.75f, endY = height * 0.1f, durationMillis = 300)
        }
        composeRule.waitForIdle()

        val toolbarAfter = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
        val reportAfter = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "供应商工具条应完全滑出视口，实际底部 ${toolbarAfter.bottom.value}",
            toolbarAfter.bottom.value <= navBottom.value + 2f,
        )
        assertTrue(
            "报告应随卡片一起滑出视口，实际底部 ${reportAfter.bottom.value}",
            reportAfter.bottom.value <= navBottom.value + 2f,
        )
        composeRule.onNodeWithTag("home_nav_summary", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_open_settings").assertIsDisplayed()

        repeat(2) {
            composeRule.onNodeWithTag("home_list_full").performTouchInput {
                swipeDown(startY = height * 0.15f, endY = height * 0.85f, durationMillis = 300)
            }
            composeRule.waitForIdle()
        }
        val reportRestored = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "下拉到列表顶部后报告应重新展开，实际底部 ${reportRestored.bottom.value}",
            reportRestored.bottom.value > navBottom.value + 100f,
        )
        composeRule.onNodeWithTag("home_nav_summary", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun 小幅上滑时报告跟手收起而不是整块消失() {
        val cards = (1..8).map { index ->
            card(credential("小幅 $index"), metrics = listOf(progress(90.0, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        val navBottom = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            val startY = height * 0.88f
            down(Offset(width / 2f, startY))
            moveTo(Offset(width / 2f, startY - 120f), delayMillis = 600)
            up()
        }
        composeRule.waitForIdle()

        val report = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "小幅上滑后报告仍应有一部分留在视口内，实际 top=${report.top.value} bottom=${report.bottom.value} nav=${navBottom.value}",
            report.top.value < navBottom.value && report.bottom.value > navBottom.value + 20f,
        )
    }

    @Test
    fun 多条风险报告展开时首张卡片紧贴工具条() {
        val cards = (1..8).map { index ->
            card(credential("贴边 $index"), metrics = listOf(progress(90.0, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
        composeRule.waitForIdle()

        val toolbarBottom = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        val firstCardTop = composeRule.onNodeWithTag("credential_card_${cards[0].credential.id}")
            .getUnclippedBoundsInRoot()
            .top
        assertTrue(
            "首张卡片应紧跟工具条，实际 gap=${firstCardTop.value - toolbarBottom.value}dp",
            abs(firstCardTop.value - toolbarBottom.value) <= 3f,
        )
    }

    @Test
    fun 上滑收起报告时分页区保持固定不重排() {
        val cards = (1..16).map { index ->
            card(credential("固定分页 $index"), metrics = listOf(progress(20.0 + index, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        fun pagerHeight(): Float = composeRule.onNodeWithTag("home_pager")
            .getUnclippedBoundsInRoot()
            .let { it.bottom.value - it.top.value }

        val before = pagerHeight()
        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeUp(startY = height * 0.82f, endY = height * 0.12f, durationMillis = 80)
        }
        composeRule.waitForIdle()
        val after = pagerHeight()
        assertEquals(
            "报告收起时只平移内容，分页区高度不能变化而触发整块重排",
            before,
            after,
            0.5f,
        )
        val toolbar = composeRule.onNodeWithTag("home_provider_toolbar")
            .getUnclippedBoundsInRoot()
        val navBottom = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        assertTrue(
            "快速上滑后供应商工具条必须完全离开视口，实际底部 ${toolbar.bottom.value}",
            toolbar.bottom.value <= navBottom.value + 2f,
        )
    }

    @Test
    fun 在报告上拖拽也能收起并重新展开头部() {
        val cards = (1..10).map { index ->
            card(credential("报告拖拽 $index"), metrics = listOf(progress(20.0, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        val navBottom = composeRule.onNodeWithTag("home_toolbar")
            .getUnclippedBoundsInRoot()
            .bottom
        composeRule.onNodeWithTag("home_health_report").performTouchInput {
            // 报告本身只有一百多 dp；把指针拖出报告边界也应由头部手势继续处理。
            swipeUp(startY = height * 0.9f, endY = height * 0.1f - height * 1.5f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertTrue(
            "在报告上上滑也应把报告和工具条一起收起",
            composeRule.onNodeWithTag("home_provider_toolbar")
                .getUnclippedBoundsInRoot()
                .bottom
                .value <= navBottom.value + 2f,
        )
        composeRule.onNodeWithTag("home_nav_summary", useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeDown(startY = height * 0.15f, endY = height * 0.85f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        val reportBottom = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
            .bottom
        assertTrue(
            "从列表顶部下拉应重新展开报告，实际底部 ${reportBottom.value}",
            reportBottom.value > navBottom.value + 100f,
        )
    }

    @Test
    fun 卡片末尾只保留居中的管理凭证胶囊() {
        val credential = credential("入口")
        var addCalls = 0
        var manageCalls = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential, metrics = listOf(progress(20.0, 100.0)))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = { addCalls++ },
                    onOpenCredentials = { manageCalls++ },
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        activeNode("home_add_credential").assertDoesNotExist()
        activeText("把其他账户也放到这里").assertDoesNotExist()
        activeNode("home_manage_credentials").assertIsDisplayed()
        activeText("管理凭证").assertIsDisplayed()

        val pillBounds = activeNode("home_manage_credentials").getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val pillCenter = (pillBounds.left.value + pillBounds.right.value) / 2f
        val rootCenter = (rootBounds.left.value + rootBounds.right.value) / 2f
        assertTrue(
            "管理凭证胶囊应水平居中，实际 pill=$pillCenter root=$rootCenter",
            abs(pillCenter - rootCenter) <= 2f,
        )

        activeNode("home_manage_credentials").performClick()
        assertEquals(0, addCalls)
        assertEquals(1, manageCalls)
    }

    @Test
    fun 上滑后导航摘要展示报警供应商logo() {
        val warningRoutin = card(
            credential("报警 Routin", ProviderId.Routin),
            metrics = listOf(progress(90.0, 100.0)),
        )
        val warningGlm = card(
            credential("报警 GLM", ProviderId.Glm),
            metrics = listOf(progress(95.0, 100.0)),
        )
        val cards = listOf(warningRoutin, warningGlm) + (1..18).map { index ->
            card(credential("普通 $index"), metrics = listOf(progress(10.0, 100.0)))
        }
        composeRule.setContent {
            MyTokenTheme {
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
                    usageCardDensity = UsageCardDensity.FULL,
                )
            }
        }

        composeRule.onNodeWithTag("home_list_full").performTouchInput {
            swipeUp(startY = height * 0.75f, endY = height * 0.1f, durationMillis = 300)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_nav_summary", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_nav_summary_count", useUnmergedTree = true)
            .assertTextEquals("2个凭证")
        composeRule.onNodeWithTag("home_nav_summary_tail", useUnmergedTree = true)
            .assertTextEquals("接近上限")
        composeRule.onNodeWithTag("home_nav_alert_logo_0", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_nav_alert_logo_1", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_nav_alert_logo_2", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun 供应商筛选使用底部面板并保留全部计数() {
        val routin = card(
            credential("Routin 账户", ProviderId.Routin),
            metrics = listOf(progress(20.0, 100.0)),
        )
        val deepSeek = card(
            credential("DeepSeek 账户", ProviderId.DeepSeek),
            metrics = listOf(progress(30.0, 100.0)),
        )
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(routin, deepSeek),
                        groups = listOf(
                            group(ProviderId.Routin, listOf(routin)),
                            group(ProviderId.DeepSeek, listOf(deepSeek)),
                        ),
                        credentialCount = 2,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_sheet", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("选择供应商", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(
            "仅筛选下方用量卡片，报告始终展示全部凭证",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag("provider_filter_item_all", useUnmergedTree = true)
            .assertIsSelected()
        composeRule.onNodeWithText("2 个凭证", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_filter_item_deepseek", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag("provider_filter_cancel", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun 报告刷新按钮触发全部刷新() {
        var refreshCount = 0
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("刷新"), metrics = listOf(progress(20.0, 100.0)))),
                        groups = emptyList(),
                        credentialCount = 1,
                    ),
                    onRefreshAll = { refreshCount += 1 },
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("report_refresh").performClick()
        assertEquals(1, refreshCount)
    }

    @Test
    fun 供应商筛选只影响下方卡片不影响报告() {
        val routin = card(
            credential("Routin 账户", ProviderId.Routin),
            metrics = listOf(progress(20.0, 100.0)),
        )
        val deepSeek = card(
            credential("DeepSeek 告警", ProviderId.DeepSeek),
            metrics = listOf(progress(90.0, 100.0)),
        )
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(routin, deepSeek),
                        groups = listOf(
                            group(ProviderId.Routin, listOf(routin)),
                            group(ProviderId.DeepSeek, listOf(deepSeek)),
                        ),
                        credentialCount = 2,
                    ),
                    onRefreshAll = {},
                    onRefreshCredential = {},
                    onOpenCredential = {},
                    onImportFromMac = {},
                    onAddManually = {},
                )
            }
        }

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_item_routin")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        activeNode("credential_card_${routin.credential.id}").assertExists()
        composeRule.onNodeWithTag("credential_card_${deepSeek.credential.id}").assertDoesNotExist()
        // 报告始终展示全部凭证，不受下方筛选影响。
        composeRule.onNodeWithTag("health_finding_${deepSeek.credential.id}").assertExists()
        composeRule.onNodeWithTag("report_attention_count").assertTextEquals("1")
    }

    @Test
    fun 点击报告风险项会清除筛选并定位到对应卡片() {
        val routinCards = (1..4).map { index ->
            card(
                credential("Routin $index", ProviderId.Routin),
                metrics = listOf(progress(10.0 + index, 100.0)),
            )
        }
        val deepSeek = card(
            credential("DeepSeek 告警", ProviderId.DeepSeek),
            metrics = listOf(progress(90.0, 100.0)),
        )
        val cards = routinCards + deepSeek
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = cards,
                        groups = listOf(
                            group(ProviderId.Routin, routinCards),
                            group(ProviderId.DeepSeek, listOf(deepSeek)),
                        ),
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

        composeRule.onNodeWithTag("provider_filter_menu").performClick()
        composeRule.onNodeWithTag("provider_filter_item_routin")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("credential_card_${deepSeek.credential.id}").assertDoesNotExist()

        composeRule.onNodeWithTag("health_finding_${deepSeek.credential.id}").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("provider_filter_menu").assertIsNotSelected()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("credential_card_${deepSeek.credential.id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitForIdle()
        activeNode("credential_card_${deepSeek.credential.id}").assertIsDisplayed()
    }

    @Test
    fun 密度滑块随分页手势移动() {
        var density by mutableStateOf(UsageCardDensity.COMPACT)
        composeRule.setContent {
            MyTokenTheme {
                HomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        cards = listOf(card(credential("滑块"), metrics = listOf(progress(20.0, 100.0)))),
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
                        density = if (density == UsageCardDensity.COMPACT) {
                            UsageCardDensity.FULL
                        } else {
                            UsageCardDensity.COMPACT
                        }
                    },
                )
            }
        }

        fun thumbLeft(): Float = composeRule
            .onNodeWithTag("usage_density_thumb", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .left
            .value

        val start = thumbLeft()
        composeRule.onNodeWithTag("home_pager").performTouchInput {
            down(Offset(width * 0.8f, height * 0.5f))
            moveTo(Offset(width * 0.45f, height * 0.5f), delayMillis = 150)
        }
        composeRule.waitForIdle()
        val mid = thumbLeft()
        assertTrue("滑块应跟随分页进度右移，start=$start mid=$mid", mid > start + 10f)

        composeRule.onNodeWithTag("home_pager").performTouchInput {
            moveTo(Offset(width * 0.15f, height * 0.5f), delayMillis = 100)
            up()
        }
        composeRule.waitUntil(5_000) { density == UsageCardDensity.FULL }
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
                    updateVersion = "6.1.0",
                )
            }
        }

        composeRule.onNodeWithTag("home_update_badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("打开设置，有可用更新").assertIsDisplayed()
    }

    @Test
    fun 有可用更新时报告上方显示星星提示条并弹窗确认更新() {
        var openedSettings = 0
        var confirmedUpdates = 0
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
                    onOpenSettings = { openedSettings++ },
                    hasUpdateAvailable = true,
                    updateVersion = "6.1.0",
                    updateReleaseNotes = "## 本次更新\n- 修复快速滑动卡顿\n- 更新提示支持星星光效",
                    onConfirmUpdate = { confirmedUpdates++ },
                )
            }
        }

        composeRule.onNodeWithTag("home_update_badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("home_update_star", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("home_update_banner").assertIsDisplayed()
        composeRule.onNodeWithText("发现新版本 6.1.0", useUnmergedTree = true).assertIsDisplayed()
        val banner = composeRule.onNodeWithTag("home_update_banner")
            .getUnclippedBoundsInRoot()
        val report = composeRule.onNodeWithTag("home_health_report")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "更新提示条应位于凭证报告上方，banner.bottom=${banner.bottom.value} report.top=${report.top.value}",
            banner.bottom.value <= report.top.value + 1f,
        )

        composeRule.onNodeWithTag("home_update_banner").performClick()
        composeRule.onNodeWithTag("home_update_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("本次更新", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("修复快速滑动卡顿", substring = true, useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, openedSettings)

        composeRule.onNodeWithTag("home_update_confirm").performClick()
        assertEquals(1, confirmedUpdates)
        assertEquals(0, openedSettings)
        composeRule.onNodeWithTag("home_update_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("home_open_settings").performClick()
        assertEquals(1, openedSettings)
    }

    @Test
    fun 无可用更新时不显示更新提示条与提示点() {
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
        composeRule.onNodeWithTag("home_update_badge", useUnmergedTree = true)
            .assertDoesNotExist()
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
        activeNode("credential_refresh_${credential.id}")
            .performSemanticsAction(SemanticsActions.OnClick)
        val refreshBounds = activeNode("credential_refresh_${credential.id}")
            .getUnclippedBoundsInRoot()
        val failureBounds = activeNode("credential_failure_${credential.id}")
            .getUnclippedBoundsInRoot()
        assertTrue(failureBounds.left < refreshBounds.left)
        // 头像承载在 44dp 触摸区内、按钮 30dp，中心对齐后顶部差约 7dp。
        assertTrue(kotlin.math.abs(failureBounds.top.value - refreshBounds.top.value) < 10f)
        assertTrue(refreshBounds.right <= composeRule.onRoot().getUnclippedBoundsInRoot().right)
        assertEquals(true, refreshed)
        activeNode("credential_failure_${credential.id}")
            .performSemanticsAction(SemanticsActions.OnClick)
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
