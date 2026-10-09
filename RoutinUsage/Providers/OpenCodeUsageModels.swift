import Foundation

/// OpenCode Console Go 订阅状态。该接口目前属于 Console 内部 API。
struct OpenCodeGoStatus: Decodable, Sendable {
    let subscriberUserId: String?
    let product: String
    let renewalProduct: String?
    let cancelAtPeriodEnd: Bool
    let renewalPending: Bool?
    let renewalAuthorizationRequired: Bool?
    let renewalRetryAt: String?
    let renewalStopReason: String?
    let access: OpenCodeGoAccess?

    enum CodingKeys: String, CodingKey {
        case subscriberUserId
        case product
        case renewalProduct
        case cancelAtPeriodEnd
        case renewalPending
        case renewalAuthorizationRequired
        case renewalRetryAt
        case renewalStopReason
        case access
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        subscriberUserId = try container.decodeIfPresent(String.self, forKey: .subscriberUserId)
        product = try container.decode(String.self, forKey: .product)
        renewalProduct = try container.decodeIfPresent(String.self, forKey: .renewalProduct)
        cancelAtPeriodEnd = try container.decodeIfPresent(Bool.self, forKey: .cancelAtPeriodEnd) ?? false
        renewalPending = try container.decodeIfPresent(Bool.self, forKey: .renewalPending)
        renewalAuthorizationRequired = try container.decodeIfPresent(
            Bool.self,
            forKey: .renewalAuthorizationRequired
        )
        renewalRetryAt = try container.decodeIfPresent(String.self, forKey: .renewalRetryAt)
        renewalStopReason = try container.decodeIfPresent(String.self, forKey: .renewalStopReason)
        access = try container.decodeIfPresent(OpenCodeGoAccess.self, forKey: .access)
    }
}

struct OpenCodeGoAccess: Decodable, Sendable {
    let startsAt: String?
    let endsAt: String?
    let meters: OpenCodeGoMeters?
}

struct OpenCodeGoMeters: Decodable, Sendable {
    let fiveHour: OpenCodeGoMeter?
    let week: OpenCodeGoMeter?
    let month: OpenCodeGoMeter?

    enum CodingKeys: String, CodingKey {
        case fiveHour
        case week
        case month
    }
}

struct OpenCodeGoMeter: Decodable, Sendable {
    let startsAt: String?
    let resetsAt: String?
    let limitMicroCents: OpenCodeMicroCents
    let usedMicroCents: OpenCodeMicroCents
}

struct OpenCodeMicroCents: Decodable, Sendable {
    let value: Decimal

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let rawValue = try? container.decode(String.self), let parsed = Decimal(string: rawValue) {
            value = parsed
        } else {
            value = try container.decode(Decimal.self)
        }
    }

    var dollars: Decimal {
        value / 100_000_000
    }
}
