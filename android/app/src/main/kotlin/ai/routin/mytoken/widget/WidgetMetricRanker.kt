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
        when (providerId) {
            ProviderId.Routin -> orderedByPriority(metrics, listOf("five-hour", "weekly", "token"))
            ProviderId.DeepSeek -> orderedByPriority(
                metrics,
                listOf("balance", "grantedBalance", "toppedUpBalance", "availability"),
            )
            ProviderId.Glm -> orderedByPriority(
                metrics,
                listOf("five-hour", "weekly", "model-calls", "zcode-mcp"),
            )
            ProviderId.NewAPI -> orderedByPriority(
                metrics,
                listOf(
                    "quota-progress",
                    "today-token",
                    "one-day-token",
                    "seven-day-token",
                    "thirty-day-token",
                    "rpm",
                    "tpm",
                    "request-count",
                ),
            )
            ProviderId.CommandCode -> orderedByPriority(
                metrics,
                listOf(
                    "five-hour",
                    "weekly",
                    "credit-progress",
                    "request-count",
                    "purchased-remaining",
                    "free-remaining",
                ),
            )
            ProviderId.OpenCode -> orderedByPriority(metrics, listOf("five-hour", "weekly"))
            ProviderId.Volcengine, ProviderId.Xiaomi, null -> metrics
        }

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
