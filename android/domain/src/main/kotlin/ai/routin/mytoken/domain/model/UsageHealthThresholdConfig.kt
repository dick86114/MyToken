package ai.routin.mytoken.domain.model

/**
 * Configurable quota-health thresholds. The shared 80/95 values remain the default,
 * but Settings (global and per-credential) decide what "warning"/"critical" means.
 *
 * The percentages are always used-percentage semantics; RemainingQuota metrics are
 * converted to their used equivalent before comparison.
 */
data class UsageHealthThresholdConfig(
    val lowPercent: Int = UsageHealthThresholds.WARNING_USED_PERCENT,
    val highPercent: Int = UsageHealthThresholds.CRITICAL_USED_PERCENT,
) {
    init {
        require(lowPercent in MIN_PERCENT..MAX_PERCENT) { "lowPercent must be in 1..99" }
        require(highPercent in MIN_PERCENT..MAX_PERCENT) { "highPercent must be in 1..99" }
        require(lowPercent < highPercent) { "lowPercent must be lower than highPercent" }
    }

    fun stateForUsedPercent(percent: Double?): UsageMetricHealthState = when {
        percent == null || !percent.isFinite() -> UsageMetricHealthState.Unknown
        percent >= highPercent -> UsageMetricHealthState.Critical
        percent >= lowPercent -> UsageMetricHealthState.Warning
        else -> UsageMetricHealthState.Normal
    }

    companion object {
        const val MIN_PERCENT = 1
        const val MAX_PERCENT = 99
        val DEFAULT = UsageHealthThresholdConfig()
    }
}
