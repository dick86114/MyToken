import SwiftUI

@MainActor
struct UsageRowView: View {
    let store: UsageStore
    let state: KeyUsageState
    let dimension: DisplayDimension

    var body: some View {
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
                    content
                }
            }
            .contentShape(Rectangle())
            .padding(.vertical, 7)
            .padding(.horizontal, 4)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint(accessibilityHint)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

enum UsageRowAccessibility {
    static func label(
        state: KeyUsageState,
        metric: UsageMetric?,
        dimension: DisplayDimension,
        isSelected: Bool
    ) -> String {
        let currentPrefix = isSelected ? "当前，" : ""
        let prefix = "\(currentPrefix)\(state.configuration.displayName)，"
        if state.isStale, metric != nil {
            return prefix + UsageFormatter.statusText(state: state, dimension: dimension)
                + "，当前显示上次成功数据"
        }
        if state.isRefreshing, state.snapshot == nil {
            return prefix + "正在加载"
        }
        if state.error == .noSubscription {
            return prefix + UsageFormatter.statusText(state: state, dimension: dimension)
        }
        if state.error != nil, state.snapshot == nil {
            return prefix + UsageFormatter.statusText(state: state, dimension: dimension)
                + "，没有缓存"
        }
        if let metric,
           let percentText = UsageFormatter.percentText(metric) {
            return prefix + "已使用 \(percentText)，\(UsageFormatter.fullAmount(metric))"
        }
        return prefix + UsageFormatter.statusText(state: state, dimension: dimension)
    }

    static func hint(isSelected: Bool) -> String {
        isSelected ? "已是菜单栏当前 Key" : "点击后设为菜单栏当前 Key"
    }
}

private extension UsageRowView {
    var isSelected: Bool {
        store.selectedKeyID == state.configuration.id
    }

    var metric: UsageMetric? {
        guard let rawMetric,
              UsageFormatter.percentText(rawMetric) != nil else {
            return nil
        }
        return rawMetric
    }

    var rawMetric: UsageMetric? {
        state.snapshot.flatMap {
            UsageFormatter.metric(in: $0, dimension: dimension)
        }
    }

    var hasInvalidMetric: Bool {
        rawMetric.map { UsageFormatter.percentText($0) == nil } ?? false
    }

    var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(state.configuration.displayName)
                    .font(.headline)
                    .lineLimit(1)
                Text(subscriptionDescription)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            if let metric,
               let percentText = UsageFormatter.percentText(metric) {
                Text(percentText)
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(progressColor)
            }
        }
    }

    @ViewBuilder
    var content: some View {
        if let metric {
            ProgressView(
                value: min(max(metric.percent, 0), 100),
                total: 100
            )
            .tint(progressColor)

            HStack(spacing: 8) {
                Text(UsageFormatter.amount(metric))
                    .help(UsageFormatter.fullAmount(metric))
                Spacer(minLength: 8)
                Text("剩余 \(UsageFormatter.remaining(metric))")
                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric))")
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .monospacedDigit()

            details

            if state.isStale {
                Label(
                    UsageFormatter.statusText(state: state, dimension: dimension),
                    systemImage: "clock.badge.exclamationmark"
                )
                    .font(.caption)
                    .foregroundStyle(.orange)
            }
        } else {
            statusLabel
        }
    }

    /// 显示窗口剩余时长和按名称配对的分组倍率。
    @ViewBuilder
    var details: some View {
        TimelineView(.periodic(from: .now, by: 60)) { timeline in
            VStack(alignment: .leading, spacing: 3) {
                if state.snapshot?.kind == .periodic {
                    detailLine(
                        "5 小时剩余",
                        value: remainingDuration(
                            for: state.snapshot?.fiveHour,
                            now: timeline.date
                        )
                    )
                    detailLine(
                        "周剩余",
                        value: remainingDuration(
                            for: state.snapshot?.weekly,
                            now: timeline.date
                        )
                    )
                }
                if let groupMultipliers = state.snapshot?.groupMultipliers,
                   !groupMultipliers.isEmpty {
                    detailLine(
                        "分组倍率",
                        value: UsageFormatter.groupMultiplierText(groupMultipliers)
                    )
                }
            }
            .font(.caption2)
            .foregroundStyle(.secondary)
            .textSelection(.enabled)
        }
    }

    func detailLine(_ title: String, value: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text(title + "：")
                .foregroundStyle(.tertiary)
            Text(value)
                .lineLimit(2)
                .truncationMode(.middle)
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
            return "\(snapshot.planName) · \(dimension == .fiveHour ? "5 小时" : "周")"
        case .tokenPack:
            return "\(snapshot.planName) · Token 资源包"
        }
    }

    var progressColor: Color {
        guard let metric else {
            return .gray
        }
        if metric.percent >= 95 {
            return .red
        }
        if metric.percent >= 80 {
            return .orange
        }
        return .green
    }

    @ViewBuilder
    var statusLabel: some View {
        if hasInvalidMetric {
            Label(
                UsageFormatter.statusText(state: state, dimension: dimension),
                systemImage: "exclamationmark.triangle"
            )
        } else if state.isRefreshing && state.snapshot == nil {
            Label(
                UsageFormatter.statusText(state: state, dimension: dimension),
                systemImage: "arrow.triangle.2.circlepath"
            )
        } else if state.error == .noSubscription {
            Label(
                UsageFormatter.statusText(state: state, dimension: dimension),
                systemImage: "minus.circle"
            )
        } else if state.error != nil {
            Label(
                UsageFormatter.statusText(state: state, dimension: dimension),
                systemImage: "exclamationmark.triangle"
            )
        } else {
            Label(
                UsageFormatter.statusText(state: state, dimension: dimension),
                systemImage: "clock"
            )
        }
    }

    var accessibilityLabel: String {
        UsageRowAccessibility.label(
            state: state,
            metric: metric,
            dimension: dimension,
            isSelected: isSelected
        )
    }

    var accessibilityHint: String {
        UsageRowAccessibility.hint(isSelected: isSelected)
    }
}
