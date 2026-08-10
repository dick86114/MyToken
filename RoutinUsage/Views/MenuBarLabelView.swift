import SwiftUI

@MainActor
struct MenuBarLabelView: View {
    let store: UsageStore
    let settings: AppSettings

    var body: some View {
        labelContent
            .help(helpText)
    }
}

private extension MenuBarLabelView {
    @ViewBuilder
    var labelContent: some View {
        let text = UsageFormatter.menuBarText(
            state: selectedState,
            dimension: settings.displayDimension,
            style: settings.menuBarStyle
        )

        if let metric = verticalMetric {
            HStack(spacing: 4) {
                Text(text)
                VerticalUsageBar(percent: metric.percent)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(verticalBarAccessibilityLabel(metric: metric))
        } else {
            Text(text)
        }
    }

    var selectedState: KeyUsageState? {
        store.selectedKeyID.flatMap(store.state(for:))
    }

    var verticalMetric: UsageMetric? {
        MenuBarVerticalUsage.metric(
            state: selectedState,
            dimension: settings.displayDimension,
            style: settings.menuBarStyle
        )
    }

    func verticalBarAccessibilityLabel(metric: UsageMetric) -> String {
        let alias = selectedState?.configuration.displayName ?? ""
        let percent = UsageFormatter.percentText(metric) ?? ""
        return "\(alias)，已使用 \(percent)"
    }

    var helpText: String {
        guard let state = selectedState else {
            return "尚未配置 Key"
        }
        var parts = [state.configuration.displayName]
        if let lastSuccessAt = state.lastSuccessAt {
            parts.append("最后更新 \(lastSuccessAt.formatted(date: .omitted, time: .shortened))")
        } else {
            parts.append("尚未更新")
        }
        if state.isStale {
            parts.append("缓存已过期")
        }
        return parts.joined(separator: " · ")
    }
}

enum MenuBarVerticalUsage {
    static func metric(
        state: KeyUsageState?,
        dimension: DisplayDimension,
        style: MenuBarStyle
    ) -> UsageMetric? {
        guard style == .aliasVerticalBar,
              let snapshot = state?.snapshot,
              snapshot.kind == .periodic,
              let metric = UsageFormatter.metric(in: snapshot, dimension: dimension),
              UsageFormatter.percentText(metric) != nil else {
            return nil
        }
        return metric
    }
}

enum MenuBarUsageRisk: Equatable {
    case normal
    case warning
    case critical

    static func level(for percent: Double) -> Self {
        if percent >= 95 {
            return .critical
        }
        if percent >= 80 {
            return .warning
        }
        return .normal
    }
}

struct VerticalUsageBar: View {
    let percent: Double

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .bottom) {
                Capsule()
                    .fill(Color.secondary.opacity(0.25))
                Capsule()
                    .fill(fillColor)
                    .frame(height: geometry.size.height * clampedPercent / 100)
            }
        }
        .frame(width: 7, height: 18)
        .accessibilityHidden(true)
    }
}

private extension VerticalUsageBar {
    var clampedPercent: Double {
        guard percent.isFinite else {
            return 0
        }
        return min(max(percent, 0), 100)
    }

    var fillColor: Color {
        switch MenuBarUsageRisk.level(for: percent) {
        case .normal:
            return .green
        case .warning:
            return .orange
        case .critical:
            return .red
        }
    }
}
