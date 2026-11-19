import Foundation

/// 简洁卡片的纯展示规则：环形图排布、重置文案和余额徽章。
enum CompactUsageCardPresentation {
    enum Arrangement: Equatable {
        case balance
        case horizontalGauges
        case verticalGauges
        case valueTiles
    }

    static func arrangement(for metrics: [NormalizedUsageMetric]) -> Arrangement {
        if metrics.count == 1, metrics[0].presentation == .balance {
            return .balance
        }
        let allProgress = !metrics.isEmpty && metrics.allSatisfy { $0.presentation == .progress }
        if allProgress, metrics.count == 1 {
            return .horizontalGauges
        }
        if allProgress, metrics.count <= 3 {
            return .verticalGauges
        }
        return .valueTiles
    }

    static func isNearlyExhausted(
        metrics: [NormalizedUsageMetric],
        rules: MenuBarColorRules = .standard
    ) -> Bool {
        metrics.contains { metric in
            guard metric.presentation == .progress else { return false }
            if metric.healthState == .critical {
                return true
            }
            guard let percent = metric.displayedPercent else {
                return false
            }
            return UsageMetricPresentation.tone(for: percent, rules: rules) == .critical
        }
    }

    static func nearlyExhaustedRemainingText(
        metrics: [NormalizedUsageMetric],
        now: Date
    ) -> String? {
        guard isNearlyExhausted(metrics: metrics) else { return nil }

        let criticalMetric = metrics.first { metric in
            metric.presentation == .progress &&
                metric.windowEnd != nil &&
                gaugeTone(for: metric) == .critical
        }

        guard let windowEnd = criticalMetric?.windowEnd else { return nil }
        return "剩余 \(UsageFormatter.remainingDurationText(until: windowEnd, now: now))"
    }

    static func compactPercentText(_ value: Double?) -> String {
        UsageFormatter.displayPercentText(value)
    }

    /// 简洁卡片只展示周期本身，统一为 5小时、周、月。
    static func compactPeriodLabel(for metric: NormalizedUsageMetric) -> String {
        let identifier = metric.id.lowercased()
        let label = metric.label
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "　", with: "")

        if identifier.contains("five")
            || identifier.contains("5h")
            || label.contains("5小时") {
            return "5小时"
        }
        if identifier.contains("week") || label.contains("周") {
            return "周"
        }
        if identifier.contains("month") || label.contains("月") {
            return "月"
        }
        return metric.label
    }

    /// 环形图内只取数字部分，百分号由视图单独缩小排版。
    static func compactPercentNumberText(_ value: Double?) -> String {
        let text = compactPercentText(value)
        return text.hasSuffix("%") ? String(text.dropLast()) : text
    }

    static func subtitle(
        for metric: NormalizedUsageMetric,
        now: Date,
        timeZone: TimeZone = .current
    ) -> String {
        if metric.presentation != .progress {
            return ""
        }

        guard let windowEnd = metric.windowEnd else {
            let percent = metric.displayedPercent ?? 0
            return percent <= 0 ? "待命中" : "--"
        }

        let interval = windowEnd.timeIntervalSince(now)
        guard interval > 0 else {
            return "等待刷新"
        }

        let totalMinutes = max(1, Int(interval / 60))
        if totalMinutes < 60 {
            return "\(totalMinutes) 分后重置"
        }

        let totalHours = totalMinutes / 60
        if totalHours < 24 {
            let minutes = totalMinutes % 60
            return minutes == 0
                ? "\(totalHours) 小时后重置"
                : "\(totalHours) 小时 \(minutes) 分后重置"
        }

        let days = totalHours / 24
        if days < 7 {
            let hours = totalHours % 24
            return hours == 0
                ? "\(days) 天后重置"
                : "\(days) 天 \(hours) 小时后重置"
        }
        return "\(UsageFormatter.resetTime(windowEnd, now: now, timeZone: timeZone)) 后重置"
    }

    static func singleMetricSubtitle(
        for metric: NormalizedUsageMetric,
        now: Date,
        timeZone: TimeZone = .current
    ) -> String {
        guard metric.presentation == .progress else {
            return subtitle(for: metric, now: now, timeZone: timeZone)
        }

        let remainingText = compactRemainingText(for: metric)
        guard let remainingText else {
            return subtitle(for: metric, now: now, timeZone: timeZone)
        }

        guard let windowEnd = metric.windowEnd else {
            return "剩余 \(remainingText)"
        }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = timeZone
        formatter.dateFormat = calendar.isDate(windowEnd, inSameDayAs: now)
            ? "今天"
            : "MM/dd"
        return "剩余 \(remainingText) · \(formatter.string(from: windowEnd)) 到期"
    }

    private static func compactRemainingText(for metric: NormalizedUsageMetric) -> String? {
        guard let remaining = metric.remaining else { return nil }
        switch metric.unit {
        case .currency:
            return UsageFormatter.currencyText(remaining, currencyCode: metric.currencyCode)
        case .token:
            return UsageFormatter.compactMetricValue(remaining)
        case .request, .boolean, .text:
            return UsageFormatter.numberText(remaining, grouping: true)
        }
    }

    static func gaugeTone(
        for metric: NormalizedUsageMetric,
        rules: MenuBarColorRules = .standard
    ) -> UsageMetricTone? {
        guard metric.presentation == .progress else { return nil }
        if let percent = metric.displayedPercent {
            return UsageMetricPresentation.tone(for: percent, rules: rules)
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

enum UsageResetCardPresentation {
    static func isVisible(cards: [UsageResetCard]) -> Bool {
        !cards.isEmpty
    }

    static func badgeText(cards: [UsageResetCard]) -> String {
        "\(cards.count)"
    }

    static func detailText(cards: [UsageResetCard]) -> String {
        guard let earliest = cards.map(\.expiresAt).min() else {
            return "\(cards.count) 张重置卡"
        }
        return "\(cards.count) 张 · 最早 \(earliestExpiryText(earliest, now: .now))"
    }

    static func detailText(
        cards: [UsageResetCard],
        now: Date,
        timeZone: TimeZone = .current
    ) -> String {
        guard let earliest = cards.map(\.expiresAt).min() else {
            return "\(cards.count) 张重置卡"
        }
        return "\(cards.count) 张 · 最早 \(earliestExpiryText(earliest, now: now, timeZone: timeZone))"
    }

    static func accessibilityText(cards: [UsageResetCard]) -> String {
        guard let earliest = cards.map(\.expiresAt).min() else {
            return "\(cards.count) 张重置卡"
        }
        return "\(cards.count) 张重置卡，最早 \(UsageFormatter.fullDateTime(earliest))"
    }

    static func earliestExpiryText(
        _ date: Date,
        now: Date,
        timeZone: TimeZone = .current
    ) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = timeZone
        if calendar.isDate(date, inSameDayAs: now) {
            formatter.dateFormat = "今天 HH:mm"
        } else {
            formatter.dateFormat = "MM/dd HH:mm"
        }
        return formatter.string(from: date)
    }
}

enum CompactGaugeStyle: Equatable {
    case horizontal
    case vertical
}
