import AppKit
import SwiftUI
import UniformTypeIdentifiers

private struct MenuBarIndicatorCardDropDelegate: DropDelegate {
    @Binding var draggedID: UUID?
    let targetID: UUID
    let orderedIDs: [UUID]
    let move: (UUID, UUID) -> Void

    func dropEntered(info: DropInfo) {
        guard
            let draggedID,
            draggedID != targetID,
            orderedIDs.contains(draggedID),
            orderedIDs.contains(targetID)
        else {
            return
        }
        withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
            move(draggedID, targetID)
        }
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        DropProposal(operation: .move)
    }

    func performDrop(info: DropInfo) -> Bool {
        draggedID = nil
        return true
    }
}

private struct MenuBarIndicatorCardDragControl: View {
    let id: UUID
    let enabled: Bool
    @Binding var draggedID: UUID?

    var body: some View {
        Image(systemName: "line.3.horizontal")
            .font(.system(.body, weight: .medium))
            .foregroundStyle(Color.secondary.opacity(enabled ? 1 : 0.35))
            .frame(width: 28, height: 32)
            .contentShape(Rectangle())
            .help(enabled ? "拖动调整菜单栏与弹窗排序" : "点击排序后可拖动")
            .accessibilityLabel("拖动排序")
    }
}

enum KeyDisplayMask {
    static func masked(suffix: String) -> String {
        guard
            suffix.count == KeyCredentialPolicy.minimumVisibleSuffixLength,
            !KeyCredentialPolicy.isLegacyShortSecretSuffix(suffix)
        else {
            return "plan-••••"
        }
        return "plan-••••\(suffix)"
    }
}

/// 设置窗口存在时临时显示 Dock 图标，关闭后恢复菜单栏应用形态。
private struct SettingsDockIconAnchor: NSViewRepresentable {
    func makeNSView(context: Context) -> SettingsDockIconView {
        SettingsDockIconView()
    }

    func updateNSView(_ nsView: SettingsDockIconView, context: Context) {}
}

private final class SettingsDockIconView: NSView {
    private weak var observedWindow: NSWindow?
    private var closeObserver: NSObjectProtocol?

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        closeObserver.map(NotificationCenter.default.removeObserver)
        closeObserver = nil
        if let observedWindow {
            SettingsWindowActivationPolicy.unregister(observedWindow)
            self.observedWindow = nil
        }

        guard let window else {
            if let observedWindow {
                SettingsWindowActivationPolicy.unregister(observedWindow)
                self.observedWindow = nil
            }
            return
        }
        SettingsWindowActivationPolicy.register(window)
        observedWindow = window
        closeObserver = NotificationCenter.default.addObserver(
            forName: NSWindow.willCloseNotification,
            object: window,
            queue: .main
        ) { notification in
            guard let window = notification.object as? NSWindow else {
                return
            }
            MainActor.assumeIsolated {
                SettingsWindowActivationPolicy.unregister(window)
            }
        }
    }

}

@MainActor
struct SettingsView: View {
    typealias UpdateValidatedKey = @MainActor (UUID, String, String) async throws -> KeyEditorSaveResult
    typealias AddValidatedCredential = @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    typealias UpdateValidatedCredential = @MainActor (UUID, ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    typealias SetKeyEnabled = @MainActor (UUID, Bool) throws -> Void
    typealias CheckForUpdates = @MainActor () async -> Void
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    typealias SubmitIssueReport = @MainActor () async -> Void
    typealias ReadKey = @MainActor (UUID) -> String?
    typealias StartRoutinCheckIn = @MainActor () async -> Void
    typealias BeginRoutinLogin = @MainActor () async -> Void
    typealias SignOutRoutin = @MainActor () async -> Void
    typealias DeleteKey = @MainActor (UUID) throws -> Void
    typealias CodexGroupDetectionRecordForKey = @MainActor (UUID) -> CodexGroupDetectionRecord?
    typealias ClearCodexGroupDetection = @MainActor (UUID) -> Void

    private enum EditorPresentation: Identifiable {
        case add
        case edit(KeyConfiguration)

        var id: String {
            switch self {
            case .add:
                return "add"
            case let .edit(configuration):
                return configuration.id.uuidString
            }
        }
    }

    private enum SettingsSection: CaseIterable, Hashable, Identifiable {
        case accounts
        case display
        case system

        var id: Self { self }

        var title: String {
            switch self {
            case .accounts:
                return "供应商与凭证"
            case .display:
                return "显示与刷新"
            case .system:
                return "系统与更新"
            }
        }

        var symbol: String {
            switch self {
            case .accounts:
                return "key.horizontal"
            case .display:
                return "rectangle.3.group"
            case .system:
                return "gearshape"
            }
        }
    }

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings

    private let loginItemManager: any LoginItemManaging
    private let updateValidatedKey: UpdateValidatedKey
    private let addValidatedCredential: AddValidatedCredential
    private let updateValidatedCredential: UpdateValidatedCredential
    private let setKeyEnabled: SetKeyEnabled
    private let updateStatus: AppUpdateStatus
    private let checkForUpdates: CheckForUpdates
    private let installAvailableUpdate: InstallAvailableUpdate
    private let submitIssueReport: SubmitIssueReport
    private let readKey: ReadKey
    private let routinCheckInState: RoutinCheckInState
    private let startRoutinCheckIn: StartRoutinCheckIn
    private let beginRoutinLogin: BeginRoutinLogin
    private let signOutRoutin: SignOutRoutin
    private let deleteKey: DeleteKey
    private let codexGroupDetectionRecord: CodexGroupDetectionRecordForKey
    private let clearCodexGroupDetection: ClearCodexGroupDetection

    @Environment(\.openWindow) private var openWindow

    @State private var editor: EditorPresentation?
    @State private var pendingDeletion: KeyConfiguration?
    @State private var selectedSection: SettingsSection? = .accounts
    @State private var isReorderingMenuBarIndicators = false
    @State private var isReorderingAvailableIndicators = false
    @State private var draggingIndicatorID: UUID?
    @State private var expandedKeyID: UUID?
    @State private var orderedKeyIDs: [UUID]
    @State private var operationError: String?

    init(
        store: UsageStore,
        settings: AppSettings,
        loginItemManager: any LoginItemManaging,
        updateValidatedKey: @escaping UpdateValidatedKey,
        addValidatedCredential: @escaping AddValidatedCredential = { _ in .saved },
        updateValidatedCredential: @escaping UpdateValidatedCredential = { _, _ in .saved },
        setKeyEnabled: @escaping SetKeyEnabled = { _, _ in },
        updateStatus: AppUpdateStatus = .idle,
        checkForUpdates: @escaping CheckForUpdates = {},
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        submitIssueReport: @escaping SubmitIssueReport = {},
        readKey: @escaping ReadKey = { _ in nil },
        routinCheckInState: RoutinCheckInState = .idle,
        startRoutinCheckIn: @escaping StartRoutinCheckIn = {},
        beginRoutinLogin: @escaping BeginRoutinLogin = {},
        signOutRoutin: @escaping SignOutRoutin = {},
        deleteKey: @escaping DeleteKey = { _ in },
        codexGroupDetectionRecord: @escaping CodexGroupDetectionRecordForKey = { _ in nil },
        clearCodexGroupDetection: @escaping ClearCodexGroupDetection = { _ in }
    ) {
        self.store = store
        self.settings = settings
        self.loginItemManager = loginItemManager
        self.updateValidatedKey = updateValidatedKey
        self.addValidatedCredential = addValidatedCredential
        self.updateValidatedCredential = updateValidatedCredential
        self.setKeyEnabled = setKeyEnabled
        self.updateStatus = updateStatus
        self.checkForUpdates = checkForUpdates
        self.installAvailableUpdate = installAvailableUpdate
        self.submitIssueReport = submitIssueReport
        self.readKey = readKey
        self.routinCheckInState = routinCheckInState
        self.startRoutinCheckIn = startRoutinCheckIn
        self.beginRoutinLogin = beginRoutinLogin
        self.signOutRoutin = signOutRoutin
        self.deleteKey = deleteKey
        self.codexGroupDetectionRecord = codexGroupDetectionRecord
        self.clearCodexGroupDetection = clearCodexGroupDetection
        _orderedKeyIDs = State(initialValue: store.orderedKeyIDs)
    }

    var body: some View {
        NavigationSplitView {
            List(SettingsSection.allCases, selection: $selectedSection) { section in
                Label(section.title, systemImage: section.symbol)
                    .tag(section)
            }
            .listStyle(.sidebar)
            .navigationTitle("MyToken")
            .frame(minWidth: 176, idealWidth: 196)
        } detail: {
            detailContent
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background(.regularMaterial)
        }
        .navigationSplitViewStyle(.balanced)
        .frame(minWidth: 760, idealWidth: 860, minHeight: 520, idealHeight: 620)
        .liquidGlassWindowBackground()
        .background(WindowFramePersistence())
        .background(SettingsDockIconAnchor())
        .onChange(of: store.orderedKeyIDs) { _, ids in
            orderedKeyIDs = ids
        }
        .onAppear {
            LoginItemSettingSynchronizer.synchronize(
                settings: settings,
                manager: loginItemManager
            )
        }
        .onReceive(NotificationCenter.default.publisher(for: NSApplication.didBecomeActiveNotification)) { _ in
            LoginItemSettingSynchronizer.synchronize(
                settings: settings,
                manager: loginItemManager
            )
        }
        .sheet(item: $editor) { presentation in
            editorView(for: presentation)
        }
        .confirmationDialog(
            "确定删除这个 Key？",
            isPresented: Binding(
                get: { pendingDeletion != nil },
                set: { if !$0 { pendingDeletion = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) { deletePendingKey() }
            Button("取消", role: .cancel) { pendingDeletion = nil }
        } message: {
            Text("将同时删除本地保存的 Key 和用量缓存，此操作无法撤销。")
        }
        .alert(
            "无法完成操作",
            isPresented: Binding(
                get: { operationError != nil },
                set: { if !$0 { operationError = nil } }
            )
        ) {
            Button("好") { operationError = nil }
        } message: {
            Text(operationError ?? "发生未知错误")
        }
    }
}

private extension SettingsView {
    @ViewBuilder
    var detailContent: some View {
        switch selectedSection ?? .accounts {
        case .accounts:
            keyManagement
        case .display:
            displayAndRefresh
        case .system:
            notificationsAndSystem
        }
    }

    var keyManagement: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("供应商与凭证")
                        .font(.title2.weight(.semibold))
                    Text("按供应商管理独立凭证；菜单栏指标请在“显示与刷新”中管理")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    editor = .add
                } label: {
                    Label("添加", systemImage: "plus")
                }
                .liquidGlassButton()
                .accessibilityLabel("添加 Key")
            }

            if orderedKeyIDs.isEmpty {
                ContentUnavailableView(
                    "尚未配置 Key",
                    systemImage: "key.slash",
                    description: Text("添加供应商凭证后即可查看用量")
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityLabel("尚未配置供应商凭证，请点击添加")
            } else {
                List {
                    ForEach(settingsProviderGroups, id: \.id) { group in
                        Section {
                            ForEach(group.ids, id: \.self) { id in
                                if let configuration = store.state(for: id)?.configuration {
                                    keyRow(configuration)
                                }
                            }
                            // 分组后的排序通过每行的拖拽投放完成，保留 .onMove(perform: move) 的整体排序语义。
                        } header: {
                            HStack(spacing: 6) {
                                Image(systemName: group.iconName)
                                Text(group.displayName)
                                Text("\(group.ids.count)")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .accessibilityLabel("Key 列表")
                .scrollContentBackground(.hidden)
            }
        }
        .padding(24)
    }

    func keyRow(_ configuration: KeyConfiguration) -> some View {
        let providerName = ProviderRegistry.builtInDescriptors.first(where: { $0.id == configuration.providerID })?.displayName ?? configuration.providerID.rawValue

        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(configuration.displayName)
                        .font(.headline)
                    if let websiteURL = configuration.websiteURL {
                        Link(destination: websiteURL) {
                            HStack(spacing: 3) {
                                Text(providerName)
                                Image(systemName: "arrow.up.right.square")
                                    .imageScale(.small)
                            }
                        }
                        .help("打开 \(providerName) 官网")
                        .accessibilityLabel("打开 \(providerName) 官网")
                    } else {
                        Text(providerName)
                    }
                }
                .font(.system(.caption, design: .monospaced))
                .foregroundStyle(.secondary)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("\(configuration.displayName)，\(configuration.providerID == .routin ? KeyDisplayMask.masked(suffix: configuration.keySuffix) : providerName)")

                Spacer()

                Toggle(isOn: Binding(
                    get: { configuration.isEnabled },
                    set: { setEnabled(configuration, enabled: $0) }
                )) {
                    Label(
                        configuration.isEnabled ? "启用" : "已停用",
                        systemImage: "power"
                    )
                }
                .toggleStyle(.button)
                .controlSize(.small)
                .tint(configuration.isEnabled ? .green : .secondary)
                .help(configuration.isEnabled ? "点击停用此凭证" : "点击启用此凭证")
                .accessibilityLabel(configuration.isEnabled ? "停用 \(configuration.displayName)" : "启用 \(configuration.displayName)")

                Toggle(isOn: Binding(
                    get: { settings.selectedCredentialIDs.contains(configuration.id) },
                    set: { setMenuBarSelection(configuration.id, selected: $0) }
                )) {
                    Label(
                        settings.selectedCredentialIDs.contains(configuration.id) ? "菜单栏" : "未显示",
                        systemImage: "menubar.rectangle"
                    )
                }
                .toggleStyle(.button)
                .controlSize(.small)
                .tint(settings.selectedCredentialIDs.contains(configuration.id) ? .accentColor : .secondary)
                .help(settings.selectedCredentialIDs.contains(configuration.id) ? "点击从菜单栏移除" : "点击添加到菜单栏（最多 5 个）")
                .accessibilityLabel(settings.selectedCredentialIDs.contains(configuration.id) ? "从菜单栏移除" : "添加到菜单栏")

                Button {
                    editor = .edit(configuration)
                } label: {
                    Image(systemName: "pencil")
                }
                .buttonStyle(.borderless)
                .help("编辑 \(configuration.displayName)")
                .accessibilityLabel("编辑 \(configuration.displayName)")

                Button(role: .destructive) {
                    pendingDeletion = configuration
                } label: {
                    Image(systemName: "trash")
                }
                .buttonStyle(.borderless)
                .help("删除 \(configuration.displayName)")
                .accessibilityLabel("删除 \(configuration.displayName)")
            }

            if let state = store.state(for: configuration.id) {
                keyUsageOverview(state)

                if expandedKeyID == configuration.id {
                    keyUsageDetails(state)
                        .padding(.top, 4)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(ProviderTheme.background(for: configuration.providerID))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(ProviderTheme.borderColor(for: configuration.providerID))
        }
        .opacity(configuration.isEnabled ? 1 : 0.55)
        .contentShape(Rectangle())
        .onTapGesture {
            expandedKeyID = expandedKeyID == configuration.id ? nil : configuration.id
        }
        .listRowSeparator(.visible)
        .listRowSeparatorTint(Color.secondary.opacity(0.28))
        .listRowBackground(Color.clear)
    }

    @ViewBuilder
    func keyUsageOverview(_ state: KeyUsageState) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 10) {
                if !snapshot.metrics.isEmpty {
                    normalizedUsageOverview(
                        snapshot: snapshot,
                        providerID: state.configuration.providerID
                    )
                } else if snapshot.kind == .periodic {
                    HStack(alignment: .top, spacing: 16) {
                        if let metric = snapshot.fiveHour {
                            usageDetailItem("5 小时", metric)
                        }
                        if snapshot.fiveHour != nil, snapshot.weekly != nil {
                            Divider().frame(height: 74)
                        }
                        if let metric = snapshot.weekly {
                            usageDetailItem("周", metric)
                        }
                    }
                } else if let token = snapshot.token {
                    usageDetailItem("Token", token)
                }

            }
        } else {
            Label(UsageFormatter.statusText(state: state), systemImage: "clock.badge.exclamationmark")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    func keyUsageDetails(_ state: KeyUsageState) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 12) {
                Divider()
                    .padding(.top, 2)

                detailSection("套餐状态", symbol: "checklist") {
                    Grid(horizontalSpacing: 20, verticalSpacing: 8) {
                        GridRow {
                            detailValue("套餐", snapshot.planName.isEmpty ? "未命名套餐" : snapshot.planName)
                            detailValue("类型", snapshot.kind == .periodic ? "周期订阅" : "Token 资源包")
                            detailValue("状态", subscriptionStatus(snapshot.status, state: state))
                        }
                    }
                }

                detailSection("订阅与周期", symbol: "calendar") {
                    Grid(horizontalSpacing: 20, verticalSpacing: 8) {
                        GridRow {
                            detailValue("订阅开始", UsageFormatter.fullDateTime(snapshot.subscriptionStartAt))
                            detailValue("订阅结束", UsageFormatter.fullDateTime(snapshot.subscriptionEndAt))
                        }

                        if snapshot.kind == .periodic {
                            GridRow {
                                if let metric = snapshot.fiveHour {
                                    detailValue("5 小时结束", UsageFormatter.fullDateTime(metric.windowEnd))
                                } else {
                                    Color.clear
                                }
                                if let metric = snapshot.weekly {
                                    detailValue("周结束", UsageFormatter.fullDateTime(metric.windowEnd))
                                } else {
                                    Color.clear
                                }
                            }
                        }
                    }
                }

                if !snapshot.metrics.isEmpty {
                    detailSection("用量明细", symbol: "chart.bar.xaxis") {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(snapshot.normalizedMetrics) { metric in
                                normalizedDetailValue(metric)
                            }
                        }
                    }
                }

                detailSection("账户与模型", symbol: "person.crop.circle") {
                    VStack(alignment: .leading, spacing: 8) {
                        detailValue(
                            "分组倍率",
                            snapshot.groupMultipliers.isEmpty
                                ? "—"
                                : UsageFormatter.groupMultiplierText(snapshot.groupMultipliers)
                        )

                        if let record = codexGroupDetectionRecord(state.configuration.id) {
                            HStack(alignment: .lastTextBaseline, spacing: 10) {
                                detailValue("已关联账号", record.accountDisplayName)
                                Button("解除关联", role: .destructive) {
                                    clearCodexGroupDetection(state.configuration.id)
                                }
                                .controlSize(.small)
                                .liquidGlassButton()
                                .help("解除 \(state.configuration.displayName) 的 Routin 账号关联")
                                .accessibilityLabel("解除 \(state.configuration.displayName) 的 Routin 账号关联")
                            }

                            detailValue(
                                "Codex 当前分组",
                                "\(record.groupName)，检测于 \(record.detectedAt.formatted(date: .omitted, time: .shortened))"
                            )
                        }

                        if !snapshot.allowedModels.isEmpty {
                            detailValue("允许模型", snapshot.allowedModels.joined(separator: "、"))
                        }
                    }
                }
            }
            .textSelection(.enabled)
        } else {
            EmptyView()
        }
    }

    func detailSection<Content: View>(
        _ title: String,
        symbol: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Label(title, systemImage: symbol)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
            content()
        }
    }

    func detailValue(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.tertiary)
            Text(value)
                .font(.caption)
                .foregroundStyle(.primary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func normalizedUsageOverview(
        snapshot: UsageSnapshot,
        providerID: ProviderID
    ) -> some View {
        let layout = UsageMetricGridPolicy.layout(
            providerID: providerID,
            metrics: snapshot.normalizedMetrics
        )
        return NormalizedUsageMetricGrid(
            metrics: layout.metrics,
            columns: layout.columns
        )
    }

    func usageDetailItem(_ title: String, _ metric: UsageMetric) -> some View {
        let percentText = UsageFormatter.percentText(metric) ?? "—"

        return VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(title)
                    .foregroundStyle(.tertiary)
                Spacer(minLength: 4)
                Text(percentText)
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(UsageMetricPresentation.color(for: metric.percent))
                    .monospacedDigit()
            }

            UsageMetricProgressBar(metric: metric)

            HStack(spacing: 8) {
                Text(UsageFormatter.amount(metric))
                Spacer(minLength: 8)
                if metric.windowEnd != nil {
                    Text("重置 \(UsageFormatter.resetTime(metric))")
                }
            }
            .font(.caption2)
            .foregroundStyle(.secondary)
            .monospacedDigit()

            Text("剩余 \(UsageFormatter.remaining(metric))")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(title)，已使用 \(percentText)，\(UsageFormatter.amount(metric))，剩余 \(UsageFormatter.remaining(metric))"
        )
    }

    func normalizedDetailValue(_ metric: NormalizedUsageMetric) -> some View {
        let value: String
        switch metric.presentation {
        case .balance:
            value = "\(decimalText(metric.value)) \(metric.currencyCode ?? "元")"
        case .status:
            value = metric.healthState == .unavailable ? "不可用" : "可用"
        case .progress:
            if let percent = metric.displayedPercent {
                value = metric.displaysRemainingPercent
                    ? "剩余 \(Int(percent.rounded()))%"
                    : "已使用 \(Int(percent.rounded()))%，剩余 \(decimalText(metric.remaining))"
            } else {
                value = "—"
            }
        case .value:
            value = decimalText(metric.value)
        }
        return detailValue(metric.label, value)
    }

    func decimalText(_ value: Decimal?) -> String {
        guard let value else { return "—" }
        return NSDecimalNumber(decimal: value).stringValue
    }

    func normalizedMetricColor(_ state: UsageMetricHealthState) -> Color {
        switch state {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        }
    }

    func subscriptionStatus(_ status: Int?, state: KeyUsageState) -> String {
        if state.error == nil, state.snapshot != nil {
            if state.isStale {
                return "已过期"
            }
            switch status {
            case 1:
                return "正常"
            case let status?:
                return "状态 \(status)"
            case nil:
                return "正常"
            }
        }
        return UsageFormatter.statusText(state: state)
    }

    func settingsPageHeader(_ title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.title2.weight(.semibold))
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    var displayAndRefresh: some View {
        VStack(alignment: .leading, spacing: 16) {
            settingsPageHeader("显示与刷新", subtitle: "选择菜单栏指标并控制后台刷新频率")

            Form {
                Section {
                    if selectedMenuBarConfigurations.isEmpty {
                        Text("尚未选择菜单栏指标")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(selectedMenuBarConfigurations) { configuration in
                            menuBarIndicatorRow(configuration)
                                .background {
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .fill(Color.primary.opacity(0.04))
                                }
                                .overlay {
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .stroke(Color.secondary.opacity(0.14), lineWidth: 1)
                                }
                                .padding(.vertical, 3)
                                .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                .onDrag {
                                    indicatorDragProvider(
                                        configuration: configuration,
                                        isActive: isReorderingMenuBarIndicators && selectedMenuBarConfigurations.count > 1
                                    )
                                }
                                .onDrop(
                                    of: [UTType.text],
                                    delegate: MenuBarIndicatorCardDropDelegate(
                                        draggedID: $draggingIndicatorID,
                                        targetID: configuration.id,
                                        orderedIDs: selectedMenuBarConfigurations.map(\.id),
                                        move: { draggedID, targetID in
                                            moveMenuBarIndicator(draggedID: draggedID, before: targetID)
                                        }
                                    )
                                )
                        }
                    }
                } header: {
                    HStack {
                        Text("菜单栏指标（\(selectedMenuBarConfigurations.count)/5）")
                        Spacer()
                        Button {
                            toggleMenuBarReordering()
                        } label: {
                            Label(
                                isReorderingMenuBarIndicators ? "完成" : "排序",
                                systemImage: isReorderingMenuBarIndicators ? "checkmark" : "arrow.up.arrow.down"
                            )
                        }
                        .buttonStyle(.borderless)
                        .disabled(selectedMenuBarConfigurations.count < 2)
                        .accessibilityLabel(isReorderingMenuBarIndicators ? "完成菜单栏指标排序" : "调整菜单栏指标排序")
                    }
                }

                Section {
                    if availableMenuBarConfigurations.isEmpty {
                        Text(orderedKeyIDs.isEmpty ? "尚未添加凭证" : "没有可添加的已启用凭证")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(availableMenuBarConfigurations) { configuration in
                            availableMenuBarIndicatorRow(configuration)
                                .background {
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .fill(Color.primary.opacity(0.04))
                                }
                                .overlay {
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .stroke(Color.secondary.opacity(0.14), lineWidth: 1)
                                }
                                .padding(.vertical, 3)
                                .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                .onDrag {
                                    indicatorDragProvider(
                                        configuration: configuration,
                                        isActive: isReorderingAvailableIndicators && availableMenuBarConfigurations.count > 1
                                    )
                                }
                                .onDrop(
                                    of: [UTType.text],
                                    delegate: MenuBarIndicatorCardDropDelegate(
                                        draggedID: $draggingIndicatorID,
                                        targetID: configuration.id,
                                        orderedIDs: orderedAvailableMenuBarIDs,
                                        move: { draggedID, targetID in
                                            moveAvailableIndicator(draggedID: draggedID, before: targetID)
                                        }
                                    )
                                )
                        }
                    }
                }
                header: {
                    HStack {
                        Text("可添加的指标")
                        Spacer()
                        Button {
                            toggleAvailableIndicatorReordering()
                        } label: {
                            Label(
                                isReorderingAvailableIndicators ? "完成" : "排序",
                                systemImage: isReorderingAvailableIndicators ? "checkmark" : "arrow.up.arrow.down"
                            )
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(isReorderingAvailableIndicators ? "完成可添加指标排序" : "调整可添加指标排序")
                    }
                }

                Section("自动刷新") {
                    Picker("刷新间隔", selection: $settings.refreshMinutes) {
                        ForEach(AppSettings.allowedRefreshMinutes, id: \.self) { minutes in
                            Text("每 \(minutes) 分钟").tag(minutes)
                        }
                    }
                    .accessibilityLabel("自动刷新间隔")
                    Text("只刷新已启用的凭证，每个凭证的用量保持独立。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .formStyle(.grouped)
        }
        .padding(24)
    }

    var notificationsAndSystem: some View {
        VStack(alignment: .leading, spacing: 16) {
            settingsPageHeader("系统与更新", subtitle: "管理登录启动、签到与软件更新")

            Form {
                Section("系统") {
                    Toggle("登录时启动", isOn: launchAtLoginBinding)
                        .accessibilityLabel("登录时启动")
                }

                Section("Routin 签到") {
                    LabeledContent("登录状态") {
                        Text(routinCheckInState.statusText)
                            .accessibilityLabel("Routin 登录状态，\(routinCheckInState.statusText)")
                    }
                    routinCheckInControls
                }

                Section("软件更新") {
                    LabeledContent("当前版本") {
                        Text(currentVersion)
                            .accessibilityLabel("当前版本 \(currentVersion)")
                    }
                    updateControls
                }
            }
            .formStyle(.grouped)
        }
        .padding(24)
    }

    var launchAtLoginBinding: Binding<Bool> {
        Binding(
            get: { settings.launchAtLogin },
            set: { enabled in
                do {
                    try LoginItemSettingSynchronizer.setEnabled(
                        enabled,
                        settings: settings,
                        manager: loginItemManager
                    )
                } catch {
                    operationError = "无法更新登录启动设置：\(error.localizedDescription)"
                }
            }
        )
    }

    var routinCheckInControls: some View {
        HStack(spacing: 8) {
            Button("立即签到") {
                openWindow(id: "routin-check-in")
                Task { await startRoutinCheckIn() }
            }
            .liquidGlassButton(prominent: true)
            .disabled(routinCheckInState.isBusy)

            if routinCheckInState.requiresLogin {
                Button("立即登录") {
                    openWindow(id: "routin-check-in")
                    Task { await beginRoutinLogin() }
                }
                .liquidGlassButton()
                .disabled(routinCheckInState.isBusy)
            } else {
                Button("退出登录", role: .destructive) {
                    Task { await signOutRoutin() }
                }
                .liquidGlassButton()
                .disabled(routinCheckInState.isBusy)
            }

            Spacer(minLength: 0)
        }
    }

    @ViewBuilder
    var updateControls: some View {
        switch updateStatus {
        case .idle:
            HStack(spacing: 8) {
                Button("检查更新") { Task { await checkForUpdates() } }
                    .liquidGlassButton()
                issueReportButton
                Spacer(minLength: 0)
            }
        case .checking:
            HStack(spacing: 8) {
                LabeledContent("正在检查更新") {
                    ProgressView()
                        .controlSize(.small)
                }
                issueReportButton
            }
        case let .available(update):
            availableUpdateControls(update)
        case let .downloading(progress):
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("正在下载更新")
                    Spacer()
                    if let progress {
                        Text("\(Int(progress * 100))%")
                            .monospacedDigit()
                    }
                }
                if let progress {
                    ProgressView(value: progress, total: 1)
                } else {
                    ProgressView()
                }
                HStack {
                    issueReportButton
                    Spacer(minLength: 0)
                }
            }
        case let .completed(version):
            HStack(spacing: 8) {
                Label("更新完成，当前版本 \(version)", systemImage: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                issueReportButton
                Spacer(minLength: 0)
            }
        case let .failed(message):
            VStack(alignment: .leading, spacing: 6) {
                Text(message).foregroundStyle(.red)
                HStack(spacing: 8) {
                    Button("重试") { Task { await checkForUpdates() } }
                        .liquidGlassButton()
                    issueReportButton
                    Spacer(minLength: 0)
                }
            }
        }
    }

    func availableUpdateControls(_ update: AppUpdate) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("发现新版本 \(update.version)")
                .fontWeight(.medium)
            UpdateNotesView(notes: update.notes)
            HStack(spacing: 8) {
                Button("安装更新") { Task { await installAvailableUpdate() } }
                    .liquidGlassButton(prominent: true)
                Link("查看发布说明", destination: update.releaseURL)
                issueReportButton
                Spacer(minLength: 0)
            }
        }
    }

    var issueReportButton: some View {
        Button("提交问题") { Task { await submitIssueReport() } }
            .liquidGlassButton()
    }

    var currentVersion: String {
        RoutinUsageApp.currentVersion
    }

    @ViewBuilder
    private func editorView(for presentation: EditorPresentation) -> some View {
        switch presentation {
        case .add:
            CredentialEditorView(save: addValidatedCredential)
        case let .edit(configuration):
            CredentialEditorView(
                title: "编辑 Key",
                initialProviderID: configuration.providerID,
                initialName: configuration.displayName,
                initialSecret: readKey(configuration.id) ?? "",
                initialMetadata: configuration.metadata
            ) { input in
                try await updateValidatedCredential(configuration.id, input)
            }
        }
    }

    func addKey(name: String, secret: String) async throws -> KeyEditorSaveResult {
        let existingIDs = Set(store.orderedKeyIDs)
        try await store.addValidatedKey(name: name, secret: secret)
        guard let addedID = store.orderedKeyIDs.first(where: { !existingIDs.contains($0) }) else {
            return .saved
        }
        return store.state(for: addedID)?.error == .noSubscription
            ? .savedWithoutSubscription
            : .saved
    }

    struct SettingsProviderGroup: Identifiable {
        let id: ProviderID
        let displayName: String
        let iconName: String
        let ids: [UUID]
    }

    var settingsProviderGroups: [SettingsProviderGroup] {
        ProviderID.allCases.compactMap { providerID in
            let ids = orderedKeyIDs.filter { id in
                store.state(for: id)?.configuration.providerID == providerID
            }
            guard !ids.isEmpty,
                  let descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == providerID })
            else { return nil }
            return SettingsProviderGroup(
                id: providerID,
                displayName: descriptor.displayName,
                iconName: descriptor.iconName,
                ids: ids
            )
        }
    }

    var selectedMenuBarConfigurations: [KeyConfiguration] {
        settings.selectedCredentialIDs.compactMap { id in
            guard let configuration = store.state(for: id)?.configuration, configuration.isEnabled else {
                return nil
            }
            return configuration
        }
    }

    var availableMenuBarConfigurations: [KeyConfiguration] {
        let orderedIDs = settings.availableCredentialIDs
            + orderedKeyIDs.filter { !settings.availableCredentialIDs.contains($0) }
        return orderedIDs.compactMap { id in
            guard let configuration = store.state(for: id)?.configuration,
                  configuration.isEnabled,
                  !settings.selectedCredentialIDs.contains(id)
            else {
                return nil
            }
            return configuration
        }
    }

    var orderedAvailableMenuBarIDs: [UUID] {
        availableMenuBarConfigurations.map(\.id)
    }

    func menuBarIndicatorRow(_ configuration: KeyConfiguration) -> some View {
        HStack(spacing: 10) {
            if isReorderingMenuBarIndicators {
                MenuBarIndicatorCardDragControl(
                    id: configuration.id,
                    enabled: selectedMenuBarConfigurations.count > 1,
                    draggedID: $draggingIndicatorID
                )
            }

            providerIdentity(for: configuration)
            Spacer(minLength: 12)
            if !isReorderingMenuBarIndicators {
                Button {
                    setMenuBarSelection(configuration.id, selected: false)
                } label: {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.borderless)
                .help("从菜单栏移除")
                .accessibilityLabel("从菜单栏移除 \(configuration.displayName)")
            }
        }
            .padding(.vertical, 8)
            .padding(.horizontal, 10)
            .opacity(draggingIndicatorID == configuration.id ? 0.78 : 1)
            .scaleEffect(draggingIndicatorID == configuration.id ? 1.02 : 1)
            .shadow(color: .black.opacity(draggingIndicatorID == configuration.id ? 0.18 : 0),
                radius: draggingIndicatorID == configuration.id ? 10 : 0,
                y: draggingIndicatorID == configuration.id ? 4 : 0
            )
            .animation(.spring(response: 0.32, dampingFraction: 0.82), value: draggingIndicatorID)
    }

    func availableMenuBarIndicatorRow(_ configuration: KeyConfiguration) -> some View {
        HStack(spacing: 10) {
            if isReorderingAvailableIndicators {
                MenuBarIndicatorCardDragControl(
                    id: configuration.id,
                    enabled: availableMenuBarConfigurations.count > 1,
                    draggedID: $draggingIndicatorID
                )
            }

            providerIdentity(for: configuration)
            Spacer(minLength: 12)
            if !isReorderingAvailableIndicators {
                Button {
                    setMenuBarSelection(configuration.id, selected: true)
                } label: {
                    Image(systemName: "plus.circle")
                }
                .buttonStyle(.borderless)
                .disabled(settings.selectedCredentialIDs.count >= 5)
                .help("添加到菜单栏")
                .accessibilityLabel("将 \(configuration.displayName) 添加到菜单栏")
            }
        }
            .padding(.vertical, 8)
            .padding(.horizontal, 10)
            .opacity(draggingIndicatorID == configuration.id ? 0.78 : 1)
            .scaleEffect(draggingIndicatorID == configuration.id ? 1.02 : 1)
            .shadow(color: .black.opacity(draggingIndicatorID == configuration.id ? 0.18 : 0),
                radius: draggingIndicatorID == configuration.id ? 10 : 0,
                y: draggingIndicatorID == configuration.id ? 4 : 0
            )
            .animation(.spring(response: 0.32, dampingFraction: 0.82), value: draggingIndicatorID)
    }

    func providerIdentity(for configuration: KeyConfiguration) -> some View {
        let descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == configuration.providerID })
        return VStack(alignment: .leading, spacing: 2) {
            Text(configuration.displayName)
                .font(.body.weight(.medium))
            HStack(spacing: 5) {
                Image(systemName: descriptor?.iconName ?? "key")
                Text(descriptor?.displayName ?? configuration.providerID.rawValue)
                if configuration.providerID == .routin {
                    Text(KeyDisplayMask.masked(suffix: configuration.keySuffix))
                        .font(.system(.caption2, design: .monospaced))
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("别名 \(configuration.displayName)，供应商 \(descriptor?.displayName ?? configuration.providerID.rawValue)")
    }

    func toggleMenuBarReordering() {
        isReorderingAvailableIndicators = false
        isReorderingMenuBarIndicators.toggle()
    }

    func toggleAvailableIndicatorReordering() {
        isReorderingMenuBarIndicators = false
        isReorderingAvailableIndicators.toggle()
    }

    private func indicatorDragProvider(
        configuration: KeyConfiguration,
        isActive: Bool
    ) -> NSItemProvider {
        guard isActive else { return NSItemProvider() }
        draggingIndicatorID = configuration.id
        let provider = NSItemProvider(object: configuration.id.uuidString as NSString)
        provider.suggestedName = configuration.displayName
        return provider
    }

    func moveMenuBarIndicator(draggedID: UUID, before targetID: UUID) {
        guard
            draggedID != targetID,
            let sourceIndex = settings.selectedCredentialIDs.firstIndex(of: draggedID),
            let targetIndex = settings.selectedCredentialIDs.firstIndex(of: targetID)
        else {
            return
        }
        let destination = targetIndex > sourceIndex ? targetIndex + 1 : targetIndex
        withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
            settings.moveSelectedCredential(
                fromOffsets: IndexSet(integer: sourceIndex),
                toOffset: destination
            )
        }
    }

    func moveAvailableIndicator(draggedID: UUID, before targetID: UUID) {
        let ids = orderedAvailableMenuBarIDs
        guard
            draggedID != targetID,
            let sourceIndex = ids.firstIndex(of: draggedID),
            let targetIndex = ids.firstIndex(of: targetID)
        else {
            return
        }
        let destination = targetIndex > sourceIndex ? targetIndex + 1 : targetIndex
        withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
            var reordered = ids
            let moving = reordered.remove(at: sourceIndex)
            reordered.insert(moving, at: destination > sourceIndex ? destination - 1 : destination)
            settings.availableCredentialIDs = reordered
        }
    }

    func setEnabled(_ configuration: KeyConfiguration, enabled: Bool) {
        do {
            try setKeyEnabled(configuration.id, enabled)
            if !enabled {
                setMenuBarSelection(configuration.id, selected: false)
            }
        } catch {
            operationError = "无法更新 Key 显示状态，请稍后重试"
        }
    }

    func setMenuBarSelection(_ id: UUID, selected: Bool) {
        var ids = settings.selectedCredentialIDs
        if selected {
            guard !ids.contains(id), ids.count < 5 else { return }
            ids.append(id)
        } else {
            ids.removeAll { $0 == id }
        }
        settings.selectedCredentialIDs = ids
    }

    func deletePendingKey() {
        guard let configuration = pendingDeletion else {
            return
        }
        pendingDeletion = nil
        do {
            try deleteKey(configuration.id)
            orderedKeyIDs.removeAll { $0 == configuration.id }
            settings.availableCredentialIDs.removeAll { $0 == configuration.id }
        } catch {
            operationError = "无法删除 Key，请稍后重试"
        }
    }

}
