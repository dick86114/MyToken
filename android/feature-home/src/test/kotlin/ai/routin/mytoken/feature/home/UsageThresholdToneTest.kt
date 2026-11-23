package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** 卡面颜色分档与共享阈值同源：<80 中性、80–94.99 琥珀、≥95 红。 */
class UsageThresholdToneTest {

    private fun progress(percent: Int, state: UsageMetricHealthState = UsageMetricHealthState.Unknown) = UsageMetric(
        id = "m",
        label = "月",
        used = percent.toBigDecimal(),
        limit = 100.toBigDecimal(),
        remaining = (100 - percent).toBigDecimal(),
        unit = UsageMetricUnit.Token,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        healthState = state,
    )

    @Test
    fun `compact card tone follows 80 and 95 boundaries`() {
        assertEquals(UsageMetricTone.Normal, CompactUsageCardPresentation.tone(progress(79)))
        assertEquals(UsageMetricTone.Warning, CompactUsageCardPresentation.tone(progress(80)))
        assertEquals(UsageMetricTone.Warning, CompactUsageCardPresentation.tone(progress(94)))
        assertEquals(UsageMetricTone.Critical, CompactUsageCardPresentation.tone(progress(95)))
    }
}
