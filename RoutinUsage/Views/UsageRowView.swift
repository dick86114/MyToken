import SwiftUI

@MainActor
struct UsageRowView: View {
    let store: UsageStore
    let state: KeyUsageState

    var body: some View {
        TimelineView(.periodic(from: .now, by: 60)) { timeline in
            Button {
                store.selectKey(state.configuration.id)
            } label: {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: isSelected ? "circle.fill" : "circle")
                        .foregroundStyle(isSelected ? Color.accentColor : .secondary)
                        .font(.system(size: 9, weight: .semibold))
                        .padding(.top, 5)
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 7) {
                        header
                        content(now: timeline.date)
                    }
                }
                .contentShape(Rectangle())
                .padding(.vertical, 7)
                .padding(.horizontal, 4)
            }
            .buttonStyle(.plain)
            .background {
                if isSelected {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.accentColor.opacity(0.10))
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel(accessibilityLabel(now: timeline.date))
            .accessibilityHint(accessibilityHint)
            .accessibilityAddTraits(isSelected ? .isSelected : [])
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

    var header: some View {
        HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(state.configuration.displayName)
                    .font(.headline)
                    .lineLimit(1)
                Text(subscriptionDescription)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            if validMetric(state.snapshot?.token) != nil
                || !(state.snapshot?.groupMultipliers.isEmpty ?? true) {
                VStack(alignment: .trailing, spacing: 3) {
                    if let metric = validMetric(state.snapshot?.token),
                       let percentText = UsageFormatter.percentText(metric) {
                        Text(percentText)
                            .font(.system(.headline, design: .rounded, weight: .semibold))
                            .monospacedDigit()
                            .foregroundStyle(progressColor(for: metric))
                    }

                    if let groupMultipliers = state.snapshot?.groupMultipliers,
                       !groupMultipliers.isEmpty {
                        Text(UsageFormatter.groupMultiplierText(groupMultipliers))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }
            }
        }
    }

    @ViewBuilder
    func content(now: Date) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 7) {
                switch snapshot.kind {
                case .periodic:
                    periodicContent(snapshot: snapshot, now: now)
                case .tokenPack:
                    if let metric = validMetric(snapshot.token) {
                        metricContent(title: "Token", metric: metric, now: now)
                    } else {
                        statusLabel
                    }
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
    func periodicContent(snapshot: UsageSnapshot, now: Date) -> some View {
        HStack(alignment: .top, spacing: 16) {
            metricContent(title: "5 小时", metric: validMetric(snapshot.fiveHour), now: now)
            metricContent(title: "周", metric: validMetric(snapshot.weekly), now: now)
        }
    }

    @ViewBuilder
    func metricContent(title: String, metric: UsageMetric?, now: Date) -> some View {
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

                ProgressView(value: min(max(metric.percent, 0), 100), total: 100)
                    .tint(progressColor(for: metric))

                Text(UsageFormatter.amount(metric))
                    .help(UsageFormatter.fullAmount(metric))
                Text("剩余 \(UsageFormatter.remaining(metric))")
                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric))")
                    Text("剩余 \(remainingDuration(for: metric, now: now))")
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

    var subscriptionDescription: String {
        guard let snapshot = state.snapshot else {
            return "等待用量数据"
        }
        switch snapshot.kind {
        case .periodic:
            return "\(snapshot.planName) · 5 小时与周"
        case .tokenPack:
            return "\(snapshot.planName) · Token 资源包"
        }
    }

    func validMetric(_ metric: UsageMetric?) -> UsageMetric? {
        guard let metric,
              UsageFormatter.percentText(metric) != nil else {
            return nil
        }
        return metric
    }

    func progressColor(for metric: UsageMetric) -> Color {
        switch MenuBarUsageRisk.level(for: metric.percent) {
        case .critical:
            return .red
        case .warning:
            return .orange
        case .normal:
            return .green
        }
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
        UsageRowAccessibility.label(
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
