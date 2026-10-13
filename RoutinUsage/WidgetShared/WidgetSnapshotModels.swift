import Foundation

enum WidgetSnapshotMetricUnit: String, Codable, Equatable, Sendable {
    case token
    case currency
    case request
    case boolean
    case text
}

enum WidgetSnapshotMetricPresentation: String, Codable, Equatable, Sendable {
    case progress
    case balance
    case status
    case value
}

enum WidgetSnapshotMetricSemantic: String, Codable, Equatable, Sendable {
    case usedQuota
    case remainingQuota
    case balance
    case status
    case value
}

enum WidgetSnapshotMetricHealthState: String, Codable, Equatable, Sendable {
    case normal
    case warning
    case critical
    case unavailable
    case stale
    case unknown
}

struct WidgetSnapshotMetric: Codable, Equatable, Sendable, Identifiable {
    let id: String
    let label: String
    let used: Decimal?
    let limit: Decimal?
    let remaining: Decimal?
    let value: Decimal?
    let unit: WidgetSnapshotMetricUnit
    let windowStart: Date?
    let windowEnd: Date?
    let presentation: WidgetSnapshotMetricPresentation
    let semantic: WidgetSnapshotMetricSemantic
    let currencyCode: String?
    let healthState: WidgetSnapshotMetricHealthState

    init(
        id: String,
        label: String,
        used: Decimal? = nil,
        limit: Decimal? = nil,
        remaining: Decimal? = nil,
        value: Decimal? = nil,
        unit: WidgetSnapshotMetricUnit,
        windowStart: Date? = nil,
        windowEnd: Date? = nil,
        presentation: WidgetSnapshotMetricPresentation,
        semantic: WidgetSnapshotMetricSemantic,
        currencyCode: String? = nil,
        healthState: WidgetSnapshotMetricHealthState = .unknown
    ) {
        self.id = id
        self.label = label
        self.used = used
        self.limit = limit
        self.remaining = remaining
        self.value = value
        self.unit = unit
        self.windowStart = windowStart
        self.windowEnd = windowEnd
        self.presentation = presentation
        self.semantic = semantic
        self.currencyCode = currencyCode
        self.healthState = healthState
    }

    var displayedPercent: Double? {
        guard let limit, limit > 0 else { return nil }
        let amount: Decimal
        switch semantic {
        case .usedQuota:
            guard let used else { return nil }
            amount = used
        case .remainingQuota:
            guard let remaining else { return nil }
            amount = remaining
        case .balance, .status, .value:
            return nil
        }
        return NSDecimalNumber(decimal: amount)
            .dividing(by: NSDecimalNumber(decimal: limit))
            .multiplying(by: 100)
            .doubleValue
    }

    private enum CodingKeys: String, CodingKey {
        case id
        case label
        case used
        case limit
        case remaining
        case value
        case unit
        case windowStart
        case windowEnd
        case presentation
        case semantic
        case currencyCode
        case healthState
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        label = try container.decode(String.self, forKey: .label)
        used = try container.decodeIfPresent(Decimal.self, forKey: .used)
        limit = try container.decodeIfPresent(Decimal.self, forKey: .limit)
        remaining = try container.decodeIfPresent(Decimal.self, forKey: .remaining)
        value = try container.decodeIfPresent(Decimal.self, forKey: .value)
        unit = try container.decode(WidgetSnapshotMetricUnit.self, forKey: .unit)
        windowStart = try container.decodeIfPresent(Date.self, forKey: .windowStart)
        windowEnd = try container.decodeIfPresent(Date.self, forKey: .windowEnd)
        presentation = try container.decode(WidgetSnapshotMetricPresentation.self, forKey: .presentation)
        semantic = try container.decode(WidgetSnapshotMetricSemantic.self, forKey: .semantic)
        currencyCode = try container.decodeIfPresent(String.self, forKey: .currencyCode)
        healthState = try container.decodeIfPresent(
            WidgetSnapshotMetricHealthState.self,
            forKey: .healthState
        ) ?? .unknown
    }
}

struct WidgetSnapshotCredential: Codable, Equatable, Sendable, Identifiable {
    let id: UUID
    let providerID: String
    let providerName: String
    let name: String
    let planName: String
    let isEnabled: Bool
    let sortOrder: Int
    let fetchedAt: Date?
    let lastSuccessAt: Date?
    let isStale: Bool
    let subscriptionEndAt: Date?
    let metrics: [WidgetSnapshotMetric]
}

struct WidgetSnapshotBundle: Codable, Equatable, Sendable {
    let schemaVersion: Int
    let generatedAt: Date
    let credentials: [WidgetSnapshotCredential]

    static let currentSchemaVersion = 1
}

enum WidgetDisplayStyle: String, Codable, Equatable, CaseIterable, Sendable {
    case auto
    case focusRing
    case scanBand
    case signalTiles
}

enum WidgetSizeKind: String, Codable, Equatable, CaseIterable, Sendable {
    case compact
    case standard
    case expanded

    var credentialMetricCapacity: Int {
        switch self {
        case .compact: return 2
        case .standard: return 4
        case .expanded: return 6
        }
    }

    var overviewCredentialCapacity: Int {
        switch self {
        case .compact: return 1
        case .standard: return 3
        case .expanded: return 6
        }
    }
}

enum WidgetMetricSeverity: String, Equatable, Sendable {
    case normal
    case warning
    case critical
    case unavailable
}

struct WidgetMetricPresentation: Equatable, Sendable, Identifiable {
    let id: String
    let label: String
    let valueText: String
    let detailText: String
    let resetText: String?
    let progressPercent: Int?
    let severity: WidgetMetricSeverity
}

struct WidgetCredentialPresentation: Equatable, Sendable, Identifiable {
    let id: UUID
    let providerID: String
    let providerName: String
    let name: String
    let planName: String
    let fetchedAt: Date?
    let isStale: Bool
    let primary: WidgetMetricPresentation?
    let secondary: [WidgetMetricPresentation]
    let fixedMetricUnavailable: Bool

    var orderedMetrics: [WidgetMetricPresentation] {
        [primary].compactMap { $0 } + secondary
    }
}

struct WidgetPresentation: Equatable, Sendable {
    let style: WidgetDisplayStyle
    let size: WidgetSizeKind
    let credentials: [WidgetCredentialPresentation]
}

struct WidgetMetricSelection: Equatable, Sendable {
    let primary: WidgetSnapshotMetric?
    let secondary: [WidgetSnapshotMetric]
    let fixedMetricUnavailable: Bool

    var orderedMetrics: [WidgetSnapshotMetric] {
        [primary].compactMap { $0 } + secondary
    }
}
