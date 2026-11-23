package ai.routin.mytoken.core.alerts

import kotlin.math.roundToInt

/**
 * Shared geometry/normalization for the 1..99 integer double sliders used by the
 * global notification thresholds and per-credential overrides. Keeping it here
 * makes "low < high" structural instead of an input error.
 */
object PercentThresholdRange {
    const val MIN_PERCENT = 1
    const val MAX_PERCENT = 99

    /** Integer percentages 1..99. */
    val VALUE_RANGE = MIN_PERCENT.toFloat()..MAX_PERCENT.toFloat()

    /**
     * Rounds both ends and keeps at least a one-point gap, preserving whichever
     * thumb the user moved.
     */
    fun normalize(
        previous: ClosedFloatingPointRange<Float>,
        proposed: ClosedFloatingPointRange<Float>,
    ): IntRange {
        val previousLow = previous.start.roundToInt().coerceIn(MIN_PERCENT, MAX_PERCENT - 1)
        var low = proposed.start.roundToInt().coerceIn(MIN_PERCENT, MAX_PERCENT - 1)
        var high = proposed.endInclusive.roundToInt().coerceIn(MIN_PERCENT + 1, MAX_PERCENT)
        if (low >= high) {
            val startMoved = low != previousLow
            if (startMoved) {
                low = (high - 1).coerceAtLeast(MIN_PERCENT)
            } else {
                high = (low + 1).coerceAtMost(MAX_PERCENT)
            }
        }
        return low..high
    }

    fun label(lowPercent: Int, highPercent: Int): String =
        "低 $lowPercent% · 高 $highPercent%"
}
