import AppKit
import SwiftUI

struct MenuBarManagementView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Bindable var environment: AppEnvironment
    let ordering: CredentialOrderingController
    var openCredentials: () -> Void = {}
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var hoveredManagementID: UUID?

    private let orderRowHeight: CGFloat = 56
    private let appearancePanelWidth: CGFloat = 310

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
                subtitle: "拖动调整显示顺序，右侧实时调整指示器外观",
                trailing: AnyView(headerSummary)
            )

            workspace
        }
        .onAppear(perform: pruneDisabledDisplayOrder)
        .onChange(of: enabledIDs) { _, _ in
            pruneDisabledDisplayOrder()
        }
    }

    private var workspace: some View {
        VStack(alignment: .leading, spacing: 14) {
            systemMenuBarStrip(highlightIndicator: true)

            HStack(alignment: .top, spacing: 14) {
                cardList
                    .frame(maxWidth: .infinity, alignment: .topLeading)

                appearancePanel
                    .frame(width: appearancePanelWidth, alignment: .topLeading)
            }
        }
    }

    private var headerSummary: some View {
        HStack(spacing: 8) {
            headerSummaryChip(
                value: "\(menuBarStates.count) / \(CredentialDisplayOrder.maximumMenuBarCount)",
                title: "显示中"
            )
            headerSummaryChip(
                value: "\(unifiedStates.count)",
                title: "个凭证"
            )
        }
    }

    private func headerSummaryChip(value: String, title: String) -> some View {
        HStack(spacing: 5) {
            Text(value)
                .font(.system(size: 12, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.primary)

            Text(title)
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 9)
        .frame(minHeight: 30)
        .background {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(SettingsDesign.fieldFill(colorScheme))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title) \(value)")
    }

    private var appearancePanel: some View {
        let shape = RoundedRectangle(
            cornerRadius: SettingsDesign.cardCornerRadius,
            style: .continuous
        )

        return VStack(spacing: 0) {
            SettingsSectionHeader(
                title: "指示器外观",
                systemImage: "chart.bar.xaxis",
                trailingText: "实时预览"
            )
            .padding(.horizontal, 14)
            .frame(minHeight: 44)

            Divider()
                .overlay(SettingsDesign.dividerColor(colorScheme))

            VStack(alignment: .leading, spacing: 14) {
                usageCardPreview
                colorRulesSection

                Text("同步用于菜单栏与用量卡片")
                    .font(SettingsTypography.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(14)
        }
        .background {
            shape.fill(CompactPopoverPalette.surface(.card, colorScheme))
        }
        .overlay {
            shape.strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
        }
    }

    private var usageCardPreview: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("用量卡片预览")
                    .font(SettingsTypography.secondary.weight(.medium))
                    .foregroundStyle(.secondary)

                Spacer(minLength: 8)

                Text("模拟数据")
                    .font(SettingsTypography.caption)
                    .foregroundStyle(.tertiary)
            }

            MenuBarUsageCardThresholdPreview(
                rules: environment.settings.menuBarColorRules
            )
        }
    }

    private var colorRulesSection: some View {
        MenuBarColorRulesEditor(
            rules: menuBarColorRulesBinding,
            layout: .compact
        )
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
        .padding(.horizontal, 12)
        .frame(minHeight: 52)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 11, style: .continuous)
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
        let shape = RoundedRectangle(
            cornerRadius: SettingsDesign.cardCornerRadius,
            style: .continuous
        )

        return VStack(spacing: 0) {
            SettingsSectionHeader(
                title: "凭证顺序",
                systemImage: "arrow.up.arrow.down",
                trailingText: "拖动排序 · 前 \(CredentialDisplayOrder.maximumMenuBarCount) 个显示"
            )
            .padding(.horizontal, 14)
            .frame(minHeight: 44)

            Divider()
                .overlay(SettingsDesign.dividerColor(colorScheme))

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
                .frame(maxWidth: .infinity, minHeight: 220)
            } else {
                ReorderableCredentialCardList(
                    ids: unifiedStates.map(\.configuration.id),
                    itemHeight: orderRowHeight,
                    itemSpacing: 2,
                    move: moveDisplay,
                    card: { (state: UUID) in
                        managementCard(state)
                    }
                )
                .padding(.horizontal, 6)
                .padding(.vertical, 4)

                Divider()
                    .overlay(SettingsDesign.dividerColor(colorScheme))

                orderSummaryFooter
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            shape.fill(CompactPopoverPalette.surface(.card, colorScheme))
        }
        .overlay {
            shape.strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
        }
    }

    private var orderSummaryFooter: some View {
        HStack(spacing: 4) {
            Text("已显示 \(menuBarStates.count) 个")
                .font(SettingsTypography.caption.weight(.semibold))
                .foregroundStyle(.secondary)

            Text("· 最多 \(CredentialDisplayOrder.maximumMenuBarCount) 个")
                .font(SettingsTypography.caption)
                .foregroundStyle(.tertiary)

            Spacer(minLength: 8)

            Text("拖动行调整顺序")
                .font(SettingsTypography.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 14)
        .frame(minHeight: 34)
        .accessibilityElement(children: .combine)
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
        let isLast = unifiedStates.last?.configuration.id == id

        return HStack(spacing: 6) {
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.secondary)
                .frame(width: 14)

            if let descriptor = descriptor(for: state) {
                MenuBarIndicatorPreview(
                    state: state,
                    descriptor: descriptor,
                    metric: menuBarMetric(for: state),
                    style: menuBarStyle(for: state),
                    colorRules: environment.settings.menuBarColorRules
                )
                .equatable()
                .frame(width: 44, height: 26)
            }

            providerLogo(for: state, size: 24)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 5) {
                    Text(state.configuration.displayName)
                        .font(SettingsTypography.rowTitle)
                        .lineLimit(1)

                    if isInMenuBar {
                        Text("菜单栏")
                            .font(SettingsTypography.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.accentColor.opacity(0.14), in: Capsule())
                    }
                }

                Text("\(providerName(for: state)) · \(planName(for: state.configuration))")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .lineLimit(1)
            .layoutPriority(1)

            Spacer(minLength: 4)

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
                .font(.system(size: 21, weight: .semibold))
                .foregroundStyle(isInMenuBar ? CompactPopoverPalette.positive(colorScheme) : Color.accentColor)
                .frame(width: 28, height: 28)
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
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(
                    hoveredManagementID == id
                        ? SettingsDesign.hoverFill(colorScheme)
                        : Color.clear
                )
        }
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(SettingsDesign.dividerColor(colorScheme))
                    .frame(height: 0.7)
                    .padding(.leading, 57)
            }
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

        return MenuBarSelectionControl(
            systemImage: "gauge.with.dots.needle.33percent",
            value: selectedMetricTitle(for: state, resolution: resolution),
            maxValueWidth: 96,
            help: "选择 \(state.configuration.displayName) 的菜单栏指标",
            accessibilityLabel: "菜单栏指标"
        ) {
            ForEach(metricOptions(for: state)) { option in
                Button {
                    setMenuBarMetric(option.metricID, for: state.configuration.id)
                } label: {
                    if option.metricID == resolution.metric?.id {
                        Label(option.label, systemImage: "checkmark")
                    } else {
                        Text(option.label)
                    }
                }
            }
        }
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
        if let metric = resolution.metric {
            return metric.label
        }
        guard let selectedMetricID = resolution.selectedMetricID else {
            return "选择指标"
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

        return MenuBarSelectionControl(
            systemImage: selectedStyle.systemImage(for: metric),
            value: selectedStyle.title(for: metric),
            maxValueWidth: 72,
            help: "选择菜单栏指标样式：\(selectedStyle.title(for: metric))",
            accessibilityLabel: "菜单栏指标样式"
        ) {
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
        }
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

private struct MenuBarSelectionControl<MenuContent: View>: View {
    let systemImage: String
    let value: String
    let maxValueWidth: CGFloat
    let help: String
    let accessibilityLabel: String
    private let menuContent: MenuContent

    @Environment(\.colorScheme) private var colorScheme
    @State private var isHovered = false

    init(
        systemImage: String,
        value: String,
        maxValueWidth: CGFloat,
        help: String,
        accessibilityLabel: String,
        @ViewBuilder menuContent: () -> MenuContent
    ) {
        self.systemImage = systemImage
        self.value = value
        self.maxValueWidth = maxValueWidth
        self.help = help
        self.accessibilityLabel = accessibilityLabel
        self.menuContent = menuContent()
    }

    var body: some View {
        Menu {
            menuContent
        } label: {
            HStack(spacing: 5) {
                Image(systemName: systemImage)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(isHovered ? Color.primary : Color.secondary)

                Text(value)
                    .font(.system(size: 10.5, weight: .medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: maxValueWidth, alignment: .leading)

                Image(systemName: "chevron.down")
                    .font(.system(size: 9.5, weight: .semibold))
                    .foregroundStyle(isHovered ? Color.accentColor : Color.secondary)
            }
            .padding(.horizontal, 8)
            .frame(minHeight: 28)
            .background {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(
                        isHovered
                            ? SettingsDesign.hoverFill(colorScheme)
                            : SettingsDesign.fieldFill(colorScheme)
                    )
            }
            .overlay {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(
                        isHovered
                            ? Color.accentColor.opacity(0.24)
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
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(value)
    }
}

private struct MenuBarUsageCardThresholdPreview: View {
    let rules: MenuBarColorRules

    @Environment(\.colorScheme) private var colorScheme

    private var samples: [Sample] {
        let warning = Double(rules.warningThreshold)
        let critical = Double(rules.criticalThreshold)
        let normalUpper = max(0, warning - 1)

        return [
            Sample(
                id: "normal",
                title: "正常区间",
                tone: .normal,
                percent: normalUpper == 0 ? 0 : max(1, normalUpper * 0.5)
            ),
            Sample(
                id: "warning",
                title: "提醒区间",
                tone: .warning,
                percent: (warning + critical) / 2
            ),
            Sample(
                id: "critical",
                title: "紧急区间",
                tone: .critical,
                percent: min(100, (critical + 100) / 2)
            )
        ]
    }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(samples.enumerated()), id: \.element.id) { index, sample in
                sampleRow(sample)

                if index < samples.count - 1 {
                    Rectangle()
                        .fill(SettingsDesign.dividerColor(colorScheme))
                        .frame(height: 0.7)
                        .padding(.leading, 40)
                }
            }
        }
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background {
            RoundedRectangle(
                cornerRadius: PopoverVisualPolicy.cornerRadius(for: .outer),
                style: .continuous
            )
            .fill(CompactPopoverPalette.surface(.card, colorScheme))
        }
        .overlay {
            RoundedRectangle(
                cornerRadius: PopoverVisualPolicy.cornerRadius(for: .outer),
                style: .continuous
            )
            .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
        }
        .accessibilityElement(children: .contain)
    }

    private func sampleRow(_ sample: Sample) -> some View {
        let color = UsageMetricPresentation.color(
            for: sample.tone,
            rules: rules
        )

        return HStack(spacing: 8) {
            CompactUsageGauge(
                percent: sample.percent,
                tone: sample.tone,
                color: color,
                size: 32
            )

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(sample.title)
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(.secondary)

                    Spacer(minLength: 4)

                    Text(
                        CompactUsageCardPresentation.compactPercentText(
                            sample.percent
                        )
                    )
                    .font(.system(size: 10.5, weight: .semibold, design: .monospaced))
                    .monospacedDigit()
                    .foregroundStyle(color)
                }
            }
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(sample.title)，\(CompactUsageCardPresentation.compactPercentText(sample.percent))"
        )
    }

    private struct Sample: Identifiable {
        let id: String
        let title: String
        let tone: UsageMetricTone
        let percent: Double
    }
}
