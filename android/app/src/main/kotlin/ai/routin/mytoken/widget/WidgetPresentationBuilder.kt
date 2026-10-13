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
    fun makeCredentialPresentation(
        credential: Credential,
        snapshot: UsageSnapshot?,
        style: WidgetDisplayStyle,
        size: WidgetSizeKind,
        fixedMetricId: String? = null,
    ): WidgetPresentation = WidgetPresentation(
        style = resolveStyle(style, size, overview = false),
        size = size,
        credential = credentialPresentation(
            credential = credential,
            snapshot = snapshot,
            capacity = size.metricCapacity(),
            fixedMetricId = fixedMetricId,
        ),
    )

    fun makeOverviewPresentation(
        entries: List<Pair<Credential, UsageSnapshot?>>,
        style: WidgetDisplayStyle,
        size: WidgetSizeKind,
    ): WidgetPresentation = WidgetPresentation(
        style = resolveStyle(style, size, overview = true),
        size = size,
        credential = entries
            .filter { it.first.isEnabled }
            .sortedWith(compareBy({ riskScore(it.second) }, { it.first.sortOrder }))
            .take(size.overviewCapacity())
            .firstOrNull()
            ?.let { (credential, snapshot) ->
                credentialPresentation(
                    credential = credential,
                    snapshot = snapshot,
                    capacity = size.metricCapacity(),
                    fixedMetricId = null,
                )
            },
    )

    private fun credentialPresentation(
        credential: Credential,
        snapshot: UsageSnapshot?,
        capacity: Int,
        fixedMetricId: String?,
    ): WidgetCredentialPresentation {
        val selection = WidgetMetricRanker.rank(
            providerId = credential.providerId,
            metrics = snapshot?.metrics.orEmpty(),
            fixedMetricId = fixedMetricId,
            capacity = capacity,
        )
        return WidgetCredentialPresentation(
            id = credential.id,
            providerId = credential.providerId.rawValue,
            providerName = ProviderCatalog.displayName(credential.providerId),
            name = credential.name,
            planName = snapshot?.planName.orEmpty(),
            isStale = false,
            primary = selection.primary?.let(::metricDisplay),
            secondary = selection.secondary.map(::metricDisplay),
            fixedMetricUnavailable = selection.fixedMetricUnavailable,
        )
    }

    private fun resolveStyle(
        style: WidgetDisplayStyle,
        size: WidgetSizeKind,
        overview: Boolean,
    ): WidgetDisplayStyle {
        if (style != WidgetDisplayStyle.Auto) return style
        if (overview) return WidgetDisplayStyle.ScanBand
        return when (size) {
            WidgetSizeKind.Compact, WidgetSizeKind.Standard -> WidgetDisplayStyle.FocusRing
            WidgetSizeKind.Expanded -> WidgetDisplayStyle.SignalTiles
        }
    }

    private fun riskScore(snapshot: UsageSnapshot?): Int {
        if (snapshot == null) return 1
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Unavailable }) return 0
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Critical }) return 2
        if (snapshot.metrics.any { it.healthState == UsageMetricHealthState.Warning }) return 3
        return 4
    }

    private fun metricDisplay(metric: UsageMetric): WidgetMetricPresentation {
        val percent = progressPercent(metric)
        return WidgetMetricPresentation(
            id = metric.id,
            label = metric.label,
            valueText = valueText(metric, percent),
            detailText = detailText(metric),
            resetText = metric.windowEnd?.let {
                "重置 " + DateTimeFormatter.ofPattern("HH:mm")
                    .format(it.atZone(ZoneId.systemDefault()))
            },
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

    private fun detailText(metric: UsageMetric): String = when (metric.presentation) {
        UsageMetricPresentation.Progress -> {
            val used = metric.used
            val limit = metric.limit
            if (used == null || limit == null) "" else "已用 ${plain(used)} / ${plain(limit)}"
        }
        else -> ""
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
}
