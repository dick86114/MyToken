import SwiftUI

@MainActor
struct UsageRowView: View {
    let store: UsageStore
    let state: KeyUsageState
    let detectionState: CodexGroupDetectionState
    let detectionRecord: CodexGroupDetectionRecord?
    let isAnotherDetectionActive: Bool
    let requestDetection: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 60)) { timeline in
            HStack(alignment: .top, spacing: 10) {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(isSelected ? Color.accentColor : .secondary)
                        .font(.system(size: 12, weight: .semibold))
                        .padding(.top, 4)
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 7) {
                        headerView(now: timeline.date)
                        content(now: timeline.date)
                    }
            }
            .contentShape(Rectangle())
            .padding(.vertical, 10)
            .padding(.horizontal, 8)
            .opacity(isSubscriptionExpired(now: timeline.date) ? 0.45 : 1)
            .background {
                if isSelected {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Color.accentColor.opacity(0.10))
                }
            }
            .overlay {
                if isSelected {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Color.accentColor.opacity(0.18), lineWidth: 1)
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel(accessibilityLabel(now: timeline.date))
            .accessibilityHint(accessibilityHint)
            .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : [.isButton])
            .accessibilityAction { store.selectKey(state.configuration.id) }
            .onTapGesture { store.selectKey(state.configuration.id) }
        }
    }
}

enum UsageRowAccessibility {
    static func label(
        state: KeyUsageState,
        metric: UsageMetric?,
        dimension: DisplayDimension,
        isSelected: Bool,
        now: Date = .now
    ) -> String {
        let currentPrefix = isSelected ? "当前，" : ""
        let prefix = "\(currentPrefix)\(state.configuration.displayName)，"
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
                  let percentText = UsageFormatter.percentText(metric) {
            summary = prefix + "已使用 \(percentText)，\(UsageFormatter.fullAmount(metric))"
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
        if snapshot.kind == .periodic {
            details.append("5 小时剩余 \(remainingDuration(for: snapshot.fiveHour, now: now))")
            details.append("周剩余 \(remainingDuration(for: snapshot.weekly, now: now))")
        }
        if !snapshot.groupMultipliers.isEmpty {
            details.append(UsageFormatter.groupMultiplierText(snapshot.groupMultipliers))
        }
        return ([summary] + details).joined(separator: "，")
    }

    static func hint(isSelected: Bool) -> String {
        isSelected ? "已是菜单栏当前 Key" : "点击后设为菜单栏当前 Key"
    }

    private static func remainingDuration(for metric: UsageMetric?, now: Date) -> String {
        guard let end = metric?.windowEnd else { return "—" }
        return UsageFormatter.remainingDurationText(until: end, now: now)
    }
}

private extension UsageRowView {
    var isSelected: Bool {
        store.selectedKeyID == state.configuration.id
    }

    func headerView(now: Date) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 5) {
                        Text(state.configuration.displayName)
                            .font(.headline)
                            .lineLimit(1)
                        if isSelected {
                            Text("当前账户")
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(Color.accentColor)
                        }
                    }
                    subscriptionDescription(now: now)
                }
                Spacer(minLength: 8)
                if validMetric(state.snapshot?.token) != nil || hasGroupMultipliers || state.snapshot?.metrics.isEmpty == false {
                    VStack(alignment: .trailing, spacing: 3) {
                        if let metric = validMetric(state.snapshot?.token),
                           let percentText = UsageFormatter.percentText(metric) {
                            Text(percentText)
                                .font(.system(.headline, design: .rounded, weight: .semibold))
                                .monospacedDigit()
                                .foregroundStyle(progressColor(for: metric))
                        }

                        if let metric = state.snapshot?.normalizedMetrics.first {
                            normalizedHeaderMetric(metric)
                        }

                        if let currentGroupMultiplier {
                            groupMultiplierText(currentGroupMultiplier)
                                .font(.caption2)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        if hasGroupMultipliers {
                            probeButton
                        }
                    }
                }
            }
            subscriptionPeriodDetails
        }
    }

    @ViewBuilder
    func groupMultiplierText(_ group: UsageGroupMultiplier) -> some View {
        Text(UsageFormatter.groupMultiplierText([group]))
            .foregroundStyle(Color.green)
            .accessibilityElement(children: .combine)
        .accessibilityLabel(groupMultiplierAccessibilityLabel(group: group))
    }

    @ViewBuilder
    var probeButton: some View {
        if detectionState.isBusy {
            ProgressView()
                .controlSize(.small)
                .frame(width: 16, height: 16)
                .padding(.leading, 5)
                .accessibilityLabel("正在获取 Codex 当前分组")
        } else {
            Button(action: requestDetection) {
                Image(systemName: "location.magnifyingglass")
                    .frame(width: 16, height: 16)
            }
            .buttonStyle(.plain)
            .disabled(isAnotherDetectionActive)
            .help("获取 Codex 当前分组")
            .accessibilityLabel("获取 Codex 当前分组")
            .padding(.leading, 5)
        }
    }

    @ViewBuilder
    var subscriptionPeriodDetails: some View {
        if let snapshot = state.snapshot, snapshot.kind == .periodic {
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

    func groupMultiplierAccessibilityLabel(group: UsageGroupMultiplier) -> String {
        var label = "Codex 当前分组：\(UsageFormatter.groupMultiplierText([group]))"
        if let detectedAt = detectionRecord?.detectedAt {
            label += "，检测于 \(detectedAt.formatted(date: .omitted, time: .shortened))"
        }
        return label
    }

    @ViewBuilder
    func normalizedHeaderMetric(_ metric: NormalizedUsageMetric) -> some View {
        switch metric.presentation {
        case .progress:
            if let used = metric.used, let limit = metric.limit, limit > 0 {
                let percent = NSDecimalNumber(decimal: used)
                    .dividing(by: NSDecimalNumber(decimal: limit))
                    .multiplying(by: 100)
                    .intValue
                Text("\(percent)%")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(normalizedMetricColor(metric.healthState))
            }
        case .balance:
            Text("余额 \(decimalText(metric.value)) 元")
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
        guard let value else { return "—" }
        return NSDecimalNumber(decimal: value).stringValue
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

                if state.configuration.providerID == .routin {
                    codexGroupDetectionStatus
                }

                if state.isRefreshing || state.isStale || state.error != nil {
                    Label(
                        UsageFormatter.statusText(state: state),
                        systemImage: "clock.badge.exclamationmark"
                    )
                        .font(.caption)
                        .foregroundStyle(.orange)
                }
            }
        } else {
            statusLabel
        }
    }

    @ViewBuilder
    var codexGroupDetectionStatus: some View {
        if detectionState == .idle {
            EmptyView()
        } else {
            HStack(spacing: 5) {
                if detectionState.isBusy {
                    ProgressView()
                        .controlSize(.small)
                        .frame(width: 12, height: 12)
                        .accessibilityHidden(true)
                } else {
                    Image(systemName: codexGroupDetectionSymbol)
                        .accessibilityHidden(true)
                }
                Text(codexGroupDetectionStatusText)
            }
            .font(.caption)
            .foregroundStyle(codexGroupDetectionColor)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Codex 分组检测：\(codexGroupDetectionStatusText)")
        }
    }

    var codexGroupDetectionStatusText: String {
        if detectionState == .succeeded, let groupName = detectionRecord?.groupName {
            return "Codex 当前分组：\(groupName)"
        }
        return detectionState.statusText
    }

    var codexGroupDetectionSymbol: String {
        if detectionState.isBusy {
            return "arrow.triangle.2.circlepath"
        }
        if detectionState == .succeeded {
            return "checkmark.circle.fill"
        }
        if detectionState == .needsLogin {
            return "person.crop.circle.badge.exclamationmark"
        }
        return detectionState.isFailure ? "exclamationmark.triangle.fill" : "info.circle"
    }

    var codexGroupDetectionColor: Color {
        if detectionState == .succeeded {
            return .green
        }
        return detectionState.isFailure ? .orange : .secondary
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

    @ViewBuilder
    func normalizedMetricsContent(snapshot: UsageSnapshot, now: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(snapshot.normalizedMetrics) { metric in
                normalizedMetricContent(metric, now: now)
            }
        }
    }

    @ViewBuilder
    func normalizedMetricContent(_ metric: NormalizedUsageMetric, now: Date) -> some View {
        switch metric.presentation {
        case .progress:
            let percent = metric.displayedPercent ?? 0
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(metric.label)
                    Spacer()
                    Text("\(Int(percent.rounded()))%")
                        .monospacedDigit()
                        .foregroundStyle(normalizedMetricColor(metric.healthState))
                }
                UsageMetricProgressBar(percent: percent)
                if let remaining = metric.remaining {
                    Text("剩余 \(decimalText(remaining))")
                }
                if let windowEnd = metric.windowEnd {
                    Text("重置 \(UsageFormatter.remainingDurationText(until: windowEnd, now: now))")
                }
            }
        case .balance:
            HStack {
                Text(metric.label)
                Spacer()
                Text("余额 \(decimalText(metric.value)) \(metric.currencyCode ?? "")")
                    .foregroundStyle(normalizedMetricColor(metric.healthState))
                    .monospacedDigit()
            }
        case .status:
            HStack(spacing: 5) {
                Circle()
                    .fill(normalizedMetricColor(metric.healthState))
                    .frame(width: 7, height: 7)
                Text(metric.label)
                Spacer()
                Text(metric.healthState == .unavailable ? "不可用" : "可用")
            }
        case .value:
            HStack {
                Text(metric.label)
                Spacer()
                Text(decimalText(metric.value))
                    .monospacedDigit()
            }
        }
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
                    Text(UsageFormatter.percentText(metric) ?? "—")
                        .font(.system(.headline, design: .rounded, weight: .semibold))
                        .foregroundStyle(progressColor(for: metric))
                        .monospacedDigit()
                }

                UsageMetricProgressBar(metric: metric)

                Text(UsageFormatter.amount(metric))
                    .help(UsageFormatter.fullAmount(metric))
                Text("剩余 \(UsageFormatter.remaining(metric))")
                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric))")
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
    func subscriptionDescription(now: Date) -> some View {
        if let snapshot = state.snapshot {
            let description = subscriptionDescriptionText(for: snapshot)

            if let expiryText = UsageFormatter.subscriptionExpiryText(
                until: snapshot.subscriptionEndAt,
                now: now
            ) {
                HStack(spacing: 4) {
                    Text(description)
                        .foregroundStyle(.secondary)
                    Text(expiryText)
                        .foregroundStyle(.red)
                }
                .font(.caption)
            } else {
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        } else {
            Text("等待用量数据")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    func subscriptionDescriptionText(for snapshot: UsageSnapshot) -> String {
        if !snapshot.planName.isEmpty, !snapshot.metrics.isEmpty {
            return snapshot.planName
        }
        switch snapshot.kind {
        case .periodic:
            return "\(snapshot.planName) · 5 小时与周"
        case .tokenPack:
            return "\(snapshot.planName) · Token 资源包"
        }
    }

    var currentGroupMultiplier: UsageGroupMultiplier? {
        UsageFormatter.currentGroupMultiplier(
            in: state.snapshot?.groupMultipliers ?? [],
            matching: detectionRecord?.groupName
        )
    }

    var hasGroupMultipliers: Bool {
        !(state.snapshot?.groupMultipliers.isEmpty ?? true)
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
        if state.isRefreshing && state.snapshot == nil {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "arrow.triangle.2.circlepath"
            )
        } else if state.error == .noSubscription {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "minus.circle"
            )
        } else if state.error != nil {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "exclamationmark.triangle"
            )
        } else {
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
                    return "余额 \(decimalText(metric.value)) \(metric.currencyCode ?? "")"
                }
                if let percent = metric.displayedPercent {
                    return "\(metric.label) \(metric.displaysRemainingPercent ? "剩余" : "已使用") \(Int(percent.rounded()))%"
                }
                return metric.label
            }.joined(separator: "，")
            return "\(isSelected ? "当前，" : "")\(state.configuration.displayName)，\(metricText)"
        }
        return UsageRowAccessibility.label(
            state: state,
            metric: validMetric(state.snapshot?.fiveHour) ?? validMetric(state.snapshot?.token),
            dimension: .fiveHour,
            isSelected: isSelected,
            now: now
        )
    }

    var accessibilityHint: String {
        UsageRowAccessibility.hint(isSelected: isSelected)
    }
}
