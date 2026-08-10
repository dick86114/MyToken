import Foundation

enum UsageFormatter {
    static func menuBarText(
        state: KeyUsageState?,
        dimension: DisplayDimension
    ) -> String {
        menuBarText(state: state, dimension: dimension, style: .percent)
    }

    static func menuBarText(
        state: KeyUsageState?,
        dimension: DisplayDimension,
        style: MenuBarStyle
    ) -> String {
        guard let state else {
            return "--"
        }
        guard let snapshot = state.snapshot else {
            if state.error == .noSubscription {
                return "--"
            }
            if state.error != nil {
                return "!"
            }
            return "…"
        }
        guard let metric = metric(in: snapshot, dimension: dimension) else {
            return "--"
        }
        guard let percent = percentText(metric) else {
            return "!"
        }
        if snapshot.kind == .tokenPack {
            return percent
        }
        switch style {
        case .percent:
            return percent
        case .aliasPercent:
            return "\(truncatedMenuBarAlias(state.configuration.displayName)) \(percent)"
        case .aliasVerticalBar:
            return truncatedMenuBarAlias(state.configuration.displayName)
        }
    }

    static func truncatedMenuBarAlias(_ alias: String) -> String {
        guard alias.count > 5 else {
            return alias
        }
        return String(alias.prefix(5)) + "…"
    }

    static func amount(_ metric: UsageMetric) -> String {
        "\(formatted(metric.used, unit: metric.unit)) / "
            + formatted(metric.limit, unit: metric.unit)
    }

    static func remaining(_ metric: UsageMetric) -> String {
        formatted(metric.remaining, unit: metric.unit)
    }

    static func fullAmount(_ metric: UsageMetric) -> String {
        guard metric.unit == .token else {
            return amount(metric)
        }
        return "\(fullToken(metric.used)) / \(fullToken(metric.limit)) Token"
    }

    static func resetTime(_ metric: UsageMetric) -> String {
        guard let windowEnd = metric.windowEnd else {
            return "--"
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: windowEnd)
    }

    /// 用于设置页和详情面板的完整本地时间；没有时间时统一显示占位符。
    static func fullDateTime(_ date: Date?) -> String {
        guard let date else {
            return "—"
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: date)
    }

    /// 以完整日期时间显示窗口结束时刻，便于跨天查看。
    static func windowEndDescription(_ metric: UsageMetric) -> String? {
        guard let windowEnd = metric.windowEnd else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: windowEnd)
    }

    static func metric(
        in snapshot: UsageSnapshot,
        dimension: DisplayDimension
    ) -> UsageMetric? {
        if snapshot.kind == .tokenPack {
            return snapshot.token
        }
        switch dimension {
        case .fiveHour:
            return snapshot.fiveHour
        case .weekly:
            return snapshot.weekly
        }
    }

    static func percentText(_ metric: UsageMetric) -> String? {
        guard metric.percent.isFinite,
              let roundedPercent = Int(exactly: metric.percent.rounded()) else {
            return nil
        }
        return "\(roundedPercent)%"
    }

    static func statusText(
        state: KeyUsageState,
        dimension: DisplayDimension? = nil
    ) -> String {
        if let dimension,
           let snapshot = state.snapshot,
           let metric = metric(in: snapshot, dimension: dimension),
           percentText(metric) == nil {
            return "用量数据异常"
        }
        if state.isStale, state.snapshot != nil {
            guard let error = state.error else {
                return "缓存已过期"
            }
            return "缓存已过期，\(errorText(error))"
        }
        if state.isRefreshing, state.snapshot == nil {
            return "正在加载"
        }
        if let error = state.error {
            return errorText(error)
        }
        return state.snapshot == nil ? "等待首次刷新" : "用量数据可用"
    }
}

private extension UsageFormatter {
    static let stableLocale = Locale(identifier: "en_US_POSIX")

    static func formatted(_ value: Decimal, unit: UsageUnit) -> String {
        switch unit {
        case .usd:
            return String(
                format: "$%.2f",
                locale: stableLocale,
                NSDecimalNumber(decimal: value).doubleValue
            )
        case .token:
            return compactToken(value)
        }
    }

    static func compactToken(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value).doubleValue
        let absolute = abs(number)
        let scaled: Double
        let suffix: String
        if absolute >= 1_000_000_000 {
            scaled = number / 1_000_000_000
            suffix = "B"
        } else if absolute >= 1_000_000 {
            scaled = number / 1_000_000
            suffix = "M"
        } else if absolute >= 1_000 {
            scaled = number / 1_000
            suffix = "K"
        } else {
            scaled = number
            suffix = ""
        }
        let text = String(format: "%.1f", locale: stableLocale, scaled)
        return text.replacingOccurrences(of: ".0", with: "") + suffix
    }

    static func fullToken(_ value: Decimal) -> String {
        let formatter = NumberFormatter()
        formatter.locale = stableLocale
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = true
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 3
        return formatter.string(from: NSDecimalNumber(decimal: value))
            ?? NSDecimalNumber(decimal: value).stringValue
    }

    static func errorText(_ error: UsageDisplayError) -> String {
        switch error {
        case .noSubscription:
            return "当前没有可用订阅"
        case .invalidKey:
            return "Key 无效"
        case .network:
            return "网络错误，请刷新重试"
        case .invalidResponse:
            return "接口数据异常，请刷新重试"
        case let .server(statusCode):
            return "服务器错误（\(statusCode)），请稍后重试"
        }
    }
}
