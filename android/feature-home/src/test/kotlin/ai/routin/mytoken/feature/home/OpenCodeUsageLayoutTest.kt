package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCodeUsageLayoutTest {
    @Test
    fun displayNameUsesOpenCodeBrand() {
        assertEquals("OpenCode", ProviderCatalog.displayName(ProviderId.OpenCode))
    }

    @Test
    fun accentColorIsVisuallyDistinctFromExistingBrands() {
        val openCode = ProviderCatalog.accentColor(ProviderId.OpenCode)
        val volcengine = ProviderCatalog.accentColor(ProviderId.Volcengine)
        val redDelta = openCode.red - volcengine.red
        val greenDelta = openCode.green - volcengine.green
        val blueDelta = openCode.blue - volcengine.blue
        val distance = sqrt(redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta)

        assertTrue("OpenCode 和火山方舟主题色过于接近", distance > 0.25)
    }

    @Test
    fun metricOrderIsShortWindowToMonthly() {
        val metrics = listOf(
            metric("monthly"),
            metric("weekly"),
            metric("fiveHour"),
            metric("unknown"),
        )

        assertEquals(
            listOf("fiveHour", "weekly", "monthly"),
            OpenCodeCardPolicy.orderedMetrics(metrics).map { it.id },
        )
    }

    @Test
    fun lifecycleLabelSeparatesRenewalFromExpiry() {
        assertEquals("续费", OpenCodeCardPolicy.lifecyclePrefix("自动续费"))
        assertEquals("续费", OpenCodeCardPolicy.lifecyclePrefix(null))
        assertEquals("到期", OpenCodeCardPolicy.lifecyclePrefix("取消续订"))
    }

    private fun metric(id: String) = UsageMetric(
        id = id,
        label = id,
        used = BigDecimal.ONE,
        limit = BigDecimal.TEN,
        remaining = BigDecimal("9"),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        currencyCode = "$",
    )
}
