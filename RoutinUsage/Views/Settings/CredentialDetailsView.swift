import SwiftUI

enum CredentialDetailsLayout {
    static let minWidth: CGFloat = 500
    static let idealWidth: CGFloat = 680
    static let maxWidth: CGFloat = 820
    static let minHeight: CGFloat = 500
}

struct CredentialDetailsView: View {
    let state: KeyUsageState
    var onClose: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var copiedModelID: String?
    @State private var copyResetTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, SettingsDesign.modalPadding)
                .padding(.top, SettingsDesign.modalPadding)
                .padding(.bottom, SettingsDesign.modalHeaderSpacing)

            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    statusOverview
                    usageSection
                    detailsPanel
                }
                .padding(SettingsDesign.modalPadding)
                .frame(maxWidth: CredentialDetailsLayout.maxWidth, alignment: .leading)
            }
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

            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(state.configuration.displayName)
                        .font(.system(size: 21, weight: .semibold))
                        .tracking(-0.2)
                        .lineLimit(1)
                        .truncationMode(.tail)

                    enabledBadge
                }

                Text("\(descriptor.displayName) · \(planName)")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 16)

            SettingsCloseButton(
                action: {
                    if let onClose {
                        onClose()
                    } else {
                        dismiss()
                    }
                },
                help: "关闭凭证详情",
                accessibilityLabel: "关闭凭证详情"
            )
        }
    }

    private var enabledBadge: some View {
        SettingsStatusPill(
            text: state.configuration.isEnabled ? "已启用" : "已停用",
            tone: state.configuration.isEnabled ? .success : .neutral
        )
    }

    private var statusOverview: some View {
        SettingsPanel(
            title: "状态概览",
            systemImage: "waveform.path.ecg"
        ) {
            HStack(alignment: .top, spacing: 10) {
                statusBadge

                if !statusDetail.isEmpty {
                    Text(statusDetail)
                        .font(SettingsTypography.body)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 12)
            }

            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)

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
    }

    private var statusBadge: some View {
        SettingsStatusPill(
            text: statusLabel,
            tone: statusTone
        )
    }

    private var usageSection: some View {
        SettingsPanel(
            title: "用量指标",
            systemImage: "chart.bar.xaxis",
            trailingText: state.snapshot.map { "\($0.normalizedMetrics.count) 项" }
        ) {
            if let snapshot = state.snapshot, !snapshot.normalizedMetrics.isEmpty {
                usageMetrics(snapshot.normalizedMetrics)
            } else {
                emptyMetrics
            }
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
        SettingsNotice(
            tone: .neutral,
            message: "完成首次刷新后会在这里显示额度、余额和重置时间。",
            title: "暂无可展示的用量指标",
            systemImage: "chart.bar.xaxis"
        )
    }

    @ViewBuilder
    private var detailsPanel: some View {
        if hasPlanDetails || hasAccountDetails || !configurationRows.isEmpty {
            SettingsPanel {
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
            }
        }
    }

    @ViewBuilder
    private var planDetailsBlock: some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 14) {
                SettingsSectionHeader(
                    title: "套餐与订阅",
                    systemImage: "calendar.badge.clock"
                )

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
                SettingsSectionHeader(
                    title: "凭证与模型",
                    systemImage: "person.crop.circle"
                )

                if state.configuration.providerID == .routin,
                   !snapshot.groupMultipliers.isEmpty {
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        Text("分组倍率")
                            .font(SettingsTypography.secondary)
                            .foregroundStyle(.secondary)

                        Spacer()

                        Text(UsageFormatter.groupMultiplierText(snapshot.groupMultipliers))
                            .font(SettingsTypography.body.weight(.medium))
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
            SettingsSectionHeader(
                title: "配置",
                systemImage: "slider.horizontal.3"
            )

            Grid(alignment: .leading, horizontalSpacing: 18, verticalSpacing: 10) {
                ForEach(configurationRows) { row in
                    GridRow(alignment: .firstTextBaseline) {
                        Text(metadataLabel(for: row.key))
                            .font(SettingsTypography.secondary)
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
                            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
                            .foregroundStyle(providerAccent)
                            .help("在浏览器中打开 \(url.absoluteString)")
                            .accessibilityLabel("打开官网地址 \(url.absoluteString)")
                        } else {
                            Text(metadataValue(for: row.key, value: row.value))
                                .font(SettingsTypography.body)
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
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)

                Spacer()

                if !models.isEmpty {
                    Text("\(models.count) 个 · 点击复制")
                        .font(SettingsTypography.caption)
                        .foregroundStyle(.tertiary)
                }
            }

            if models.isEmpty {
                Text("接口未返回")
                    .font(SettingsTypography.body.weight(.medium))
                    .foregroundStyle(.secondary)
            } else {
                ModelIDChipFlow(
                    models: models,
                    copiedModelID: copiedModelID,
                    accentColor: ProviderTheme.accentColor(
                        for: state.configuration.providerID
                    ),
                    onCopied: copyModelID
                )
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

    private func overviewFact(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(SettingsTypography.secondary)
                .foregroundStyle(.secondary)
            Text(value)
                .font(SettingsTypography.body.weight(.medium))
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func detailFact(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(SettingsTypography.caption)
                .foregroundStyle(.tertiary)
            Text(value)
                .font(SettingsTypography.body.weight(.medium))
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var blockDivider: some View {
        Rectangle()
            .fill(SettingsDesign.dividerColor(colorScheme))
            .frame(height: 0.8)
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

    private var statusTone: SettingsNoticeTone {
        if !state.configuration.isEnabled {
            return .neutral
        }
        if state.isRefreshing {
            return .info
        }
        if state.error != nil {
            return .error
        }
        if state.isStale {
            return .warning
        }
        return state.snapshot == nil ? .neutral : .success
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
        return ""
    }

    private var providerAccent: Color {
        Color.accentColor
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
