import AppKit

enum MenuBarVerticalUsage {
    static func metric(
        state: KeyUsageState?,
        dimension: DisplayDimension,
        style: MenuBarStyle
    ) -> UsageMetric? {
        guard (style == .aliasLogoProgress || style == .logoProgress || style == .aliasVerticalBar),
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
        if percent >= 80 {
            return .critical
        }
        if percent >= 50 {
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

enum MenuBarLogoUsageIcon {
    static let size = NSSize(width: 18, height: 18)

    static func image(percent: Double) -> NSImage {
        let image = NSImage(size: size)
        image.lockFocus()
        defer { image.unlockFocus() }

        let rect = NSRect(origin: .zero, size: size)
        draw(percent: percent, in: rect)
        image.isTemplate = false
        return image
    }

    static func draw(percent: Double, in rect: NSRect) {
        guard
            let outline = NSImage(named: "MenuBarLogoOutline"),
            let mask = NSImage(named: "MenuBarLogoMask")
        else {
            return
        }

        let height = rect.height * clampedPercent(percent) / 100
        if height > 0 {
            NSGraphicsContext.saveGraphicsState()
            color(for: percent).setFill()
            NSBezierPath(
                rect: NSRect(
                    x: rect.minX,
                    y: rect.minY,
                    width: rect.width,
                    height: height
                )
            )
            .fill()
            mask.draw(in: rect, from: .zero, operation: .destinationIn, fraction: 1)
            NSGraphicsContext.restoreGraphicsState()
        }

        let tintedOutline = NSImage(size: rect.size)
        tintedOutline.lockFocus()
        NSColor.labelColor.setFill()
        NSBezierPath(rect: NSRect(origin: .zero, size: rect.size)).fill()
        outline.draw(
            in: NSRect(origin: .zero, size: rect.size),
            from: .zero,
            operation: .destinationIn,
            fraction: 1
        )
        tintedOutline.unlockFocus()
        tintedOutline.draw(in: rect, from: .zero, operation: .sourceOver, fraction: 1)
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
