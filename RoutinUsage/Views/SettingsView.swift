import AppKit
import SwiftUI
import UniformTypeIdentifiers

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

private struct KeyRowDropDelegate: DropDelegate {
    let targetID: UUID
    let move: @MainActor (UUID) -> Void

    func performDrop(info: DropInfo) -> Bool {
        guard let provider = info.itemProviders(for: [UTType.text]).first else {
            return false
        }
        provider.loadObject(ofClass: NSString.self) { object, _ in
            guard
                let value = object as? String,
                let draggedID = UUID(uuidString: value)
            else {
                return
            }
            Task { @MainActor in
                move(draggedID)
            }
        }
        return true
    }
}

@MainActor
struct SettingsView: View {
    typealias UpdateValidatedKey = @MainActor (UUID, String, String) async throws -> KeyEditorSaveResult
    typealias AddValidatedCredential = @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    typealias UpdateValidatedCredential = @MainActor (UUID, ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    typealias MoveKey = @MainActor (IndexSet, Int) -> Void
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
                return "账户"
            case .display:
                return "显示与刷新"
            case .system:
                return "通知与系统"
            }
        }

        var symbol: String {
            switch self {
            case .accounts:
                return "key.horizontal"
            case .display:
                return "rectangle.3.group"
            case .system:
                return "bell.badge"
            }
        }
    }

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings

    private let loginItemManager: any LoginItemManaging
    private let updateValidatedKey: UpdateValidatedKey
    private let addValidatedCredential: AddValidatedCredential
    private let updateValidatedCredential: UpdateValidatedCredential
    private let moveKey: MoveKey
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
    @State private var isReordering = false
    @State private var expandedKeyID: UUID?
    @State private var orderedKeyIDs: [UUID]
    @State private var lowThreshold: Int
    @State private var highThreshold: Int
    @State private var thresholdError: String?
    @State private var operationError: String?

    init(
        store: UsageStore,
        settings: AppSettings,
        loginItemManager: any LoginItemManaging,
        updateValidatedKey: @escaping UpdateValidatedKey,
        addValidatedCredential: @escaping AddValidatedCredential = { _ in .saved },
        updateValidatedCredential: @escaping UpdateValidatedCredential = { _, _ in .saved },
        moveKey: @escaping MoveKey,
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
        self.moveKey = moveKey
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
        _lowThreshold = State(initialValue: settings.thresholds.low)
        _highThreshold = State(initialValue: settings.thresholds.high)
    }

    var body: some View {
        NavigationSplitView {
            List(SettingsSection.allCases, selection: $selectedSection) { section in
                Label(section.title, systemImage: section.symbol)
                    .tag(section)
            }
            .listStyle(.sidebar)
            .navigationTitle("MyRoutin")
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
        .onChange(of: store.orderedKeyIDs) { _, ids in
            orderedKeyIDs = ids
        }
        .onAppear {
            // 应用以 LSUIElement 菜单栏模式启动。打开独立设置窗口时切换为普通应用，
            // 让程序坞显示图标并允许用户在多个窗口间切换；窗口关闭后恢复菜单栏模式。
            NSApp.setActivationPolicy(.regular)
            NSApp.activate(ignoringOtherApps: true)
            LoginItemSettingSynchronizer.synchronize(
                settings: settings,
                manager: loginItemManager
            )
        }
        .onDisappear { NSApp.setActivationPolicy(.accessory) }
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
                    Text("账户")
                        .font(.title2.weight(.semibold))
                    Text("拖动列表可调整顺序；菜单栏最多显示 4 个凭证")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    isReordering.toggle()
                } label: {
                    Label(
                        isReordering ? "完成" : "排序",
                        systemImage: isReordering ? "checkmark" : "arrow.up.arrow.down"
                    )
                }
                .liquidGlassButton()
                .accessibilityLabel(isReordering ? "完成排序" : "调整 Key 排序")
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
                    description: Text("添加 plan Key 后即可查看订阅用量")
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityLabel("尚未配置 Key，请添加 plan Key")
            } else {
                List {
                    ForEach(orderedKeyIDs, id: \.self) { id in
                        if let configuration = store.state(for: id)?.configuration {
                            keyRow(configuration)
                        }
                    }
                    .onMove(perform: move)
                }
                .listStyle(.plain)
                .accessibilityLabel("Key 列表，可拖动排序")
                .scrollContentBackground(.hidden)
            }
        }
        .padding(24)
    }

    func keyRow(_ configuration: KeyConfiguration) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(configuration.displayName)
                        .font(.headline)
                    Text(configuration.providerID == .routin
                        ? KeyDisplayMask.masked(suffix: configuration.keySuffix)
                        : ProviderRegistry.builtInDescriptors.first(where: { $0.id == configuration.providerID })?.displayName ?? configuration.providerID.rawValue)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundStyle(.secondary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("\(configuration.displayName)，\(KeyDisplayMask.masked(suffix: configuration.keySuffix))")

                Spacer()

                Toggle(
                    "在菜单栏显示",
                    isOn: Binding(
                        get: { configuration.isEnabled },
                        set: { setEnabled(configuration, enabled: $0) }
                    )
                )
                .labelsHidden()
                .controlSize(.small)
                .help(configuration.isEnabled ? "从菜单栏隐藏此 Key" : "在菜单栏显示此 Key")
                .accessibilityLabel(configuration.isEnabled ? "禁用 \(configuration.displayName)" : "启用 \(configuration.displayName)")

                Toggle(
                    "菜单栏指标",
                    isOn: Binding(
                        get: { settings.selectedCredentialIDs.contains(configuration.id) },
                        set: { setMenuBarSelection(configuration.id, selected: $0) }
                    )
                )
                .labelsHidden()
                .controlSize(.small)
                .help(settings.selectedCredentialIDs.contains(configuration.id) ? "从菜单栏指标中移除" : "添加到菜单栏指标（最多 4 个）")
                .accessibilityLabel(settings.selectedCredentialIDs.contains(configuration.id) ? "移除菜单栏指标" : "添加菜单栏指标")

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
        .opacity(configuration.isEnabled ? 1 : 0.55)
        .contentShape(Rectangle())
        .onHover { isHovered in
            if isHovered {
                NSCursor.pointingHand.push()
            } else {
                NSCursor.pop()
            }
        }
        .onDrag {
            NSItemProvider(object: configuration.id.uuidString as NSString)
        }
        .onDrop(
            of: [UTType.text],
            delegate: KeyRowDropDelegate(targetID: configuration.id) { draggedID in
                move(draggedID: draggedID, before: configuration.id)
            }
        )
        .onTapGesture {
            expandedKeyID = configuration.id
        }
        .listRowSeparator(.visible)
        .listRowSeparatorTint(Color.secondary.opacity(0.28))
        .listRowBackground(
            store.selectedKeyID == configuration.id
                ? Color.accentColor.opacity(0.10)
                : Color.clear
        )
    }

    @ViewBuilder
    func keyUsageOverview(_ state: KeyUsageState) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 10) {
                if snapshot.kind == .periodic {
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
            settingsPageHeader("显示与刷新", subtitle: "控制菜单栏展示内容与后台刷新频率")

            Form {
                Section("菜单栏显示") {
                    Picker("当前 Key", selection: selectedKeyBinding) {
                        ForEach(orderedKeyIDs, id: \.self) { id in
                            if let configuration = store.state(for: id)?.configuration {
                                Text(configuration.displayName).tag(Optional(id))
                            }
                        }
                    }
                    .disabled(orderedKeyIDs.isEmpty)
                    .accessibilityLabel("菜单栏当前 Key")

                    Picker("周期维度", selection: $settings.displayDimension) {
                        Text("5 小时").tag(DisplayDimension.fiveHour)
                        Text("周").tag(DisplayDimension.weekly)
                    }
                    .accessibilityLabel("周期订阅显示维度")

                    Picker("显示样式", selection: $settings.menuBarStyle) {
                        ForEach(MenuBarStyle.allCases, id: \.rawValue) { style in
                            Text(style.title).tag(style)
                        }
                    }
                    .accessibilityLabel("菜单栏显示样式")
                }

                Section("自动刷新") {
                    Picker("刷新间隔", selection: $settings.refreshMinutes) {
                        ForEach(AppSettings.allowedRefreshMinutes, id: \.self) { minutes in
                            Text("\(minutes) 分钟").tag(minutes)
                        }
                    }
                    .accessibilityLabel("自动刷新间隔")
                }
            }
            .formStyle(.grouped)
        }
        .padding(24)
    }

    var notificationsAndSystem: some View {
        VStack(alignment: .leading, spacing: 16) {
            settingsPageHeader("通知与系统", subtitle: "管理额度预警、登录启动、签到与软件更新")

            Form {
                Section("额度通知") {
                    Toggle("启用额度通知", isOn: $settings.notificationsEnabled)
                        .accessibilityLabel("启用额度通知")

                    Stepper("低阈值：\(lowThreshold)%", value: $lowThreshold, in: 1...100)
                        .disabled(!settings.notificationsEnabled)
                        .accessibilityLabel("低通知阈值，\(lowThreshold)%")
                        .onChange(of: lowThreshold) { _, _ in applyThresholds() }

                    Stepper("高阈值：\(highThreshold)%", value: $highThreshold, in: 1...100)
                        .disabled(!settings.notificationsEnabled)
                        .accessibilityLabel("高通知阈值，\(highThreshold)%")
                        .onChange(of: highThreshold) { _, _ in applyThresholds() }

                    if let thresholdError {
                        Text(thresholdError)
                            .font(.caption)
                            .foregroundStyle(.red)
                            .accessibilityLabel("阈值错误，\(thresholdError)")
                    }
                }

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

    var selectedKeyBinding: Binding<UUID?> {
        Binding(
            get: { store.selectedKeyID },
            set: { id in
                if let id {
                    store.selectKey(id)
                }
            }
        )
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

    func move(fromOffsets: IndexSet, toOffset: Int) {
        let validOffsets = fromOffsets.filter { orderedKeyIDs.indices.contains($0) }
        guard !validOffsets.isEmpty, (0...orderedKeyIDs.count).contains(toOffset) else {
            return
        }
        let moving = validOffsets.map { orderedKeyIDs[$0] }
        for index in validOffsets.reversed() {
            orderedKeyIDs.remove(at: index)
        }
        let removedBeforeDestination = validOffsets.filter { $0 < toOffset }.count
        orderedKeyIDs.insert(contentsOf: moving, at: toOffset - removedBeforeDestination)
        moveKey(IndexSet(validOffsets), toOffset)
    }

    func move(draggedID: UUID, before targetID: UUID) {
        guard
            draggedID != targetID,
            let sourceIndex = orderedKeyIDs.firstIndex(of: draggedID),
            let targetIndex = orderedKeyIDs.firstIndex(of: targetID)
        else {
            return
        }
        move(fromOffsets: IndexSet(integer: sourceIndex), toOffset: targetIndex)
    }

    func setEnabled(_ configuration: KeyConfiguration, enabled: Bool) {
        do {
            try setKeyEnabled(configuration.id, enabled)
        } catch {
            operationError = "无法更新 Key 显示状态，请稍后重试"
        }
    }

    func setMenuBarSelection(_ id: UUID, selected: Bool) {
        var ids = settings.selectedCredentialIDs
        if selected {
            guard !ids.contains(id), ids.count < 4 else { return }
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
        } catch {
            operationError = "无法删除 Key，请稍后重试"
        }
    }

    func applyThresholds() {
        do {
            try KeyEditorValidation.validateThresholds(low: lowThreshold, high: highThreshold)
            settings.thresholds = AlertThresholds(low: lowThreshold, high: highThreshold)
            thresholdError = nil
        } catch {
            thresholdError = error.localizedDescription
        }
    }
}
