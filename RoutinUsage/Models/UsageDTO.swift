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
    let groupNames: UsageKeyedValues<String>?
    let groupMultipliers: UsageKeyedValues<Decimal>?

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
        self.groupNames = groupNames.map(UsageKeyedValues.init)
        self.groupMultipliers = groupMultipliers.map(UsageKeyedValues.init)
    }
}

/// 兼容旧接口的数组形式和新接口按分组 key 返回的对象形式。
struct UsageKeyedValues<Value: Decodable & Sendable>: Decodable, Sendable {
    let keys: [String]
    let values: [String: Value]

    init(_ array: [Value]) {
        let keys = array.indices.map(String.init)
        self.init(
            keys: keys,
            values: Dictionary(uniqueKeysWithValues: zip(keys, array))
        )
    }

    private init(keys: [String], values: [String: Value]) {
        self.keys = keys
        self.values = values
    }

    init(from decoder: Decoder) throws {
        if var unkeyed = try? decoder.unkeyedContainer() {
            var decoded: [Value] = []
            while !unkeyed.isAtEnd {
                decoded.append(try unkeyed.decode(Value.self))
            }
            self.init(decoded)
            return
        }

        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        // JSON 对象键的枚举顺序未定义；按 key 排序保证菜单和测试结果稳定。
        let keys = container.allKeys.map(\.stringValue).sorted()
        var values: [String: Value] = [:]
        for key in container.allKeys {
            values[key.stringValue] = try container.decode(Value.self, forKey: key)
        }
        self.init(keys: keys, values: values)
    }
}

private struct DynamicCodingKey: CodingKey {
    let stringValue: String
    let intValue: Int?

    init?(stringValue: String) {
        self.stringValue = stringValue
        intValue = Int(stringValue)
    }

    init?(intValue: Int) {
        stringValue = String(intValue)
        self.intValue = intValue
    }
}
