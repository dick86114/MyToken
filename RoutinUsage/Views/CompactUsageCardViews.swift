import SwiftUI

/// 简洁卡片视觉组件：头像、环形用量、余额条和圆形操作按钮。
enum CompactPopoverMetrics {
    static let cardCornerRadius: CGFloat = 16
    static let metricCornerRadius: CGFloat = 12
    static let avatarSize: CGFloat = 28
    static let largeGaugeSize: CGFloat = 44
    static let smallGaugeSize: CGFloat = 40
    static let actionSize: CGFloat = 24
}


enum CompactPopoverPalette {
    // 深色令牌取自 simple-ui/dark：深蓝底 #0A0D14、面板 #121722、emerald 主色。
    static let darkCanvas = Color(red: 0.039, green: 0.051, blue: 0.078)
    static let darkSurface = Color(red: 0.071, green: 0.090, blue: 0.133)
    static let darkEmerald = Color(red: 0.063, green: 0.957, blue: 0.612)
    static let darkMutedText = Color(red: 0.580, green: 0.639, blue: 0.722)

    static func cardSurfaceTint(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkSurface.opacity(0.50) : glassTint(scheme)
    }

    static func isDark(_ scheme: ColorScheme) -> Bool { scheme == .dark }

    static func glassTint(_ scheme: ColorScheme) -> Color {
        Color.white.opacity(isDark(scheme) ? 0.04 : 0.22)
    }

    static func cardStroke(_ scheme: ColorScheme) -> Color {
        Color.white.opacity(isDark(scheme) ? 0.10 : 0.82)
    }

    static func chipFill(hovered: Bool, _ scheme: ColorScheme) -> Color {
        if isDark(scheme) {
            return Color.white.opacity(hovered ? 0.10 : 0.06)
        }
        return Color.white.opacity(hovered ? 0.90 : 0.70)
    }

    static func chipText(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.92) : Color(red: 0.20, green: 0.25, blue: 0.33)
    }

    static func chipSecondary(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkMutedText : Color(red: 0.58, green: 0.64, blue: 0.72)
    }

    static func selectedSegmentFill(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.10) : Color.white
    }

    static func selectedSegmentStroke(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? Color.white.opacity(0.18) : Color.clear
    }

    static func tileFill(_ scheme: ColorScheme, tone: UsageMetricTone) -> Color {
        switch tone {
        case .warning:
            return Color.orange.opacity(isDark(scheme) ? 0.12 : 0.08)
        case .critical:
            return Color.red.opacity(isDark(scheme) ? 0.12 : 0.08)
        case .normal:
            // 深色下普通用量不铺底色，避免"卡片套卡片"的繁琐感。
            return isDark(scheme) ? .clear : Color.white.opacity(0.45)
        }
    }

    static func tileStroke(_ scheme: ColorScheme, tone: UsageMetricTone) -> Color {
        switch tone {
        case .warning:
            return Color.orange.opacity(isDark(scheme) ? 0.40 : 0.25)
        case .critical:
            return Color.red.opacity(isDark(scheme) ? 0.40 : 0.22)
        case .normal:
            return isDark(scheme) ? .clear : Color.white.opacity(0.55)
        }
    }

    static func subtitle(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkMutedText : Color(red: 0.53, green: 0.53, blue: 0.55)
    }

    static func amountGreen(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkEmerald : Color(red: 0.02, green: 0.59, blue: 0.41)
    }

    static func badgeGreen(_ scheme: ColorScheme) -> Color {
        isDark(scheme) ? darkEmerald : Color(red: 0.02, green: 0.45, blue: 0.33)
    }

    static func gaugeTrack(_ scheme: ColorScheme) -> Color {
        Color.primary.opacity(isDark(scheme) ? 0.06 : 0.08)
    }

    static func actionStroke(_ scheme: ColorScheme) -> Color {
        Color.white.opacity(isDark(scheme) ? 0.12 : 0.55)
    }

    static func hairline(_ scheme: ColorScheme) -> Color {
        Color.primary.opacity(isDark(scheme) ? 0.08 : 0.04)
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
                in: RoundedRectangle(cornerRadius: 12, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(ProviderTheme.accentColor(for: providerID).opacity(0.30), lineWidth: 1)
            }
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
    let percent: Double?
    let tone: UsageMetricTone
    let accent: Color
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
            return accent
        case .warning, .critical:
            return tone.color
        }
    }

    /// 数字放大突出，百分号缩小并轻微上移。
    private var gaugeLabel: Text {
        let numberSize: CGFloat = size > 42 ? 13.5 : 11.5
        let percentSize: CGFloat = size > 42 ? 9.5 : 8.0
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
            return .red
        case .warning:
            return Color.orange
        case .normal:
            return Color.primary.opacity(0.85)
        }
    }
}

struct CompactMetricGaugeTile: View {
    let metric: NormalizedUsageMetric
    let now: Date
    let style: CompactGaugeStyle
    let providerID: ProviderID
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let tone = CompactUsageCardPresentation.gaugeTone(for: metric) ?? .normal
        let subtitle = CompactUsageCardPresentation.subtitle(for: metric, now: now, style: style)
        Group {
            if style == .horizontal {
                HStack(spacing: 12) {
                    CompactUsageGauge(
                        percent: metric.displayedPercent,
                        tone: tone,
                        accent: ProviderTheme.accentColor(for: providerID),
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
                        accent: ProviderTheme.accentColor(for: providerID),
                        size: CompactPopoverMetrics.smallGaugeSize
                    )
                    labelRow(tone: tone)
                        .multilineTextAlignment(.center)
                    Text(subtitle)
                        .font(.system(size: 9, design: .monospaced))
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
                    .fill(tone.color)
                    .frame(width: tone == .critical ? 6 : 4, height: tone == .critical ? 6 : 4)
            }
            Text(metric.label)
                .font(.system(size: style == .horizontal ? 11 : 10, weight: .medium))
                .foregroundStyle(tone == .warning || tone == .critical ? tone.color : Color.primary.opacity(0.78))
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
            return Color.orange
        case .critical:
            return Color.red
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
                .font(.system(size: 12, weight: .semibold, design: .rounded))
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
                .shadow(color: Color.green.opacity(0.12), radius: 3, y: 1)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))")
    }
}

struct CompactBalanceWave: View {
    var body: some View {
        GeometryReader { geometry in
            Path { path in
                let width = geometry.size.width
                let height = geometry.size.height
                path.move(to: CGPoint(x: 0, y: height * 0.78))
                path.addQuadCurve(
                    to: CGPoint(x: width * 0.55, y: height * 0.58),
                    control: CGPoint(x: width * 0.22, y: height * 0.18)
                )
                path.addQuadCurve(
                    to: CGPoint(x: width, y: height * 0.28),
                    control: CGPoint(x: width * 0.82, y: height * 0.92)
                )
                path.addLine(to: CGPoint(x: width, y: height))
                path.addLine(to: CGPoint(x: 0, y: height))
                path.closeSubpath()
            }
            .fill(Color.green.opacity(0.10))
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
