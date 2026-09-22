import SwiftUI

@MainActor
struct UsageRowView: View {
    @Environment(\.colorScheme) private var colorScheme
    let state: KeyUsageState
    var density: UsageCardDensity = .full
    var actions: AnyView?
    var refreshCredential: () -> Void = {}
    var retryCredential: () -> Void = {}
    var onShare: (() -> Void)? = nil

    @State private var showsFailureDetails = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 60)) { timeline in
            if density == .compact {
                compactCard(now: timeline.date)
            } else {
                fullCard(now: timeline.date)
            }
        }
    }

    private func fullCard(now: Date) -> some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 7) {
                headerView(now: now)
                content(now: now)
            }
            if let actions {
                actions
                    .padding(.top, 2)
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 8)
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }

    private func compactCard(now: Date) -> some View {
        let metrics = compactOrderedMetrics
        let arrangement = CompactUsageCardPresentation.arrangement(for: metrics)

        return Group {
            if arrangement == .balance, let metric = metrics.first {
                compactBalanceCard(metric: metric)
                    .background(alignment: .bottom) {
                        CompactBalanceWave()
                            .frame(height: 40)
                            .allowsHitTesting(false)
                    }
            } else {
                compactStandardCard(now: now)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }

    private func compactStandardCard(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            compactHeader(showsDivider: true)

            if let snapshot = state.snapshot {
                compactMetrics(snapshot: snapshot, now: now)
            } else {
                statusLabel
            }
        }
    }

    private func compactBalanceCard(metric: NormalizedUsageMetric) -> some View {
        HStack(alignment: .center, spacing: 10) {
            compactIdentity
            Spacer(minLength: 8)
            CompactBalanceStrip(metric: metric)
            Spacer(minLength: 8)
            HStack(spacing: 8) {
                Text(CompactUsageCardPresentation.balanceStatusText(healthState: metric.healthState))
                    .font(.system(size: 10, weight: .medium, design: .monospaced))
                    .foregroundStyle(CompactPopoverPalette.badgeGreen(colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background {
                        Capsule()
                            .fill(Color.green.opacity(0.10))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(Color.green.opacity(0.20), lineWidth: 1)
                    }
                compactActionButtons
            }
        }
    }

    private func compactHeader(showsDivider: Bool) -> some View {
        HStack(alignment: .center, spacing: 10) {
            compactIdentity
            Spacer(minLength: 8)
            compactActionButtons
        }
        .padding(.bottom, showsDivider ? 10 : 0)
        .overlay(alignment: .bottom) {
            if showsDivider {
                Rectangle()
                    .fill(CompactPopoverPalette.hairline(colorScheme))
                    .frame(height: 1)
                    .allowsHitTesting(false)
            }
        }
    }

    private var compactIdentity: some View {
        HStack(alignment: .center, spacing: 10) {
            avatarWithStatus

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(state.configuration.displayName)
                        .font(.system(size: 13, weight: .semibold))
                        .lineLimit(1)

                    if CompactUsageCardPresentation.isNearlyExhausted(metrics: compactOrderedMetrics) {
                        Text("即将耗尽")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(Color.red)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 1)
                            .background {
                                RoundedRectangle(cornerRadius: 4, style: .continuous)
                                    .fill(Color.red.opacity(0.10))
                            }
                            .overlay {
                                RoundedRectangle(cornerRadius: 4, style: .continuous)
                                    .strokeBorder(Color.red.opacity(0.20), lineWidth: 1)
                            }
                    }
                }

                Text(UsageRowPresentation.subscriptionDescription(
                    providerID: state.configuration.providerID,
                    planName: state.snapshot?.planName ?? ""
                ))
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                .lineLimit(2)
            }
        }
    }

    private var compactActionButtons: some View {
        HStack(spacing: 6) {
            if onShare != nil {
                CompactCardActionButton(
                    action: { onShare?() },
                    help: "分享 \(state.configuration.displayName) 当前用量",
                    disabled: state.snapshot == nil
                ) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 11, weight: .semibold))
                }
                .accessibilityLabel("分享 \(state.configuration.displayName) 当前用量")
            }
            CompactCardActionButton(
                action: refreshCredential,
                help: "刷新 \(state.configuration.displayName)",
                disabled: state.isRefreshing || !state.configuration.isEnabled
            ) {
                Group {
                    if state.isRefreshing {
                        ProgressView()
                            .controlSize(.mini)
                    } else {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 11, weight: .semibold))
                    }
                }
            }
            .accessibilityLabel("刷新 \(state.configuration.displayName)")
        }
    }

    private var compactOrderedMetrics: [NormalizedUsageMetric] {
        guard let snapshot = state.snapshot else { return [] }
        return UsageCardDensityPolicy.orderedMetrics(
            providerID: state.configuration.providerID,
            metadata: state.configuration.metadata,
            metrics: snapshot.normalizedMetrics
        )
    }
}

/// 刷新时的流星边框：发光亮点沿卡片边框匀速环绕，身后拖出渐隐尾迹。
private struct RefreshingCardBorder: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let color: Color

    private let orbitPeriod: Double = 2.4
    private let tailFraction: CGFloat = 0.22
    private let tailSegments = 4

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0, paused: reduceMotion)) { timeline in
            let phase = reduceMotion
                ? CGFloat(0.72)
                : CGFloat(
                    (timeline.date.timeIntervalSinceReferenceDate / orbitPeriod)
                        .truncatingRemainder(dividingBy: 1)
                )

            GeometryReader { geometry in
                let path = RoundedRectangle(
                    cornerRadius: CompactPopoverMetrics.cardCornerRadius,
                    style: .continuous
                )
                .path(in: CGRect(origin: .zero, size: geometry.size))

                ZStack {
                    ForEach(0..<tailSegments, id: \.self) { index in
                        cometSegment(path: path, head: phase, index: index)
                    }

                    Circle()
                        .fill(color)
                        .frame(width: 5, height: 5)
                        .shadow(color: color, radius: 5)
                        .position(cometHead(path: path, at: phase))
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private func cometHead(path: Path, at phase: CGFloat) -> CGPoint {
        // Path 没有 point(at:)，用极短片段的终点近似头部位置。
        let epsilon: CGFloat = 0.0008
        let head = path.trimmedPath(from: phase, to: min(phase + epsilon, 1))
        return head.currentPoint ?? path.currentPoint ?? .zero
    }

    private func cometSegment(path: Path, head: CGFloat, index: Int) -> some View {
        let fade = 1 - CGFloat(index) / CGFloat(tailSegments)
        let end = head - tailFraction * (1 - fade)
        let start = end - tailFraction / CGFloat(tailSegments)

        return segmentPath(path, from: start, to: end)
            .stroke(
                color.opacity(Double(fade) * 0.9),
                style: StrokeStyle(lineWidth: 2.4 * fade + 0.4, lineCap: .round)
            )
    }

    private func segmentPath(_ path: Path, from start: CGFloat, to end: CGFloat) -> Path {
        if start < 0 {
            return path
                .trimmedPath(from: start + 1, to: 1)
                .union(path.trimmedPath(from: 0, to: end))
        }
        return path.trimmedPath(from: start, to: end)
    }
}

private struct UsageCardChrome: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme

    let state: KeyUsageState
    let isExpired: Bool

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: CompactPopoverMetrics.cardCornerRadius, style: .continuous)
        let highlight = CompactPopoverPalette.cardStroke(colorScheme)

        Group {
            if #available(macOS 26.0, *) {
                content
                    .glassEffect(
                        .regular.tint(CompactPopoverPalette.cardSurfaceTint(colorScheme)),
                        in: shape
                    )
            } else {
                content
                    .background {
                        shape
                            .fill(.ultraThinMaterial)
                            .overlay {
                                shape.fill(
                                    colorScheme == .dark
                                        ? CompactPopoverPalette.darkSurface.opacity(0.50)
                                        : Color.white.opacity(0.42)
                                )
                            }
                            .allowsHitTesting(false)
                    }
            }
        }
        .overlay {
            shape
                .strokeBorder(highlight, lineWidth: 1)
                .allowsHitTesting(false)
        }
        .overlay {
            if state.isRefreshing {
                RefreshingCardBorder(
                    color: ProviderTheme.accentColor(for: state.configuration.providerID)
                )
                .allowsHitTesting(false)
            }
        }
        .shadow(color: Color.black.opacity(0.04), radius: 8, y: 2)
        .saturation(state.configuration.isEnabled ? 1 : 0)
        .opacity(isExpired ? 0.45 : 1)
    }
}

enum UsageRowAccessibility {
    static func label(
        state: KeyUsageState,
        metric: UsageMetric?,
        dimension: DisplayDimension,
        now: Date = .now
    ) -> String {
        let prefix = "\(state.configuration.displayName)，"
        let summary: String
        if state.isRefreshing || (state.error != nil && metric != nil) {
            summary = prefix + UsageFormatter.statusText(state: state, dimension: dimension)
        } else if state.isStale, metric != nil {
            summary = prefix + UsageFormatter.statusText(state: state, dimension: dimension)
                + "，当前显示上次成功数据"
        } else if state.isRefreshing, state.snapshot == nil {
            summary = prefix + "正在加载"
        } else if state.error == .noSubscription {
            summary = prefix + UsageFormatter.statusText(state: state, dimension: dimension)
        } else if state.error != nil, state.snapshot == nil {
            summary = prefix + UsageFormatter.statusText(state: state, dimension: dimension)
                + "，没有缓存"
        } else if let metric,
                  metric.percent.isFinite {
            summary = prefix + "已使用 \(UsageFormatter.displayPercentText(metric.percent))，\(UsageFormatter.fullAmount(metric))"
        } else {
            summary = prefix + UsageFormatter.statusText(state: state, dimension: dimension)
        }

        guard let snapshot = state.snapshot else { return summary }
        var details: [String] = []
        if snapshot.subscriptionStartAt != nil {
            details.append("订阅开始 " + UsageFormatter.subscriptionDateText(snapshot.subscriptionStartAt))
        }
        if snapshot.subscriptionEndAt != nil {
            details.append("订阅结束 " + UsageFormatter.subscriptionDateText(snapshot.subscriptionEndAt))
        }
        if let expiryText = UsageFormatter.subscriptionExpiryText(
            until: snapshot.subscriptionEndAt,
            now: now
        ) {
            details.append(expiryText)
        }
        if snapshot.kind == .periodic, state.configuration.providerID != .newAPI,
           snapshot.fiveHour != nil || snapshot.weekly != nil {
            details.append("5 小时剩余 \(remainingDuration(for: snapshot.fiveHour, now: now))")
            details.append("周剩余 \(remainingDuration(for: snapshot.weekly, now: now))")
        }
        return ([summary] + details).joined(separator: "，")
    }

    static func hint() -> String {
        ""
    }

    private static func remainingDuration(for metric: UsageMetric?, now: Date) -> String {
        guard let end = metric?.windowEnd else { return "—" }
        return UsageFormatter.remainingDurationText(until: end, now: now)
    }
}

enum UsageRowPresentation {
    static func subscriptionDescription(providerID: ProviderID, planName: String) -> String {
        let providerName = ProviderRegistry.builtInDescriptors
            .first(where: { $0.id == providerID })?
            .displayName ?? providerID.rawValue
        guard !planName.isEmpty else {
            return providerName
        }
        return "\(providerName) · \(planName)"
    }
}

private extension UsageRowView {
    func headerView(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .center, spacing: 10) {
                compactIdentity
                Spacer(minLength: 8)
                compactActionButtons
            }

            if fullHeaderShowsSummary {
                HStack(alignment: .top, spacing: 8) {
                    subscriptionPeriodDetails
                        .frame(maxWidth: .infinity, alignment: .leading)

                    VStack(alignment: .trailing, spacing: 3) {
                        if let metric = validMetric(state.snapshot?.token),
                           metric.percent.isFinite {
                            Text(UsageFormatter.displayPercentText(metric.percent))
                                .font(.system(.headline, design: .rounded, weight: .semibold))
                                .monospacedDigit()
                                .foregroundStyle(progressColor(for: metric))
                        }

                        if let metric = state.snapshot?.normalizedMetrics.first,
                           metric.presentation != .progress,
                           state.configuration.providerID != .deepseek,
                           state.configuration.providerID != .newAPI,
                           state.configuration.providerID != .xiaomi {
                            normalizedHeaderMetric(metric)
                        }
                    }
                }
            }
        }
    }

    private var fullHeaderShowsSummary: Bool {
        if validMetric(state.snapshot?.token) != nil {
            return true
        }
        if state.snapshot?.metrics.isEmpty == false {
            return true
        }
        if let snapshot = state.snapshot,
           snapshot.kind == .periodic,
           snapshot.subscriptionStartAt != nil || snapshot.subscriptionEndAt != nil {
            return true
        }
        return false
    }

    @ViewBuilder
    var subscriptionPeriodDetails: some View {
        if density == .full {
            if let snapshot = state.snapshot,
               snapshot.kind == .periodic,
               snapshot.subscriptionStartAt != nil || snapshot.subscriptionEndAt != nil {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("开始 " + UsageFormatter.subscriptionDateText(snapshot.subscriptionStartAt))
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("结束 " + UsageFormatter.subscriptionDateText(snapshot.subscriptionEndAt))
                        .fixedSize(horizontal: false, vertical: true)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .monospacedDigit()
            }
        }
    }

    @ViewBuilder
    func normalizedHeaderMetric(_ metric: NormalizedUsageMetric) -> some View {
        switch metric.presentation {
        case .progress:
            if let used = metric.used, let limit = metric.limit, limit > 0 {
                let percent = NSDecimalNumber(decimal: used)
                    .dividing(by: NSDecimalNumber(decimal: limit))
                    .multiplying(by: 100)
                    .doubleValue
                Text(UsageFormatter.displayPercentText(percent))
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(normalizedMetricColor(metric.healthState))
            }
        case .balance:
            Text("余额 \(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))")
                .font(.caption.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(normalizedMetricColor(metric.healthState))
        case .status:
            Text(metric.healthState == .unavailable ? "不可用" : "可用")
                .font(.caption.weight(.semibold))
                .foregroundStyle(normalizedMetricColor(metric.healthState))
        case .value:
            EmptyView()
        }
    }

    func decimalText(_ value: Decimal?) -> String {
        UsageFormatter.numberText(value)
    }

    func normalizedMetricColor(_ state: UsageMetricHealthState) -> Color {
        switch state {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        }
    }

    @ViewBuilder
    func content(now: Date) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 7) {
                if !snapshot.metrics.isEmpty {
                    normalizedMetricsContent(snapshot: snapshot, now: now)
                } else {
                    switch snapshot.kind {
                    case .periodic:
                        periodicContent(snapshot: snapshot, now: now)
                    case .tokenPack:
                        if let metric = validMetric(snapshot.token) {
                            metricContent(title: "Token", metric: metric, dimension: .token, now: now)
                        } else {
                            statusLabel
                        }
                    }
                }

            }
        } else {
            statusLabel
        }
    }

    @ViewBuilder
    func compactMetrics(snapshot: UsageSnapshot, now: Date) -> some View {
        let metrics = UsageCardDensityPolicy.orderedMetrics(
            providerID: state.configuration.providerID,
            metadata: state.configuration.metadata,
            metrics: snapshot.normalizedMetrics
        )
        let arrangement = CompactUsageCardPresentation.arrangement(for: metrics)

        if !metrics.isEmpty {
            switch arrangement {
            case .balance:
                EmptyView()
            case .horizontalGauges:
                HStack(alignment: .top, spacing: 10) {
                    ForEach(metrics) { metric in
                        compactMetricRow(metric, now: now)
                    }
                }
            case .verticalGauges:
                HStack(alignment: .top, spacing: 8) {
                    ForEach(metrics) { metric in
                        CompactMetricGaugeTile(
                            metric: metric,
                            now: now,
                            style: .vertical,
                            providerID: state.configuration.providerID
                        )
                    }
                }
            case .valueTiles:
                LazyVGrid(
                    columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)],
                    spacing: 8
                ) {
                    ForEach(metrics) { metric in
                        if metric.presentation == .progress {
                            CompactMetricGaugeTile(
                                metric: metric,
                                now: now,
                                style: metrics.count > 2 ? .vertical : .horizontal,
                                providerID: state.configuration.providerID
                            )
                        } else {
                            CompactValueTile(metric: metric, valueText: compactValueText(for: metric))
                        }
                    }
                }
            }
        } else if snapshot.kind == .periodic {
            HStack(alignment: .top, spacing: 10) {
                compactLegacyMetricRow(title: "5 小时", metric: validMetric(snapshot.fiveHour), now: now)
                compactLegacyMetricRow(title: "周", metric: validMetric(snapshot.weekly), now: now)
            }
        } else if let token = validMetric(snapshot.token) {
            compactLegacyMetricRow(title: "Token", metric: token, now: now)
        } else {
            statusLabel
        }
    }

    func compactMetricRow(_ metric: NormalizedUsageMetric, now: Date) -> some View {
        CompactMetricGaugeTile(
            metric: metric,
            now: now,
            style: .horizontal,
            providerID: state.configuration.providerID
        )
    }

    func compactLegacyMetricRow(
        title: String,
        metric: UsageMetric?,
        now: Date
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Spacer(minLength: 4)

                if let metric {
                    Text(UsageFormatter.displayPercentText(metric.percent))
                        .font(.system(.caption, design: .rounded, weight: .semibold))
                        .foregroundStyle(progressColor(for: metric))
                        .monospacedDigit()
                }
            }

            if let metric {
                UsageMetricProgressBar(percent: metric.percent)

                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric, now: now))")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
        }
    }

    func compactValueText(for metric: NormalizedUsageMetric) -> String {
        switch metric.presentation {
        case .balance:
            return UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)
        case .value:
            return metric.unit == .token
                ? UsageFormatter.exactTokenText(metric.value)
                : UsageFormatter.numberText(metric.value, grouping: true)
        case .status:
            return metric.healthState == .unavailable ? "不可用" : "可用"
        case .progress:
            return ""
        }
    }

    func compactAccessibilityText(for metric: NormalizedUsageMetric, now: Date) -> String {
        if metric.presentation == .progress {
            let percent = UsageFormatter.displayPercentText(metric.displayedPercent)
            let reset = metric.windowEnd.map {
                "，重置 \(UsageFormatter.resetTime($0, now: now))"
            } ?? ""
            return "\(metric.label)，已使用 \(percent)\(reset)"
        }
        return "\(metric.label)，\(compactValueText(for: metric))"
    }

    @ViewBuilder
    func periodicContent(snapshot: UsageSnapshot, now: Date) -> some View {
        HStack(alignment: .top, spacing: 16) {
            metricContent(
                title: "5 小时",
                metric: validMetric(snapshot.fiveHour),
                dimension: .fiveHour,
                now: now
            )
            metricContent(
                title: "周",
                metric: validMetric(snapshot.weekly),
                dimension: .weekly,
                now: now
            )
        }
    }

    func normalizedMetricsContent(snapshot: UsageSnapshot, now: Date) -> some View {
        if state.configuration.providerID == .xiaomi,
           state.configuration.metadata["usageKind"] != "plan" {
            return AnyView(
                XiaomiAPIMetricsView(metrics: snapshot.normalizedMetrics)
            )
        }
        if state.configuration.providerID == .commandCode {
            return AnyView(
                CommandCodeUsageMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    now: now,
                    displayMode: .card
                )
            )
        }

        if state.configuration.providerID == .glm {
            return AnyView(
                GLMUsageMetricsView(metrics: snapshot.normalizedMetrics, now: now)
            )
        }

        if state.configuration.providerID == .newAPI {
            return AnyView(
                NewAPIUsageMetricsView(metrics: snapshot.normalizedMetrics, now: now)
            )
        }

        if state.configuration.providerID == .volcengine {
            if state.configuration.metadata["planType"] == "coding" {
                return AnyView(
                    VolcengineCodingPlanMetricsView(
                        metrics: snapshot.normalizedMetrics,
                        now: now
                    )
                )
            }
            return AnyView(
                VolcenginePlanUsageMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    now: now
                )
            )
        }

        let layout = UsageMetricGridPolicy.layout(
            providerID: state.configuration.providerID,
            metrics: snapshot.normalizedMetrics
        )
        return AnyView(
            NormalizedUsageMetricGrid(
                metrics: layout.metrics,
                columns: layout.columns,
                resetTimeStyle: .relativeDuration,
                now: now
            )
        )
    }

    @ViewBuilder
    func metricContent(
        title: String,
        metric: UsageMetric?,
        dimension: UsageDimension,
        now: Date
    ) -> some View {
        if let metric {
            VStack(alignment: .leading, spacing: 5) {
                HStack {
                    Text(title)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 4)
                    Text(UsageFormatter.displayPercentText(metric.percent))
                        .font(.system(.headline, design: .rounded, weight: .semibold))
                        .foregroundStyle(progressColor(for: metric))
                        .monospacedDigit()
                }

                UsageMetricProgressBar(metric: metric)

                if density == .full {
                    Text(UsageFormatter.amount(metric))
                        .help(UsageFormatter.fullAmount(metric))
                    Text("剩余 \(UsageFormatter.remaining(metric))")
                }
                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric))")
                    if density == .full {
                        Text("剩余 \(remainingDuration(for: metric, now: now))")
                            .foregroundStyle(
                                UsageFormatter.shouldHighlightRemainingDuration(
                                    for: metric,
                                    dimension: dimension,
                                    now: now
                                ) ? Color.green : Color.secondary
                            )
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .font(.caption2)
            .foregroundStyle(.secondary)
            .monospacedDigit()
            .textSelection(.enabled)
        } else {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                Text("—")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .font(.caption)
            .foregroundStyle(.secondary)
        }
    }

    func remainingDuration(for metric: UsageMetric?, now: Date) -> String {
        guard let end = metric?.windowEnd else {
            return "—"
        }
        return UsageFormatter.remainingDurationText(until: end, now: now)
    }

    func isSubscriptionExpired(now: Date) -> Bool {
        guard let end = state.snapshot?.subscriptionEndAt else {
            return false
        }
        return end <= now
    }

    @ViewBuilder
    var providerNameLabel: some View {
        let providerName = ProviderRegistry.builtInDescriptors
            .first(where: { $0.id == state.configuration.providerID })?
            .displayName ?? state.configuration.providerID.rawValue

        if let websiteURL = state.configuration.websiteURL {
            Link(providerName, destination: websiteURL)
                .help("打开 \(providerName) 官网")
                .accessibilityLabel("打开 \(providerName) 官网")
        } else {
            Text(providerName)
        }
    }

    /// 失败徽章挂在别名头像右上角，点击头像即可查看失败详情。
    @ViewBuilder
    var avatarWithStatus: some View {
        let avatar = CompactAccountAvatar(
            letter: CompactUsageCardPresentation.avatarLetter(
                displayName: state.configuration.displayName,
                providerID: state.configuration.providerID
            ),
            providerID: state.configuration.providerID
        )

        if state.error != nil {
            Button {
                showsFailureDetails = true
            } label: {
                avatar
                    .overlay(alignment: .topTrailing) {
                        refreshFailureIndicator
                    }
                    .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .popover(isPresented: $showsFailureDetails, arrowEdge: .bottom) {
                RefreshFailurePopover(
                    state: state,
                    retry: {
                        showsFailureDetails = false
                        retryCredential()
                    }
                )
            }
            .help(UsageFormatter.refreshFailureTooltip(state: state))
            .accessibilityLabel("查看刷新失败详情")
        } else {
            avatar
        }
    }

    var refreshFailureIndicator: some View {
        Image(systemName: "exclamationmark")
            .font(.system(size: 7, weight: .black))
            .foregroundStyle(.white)
            .frame(width: 12, height: 12)
            .background {
                Circle()
                    .fill(Color(red: 1.0, green: 0.28, blue: 0.34))
            }
            .overlay {
                Circle()
                    .strokeBorder(.white.opacity(0.85), lineWidth: 1)
            }
            .offset(x: 5, y: -5)
    }

    func subscriptionDescriptionText(for snapshot: UsageSnapshot) -> String {
        UsageRowPresentation.subscriptionDescription(
            providerID: state.configuration.providerID,
            planName: snapshot.planName
        )
    }

    func validMetric(_ metric: UsageMetric?) -> UsageMetric? {
        guard let metric,
              UsageFormatter.percentText(metric) != nil else {
            return nil
        }
        return metric
    }

    func progressColor(for metric: UsageMetric) -> Color {
        UsageMetricPresentation.color(for: metric.percent)
    }

    @ViewBuilder
    var statusLabel: some View {
        if !state.isRefreshing, state.error == nil {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "clock"
            )
        }
    }

    func accessibilityLabel(now: Date) -> String {
        if let snapshot = state.snapshot, !snapshot.metrics.isEmpty {
            let metricText = snapshot.normalizedMetrics.map { metric in
                if metric.presentation == .balance {
                    return "余额 \(UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode))"
                }
                if let percent = metric.displayedPercent {
                    return "\(metric.label) \(metric.displaysRemainingPercent ? "剩余" : "已使用") \(UsageFormatter.displayPercentText(percent))"
                }
                return metric.label
            }.joined(separator: "，")
            return "\(state.configuration.displayName)，\(metricText)"
        }
        return UsageRowAccessibility.label(
            state: state,
            metric: validMetric(state.snapshot?.fiveHour) ?? validMetric(state.snapshot?.token),
            dimension: .fiveHour,
            now: now
        )
    }

    var accessibilityHint: String {
        UsageRowAccessibility.hint()
    }
}

private struct RefreshFailurePopover: View {
    let state: KeyUsageState
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("刷新失败", systemImage: "exclamationmark.triangle.fill")
                .font(.headline)
                .foregroundStyle(.orange)

            Text(failureReason)
                .font(.callout)
                .fixedSize(horizontal: false, vertical: true)

            Text(dataSourceText)
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack {
                Spacer()
                Button(action: retry) {
                    Label("重试", systemImage: "arrow.clockwise")
                }
                .liquidGlassButton(prominent: true)
                .disabled(state.isRefreshing)
                .accessibilityLabel("重试刷新 \(state.configuration.displayName)")
            }
        }
        .padding(16)
        .frame(width: 320)
    }

    private var failureReason: String {
        if let message = state.failureMessage, !message.isEmpty {
            return message
        }
        guard let error = state.error else {
            return "未知错误"
        }
        return UsageFormatter.errorText(error)
    }

    private var dataSourceText: String {
        guard let lastSuccessAt = state.lastSuccessAt, state.snapshot != nil else {
            return "暂无可用缓存，重试将重新请求用量数据。"
        }
        return "当前显示 \(lastSuccessAt.formatted(date: .abbreviated, time: .shortened)) 的上次成功数据。"
    }
}
