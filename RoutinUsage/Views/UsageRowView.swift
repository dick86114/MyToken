import SwiftUI

@MainActor
struct UsageRowView: View {
    @Environment(\.menuBarColorRules) private var menuBarColorRules
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let state: KeyUsageState
    var density: UsageCardDensity = .full
    var actions: AnyView?
    var refreshCredential: () -> Void = {}
    var retryCredential: () -> Void = {}
    var onShare: (() -> Void)? = nil

    @State private var showsFailureDetails = false
    @State private var isDetailsExpanded = false

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
        VStack(alignment: .leading, spacing: 6) {
            headerView(now: now)

            content(now: now)
                .padding(.leading, CompactPopoverMetrics.fullContentIndent)
                .contentShape(Rectangle())
                .onTapGesture {
                    toggleDetails()
                }

            fullCycleNote(now: now)
                .padding(.leading, CompactPopoverMetrics.fullContentIndent)
                .contentShape(Rectangle())
                .onTapGesture {
                    toggleDetails()
                }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, CompactPopoverMetrics.fullCardHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityAction(
            named: isDetailsExpanded ? "收起更多信息" : "展开更多信息"
        ) {
            toggleDetails()
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }

    private func compactCard(now: Date) -> some View {
        let metrics = compactOrderedMetrics
        let arrangement = CompactUsageCardPresentation.arrangement(for: metrics)

        return Group {
            if arrangement == .balance, let metric = metrics.first {
                let compositionMetrics = Self.balanceCompositionMetrics(
                    in: state.snapshot?.normalizedMetrics
                )
                compactBalanceCard(
                    metric: metric,
                    compositionMetrics: compositionMetrics,
                    style: .compact
                )
            } else {
                compactStandardCard(now: now)
            }
        }
        .padding(.horizontal, CompactPopoverMetrics.cardHorizontalPadding)
        .padding(.vertical, CompactPopoverMetrics.cardVerticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }

    private func compactStandardCard(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            compactHeader()

            if let snapshot = state.snapshot {
                compactMetrics(snapshot: snapshot, now: now)
                    .padding(.leading, CompactPopoverMetrics.contentIndent)
            } else {
                statusLabel
                    .padding(.leading, CompactPopoverMetrics.contentIndent)
            }
        }
    }

    private func compactBalanceCard(
        metric: NormalizedUsageMetric,
        compositionMetrics: [NormalizedUsageMetric],
        style: BalanceSummaryStyle
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            compactHeader()
            CompactBalanceStrip(
                metric: metric,
                compositionMetrics: compositionMetrics,
                style: style,
                metaText: style == .full ? "账户可用 · 余额不足时自动提醒" : nil
            )
            .padding(.leading, style == .compact
                ? CompactPopoverMetrics.contentIndent
                : CompactPopoverMetrics.fullContentIndent)
        }
    }

    private func compactHeader() -> some View {
        HStack(alignment: .center, spacing: 10) {
            compactIdentity()
            Spacer(minLength: 8)
            compactActionButtons
        }
        .padding(.bottom, 8)
    }

    private func compactIdentity(
        avatarSize: CGFloat = CompactPopoverMetrics.avatarSize,
        onTitleTap: (() -> Void)? = nil
    ) -> some View {
        return HStack(alignment: .center, spacing: 10) {
            avatarWithStatus(size: avatarSize)
                .contentShape(Rectangle())
                .onTapGesture {
                    onTitleTap?()
                }

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(state.configuration.displayName)
                        .font(.system(size: 13, weight: .semibold))
                        .tracking(-0.01)
                        .lineLimit(1)

                    statusDot
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    onTitleTap?()
                }

                Group {
                    providerSubtitle()
                }
                .font(.system(size: 11))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                .lineLimit(1)
            }
        }
    }

    private var compactActionButtons: some View {
        HStack(spacing: 2) {
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
                            .font(.system(size: 12, weight: .semibold))
                    }
                }
            }
            .accessibilityLabel("刷新 \(state.configuration.displayName)")

            if onShare != nil {
                CompactCardActionButton(
                    action: { onShare?() },
                    help: "分享 \(state.configuration.displayName) 当前用量",
                    disabled: state.snapshot == nil
                ) {
                    Image(systemName: "arrow.up.right.square")
                        .font(.system(size: 12, weight: .medium))
                }
                .accessibilityLabel("分享 \(state.configuration.displayName) 当前用量")
            }
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

    private static func balanceCompositionMetrics(
        in metrics: [NormalizedUsageMetric]?
    ) -> [NormalizedUsageMetric] {
        guard let metrics else { return [] }
        return ["toppedUpBalance", "grantedBalance", "cash-balance", "gift-balance"].compactMap { metricID in
            metrics.first { $0.id == metricID && ($0.value ?? 0) > 0 }
        }
    }
}

/// 刷新时的流星边框：发光亮点沿卡片边框匀速环绕，身后拖出渐隐尾迹。
private struct RefreshingCardBorder: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let color: Color

    private let orbitPeriod: Double = 2.4
    private let tailFraction: CGFloat = 0.22
    private let tailSegments = 4
    private let isLowPowerModeEnabled = ProcessInfo.processInfo.isLowPowerModeEnabled

    var body: some View {
        let isPaused = RefreshAnimationPolicy.isPaused(
            reduceMotion: reduceMotion,
            lowPowerModeEnabled: isLowPowerModeEnabled
        )
        TimelineView(.animation(minimumInterval: RefreshAnimationPolicy.minimumInterval, paused: isPaused)) { timeline in
            let phase = isPaused
                ? CGFloat(0.72)
                : RefreshAnimationPolicy.phase(for: timeline.date, orbitPeriod: orbitPeriod)

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
                        .shadow(color: color.opacity(0.22), radius: 2)
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
            var wrapped = Path()
            wrapped.addPath(path.trimmedPath(from: start + 1, to: 1))
            wrapped.addPath(path.trimmedPath(from: 0, to: end))
            return wrapped
        }
        return path.trimmedPath(from: start, to: end)
    }
}

private struct ProviderWebsiteLink: View {
    let providerName: String
    let subtitle: String
    let destination: URL
    let colorScheme: ColorScheme

    @State private var isHovering = false

    var body: some View {
        Link(destination: destination) {
            HStack(spacing: 2) {
                Text(subtitle)
                    .lineLimit(1)

                if isHovering {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 7, weight: .semibold))
                        .transition(.opacity)
                }
            }
            .foregroundStyle(
                isHovering
                    ? CompactPopoverPalette.brand(colorScheme)
                    : CompactPopoverPalette.subtitle(colorScheme)
            )
        }
        .contentShape(Rectangle())
        .help("打开 \(providerName) 官网")
        .accessibilityLabel("打开 \(providerName) 官网")
        .animation(.easeOut(duration: 0.14), value: isHovering)
        .onHover { hovering in
            isHovering = hovering
            if hovering {
                NSCursor.pointingHand.push()
            } else {
                NSCursor.pop()
            }
        }
        .onDisappear {
            guard isHovering else { return }
            isHovering = false
            NSCursor.pop()
        }
    }
}

private struct UsageCardChrome: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme

    let state: KeyUsageState
    let isExpired: Bool

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .outer), style: .continuous)
        let material = PopoverVisualPolicy.material(for: .card)
        let highlight = CompactPopoverPalette.cardStroke(colorScheme)

        Group {
            switch material {
            case .solid:
                content.background {
                    shape.fill(CompactPopoverPalette.surface(.card, colorScheme))
                }
            case .windowGlass, .modalGlass:
                content.background(shape.fill(CompactPopoverPalette.surface(.card, colorScheme)))
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
                    color: CompactPopoverPalette.brand(colorScheme)
                )
                .allowsHitTesting(false)
            }
        }
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
            details.append(
                UsageRowPresentation.subscriptionEndLabel(
                    providerID: state.configuration.providerID,
                    billingMode: snapshot.billingMode
                ) + " " + UsageFormatter.subscriptionDateText(snapshot.subscriptionEndAt)
            )
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
    static func providerName(for providerID: ProviderID) -> String {
        ProviderRegistry.builtInDescriptors
            .first(where: { $0.id == providerID })?
            .displayName ?? providerID.rawValue
    }

    static func subscriptionDescription(providerID: ProviderID, planName: String) -> String {
        let providerName = providerName(for: providerID)
        guard !planName.isEmpty else {
            return providerName
        }
        return "\(providerName) · \(planName)"
    }

    static func accountDescription(
        configuration: KeyConfiguration,
        snapshot: UsageSnapshot?
    ) -> String {
        let providerName = providerName(for: configuration.providerID)
        var parts = [providerName]

        if let snapshot {
            var planName = snapshot.planName.trimmingCharacters(in: .whitespacesAndNewlines)
            if planName.lowercased().hasPrefix(providerName.lowercased()) {
                planName = String(planName.dropFirst(providerName.count))
                    .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines.union(.punctuationCharacters))
            }
            if !planName.isEmpty, planName != providerName {
                parts.append(planName)
            }

            if let billingMode = snapshot.billingMode?
                .trimmingCharacters(in: .whitespacesAndNewlines),
               !billingMode.isEmpty,
               !parts.contains(billingMode),
               shouldAppendBillingMode(for: configuration) {
                parts.append(billingMode)
            }

            if configuration.providerID == .deepseek || configuration.providerID == .xiaomi,
               let currencyCode = snapshot.normalizedMetrics
                .first(where: { $0.presentation == .balance })?
                .currencyCode,
               !currencyCode.isEmpty {
                parts.append(currencyCode)
            }
        }

        if parts.count == 1 {
            if let fallback = fallbackPlanDescription(configuration: configuration) {
                parts.append(fallback)
            }
        }

        return parts.joined(separator: " · ")
    }

    static func cycleNote(
        configuration: KeyConfiguration,
        snapshot: UsageSnapshot?
    ) -> String? {
        guard let snapshot else { return nil }
        let start = snapshot.subscriptionStartAt
        let end = snapshot.subscriptionEndAt

        switch configuration.providerID {
        case .xiaomi where configuration.metadata["usageKind"] == "plan":
            guard start != nil || end != nil else { return nil }
            return "套餐周期 \(cycleDateText(start)) – \(cycleDateText(end))"
        case .opencode:
            guard let end else { return nil }
            let cancelled = snapshot.billingMode == "取消续订"
            return "\(UsageRowPresentation.subscriptionEndLabel(providerID: .opencode, billingMode: snapshot.billingMode)) \(cycleDateText(end)) · \(cancelled ? "已取消续订" : "当前未取消")"
        case .volcengine:
            guard start != nil || end != nil else { return nil }
            return "订阅 \(cycleDateText(start)) – \(cycleDateText(end))"
        case .commandCode, .deepseek, .glm, .newAPI, .routin, .xiaomi:
            return nil
        }
    }

    private static func fallbackPlanDescription(configuration: KeyConfiguration) -> String? {
        switch configuration.providerID {
        case .deepseek:
            return "API 余额"
        case .xiaomi:
            return configuration.metadata["usageKind"] == "plan" ? "Token Plan" : "API 按量"
        case .volcengine:
            return configuration.metadata["planType"] == "coding" ? "Coding Plan" : "Agent Plan"
        case .newAPI:
            return "自建服务"
        case .routin:
            return configuration.metadata["usageKind"] == "tokenPack" ? "Token Pack" : "Plan Key"
        case .glm, .commandCode, .opencode:
            return nil
        }
    }

    private static func shouldAppendBillingMode(for configuration: KeyConfiguration) -> Bool {
        switch configuration.providerID {
        case .opencode, .volcengine:
            return true
        case .xiaomi:
            return configuration.metadata["usageKind"] == "plan"
        case .routin, .deepseek, .glm, .newAPI, .commandCode:
            return false
        }
    }

    private static func cycleDateText(_ date: Date?) -> String {
        guard let date else { return "—" }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "MM/dd"
        return formatter.string(from: date)
    }

    static func subscriptionEndLabel(
        providerID: ProviderID,
        billingMode: String?
    ) -> String {
        guard providerID == .opencode else { return "订阅结束" }
        return billingMode == "取消续订" ? "到期" : "续费"
    }
}

private extension UsageRowView {
    func headerView(now: Date) -> some View {
        HStack(alignment: .center, spacing: 10) {
            compactIdentity(
                avatarSize: CompactPopoverMetrics.fullAvatarSize,
                onTitleTap: { toggleDetails() }
            )
            Spacer(minLength: 8)
            if hasExpandableDetails {
                Image(systemName: "chevron.down")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.tertiary)
                    .rotationEffect(.degrees(isDetailsExpanded ? 180 : 0))
                    .frame(width: 20, height: 24)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        toggleDetails()
                    }
                    .accessibilityHidden(true)
            }
            if let actions {
                actions
            }
            compactActionButtons
        }
    }

    private var hasExpandableDetails: Bool {
        guard let metrics = state.snapshot?.normalizedMetrics else { return false }
        switch state.configuration.providerID {
        case .glm:
            return metrics.contains { metric in
                metric.id == "model-calls"
                    || metric.id == "zcode-mcp"
                    || metric.id.hasPrefix("activity-")
            }
        case .newAPI:
            return metrics.contains { metric in
                metric.id == "rpm"
                    || metric.id == "tpm"
                    || metric.id == "request-count"
            }
        case .xiaomi:
            return state.configuration.metadata["usageKind"] == "plan"
                && metrics.contains { $0.id == "plan-status" }
        case .routin, .deepseek, .volcengine, .commandCode, .opencode:
            return false
        }
    }

    private func toggleDetails() {
        guard density == .full, hasExpandableDetails else { return }
        withAnimation(
            reduceMotion
                ? .easeOut(duration: 0.16)
                : .spring(response: 0.34, dampingFraction: 0.88)
        ) {
            isDetailsExpanded.toggle()
        }
    }

    @ViewBuilder
    private func expandedPlanStatus(_ metric: NormalizedUsageMetric) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(metric.label)
                .font(.system(size: 11))
                .foregroundStyle(.tertiary)

            Spacer(minLength: 8)

            Text(metric.healthState == .unavailable ? "不可用" : "可用")
                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                .foregroundStyle(
                    CompactPopoverPalette.healthColor(
                        for: metric.healthState,
                        colorScheme
                    )
                )
        }
        .transition(.opacity.combined(with: .move(edge: .top)))
    }

    @ViewBuilder
    func fullCycleNote(now: Date) -> some View {
        if let note = UsageRowPresentation.cycleNote(
            configuration: state.configuration,
            snapshot: state.snapshot
        ) {
            Text(note)
                .font(.system(size: 10))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .monospacedDigit()
        }
    }

    func decimalText(_ value: Decimal?) -> String {
        UsageFormatter.numberText(value)
    }

    @ViewBuilder
    func content(now: Date) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 7) {
                if !snapshot.metrics.isEmpty {
                    normalizedMetricsContent(snapshot: snapshot, now: now)
                    if isDetailsExpanded,
                       state.configuration.providerID == .xiaomi,
                       state.configuration.metadata["usageKind"] == "plan",
                       let status = snapshot.normalizedMetrics.first(where: { $0.id == "plan-status" }) {
                        expandedPlanStatus(status)
                    }
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
                compactMetricRow(metrics[0], now: now)
            case .verticalGauges:
                HStack(alignment: .top, spacing: 7) {
                    ForEach(metrics) { metric in
                        CompactMetricGaugeTile(
                            metric: metric,
                            now: now,
                            style: .vertical,
                        )
                        .frame(maxWidth: .infinity)
                    }
                }
            case .valueTiles:
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 10),
                        GridItem(.flexible(), spacing: 10)
                    ],
                    spacing: 7
                ) {
                    ForEach(metrics) { metric in
                        if metric.presentation == .progress {
                            CompactMetricGaugeTile(
                                metric: metric,
                                now: now,
                                style: .vertical,
                            )
                        } else {
                            CompactValueTile(metric: metric, valueText: compactValueText(for: metric))
                        }
                    }
                }
            }
        } else if snapshot.kind == .periodic {
            HStack(alignment: .top, spacing: 7) {
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
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)

                Spacer(minLength: 4)

                if let metric {
                    Text(UsageFormatter.displayPercentText(metric.percent))
                        .font(.system(size: 10, weight: .semibold, design: .monospaced))
                        .foregroundStyle(progressColor(for: metric))
                        .monospacedDigit()
                }
            }

            if let metric {
                UsageMetricProgressBar(percent: metric.percent)

                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric, now: now))")
                        .font(.system(size: 10))
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
        if state.configuration.providerID == .deepseek {
            return AnyView(
                DeepSeekMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    allowedModels: snapshot.allowedModels,
                    visualStyle: .popover
                )
            )
        }

        if state.configuration.providerID == .xiaomi,
           state.configuration.metadata["usageKind"] != "plan" {
            return AnyView(
                XiaomiAPIMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    visualStyle: .popover
                )
            )
        }
        if state.configuration.providerID == .commandCode {
            return AnyView(
                CommandCodeUsageMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    now: now,
                    displayMode: .details,
                    visualStyle: .popover
                )
            )
        }

        if state.configuration.providerID == .glm {
            return AnyView(
                GLMUsageMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    now: now,
                    showsSupplementaryMetrics: isDetailsExpanded,
                    visualStyle: .popover
                )
            )
        }

        if state.configuration.providerID == .newAPI {
            return AnyView(
                NewAPIUsageMetricsView(
                    metrics: snapshot.normalizedMetrics,
                    now: now,
                    visualStyle: .popover,
                    showsActivityDetails: isDetailsExpanded
                )
            )
        }

        if state.configuration.providerID == .volcengine {
            if state.configuration.metadata["planType"] == "coding" {
                return AnyView(
                    VolcengineCodingPlanMetricsView(
                        metrics: snapshot.normalizedMetrics,
                        now: now,
                        visualStyle: .popover
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
                now: now,
                showsRemainingDuration: layout.columns <= 2
                    && state.configuration.providerID != .xiaomi,
                visualStyle: .popover
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
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 4)
                    Text(UsageFormatter.displayPercentText(metric.percent))
                        .font(.system(size: 14, weight: .semibold, design: .monospaced))
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
                                ) ? CompactPopoverPalette.positive(colorScheme) : Color.secondary
                            )
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .font(.system(size: 10))
            .foregroundStyle(.secondary)
            .monospacedDigit()
            .textSelection(.enabled)
        } else {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                Text("—")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .font(.system(size: 12))
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

    var statusDot: some View {
        Circle()
            .fill(cardStatusColor)
            .frame(width: 5, height: 5)
            .overlay {
                Circle()
                    .stroke(cardStatusColor.opacity(0.16), lineWidth: 2)
            }
            .accessibilityHidden(true)
    }

    var cardStatusColor: Color {
        if state.error != nil {
            return CompactPopoverPalette.criticalColor
        }
        if state.isStale {
            return CompactPopoverPalette.warningColor
        }

        let healthStates = state.snapshot?.normalizedMetrics.map(\.healthState) ?? []
        if healthStates.contains(where: { $0 == .critical || $0 == .unavailable }) {
            return CompactPopoverPalette.criticalColor
        }
        if healthStates.contains(.warning) {
            return CompactPopoverPalette.warningColor
        }
        if healthStates.contains(.normal) {
            return CompactPopoverPalette.positive(colorScheme)
        }
        return Color.secondary.opacity(0.55)
    }

    @ViewBuilder
    func providerSubtitle() -> some View {
        let providerName = UsageRowPresentation.providerName(
            for: state.configuration.providerID
        )
        let description = UsageRowPresentation.accountDescription(
            configuration: state.configuration,
            snapshot: state.snapshot
        )

        if let websiteURL = state.configuration.websiteURL {
            ProviderWebsiteLink(
                providerName: providerName,
                subtitle: description,
                destination: websiteURL,
                colorScheme: colorScheme
            )
        } else {
            Text(description)
        }
    }

    /// 失败徽章挂在别名头像右上角，点击头像即可查看失败详情。
    @ViewBuilder
    func avatarWithStatus(size: CGFloat) -> some View {
        let avatar = CompactAccountAvatar(
            providerID: state.configuration.providerID,
            size: size
        )

        if state.error != nil {
            Button {
                showsFailureDetails = true
            } label: {
                avatar
                    .overlay(alignment: .topTrailing) {
                        refreshFailureIndicator
                    }
                    .contentShape(RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous))
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

    var avatarWithStatus: some View {
        avatarWithStatus(size: CompactPopoverMetrics.avatarSize)
    }

    var refreshFailureIndicator: some View {
        Image(systemName: "exclamationmark")
            .font(.system(size: 7, weight: .black))
            .foregroundStyle(.white)
            .frame(width: 12, height: 12)
            .background {
                Circle()
                    .fill(CompactPopoverPalette.criticalColor)
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
        CompactPopoverPalette.statusColor(
            for: UsageMetricPresentation.tone(
                for: metric.percent,
                rules: menuBarColorRules
            ),
            colorScheme
        )
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
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.warningColor)

            Text(failureReason)
                .font(.system(size: 12))
                .fixedSize(horizontal: false, vertical: true)

            Text(dataSourceText)
                .font(.system(size: 12))
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
        return "当前显示 \(UsageFormatter.lastSuccessDateText(lastSuccessAt)) 的上次成功数据。"
    }
}
