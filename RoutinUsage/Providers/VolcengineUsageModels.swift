import Foundation

struct VolcengineUsageWindow: Decodable, Sendable {
    let quota: Decimal
    let used: Decimal
    let resetTime: Date?
    let subscribeTime: Date?

    private enum CodingKeys: String, CodingKey {
        case quota = "Quota"
        case used = "Used"
        case resetTime = "ResetTime"
        case subscribeTime = "SubscribeTime"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        quota = try Self.decimal(container, key: .quota)
        used = try Self.decimal(container, key: .used)
        resetTime = Self.date(try container.decodeIfPresent(Int64.self, forKey: .resetTime))
        subscribeTime = Self.date(try container.decodeIfPresent(Int64.self, forKey: .subscribeTime))
    }

    private static func decimal(_ container: KeyedDecodingContainer<CodingKeys>, key: CodingKeys) throws -> Decimal {
        if let value = try? container.decode(Decimal.self, forKey: key) { return value }
        if let value = try? container.decode(String.self, forKey: key), let decimal = Decimal(string: value) { return decimal }
        throw DecodingError.dataCorruptedError(forKey: key, in: container, debugDescription: "无法解析 AFP 数值")
    }

    private static func date(_ milliseconds: Int64?) -> Date? {
        guard let milliseconds else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1_000)
    }
}

struct VolcengineUsageResult: Decodable, Sendable {
    let afpDaily: VolcengineUsageWindow?
    let afpFiveHour: VolcengineUsageWindow?
    let afpMonthly: VolcengineUsageWindow?
    let afpWeekly: VolcengineUsageWindow?
    let planType: String?

    private enum CodingKeys: String, CodingKey {
        case afpDaily = "AFPDaily"
        case afpFiveHour = "AFPFiveHour"
        case afpMonthly = "AFPMonthly"
        case afpWeekly = "AFPWeekly"
        case planType = "PlanType"
    }
}

struct VolcengineUsageResponse: Decodable, Sendable {
    let result: VolcengineUsageResult?

    private enum CodingKeys: String, CodingKey {
        case result = "Result"
    }
}
