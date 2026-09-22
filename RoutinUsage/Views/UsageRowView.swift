import SwiftUI

@MainActor
struct UsageRowView: View {
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
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }

    private func compactCard(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 6) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.configuration.displayName)
                        .font(.headline.weight(.semibold))
                        .lineLimit(1)

                    Text(UsageRowPresentation.subscriptionDescription(
                        providerID: state.configuration.providerID,
                        planName: state.snapshot?.planName ?? ""
                    ))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 4)

                HStack(spacing: 2) {
                    if state.error != nil {
                        refreshFailureIndicator
                    }
                    if onShare != nil {
                        shareButton
                    }
                    headerRefreshButton
                }
            }

            if let snapshot = state.snapshot {
                compactMetrics(snapshot: snapshot, now: now)
            } else {
                statusLabel
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .modifier(UsageCardChrome(state: state, isExpired: isSubscriptionExpired(now: now)))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel(now: now))
        .accessibilityHint(accessibilityHint)
    }
}

private struct RefreshingCardBorder: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let color: Color

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: reduceMotion)) { timeline in
            let phase = reduceMotion
                ? 0
                : -timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1) * 28

            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(
                    color,
                    style: StrokeStyle(
                        lineWidth: 2,
                        lineCap: .round,
                        lineJoin: .round,
                        dash: [12, 7],
                        dashPhase: phase
                    )
                )
                .shadow(color: color.opacity(0.35), radius: 2)
        }
        .accessibilityHidden(true)
    }
}

private struct UsageCardChrome: ViewModifier {
    let state: KeyUsageState
    let isExpired: Bool

    func body(content: Content) -> some View {
        content
            .background {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(ProviderTheme.background(for: state.configuration.providerID))
            }
            .overlay {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(ProviderTheme.borderColor(for: state.configuration.providerID))
            }
            .overlay {
                if state.isRefreshing {
                    RefreshingCardBorder(
                        color: ProviderTheme.accentColor(for: state.configuration.providerID)
                    )
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
        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .center, spacing: 8) {
                Text(state.configuration.displayName)
                    .font(.largeTitle.weight(.semibold))
                    .lineLimit(1)

                VStack(alignment: .leading, spacing: 2) {
                    subscriptionDescription(now: now)
                    subscriptionPeriodDetails
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer(minLength: 8)

                if validMetric(state.snapshot?.token) != nil || state.snapshot?.metrics.isEmpty == false {
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

                HStack(spacing: 2) {
                    if state.error != nil {
                        refreshFailureIndicator
                    }
                    if onShare != nil {
                        shareButton
                    }
                    headerRefreshButton
                }
            }
        }
    }

    var shareButton: some View {
        Button {
            onShare?()
        } label: {
            Image(systemName: "square.and.arrow.up")
                .font(.system(size: 12, weight: .semibold))
                .frame(width: 20, height: 20)
        }
        .buttonStyle(.borderless)
        .disabled(state.snapshot == nil)
        .help("分享 \(state.configuration.displayName) 当前用量")
        .accessibilityLabel("分享 \(state.configuration.displayName) 当前用量")
    }

    var headerRefreshButton: some View {
        Button(action: refreshCredential) {
            Group {
                if state.isRefreshing {
                    ProgressView()
                        .controlSize(.small)
                } else {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 12, weight: .semibold))
                }
            }
            .frame(width: 20, height: 20)
        }
        .buttonStyle(.borderless)
        .disabled(state.isRefreshing || !state.configuration.isEnabled)
        .help("刷新 \(state.configuration.displayName)")
        .accessibilityLabel("刷新 \(state.configuration.displayName)")
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

        if !metrics.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(metrics) { metric in
                    compactMetricRow(metric, now: now)
                }
            }
        } else if snapshot.kind == .periodic {
            VStack(alignment: .leading, spacing: 10) {
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
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)

                Spacer(minLength: 4)

                if metric.presentation == .progress, let percent = metric.displayedPercent {
                    Text(UsageFormatter.displayPercentText(percent))
                        .font(.system(.caption, design: .rounded, weight: .semibold))
                        .foregroundStyle(normalizedMetricColor(metric.healthState))
                        .monospacedDigit()
                }
            }

            if metric.presentation == .progress {
                UsageMetricProgressBar(percent: metric.displayedPercent ?? 0)

                if let windowEnd = metric.windowEnd {
                    Text("重置 \(UsageFormatter.resetTime(windowEnd, now: now))")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                        .help("重置 \(UsageFormatter.fullDateTime(windowEnd))")
                }
            } else {
                Text(compactValueText(for: metric))
                    .font(.system(.caption, design: .rounded, weight: .semibold))
                    .foregroundStyle(normalizedMetricColor(metric.healthState))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(compactAccessibilityText(for: metric, now: now))
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

    @ViewBuilder
    func subscriptionDescription(now: Date) -> some View {
        subscriptionDescriptionContent(now: now)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    func subscriptionDescriptionContent(now: Date) -> some View {
        if let snapshot = state.snapshot {
            let planSuffix = snapshot.planName.isEmpty ? "" : " · \(snapshot.planName)"

            if let expiryText = UsageFormatter.subscriptionExpiryText(
                until: snapshot.subscriptionEndAt,
                now: now
            ) {
                HStack(spacing: 4) {
                    HStack(spacing: 2) {
                        providerNameLabel
                        if !planSuffix.isEmpty {
                            Text(planSuffix)
                        }
                    }
                    .foregroundStyle(.secondary)
                    Text(expiryText)
                        .foregroundStyle(.red)
                }
                .font(.caption)
            } else {
                HStack(spacing: 2) {
                    providerNameLabel
                    if !planSuffix.isEmpty {
                        Text(planSuffix)
                    }
                }
                .font(.system(.body, weight: .semibold))
                .foregroundStyle(.primary)
            }
        } else {
            Text("等待用量数据")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    var refreshFailureIndicator: some View {
        Button {
            showsFailureDetails = true
        } label: {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.caption)
                .foregroundStyle(.orange)
                .frame(width: 16, height: 16)
        }
        .buttonStyle(.borderless)
        .help(UsageFormatter.refreshFailureTooltip(state: state))
        .accessibilityLabel("查看刷新失败详情")
        .popover(isPresented: $showsFailureDetails, arrowEdge: .bottom) {
            RefreshFailurePopover(
                state: state,
                retry: {
                    showsFailureDetails = false
                    retryCredential()
                }
            )
        }
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
