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
    val resetClockText: String? = null,
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
    val status: WidgetPresentationStatus = WidgetPresentationStatus.Ready,
    /** 只有余额的凭证不画进度，直接在组件里展示余额文本。 */
    val isBalanceOnly: Boolean = false,
) {
    val orderedMetrics: List<WidgetMetricPresentation> = listOfNotNull(primary) + secondary
}

data class WidgetPresentation(
    val style: WidgetDisplayStyle,
    val size: WidgetSizeKind,
    val credentials: List<WidgetCredentialPresentation>,
) {
    val credential: WidgetCredentialPresentation? get() = credentials.firstOrNull()
}

internal fun WidgetSizeKind.overviewCapacity(): Int = when (this) {
    WidgetSizeKind.Compact -> 1
    WidgetSizeKind.Standard -> 2
    WidgetSizeKind.Expanded -> 4
}
