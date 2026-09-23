import SwiftUI

enum UsageMetricTone: Equatable {
    case normal
    case warning
    case critical
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

    init(percent: Double) {
        self.percent = percent
    }

    init(metric: UsageMetric) {
        percent = metric.percent
    }

    var body: some View {
        GeometryReader { geometry in
            let clampedPercent = UsageMetricPresentation.clampedPercent(percent)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.primary.opacity(0.12))
                Capsule()
                    .fill(
                        UsageMetricPresentation.color(
                            for: percent,
                            scheme: colorScheme,
                            rules: menuBarColorRules
                        )
                    )
                    .frame(
                        width: max(
                            clampedPercent == 0 ? 0 : 2,
                            geometry.size.width * clampedPercent / 100
                        )
                    )
            }
        }
        .frame(height: 6)
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
        }
    }

    static func background(for providerID: ProviderID) -> Color {
        accentColor(for: providerID).opacity(0.10)
    }

    static func borderColor(for providerID: ProviderID) -> Color {
        accentColor(for: providerID).opacity(0.18)
    }
}
