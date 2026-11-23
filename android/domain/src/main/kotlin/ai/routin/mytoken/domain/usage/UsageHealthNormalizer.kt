package ai.routin.mytoken.domain.usage

import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.math.BigDecimal

/**
 * Recomputes quota health from the user's current thresholds after a provider fetch
 * (and on cold-start cache restore). Only UsedQuota / RemainingQuota are touched:
 * - Unavailable and Stale are preserved (they describe availability, not severity).
 * - Balance / Status / Value keep the provider's result (balance still uses the
 *   per-credential absolute warning threshold).
 * - Unknown is filled in whenever a used percentage can be computed.
 */
object UsageHealthNormalizer {

    fun normalize(
        snapshot: UsageSnapshot,
        thresholds: UsageHealthThresholdConfig,
    ): UsageSnapshot {
        var changed = false
        val metrics = snapshot.metrics.map { metric ->
            val state = healthStateFor(metric, thresholds) ?: return@map metric
            if (state == metric.healthState) {
                metric
            } else {
                changed = true
                metric.copy(healthState = state)
            }
        }
        return if (changed) snapshot.copy(metrics = metrics) else snapshot
    }

    /** Returns the recomputed state, or null when the metric must keep its own state. */
    fun healthStateFor(
        metric: UsageMetric,
        thresholds: UsageHealthThresholdConfig,
    ): UsageMetricHealthState? {
        if (metric.healthState == UsageMetricHealthState.Unavailable ||
            metric.healthState == UsageMetricHealthState.Stale
        ) {
            return null
        }
        val usedPercent = usedPercentOf(metric) ?: return null
        return thresholds.stateForUsedPercent(usedPercent)
    }

    /** Used-equivalent percentage for quota metrics; null when it cannot be computed. */
    fun usedPercentOf(metric: UsageMetric): Double? {
        val limit = metric.limit ?: return null
        if (limit.signum() <= 0) return null
        val numerator: BigDecimal = when (metric.semantic) {
            UsageMetricSemantic.UsedQuota ->
                metric.used ?: metric.remaining?.let { limit - it } ?: return null
            UsageMetricSemantic.RemainingQuota ->
                metric.remaining?.let { limit - it } ?: metric.used ?: return null
            else -> return null
        }
        return numerator.toDouble() / limit.toDouble() * 100.0
    }
}
