package ai.routin.mytoken.domain.model

/**
 * Single source of truth for quota health thresholds on Android.
 *
 * Mirrors `healthThresholds` in shared/provider-contracts/provider-capabilities.json
 * (enforced by the provider contract test) and the macOS UsageHealthThresholds:
 * a metric is warning from 80% used and critical from 95% used.
 *
 * When a metric carries a percent already (GLM / Volcengine / Xiaomi plan), the
 * percent is used-based. Remaining-based display converts the direction at the
 * presentation layer, never here.
 */
object UsageHealthThresholds {
    const val WARNING_USED_PERCENT: Int = 80
    const val CRITICAL_USED_PERCENT: Int = 95

    fun fromUsedPercent(percent: Double?): UsageMetricHealthState = when {
        percent == null || !percent.isFinite() -> UsageMetricHealthState.Unknown
        percent >= CRITICAL_USED_PERCENT -> UsageMetricHealthState.Critical
        percent >= WARNING_USED_PERCENT -> UsageMetricHealthState.Warning
        else -> UsageMetricHealthState.Normal
    }
}
