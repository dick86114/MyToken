import AppKit
import SwiftUI

struct MenuBarManagementView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Bindable var environment: AppEnvironment
    let ordering: CredentialOrderingController
    var openCredentials: () -> Void = {}
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var hoveredManagementID: UUID?

    private var displayOrder: CredentialDisplayOrder {
        environment.settings.displayOrder
    }

    private var enabledIDs: Set<UUID> {
        Set(environment.store.visibleKeyIDs)
    }

    private var visibility: CredentialDisplayVisibility {
        displayOrder.visible(enabledIDs: enabledIDs)
    }

    private var displayOrderWithoutDisabledIDs: CredentialDisplayOrder {
        displayOrder.removingDisabledCredentials(enabledIDs: enabledIDs)
    }

    private var unifiedStates: [KeyUsageState] {
        visibility.popoverIDs.compactMap { environment.store.state(for: $0) }
    }

    private var menuBarStates: [KeyUsageState] {
        visibility.menuBarIDs.compactMap { environment.store.state(for: $0) }
    }

    var body: some View {
        SettingsPageScroll {
            SettingsPageHeader(
                title: "菜单栏管理",
                subtitle: "最多显示 \(CredentialDisplayOrder.maximumMenuBarCount) 个菜单栏指标"
            )

            workspace
            colorRulesSection
        }
        .onAppear(perform: pruneDisabledDisplayOrder)
        .onChange(of: enabledIDs) { _, _ in
            pruneDisabledDisplayOrder()
        }
    }

    private var workspace: some View {
        VStack(alignment: .leading, spacing: 14) {
            systemMenuBarStrip(highlightIndicator: true)
            cardList
        }
    }

    private var colorRulesSection: some View {
        SettingsGroup(
            title: "指示器外观",
            subtitle: "进度颜色只影响菜单栏指标；拖动色带上的两个分界点调整区间。"
        ) {
            MenuBarColorRulesEditor(rules: menuBarColorRulesBinding)
                .frame(maxWidth: 520, alignment: .leading)
                .padding(.vertical, 6)
        }
    }

    private var menuBarColorRulesBinding: Binding<MenuBarColorRules> {
        Binding(
            get: { environment.settings.menuBarColorRules },
            set: { environment.settings.menuBarColorRules = $0 }
        )
    }

    private func systemMenuBarStrip(highlightIndicator: Bool) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "apple.logo")
                .font(.system(size: 12, weight: .semibold))
            Text("Finder")
                .font(.system(size: 12, weight: .medium))
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
            Text("文件")
                .font(.system(size: 12))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)

            Spacer(minLength: 12)

            menuBarIndicators
                .padding(.horizontal, highlightIndicator ? 6 : 0)
                .padding(.vertical, highlightIndicator ? 3 : 0)
                .background(
                    highlightIndicator ? Color.accentColor.opacity(0.18) : .clear,
                    in: RoundedRectangle(cornerRadius: 5, style: .continuous)
                )

            Image(systemName: "wifi")
                .font(.system(size: 12))
                .foregroundStyle(.secondary)
            Text("10:09")
                .font(.system(size: 10, design: .monospaced))
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(.primary.opacity(0.09), lineWidth: 0.5)
        }
    }

    private var menuBarIndicators: some View {
        HStack(spacing: 0) {
            if menuBarStates.isEmpty {
                Text("未显示指标")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            } else {
                ForEach(menuBarStates, id: \.configuration.id) { state in
                    if let descriptor = descriptor(for: state) {
                        MenuBarIndicatorPreview(
                            state: state,
                            descriptor: descriptor,
                            metric: menuBarMetric(for: state),
                            style: menuBarStyle(for: state),
                            colorRules: environment.settings.menuBarColorRules
                        )
                        .equatable()
                    }
                }
            }
        }
    }

    private var cardList: some View {
        SettingsPanel(
            title: "凭证顺序",
            systemImage: "line.3.horizontal",
            trailingText: "\(unifiedStates.count) 个"
        ) {
            if unifiedStates.isEmpty {
                VStack(spacing: 14) {
                    ContentUnavailableView(
                        "还没有可管理的凭证",
                        systemImage: "key.slash",
                        description: Text("先在凭证管理中添加供应商凭证，再选择希望显示在菜单栏的指标。")
                    )

                    Button("前往凭证管理", action: openCredentials)
                        .liquidGlassButton()
                        .accessibilityLabel("打开凭证管理")
                }
                .frame(maxWidth: .infinity, minHeight: 180)
            } else {
                ReorderableCredentialCardList(
                    ids: unifiedStates.map(\.configuration.id),
                    itemHeight: 72,
                    itemSpacing: 10,
                    move: moveDisplay,
                    card: { (state: UUID) in
                        managementCard(state)
                    }
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func managementCard(_ id: UUID) -> some View {
        if let state = unifiedStates.first(where: { $0.configuration.id == id }) {
            managementCardContent(state)
        } else {
            Color.clear
        }
    }

    private func managementCardContent(_ state: KeyUsageState) -> some View {
        let id = state.configuration.id
        let isInMenuBar = displayOrder.menuBarCredentialIDs.contains(id)

        return HStack(spacing: 11) {
            Image(systemName: "line.3.horizontal")
                .font(.body.weight(.medium))
                .foregroundStyle(.secondary)
                .frame(width: 20)

            if let descriptor = descriptor(for: state) {
                MenuBarIndicatorPreview(
                    state: state,
                    descriptor: descriptor,
                    metric: menuBarMetric(for: state),
                    style: menuBarStyle(for: state),
                    colorRules: environment.settings.menuBarColorRules
                )
                .equatable()
                .frame(width: 36, height: 28)
            }

            providerLogo(for: state, size: 28)

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 7) {
                    Text(state.configuration.displayName)
                        .font(SettingsTypography.rowTitle)
                        .lineLimit(1)

                    if isInMenuBar {
                        Text("菜单栏")
                            .font(SettingsTypography.caption)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(.tint.opacity(0.14), in: Capsule())
                    }
                }

                Text("\(providerName(for: state)) · \(planName(for: state.configuration))")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .lineLimit(1)
            .layoutPriority(1)

            Spacer(minLength: 8)

            metricMenu(for: state)
            styleMenu(for: state)

            Button {
                setMenuBarMembership(isInMenuBar: !isInMenuBar, id: id)
            } label: {
                Image(
                    systemName: isInMenuBar
                        ? "checkmark.circle.fill"
                        : "plus.circle.fill"
                )
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(isInMenuBar ? CompactPopoverPalette.positive(colorScheme) : Color.accentColor)
                .frame(width: 34, height: 34)
                .background {
                    Circle()
                        .fill(
                            (isInMenuBar
                                ? CompactPopoverPalette.positive(colorScheme)
                                : Color.accentColor)
                                .opacity(0.10)
                        )
                }
                .contentShape(Circle())
            }
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.94))
            .disabled(!isInMenuBar && menuBarStates.count >= CredentialDisplayOrder.maximumMenuBarCount)
            .help(isInMenuBar ? "从菜单栏移除 \(state.configuration.displayName)" : "添加到菜单栏 \(state.configuration.displayName)")
            .accessibilityLabel(isInMenuBar ? "从菜单栏移除" : "添加到菜单栏")
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(
                    hoveredManagementID == id
                        ? SettingsDesign.hoverFill(colorScheme)
                        : Color.clear
                )
        }
        .onHover { isHovered in
            hoveredManagementID = isHovered
                ? id
                : (hoveredManagementID == id ? nil : hoveredManagementID)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(
            "\(state.configuration.displayName)，\(providerName(for: state))，\(planName(for: state.configuration))，\(isInMenuBar ? "已加入菜单栏" : "未加入菜单栏")"
        )
        .accessibilityAction(named: isInMenuBar ? "从菜单栏移除" : "添加到菜单栏") {
            setMenuBarMembership(isInMenuBar: !isInMenuBar, id: id)
        }
        .accessibilityAction(named: "上移") {
            moveByOffset(id: id, offset: -1)
        }
        .accessibilityAction(named: "下移") {
            moveByOffset(id: id, offset: 1)
        }
    }

    private func pruneDisabledDisplayOrder() {
        let updated = displayOrderWithoutDisabledIDs
        guard updated != displayOrder else { return }
        environment.settings.displayOrder = updated
    }

    private func moveDisplay(_ draggedID: UUID, to targetIndex: Int) -> Bool {
        let updated = displayOrderWithoutDisabledIDs.reorderingDisplay(
            id: draggedID,
            toIndex: targetIndex
        )
        guard updated != displayOrder else { return false }

        var transaction = Transaction()
        transaction.animation = nil
        withTransaction(transaction) {
            environment.settings.displayOrder = updated
        }
        return true
    }

    private func moveByOffset(id: UUID, offset: Int) {
        let ids = unifiedStates.map(\.configuration.id)
        guard let source = ids.firstIndex(of: id) else { return }
        let target = max(0, min(ids.count - 1, source + offset))
        guard target != source else { return }
        _ = moveDisplay(id, to: target)
    }

    private func setMenuBarMembership(isInMenuBar: Bool, id: UUID) {
        var updated = displayOrderWithoutDisabledIDs
        if isInMenuBar {
            let index = visibility.popoverIDs.firstIndex(of: id) ?? visibility.popoverIDs.count
            updated = updated.addingToMenuBar(id, toIndex: index)
        } else {
            updated = updated.removingFromMenuBar(id)
        }
        updated.menuBarCredentialIDs = updated.popoverCredentialIDs.filter {
            updated.menuBarCredentialIDs.contains($0)
        }

        withAnimation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.84)) {
            environment.settings.displayOrder = updated
        }
    }

    private func providerLogo(
        for state: KeyUsageState,
        size: CGFloat
    ) -> some View {
        ProviderLogoBadge(
            providerID: state.configuration.providerID,
            size: size,
            cornerRadius: max(5, size * 0.25)
        )
    }

    private func descriptor(for state: KeyUsageState) -> ProviderDescriptor? {
        ProviderRegistry.builtInDescriptors.first { $0.id == state.configuration.providerID }
    }

    private func menuBarMetric(for state: KeyUsageState) -> NormalizedUsageMetric? {
        menuBarResolution(for: state).metric
    }

    private func menuBarStyle(for state: KeyUsageState) -> MenuBarIndicatorStyle {
        environment.settings.usagePreferences(for: state.configuration.id).menuBarStyle
    }

    private func menuBarResolution(for state: KeyUsageState) -> MenuBarMetricResolution {
        let id = state.configuration.id
        let preferences = environment.settings.usagePreferences(for: id)
        let capabilities = environment.providerRegistry?.metricCapabilities(for: state.configuration) ?? []
        return MenuBarMetricResolver.resolve(
            selectedMetricID: preferences.menuBarMetricID,
            metrics: state.snapshot?.normalizedMetrics ?? [],
            capabilities: capabilities
        )
    }

    private func metricMenu(for state: KeyUsageState) -> some View {
        let resolution = menuBarResolution(for: state)

        return Menu {
            Button("自动") {
                setMenuBarMetric(nil, for: state.configuration.id)
            }

            ForEach(metricOptions(for: state)) { option in
                Button(option.label) {
                    setMenuBarMetric(option.metricID, for: state.configuration.id)
                }
            }
        } label: {
            HStack(spacing: 5) {
                Label(
                    selectedMetricTitle(for: state, resolution: resolution),
                    systemImage: "gauge.with.dots.needle.33percent"
                )
                .font(.system(size: 11.5, weight: .medium))
                .lineLimit(1)

                Image(systemName: "chevron.down")
                    .font(.system(size: 8, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 8)
            .frame(minHeight: 28)
            .background(
                SettingsDesign.fieldFill(colorScheme),
                in: RoundedRectangle(cornerRadius: 8, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
            }
            .contentShape(Rectangle())
        }
        .menuStyle(.borderlessButton)
        .fixedSize()
        .help("设置 \(state.configuration.displayName) 的菜单栏指标")
    }

    private func metricOptions(for state: KeyUsageState) -> [UsageMetricCapability] {
        MenuBarMetricResolver.options(
            metrics: state.snapshot?.normalizedMetrics ?? [],
            capabilities: environment.providerRegistry?.metricCapabilities(for: state.configuration) ?? []
        )
    }

    private func selectedMetricTitle(
        for state: KeyUsageState,
        resolution: MenuBarMetricResolution
    ) -> String {
        if resolution.isFallback {
            return "自动（原指标当前不可用）"
        }
        guard let selectedMetricID = resolution.selectedMetricID else {
            return "自动"
        }
        let metricTitle = state.snapshot?.normalizedMetrics.first { $0.id == selectedMetricID }?.label
        let capabilityTitle = environment.providerRegistry?
            .metricCapabilities(for: state.configuration)
            .first { $0.metricID == selectedMetricID }?
            .label
        return metricTitle ?? capabilityTitle ?? selectedMetricID
    }

    private func setMenuBarMetric(_ metricID: String?, for id: UUID) {
        var preferences = environment.settings.usagePreferences(for: id)
        preferences.menuBarMetricID = metricID
        environment.settings.setUsagePreferences(preferences, for: id)
    }

    private func styleMenu(for state: KeyUsageState) -> some View {
        let metric = menuBarMetric(for: state)
        let availableStyles = MenuBarIndicatorStyle.availableStyles(for: metric)
        let storedStyle = menuBarStyle(for: state)
        let selectedStyle = availableStyles.contains(storedStyle)
            ? storedStyle
            : availableStyles[0]

        return Menu {
            ForEach(availableStyles, id: \.self) { style in
                Button {
                    setMenuBarStyle(style, for: state.configuration.id)
                } label: {
                    if style == selectedStyle {
                        Label(style.title(for: metric), systemImage: "checkmark")
                    } else {
                        Text(style.title(for: metric))
                    }
                }
            }
        } label: {
            HStack(spacing: 3) {
                Image(systemName: selectedStyle.systemImage(for: metric))
                    .font(.system(size: 13, weight: .medium))

                Text(selectedStyle.title(for: metric))
                    .font(.system(size: 11.5, weight: .medium))
                    .lineLimit(1)

                Image(systemName: "chevron.down")
                    .font(.system(size: 8, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 8)
            .frame(minHeight: 28)
            .background(
                SettingsDesign.fieldFill(colorScheme),
                in: RoundedRectangle(cornerRadius: 8, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
            }
            .contentShape(Rectangle())
        }
        .menuStyle(.borderlessButton)
        .fixedSize()
        .help("菜单栏指标样式：\(selectedStyle.title(for: metric))")
        .accessibilityLabel("菜单栏指标样式")
    }

    private func setMenuBarStyle(_ style: MenuBarIndicatorStyle, for id: UUID) {
        var preferences = environment.settings.usagePreferences(for: id)
        preferences.menuBarStyle = style
        environment.settings.setUsagePreferences(preferences, for: id)
    }

    private func providerName(for state: KeyUsageState) -> String {
        descriptor(for: state)?.displayName ?? state.configuration.providerID.rawValue
    }

    private func planName(for configuration: KeyConfiguration) -> String {
        if configuration.providerID == .volcengine {
            return configuration.metadata["planType"] == "coding" ? "Coding Plan" : "Agent Plan"
        }
        switch configuration.credentialKind {
        case .bearerAPIKey:
            return configuration.providerID == .routin ? "Plan Key" : "Bearer Token"
        case .apiKey:
            return "API Key"
        case .accessKeyPair:
            return "Access Key Pair"
        }
    }
}
