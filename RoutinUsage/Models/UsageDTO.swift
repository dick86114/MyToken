import Foundation

struct UsageResponseDTO: Decodable, Sendable {
    let subscriptionId: String?
    let planId: String?
    let planName: String?
    let type: Int?
    let status: Int?
    let startAt: String?
    let endAt: String?
    let dailyLimitUsd: Decimal?
    let weeklyLimitUsd: Decimal?
    let dailyUsedUsd: Decimal?
    let weeklyUsedUsd: Decimal?
    let dailyRemainingUsd: Decimal?
    let weeklyRemainingUsd: Decimal?
    let dayWindowEndAt: String?
    let weekWindowEndAt: String?
    let totalTokens: Decimal?
    let consumedTokens: Decimal?
    let remainingTokens: Decimal?
    let allowedModels: [String]?
    let groupNames: [String]?
    let groupMultipliers: [Decimal]?

    init(
        subscriptionId: String? = nil,
        planId: String? = nil,
        planName: String?,
        type: Int?,
        status: Int? = nil,
        startAt: String? = nil,
        endAt: String? = nil,
        dailyLimitUsd: Decimal?, weeklyLimitUsd: Decimal?,
        dailyUsedUsd: Decimal?, weeklyUsedUsd: Decimal?,
        dailyRemainingUsd: Decimal?, weeklyRemainingUsd: Decimal?,
        dayWindowEndAt: String?, weekWindowEndAt: String?,
        totalTokens: Decimal?, consumedTokens: Decimal?, remainingTokens: Decimal?,
        allowedModels: [String]?,
        groupNames: [String]? = nil,
        groupMultipliers: [Decimal]? = nil
    ) {
        self.subscriptionId = subscriptionId
        self.planId = planId
        self.planName = planName
        self.type = type
        self.status = status
        self.startAt = startAt
        self.endAt = endAt
        self.dailyLimitUsd = dailyLimitUsd
        self.weeklyLimitUsd = weeklyLimitUsd
        self.dailyUsedUsd = dailyUsedUsd
        self.weeklyUsedUsd = weeklyUsedUsd
        self.dailyRemainingUsd = dailyRemainingUsd
        self.weeklyRemainingUsd = weeklyRemainingUsd
        self.dayWindowEndAt = dayWindowEndAt
        self.weekWindowEndAt = weekWindowEndAt
        self.totalTokens = totalTokens
        self.consumedTokens = consumedTokens
        self.remainingTokens = remainingTokens
        self.allowedModels = allowedModels
        self.groupNames = groupNames
        self.groupMultipliers = groupMultipliers
    }
}
