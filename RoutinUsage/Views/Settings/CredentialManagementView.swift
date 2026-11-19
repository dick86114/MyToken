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
    @State private var hoveredProviderID: ProviderID?
    @FocusState private var isSearchFocused: Bool

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(environment: AppEnvironment, ordering: CredentialOrderingController) {
        self.environment = environment
        let model = CredentialManagementModel(environment: environment, ordering: ordering)
        _model = State(initialValue: model)
    }

    var body: some View {
        SettingsPageScroll {
            SettingsPageHeader(
                title: "凭证管理",
                subtitle: headerSubtitle,
                trailing: AnyView(addButton)
            )
            if showsSummaryStrip {
                summaryStrip
            }
            filterBar
            providerSection
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
        .liquidGlassOverlay(
            item: $editor,
            dismissOnBackgroundTap: false,
            dismissOnEscape: false
        ) { presentation in
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
                .frame(width: 680)
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
        .onChange(of: model.filter.provider) { _, providerID in
            if let providerID {
                model.setProviderExpanded(providerID, expanded: true)
            }
        }
    }

    private var headerSubtitle: String {
        "\(model.storeCount) 个凭证 · \(model.providerCount) 个供应商"
    }

    private var showsSummaryStrip: Bool {
        model.storeCount >= 3
            || model.attentionCount > 0
            || model.disabledCount > 0
    }

    private var addButton: some View {
        Button {
            editor = .add
        } label: {
            Label("添加凭证", systemImage: "plus")
        }
        .liquidGlassButton(prominent: true)
        .accessibilityLabel("添加凭证")
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
        .padding(.vertical, 11)
        .background {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.038 : 0.022))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    Color.primary.opacity(colorScheme == .dark ? 0.08 : 0.055),
                    lineWidth: 0.8
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
                .font(.system(size: 20, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(color)

            Text(title)
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title) \(value)")
    }

    private var summaryDivider: some View {
        Rectangle()
            .fill(Color.primary.opacity(colorScheme == .dark ? 0.09 : 0.07))
            .frame(width: 0.8, height: 26)
    }

    private var filterBar: some View {
        HStack(spacing: 8) {
            searchField
            Spacer(minLength: 8)
            statusFilterMenu
            providerFilterMenu

            if hasActiveFilter {
                Button {
                    model.filter = CredentialFilter()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.secondary)
                        .frame(width: 28, height: 28)
                        .contentShape(Circle())
                }
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.94))
                .help("清除状态和供应商筛选")
                .accessibilityLabel("清除状态和供应商筛选")
            }
        }
    }

    private var statusFilterMenu: some View {
        CredentialFilterMenuControl(
            title: "状态",
            value: model.filter.status == .all ? "全部" : model.filter.status.title,
            systemImage: "circle.dashed",
            isActive: model.filter.status != .all,
            help: "按凭证状态筛选"
        ) {
            Picker(selection: $model.filter.status) {
                ForEach(CredentialStatusFilter.allCases) { status in
                    Text(status.title).tag(status)
                }
            } label: {
                EmptyView()
            }
            .labelsHidden()
            .pickerStyle(.inline)
        }
    }

    private var providerFilterMenu: some View {
        CredentialFilterMenuControl(
            title: "供应商",
            value: model.filter.provider.map(providerDisplayName) ?? "全部",
            systemImage: "square.stack.3d.up",
            isActive: model.filter.provider != nil,
            help: "按供应商筛选"
        ) {
            Picker(selection: $model.filter.provider) {
                Text("全部供应商").tag(ProviderID?.none)

                ForEach(model.providerFilterDescriptors) { provider in
                    Text(
                        "\(provider.displayName) · \(model.providerFilterCounts[provider.id, default: 0])"
                    )
                    .tag(Optional(provider.id))
                }
            } label: {
                EmptyView()
            }
            .labelsHidden()
            .pickerStyle(.inline)
        }
    }

    private func providerDisplayName(_ providerID: ProviderID) -> String {
        model.providerFilterDescriptors
            .first(where: { $0.id == providerID })?
            .displayName
            ?? providerID.rawValue
    }

    private var hasActiveFilter: Bool {
        model.filter.status != .all || model.filter.provider != nil
    }

    private var searchField: some View {
        SettingsFieldSurface(
            isFocused: isSearchFocused,
            height: 32
        ) {
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
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.94))
                .help("清除搜索")
                .accessibilityLabel("清除搜索")
            }
        }
        .frame(minWidth: 170, idealWidth: 280, maxWidth: 340)
    }

    @ViewBuilder
    private var providerSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: 7) {
                Text("供应商")
                    .font(SettingsTypography.sectionTitle)

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
                    .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.98))
                    .foregroundStyle(.secondary)
                    .help(model.allVisibleGroupsExpanded ? "收起所有供应商" : "展开所有供应商")
                }
            }

            if groups.isEmpty {
                VStack(spacing: 14) {
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

                    if model.hasCredentials {
                        Button("清除筛选") {
                            model.filter = CredentialFilter()
                        }
                        .liquidGlassButton()
                        .accessibilityLabel("清除凭证筛选条件")
                    } else {
                        Button("添加凭证") {
                            editor = .add
                        }
                        .liquidGlassButton(prominent: true)
                        .accessibilityLabel("添加第一个凭证")
                    }
                }
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
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.995))
            .onHover { isHovered in
                hoveredProviderID = isHovered
                    ? group.provider.id
                    : (hoveredProviderID == group.provider.id ? nil : hoveredProviderID)
            }
            .accessibilityLabel(groupAccessibilityLabel(group, isExpanded: isExpanded))
            .accessibilityHint("展开或收起该供应商的凭证")

            if isExpanded {
                VStack(spacing: 2) {
                    ForEach(group.states, id: \.configuration.id) { state in
                        credentialRow(state, provider: group.provider)
                    }
                }
                .padding(.leading, 18)
                .padding(.trailing, 8)
                .padding(.top, 2)
                .padding(.bottom, 8)
                .transition(.opacity)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .background {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(CompactPopoverPalette.surface(.card, colorScheme))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    SettingsDesign.fieldBorder(colorScheme),
                    lineWidth: 0.8
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
                .animation(
                    reduceMotion
                        ? nil
                        : .spring(response: 0.3, dampingFraction: 0.86),
                    value: isExpanded
                )

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
                    .font(SettingsTypography.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 12)

            if group.attentionCount > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 9, weight: .semibold))
                    Text("\(group.attentionCount) 需注意")
                        .font(.system(size: 10.5, weight: .semibold))
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
        .background {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(
                    hoveredProviderID == group.provider.id
                        ? SettingsDesign.hoverFill(colorScheme)
                        : Color.clear
                )
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
        }
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
        reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.88)
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

private struct CredentialFilterMenuControl<MenuContent: View>: View {
    let title: String
    let value: String
    let systemImage: String
    let isActive: Bool
    let help: String
    private let menuContent: MenuContent

    @Environment(\.colorScheme) private var colorScheme
    @State private var isHovered = false

    init(
        title: String,
        value: String,
        systemImage: String,
        isActive: Bool,
        help: String,
        @ViewBuilder menuContent: () -> MenuContent
    ) {
        self.title = title
        self.value = value
        self.systemImage = systemImage
        self.isActive = isActive
        self.help = help
        self.menuContent = menuContent()
    }

    var body: some View {
        Menu {
            menuContent
        } label: {
            HStack(spacing: 5) {
                Image(systemName: systemImage)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(
                        isActive
                            ? Color.accentColor
                            : (isHovered ? Color.primary : Color.secondary)
                    )

                Text("\(title) \(value)")
                    .font(.system(size: 10.5, weight: .medium))
                    .foregroundStyle(
                        isActive ? Color.accentColor : Color.primary
                    )
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: 124, alignment: .leading)

                Image(systemName: "chevron.down")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(
                        isActive
                            ? Color.accentColor
                            : Color.secondary
                    )
            }
            .padding(.horizontal, 9)
            .frame(minHeight: 32)
            .background {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(
                        isActive
                            ? Color.accentColor.opacity(0.09)
                            : (isHovered
                                ? SettingsDesign.hoverFill(colorScheme)
                                : SettingsDesign.fieldFill(colorScheme))
                    )
            }
            .overlay {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .strokeBorder(
                        isActive
                            ? Color.accentColor.opacity(0.30)
                            : SettingsDesign.fieldBorder(colorScheme),
                        lineWidth: 0.8
                    )
            }
            .contentShape(Rectangle())
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize(horizontal: true, vertical: false)
        .onHover { isHovered = $0 }
        .animation(.easeOut(duration: 0.12), value: isHovered)
        .help(help)
        .accessibilityLabel("\(title)筛选")
        .accessibilityValue(value)
    }
}
