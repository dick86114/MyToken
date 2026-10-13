import Foundation

enum WidgetPresentationBuilder {
    static func makeCredentialPresentation(
        credential: WidgetSnapshotCredential,
        style: WidgetDisplayStyle,
        size: WidgetSizeKind,
        fixedMetricID: String? = nil
    ) -> WidgetPresentation {
        let resolvedStyle = resolveStyle(
            style,
            size: size,
            component: .credential
        )
        return WidgetPresentation(
            style: resolvedStyle,
            size: size,
            credentials: [
                makeCredential(
                    credential,
                    capacity: size.credentialMetricCapacity,
                    fixedMetricID: fixedMetricID
                )
            ]
        )
    }

    static func makeOverviewPresentation(
        credentials: [WidgetSnapshotCredential],
        style: WidgetDisplayStyle,
        size: WidgetSizeKind
    ) -> WidgetPresentation {
        let resolvedStyle = resolveStyle(
            style,
            size: size,
            component: .overview
        )
        let visibleCredentials = credentials
            .filter(\.isEnabled)
            .sorted(by: overviewSort)
            .prefix(size.overviewCredentialCapacity)
        return WidgetPresentation(
            style: resolvedStyle,
            size: size,
            credentials: visibleCredentials.map {
                makeCredential($0, capacity: size.credentialMetricCapacity)
            }
        )
    }

    private enum Component {
        case credential
        case overview
    }

    private static func resolveStyle(
        _ style: WidgetDisplayStyle,
        size: WidgetSizeKind,
        component: Component
    ) -> WidgetDisplayStyle {
        guard style == .auto else { return style }
        if component == .overview { return .scanBand }
        switch size {
        case .compact, .standard: return .focusRing
        case .expanded: return .signalTiles
        }
    }

    private static func makeCredential(
        _ credential: WidgetSnapshotCredential,
        capacity: Int,
        fixedMetricID: String? = nil
    ) -> WidgetCredentialPresentation {
        let selection = WidgetMetricRanker.rank(
            providerID: credential.providerID,
            metrics: credential.metrics,
            fixedMetricID: fixedMetricID,
            capacity: capacity
        )
        return WidgetCredentialPresentation(
            id: credential.id,
            providerID: credential.providerID,
            providerName: credential.providerName,
            name: credential.name,
            planName: credential.planName,
            fetchedAt: credential.fetchedAt,
            isStale: credential.isStale,
            primary: selection.primary.map(WidgetMetricFormatter.makePresentation),
            secondary: selection.secondary.map(WidgetMetricFormatter.makePresentation),
            fixedMetricUnavailable: selection.fixedMetricUnavailable
        )
    }

    private static func overviewSort(
        _ lhs: WidgetSnapshotCredential,
        _ rhs: WidgetSnapshotCredential
    ) -> Bool {
        let lhsRisk = overviewRisk(lhs)
        let rhsRisk = overviewRisk(rhs)
        if lhsRisk != rhsRisk { return lhsRisk < rhsRisk }
        return lhs.sortOrder < rhs.sortOrder
    }

    private static func overviewRisk(_ credential: WidgetSnapshotCredential) -> Int {
        if credential.isStale { return 3 }
        if credential.metrics.contains(where: { $0.healthState == .unavailable }) { return 1 }
        if credential.metrics.contains(where: { $0.healthState == .critical }) { return 2 }
        if credential.metrics.contains(where: { $0.healthState == .warning }) { return 4 }
        return 5
    }
}

private enum WidgetMetricFormatter {
    static func makePresentation(_ metric: WidgetSnapshotMetric) -> WidgetMetricPresentation {
        WidgetMetricPresentation(
            id: metric.id,
            label: metric.label,
            valueText: valueText(metric),
            detailText: detailText(metric),
            resetText: metric.windowEnd.map { "重置 \(shortTime($0))" },
            progressPercent: progressPercent(metric),
            severity: severity(metric)
        )
    }

    private static func valueText(_ metric: WidgetSnapshotMetric) -> String {
        switch metric.presentation {
        case .progress:
            guard let percent = metric.displayedPercent else { return "-" }
            return "\(Int(percent.rounded()))%"
        case .balance:
            return currencyText(metric.value, currencyCode: metric.currencyCode)
        case .status:
            return metric.healthState == .unavailable ? "不可用" : "可用"
        case .value:
            switch metric.unit {
            case .currency:
                return currencyText(metric.value, currencyCode: metric.currencyCode)
            case .request:
                return metric.value.map { grouped($0) + " 次" } ?? "-"
            case .boolean:
                return metric.value == 0 ? "否" : "是"
            case .token, .text:
                return metric.value.map(grouped) ?? "-"
            }
        }
    }

    private static func detailText(_ metric: WidgetSnapshotMetric) -> String {
        switch metric.presentation {
        case .progress:
            guard let used = metric.used, let limit = metric.limit else { return "" }
            return "已用 \(plain(used)) / \(plain(limit))"
        case .balance, .status, .value:
            return ""
        }
    }

    private static func progressPercent(_ metric: WidgetSnapshotMetric) -> Int? {
        guard metric.presentation == .progress, let percent = metric.displayedPercent else {
            return nil
        }
        return Int(percent.rounded()).clamped(to: 0...100)
    }

    private static func severity(_ metric: WidgetSnapshotMetric) -> WidgetMetricSeverity {
        if metric.healthState == .unavailable { return .unavailable }
        if metric.healthState == .critical { return .critical }
        if metric.healthState == .warning { return .warning }
        if let percent = metric.displayedPercent {
            if percent >= 95 { return .critical }
            if percent >= 80 { return .warning }
        }
        return .normal
    }

    private static func currencyText(_ value: Decimal?, currencyCode: String?) -> String {
        guard let value else { return "-" }
        let amount = decimalString(value, minimumFractionDigits: 2, maximumFractionDigits: 2)
        switch currencyCode?.uppercased() {
        case "CNY", "RMB", "¥": return "¥\(amount)"
        case "USD", "$": return "$\(amount)"
        case "EUR", "€": return "€\(amount)"
        case let code?: return "\(amount) \(code)"
        case nil: return amount
        }
    }

    private static func grouped(_ value: Decimal) -> String {
        decimalString(value, minimumFractionDigits: 0, maximumFractionDigits: 2)
    }

    private static func plain(_ value: Decimal) -> String {
        decimalString(value, minimumFractionDigits: 0, maximumFractionDigits: 2)
    }

    private static func decimalString(
        _ value: Decimal,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int
    ) -> String {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = true
        formatter.groupingSeparator = ","
        formatter.decimalSeparator = "."
        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits
        return formatter.string(from: NSDecimalNumber(decimal: value)) ?? "\(value)"
    }

    private static func shortTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
