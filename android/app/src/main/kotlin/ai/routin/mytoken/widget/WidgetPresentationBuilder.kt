package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.feature.home.ProviderCatalog
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object WidgetPresentationBuilder {
    /** 单凭证组件固定聚焦环，只展示带进度的用量指标；余额型凭证单独处理。 */
    fun makeCredentialPresentation(
        credential: Credential,
        snapshot: UsageSnapshot?,
        size: WidgetSizeKind,
    ): WidgetPresentation = WidgetPresentation(
        style = WidgetDisplayStyle.FocusRing,
        size = size,
        credentials = listOf(
            credentialPresentation(credential = credential, snapshot = snapshot),
        ),
    )

    /** 总览组件固定信号格，凭证按风险排序后取当前尺寸能容纳的数量。 */
    fun makeOverviewPresentation(
        entries: List<Pair<Credential, UsageSnapshot?>>,
        size: WidgetSizeKind,
    ): WidgetPresentation = WidgetPresentation(
        style = WidgetDisplayStyle.SignalTiles,
        size = size,
        credentials = entries
            .filter { it.first.isEnabled }
            .sortedWith(compareBy({ riskScore(it.second) }, { it.first.sortOrder }))
            .take(size.overviewCapacity())
            .map { (credential, snapshot) -> credentialPresentation(credential, snapshot) },
    )

    private fun credentialPresentation(
        credential: Credential,
        snapshot: UsageSnapshot?,
    ): WidgetCredentialPresentation {
        val selection = WidgetMetricRanker.rank(
            providerId = credential.providerId,
            metrics = snapshot?.metrics.orEmpty(),
            fixedMetricId = null,
            capacity = METRIC_SCAN_CAPACITY,
        )
        val isBalanceOnly = selection.primary?.presentation == UsageMetricPresentation.Balance
        val visibleMetrics = if (isBalanceOnly) {
            listOfNotNull(selection.primary)
        } else {
            selection.orderedMetrics
                .filter { it.presentation == UsageMetricPresentation.Progress }
                .ifEmpty { selection.orderedMetrics }
                .take(VISIBLE_METRIC_CAPACITY)
        }
        return WidgetCredentialPresentation(
            id = credential.id,
            providerId = credential.providerId.rawValue,
            providerName = ProviderCatalog.displayName(credential.providerId),
            name = credential.name,
            planName = snapshot?.planName.orEmpty(),
            isStale = false,
            primary = visibleMetrics.firstOrNull()?.let { metricDisplay(it, credential.providerId) },
            secondary = visibleMetrics.drop(1).map { metricDisplay(it, credential.providerId) },
            fixedMetricUnavailable = selection.fixedMetricUnavailable,
            isBalanceOnly = isBalanceOnly,
            status = WidgetPresentationStateResolver.resolve(
                isEnabled = credential.isEnabled,
                hasSnapshot = snapshot != null,
                isStale = false,
                isRefreshing = false,
                failure = null,
                metricCount = snapshot?.metrics?.size ?: 0,
                fixedMetricUnavailable = selection.fixedMetricUnavailable,
            ),
        )
    }

    private fun riskScore(snapshot: UsageSnapshot?): Int {
        if (snapshot == null) return 1
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Unavailable }) return 0
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Critical }) return 2
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Warning }) return 3
        return 4
    }

    private fun metricDisplay(
        metric: UsageMetric,
        providerId: ProviderId,
    ): WidgetMetricPresentation {
        val percent = progressPercent(metric)
        val resetAt = metric.windowEnd?.atZone(ZoneId.systemDefault())
        return WidgetMetricPresentation(
            id = metric.id,
            label = displayLabel(metric),
            valueText = valueText(metric, percent),
            detailText = detailText(metric, providerId),
            resetText = resetAt?.let { "重置 " + RESET_CLOCK_FORMAT.format(it) },
            resetClockText = resetAt?.let(RESET_CLOCK_FORMAT::format),
            progressPercent = percent?.roundToInt()?.coerceIn(0, 100),
            severity = severity(metric, percent),
        )
    }

    private fun valueText(metric: UsageMetric, percent: Double?): String = when (metric.presentation) {
        UsageMetricPresentation.Progress -> percent?.let { "${it.roundToInt()}%" } ?: "-"
        UsageMetricPresentation.Balance -> currency(metric.value, metric.currencyCode)
        UsageMetricPresentation.Status -> if (metric.healthState == UsageMetricHealthState.Unavailable) {
            "不可用"
        } else {
            "可用"
        }
        UsageMetricPresentation.Value -> when (metric.unit) {
            UsageMetricUnit.Currency -> currency(metric.value, metric.currencyCode)
            UsageMetricUnit.Request -> metric.value?.let { grouped(it) + " 次" } ?: "-"
            UsageMetricUnit.Boolean_ -> if (metric.value == BigDecimal.ZERO) "否" else "是"
            UsageMetricUnit.Token, UsageMetricUnit.Text -> metric.value?.let(::grouped) ?: "-"
        }
    }

    private fun detailText(metric: UsageMetric, providerId: ProviderId): String = when (metric.presentation) {
        UsageMetricPresentation.Progress -> {
            if (providerId == ProviderId.Glm) {
                ""
            } else {
                val used = metric.used
                val limit = metric.limit
                if (used == null || limit == null) "" else "已用 ${plain(used)} / ${plain(limit)}"
            }
        }
        else -> ""
    }

    private fun displayLabel(metric: UsageMetric): String = when {
        metric.id == "five-hour" || metric.id == "fiveHour" -> "5h"
        metric.id == "weekly" -> "周"
        metric.id == "monthly" -> "月"
        metric.label.contains("5 小时") || metric.label.contains("5小时") -> "5h"
        metric.label.contains("每周") || metric.label.contains("周用量") -> "周"
        metric.label.contains("每月") || metric.label.contains("月用量") -> "月"
        else -> metric.label
    }

    private fun progressPercent(metric: UsageMetric): Double? {
        val limit = metric.limit ?: return null
        if (limit.compareTo(BigDecimal.ZERO) == 0) return null
        val amount = when (metric.semantic) {
            UsageMetricSemantic.UsedQuota -> metric.used
            UsageMetricSemantic.RemainingQuota -> metric.remaining
            else -> null
        } ?: return null
        return amount.divide(limit, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toDouble()
    }

    private fun severity(metric: UsageMetric, percent: Double?): WidgetMetricSeverity = when {
        metric.healthState == UsageMetricHealthState.Unavailable -> WidgetMetricSeverity.Unavailable
        metric.healthState == UsageMetricHealthState.Critical || (percent ?: 0.0) >= 95 ->
            WidgetMetricSeverity.Critical
        metric.healthState == UsageMetricHealthState.Warning || (percent ?: 0.0) >= 80 ->
            WidgetMetricSeverity.Warning
        else -> WidgetMetricSeverity.Normal
    }

    private fun currency(value: BigDecimal?, code: String?): String {
        val amount = value?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "-"
        return when (code?.uppercase()) {
            "CNY", "RMB", "¥" -> "¥$amount"
            "USD", "$" -> "\$$amount"
            "EUR", "€" -> "€$amount"
            null -> amount
            else -> "$amount $code"
        }
    }

    private fun grouped(value: BigDecimal): String {
        val format = DecimalFormat("#,##0.##")
        format.isGroupingUsed = true
        return format.format(value)
    }

    private fun plain(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    /** 单凭证聚焦环最多三个；扫描额度更大的候选集再按进度过滤。 */
    private const val VISIBLE_METRIC_CAPACITY = 3
    private const val METRIC_SCAN_CAPACITY = 6

    private val RESET_CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
}
