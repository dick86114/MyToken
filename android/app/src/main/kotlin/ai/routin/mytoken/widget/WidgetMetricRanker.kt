package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric

object WidgetMetricRanker {
    fun rank(
        providerId: ProviderId?,
        metrics: List<UsageMetric>,
        fixedMetricId: String?,
        capacity: Int,
    ): WidgetMetricSelection {
        val resolvedCapacity = capacity.coerceAtLeast(1)
        val ordered = orderedMetrics(providerId, metrics)
        val fixedMetric = fixedMetricId?.let { id -> ordered.firstOrNull { it.id == id } }
        val primary = fixedMetric ?: ordered.firstOrNull()
        val candidates = ordered.filterNot { it.id == primary?.id }
        val secondary = semanticDeduplicated(candidates, metrics.mapTo(mutableSetOf()) { it.id })
            .take(resolvedCapacity - 1)
        return WidgetMetricSelection(
            primary = primary,
            secondary = secondary,
            fixedMetricUnavailable = fixedMetricId != null && fixedMetric == null,
        )
    }

    private fun orderedMetrics(providerId: ProviderId?, metrics: List<UsageMetric>): List<UsageMetric> =
        orderedByPriority(
            if (providerId == ProviderId.Glm) {
                metrics.filter {
                    it.presentation == ai.routin.mytoken.domain.model.UsageMetricPresentation.Progress
                }
            } else {
                metrics
            },
            WINDOW_PRIORITY + providerPriority(providerId),
        )

    private fun providerPriority(providerId: ProviderId?): List<String> =
        when (providerId) {
            ProviderId.Routin -> listOf("token")
            ProviderId.DeepSeek -> listOf("balance", "grantedBalance", "toppedUpBalance", "availability")
            ProviderId.Glm -> emptyList()
            ProviderId.NewAPI -> listOf(
                    "quota-progress",
                    "today-token",
                    "one-day-token",
                    "seven-day-token",
                    "thirty-day-token",
                    "rpm",
                    "tpm",
                    "request-count",
                )
            ProviderId.CommandCode -> listOf(
                    "credit-progress",
                    "request-count",
                    "purchased-remaining",
                    "free-remaining",
                )
            ProviderId.OpenCode -> emptyList()
            ProviderId.Volcengine, ProviderId.Xiaomi, null -> emptyList()
        }

    private val WINDOW_PRIORITY = listOf(
        "five-hour",
        "fiveHour",
        "weekly",
        "7-day",
        "monthly",
        "month",
    )

    private fun orderedByPriority(
        metrics: List<UsageMetric>,
        priority: List<String>,
    ): List<UsageMetric> {
        val byId = metrics.associateBy(UsageMetric::id)
        val result = priority.mapNotNull(byId::get).toMutableList()
        val selected = result.mapTo(mutableSetOf()) { it.id }
        result += metrics.filterNot { it.id in selected }
        return result
    }

    private fun semanticDeduplicated(
        metrics: List<UsageMetric>,
        knownIds: Set<String>,
    ): List<UsageMetric> = metrics.filter { metric ->
        if (!metric.id.endsWith("-cost")) {
            true
        } else {
            metric.id.removeSuffix("-cost") !in knownIds
        }
    }
}
