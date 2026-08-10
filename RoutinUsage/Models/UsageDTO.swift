import Foundation

struct UsageResponseDTO: Decodable, Sendable {
    let subscriptionId: String?
    let planId: String?
    let planName: String?
    let type: Int?
    let status: Int?
    let subscriptionStartAt: String?
    let subscriptionEndAt: String?
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
    /// 当前订阅分组的计费倍率；接口未返回时保持为空。
    let groupMultiplier: Decimal?

    init(
        subscriptionId: String? = nil,
        planId: String? = nil,
        planName: String?,
        type: Int?,
        status: Int? = nil,
        subscriptionStartAt: String? = nil,
        subscriptionEndAt: String? = nil,
        dailyLimitUsd: Decimal?, weeklyLimitUsd: Decimal?,
        dailyUsedUsd: Decimal?, weeklyUsedUsd: Decimal?,
        dailyRemainingUsd: Decimal?, weeklyRemainingUsd: Decimal?,
        dayWindowEndAt: String?, weekWindowEndAt: String?,
        totalTokens: Decimal?, consumedTokens: Decimal?, remainingTokens: Decimal?,
        allowedModels: [String]?, groupMultiplier: Decimal? = nil
    ) {
        self.subscriptionId = subscriptionId
        self.planId = planId
        self.planName = planName
        self.type = type
        self.status = status
        self.subscriptionStartAt = subscriptionStartAt
        self.subscriptionEndAt = subscriptionEndAt
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
        self.groupMultiplier = groupMultiplier
    }
}
