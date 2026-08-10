import Foundation

enum UsageKind: String, Codable, Equatable, Sendable {
    case periodic
    case tokenPack
}

enum UsageUnit: String, Codable, Equatable, Sendable {
    case usd
    case token
}

enum UsageDimension: String, Codable, Equatable, Sendable {
    case fiveHour
    case weekly
    case token
}

enum DisplayDimension: String, Codable, Equatable, Sendable {
    case fiveHour
    case weekly
}

struct UsageMetric: Codable, Equatable, Sendable {
    let used: Decimal
    let limit: Decimal
    let remaining: Decimal
    let percent: Double
    let unit: UsageUnit
    let windowEnd: Date?
}

struct UsageSnapshot: Codable, Equatable, Sendable {
    let planName: String
    let kind: UsageKind
    let fiveHour: UsageMetric?
    let weekly: UsageMetric?
    let token: UsageMetric?
    let allowedModels: [String]
    let fetchedAt: Date
}
