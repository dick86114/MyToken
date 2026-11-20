package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
import ai.routin.mytoken.domain.model.AppError
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import java.math.BigDecimal
import java.time.Instant
import androidx.compose.ui.unit.dp
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
class CredentialDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fetchedAt = Instant.parse("2026-09-14T08:30:00Z")

    private fun credential(provider: ProviderId = ProviderId.Routin) = Credential(
        id = UUID.fromString("00000001-0000-0000-0000-000000000000"),
        providerId = provider,
        credentialKind = CredentialKind.BearerApiKey,
        name = "主力账户",
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
    ) = CredentialCardUi(
        credential = credential,
        status = status,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = fetchedAt,
            metrics = metrics,
            planName = "成长版",
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
    fun detailShareAndMoreAre48() {
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

        listOf("分享用量", "更多操作", "返回").forEach { description ->
            val bounds = composeRule.onNodeWithContentDescription(description, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue(
                "$description 触控高度应 ≥48dp",
                bounds.bottom - bounds.top >= 48.dp,
            )
        }
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
