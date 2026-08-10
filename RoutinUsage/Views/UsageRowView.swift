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
        .accessibilityHint("点击后设为菜单栏当前 Key")
    }
}

private extension UsageRowView {
    var isSelected: Bool {
        store.selectedKeyID == state.configuration.id
    }

    var metric: UsageMetric? {
        state.snapshot.flatMap {
            UsageFormatter.metric(in: $0, dimension: dimension)
        }
    }

    var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(state.configuration.name)
                    .font(.headline)
                    .lineLimit(1)
                Text(subscriptionDescription)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            if let metric {
                Text("\(Int(metric.percent.rounded()))%")
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

            if state.isStale {
                Label(
                    UsageFormatter.statusText(state: state),
                    systemImage: "clock.badge.exclamationmark"
                )
                    .font(.caption)
                    .foregroundStyle(.orange)
            }
        } else {
            statusLabel
        }
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
        if state.isRefreshing && state.snapshot == nil {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "arrow.triangle.2.circlepath"
            )
        } else if state.error == .noSubscription {
            Label(UsageFormatter.statusText(state: state), systemImage: "minus.circle")
        } else if state.error != nil {
            Label(
                UsageFormatter.statusText(state: state),
                systemImage: "exclamationmark.triangle"
            )
        } else {
            Label(UsageFormatter.statusText(state: state), systemImage: "clock")
        }
    }

    var accessibilityLabel: String {
        let prefix = "\(state.configuration.name)，"
        if state.isStale, metric != nil {
            return prefix + UsageFormatter.statusText(state: state)
                + "，当前显示上次成功数据"
        }
        if state.isRefreshing, state.snapshot == nil {
            return prefix + "正在加载"
        }
        if state.error == .noSubscription {
            return prefix + UsageFormatter.statusText(state: state)
        }
        if state.error != nil, state.snapshot == nil {
            return prefix + UsageFormatter.statusText(state: state) + "，没有缓存"
        }
        if let metric {
            return prefix + "已使用 \(Int(metric.percent.rounded()))%，\(UsageFormatter.fullAmount(metric))"
        }
        return prefix + UsageFormatter.statusText(state: state)
    }
}
