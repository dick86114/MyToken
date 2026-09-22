package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation

internal enum class CompactUsageArrangement {
    Balance,
    HorizontalRings,
    VerticalRings,
    ValueTiles,
}

internal enum class UsageMetricTone { Normal, Warning, Critical }

/** 简洁卡片的排布与色调规则，与 macOS CompactUsageCardPresentation 对齐。 */
internal object CompactUsageCardPresentation {
    fun arrangement(forMetrics: List<UsageMetric>): CompactUsageArrangement = when {
        forMetrics.isEmpty() -> CompactUsageArrangement.ValueTiles
        forMetrics.size == 1 && forMetrics[0].presentation == UsageMetricPresentation.Balance ->
            CompactUsageArrangement.Balance
        forMetrics.all { it.presentation == UsageMetricPresentation.Progress } && forMetrics.size <= 2 ->
            CompactUsageArrangement.HorizontalRings
        forMetrics.all { it.presentation == UsageMetricPresentation.Progress } && forMetrics.size == 3 ->
            CompactUsageArrangement.VerticalRings
        else -> CompactUsageArrangement.ValueTiles
    }

    /** 环形图内只取数字部分，百分号由视图单独缩小排版。 */
    fun compactPercentNumberText(percent: Double?): String {
        val text = formatPercent(percent)
        return if (text.endsWith("%")) text.dropLast(1) else text
    }

    fun tone(metric: UsageMetric): UsageMetricTone {
        if (metric.presentation != UsageMetricPresentation.Progress) return UsageMetricTone.Normal
        if (metric.healthState == UsageMetricHealthState.Critical) return UsageMetricTone.Critical
        val percent = progressPercent(metric) ?: return UsageMetricTone.Normal
        return when {
            percent >= 80.0 -> UsageMetricTone.Critical
            percent >= 50.0 -> UsageMetricTone.Warning
            else -> UsageMetricTone.Normal
        }
    }
}
