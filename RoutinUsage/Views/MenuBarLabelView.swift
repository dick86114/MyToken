import AppKit

struct MenuBarIndicatorModel: Equatable, Sendable {
    let shortCode: String
    let percent: Double?
    let healthState: UsageMetricHealthState
    let accessibilityLabel: String

    static func hoverSummary(for indicators: [MenuBarIndicatorModel]) -> String {
        indicators.map(\.accessibilityLabel).joined(separator: "\n")
    }

    static func make(
        state: KeyUsageState,
        descriptor: ProviderDescriptor,
        dimension: DisplayDimension
    ) -> Self {
        if let metric = state.snapshot?.metrics.first(where: { $0.presentation == .progress }),
           let percent = metric.displayedPercent {
            return Self(
                shortCode: descriptor.shortCode,
                percent: percent,
                healthState: metric.healthState == .unknown
                    ? MenuBarUsageRisk.healthState(for: percent)
                    : metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，\(metric.displaysRemainingPercent ? "剩余" : "已使用") \(Int(percent.rounded()))%"
            )
        }

        if let metric = state.snapshot?.metrics.first(where: { $0.presentation == .balance }) {
            let value = metric.value.map { NSDecimalNumber(decimal: $0).stringValue } ?? "未知"
            return Self(
                shortCode: descriptor.shortCode,
                percent: nil,
                healthState: metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，余额 \(value)"
            )
        }

        if let metric = state.snapshot?.metrics.first(where: { $0.presentation == .status }) {
            return Self(
                shortCode: descriptor.shortCode,
                percent: nil,
                healthState: metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，账户状态"
            )
        }

        if let snapshot = state.snapshot,
           let metric = UsageFormatter.metric(in: snapshot, dimension: dimension) {
            return Self(
                shortCode: descriptor.shortCode,
                percent: metric.percent,
                healthState: MenuBarUsageRisk.healthState(for: metric.percent),
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，已使用 \(Int(metric.percent.rounded()))%"
            )
        }

        return Self(
            shortCode: descriptor.shortCode,
            percent: nil,
            healthState: state.error == nil ? .unknown : .unavailable,
            accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，暂无用量数据"
        )
    }
}

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

    static func healthState(for percent: Double) -> UsageMetricHealthState {
        switch level(for: percent) {
        case .normal: return .normal
        case .warning: return .warning
        case .critical: return .critical
        }
    }
}

enum MenuBarMultiUsageIcon {
    static let maximumCount = 5
    static let unitWidth: CGFloat = 17
    static let gap: CGFloat = 3
    static let size = NSSize(width: unitWidth, height: 18)

    static func image(
        indicators: [MenuBarIndicatorModel],
        appearance: NSAppearance? = nil
    ) -> NSImage {
        let count = max(1, min(indicators.count, maximumCount))
        let image = NSImage(size: NSSize(width: unitWidth * CGFloat(count) + gap * CGFloat(count - 1), height: size.height))
        image.lockFocus()
        defer { image.unlockFocus() }
        let labelColor = foregroundColor(for: appearance ?? NSApp?.effectiveAppearance)
        for (index, indicator) in indicators.prefix(maximumCount).enumerated() {
            let x = CGFloat(index) * (unitWidth + gap)
            draw(
                indicator: indicator,
                in: NSRect(x: x, y: 0, width: unitWidth, height: size.height),
                foregroundColor: labelColor
            )
        }
        // 模板图会丢弃颜色；这里保留风险色，同时手动根据菜单栏深浅绘制文字。
        image.isTemplate = false
        return image
    }

    static func codeFont(for characterCount: Int) -> NSFont {
        let size: CGFloat = characterCount >= 3 ? 4.7 : 6
        return NSFont.monospacedSystemFont(ofSize: size, weight: .bold)
    }

    static func codeSlotHeight(for characterCount: Int) -> CGFloat {
        (size.height - 4) / CGFloat(max(1, characterCount))
    }

    static func codeBaselineY(
        characterCount: Int,
        index: Int,
        font: NSFont
    ) -> CGFloat {
        let slotHeight = codeSlotHeight(for: characterCount)
        let slotBottom = size.height - 2 - slotHeight * CGFloat(index + 1)
        return slotBottom + (slotHeight - font.capHeight) / 2
    }

    private static func draw(
        indicator: MenuBarIndicatorModel,
        in rect: NSRect,
        foregroundColor: NSColor
    ) {
        let text = indicator.shortCode
        let characters = Array(text.prefix(3))
        let font = codeFont(for: characters.count)
        let paragraph = NSMutableParagraphStyle()
        paragraph.alignment = .center
        let attributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: foregroundColor,
            .paragraphStyle: paragraph
        ]
        let textWidth: CGFloat = 9

        characters.enumerated().forEach { index, character in
            let character = String(character)
            let characterSize = NSString(string: character).size(withAttributes: attributes)
            let baselineY = codeBaselineY(
                characterCount: characters.count,
                index: index,
                font: font
            )
            let origin = NSPoint(
                x: rect.minX + (textWidth - characterSize.width) / 2,
                y: baselineY
            )
            NSString(string: character).draw(at: origin, withAttributes: attributes)
        }

        let trackRect = NSRect(x: rect.minX + 12, y: 2, width: 3, height: rect.height - 4)
        let track = NSBezierPath(roundedRect: trackRect, xRadius: 1.5, yRadius: 1.5)
        foregroundColor.withAlphaComponent(0.28).setFill()
        track.fill()

        let color: NSColor
        switch indicator.healthState {
        case .normal: color = .systemGreen
        case .warning: color = .systemOrange
        case .critical, .unavailable: color = .systemRed
        case .stale: color = .systemGray
        case .unknown: color = .secondaryLabelColor
        }
        let fillHeight: CGFloat
        if let percent = indicator.percent {
            fillHeight = (rect.height - 4) * CGFloat(min(max(percent, 0), 100)) / 100
        } else {
            fillHeight = indicator.healthState == .unknown ? 0 : 3
        }
        guard fillHeight > 0 else { return }
        NSGraphicsContext.saveGraphicsState()
        track.addClip()
        color.setFill()
        NSBezierPath(rect: NSRect(x: trackRect.minX, y: trackRect.minY, width: trackRect.width, height: fillHeight + 1)).fill()
        NSGraphicsContext.restoreGraphicsState()
    }

    private static func foregroundColor(for appearance: NSAppearance?) -> NSColor {
        guard let appearance,
              appearance.bestMatch(from: [.aqua, .darkAqua]) == .darkAqua else {
            return .black
        }
        return .white
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
        image.isTemplate = true
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

    static func image(
        percent: Double,
        appearance: NSAppearance? = nil
    ) -> NSImage {
        let image = NSImage(size: size)
        image.lockFocus()
        defer { image.unlockFocus() }

        let rect = NSRect(origin: .zero, size: size)
        draw(
            percent: percent,
            in: rect,
            appearance: appearance ?? NSApp?.effectiveAppearance
                ?? NSAppearance(named: .aqua)!
        )
        image.isTemplate = true
        return image
    }

    static func draw(
        percent: Double,
        in rect: NSRect,
        appearance: NSAppearance
    ) {
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

        let outlineMask = NSImage(size: rect.size)
        outlineMask.lockFocus()
        outline.draw(
            in: NSRect(origin: .zero, size: rect.size),
            from: .zero,
            operation: .sourceOver,
            fraction: 1
        )
        outlineMask.unlockFocus()
        outlineMask.draw(in: rect, from: .zero, operation: .sourceOver, fraction: 1)
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
