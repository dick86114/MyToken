package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class CompactBalanceCompositionPolicyTest {
    @Test
    fun `按 macOS 顺序筛选正数余额明细`() {
        val segments = CompactBalanceCompositionPolicy.segments(
            listOf(
                metric("gift-balance", 2.5),
                metric("toppedUpBalance", 3.0),
                metric("cash-balance", 10.0),
                metric("grantedBalance", 2.0),
            )
        )

        assertEquals(
            listOf("toppedUpBalance", "grantedBalance", "cash-balance", "gift-balance"),
            segments.map { it.id },
        )
        assertEquals(listOf("充值", "赠金", "现金", "赠送"), segments.map { it.label })
        assertEquals(
            listOf(
                CompactBalanceSegmentKind.ToppedUp,
                CompactBalanceSegmentKind.Granted,
                CompactBalanceSegmentKind.ToppedUp,
                CompactBalanceSegmentKind.Granted,
            ),
            segments.map { it.kind },
        )
    }

    @Test
    fun `零余额和缺失余额不进入简洁明细`() {
        val segments = CompactBalanceCompositionPolicy.segments(
            listOf(
                metric("cash-balance", 10.0),
                metric("gift-balance", 0.0),
            )
        )

        assertEquals(listOf("cash-balance"), segments.map { it.id })
    }

    private fun metric(id: String, value: Double) = UsageMetric(
        id = id,
        label = id,
        value = BigDecimal.valueOf(value),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.Value,
        currencyCode = "CNY",
        healthState = UsageMetricHealthState.Normal,
    )
}
