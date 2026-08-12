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
        let alias = UsageFormatter.menuBarText(
            state: selectedState,
            dimension: settings.displayDimension,
            style: settings.menuBarStyle
        )

        if let metric = verticalMetric {
            Image(nsImage: MenuBarAliasVerticalUsageIcon.image(alias: alias, percent: metric.percent))
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(verticalBarAccessibilityLabel(metric: metric))
        } else {
            Text(alias)
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
    static let size = NSSize(width: 7, height: 18)

    static func image(percent: Double) -> NSImage {
        let image = NSImage(size: size)
        image.lockFocus()
        defer { image.unlockFocus() }

        draw(
            percent: percent,
            in: NSRect(origin: .zero, size: size)
        )
        image.isTemplate = false
        return image
    }

    static func draw(percent: Double, in trackRect: NSRect) {
        let track = NSBezierPath(
            roundedRect: trackRect,
            xRadius: trackRect.width / 2,
            yRadius: trackRect.width / 2
        )
        NSColor.secondaryLabelColor.withAlphaComponent(0.25).setFill()
        track.fill()

        let height = trackRect.height * clampedPercent(percent) / 100
        guard height > 0 else {
            return
        }

        NSGraphicsContext.saveGraphicsState()
        track.addClip()
        color(for: percent).setFill()
        NSBezierPath(
            rect: NSRect(
                x: trackRect.minX,
                y: trackRect.minY,
                width: trackRect.width,
                height: height
            )
        )
        .fill()
        NSGraphicsContext.restoreGraphicsState()
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

@MainActor
enum MenuBarAliasVerticalUsageIcon {
    private static let textFont = NSFont.systemFont(ofSize: NSFont.systemFontSize)
    private static let barGap: CGFloat = 5

    static func image(alias: String, percent: Double) -> NSImage {
        let representation = DynamicMenuBarAliasVerticalUsageImageRep(
            alias: alias,
            percent: percent,
            fontSize: textFont.pointSize,
            barGap: barGap
        )
        let image = NSImage(size: representation.size)
        image.addRepresentation(representation)
        image.isTemplate = false
        return image
    }
}

/// 在状态栏实际绘制时再解析系统前景色，避免将文字颜色烘焙进静态位图。
private final class DynamicMenuBarAliasVerticalUsageImageRep: NSImageRep, @unchecked Sendable {
    private let text: String
    private let percent: Double
    private let fontSize: CGFloat
    private let textSize: NSSize
    private let barGap: CGFloat

    init(alias: String, percent: Double, fontSize: CGFloat, barGap: CGFloat) {
        self.text = alias + " · "
        self.percent = percent
        self.fontSize = fontSize
        self.textSize = (alias + " · " as NSString).size(withAttributes: [.font: NSFont.systemFont(ofSize: fontSize)])
        self.barGap = barGap
        super.init()
        size = NSSize(
            width: ceil(textSize.width) + barGap + MenuBarVerticalUsageIcon.size.width,
            height: MenuBarVerticalUsageIcon.size.height
        )
    }

    required convenience init?(pasteboardPropertyList propertyList: Any, ofType type: NSPasteboard.PasteboardType) {
        self.init(alias: "", percent: 0, fontSize: NSFont.systemFontSize, barGap: 5)
    }

    required init(coder: NSCoder) {
        text = " · "
        percent = 0
        fontSize = NSFont.systemFontSize
        textSize = (text as NSString).size(withAttributes: [.font: NSFont.systemFont(ofSize: fontSize)])
        barGap = 5
        super.init()
        size = NSSize(
            width: ceil(textSize.width) + barGap + MenuBarVerticalUsageIcon.size.width,
            height: MenuBarVerticalUsageIcon.size.height
        )
    }

    override func draw(
        in rect: NSRect,
        from fromRect: NSRect,
        operation op: NSCompositingOperation,
        fraction delta: CGFloat,
        respectFlipped respectContextIsFlipped: Bool,
        hints: [NSImageRep.HintKey: Any]? = nil
    ) -> Bool {
        guard let context = NSGraphicsContext.current?.cgContext else {
            return false
        }
        context.saveGState()
        defer { context.restoreGState() }

        context.translateBy(x: rect.minX, y: rect.minY)
        context.scaleBy(x: rect.width / size.width, y: rect.height / size.height)

        (text as NSString).draw(
            at: NSPoint(x: 0, y: floor((size.height - textSize.height) / 2)),
            withAttributes: [
                .font: NSFont.systemFont(ofSize: fontSize),
                // 在状态栏控件的绘制上下文中解析，随菜单栏背景自动选择黑/白前景色。
                .foregroundColor: NSColor.labelColor,
            ]
        )
        MenuBarVerticalUsageIcon.draw(
            percent: percent,
            in: NSRect(
                x: ceil(textSize.width) + barGap,
                y: 0,
                width: MenuBarVerticalUsageIcon.size.width,
                height: MenuBarVerticalUsageIcon.size.height
            )
        )
        return true
    }
}
