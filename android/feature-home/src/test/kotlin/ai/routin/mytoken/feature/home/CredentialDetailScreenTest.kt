package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageGroupMultiplier
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageResetCard
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import java.math.BigDecimal
import java.time.Instant
import androidx.compose.ui.unit.dp
import java.util.UUID
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
class CredentialDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fetchedAt = Instant.parse("2026-09-14T08:30:00Z")

    private fun credential(
        provider: ProviderId = ProviderId.Routin,
        metadata: Map<CredentialMetadataKey, String> = emptyMap(),
    ) = Credential(
        id = UUID.fromString("00000001-0000-0000-0000-000000000000"),
        providerId = provider,
        credentialKind = CredentialKind.BearerApiKey,
        name = "主力账户",
        metadata = metadata,
    )

    private fun progress(id: String = "fiveHour") = UsageMetric(
        id = id,
        label = "5 小时",
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "USD",
        healthState = UsageMetricHealthState.Normal,
    )

    private fun card(
        credential: Credential = credential(),
        metrics: List<UsageMetric> = listOf(progress()),
        status: RefreshStatus = RefreshStatus.Ready,
        error: AppError? = null,
        isStale: Boolean = false,
        allowedModels: List<String> = emptyList(),
        groupMultipliers: List<UsageGroupMultiplier> = emptyList(),
    ) = CredentialCardUi(
        credential = credential,
        status = status,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = fetchedAt,
            metrics = metrics,
            planName = "成长版",
            allowedModels = allowedModels,
            groupMultipliers = groupMultipliers,
        ),
        isStale = isStale,
        error = error,
        freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )

    @Test
    fun heroShowsUpdateTimeOnceAndNoDataTimeRow() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(),
                    nextRefreshAt = fetchedAt.plusSeconds(300),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag("detail_hero", useUnmergedTree = true).assertExists()
        assertEquals(
            "更新时间只出现一次，且包含下次刷新",
            1,
            composeRule.onAllNodesWithText("更新", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
        composeRule.onNodeWithText("下次刷新", substring = true, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("数据时间", substring = true).assertDoesNotExist()
    }

    @Test
    fun failedDetailKeepsDataAndShowsRetry() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(
                        status = RefreshStatus.Failed,
                        error = AppError.Network("连接超时"),
                    ),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        assertTrue(
            "失败时仍保留缓存的剩余额度",
            composeRule.onAllNodesWithText("剩余 58%", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
        assertTrue(
            "失败状态应可见",
            composeRule.onAllNodesWithText("更新失败", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
        composeRule.onNodeWithTag("detail_refresh_now", useUnmergedTree = true).assertExists()
    }

    @Test
    fun balanceDetailUsesCompositionAndGroupsCurrencies() {
        val balanceCard = card(
            credential = credential(ProviderId.DeepSeek),
            metrics = listOf(
                balance("balance-CNY", 120.0, "CNY"),
                balance("toppedUpBalance-CNY", 80.0, "CNY"),
                balance("grantedBalance-CNY", 40.0, "CNY"),
                balance("balance-USD", 50.0, "USD"),
                balance("toppedUpBalance-USD", 50.0, "USD"),
            ),
        )
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = balanceCard,
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        listOf("¥120.00", "$50.00").forEach { amount ->
            assertTrue(
                "应展示 $amount",
                composeRule.onAllNodesWithText(amount, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
        listOf("充值", "赠金").forEach { legend ->
            assertTrue(
                "应展示构成图例 $legend",
                composeRule.onAllNodesWithText(legend, substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
    }

    @Test
    fun detailBottomActionsPutRefreshOnTheRightAsPrimary() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val edit = composeRule.onNodeWithTag("detail_edit", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val refresh = composeRule.onNodeWithTag("detail_refresh_now", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("立即刷新应在编辑右侧", refresh.left > edit.left)
    }

    @Test
    fun detailShareDeleteAndBackAre48() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        listOf("分享用量", "删除凭证", "返回").forEach { description ->
            val bounds = composeRule.onNodeWithContentDescription(description, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue(
                "$description 触控高度应 ≥48dp",
                bounds.bottom - bounds.top >= 48.dp,
            )
        }
        composeRule.onNodeWithContentDescription("更多操作", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun heroDefaultsToShortestWindowMetric() {
        val weekly = progress("weekly").copy(
            label = "周",
            windowStart = fetchedAt.minusSeconds(7 * 86_400),
            windowEnd = fetchedAt,
        )
        val fiveHour = progress("five-hour").copy(
            label = "5 小时",
            windowStart = fetchedAt.minusSeconds(5 * 3_600),
            windowEnd = fetchedAt,
        )

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(metrics = listOf(weekly, fiveHour)),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag("detail_hero_metric_label", useUnmergedTree = true)
            .assertTextEquals("5 小时用量")
    }

    @Test
    fun detailWindowsShowResetCardsAsCompactChips() {
        val first = Instant.parse("2026-12-01T08:00:00Z")
        val second = Instant.parse("2026-12-15T08:00:00Z")
        val metric = progress("five-hour").copy(
            label = "5 小时用量",
            resetCards = listOf(UsageResetCard(first), UsageResetCard(second)),
        )

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(metrics = listOf(metric)),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val chips = composeRule.onAllNodesWithTag("detail_reset_chip", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("重置卡应按到期日拆成日期 chip", 2, chips.size)
        composeRule.onNodeWithText(formatResetCardCompact(first), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("重置卡 2 张 ·", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun detailModelsUseStackedDeckAndExpandToGrid() {
        val models = (1..12).map { "glm-5.$it" }

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(
                        allowedModels = models,
                        groupMultipliers = listOf(
                            UsageGroupMultiplier("编程组", BigDecimal("1.0")),
                        ),
                    ),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("分组倍率").assertDoesNotExist()
        val deck = composeRule.onNodeWithTag("detail_models_more", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("glm-5.1").assertExists()
        composeRule.onNodeWithText("另有 11 个模型", substring = true, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithContentDescription("复制模型 ID glm-5.2").assertDoesNotExist()

        deck.performClick()
        composeRule.waitForIdle()
        val first = composeRule.onNodeWithContentDescription("复制模型 ID glm-5.1")
            .performScrollTo()
            .getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithContentDescription("复制模型 ID glm-5.2")
            .getUnclippedBoundsInRoot()
        assertEquals("展开后模型应排成两列", first.top, second.top)
        assertTrue("第二个模型应在右侧一列", second.left > first.left)
        composeRule.onNodeWithContentDescription("复制模型 ID glm-5.12")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("detail_models_collapse", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("detail_models_more", useUnmergedTree = true).assertExists()
    }

    @Test
    fun detailModelsWithSingleModelShowDirectCopyRow() {
        val models = listOf("glm-5.3-air")

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(allowedModels = models),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag("detail_models_more", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("复制模型 ID glm-5.3-air")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun detailConfigWrapsLongWebsiteUrl() {
        // Robolectric 每个字形约 1px，URL 需要足够长才能触发换行。
        val url = "https://example.com/" +
            (1..40).joinToString("/") { "very-long-segment-$it" } +
            "?token=abcdefghijklmnop"
        val credential = credential(
            metadata = mapOf(CredentialMetadataKey.WebsiteURL to url),
        )

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(credential = credential),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag("detail_website_url", useUnmergedTree = true)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(results)
            }
        val layout = results.single()
        assertEquals("官网地址不应限制行数", Int.MAX_VALUE, layout.layoutInput.maxLines)
        assertEquals("官网地址不能用省略号截断", TextOverflow.Clip, layout.layoutInput.overflow)
        assertEquals(
            "官网地址应完整展示",
            url,
            layout.layoutInput.text.text.replace("\u200B", ""),
        )
        assertFalse("官网地址不应被截断", layout.hasVisualOverflow)
    }

    @Test
    fun detailWindowsAlwaysPutMonthLast() {
        val monthly = progress("credit-progress").copy(label = "月")
        val weekly = progress("weekly").copy(label = "周")
        val fiveHour = progress("five-hour").copy(label = "5 小时")

        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = card(metrics = listOf(monthly, weekly, fiveHour)),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val tops = listOf("5 小时", "周", "月").map { label ->
            composeRule.onNodeWithText(label, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .top
                .value
        }
        assertTrue(
            "全部窗口应按 5 小时 → 周 → 月 排列，实际 $tops",
            tops[0] < tops[1] && tops[1] < tops[2],
        )
    }

    private fun balance(id: String, value: Double, currency: String) = UsageMetric(
        id = id,
        label = "余额",
        value = BigDecimal.valueOf(value),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Balance,
        semantic = UsageMetricSemantic.Balance,
        currencyCode = currency,
        healthState = UsageMetricHealthState.Normal,
    )
}
