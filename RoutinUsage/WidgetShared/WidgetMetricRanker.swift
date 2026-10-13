import Foundation

enum WidgetMetricRanker {
    static func rank(
        providerID: String,
        metrics: [WidgetSnapshotMetric],
        fixedMetricID: String?,
        capacity: Int
    ) -> WidgetMetricSelection {
        let resolvedCapacity = max(1, capacity)
        let ordered = orderedMetrics(providerID: providerID, metrics: metrics)
        let fixedMetric = fixedMetricID.flatMap { fixedID in
            ordered.first { $0.id == fixedID }
        }
        let primary = fixedMetric ?? ordered.first
        let primaryID = primary?.id
        let candidates = ordered.filter { $0.id != primaryID }
        let deduplicated = semanticDeduplicated(
            candidates,
            knownMetricIDs: Set(metrics.map(\.id))
        )
        let secondary = Array(deduplicated.prefix(resolvedCapacity - 1))

        return WidgetMetricSelection(
            primary: primary,
            secondary: secondary,
            fixedMetricUnavailable: fixedMetricID != nil && fixedMetric == nil
        )
    }

    private static func orderedMetrics(
        providerID: String,
        metrics: [WidgetSnapshotMetric]
    ) -> [WidgetSnapshotMetric] {
        switch providerID.lowercased() {
        case "routin":
            return orderedByPriority(metrics, priority: ["five-hour", "weekly", "token"])
        case "deepseek":
            return orderedByPriority(
                metrics,
                priority: ["balance", "grantedBalance", "toppedUpBalance", "availability"]
            )
        case "glm":
            return orderedByPriority(
                metrics,
                priority: ["five-hour", "weekly", "model-calls", "zcode-mcp"]
            )
        case "newapi":
            return orderedByPriority(
                metrics,
                priority: [
                    "quota-progress",
                    "today-token",
                    "one-day-token",
                    "seven-day-token",
                    "thirty-day-token",
                    "rpm",
                    "tpm",
                    "request-count"
                ]
            )
        case "commandcode":
            return orderedByPriority(
                metrics,
                priority: [
                    "five-hour",
                    "weekly",
                    "credit-progress",
                    "request-count",
                    "purchased-remaining",
                    "free-remaining"
                ]
            )
        case "opencode":
            return orderedByPriority(metrics, priority: ["five-hour", "weekly"])
        default:
            return metrics
        }
    }

    private static func orderedByPriority(
        _ metrics: [WidgetSnapshotMetric],
        priority: [String]
    ) -> [WidgetSnapshotMetric] {
        let byID = Dictionary(uniqueKeysWithValues: metrics.map { ($0.id, $0) })
        var result = priority.compactMap { byID[$0] }
        let selectedIDs = Set(result.map(\.id))
        result.append(contentsOf: metrics.filter { !selectedIDs.contains($0.id) })
        return result
    }

    private static func semanticDeduplicated(
        _ metrics: [WidgetSnapshotMetric],
        knownMetricIDs: Set<String>
    ) -> [WidgetSnapshotMetric] {
        metrics.filter { metric in
            guard metric.id.hasSuffix("-cost") else { return true }
            let baseID = String(metric.id.dropLast("-cost".count))
            return !knownMetricIDs.contains(baseID)
        }
    }
}
