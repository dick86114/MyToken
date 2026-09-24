import AppKit

enum MenuBarBalanceFormatter {
    static func compactText(_ value: Decimal?) -> String {
        guard let value, !value.isNaN else {
            return "--"
        }

        let integerBehavior = NSDecimalNumberHandler(
            roundingMode: .down,
            scale: 0,
            raiseOnExactness: false,
            raiseOnOverflow: false,
            raiseOnUnderflow: false,
            raiseOnDivideByZero: false
        )
        let absoluteDecimal = abs(value)
        let absoluteValue = NSDecimalNumber(decimal: absoluteDecimal)
        let isNegative = value < 0

        let units: [(threshold: Decimal, divisor: Decimal, suffix: String)] = [
            (Decimal(1_000_000_000), Decimal(1_000_000_000), "B"),
            (Decimal(1_000_000), Decimal(1_000_000), "M"),
            (Decimal(1_000), Decimal(1_000), "k")
        ]

        for unit in units where absoluteDecimal >= unit.threshold {
            let shortened = absoluteValue
                .dividing(by: NSDecimalNumber(decimal: unit.divisor))
                .rounding(accordingToBehavior: integerBehavior)
            let signedShortened = isNegative ? -shortened.decimalValue : shortened.decimalValue
            let number = NSDecimalNumber(decimal: signedShortened).stringValue
            return number + unit.suffix
        }

        let integer = absoluteValue.rounding(accordingToBehavior: integerBehavior)
        let signedInteger = isNegative ? -integer.decimalValue : integer.decimalValue
        return NSDecimalNumber(decimal: signedInteger).stringValue
    }
}

enum MenuBarIndicatorContent: Equatable, Sendable {
    case progress(Double)
    case balance(String)
    case status
    case none
}

struct MenuBarIndicatorModel: Equatable, Sendable {
    let shortCode: String
    let percent: Double?
    let healthState: UsageMetricHealthState
    let accessibilityLabel: String
    let content: MenuBarIndicatorContent

    init(
        shortCode: String,
        percent: Double?,
        healthState: UsageMetricHealthState,
        accessibilityLabel: String,
        content: MenuBarIndicatorContent? = nil
    ) {
        self.shortCode = shortCode
        self.percent = percent
        self.healthState = healthState
        self.accessibilityLabel = accessibilityLabel
        self.content = content ?? percent.map { .progress($0) } ?? .none
    }

    static func hoverSummary(for indicators: [MenuBarIndicatorModel]) -> String {
        indicators.map(\.accessibilityLabel).joined(separator: "\n")
    }

    static func make(
        state: KeyUsageState,
        descriptor: ProviderDescriptor,
        metric: NormalizedUsageMetric?
    ) -> Self {
        if let metric, metric.semantic == .usedQuota || metric.semantic == .remainingQuota,
           let percent = metric.displayedPercent {
            return Self(
                shortCode: descriptor.shortCode,
                percent: percent,
                healthState: metric.healthState == .unknown
                    ? MenuBarUsageRisk.healthState(for: percent)
                    : metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，\(metric.displaysRemainingPercent ? "剩余" : "已使用") \(Int(percent.rounded()))%",
                content: .progress(percent)
            )
        }

        if let metric, metric.semantic == .balance {
            let value = metric.value.map { NSDecimalNumber(decimal: $0).stringValue } ?? "未知"
            return Self(
                shortCode: descriptor.shortCode,
                percent: nil,
                healthState: metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，余额 \(value)",
                content: .balance(MenuBarBalanceFormatter.compactText(metric.value))
            )
        }

        if let metric, metric.semantic == .status {
            return Self(
                shortCode: descriptor.shortCode,
                percent: nil,
                healthState: metric.healthState,
                accessibilityLabel: "\(descriptor.displayName)，\(state.configuration.displayName)，账户状态",
                content: .status
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
        level(for: percent, rules: .standard)
    }

    static func level(for percent: Double, rules: MenuBarColorRules) -> Self {
        let clampedPercent = percent.isFinite ? min(max(percent, 0), 100) : 0
        if clampedPercent >= Double(rules.criticalThreshold) {
            return .critical
        }
        if clampedPercent >= Double(rules.warningThreshold) {
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
    static let unitWidth: CGFloat = 24
    static let gap: CGFloat = 0
    static let outerPadding: CGFloat = 1.5
    static let size = NSSize(width: unitWidth, height: 26)
    static let progressTrackHeight: CGFloat = 18
    static let progressFillInset: CGFloat = 1
    static let codeVerticalOffset: CGFloat = 2

    static func imageWidth(for count: Int) -> CGFloat {
        let displayedCount = max(1, min(count, maximumCount))
        return outerPadding * 2
            + unitWidth * CGFloat(displayedCount)
            + gap * CGFloat(max(0, displayedCount - 1))
    }

    static func image(
        indicators: [MenuBarIndicatorModel],
        colorRules: MenuBarColorRules = .standard
    ) -> NSImage {
        let displayedIndicators = Array(indicators.prefix(maximumCount))
        let imageSize = NSSize(
            width: imageWidth(for: displayedIndicators),
            height: size.height
        )
        let image = NSImage(size: imageSize, flipped: false) { _ in
            var cursor = outerPadding
            for (index, indicator) in displayedIndicators.enumerated() {
                let width = unitWidth(for: indicator)
                draw(
                    indicator: indicator,
                    colorRules: colorRules,
                    in: NSRect(
                        x: cursor,
                        y: 0,
                        width: width,
                        height: size.height
                    )
                )
                cursor += width
                if index < displayedIndicators.count - 1 {
                    cursor += gap
                }
            }
            return true
        }
        // 延迟绘制让 labelColor 在每块屏幕的菜单栏外观里解析，彩色填充不会被模板化抹掉。
        image.isTemplate = false
        return image
    }

    static func codeFont(for characterCount: Int) -> NSFont {
        let size: CGFloat
        switch characterCount {
        case ..<2:
            size = 9.5
        case 2:
            size = 8.4
        default:
            size = 7
        }
        return NSFont.monospacedSystemFont(ofSize: size, weight: .bold)
    }

    static func codeSlotHeight(for characterCount: Int) -> CGFloat {
        guard characterCount > 1 else { return 0 }
        let font = codeFont(for: characterCount)
        return (progressTrackHeight - font.capHeight)
            / CGFloat(characterCount - 1)
    }

    static func codeBaselineY(
        characterCount: Int,
        index: Int,
        font: NSFont
    ) -> CGFloat {
        guard characterCount > 1 else {
            return size.height / 2 + font.capHeight / 2 - codeVerticalOffset
        }
        let slotHeight = codeSlotHeight(for: characterCount)
        let trackTop = (size.height - progressTrackHeight) / 2
        let topBaseline = trackTop + progressTrackHeight
            - font.capHeight
            - codeVerticalOffset
        return topBaseline - slotHeight * CGFloat(index)
    }

    private static func draw(
        indicator: MenuBarIndicatorModel,
        colorRules: MenuBarColorRules,
        in rect: NSRect
    ) {
        let text = indicator.shortCode
        let characters = Array(text.prefix(3))
        let font = codeFont(for: characters.count)
        let attributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: NSColor.labelColor
        ]
        let textCenter = rect.minX + 4.5

        characters.enumerated().forEach { index, character in
            let character = String(character)
            let characterSize = NSString(string: character).size(withAttributes: attributes)
            let baselineY = codeBaselineY(
                characterCount: characters.count,
                index: index,
                font: font
            )
            let origin = NSPoint(
                x: textCenter - characterSize.width / 2,
                y: baselineY
            )
            NSString(string: character).draw(at: origin, withAttributes: attributes)
        }

        switch indicator.content {
        case let .progress(percent):
            drawProgress(
                percent: percent,
                indicator: indicator,
                colorRules: colorRules,
                in: rect
            )
        case let .balance(text):
            drawBalance(
                text: text,
                indicator: indicator,
                colorRules: colorRules,
                in: rect
            )
        case .status, .none:
            break
        }
    }

    private static func progressColor(
        for indicator: MenuBarIndicatorModel,
        rules: MenuBarColorRules
    ) -> NSColor {
        if let percent = indicator.percent {
            return rules.color(for: MenuBarUsageRisk.level(for: percent, rules: rules))
        }

        switch indicator.healthState {
        case .normal:
            return rules.color(for: .normal)
        case .warning:
            return rules.color(for: .warning)
        case .critical, .unavailable:
            return rules.color(for: .critical)
        case .stale:
            return rules.color(for: .warning)
        case .unknown:
            return rules.color(for: .normal)
        }
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

enum MenuBarMonoBrandLogo {
    static let size = NSSize(width: 18, height: 18)

    static func image() -> NSImage {
        // 和短码/进度条一致：使用动态 labelColor 延迟解析菜单栏外观。
        let image = NSImage(size: size, flipped: false) { _ in
            let rect = NSRect(origin: .zero, size: size)
            guard let source = NSImage(named: "MenuBarMonoBrandLogo") else {
                return false
            }

            NSColor.labelColor.setFill()
            NSBezierPath(rect: rect).fill()
            source.draw(
                in: rect,
                from: .zero,
                operation: .destinationIn,
                fraction: 1
            )
            return true
        }
        image.isTemplate = false
        return image
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
extension MenuBarMultiUsageIcon {
    static let indicatorStrokeWidth: CGFloat = 1
    static var indicatorBorderColor: NSColor {
        NSColor.labelColor
    }
    static let balanceUnitWidth: CGFloat = 30
    static let balanceDiameter: CGFloat = 18
    static var balanceTextMaximumWidth: CGFloat {
        balanceDiameter - indicatorStrokeWidth - 4
    }

    static func balanceFontSize(for text: String) -> CGFloat {
        let preferredSize: CGFloat
        switch text.count {
        case 1:
            preferredSize = 10
        case 2:
            preferredSize = 9
        case 3:
            preferredSize = 7.5
        default:
            preferredSize = 6.5
        }

        var fontSize = preferredSize
        while fontSize > 5 {
            let font = NSFont.monospacedSystemFont(ofSize: fontSize, weight: .bold)
            let width = NSString(string: text).size(withAttributes: [
                .font: font
            ]).width
            if width <= balanceTextMaximumWidth {
                break
            }
            fontSize -= 0.5
        }
        return fontSize
    }
    static func imageWidth(for indicators: [MenuBarIndicatorModel]) -> CGFloat {
        let displayed = indicators.prefix(maximumCount)
        guard !displayed.isEmpty else {
            return imageWidth(for: 0)
        }

        let widths = displayed.map(unitWidth(for:))
        return outerPadding * 2
            + widths.reduce(0, +)
            + gap * CGFloat(max(0, widths.count - 1))
    }

    static func unitWidth(for indicator: MenuBarIndicatorModel) -> CGFloat {
        if case .balance = indicator.content {
            return balanceUnitWidth
        }
        return unitWidth
    }
    private static func drawProgress(
        percent: Double,
        indicator: MenuBarIndicatorModel,
        colorRules: MenuBarColorRules,
        in rect: NSRect
    ) {
        let trackRect = NSRect(
            x: rect.minX + 9.5,
            y: (rect.height - progressTrackHeight) / 2,
            width: 7.5,
            height: progressTrackHeight
        )
        let track = NSBezierPath(roundedRect: trackRect, xRadius: 2.5, yRadius: 2.5)
        track.lineWidth = Self.indicatorStrokeWidth
        indicatorBorderColor.setStroke()
        NSColor.secondaryLabelColor.withAlphaComponent(0.22).setFill()
        track.fill()

        let innerRect = trackRect.insetBy(
            dx: track.lineWidth / 2 + progressFillInset,
            dy: track.lineWidth / 2 + progressFillInset
        )
        let fillHeight = innerRect.height
            * CGFloat(min(max(percent, 0), 100)) / 100
        if fillHeight > 0 {
            progressColor(for: indicator, rules: colorRules).setFill()
            let fillRect = NSRect(
                x: innerRect.minX,
                y: innerRect.minY,
                width: innerRect.width,
                height: fillHeight
            )
            let fillRadius = min(2, min(fillRect.width, fillRect.height) / 2)
            NSBezierPath(
                roundedRect: fillRect,
                xRadius: fillRadius,
                yRadius: fillRadius
            ).fill()
        }
        track.stroke()
    }

    private static func drawBalance(
        text: String,
        indicator: MenuBarIndicatorModel,
        colorRules: MenuBarColorRules,
        in rect: NSRect
    ) {
        let circleRect = NSRect(
            x: rect.maxX - balanceDiameter - 1.5,
            y: (rect.height - balanceDiameter) / 2,
            width: balanceDiameter,
            height: balanceDiameter
        )
        let statusColor = balanceColor(for: indicator, rules: colorRules)
        let circle = NSBezierPath(ovalIn: circleRect)
        circle.lineWidth = Self.indicatorStrokeWidth
        indicatorBorderColor.setStroke()
        circle.stroke()

        let fontSize = balanceFontSize(for: text)
        let font = NSFont.monospacedSystemFont(ofSize: fontSize, weight: .bold)
        let attributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: statusColor
        ]
        let textSize = NSString(string: text).size(withAttributes: attributes)
        NSString(string: text).draw(
            at: NSPoint(
                x: circleRect.midX - textSize.width / 2,
                y: circleRect.midY - textSize.height / 2
            ),
            withAttributes: attributes
        )
    }

    static func highContrastTextColor(for fill: NSColor) -> NSColor {
        guard let srgb = fill.usingColorSpace(.sRGB) else {
            return .white
        }

        func linearized(_ component: CGFloat) -> CGFloat {
            component <= 0.04045
                ? component / 12.92
                : pow((component + 0.055) / 1.055, 2.4)
        }

        let luminance = 0.2126 * linearized(srgb.redComponent)
            + 0.7152 * linearized(srgb.greenComponent)
            + 0.0722 * linearized(srgb.blueComponent)
        let contrastWithBlack = (luminance + 0.05) / 0.05
        let contrastWithWhite = 1.05 / (luminance + 0.05)
        return contrastWithBlack >= contrastWithWhite ? .black : .white
    }

    private static func balanceColor(
        for indicator: MenuBarIndicatorModel,
        rules: MenuBarColorRules
    ) -> NSColor {
        if case let .balance(text) = indicator.content, text == "--" {
            return .gray
        }

        switch indicator.healthState {
        case .normal:
            return rules.normalColor.nsColor
        case .warning, .critical, .unavailable, .stale:
            return rules.criticalColor.nsColor
        case .unknown:
            return .gray
        }
    }
}
