import AppKit
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
                Image(nsImage: MenuBarVerticalUsageIcon.image(percent: metric.percent))
                    .frame(width: 7, height: 18)
                    .accessibilityHidden(true)
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

enum MenuBarVerticalUsageIcon {
    private static let size = NSSize(width: 7, height: 18)

    static func image(percent: Double) -> NSImage {
        let image = NSImage(size: size)
        image.lockFocus()
        defer { image.unlockFocus() }

        let trackRect = NSRect(origin: .zero, size: size)
        let track = NSBezierPath(
            roundedRect: trackRect,
            xRadius: size.width / 2,
            yRadius: size.width / 2
        )
        NSColor.secondaryLabelColor.withAlphaComponent(0.25).setFill()
        track.fill()

        let height = size.height * clampedPercent(percent) / 100
        guard height > 0 else {
            image.isTemplate = false
            return image
        }

        NSGraphicsContext.saveGraphicsState()
        track.addClip()
        color(for: percent).setFill()
        NSBezierPath(rect: NSRect(x: 0, y: 0, width: size.width, height: height)).fill()
        NSGraphicsContext.restoreGraphicsState()
        image.isTemplate = false
        return image
    }

    private static func clampedPercent(_ percent: Double) -> Double {
        guard percent.isFinite else {
            return 0
        }
        return min(max(percent, 0), 100)
    }

    private static func color(for percent: Double) -> NSColor {
        switch MenuBarUsageRisk.level(for: percent) {
        case .normal:
            return .systemGreen
        case .warning:
            return .systemOrange
        case .critical:
            return .systemRed
        }
    }
}
