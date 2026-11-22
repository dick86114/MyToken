package ai.routin.mytoken.feature.home

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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 卡片健康映射与边界值：失败优先、阈值兜底、0 限额不产生 NaN。 */
class UsageCardHealthTest {

    private fun progress(id: String, used: Double, limit: Double, state: UsageMetricHealthState = UsageMetricHealthState.Unknown) =
        UsageMetric(
            id = id,
            label = id,
            used = BigDecimal.valueOf(used),
            limit = BigDecimal.valueOf(limit),
            remaining = BigDecimal.valueOf(limit - used),
            unit = UsageMetricUnit.Token,
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            healthState = state,
        )

    private fun card(
        metrics: List<UsageMetric>,
        status: RefreshStatus = RefreshStatus.Ready,
        error: AppError? = null,
    ): CredentialCardUi {
        val id = UUID.randomUUID()
        return CredentialCardUi(
            credential = Credential(
                id = id,
                providerId = ProviderId.Routin,
                credentialKind = CredentialKind.BearerApiKey,
                name = "测试凭证",
            ),
            status = status,
            snapshot = UsageSnapshot(credentialId = id, fetchedAt = Instant.parse("2026-09-14T00:00:00Z"), metrics = metrics),
            isStale = false,
            error = error,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
    }

    @Test
    fun warningAndCriticalOutrankNormal() {
        assertEquals(
            UsageCardHealth.Normal,
            usageCardHealth(card(listOf(progress("a", 20.0, 100.0)))),
        )
        assertEquals(
            UsageCardHealth.Warning,
            usageCardHealth(card(listOf(progress("a", 20.0, 100.0), progress("b", 60.0, 100.0)))),
        )
        assertEquals(
            UsageCardHealth.Critical,
            usageCardHealth(card(listOf(progress("a", 60.0, 100.0), progress("b", 90.0, 100.0)))),
        )
    }

    @Test
    fun failedKeepsCachedSnapshotAndMapsToFailed() {
        val failed = card(
            metrics = listOf(progress("a", 42.0, 100.0)),
            status = RefreshStatus.Failed,
            error = AppError.Authentication("凭证无效"),
        )

        assertEquals(UsageCardHealth.Failed, usageCardHealth(failed))
        assertNotNull("失败时必须保留上次成功数据", failed.snapshot)
        assertEquals(1, failed.snapshot?.metrics?.size)
        assertEquals("更新失败", usageCardStatusText(UsageCardHealth.Failed))
    }

    @Test
    fun zeroLimitReturnsNullPercentAndDashGauge() {
        val zeroLimit = progress("a", 10.0, 0.0)
        assertNull(progressPercent(zeroLimit))
        assertEquals("-", formatPercent(progressPercent(zeroLimit)))
        assertEquals("-", formatPercent(null))
    }

    @Test
    fun unavailableMetricIsCritical() {
        assertEquals(
            UsageCardHealth.Critical,
            usageCardHealth(card(listOf(progress("a", 10.0, 100.0, UsageMetricHealthState.Unavailable)))),
        )
    }

    @Test
    fun staleCardIsNeutralNotWarning() {
        val stale = card(listOf(progress("a", 90.0, 100.0)))
            .copy(isStale = true, status = RefreshStatus.Ready)

        assertEquals(UsageCardHealth.Stale, usageCardHealth(stale))
    }
}
