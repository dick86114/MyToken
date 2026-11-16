package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.UsageMetric
import java.math.BigDecimal

internal enum class CompactBalanceSegmentKind {
    ToppedUp,
    Granted,
}

internal data class CompactBalanceSegment(
    val id: String,
    val label: String,
    val value: BigDecimal,
    val kind: CompactBalanceSegmentKind,
)

/**
 * 简洁余额卡的构成明细，与 macOS 的余额拆分顺序和展示口径保持一致。
 * 只展示大于 0 的明细，避免零余额占据简洁卡片空间。
 */
internal object CompactBalanceCompositionPolicy {
    private val metricIDs = listOf(
        "toppedUpBalance",
        "grantedBalance",
        "cash-balance",
        "gift-balance",
    )

    fun segments(metrics: List<UsageMetric>): List<CompactBalanceSegment> =
        metricIDs.mapNotNull { metricID ->
            val metric = metrics.firstOrNull { it.id == metricID } ?: return@mapNotNull null
            val value = metric.value ?: return@mapNotNull null
            if (value.signum() <= 0) return@mapNotNull null
            val (label, kind) = when (metricID) {
                "toppedUpBalance" -> "充值" to CompactBalanceSegmentKind.ToppedUp
                "grantedBalance" -> "赠金" to CompactBalanceSegmentKind.Granted
                "cash-balance" -> "现金" to CompactBalanceSegmentKind.ToppedUp
                else -> "赠送" to CompactBalanceSegmentKind.Granted
            }
            CompactBalanceSegment(
                id = metricID,
                label = label,
                value = value,
                kind = kind,
            )
        }
}
