package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 详情页 Hero 默认展示最小周期窗口，而不是接口返回顺序里的第一项。 */
class HeroMetricSelectionTest {

    private val now = Instant.parse("2026-09-14T08:30:00Z")

    private fun metric(
        id: String,
        label: String,
        start: Instant? = null,
        end: Instant? = null,
        presentation: UsageMetricPresentation = UsageMetricPresentation.Progress,
    ) = UsageMetric(
        id = id,
        label = label,
        used = BigDecimal("42"),
        limit = BigDecimal("100"),
        remaining = BigDecimal("58"),
        unit = UsageMetricUnit.Currency,
        windowStart = start,
        windowEnd = end,
        presentation = presentation,
        semantic = if (presentation == UsageMetricPresentation.Progress) {
            UsageMetricSemantic.UsedQuota
        } else {
            UsageMetricSemantic.Balance
        },
        healthState = UsageMetricHealthState.Normal,
    )

    @Test
    fun 默认取窗口时长最短的进度指标() {
        val weekly = metric(
            id = "weekly",
            label = "周",
            start = now.minusSeconds(7 * 86_400),
            end = now,
        )
        val fiveHour = metric(
            id = "five-hour",
            label = "5 小时",
            start = now.minusSeconds(5 * 3_600),
            end = now,
        )

        assertEquals(fiveHour, defaultHeroProgressMetric(listOf(weekly, fiveHour)))
    }

    @Test
    fun 没有起止时间时按id与标签推断周期() {
        val monthly = metric(id = "credit-progress", label = "月额度")
        val fiveHour = metric(id = "five-hour", label = "5 小时用量")
        val weekly = metric(id = "weekly", label = "周")

        assertEquals(
            fiveHour,
            defaultHeroProgressMetric(listOf(monthly, weekly, fiveHour)),
        )
    }

    @Test
    fun 余额卡没有进度指标时返回null() {
        val balance = metric(
            id = "balance",
            label = "余额",
            presentation = UsageMetricPresentation.Balance,
        )

        assertNull(defaultHeroProgressMetric(listOf(balance)))
    }
}
