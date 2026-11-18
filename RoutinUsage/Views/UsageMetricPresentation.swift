import SwiftUI

enum UsageMetricTone: Equatable {
    case normal
    case warning
    case critical
}

/// 量指标在不同页面的排版层级；弹窗完整视图采用设计稿的紧凑规格。
enum UsageMetricsVisualStyle: Equatable {
    case standard
    case popover

    var progressLabelSize: CGFloat {
        switch self {
        case .standard: return 11
        case .popover: return 11
        }
    }

    var valueLabelSize: CGFloat {
        switch self {
        case .standard: return 12
        case .popover: return 11
        }
    }

    var valueSize: CGFloat {
        switch self {
        case .standard: return 14
        case .popover: return 12
        }
    }

    var detailSize: CGFloat {
        switch self {
        case .standard: return 10.5
        case .popover: return 11
        }
    }

    var progressHeight: CGFloat {
        switch self {
        case .standard: return 3
        case .popover: return 2
        }
    }

    var columnSpacing: CGFloat {
        switch self {
        case .standard: return 16
        case .popover: return 7
        }
    }

    var rowSpacing: CGFloat {
        switch self {
        case .standard: return 14
        case .popover: return 10
        }
    }

    var balanceAmountSize: CGFloat {
        switch self {
        case .standard: return 20
        case .popover: return 16
        }
    }

    var compactValueLabelSize: CGFloat {
        switch self {
        case .standard: return 10
        case .popover: return 10
        }
    }

    var compactValueSize: CGFloat {
        switch self {
        case .standard: return 14
        case .popover: return 11
        }
    }
}

enum UsageMetricPresentation {
    static func tone(for percent: Double, rules: MenuBarColorRules) -> UsageMetricTone {
        switch MenuBarUsageRisk.level(for: percent, rules: rules) {
        case .normal:
            return .normal
        case .warning:
            return .warning
        case .critical:
            return .critical
        }
    }

    static func color(
        for percent: Double,
        scheme: ColorScheme,
        rules: MenuBarColorRules
    ) -> Color {
        Color(nsColor: rules.color(for: MenuBarUsageRisk.level(for: percent, rules: rules)))
    }

    static func color(for tone: UsageMetricTone, rules: MenuBarColorRules) -> Color {
        let level: MenuBarUsageRisk
        switch tone {
        case .normal: level = .normal
        case .warning: level = .warning
        case .critical: level = .critical
        }
        return Color(nsColor: rules.color(for: level))
    }
    static func clampedPercent(_ percent: Double) -> Double {
        guard percent.isFinite else {
            return 0
        }
        return min(max(percent, 0), 100)
    }

    static func tone(for percent: Double) -> UsageMetricTone {
        switch MenuBarUsageRisk.level(for: percent) {
        case .normal:
            return .normal
        case .warning:
            return .warning
        case .critical:
            return .critical
        }
    }

    static func color(for percent: Double, scheme: ColorScheme) -> Color {
        switch tone(for: percent) {
        case .normal:
            return CompactPopoverPalette.brand(scheme)
        case .warning:
            return CompactPopoverPalette.warningColor
        case .critical:
            return CompactPopoverPalette.criticalColor
        }
    }
}

struct UsageMetricProgressBar: View {
    @Environment(\.menuBarColorRules) private var menuBarColorRules
    @Environment(\.colorScheme) private var colorScheme
    let percent: Double
    let height: CGFloat

    init(percent: Double, height: CGFloat = 6) {
        self.percent = percent
        self.height = height
    }

    init(metric: UsageMetric, height: CGFloat = 6) {
        percent = metric.percent
        self.height = height
    }

    var body: some View {
        GeometryReader { geometry in
            let clampedPercent = UsageMetricPresentation.clampedPercent(percent)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.primary.opacity(0.12))
                Capsule()
                    .fill(CompactPopoverPalette.statusColor(
                        for: UsageMetricPresentation.tone(
                            for: percent,
                            rules: menuBarColorRules
                        ),
                        colorScheme
                    ))
                    .frame(
                        width: max(
                            clampedPercent == 0 ? 0 : 2,
                            geometry.size.width * clampedPercent / 100
                        )
                    )
            }
        }
        .frame(height: height)
        .accessibilityHidden(true)
    }
}

private struct MenuBarColorRulesKey: EnvironmentKey {
    static let defaultValue: MenuBarColorRules = .standard
}

extension EnvironmentValues {
    var menuBarColorRules: MenuBarColorRules {
        get { self[MenuBarColorRulesKey.self] }
        set { self[MenuBarColorRulesKey.self] = newValue }
    }
}

enum ProviderTheme {
    static func accentColor(for providerID: ProviderID) -> Color {
        switch providerID {
        case .routin:
            return .blue
        case .deepseek:
            return .indigo
        case .glm:
            return .green
        case .volcengine:
            return .orange
        case .newAPI:
            return .purple
        case .commandCode:
            return .teal
        case .xiaomi:
            return .pink
        case .opencode:
            return Color(red: 0.80, green: 0.91, blue: 0.20)
        }
    }

    static func background(for providerID: ProviderID) -> Color {
        accentColor(for: providerID).opacity(0.10)
    }

    static func borderColor(for providerID: ProviderID) -> Color {
        accentColor(for: providerID).opacity(0.18)
    }
}
