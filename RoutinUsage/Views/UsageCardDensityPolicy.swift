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
        }
    }
}
