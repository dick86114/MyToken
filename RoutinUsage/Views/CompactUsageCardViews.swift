import SwiftUI

/// 简洁卡片视觉组件：头像、环形用量、余额条和圆形操作按钮。
enum CompactPopoverMetrics {
    static var cardCornerRadius: CGFloat { PopoverVisualPolicy.cornerRadius(for: .outer) }
    static var metricCornerRadius: CGFloat { PopoverVisualPolicy.cornerRadius(for: .inner) }
    static let avatarSize: CGFloat = 28
    static let largeGaugeSize: CGFloat = 44
    static let smallGaugeSize: CGFloat = 40
    static let actionSize: CGFloat = 24
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

    static func statusColor(for tone: UsageMetricTone) -> Color {
        switch PopoverVisualPolicy.gaugeAccentRole(for: tone) {
        case .brand: return brandLight
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

    static func tileStroke(_ scheme: ColorScheme, tone: UsageMetricTone) -> Color {
        switch tone {
        case .warning:
            return warningColor.opacity(isDark(scheme) ? 0.34 : 0.22)
        case .critical:
            return criticalColor.opacity(isDark(scheme) ? 0.34 : 0.22)
        case .normal:
            return cardStroke(scheme)
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
    let letter: String
    let providerID: ProviderID

    var body: some View {
        Text(letter)
            .font(.system(size: 12, weight: .bold))
            .foregroundStyle(ProviderTheme.accentColor(for: providerID))
            .frame(width: CompactPopoverMetrics.avatarSize, height: CompactPopoverMetrics.avatarSize)
            .background(
                ProviderTheme.accentColor(for: providerID).opacity(0.15),
                in: RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                    .strokeBorder(ProviderTheme.accentColor(for: providerID).opacity(0.30), lineWidth: 1)
            }
            .saturation(0.55)
            .accessibilityHidden(true)
    }
}

struct CompactCardActionButton<Label: View>: View {
    var action: () -> Void
    var help: String
    var disabled: Bool = false
    @ViewBuilder var label: () -> Label

    @State private var isHovered = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button(action: action) {
            label()
                .foregroundStyle(isHovered ? Color.primary : Color.secondary)
                .frame(width: CompactPopoverMetrics.actionSize, height: CompactPopoverMetrics.actionSize)
                .background {
                    Circle()
                        .fill(Color.primary.opacity(isHovered ? 0.14 : 0.08))
                }
                .overlay {
                    Circle()
                        .strokeBorder(CompactPopoverPalette.actionStroke(colorScheme), lineWidth: 1)
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
    @Environment(\.menuBarColorRules) private var menuBarColorRules
    let percent: Double?
    let tone: UsageMetricTone
    var size: CGFloat = CompactPopoverMetrics.largeGaugeSize
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let clamped = UsageMetricPresentation.clampedPercent(percent ?? 0)
        let ringColor = gaugeColor
        ZStack {
            Circle()
                .stroke(CompactPopoverPalette.gaugeTrack(colorScheme), lineWidth: lineWidth)
            if clamped > 0 {
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
                .frame(width: size - 8)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }

    private var lineWidth: CGFloat {
        size * 3.6 / 36
    }

    private var gaugeColor: Color {
        switch tone {
        case .normal:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        case .warning, .critical:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        }
    }

    /// 数字放大突出，百分号缩小并轻微上移。
    private var gaugeLabel: Text {
        let numberSize: CGFloat = size > 42 ? 14 : 12
        let percentSize: CGFloat = 10
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
        switch tone {
        case .critical:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        case .warning:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        case .normal:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        }
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
        let subtitle = CompactUsageCardPresentation.subtitle(
            for: metric,
            now: now,
            style: style,
            rules: menuBarColorRules
        )
        Group {
            if style == .horizontal {
                HStack(spacing: 12) {
                    CompactUsageGauge(
                        percent: metric.displayedPercent,
                        tone: tone,
                        size: CompactPopoverMetrics.largeGaugeSize
                    )
                    VStack(alignment: .leading, spacing: 4) {
                        labelRow(tone: tone)
                        Text(subtitle)
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundStyle(subtitleColor(tone: tone))
                            .monospacedDigit()
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(10)
            } else {
                VStack(spacing: 6) {
                    CompactUsageGauge(
                        percent: metric.displayedPercent,
                        tone: tone,
                        size: CompactPopoverMetrics.smallGaugeSize
                    )
                    labelRow(tone: tone)
                        .multilineTextAlignment(.center)
                    Text(subtitle)
                            .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(subtitleColor(tone: tone))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                        .frame(maxWidth: .infinity)
                }
                .padding(8)
            }
        }
        .frame(maxWidth: .infinity, alignment: style == .horizontal ? .leading : .center)
        .background {
            RoundedRectangle(cornerRadius: CompactPopoverMetrics.metricCornerRadius, style: .continuous)
                .fill(tileFill(tone: tone))
        }
        .overlay {
            RoundedRectangle(cornerRadius: CompactPopoverMetrics.metricCornerRadius, style: .continuous)
                .strokeBorder(tileStroke(tone: tone), lineWidth: 1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText(subtitle: subtitle))
    }

    @ViewBuilder
    private func labelRow(tone: UsageMetricTone) -> some View {
        HStack(spacing: 4) {
            if tone == .critical || tone == .warning {
                Circle()
                    .fill(UsageMetricPresentation.color(for: tone, rules: menuBarColorRules))
                    .frame(width: tone == .critical ? 6 : 4, height: tone == .critical ? 6 : 4)
            }
            Text(metric.label)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(
                    tone == .warning || tone == .critical
                        ? UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
                        : Color.primary.opacity(0.82)
                )
                .lineLimit(1)
        }
    }

    private func tileFill(tone: UsageMetricTone) -> Color {
        CompactPopoverPalette.tileFill(colorScheme, tone: tone)
    }

    private func tileStroke(tone: UsageMetricTone) -> Color {
        CompactPopoverPalette.tileStroke(colorScheme, tone: tone)
    }

    private func subtitleColor(tone: UsageMetricTone) -> Color {
        switch tone {
        case .warning:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        case .critical:
            return UsageMetricPresentation.color(for: tone, rules: menuBarColorRules)
        case .normal:
            return CompactPopoverPalette.subtitle(colorScheme)
        }
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
        VStack(alignment: .leading, spacing: 4) {
            Text(metric.label)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(valueText)
                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                .monospacedDigit()
                .foregroundStyle(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background {
            RoundedRectangle(cornerRadius: CompactPopoverMetrics.metricCornerRadius, style: .continuous)
                .fill(CompactPopoverPalette.tileFill(colorScheme, tone: .normal))
        }
        .overlay {
            RoundedRectangle(cornerRadius: CompactPopoverMetrics.metricCornerRadius, style: .continuous)
                .strokeBorder(CompactPopoverPalette.tileStroke(colorScheme, tone: .normal), lineWidth: 1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(valueText)")
    }
}

struct CompactBalanceStrip: View {
    let metric: NormalizedUsageMetric
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 1) {
            Text(metric.label)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
            Text(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))
                .font(.system(size: 20, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(CompactPopoverPalette.amountGreen(colorScheme))
                .minimumScaleFactor(0.7)
                .lineLimit(1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))")
    }
}
