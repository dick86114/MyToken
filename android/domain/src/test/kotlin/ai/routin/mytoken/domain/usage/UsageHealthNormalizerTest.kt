package ai.routin.mytoken.domain.usage

import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class UsageHealthNormalizerTest {

    private val custom = UsageHealthThresholdConfig(lowPercent = 85, highPercent = 95)

    private fun metric(
        state: UsageMetricHealthState = UsageMetricHealthState.Unknown,
        used: Double? = null,
        remaining: Double? = null,
        limit: Double? = 100.0,
        semantic: UsageMetricSemantic = UsageMetricSemantic.UsedQuota,
        presentation: UsageMetricPresentation = UsageMetricPresentation.Progress,
        id: String = "quota",
    ) = UsageMetric(
        id = id,
        label = id,
        used = used?.toBigDecimal(),
        limit = limit?.toBigDecimal(),
        remaining = remaining?.toBigDecimal(),
        unit = UsageMetricUnit.Token,
        presentation = presentation,
        semantic = semantic,
        healthState = state,
    )

    private fun normalize(vararg metrics: UsageMetric, thresholds: UsageHealthThresholdConfig = custom) =
        UsageHealthNormalizer.normalize(
            UsageSnapshot(
                credentialId = UUID.fromString("11111111-1111-4111-8111-111111111111"),
                fetchedAt = Instant.parse("2026-09-07T04:00:00Z"),
                metrics = metrics.toList(),
            ),
            thresholds,
        )

    @Test
    fun `default thresholds keep 80 and 95 boundaries`() {
        val default = UsageHealthThresholdConfig.DEFAULT
        assertEquals(UsageMetricHealthState.Normal, default.stateForUsedPercent(79.99))
        assertEquals(UsageMetricHealthState.Warning, default.stateForUsedPercent(80.0))
        assertEquals(UsageMetricHealthState.Warning, default.stateForUsedPercent(94.99))
        assertEquals(UsageMetricHealthState.Critical, default.stateForUsedPercent(95.0))
    }

    @Test
    fun `used quota follows custom low and high boundaries`() {
        val belowLow = normalize(metric(used = 84.0)).metrics.single()
        val atLow = normalize(metric(used = 85.0)).metrics.single()
        val belowHigh = normalize(metric(used = 94.0)).metrics.single()
        val atHigh = normalize(metric(used = 95.0)).metrics.single()

        assertEquals(UsageMetricHealthState.Normal, belowLow.healthState)
        assertEquals(UsageMetricHealthState.Warning, atLow.healthState)
        assertEquals(UsageMetricHealthState.Warning, belowHigh.healthState)
        assertEquals(UsageMetricHealthState.Critical, atHigh.healthState)
    }

    @Test
    fun `unknown quota health is filled when percent is computable`() {
        val normalized = normalize(metric(used = 85.0, state = UsageMetricHealthState.Unknown))

        assertEquals(UsageMetricHealthState.Warning, normalized.metrics.single().healthState)
    }

    @Test
    fun `used quota falls back to limit minus remaining`() {
        val normalized = normalize(metric(used = null, remaining = 15.0))

        assertEquals(UsageMetricHealthState.Warning, normalized.metrics.single().healthState)
    }

    @Test
    fun `remaining quota converts to used percent before applying thresholds`() {
        // remaining 16% => used 84% => Normal under 85/95.
        val normal = normalize(
            metric(
                remaining = 16.0,
                semantic = UsageMetricSemantic.RemainingQuota,
                state = UsageMetricHealthState.Unknown,
            )
        ).metrics.single()
        // remaining 15% => used 85% => Warning.
        val warning = normalize(
            metric(
                remaining = 15.0,
                semantic = UsageMetricSemantic.RemainingQuota,
                state = UsageMetricHealthState.Unknown,
            )
        ).metrics.single()
        // remaining 5% => used 95% => Critical.
        val critical = normalize(
            metric(
                remaining = 5.0,
                semantic = UsageMetricSemantic.RemainingQuota,
                state = UsageMetricHealthState.Unknown,
            )
        ).metrics.single()

        assertEquals(UsageMetricHealthState.Normal, normal.healthState)
        assertEquals(UsageMetricHealthState.Warning, warning.healthState)
        assertEquals(UsageMetricHealthState.Critical, critical.healthState)
    }

    @Test
    fun `remaining quota falls back to used when remaining is null`() {
        val normalized = normalize(
            metric(
                used = 96.0,
                remaining = null,
                semantic = UsageMetricSemantic.RemainingQuota,
            )
        )

        assertEquals(UsageMetricHealthState.Critical, normalized.metrics.single().healthState)
    }

    @Test
    fun `unavailable and stale states are preserved`() {
        val normalized = normalize(
            metric(used = 99.0, state = UsageMetricHealthState.Unavailable),
            metric(used = 99.0, state = UsageMetricHealthState.Stale, id = "stale"),
        )

        assertEquals(
            UsageMetricHealthState.Unavailable,
            normalized.metrics[0].healthState,
        )
        assertEquals(UsageMetricHealthState.Stale, normalized.metrics[1].healthState)
    }

    @Test
    fun `balance and status metrics keep the provider result`() {
        val balance = metric(
            used = 99.0,
            state = UsageMetricHealthState.Warning,
            semantic = UsageMetricSemantic.Balance,
            presentation = UsageMetricPresentation.Balance,
            id = "balance",
        )
        val status = metric(
            used = 99.0,
            state = UsageMetricHealthState.Normal,
            semantic = UsageMetricSemantic.Status,
            presentation = UsageMetricPresentation.Status,
            id = "status",
        )

        val normalized = normalize(balance, status)

        assertEquals(UsageMetricHealthState.Warning, normalized.metrics[0].healthState)
        assertEquals(UsageMetricHealthState.Normal, normalized.metrics[1].healthState)
    }

    @Test
    fun `uncomputable quota keeps its original state`() {
        val noLimit = metric(used = 84.0, limit = null)
        val zeroLimit = metric(used = 84.0, limit = 0.0, id = "zero")
        val noAmounts = metric(used = null, remaining = null, id = "empty")

        val normalized = normalize(noLimit, zeroLimit, noAmounts)

        normalized.metrics.forEach {
            assertEquals(UsageMetricHealthState.Unknown, it.healthState)
        }
        assertNull(UsageHealthNormalizer.usedPercentOf(noLimit))
        assertNull(UsageHealthNormalizer.usedPercentOf(zeroLimit))
        assertNull(UsageHealthNormalizer.usedPercentOf(noAmounts))
    }

    @Test
    fun `snapshot is returned unchanged when states already match`() {
        val snapshot = UsageSnapshot(
            credentialId = UUID.fromString("11111111-1111-4111-8111-111111111111"),
            fetchedAt = Instant.parse("2026-09-07T04:00:00Z"),
            metrics = listOf(
                metric(used = 85.0, state = UsageMetricHealthState.Warning),
            ),
        )

        val normalized = UsageHealthNormalizer.normalize(snapshot, custom)

        assertSame(snapshot, normalized)
    }

    @Test
    fun `used percent rounds out of BigDecimal without NaN`() {
        assertEquals(
            84.0,
            UsageHealthNormalizer.usedPercentOf(metric(used = 84.0))!!,
            0.0001,
        )
        assertEquals(
            85.0,
            UsageHealthNormalizer.usedPercentOf(
                metric(
                    remaining = 15.0,
                    semantic = UsageMetricSemantic.RemainingQuota,
                )
            )!!,
            0.0001,
        )
        assertEquals(
            null,
            UsageHealthNormalizer.usedPercentOf(metric(used = 84.0, limit = -1.0)),
        )
    }
}
