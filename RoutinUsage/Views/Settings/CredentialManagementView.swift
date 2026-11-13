import SwiftUI

struct CredentialFilter: Equatable {
    var status: CredentialStatusFilter = .all
    var provider: ProviderID?
    var searchText = ""
}

enum CredentialStatusFilter: String, CaseIterable, Identifiable {
    case all, enabled, attention, disabled

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "全部"
        case .enabled: "启用"
        case .attention: "需注意"
        case .disabled: "停用"
        }
    }
}

extension KeyUsageState: Identifiable {
    var id: UUID { configuration.id }
}

private struct CredentialAlertSettingsPresentation: Identifiable {
    let id: UUID
    let title: String
    let model: CredentialAlertSettingsModel
}

struct CredentialManagementView: View {
    @Bindable var environment: AppEnvironment
    @State private var model: CredentialManagementModel
    @State private var editor: EditorPresentation?
    @State private var alertSettingsPresentation: CredentialAlertSettingsPresentation?
    @State private var detailsState: KeyUsageState?
    @State private var showsTransferToAndroid = false
    @FocusState private var isSearchFocused: Bool

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(environment: AppEnvironment, ordering: CredentialOrderingController) {
        self.environment = environment
        let model = CredentialManagementModel(environment: environment, ordering: ordering)
        _model = State(initialValue: model)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                SettingsPageHeader(
                    title: "凭证管理",
                    subtitle: headerSubtitle,
                    trailing: AnyView(addButton)
                )
                summaryStrip
                filterBar
                providerSection
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .sheet(isPresented: $showsTransferToAndroid) {
            TransferToAndroidView(environment: environment) {
                showsTransferToAndroid = false
            }
        }
        .confirmationDialog(
            "确定删除这个凭证？",
            isPresented: Binding(
                get: { model.pendingDeletion != nil },
                set: { if !$0 { model.pendingDeletion = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) { model.deletePending() }
            Button("取消", role: .cancel) { model.pendingDeletion = nil }
        } message: {
            Text("将同时删除本地保存的密钥和用量缓存，此操作无法撤销。")
        }
        .alert(
            model.operationNotice?.title ?? "无法完成操作",
            isPresented: Binding(
                get: { model.operationNotice != nil },
                set: { if !$0 { model.clearOperationNotice() } }
            )
        ) {
            Button("好") { model.clearOperationNotice() }
        } message: {
            Text(model.operationNotice?.message ?? "发生未知错误")
        }
        .liquidGlassOverlay(item: $editor) { presentation in
            credentialEditor(presentation)
        }
        .liquidGlassOverlay(item: $alertSettingsPresentation) { presentation in
            CredentialAlertSettingsView(
                title: presentation.title,
                model: presentation.model,
                onClose: { alertSettingsPresentation = nil }
            )
        }
        .liquidGlassOverlay(item: $detailsState) { state in
            CredentialDetailsView(state: state, onClose: closeDetails)
                .frame(width: 640)
                .frame(minHeight: 500, maxHeight: 720)
        }
        .onReceive(NotificationCenter.default.publisher(for: .myTokenPresentCredential)) { notification in
            guard
                let keyID = notification.userInfo?["credentialID"] as? UUID,
                let state = environment.store.state(for: keyID)
            else {
                return
            }
            detailsState = state
            environment.consumePendingSettingsCredentialID(keyID)
        }
        .onChange(of: model.filter.status) { _, _ in
            model.expandGroupsForActiveFilter()
        }
        .onChange(of: model.filter.searchText) { _, _ in
            model.expandGroupsForActiveFilter()
        }
    }

    private var headerSubtitle: String {
        "\(model.storeCount) 个凭证 · \(model.providerCount) 个供应商"
    }

    private var addButton: some View {
        HStack(spacing: 10) {
            Button {
                showsTransferToAndroid = true
            } label: {
                Label("迁移到 Android", systemImage: "iphone.and.arrow.forward")
            }
            .liquidGlassButton()
            .accessibilityLabel("迁移到 Android")

            Button {
                editor = .add
            } label: {
                Label("添加凭证", systemImage: "plus")
            }
            .liquidGlassButton(prominent: true)
            .accessibilityLabel("添加凭证")
        }
    }

    private var summaryStrip: some View {
        HStack(spacing: 0) {
            summaryItem(
                title: "已启用",
                value: model.enabledCount,
                color: .primary
            )
            summaryDivider
            summaryItem(
                title: "需注意",
                value: model.attentionCount,
                color: model.attentionCount > 0
                    ? CompactPopoverPalette.warningColor
                    : .secondary
            )
            summaryDivider
            summaryItem(
                title: "已停用",
                value: model.disabledCount,
                color: .secondary
            )
            summaryDivider
            summaryItem(
                title: "供应商",
                value: model.providerCount,
                color: .primary
            )
        }
        .padding(.vertical, 12)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.045 : 0.028))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(
                    Color.primary.opacity(colorScheme == .dark ? 0.09 : 0.065),
                    lineWidth: 1
                )
        }
    }

    private func summaryItem(
        title: String,
        value: Int,
        color: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(value)")
                .font(.system(size: 21, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(color)

            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title) \(value)")
    }

    private var summaryDivider: some View {
        Rectangle()
            .fill(Color.primary.opacity(colorScheme == .dark ? 0.09 : 0.07))
            .frame(width: 1, height: 28)
    }

    private var filterBar: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                searchField
                Spacer(minLength: 8)

                Picker("状态", selection: $model.filter.status) {
                    ForEach(CredentialStatusFilter.allCases) { status in
                        Text(status.title).tag(status)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 270)
                .accessibilityLabel("凭证状态筛选")
            }

            ProviderFilterChips(
                providers: model.providerFilterDescriptors,
                counts: model.providerFilterCounts,
                selectedProviderID: model.filter.provider,
                select: { providerID in
                    model.filter.provider = providerID
                    if let providerID {
                        model.setProviderExpanded(providerID, expanded: true)
                    }
                }
            )
        }
    }

    private var searchField: some View {
        HStack(spacing: 7) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(isSearchFocused ? Color.accentColor : Color.secondary)

            TextField("搜索别名或供应商", text: $model.filter.searchText)
                .textFieldStyle(.plain)
                .font(.system(size: 13))
                .focused($isSearchFocused)
                .accessibilityLabel("搜索别名或供应商")

            if !model.filter.searchText.isEmpty {
                Button {
                    model.filter.searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 12))
                        .foregroundStyle(.tertiary)
                }
                .buttonStyle(.plain)
                .help("清除搜索")
                .accessibilityLabel("清除搜索")
            }
        }
        .padding(.horizontal, 10)
        .frame(height: 30)
        .background {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.075 : 0.04))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(
                    isSearchFocused
                        ? Color.accentColor.opacity(0.70)
                        : Color.secondary.opacity(0.16),
                    lineWidth: isSearchFocused ? 1.5 : 1
                )
        }
        .shadow(
            color: isSearchFocused ? Color.accentColor.opacity(0.10) : .clear,
            radius: 3
        )
        .frame(minWidth: 170, idealWidth: 300, maxWidth: 360)
    }

    @ViewBuilder
    private var providerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: 7) {
                Text("供应商")
                    .font(.system(size: 14, weight: .semibold))

                Text("\(groups.count)")
                    .font(.caption.weight(.medium))
                    .monospacedDigit()
                    .foregroundStyle(.tertiary)

                Spacer(minLength: 12)

                if !groups.isEmpty {
                    Button {
                        withAnimation(groupAnimation) {
                            model.toggleAllVisibleGroups()
                        }
                    } label: {
                        Label(
                            model.allVisibleGroupsExpanded ? "全部收起" : "全部展开",
                            systemImage: model.allVisibleGroupsExpanded
                                ? "rectangle.compress.vertical"
                                : "rectangle.expand.vertical"
                        )
                        .font(.caption)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
                    .help(model.allVisibleGroupsExpanded ? "收起所有供应商" : "展开所有供应商")
                }
            }

            if groups.isEmpty {
                ContentUnavailableView(
                    model.hasCredentials ? "没有匹配的凭证" : "尚未添加凭证",
                    systemImage: model.hasCredentials
                        ? "line.3.horizontal.decrease.circle"
                        : "key.slash",
                    description: Text(
                        model.hasCredentials
                            ? "调整状态、供应商或搜索条件后再试"
                            : "添加供应商凭证后即可管理用量展示"
                    )
                )
                .frame(maxWidth: .infinity, minHeight: 230)
                .accessibilityLabel(model.hasCredentials ? "没有匹配的凭证" : "尚未添加凭证")
            } else {
                VStack(spacing: 10) {
                    ForEach(groups) { group in
                        providerGroup(group)
                    }
                }
            }
        }
    }

    private func providerGroup(_ group: CredentialProviderGroup) -> some View {
        let isExpanded = model.expandedProviderIDs.contains(group.provider.id)

        return VStack(spacing: 0) {
            Button {
                withAnimation(groupAnimation) {
                    model.toggleProviderExpansion(group.provider.id)
                }
            } label: {
                groupHeader(group, isExpanded: isExpanded)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(groupAccessibilityLabel(group, isExpanded: isExpanded))
            .accessibilityHint("展开或收起该供应商的凭证")

            if isExpanded {
                Divider()
                    .overlay(Color.primary.opacity(colorScheme == .dark ? 0.09 : 0.065))
                    .padding(.horizontal, 14)

                VStack(spacing: 0) {
                    ForEach(group.states, id: \.configuration.id) { state in
                        credentialRow(state, provider: group.provider)

                        if state.configuration.id != group.states.last?.configuration.id {
                            Divider()
                                .overlay(Color.primary.opacity(colorScheme == .dark ? 0.075 : 0.05))
                                .padding(.leading, 14)
                        }
                    }
                }
                .transition(.opacity)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.032 : 0.022))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(
                    Color.primary.opacity(colorScheme == .dark ? 0.085 : 0.06),
                    lineWidth: 1
                )
        }
    }

    private func credentialRow(
        _ state: KeyUsageState,
        provider: ProviderDescriptor
    ) -> some View {
        let planType = model.planTitle(for: state.configuration)
        let attention = model.attention(for: state)

        return CredentialCompactRow(
            state: state,
            planType: planType,
            attention: attention,
            attentionMetric: model.attentionMetric(for: state),
            alertsEnabled: environment.settings
                .usagePreferences(for: state.configuration.id)
                .notificationsEnabled,
            accessibilityLabel: model.accessibilitySummary(
                state,
                providerName: provider.displayName,
                planType: planType,
                attention: attention
            ),
            setEnabled: { enabled in
                model.setEnabled(state.configuration.id, enabled: enabled)
            },
            openDetails: {
                detailsState = state
            },
            share: {
                if let content = UsageShareContentBuilder.build(state: state) {
                    UsageSharePanelController.shared.present(content: content)
                }
            },
            edit: {
                editor = .edit(state.configuration)
            },
            showAlertSettings: {
                presentAlertSettings(for: state)
            },
            refresh: {
                refresh(state)
            },
            delete: {
                model.pendingDeletion = state.configuration
            }
        )
    }

    private func groupHeader(
        _ group: CredentialProviderGroup,
        isExpanded: Bool
    ) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "chevron.right")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.tertiary)
                .rotationEffect(.degrees(isExpanded ? 90 : 0))
                .frame(width: 10)

            ProviderLogoMark(
                providerID: group.provider.id,
                size: 26,
                cornerRadius: 7
            )

            VStack(alignment: .leading, spacing: 1) {
                Text(group.provider.displayName)
                    .font(.system(size: 13, weight: .semibold))
                    .lineLimit(1)

                Text(groupSubtitle(group))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 12)

            if group.attentionCount > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 9, weight: .semibold))
                    Text("\(group.attentionCount) 需注意")
                        .font(.system(size: 10, weight: .semibold))
                        .monospacedDigit()
                }
                .foregroundStyle(groupAttentionColor(group))
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background {
                    Capsule()
                        .fill(groupAttentionColor(group).opacity(0.10))
                }
                .overlay {
                    Capsule()
                        .strokeBorder(groupAttentionColor(group).opacity(0.18), lineWidth: 1)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }

    private var groups: [CredentialProviderGroup] {
        model.groups
    }

    private func groupSubtitle(_ group: CredentialProviderGroup) -> String {
        if group.enabledCount == 0 {
            return "\(group.states.count) 个凭证 · 已全部停用"
        }
        if group.enabledCount == group.states.count {
            return "\(group.states.count) 个凭证 · 全部启用"
        }
        return "\(group.states.count) 个凭证 · \(group.enabledCount) 个启用"
    }

    private var groupAnimation: Animation? {
        reduceMotion ? nil : .easeInOut(duration: 0.16)
    }

    private func groupAttentionColor(_ group: CredentialProviderGroup) -> Color {
        let hasCritical = group.states.contains { state in
            guard let attention = model.attention(for: state) else {
                return false
            }
            switch attention {
            case .refreshFailed, .subscriptionExpired, .criticalMetric:
                return true
            case .staleData, .warningMetric:
                return false
            }
        }
        return hasCritical
            ? CompactPopoverPalette.criticalColor
            : CompactPopoverPalette.warningColor
    }

    private func groupAccessibilityLabel(
        _ group: CredentialProviderGroup,
        isExpanded: Bool
    ) -> String {
        var parts = [
            group.provider.displayName,
            "\(group.states.count) 个凭证",
            "\(group.enabledCount) 个启用"
        ]
        if group.attentionCount > 0 {
            parts.append("\(group.attentionCount) 个需注意")
        }
        parts.append(isExpanded ? "已展开" : "已收起")
        return parts.joined(separator: "，")
    }

    private func refresh(_ state: KeyUsageState) {
        Task {
            if state.error == nil {
                await environment.refreshCredential(state.configuration.id)
            } else {
                await environment.retryCredential(state.configuration.id)
            }
        }
    }

    private func presentAlertSettings(for state: KeyUsageState) {
        alertSettingsPresentation = CredentialAlertSettingsPresentation(
            id: state.configuration.id,
            title: "\(state.configuration.displayName) 提醒设置",
            model: alertSettingsModel(for: state)
        )
    }

    private func closeDetails() {
        detailsState = nil
    }

    @ViewBuilder
    private func credentialEditor(_ presentation: EditorPresentation) -> some View {
        switch presentation {
        case .add:
            CredentialEditorView(
                save: model.addValidatedCredential,
                onClose: { editor = nil }
            )
        case let .edit(configuration):
            CredentialEditorView(
                title: "编辑凭证",
                initialProviderID: configuration.providerID,
                initialName: configuration.displayName,
                initialSecret: environment.readKey(id: configuration.id) ?? "",
                initialMetadata: configuration.metadata
            ) { input in
                try await model.updateValidatedCredential(id: configuration.id, input: input)
            } onSaved: {} onClose: { editor = nil }
        }
    }

    private enum EditorPresentation: Identifiable {
        case add
        case edit(KeyConfiguration)

        var id: String {
            switch self {
            case .add:
                "add"
            case let .edit(configuration):
                configuration.id.uuidString
            }
        }
    }
}

private extension CredentialManagementView {
    func alertSettingsModel(for state: KeyUsageState) -> CredentialAlertSettingsModel {
        CredentialAlertSettingsModel(
            credentialID: state.configuration.id,
            preferences: environment.settings.usagePreferences(for: state.configuration.id),
            metrics: state.snapshot?.normalizedMetrics ?? [],
            capabilities: environment.providerRegistry?.metricCapabilities(for: state.configuration) ?? []
        ) { preferences in
            environment.settings.setUsagePreferences(preferences, for: state.configuration.id)
        }
    }
}
