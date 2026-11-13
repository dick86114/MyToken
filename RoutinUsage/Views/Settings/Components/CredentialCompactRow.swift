import SwiftUI

/// 凭证管理页的紧凑行：只保留身份、状态和一项主指标，完整用量留给详情页。
struct CredentialCompactRow: View {
    let state: KeyUsageState
    let planType: String
    let attention: CredentialAttention?
    let attentionMetric: NormalizedUsageMetric?
    let alertsEnabled: Bool
    let accessibilityLabel: String
    let setEnabled: (Bool) -> Void
    let openDetails: () -> Void
    let share: () -> Void
    let edit: () -> Void
    let showAlertSettings: () -> Void
    let refresh: () -> Void
    let delete: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @State private var isHovered = false

    var body: some View {
        HStack(spacing: 10) {
            Button(action: openDetails) {
                HStack(spacing: 11) {
                    statusDot

                    VStack(alignment: .leading, spacing: 3) {
                        Text(state.configuration.displayName)
                            .font(.system(size: 13.5, weight: .semibold))
                            .lineLimit(1)

                        HStack(spacing: 4) {
                            Text(planType)
                                .foregroundStyle(.tertiary)
                                .lineLimit(1)

                            Text("·")
                                .foregroundStyle(.quaternary)

                            Text(statusLine)
                                .foregroundStyle(statusColor)
                                .lineLimit(1)
                                .truncationMode(.tail)
                                .layoutPriority(1)
                        }
                        .font(.system(size: 11.5))
                    }
                    .opacity(state.configuration.isEnabled ? 1 : 0.62)

                    Spacer(minLength: 12)

                    trailingSummary
                        .opacity(state.configuration.isEnabled ? 1 : 0.52)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .help("查看 \(state.configuration.displayName) 的完整详情")
            .accessibilityLabel(accessibilityLabel)

            Toggle("", isOn: Binding(
                get: { state.configuration.isEnabled },
                set: setEnabled
            ))
            .labelsHidden()
            .toggleStyle(.switch)
            .controlSize(.small)
            .tint(.accentColor)
            .help(state.configuration.isEnabled
                ? "停用 \(state.configuration.displayName)"
                : "启用 \(state.configuration.displayName)")
            .accessibilityLabel(state.configuration.isEnabled
                ? "停用 \(state.configuration.displayName)"
                : "启用 \(state.configuration.displayName)")

            actionsMenu
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .background {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color.primary.opacity(isHovered ? 0.045 : 0))
        }
        .onHover { isHovered = $0 }
    }

    private var statusDot: some View {
        Circle()
            .fill(statusColor)
            .frame(width: 7, height: 7)
            .overlay {
                Circle()
                    .strokeBorder(Color.primary.opacity(colorScheme == .dark ? 0.12 : 0.06))
            }
            .accessibilityHidden(true)
    }

    private var trailingSummary: some View {
        VStack(alignment: .trailing, spacing: 3) {
            if let displayMetricSummary {
                Text(displayMetricSummary)
                    .font(.system(size: 11.5, weight: .medium, design: .monospaced))
                    .foregroundStyle(displayMetricColor)
                    .lineLimit(1)
            }

            if let attentionProgress {
                attentionProgressBar(attentionProgress)
            }

            if let updateText {
                Text(updateText)
                    .font(.system(size: 10))
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }
        }
        .frame(minWidth: 122, alignment: .trailing)
    }

    private var actionsMenu: some View {
        Menu {
            Button(action: openDetails) {
                Label("查看详情", systemImage: "info.circle")
            }

            Button(action: share) {
                Label("分享用量", systemImage: "square.and.arrow.up")
            }
            .disabled(state.snapshot == nil)

            Button(action: refresh) {
                Label(
                    state.error == nil ? "立即刷新" : "重试刷新",
                    systemImage: "arrow.clockwise"
                )
            }
            .disabled(!state.configuration.isEnabled || state.isRefreshing)

            Divider()

            Button(action: edit) {
                Label("编辑凭证", systemImage: "pencil")
            }

            Button(action: showAlertSettings) {
                Label("提醒设置", systemImage: alertIcon)
            }

            Divider()

            Button(role: .destructive, action: delete) {
                Label("删除凭证", systemImage: "trash")
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.secondary)
                .frame(width: 24, height: 24)
                .contentShape(Circle())
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .help("管理 \(state.configuration.displayName)")
        .accessibilityLabel("管理 \(state.configuration.displayName)")
    }

    private var statusLine: String {
        if !state.configuration.isEnabled {
            return "已停用"
        }
        if state.isRefreshing {
            return state.snapshot == nil ? "正在加载" : "正在刷新"
        }
        if let attention {
            switch attention {
            case .refreshFailed:
                let reason = state.failureMessage.flatMap { message in
                    message.isEmpty ? nil : message
                } ?? state.error.map(UsageFormatter.errorText)
                return [attention.title, reason]
                    .compactMap { $0 }
                    .joined(separator: " · ")
            case .subscriptionExpired:
                return "\(attention.title) · 请续费或更换套餐"
            case let .criticalMetric(metric):
                return metric.label.contains("余额")
                    ? "\(attention.title) · 请及时充值"
                    : "\(attention.title) · 请检查额度"
            case .staleData:
                return "\(attention.title) · 请刷新"
            case let .warningMetric(metric):
                return "\(metric.label) 需注意 · 请留意余量"
            }
        }
        return state.snapshot == nil ? "等待首次刷新" : "数据正常"
    }

    private var statusColor: Color {
        if !state.configuration.isEnabled {
            return .secondary
        }
        if state.isRefreshing {
            return .accentColor
        }
        guard let attention else {
            return state.snapshot == nil ? .secondary : CompactPopoverPalette.positive(colorScheme)
        }
        switch attention {
        case .refreshFailed, .subscriptionExpired, .criticalMetric:
            return CompactPopoverPalette.criticalColor
        case .staleData, .warningMetric:
            return CompactPopoverPalette.warningColor
        }
    }

    private var displayMetric: NormalizedUsageMetric? {
        if let attentionMetric {
            return attentionMetric
        }
        guard
            state.configuration.isEnabled,
            let snapshot = state.snapshot
        else {
            return nil
        }

        let orderedMetrics = UsageCardDensityPolicy.orderedMetrics(
            providerID: state.configuration.providerID,
            metadata: state.configuration.metadata,
            metrics: snapshot.normalizedMetrics
        )
        let metric = orderedMetrics.first(where: { $0.presentation != .status })
            ?? snapshot.normalizedMetrics.first(where: { $0.presentation != .status })
        return metric
    }

    private var displayMetricSummary: String? {
        displayMetric.flatMap { Self.metricSummary(for: $0) }
    }

    private var displayMetricColor: Color {
        guard let attentionMetric else {
            return .secondary
        }
        switch attentionMetric.healthState {
        case .critical:
            return CompactPopoverPalette.criticalColor
        case .warning:
            return CompactPopoverPalette.warningColor
        case .normal, .unavailable, .stale, .unknown:
            return statusColor
        }
    }

    private var attentionProgress: Double? {
        guard
            let attentionMetric,
            attentionMetric.presentation == .progress,
            let percent = attentionMetric.displayedPercent
        else {
            return nil
        }
        return min(max(percent, 0), 100)
    }

    private func attentionProgressBar(_ percent: Double) -> some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.primary.opacity(colorScheme == .dark ? 0.12 : 0.08))

                Capsule()
                    .fill(displayMetricColor)
                    .frame(
                        width: geometry.size.width * CGFloat(percent / 100)
                    )
            }
        }
        .frame(width: 122, height: 4)
        .accessibilityHidden(true)
    }

    private var updateText: String? {
        if state.isRefreshing {
            return "刷新中"
        }
        guard let lastSuccessAt = state.lastSuccessAt else {
            return state.error == nil ? nil : "未成功更新"
        }

        let calendar = Calendar.current
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm"
        let time = formatter.string(from: lastSuccessAt)
        if calendar.isDateInToday(lastSuccessAt) {
            return "\(time) 更新"
        }
        if calendar.isDateInYesterday(lastSuccessAt) {
            return "昨天 \(time)"
        }
        formatter.dateFormat = "MM-dd"
        return "\(formatter.string(from: lastSuccessAt)) \(time)"
    }

    private var alertIcon: String {
        alertsEnabled ? "bell.badge" : "bell.slash"
    }

    private static func metricSummary(for metric: NormalizedUsageMetric) -> String? {
        switch metric.presentation {
        case .balance:
            let value = metric.value ?? metric.remaining
            guard let value else {
                return metric.label
            }
            return "余额 \(UsageFormatter.currencyText(value, currencyCode: metric.currencyCode))"

        case .progress:
            guard let percent = metric.displayedPercent else {
                return metric.label
            }
            let clamped = min(max(percent, 0), 100)
            let direction = metric.displaysRemainingPercent ? "剩余" : "已用"
            return "\(metric.label) \(direction) \(UsageFormatter.displayPercentText(clamped))"

        case .status:
            return nil

        case .value:
            guard let value = metric.value else {
                return metric.label
            }
            switch metric.unit {
            case .currency:
                return "\(metric.label) \(UsageFormatter.currencyText(value, currencyCode: metric.currencyCode))"
            case .token:
                return "\(metric.label) \(UsageFormatter.compactMetricValue(value))"
            case .request:
                return "\(metric.label) \(UsageFormatter.numberText(value)) 次"
            case .boolean, .text:
                return metric.label
            }
        }
    }
}
