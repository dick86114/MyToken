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

struct UsageGroupMultiplier: Codable, Equatable, Sendable {
    let name: String
    let multiplier: Decimal
}

struct UsageSnapshot: Codable, Equatable, Sendable {
    let subscriptionId: String?
    let planId: String?
    let planName: String
    let kind: UsageKind
    let fiveHour: UsageMetric?
    let weekly: UsageMetric?
    let token: UsageMetric?
    let allowedModels: [String]
    let fetchedAt: Date
    /// 当前订阅分组的计费倍率；资源包或旧缓存可能没有该数据。
    let groupMultiplier: Decimal?
    /// 按接口返回顺序配对的订阅分组和计费倍率；旧缓存缺少时为空。
    let groupMultipliers: [UsageGroupMultiplier]
    let status: Int?
    let subscriptionStartAt: Date?
    let subscriptionEndAt: Date?

    init(
        planName: String,
        subscriptionId: String? = nil,
        planId: String? = nil,
        kind: UsageKind,
        fiveHour: UsageMetric?,
        weekly: UsageMetric?,
        token: UsageMetric?,
        allowedModels: [String],
        fetchedAt: Date,
        groupMultiplier: Decimal? = nil,
        groupMultipliers: [UsageGroupMultiplier] = [],
        status: Int? = nil,
        subscriptionStartAt: Date? = nil,
        subscriptionEndAt: Date? = nil
    ) {
        self.subscriptionId = subscriptionId
        self.planId = planId
        self.planName = planName
        self.kind = kind
        self.fiveHour = fiveHour
        self.weekly = weekly
        self.token = token
        self.allowedModels = allowedModels
        self.fetchedAt = fetchedAt
        self.groupMultiplier = groupMultiplier
        self.groupMultipliers = groupMultipliers
        self.status = status
        self.subscriptionStartAt = subscriptionStartAt
        self.subscriptionEndAt = subscriptionEndAt
    }

    private enum CodingKeys: String, CodingKey {
        case planName
        case subscriptionId
        case planId
        case kind
        case fiveHour
        case weekly
        case token
        case allowedModels
        case fetchedAt
        case groupMultiplier
        case groupMultipliers
        case status
        case subscriptionStartAt
        case subscriptionEndAt
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        planName = try container.decode(String.self, forKey: .planName)
        subscriptionId = try container.decodeIfPresent(String.self, forKey: .subscriptionId)
        planId = try container.decodeIfPresent(String.self, forKey: .planId)
        kind = try container.decode(UsageKind.self, forKey: .kind)
        fiveHour = try container.decodeIfPresent(UsageMetric.self, forKey: .fiveHour)
        weekly = try container.decodeIfPresent(UsageMetric.self, forKey: .weekly)
        token = try container.decodeIfPresent(UsageMetric.self, forKey: .token)
        allowedModels = try container.decode([String].self, forKey: .allowedModels)
        fetchedAt = try container.decode(Date.self, forKey: .fetchedAt)
        groupMultiplier = try container.decodeIfPresent(Decimal.self, forKey: .groupMultiplier)
        groupMultipliers = try container.decodeIfPresent([UsageGroupMultiplier].self, forKey: .groupMultipliers) ?? []
        status = try container.decodeIfPresent(Int.self, forKey: .status)
        subscriptionStartAt = try container.decodeIfPresent(Date.self, forKey: .subscriptionStartAt)
        subscriptionEndAt = try container.decodeIfPresent(Date.self, forKey: .subscriptionEndAt)
    }
}
