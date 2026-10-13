package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.UsageMetric
import java.util.UUID

enum class WidgetMetricSeverity {
    Normal,
    Warning,
    Critical,
    Unavailable,
}

data class WidgetMetricPresentation(
    val id: String,
    val label: String,
    val valueText: String,
    val detailText: String = "",
    val resetText: String? = null,
    val progressPercent: Int? = null,
    val severity: WidgetMetricSeverity = WidgetMetricSeverity.Normal,
)

data class WidgetMetricSelection(
    val primary: UsageMetric?,
    val secondary: List<UsageMetric>,
    val fixedMetricUnavailable: Boolean,
) {
    val orderedMetrics: List<UsageMetric> = listOfNotNull(primary) + secondary
}

data class WidgetCredentialPresentation(
    val id: UUID,
    val providerId: String,
    val providerName: String,
    val name: String,
    val planName: String,
    val isStale: Boolean,
    val primary: WidgetMetricPresentation?,
    val secondary: List<WidgetMetricPresentation>,
    val fixedMetricUnavailable: Boolean,
) {
    val orderedMetrics: List<WidgetMetricPresentation> = listOfNotNull(primary) + secondary
}

data class WidgetPresentation(
    val style: WidgetDisplayStyle,
    val size: WidgetSizeKind,
    val credential: WidgetCredentialPresentation?,
)

internal fun WidgetSizeKind.metricCapacity(): Int = when (this) {
    WidgetSizeKind.Compact -> 2
    WidgetSizeKind.Standard -> 4
    WidgetSizeKind.Expanded -> 6
}

internal fun WidgetSizeKind.overviewCapacity(): Int = when (this) {
    WidgetSizeKind.Compact -> 1
    WidgetSizeKind.Standard -> 3
    WidgetSizeKind.Expanded -> 6
}
