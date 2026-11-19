import Foundation

struct UsageCardCompactSpec: Equatable {
    var metricIDs: [String]
    var showsResetTime: Bool
}

enum UsageCardDensityPolicy {
    static func compactSpec(
        providerID: ProviderID,
        metadata: [String: String]
    ) -> UsageCardCompactSpec {
        switch providerID {
        case .routin:
            return UsageCardCompactSpec(
                metricIDs: metadata["usageKind"] == "tokenPack" ? ["token"] : ["fiveHour", "weekly"],
                showsResetTime: true
            )
        case .deepseek:
            return UsageCardCompactSpec(metricIDs: ["balance"], showsResetTime: true)
        case .xiaomi:
            let isPlan = metadata["usageKind"] == "plan"
            return UsageCardCompactSpec(
                metricIDs: [isPlan ? "plan-total" : "account-balance"],
                showsResetTime: !isPlan
            )
        case .glm:
            return UsageCardCompactSpec(metricIDs: ["five-hour", "weekly"], showsResetTime: true)
        case .volcengine:
            return UsageCardCompactSpec(
                metricIDs: ["fiveHour", "weekly", "monthly"],
                showsResetTime: true
            )
        case .newAPI:
            return UsageCardCompactSpec(
                metricIDs: ["today-token", "one-day-token", "seven-day-token", "thirty-day-token"],
                showsResetTime: true
            )
        case .commandCode:
            return UsageCardCompactSpec(
                metricIDs: ["five-hour", "weekly", "credit-progress"],
                showsResetTime: true
            )
        case .opencode:
            return UsageCardCompactSpec(
                metricIDs: ["fiveHour", "weekly", "monthly"],
                showsResetTime: true
            )
        }
    }

    static func orderedMetrics(
        providerID: ProviderID,
        metadata: [String: String],
        metrics: [NormalizedUsageMetric]
    ) -> [NormalizedUsageMetric] {
        let byID = Dictionary(
            uniqueKeysWithValues: metrics.map { ($0.id, $0) }
        )
        let ordered = compactSpec(providerID: providerID, metadata: metadata)
            .metricIDs
            .compactMap { byID[$0] }
        if !ordered.isEmpty {
            return ordered
        }

        // 多币种账户的指标 id 带币种后缀（如 balance-CNY），
        // 简洁模式取首个余额型指标，避免整卡空白。
        if providerID == .deepseek {
            return metrics.filter { $0.presentation == .balance }.prefix(1).map { $0 }
        }
        return ordered
    }
}
