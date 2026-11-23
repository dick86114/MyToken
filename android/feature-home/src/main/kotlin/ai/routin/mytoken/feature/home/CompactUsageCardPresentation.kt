package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation

/** 简洁卡片的排布与色调规则。 */
internal enum class CompactUsageArrangement {
    /** 余额型：金额 + 构成条。 */
    Balance,

    /** 配额型：一个主指标环 + 0..2 条迷你窗口行。 */
    RingWithWindows,

    /** 多个值型指标：2 列数值网格。 */
    ValueTiles,
}

internal enum class UsageMetricTone { Normal, Warning, Critical }

internal object CompactUsageCardPresentation {
    fun arrangement(forMetrics: List<UsageMetric>): CompactUsageArrangement = when {
        forMetrics.isEmpty() -> CompactUsageArrangement.ValueTiles
        forMetrics.size == 1 && forMetrics[0].presentation == UsageMetricPresentation.Balance ->
            CompactUsageArrangement.Balance

        forMetrics.all { it.presentation == UsageMetricPresentation.Progress } ->
            CompactUsageArrangement.RingWithWindows

        else -> CompactUsageArrangement.ValueTiles
    }

    /** 环形图内只取数字部分，百分号由视图单独缩小排版。 */
    fun compactPercentNumberText(percent: Double?): String {
        val text = formatPercent(percent)
        return if (text.endsWith("%")) text.dropLast(1) else text
    }

    /** 与卡面状态文案共用 [metricHealthTone]，阈值只在共享契约里定义一次。 */
    fun tone(metric: UsageMetric): UsageMetricTone {
        if (metric.presentation != UsageMetricPresentation.Progress) return UsageMetricTone.Normal
        return when (metricHealthTone(metric)) {
            UsageCardHealth.Critical -> UsageMetricTone.Critical
            UsageCardHealth.Warning -> UsageMetricTone.Warning
            else -> UsageMetricTone.Normal
        }
    }
}

/** 余额金额的排版拆分：币种符号/代码用小字（0.6em），数字保持 tabular。 */
internal data class BalanceAmountParts(
    val prefix: String,
    val core: String,
    val suffix: String,
)

internal fun splitBalanceAmount(formatted: String): BalanceAmountParts {
    val prefixEnd = formatted.indexOfFirst { it.isDigit() || it == '-' || it == '.' }
    val prefix = if (prefixEnd == -1) "" else formatted.substring(0, prefixEnd)
    val rest = if (prefixEnd == -1) formatted else formatted.substring(prefixEnd)
    val spaceIndex = rest.indexOf(' ')
    return if (spaceIndex == -1) {
        BalanceAmountParts(prefix, rest, "")
    } else {
        BalanceAmountParts(prefix, rest.substring(0, spaceIndex), rest.substring(spaceIndex + 1))
    }
}
