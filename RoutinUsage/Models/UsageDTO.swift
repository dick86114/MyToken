import Foundation

struct UsageResponseDTO: Decodable, Sendable {
    let planName: String?
    let type: Int?
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
}
