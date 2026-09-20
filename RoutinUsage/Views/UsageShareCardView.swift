import AppKit
import CoreImage
import SwiftUI
import UniformTypeIdentifiers

private extension Color {
    init(rgb: UInt32, opacity: Double = 1) {
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255,
            opacity: opacity
        )
    }
}

struct UsageShareChrome {
    let windowFill: Color
    let titleBar: Color
    let previewPane: Color
    let editorPane: Color
    let divider: Color
    let fieldFill: Color
    let inputFill: Color
    let inputBorder: Color
    let textPrimary: Color
    let textSecondary: Color
    let textTertiary: Color
    let amber: Color
    let amberSoft: Color
    let blue: Color

    static func make(_ scheme: ColorScheme) -> UsageShareChrome {
        if scheme == .dark {
            return UsageShareChrome(
                windowFill: Color(rgb: 0x161922),
                titleBar: Color(rgb: 0x1B1F2B),
                previewPane: Color(rgb: 0x10121A),
                editorPane: Color(rgb: 0x181B25),
                divider: Color.white.opacity(0.08),
                fieldFill: Color(rgb: 0x11131B),
                inputFill: Color(rgb: 0x10121A),
                inputBorder: Color.white.opacity(0.10),
                textPrimary: Color.white,
                textSecondary: Color(rgb: 0x94A3B8),
                textTertiary: Color(rgb: 0x64748B),
                amber: Color(rgb: 0xFBBF24),
                amberSoft: Color(rgb: 0xFCD34D),
                blue: Color(rgb: 0x3B82F6)
            )
        }
        return UsageShareChrome(
            windowFill: Color(rgb: 0xF4F6FA),
            titleBar: Color(rgb: 0xECEFF5),
            previewPane: Color(rgb: 0xF3F5F8),
            editorPane: Color.white,
            divider: Color.black.opacity(0.08),
            fieldFill: Color(rgb: 0xF4F6FA),
            inputFill: Color.white,
            inputBorder: Color.black.opacity(0.10),
            textPrimary: Color(rgb: 0x1B1F2B),
            textSecondary: Color(rgb: 0x5B6575),
            textTertiary: Color(rgb: 0x8B95A5),
            amber: Color(rgb: 0xB45309),
            amberSoft: Color(rgb: 0xD97706),
            blue: Color(rgb: 0x2563EB)
        )
    }
}

struct UsageShareCardView: View {
    let card: UsageShareRenderedCard
    var notchFill: Color = Color(rgb: 0x14171F)

    static let canvasWidth: CGFloat = 430

    var body: some View {
        switch card.template {
        case .ticket:
            ticketCard
        case .dark:
            compactCard(isLight: false)
        case .light:
            compactCard(isLight: true)
        }
    }

    private var ticketCard: some View {
        ZStack {
            notchFill
            ticketBody
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
        }
        .frame(width: Self.canvasWidth)
    }

    private var ticketBody: some View {
        VStack(spacing: 0) {
            ticketHeader
            ticketTear
            ticketMetrics
        }
        .background(Color(rgb: 0x181A22))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(Color(rgb: 0xF59E0B).opacity(0.30), lineWidth: 1)
        }
        .overlay { ticketCutouts }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.45), radius: 18, y: 10)
    }

    private var ticketCutouts: some View {
        GeometryReader { geo in
            let y = geo.size.height / 2
            ZStack {
                cutoutCircle(insetX: -2)
                    .position(x: 0, y: y)
                cutoutCircle(insetX: 2)
                    .position(x: geo.size.width, y: y)
            }
        }
        .allowsHitTesting(false)
    }

    private func cutoutCircle(insetX: CGFloat) -> some View {
        Circle()
            .fill(notchFill)
            .frame(width: 24, height: 24)
            .overlay {
                Circle()
                    .stroke(Color(rgb: 0xF59E0B).opacity(0.30), lineWidth: 1)
            }
            .shadow(color: Color.black.opacity(0.5), radius: 1.5, x: insetX, y: 0)
    }

    private var ticketHeader: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 10) {
                avatar(size: 36, fill: Color(rgb: 0xF59E0B).opacity(0.15), stroke: Color(rgb: 0xF59E0B).opacity(0.30), text: Color(rgb: 0xFBBF24))
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(card.passCode)
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundStyle(Color(rgb: 0xFCD34D))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color(rgb: 0xF59E0B).opacity(0.20), in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                        if card.showsStatus {
                            statusBadge(light: false)
                        }
                    }
                    Text(card.displayName)
                        .font(.system(size: 16, weight: .heavy))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("SUPPLIER")
                        .font(.system(size: 9, weight: .medium, design: .monospaced))
                        .foregroundStyle(Color(rgb: 0x94A3B8))
                    Text(card.providerName)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(rgb: 0xFDE68A))
                        .lineLimit(1)
                }
            }

            if !card.subtitle.isEmpty || card.cycleRemainingText != nil {
                HStack(alignment: .top, spacing: 12) {
                    if !card.subtitle.isEmpty {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("套餐规格")
                                .font(.system(size: 10))
                                .foregroundStyle(Color(rgb: 0x94A3B8))
                            Text(card.subtitle)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(2)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    if let cycle = card.cycleRemainingText {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("订阅周期 / 剩余")
                                .font(.system(size: 10))
                                .foregroundStyle(Color(rgb: 0x94A3B8))
                            Text(cycle)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundStyle(Color(rgb: 0xE2E8F0))
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(.top, 4)
                .overlay(alignment: .top) {
                    Rectangle().fill(Color.white.opacity(0.08)).frame(height: 1)
                }
                .padding(.top, 8)
            }

            if let note = card.note {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color(rgb: 0xFBBF24))
                    Text(note)
                        .font(.system(size: 11))
                        .foregroundStyle(Color(rgb: 0xFDE68A))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(rgb: 0xF59E0B).opacity(0.10), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .strokeBorder(Color(rgb: 0xF59E0B).opacity(0.20), lineWidth: 1)
                }
            }
        }
        .padding(16)
        .background(
            LinearGradient(
                colors: [Color(rgb: 0x222733), Color(rgb: 0x171922)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }

    private var ticketTear: some View {
        Color(rgb: 0x14161F)
            .frame(height: 12)
            .overlay(alignment: .center) {
                Rectangle()
                    .fill(Color(rgb: 0x475569))
                    .frame(height: 1.5)
                    .mask(
                        Rectangle()
                            .stroke(style: StrokeStyle(lineWidth: 1.5, dash: [4, 4]))
                    )
                    .padding(.horizontal, 20)
            }
    }

    private var ticketMetrics: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(progressMetrics) { item in
                ticketGauge(item)
            }
            if !tileMetrics.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                    ForEach(tileMetrics) { item in
                        ticketTile(item)
                    }
                }
            }
            if let token = card.tokenPercentText, card.metrics.allSatisfy({ $0.percent == nil || $0.id != "token" }) {
                ticketTile(
                    UsageShareMetricItem(
                        id: "token-ratio",
                        title: "Token 占比",
                        headline: token,
                        percent: nil,
                        amountDetails: [],
                        timeDetails: [],
                        usedText: nil,
                        limitText: nil,
                        remainingAmountText: nil,
                        resetBadgeText: nil,
                        companionText: nil,
                        health: .normal,
                        spansFullWidth: false
                    )
                )
            }
            if card.groupMultiplierText != nil || card.detectionText != nil {
                ticketTile(
                    UsageShareMetricItem(
                        id: "codex-group",
                        title: "Codex 分组倍率",
                        headline: card.groupMultiplierText ?? card.detectionText ?? "—",
                        percent: nil,
                        amountDetails: card.detectionText.map { [$0] } ?? [],
                        timeDetails: [],
                        usedText: nil,
                        limitText: nil,
                        remainingAmountText: nil,
                        resetBadgeText: nil,
                        companionText: card.detectionText,
                        health: .normal,
                        spansFullWidth: false
                    )
                )
            }
            if card.showsWatermark {
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Color(rgb: 0x34D399))
                        Text("MyToken 本地快照 · 无凭据")
                            .foregroundStyle(Color(rgb: 0x94A3B8))
                    }
                    .font(.system(size: 9, design: .monospaced))
                    Spacer()
                    Text(card.capturedAtText.replacingOccurrences(of: ".", with: "-"))
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundStyle(Color(rgb: 0x94A3B8))
                }
                .padding(.top, 4)
            }
            brandFooter
        }
        .padding(16)
        .background(Color(rgb: 0x181A22))
    }

    private func ticketGauge(_ item: UsageShareMetricItem) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(Color(rgb: 0x3B82F6))
                        .frame(width: 8, height: 8)
                    Text(item.title)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(Color(rgb: 0xCBD5E1))
                        .lineLimit(2)
                }
                Spacer()
                Text(item.headline)
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .foregroundStyle(Color(rgb: 0xFBBF24))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            if let percent = item.percent {
                gaugeBar(percent: percent, gradient: true, track: Color(rgb: 0x1E293B))
            }
            if item.usedText != nil || item.limitText != nil {
                HStack {
                    if let used = item.usedText {
                        Text("已用：\(used)")
                            .foregroundStyle(Color(rgb: 0xE2E8F0))
                    }
                    Spacer()
                    if let limit = item.limitText {
                        Text("上限：\(limit)")
                    }
                }
                .font(.system(size: 10, design: .monospaced))
                .foregroundStyle(Color(rgb: 0x94A3B8))
            }
            if item.remainingAmountText != nil || item.resetBadgeText != nil {
                HStack {
                    if let remaining = item.remainingAmountText {
                        Text("剩余：\(remaining)")
                            .foregroundStyle(Color(rgb: 0x34D399))
                    }
                    Spacer()
                    if let badge = item.resetBadgeText {
                        Text(badge)
                            .foregroundStyle(Color(rgb: 0xFCD34D))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color(rgb: 0xF59E0B).opacity(0.12), in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                    }
                }
                .font(.system(size: 10, design: .monospaced))
            }
            if let companion = item.companionText {
                Text(companion)
                    .font(.system(size: 10, design: .monospaced))
                    .foregroundStyle(Color(rgb: 0x94A3B8))
            }
        }
        .padding(12)
        .background(Color(rgb: 0x0F172A).opacity(0.80), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(Color(rgb: 0x1E293B), lineWidth: 1)
        }
    }

    private func ticketTile(_ item: UsageShareMetricItem) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.title)
                .font(.system(size: 9))
                .foregroundStyle(Color(rgb: 0x94A3B8))
            Text(item.headline)
                .font(.system(size: 12, weight: .bold, design: .monospaced))
                .foregroundStyle(tileHeadlineColor(item))
                .lineLimit(2)
                .minimumScaleFactor(0.55)
                .fixedSize(horizontal: false, vertical: true)
            if let companion = item.companionText ?? item.amountDetails.first {
                Text(companion)
                    .font(.system(size: 9, design: .monospaced))
                    .foregroundStyle(Color(rgb: 0x94A3B8))
                    .lineLimit(2)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(rgb: 0x0F172A).opacity(0.60), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(Color.white.opacity(0.06), lineWidth: 1)
        }
    }

    private func compactCard(isLight: Bool) -> some View {
        let bg = isLight ? Color(rgb: 0xFAFAFA) : Color(rgb: 0x0F1117)
        let border = isLight ? Color(rgb: 0xE2E8F0) : Color(rgb: 0x1E293B)
        let titleColor = isLight ? Color(rgb: 0x0F172A) : Color.white
        let sub = isLight ? Color(rgb: 0x64748B) : Color(rgb: 0x94A3B8)
        let panel = isLight ? Color.white : Color(rgb: 0x0B1220)
        let bar = isLight ? Color(rgb: 0x2563EB) : Color(rgb: 0x3B82F6)
        let track = isLight ? Color(rgb: 0xF1F5F9) : Color(rgb: 0x1E293B)
        let percentColor = isLight ? Color(rgb: 0x2563EB) : Color(rgb: 0x60A5FA)
        let resetColor = isLight ? Color(rgb: 0xD97706) : Color(rgb: 0xFBBF24)
        let remainColor = isLight ? Color(rgb: 0x059669) : Color(rgb: 0x34D399)
        let hairline = isLight ? Color(rgb: 0xE2E8F0) : Color.white.opacity(0.08)

        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 10) {
                avatar(
                    size: 32,
                    fill: isLight ? Color(rgb: 0x0F172A) : Color(rgb: 0x2563EB),
                    stroke: .clear,
                    text: .white
                )
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(card.passCode)
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundStyle(isLight ? Color(rgb: 0x334155) : Color(rgb: 0xCBD5E1))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(
                                (isLight ? Color(rgb: 0xE2E8F0) : Color.white.opacity(0.08)),
                                in: RoundedRectangle(cornerRadius: 4, style: .continuous)
                            )
                        if card.showsStatus {
                            Text(card.isAvailable ? "可用" : "不可用")
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundStyle(card.isAvailable ? remainColor : Color(rgb: 0xDC2626))
                        }
                    }
                    Text(card.displayName)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(titleColor)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("SUPPLIER")
                        .font(.system(size: 9, weight: .medium, design: .monospaced))
                        .foregroundStyle(sub)
                    Text(card.providerName)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(titleColor)
                        .lineLimit(1)
                }
            }

            if !card.subtitle.isEmpty || card.cycleRemainingText != nil {
                HStack(alignment: .top, spacing: 12) {
                    if !card.subtitle.isEmpty {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("套餐规格")
                                .font(.system(size: 10))
                                .foregroundStyle(sub)
                            Text(card.subtitle)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(titleColor)
                                .lineLimit(2)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    if let cycle = card.cycleRemainingText {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("订阅周期 / 剩余")
                                .font(.system(size: 10))
                                .foregroundStyle(sub)
                            Text(cycle)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundStyle(titleColor)
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(.top, 8)
                .overlay(alignment: .top) { Rectangle().fill(hairline).frame(height: 1) }
            }

            if let note = card.note {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(resetColor)
                    Text(note)
                        .font(.system(size: 11))
                        .foregroundStyle(titleColor)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(resetColor.opacity(isLight ? 0.08 : 0.12), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
            }

            ForEach(progressMetrics) { item in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(item.title)
                            .font(.system(size: 11))
                            .foregroundStyle(sub)
                            .lineLimit(2)
                        Spacer()
                        Text(item.headline)
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundStyle(percentColor)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                    }
                    if let percent = item.percent {
                        gaugeBar(percent: percent, gradient: false, track: track, fill: bar)
                    }
                    if item.usedText != nil || item.limitText != nil {
                        HStack {
                            if let used = item.usedText {
                                Text("已用：\(used)")
                            }
                            Spacer()
                            if let limit = item.limitText {
                                Text("上限：\(limit)")
                            }
                        }
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(sub)
                    }
                    if item.remainingAmountText != nil || item.resetBadgeText != nil {
                        HStack {
                            if let remaining = item.remainingAmountText {
                                Text("剩余：\(remaining)")
                                    .foregroundStyle(remainColor)
                            }
                            Spacer()
                            if let badge = item.resetBadgeText {
                                Text(badge)
                                    .foregroundStyle(resetColor)
                            }
                        }
                        .font(.system(size: 10, design: .monospaced))
                    }
                    if let companion = item.companionText {
                        Text(companion)
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundStyle(sub)
                    }
                }
                .padding(12)
                .background(panel, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(border, lineWidth: 1)
                }
            }

            if !allTiles.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                    ForEach(allTiles) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.title)
                                .font(.system(size: 10))
                                .foregroundStyle(sub)
                            Text(item.headline)
                                .font(.system(size: 12, weight: .bold, design: .monospaced))
                                .foregroundStyle(titleColor)
                                .lineLimit(2)
                                .minimumScaleFactor(0.55)
                                .fixedSize(horizontal: false, vertical: true)
                            if let companion = item.companionText ?? item.amountDetails.first {
                                Text(companion)
                                    .font(.system(size: 9, design: .monospaced))
                                    .foregroundStyle(sub)
                                    .lineLimit(2)
                            }
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(panel, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .strokeBorder(border, lineWidth: 1)
                        }
                    }
                }
            }

            if card.showsWatermark {
                HStack {
                    Text("MyToken 本地快照 · 无凭据")
                    Spacer()
                    Text(card.capturedAtText.replacingOccurrences(of: ".", with: "-"))
                }
                .font(.system(size: 9, design: .monospaced))
                .foregroundStyle(sub)
            }
            brandFooter
        }
        .padding(16)
        .frame(width: Self.canvasWidth, alignment: .leading)
        .background(bg)
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(border, lineWidth: 1)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(isLight ? 0.08 : 0.35), radius: 16, y: 8)
    }

    private var progressMetrics: [UsageShareMetricItem] {
        card.metrics.filter { $0.percent != nil }
    }

    private var tileMetrics: [UsageShareMetricItem] {
        card.metrics.filter { $0.percent == nil }
    }

    private var allTiles: [UsageShareMetricItem] {
        var items = tileMetrics
        if let token = card.tokenPercentText, card.metrics.allSatisfy({ $0.id != "token" }) {
            items.append(
                UsageShareMetricItem(
                    id: "token-ratio",
                    title: "Token 占比",
                    headline: token,
                    percent: nil,
                    amountDetails: [],
                    timeDetails: [],
                    usedText: nil,
                    limitText: nil,
                    remainingAmountText: nil,
                    resetBadgeText: nil,
                    companionText: nil,
                    health: .normal,
                    spansFullWidth: false
                )
            )
        }
        if card.groupMultiplierText != nil || card.detectionText != nil {
            items.append(
                UsageShareMetricItem(
                    id: "codex-group",
                    title: "Codex 分组倍率",
                    headline: card.groupMultiplierText ?? card.detectionText ?? "—",
                    percent: nil,
                    amountDetails: [],
                    timeDetails: [],
                    usedText: nil,
                    limitText: nil,
                    remainingAmountText: nil,
                    resetBadgeText: nil,
                    companionText: card.detectionText,
                    health: .normal,
                    spansFullWidth: false
                )
            )
        }
        return items
    }

    private func avatar(size: CGFloat, fill: Color, stroke: Color, text: Color) -> some View {
        Text(card.avatarLetter)
            .font(.system(size: size * 0.42, weight: .black))
            .foregroundStyle(text)
            .frame(width: size, height: size)
            .background(fill, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(stroke, lineWidth: stroke == .clear ? 0 : 1)
            }
    }

    private func statusBadge(light: Bool) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(card.isAvailable ? Color(rgb: 0x34D399) : Color(rgb: 0xF87171))
                .frame(width: 6, height: 6)
            Text(card.isAvailable ? "可用" : "不可用")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(card.isAvailable ? Color(rgb: 0x34D399) : Color(rgb: 0xF87171))
        }
    }

    @ViewBuilder
    private func gaugeBar(percent: Double, gradient: Bool, track: Color, fill: Color = Color(rgb: 0x3B82F6)) -> some View {
        let clamped = UsageMetricPresentation.clampedPercent(percent)
        Capsule()
            .fill(track)
            .frame(height: gradient ? 8 : 6)
            .overlay(alignment: .leading) {
                Group {
                    if gradient {
                        Capsule().fill(LinearGradient(colors: [Color(rgb: 0x3B82F6), Color(rgb: 0xFBBF24)], startPoint: .leading, endPoint: .trailing))
                    } else {
                        Capsule().fill(fill)
                    }
                }
                .scaleEffect(x: max(clamped / 100, clamped == 0 ? 0 : 0.02), y: 1, anchor: .leading)
            }
            .clipShape(Capsule())
    }

    private func tileHeadlineColor(_ item: UsageShareMetricItem) -> Color {
        if item.id.contains("codex") || item.title.contains("分组") {
            return Color(rgb: 0xC084FC)
        }
        return .white
    }

    private static let websiteURL = "https://mytoken.idickies.cc/"
    private static let websiteDisplay = "https://mytoken.idickies.cc"
    private static let tagline = "AI 用量，一目了然"

    private var brandFooter: some View {
        Group {
            if card.showsWatermark {
                VStack(spacing: 0) {
                    Rectangle()
                        .fill(LinearGradient(colors: shadowColors, startPoint: .leading, endPoint: .trailing))
                        .frame(height: 1)
                    HStack(spacing: 10) {
                        Image(nsImage: NSImage(named: "PopoverColorBrandLogo") ?? NSApp.applicationIconImage)
                            .resizable()
                            .interpolation(.high)
                            .scaledToFit()
                            .frame(width: 22, height: 22)

                        VStack(alignment: .leading, spacing: 2) {
                            Text("MyToken")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(brandText)
                            Text(Self.tagline)
                                .font(.system(size: 9))
                                .foregroundStyle(brandSubtext)
                            Text(Self.websiteDisplay)
                                .font(.system(size: 9, design: .monospaced))
                                .foregroundStyle(brandSubtext)
                        }

                        Spacer(minLength: 8)

                        if let qr = Self.qrCodeImage {
                            Image(nsImage: qr)
                                .resizable()
                                .interpolation(.none)
                                .scaledToFit()
                                .frame(width: 44, height: 44)
                                .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }
                .background(brandFooterBg)
            }
        }
    }


    private var brandFooterBg: Color {
        switch card.template {
        case .ticket, .dark: return Color(rgb: 0x14171F)
        case .light: return Color(rgb: 0xF1F3F7)
        }
    }

    private var shadowColors: [Color] {
        [Color.black.opacity(0.12), Color.black.opacity(0.04)]
    }

    private var brandText: Color {
        switch card.template {
        case .ticket, .dark: return .white
        case .light: return Color(rgb: 0x1B1F2B)
        }
    }

    private var brandSubtext: Color {
        switch card.template {
        case .ticket, .dark: return Color(rgb: 0x94A3B8)
        case .light: return Color(rgb: 0x64748B)
        }
    }

    private static var qrCodeImage: NSImage? {
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(Data(websiteURL.utf8), forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else { return nil }
        let scale = CGAffineTransform(scaleX: 8, y: 8)
        let scaled = output.transformed(by: scale)
        let context = CIContext()
        guard let cgImage = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return NSImage(cgImage: cgImage, size: NSSize(width: 44, height: 44))
    }
}


enum UsageShareExport {

    @MainActor
    static func image(for card: UsageShareRenderedCard) -> NSImage? {
        let view = UsageShareCardView(card: card)
        let renderer = ImageRenderer(content: view)
        renderer.scale = 2
        renderer.proposedSize = ProposedViewSize(width: UsageShareCardView.canvasWidth, height: nil)
        return renderer.nsImage
    }

    static func pngData(from image: NSImage) -> Data? {
        guard let tiff = image.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiff) else {
            return nil
        }
        return bitmap.representation(using: .png, properties: [:])
    }

    @MainActor
    static func copyPNG(for card: UsageShareRenderedCard) -> Bool {
        guard let image = image(for: card), let data = pngData(from: image) else {
            return false
        }
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setData(data, forType: .png)
        return true
    }

    @MainActor
    static func savePNG(for card: UsageShareRenderedCard, suggestedName: String) {
        guard let image = image(for: card), let data = pngData(from: image) else {
            return
        }
        let panel = NSSavePanel()
        panel.allowedContentTypes = [.png]
        panel.canCreateDirectories = true
        panel.isExtensionHidden = false
        panel.nameFieldStringValue = suggestedName
        panel.begin { response in
            guard response == .OK, let url = panel.url else { return }
            try? data.write(to: url, options: .atomic)
        }
    }
}
