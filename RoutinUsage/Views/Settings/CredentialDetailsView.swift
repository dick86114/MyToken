import SwiftUI

enum CredentialDetailsLayout {
    static let minWidth: CGFloat = 500
    static let idealWidth: CGFloat = 680
    static let maxWidth: CGFloat = 820
    static let minHeight: CGFloat = 560
}

struct CredentialDetailsView: View {
    let state: KeyUsageState
    var onClose: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var copiedModelID: String?
    @State private var copyResetTask: Task<Void, Never>?

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 22) {
                header
                statusOverview
                usageSection
                detailsPanel
            }
            .padding(28)
            .frame(maxWidth: CredentialDetailsLayout.maxWidth, alignment: .leading)
        }
        .frame(
            minWidth: CredentialDetailsLayout.minWidth,
            idealWidth: CredentialDetailsLayout.idealWidth,
            maxWidth: CredentialDetailsLayout.maxWidth,
            minHeight: CredentialDetailsLayout.minHeight
        )
        .navigationTitle("凭证详情")
        .onDisappear {
            copyResetTask?.cancel()
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 14) {
            ProviderLogoMark(
                providerID: state.configuration.providerID,
                size: 44,
                cornerRadius: 11
            )

            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 8) {
                    Text(state.configuration.displayName)
                        .font(.system(size: 22, weight: .semibold))
                        .lineLimit(1)
                        .truncationMode(.tail)

                    enabledBadge
                }

                Text("\(descriptor.displayName) · \(planName)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 16)

            Button {
                if let onClose {
                    onClose()
                } else {
                    dismiss()
                }
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .frame(width: 30, height: 30)
                    .background {
                        Circle()
                            .fill(Color.primary.opacity(colorScheme == .dark ? 0.10 : 0.06))
                    }
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("关闭凭证详情")
            .accessibilityLabel("关闭凭证详情")
        }
    }

    private var enabledBadge: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(state.configuration.isEnabled ? positiveColor : Color.secondary)
                .frame(width: 6, height: 6)

            Text(state.configuration.isEnabled ? "已启用" : "已停用")
                .font(.caption.weight(.medium))
        }
        .foregroundStyle(state.configuration.isEnabled ? positiveColor : Color.secondary)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background {
            Capsule()
                .fill((state.configuration.isEnabled ? positiveColor : Color.secondary).opacity(0.10))
        }
    }

    private var statusOverview: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 10) {
                statusBadge

                Text(statusDetail)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 12)
            }

            Divider()
                .overlay(providerAccent.opacity(0.16))

            LazyVGrid(
                columns: overviewColumns,
                alignment: .leading,
                spacing: 14
            ) {
                overviewFact(
                    "套餐",
                    state.snapshot?.planName.isEmpty == false
                        ? state.snapshot?.planName ?? planName
                        : planName
                )
                overviewFact(
                    "最近成功更新",
                    state.lastSuccessAt.map { UsageFormatter.fullDateTime($0) } ?? "尚未成功更新"
                )
                if let snapshot = state.snapshot, showsSeparateSnapshotTime {
                    overviewFact("数据快照", UsageFormatter.fullDateTime(snapshot.fetchedAt))
                }
            }
        }
        .padding(18)
        .background {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(providerAccent.opacity(colorScheme == .dark ? 0.09 : 0.055))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(providerAccent.opacity(colorScheme == .dark ? 0.20 : 0.14), lineWidth: 1)
        }
    }

    private var statusBadge: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(statusTone)
                .frame(width: 7, height: 7)

            Text(statusLabel)
                .font(.caption.weight(.semibold))
        }
        .foregroundStyle(statusTone)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background {
            Capsule()
                .fill(statusTone.opacity(0.11))
        }
        .accessibilityElement(children: .combine)
    }

    private var usageSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(
                "用量指标",
                symbol: "chart.bar.xaxis",
                trailing: state.snapshot.map { "\($0.normalizedMetrics.count) 项" }
            )

            if let snapshot = state.snapshot, !snapshot.normalizedMetrics.isEmpty {
                usageMetrics(snapshot.normalizedMetrics)
            } else {
                emptyMetrics
            }
        }
        .padding(18)
        .background {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(panelFill)
        }
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(panelStroke, lineWidth: 1)
        }
    }

    @ViewBuilder
    private func usageMetrics(_ metrics: [NormalizedUsageMetric]) -> some View {
        if state.configuration.providerID == .commandCode {
            CommandCodeUsageMetricsView(metrics: metrics, now: .now)
        } else if state.configuration.providerID == .glm {
            GLMUsageMetricsView(metrics: metrics, now: .now)
        } else {
            NormalizedUsageMetricGrid(
                metrics: metrics,
                columns: 2,
                resetTimeStyle: .relativeDuration,
                now: .now
            )
        }
    }

    private var emptyMetrics: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "chart.bar.xaxis")
                .font(.title3)
                .foregroundStyle(.tertiary)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 4) {
                Text("暂无可展示的用量指标")
                    .font(.callout.weight(.medium))
                Text("完成首次刷新后会在这里显示额度、余额和重置时间。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
    }

    @ViewBuilder
    private var detailsPanel: some View {
        if hasPlanDetails || hasAccountDetails || !configurationRows.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                if hasPlanDetails {
                    planDetailsBlock
                    if hasAccountDetails || !configurationRows.isEmpty {
                        blockDivider
                    }
                }

                if hasAccountDetails {
                    accountModelsBlock
                    if !configurationRows.isEmpty {
                        blockDivider
                    }
                }

                if !configurationRows.isEmpty {
                    configurationBlock
                }
            }
            .padding(20)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(panelFill)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(panelStroke, lineWidth: 1)
            }
        }
    }

    @ViewBuilder
    private var planDetailsBlock: some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 14) {
                sectionHeader("套餐与订阅", symbol: "calendar.badge.clock")

                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), alignment: .leading),
                        GridItem(.flexible(), alignment: .leading)
                    ],
                    alignment: .leading,
                    spacing: 14
                ) {
                    ForEach(planFacts(snapshot)) { fact in
                        detailFact(fact.label, fact.value)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var accountModelsBlock: some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 14) {
                sectionHeader("账户与模型", symbol: "person.crop.circle")

                if state.configuration.providerID == .routin,
                   !snapshot.groupMultipliers.isEmpty {
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        Text("分组倍率")
                            .font(.caption)
                            .foregroundStyle(.secondary)

                        Spacer()

                        Text(UsageFormatter.groupMultiplierText(snapshot.groupMultipliers))
                            .font(.callout.weight(.medium))
                            .multilineTextAlignment(.trailing)
                            .textSelection(.enabled)
                    }
                }

                allowedModelsSection(snapshot.allowedModels)
            }
        }
    }

    private var configurationBlock: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader("配置", symbol: "slider.horizontal.3")

            Grid(alignment: .leading, horizontalSpacing: 18, verticalSpacing: 10) {
                ForEach(configurationRows) { row in
                    GridRow(alignment: .firstTextBaseline) {
                        Text(metadataLabel(for: row.key))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .gridColumnAlignment(.leading)

                        if row.key == "websiteURL", let url = URL(string: row.value) {
                            Link(destination: url) {
                                HStack(spacing: 5) {
                                    Text(url.absoluteString)
                                        .lineLimit(1)
                                        .truncationMode(.middle)
                                    Image(systemName: "arrow.up.right")
                                        .font(.system(size: 9, weight: .semibold))
                                }
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(providerAccent)
                            .help("在浏览器中打开 \(url.absoluteString)")
                            .accessibilityLabel("打开官网地址 \(url.absoluteString)")
                        } else {
                            Text(metadataValue(for: row.key, value: row.value))
                                .font(.callout)
                                .fixedSize(horizontal: false, vertical: true)
                                .textSelection(.enabled)
                        }
                    }
                }
            }
        }
    }

    private func allowedModelsSection(_ models: [String]) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .firstTextBaseline) {
                Text("允许模型")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Spacer()

                if !models.isEmpty {
                    Text("\(models.count) 个 · 点击复制")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }

            if models.isEmpty {
                Text("接口未返回")
                    .font(.callout.weight(.medium))
                    .foregroundStyle(.secondary)
            } else {
                ModelIDChipFlow(models: models, copiedModelID: copiedModelID, onCopied: copyModelID)
            }
        }
    }

    private func copyModelID(_ modelID: String) {
        copyResetTask?.cancel()
        copiedModelID = modelID
        copyResetTask = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(1_200))
            guard !Task.isCancelled else { return }
            copiedModelID = nil
        }
    }

    private func sectionHeader(
        _ title: String,
        symbol: String,
        trailing: String? = nil
    ) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(providerAccent)

            Text(title)
                .font(.system(size: 15, weight: .semibold))

            Spacer(minLength: 12)

            if let trailing {
                Text(trailing)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
        }
    }

    private func overviewFact(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.callout.weight(.medium))
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func detailFact(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption2.weight(.medium))
                .foregroundStyle(.tertiary)
            Text(value)
                .font(.callout.weight(.medium))
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var blockDivider: some View {
        Divider()
            .overlay(panelStroke)
            .padding(.vertical, 18)
    }

    private var overviewColumns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), alignment: .leading),
            count: showsSeparateSnapshotTime ? 3 : 2
        )
    }

    private var showsSeparateSnapshotTime: Bool {
        guard let snapshot = state.snapshot else { return false }
        guard let lastSuccessAt = state.lastSuccessAt else { return true }
        return abs(snapshot.fetchedAt.timeIntervalSince(lastSuccessAt)) >= 1
    }

    private var statusLabel: String {
        if !state.configuration.isEnabled {
            return "已停用"
        }
        if state.isRefreshing {
            return state.snapshot == nil ? "正在加载" : "正在刷新"
        }
        if state.error != nil {
            return "刷新失败"
        }
        if state.isStale {
            return "数据过期"
        }
        return state.snapshot == nil ? "等待刷新" : "数据正常"
    }

    private var statusTone: Color {
        if !state.configuration.isEnabled {
            return .secondary
        }
        if state.isRefreshing {
            return providerAccent
        }
        if state.error != nil {
            return CompactPopoverPalette.criticalColor
        }
        if state.isStale {
            return CompactPopoverPalette.warningColor
        }
        return state.snapshot == nil ? Color.secondary : positiveColor
    }

    private var statusDetail: String {
        if !state.configuration.isEnabled {
            return state.snapshot == nil
                ? "凭证已停用，尚无可用数据。"
                : "凭证已停用，当前显示最近一次缓存数据。"
        }
        if state.isRefreshing || state.error != nil || state.isStale || state.snapshot == nil {
            return UsageFormatter.statusText(state: state)
        }
        return "最近一次刷新已成功，当前显示官方接口返回的数据。"
    }

    private var positiveColor: Color {
        CompactPopoverPalette.positive(colorScheme)
    }

    private var providerAccent: Color {
        ProviderTheme.accentColor(for: state.configuration.providerID)
    }

    private var panelFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.045) : Color.black.opacity(0.025)
    }

    private var panelStroke: Color {
        colorScheme == .dark ? Color.white.opacity(0.10) : Color.black.opacity(0.075)
    }

    private var hasPlanDetails: Bool {
        guard state.snapshot != nil else { return false }
        switch state.configuration.providerID {
        case .routin, .volcengine, .commandCode, .xiaomi:
            return true
        case .deepseek, .glm, .newAPI, .opencode:
            return false
        }
    }

    private var hasAccountDetails: Bool {
        guard let snapshot = state.snapshot else { return false }
        if !snapshot.allowedModels.isEmpty {
            return true
        }
        return state.configuration.providerID == .routin && !snapshot.groupMultipliers.isEmpty
    }

    private var configurationRows: [ConfigurationRow] {
        state.configuration.metadata
            .filter { key, _ in
                let isSecretField = [
                    "accessKeyID",
                    "secretAccessKey",
                    "apiKey",
                    "token",
                    "password",
                    "cookie"
                ].contains(key)
                let isLegacyCommandCodeThreshold = state.configuration.providerID == .commandCode
                    && key == "balanceWarningThreshold"
                return !isSecretField && !isLegacyCommandCodeThreshold
            }
            .sorted { $0.key < $1.key }
            .map { ConfigurationRow(key: $0.key, value: $0.value) }
    }

    private func planFacts(_ snapshot: UsageSnapshot) -> [DetailFact] {
        var facts = [
            DetailFact("套餐", snapshot.planName.isEmpty ? planName : snapshot.planName),
            DetailFact("类型", snapshot.kind == .periodic ? "周期订阅" : "Token 资源包"),
            DetailFact("状态", displayStatus(snapshot)),
            DetailFact("计费模式", snapshot.billingMode ?? "接口未返回"),
            DetailFact(
                "订阅开始",
                snapshot.subscriptionStartAt.map { UsageFormatter.fullDateTime($0) } ?? "接口未返回"
            ),
            DetailFact(
                "订阅结束",
                snapshot.subscriptionEndAt.map { UsageFormatter.fullDateTime($0) } ?? "接口未返回"
            )
        ]

        if snapshot.kind == .periodic, state.configuration.providerID == .routin {
            facts.append(
                DetailFact(
                    "5 小时结束",
                    UsageFormatter.fullDateTime(snapshot.fiveHour?.windowEnd)
                )
            )
            facts.append(
                DetailFact(
                    "周结束",
                    UsageFormatter.fullDateTime(snapshot.weekly?.windowEnd)
                )
            )
        }

        return facts
    }

    private func subscriptionStatus(_ status: Int?) -> String {
        if state.error == nil, state.snapshot != nil {
            if state.isStale {
                return "已过期"
            }
            switch status {
            case 1, nil:
                return "正常"
            case let value?:
                return "状态 \(value)"
            }
        }
        return UsageFormatter.statusText(state: state)
    }

    private func displayStatus(_ snapshot: UsageSnapshot) -> String {
        if let statusText = snapshot.statusText, !statusText.isEmpty {
            return statusText
        }
        if state.configuration.providerID == .volcengine {
            return "接口未返回"
        }
        return subscriptionStatus(snapshot.status)
    }

    private func metadataLabel(for key: String) -> String {
        switch key {
        case "balanceWarningThreshold": "低额度预警值"
        case "baseURL": "接口地址"
        case "planType": "计划类型"
        case "region": "区域"
        case "userID": "用户 ID"
        case "usageKind": "用量类型"
        case "websiteURL": "官网地址"
        default: key
        }
    }

    private func metadataValue(for key: String, value: String) -> String {
        switch key {
        case "planType":
            return value == "coding" ? "Coding Plan" : "Agent Plan"
        case "usageKind":
            switch value {
            case "tokenPack": return "Token 资源包"
            case "api": return "API 按量"
            case "plan": return "Token Plan"
            default: return value
            }
        default:
            return value
        }
    }

    private var descriptor: ProviderDescriptor {
        ProviderRegistry.builtInDescriptors.first { $0.id == state.configuration.providerID }!
    }

    private var planName: String {
        if let planName = state.snapshot?.planName, !planName.isEmpty {
            return planName
        }
        if state.configuration.providerID == .volcengine {
            return state.configuration.metadata["planType"] == "coding" ? "Coding Plan" : "Agent Plan"
        }
        return state.configuration.credentialKind == .bearerAPIKey
            ? (state.configuration.providerID == .routin ? "Plan Key" : "Bearer Token")
            : "API Key"
    }
}

private struct DetailFact: Identifiable {
    let label: String
    let value: String

    var id: String { label }

    init(_ label: String, _ value: String) {
        self.label = label
        self.value = value
    }
}

private struct ConfigurationRow: Identifiable {
    let key: String
    let value: String

    var id: String { key }
}
