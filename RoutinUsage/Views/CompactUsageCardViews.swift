import SwiftUI

/// 简洁卡片视觉组件：头像、环形用量、余额条和圆形操作按钮。
enum CompactPopoverMetrics {
    static var cardCornerRadius: CGFloat { PopoverVisualPolicy.cornerRadius(for: .outer) }
    static var metricCornerRadius: CGFloat { PopoverVisualPolicy.cornerRadius(for: .inner) }
    static let avatarSize: CGFloat = 28
    static let fullAvatarSize: CGFloat = 28
    static let gaugeSize: CGFloat = 36
    static let actionSize: CGFloat = 25
    static let contentIndent: CGFloat = 38
    static let fullContentIndent: CGFloat = 38
    static let cardHorizontalPadding: CGFloat = 12
    static let fullCardHorizontalPadding: CGFloat = 12
    static let cardVerticalPadding: CGFloat = 10
}


enum CompactPopoverPalette {
    // 方案 A：石墨蓝承载结构，品牌蓝强调操作和正常用量，彩色只表达状态。
    static let darkCanvas = Color(red: 0.043, green: 0.059, blue: 0.078)
    static let darkSurface = Color(red: 0.071, green: 0.094, blue: 0.129)
    static let darkElevated = Color(red: 0.102, green: 0.133, blue: 0.188)
    static let darkMutedText = Color(red: 0.580, green: 0.639, blue: 0.722)
    static let lightCanvas = Color(red: 0.957, green: 0.961, blue: 0.969)
    static let brandLight = Color(red: 0.184, green: 0.502, blue: 0.929)
    static let brandDark = Color(red: 0.302, green: 0.580, blue: 0.961)
    static let positiveLight = Color(red: 0.184, green: 0.620, blue: 0.471)
    static let positiveDark = Color(red: 0.271, green: 0.706, blue: 0.545)
    static let warningColor = Color(red: 0.788, green: 0.541, blue: 0.180)
    static let criticalColor = Color(red: 0.820, green: 0.357, blue: 0.357)

    static func brand(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? brandDark : brandLight
    }

    static func balanceAccent(
        for state: UsageMetricHealthState,
        _ scheme: ColorScheme
    ) -> Color {
        switch PopoverVisualPolicy.balanceAccentRole(for: state) {
        case .positive:
            return positive(scheme)
        case .critical:
            return criticalColor
        case .secondary:
            return .secondary
        }
    }

    static func positive(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? positiveDark : positiveLight
    }

    static func healthColor(for state: UsageMetricHealthState, _ scheme: ColorScheme) -> Color {
        switch PopoverVisualPolicy.healthAccentRole(for: state) {
        case .brand:
            return brand(scheme)
        case .warning:
            return warningColor
        case .critical:
            return criticalColor
        case .secondary:
            return .secondary
        }
    }

    static func statusColor(for tone: UsageMetricTone, _ scheme: ColorScheme) -> Color {
        switch PopoverVisualPolicy.gaugeAccentRole(for: tone) {
        case .brand: return brand(scheme)
        case .warning: return warningColor
        case .critical: return criticalColor
        }
    }

    static func surface(_ role: PopoverVisualPolicy.SurfaceRole, _ scheme: ColorScheme) -> Color {
        switch PopoverVisualPolicy.material(for: role) {
        case .windowGlass:
            return isDark(scheme) ? darkCanvas.opacity(0.72) : lightCanvas.opacity(0.38)
        case .solid:
            switch role {
            case .card: return isDark(scheme) ? darkSurface : .white
            case .metric: return isDark(scheme) ? darkElevated.opacity(0.58) : lightCanvas
            case .control: return isDark(scheme) ? darkElevated.opacity(0.72) : .white.opacity(0.88)
            case .window, .modal: return .clear
            }
        case .modalGlass:
            return isDark(scheme) ? darkSurface.opacity(0.94) : .white.opacity(0.94)
        }
    }

    static func opaqueWindow(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkCanvas : lightCanvas
    }

    static func isDark(_ scheme: ColorScheme) -> Bool { scheme == .dark }

    static func cardStroke(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.10) : Color.black.opacity(0.08)
    }

    static func chipFill(hovered: Bool, _ scheme: ColorScheme) -> Color {
        if isDark(scheme) {
            return darkElevated.opacity(hovered ? 0.96 : 0.74)
        }
        return .white.opacity(hovered ? 1 : 0.86)
    }

    static func chipText(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.92) : Color(red: 0.12, green: 0.15, blue: 0.20)
    }

    static func chipSecondary(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkMutedText : Color(red: 0.43, green: 0.47, blue: 0.53)
    }

    static func selectedSegmentFill(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkElevated : .white
    }

    static func selectedSegmentStroke(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.12) : Color.black.opacity(0.06)
    }

    static func tileFill(_ scheme: ColorScheme, tone: UsageMetricTone) -> Color {
        let base = surface(.metric, scheme)
        switch tone {
        case .warning:
            return warningColor.opacity(isDark(scheme) ? 0.12 : 0.08)
        case .critical:
            return criticalColor.opacity(isDark(scheme) ? 0.12 : 0.08)
        case .normal:
            return base
        }
    }

    static func subtitle(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkMutedText : Color(red: 0.42, green: 0.45, blue: 0.50)
    }

    static func amountGreen(_ scheme: ColorScheme) -> Color {
        positive(scheme)
    }

    static func badgeGreen(_ scheme: ColorScheme) -> Color {
        positive(scheme)
    }

    static func gaugeTrack(_ scheme: ColorScheme) -> Color {
        Color.primary.opacity(isDark(scheme) ? 0.10 : 0.10)
    }

    static func actionStroke(_ scheme: ColorScheme) -> Color {
        cardStroke(scheme)
    }
}


struct CompactAccountAvatar: View {
    let providerID: ProviderID
    var size: CGFloat = CompactPopoverMetrics.avatarSize

    var body: some View {
        ProviderLogoBadge(
            providerID: providerID,
            size: size,
            cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner)
        )
    }
}

struct CompactCardActionButton<Label: View>: View {
    var action: () -> Void
    var help: String
    var disabled: Bool = false
    @ViewBuilder var label: () -> Label

    @State private var isHovered = false

    var body: some View {
        Button(action: action) {
            label()
                .foregroundStyle(isHovered ? Color.primary : Color.secondary)
                .frame(width: CompactPopoverMetrics.actionSize, height: CompactPopoverMetrics.actionSize)
                .background {
                    Circle()
                        .fill(Color.primary.opacity(isHovered ? 0.08 : 0.001))
                }
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .onHover { isHovered = $0 }
        .help(help)
    }
}

struct CompactUsageGauge: View {
    let percent: Double?
    let tone: UsageMetricTone
    var color: Color? = nil
    var size: CGFloat = CompactPopoverMetrics.gaugeSize
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let clamped = UsageMetricPresentation.clampedPercent(percent ?? 0)
        let ringColor = gaugeColor
        ZStack {
            Circle()
                .stroke(CompactPopoverPalette.gaugeTrack(colorScheme), lineWidth: lineWidth)
            if clamped > 0 {
                GaugeSectorShape(progress: clamped / 100)
                    .fill(ringColor.opacity(colorScheme == .dark ? 0.18 : 0.12))
                    .padding(lineWidth + 0.5)

                Circle()
                    .trim(from: 0, to: CGFloat(clamped / 100))
                    .stroke(
                        ringColor,
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
            }
            gaugeLabel
                .foregroundStyle(percentTextColor)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
                .frame(width: size - 10)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }

    private var lineWidth: CGFloat {
        size * 3 / 36
    }

    private var gaugeColor: Color {
        color ?? CompactPopoverPalette.statusColor(for: tone, colorScheme)
    }

    /// 数字放大突出，百分号缩小并轻微上移。
    private var gaugeLabel: Text {
        let numberSize: CGFloat = 10.5
        let percentSize: CGFloat = 8
        let numberWeight: Font.Weight = (percent ?? 0) <= 0 ? .semibold : .bold

        return Text(CompactUsageCardPresentation.compactPercentNumberText(percent))
            .font(.system(size: numberSize, weight: numberWeight, design: .monospaced))
            + Text("%")
                .font(.system(size: percentSize, weight: .semibold, design: .monospaced))
                .baselineOffset(1)
    }

    private var percentTextColor: Color {
        if (percent ?? 0) <= 0 {
            return Color.secondary.opacity(0.7)
        }
        return Color.primary.opacity(0.82)
    }
}

/// 环内淡扇形：从圆心铺开，半径止于外圈内缘。
private struct GaugeSectorShape: Shape {
    let progress: CGFloat

    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2
        var path = Path()
        path.move(to: center)
        path.addArc(
            center: center,
            radius: radius,
            startAngle: .degrees(-90),
            endAngle: .degrees(-90 + 360 * progress),
            clockwise: false
        )
        path.closeSubpath()
        return path
    }
}

struct CompactMetricGaugeTile: View {
    @Environment(\.menuBarColorRules) private var menuBarColorRules
    let metric: NormalizedUsageMetric
    let now: Date
    let style: CompactGaugeStyle
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let tone = CompactUsageCardPresentation.gaugeTone(
            for: metric,
            rules: menuBarColorRules
        ) ?? .normal
        let subtitle = style == .horizontal
            ? CompactUsageCardPresentation.singleMetricSubtitle(for: metric, now: now)
            : CompactUsageCardPresentation.subtitle(for: metric, now: now)
        Group {
            if style == .horizontal {
                horizontalLayout(tone: tone, subtitle: subtitle)
            } else {
                verticalLayout(tone: tone, subtitle: subtitle)
            }
        }
        .padding(.horizontal, style == .horizontal ? 9 : 8)
        .padding(.vertical, style == .horizontal ? 9 : 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(
                cornerRadius: CompactPopoverMetrics.metricCornerRadius,
                style: .continuous
            )
            .fill(CompactPopoverPalette.surface(.metric, colorScheme))
        }
        .overlay {
            RoundedRectangle(
                cornerRadius: CompactPopoverMetrics.metricCornerRadius,
                style: .continuous
            )
            .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme).opacity(0.72), lineWidth: 1)
            .allowsHitTesting(false)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText(subtitle: subtitle))
    }

    private func horizontalLayout(tone: UsageMetricTone, subtitle: String) -> some View {
        HStack(alignment: .center, spacing: 7) {
            CompactUsageGauge(
                percent: metric.displayedPercent,
                tone: tone,
                color: UsageMetricPresentation.color(
                    for: tone,
                    rules: menuBarColorRules
                ),
                size: CompactPopoverMetrics.gaugeSize
            )

            if !metric.resetCards.isEmpty {
                UsageResetCardsBadge(resetCards: metric.resetCards, style: .compact)
            }

            metricBody(subtitle: subtitle)
        }
    }

    private func verticalLayout(tone: UsageMetricTone, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(alignment: .center, spacing: 7) {
                CompactUsageGauge(
                    percent: metric.displayedPercent,
                    tone: tone,
                    color: UsageMetricPresentation.color(
                        for: tone,
                        rules: menuBarColorRules
                    ),
                    size: CompactPopoverMetrics.gaugeSize
                )

                if !metric.resetCards.isEmpty {
                    UsageResetCardsBadge(resetCards: metric.resetCards, style: .compact)
                }

                Spacer(minLength: 0)
            }

            metricBody(subtitle: subtitle)
        }
    }

    private func metricBody(subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(metric.label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(Color.primary.opacity(0.82))
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            UsageMetricProgressBar(percent: metric.displayedPercent ?? 0, height: 3)

            Text(subtitle)
                .font(.system(size: 10))
                .foregroundStyle(subtitleColor())
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func subtitleColor() -> Color {
        CompactPopoverPalette.subtitle(colorScheme)
    }

    private func accessibilityText(subtitle: String) -> String {
        let percent = CompactUsageCardPresentation.compactPercentText(metric.displayedPercent)
        if subtitle.isEmpty {
            return "\(metric.label)，已使用 \(percent)"
        }
        return "\(metric.label)，已使用 \(percent)，\(subtitle)"
    }
}

struct CompactValueTile: View {
    let metric: NormalizedUsageMetric
    let valueText: String
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(metric.label)
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                .lineLimit(1)
            Text(valueText)
                .font(.system(size: 12.5, weight: .semibold, design: .monospaced))
                .monospacedDigit()
                .foregroundStyle(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 7)
        .padding(.vertical, 5)
        .background {
            RoundedRectangle(
                cornerRadius: CompactPopoverMetrics.metricCornerRadius,
                style: .continuous
            )
            .fill(CompactPopoverPalette.surface(.metric, colorScheme))
        }
        .overlay {
            RoundedRectangle(
                cornerRadius: CompactPopoverMetrics.metricCornerRadius,
                style: .continuous
            )
            .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme).opacity(0.72), lineWidth: 1)
            .allowsHitTesting(false)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(valueText)")
    }
}

enum BalanceSummaryStyle {
    case compact
    case full
}

struct CompactBalanceStrip: View {
    let metric: NormalizedUsageMetric
    var compositionMetrics: [NormalizedUsageMetric] = []
    var style: BalanceSummaryStyle = .compact
    var metaText: String? = nil
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    amountText
                    Spacer(minLength: 4)
                    compositionDetails
                }

                VStack(alignment: .leading, spacing: 4) {
                    amountText
                    compositionDetails
                }
            }

            if let metaText, !metaText.isEmpty {
                Text(metaText)
                    .font(.system(size: 10))
                    .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
    }

    private var amountText: some View {
        Text(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))
            .font(.system(size: style == .compact ? 19 : 15, weight: .bold))
            .monospacedDigit()
            .foregroundStyle(CompactPopoverPalette.amountGreen(colorScheme))
            .minimumScaleFactor(0.7)
            .lineLimit(1)
    }

    @ViewBuilder
    private var compositionDetails: some View {
        if let composition = balanceComposition {
            BalanceCompositionDetails(composition: composition)
        }
    }

    private var balanceComposition: BalanceComposition? {
        BalanceComposition(
            currencyCode: metric.currencyCode,
            segments: compositionMetrics.map(BalanceCompositionSegment.init)
        )
    }

    private var accessibilityLabel: String {
        let amount = UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)
        guard let composition = balanceComposition else { return "\(metric.label)，\(amount)" }
        let parts = composition.segments.map { segment in
            "\(segment.label)\(UsageFormatter.currencyText(segment.value, currencyCode: composition.currencyCode))"
        }
        return "\(metric.label)，\(amount)，\(parts.joined(separator: "，"))"
    }
}

private struct BalanceCompositionSegment: Identifiable, Equatable {
    let id: String
    let label: String
    let compactLabel: String
    let value: Decimal
    let kind: BalanceCompositionKind

    init(_ metric: NormalizedUsageMetric) {
        id = metric.id
        label = metric.label
        value = metric.value ?? 0
        switch metric.id {
        case "toppedUpBalance":
            compactLabel = "充值"
            kind = .toppedUp
        case "grantedBalance":
            compactLabel = "赠金"
            kind = .granted
        case "cash-balance":
            compactLabel = "现金"
            kind = .toppedUp
        case "gift-balance":
            compactLabel = "赠送"
            kind = .granted
        default:
            compactLabel = metric.label
            kind = .toppedUp
        }
    }

    func color(_ scheme: ColorScheme) -> Color {
        switch kind {
        case .toppedUp:
            return CompactPopoverPalette.positive(scheme)
        case .granted:
            return CompactPopoverPalette.brand(scheme)
        }
    }
}

private enum BalanceCompositionKind: Equatable {
    case toppedUp
    case granted
}

private struct BalanceComposition: Equatable {
    let currencyCode: String?
    let segments: [BalanceCompositionSegment]
}

private enum BalanceCompositionDetailMetrics {
    static let groupSpacing: CGFloat = 8
    static let itemSpacing: CGFloat = 2
    static let dotSize: CGFloat = 5
}

private struct BalanceCompositionDetails: View {
    let composition: BalanceComposition

    var body: some View {
        HStack(spacing: BalanceCompositionDetailMetrics.groupSpacing) {
            ForEach(composition.segments) { segment in
                BalanceCompositionDetailItem(
                    segment: segment,
                    currencyCode: composition.currencyCode
                )
            }
        }
        .fixedSize(horizontal: true, vertical: false)
    }
}

private struct BalanceCompositionDetailItem: View {
    let segment: BalanceCompositionSegment
    let currencyCode: String?
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let tint = segment.color(colorScheme)
        HStack(spacing: BalanceCompositionDetailMetrics.itemSpacing) {
            Circle()
                .fill(tint)
                .frame(
                    width: BalanceCompositionDetailMetrics.dotSize,
                    height: BalanceCompositionDetailMetrics.dotSize
                )
            Text(segment.compactLabel)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(tint)
            Text(UsageFormatter.currencyText(segment.value, currencyCode: currencyCode))
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .monospacedDigit()
                .foregroundStyle(tint)
        }
        .lineLimit(1)
        .minimumScaleFactor(0.75)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(segment.label)，\(UsageFormatter.currencyText(segment.value, currencyCode: currencyCode))"
        )
    }
}
