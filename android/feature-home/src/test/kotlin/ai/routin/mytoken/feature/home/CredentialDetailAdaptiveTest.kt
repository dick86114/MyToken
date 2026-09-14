package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenTheme
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h1100dp")
class CredentialDetailAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 平板详情限制最大阅读宽度() {
        composeRule.setContent {
            MyTokenTheme {
                CredentialDetailScreen(
                    card = detailCard(),
                    onBack = {},
                    onRefresh = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val bounds = composeRule.onNodeWithTag("detail_content").getUnclippedBoundsInRoot()
        assertTrue(bounds.width <= 840.dp)
    }
}

private fun detailCard(): CredentialCardUi {
    val credential = Credential(
        id = UUID.randomUUID(),
        providerId = ProviderId.Routin,
        credentialKind = CredentialKind.BearerApiKey,
        name = "Routin 主账号",
    )
    val now = Instant.parse("2026-09-14T00:00:00Z")
    return CredentialCardUi(
        credential = credential,
        status = RefreshStatus.Ready,
        snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = now,
            metrics = listOf(
                UsageMetric(
                    id = "fiveHour",
                    label = "5 小时",
                    used = BigDecimal("42"),
                    limit = BigDecimal("100"),
                    remaining = BigDecimal("58"),
                    unit = UsageMetricUnit.Currency,
                    presentation = UsageMetricPresentation.Progress,
                    semantic = UsageMetricSemantic.UsedQuota,
                    currencyCode = "USD",
                    healthState = UsageMetricHealthState.Normal,
                ),
            ),
            planName = "成长版",
        ),
        isStale = false,
        error = null,
        freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )
}
