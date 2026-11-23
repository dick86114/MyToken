package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** 卡面颜色分档沿用上游归一化状态：<80 中性、80–94.99 琥珀、≥95 红。 */
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

    private fun normalized(percent: Int) = progress(
        percent,
        UsageHealthThresholdConfig.DEFAULT.stateForUsedPercent(percent.toDouble()),
    )

    @Test
    fun `compact card tone follows normalized 80 and 95 boundaries`() {
        assertEquals(UsageMetricTone.Normal, CompactUsageCardPresentation.tone(normalized(79)))
        assertEquals(UsageMetricTone.Warning, CompactUsageCardPresentation.tone(normalized(80)))
        assertEquals(UsageMetricTone.Warning, CompactUsageCardPresentation.tone(normalized(94)))
        assertEquals(UsageMetricTone.Critical, CompactUsageCardPresentation.tone(normalized(95)))
    }

    @Test
    fun `unknown health stays neutral instead of re-applying fixed thresholds`() {
        assertEquals(UsageMetricTone.Normal, CompactUsageCardPresentation.tone(progress(95)))
    }
}
