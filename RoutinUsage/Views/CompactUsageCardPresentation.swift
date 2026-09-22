import Foundation

/// 简洁卡片的纯展示规则：环形图排布、重置文案和余额徽章。
enum CompactUsageCardPresentation {
    enum Arrangement: Equatable {
        case balance
        case horizontalGauges
        case verticalGauges
        case valueTiles
    }

    static func avatarLetter(displayName: String, providerID: ProviderID) -> String {
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        if let first = trimmed.first {
            return String(first)
        }
        let code = ProviderRegistry.builtInDescriptors.first(where: { $0.id == providerID })?.shortCode
        return String(code?.prefix(1) ?? "M")
    }

    static func arrangement(for metrics: [NormalizedUsageMetric]) -> Arrangement {
        if metrics.count == 1, metrics[0].presentation == .balance {
            return .balance
        }
        let allProgress = !metrics.isEmpty && metrics.allSatisfy { $0.presentation == .progress }
        if allProgress, metrics.count <= 2 {
            return .horizontalGauges
        }
        if allProgress, metrics.count == 3 {
            return .verticalGauges
        }
        return .valueTiles
    }

    static func isNearlyExhausted(metrics: [NormalizedUsageMetric]) -> Bool {
        metrics.contains { metric in
            guard metric.presentation == .progress else { return false }
            if metric.healthState == .critical {
                return true
            }
            guard let percent = metric.displayedPercent else {
                return false
            }
            return UsageMetricPresentation.tone(for: percent) == .critical
        }
    }

    static func compactPercentText(_ value: Double?) -> String {
        UsageFormatter.displayPercentText(value)
    }

    /// 环形图内只取数字部分，百分号由视图单独缩小排版。
    static func compactPercentNumberText(_ value: Double?) -> String {
        let text = compactPercentText(value)
        return text.hasSuffix("%") ? String(text.dropLast()) : text
    }

    static func subtitle(
        for metric: NormalizedUsageMetric,
        now: Date,
        style: CompactGaugeStyle,
        timeZone: TimeZone = .current
    ) -> String {
        if metric.presentation != .progress {
            return ""
        }

        let tone = metric.displayedPercent.map(UsageMetricPresentation.tone(for:))
        if style == .vertical {
            if tone == .critical || metric.healthState == .critical {
                return "即将耗尽"
            }
            if tone == .warning || metric.healthState == .warning {
                return "用量偏高"
            }
        }

        guard let windowEnd = metric.windowEnd else {
            let percent = metric.displayedPercent ?? 0
            return percent <= 0 ? "待命中" : "--"
        }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let reset = UsageFormatter.resetTime(windowEnd, now: now, timeZone: timeZone)
        if calendar.isDate(windowEnd, inSameDayAs: now) {
            return "重置 \(reset)"
        }
        return reset
    }

    static func balanceStatusText(healthState: UsageMetricHealthState) -> String {
        switch healthState {
        case .normal, .unknown, .stale:
            return "充足"
        case .warning:
            return "偏低"
        case .critical, .unavailable:
            return "不足"
        }
    }

    static func gaugeTone(
        for metric: NormalizedUsageMetric
    ) -> UsageMetricTone? {
        guard metric.presentation == .progress else { return nil }
        if let percent = metric.displayedPercent {
            return UsageMetricPresentation.tone(for: percent)
        }
        switch metric.healthState {
        case .warning:
            return .warning
        case .critical, .unavailable:
            return .critical
        default:
            return .normal
        }
    }
}

enum CompactGaugeStyle: Equatable {
    case horizontal
    case vertical
}
