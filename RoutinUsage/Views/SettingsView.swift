import AppKit
import SwiftUI

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

@MainActor
struct SettingsView: View {
    typealias UpdateValidatedKey = @MainActor (UUID, String, String) async throws -> KeyEditorSaveResult
    typealias MoveKey = @MainActor (IndexSet, Int) -> Void
    typealias CheckForUpdates = @MainActor () async -> Void
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    typealias ReadKey = @MainActor (UUID) -> String?

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

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings

    private let loginItemManager: any LoginItemManaging
    private let updateValidatedKey: UpdateValidatedKey
    private let moveKey: MoveKey
    private let updateStatus: AppUpdateStatus
    private let checkForUpdates: CheckForUpdates
    private let installAvailableUpdate: InstallAvailableUpdate
    private let readKey: ReadKey

    @State private var editor: EditorPresentation?
    @State private var pendingDeletion: KeyConfiguration?
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
        moveKey: @escaping MoveKey,
        updateStatus: AppUpdateStatus = .idle,
        checkForUpdates: @escaping CheckForUpdates = {},
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        readKey: @escaping ReadKey = { _ in nil }
    ) {
        self.store = store
        self.settings = settings
        self.loginItemManager = loginItemManager
        self.updateValidatedKey = updateValidatedKey
        self.moveKey = moveKey
        self.updateStatus = updateStatus
        self.checkForUpdates = checkForUpdates
        self.installAvailableUpdate = installAvailableUpdate
        self.readKey = readKey
        _orderedKeyIDs = State(initialValue: store.orderedKeyIDs)
        _lowThreshold = State(initialValue: settings.thresholds.low)
        _highThreshold = State(initialValue: settings.thresholds.high)
    }

    var body: some View {
        TabView {
            keyManagement
                .tabItem { Label("Key", systemImage: "key") }

            displayAndRefresh
                .tabItem { Label("显示与刷新", systemImage: "slider.horizontal.3") }

            notificationsAndSystem
                .tabItem { Label("通知与系统", systemImage: "bell") }
        }
        .padding(12)
        .liquidGlassSurface(cornerRadius: 18)
        .padding(16)
        .frame(minWidth: 520, idealWidth: 560, minHeight: 420, idealHeight: 500)
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
    var keyManagement: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Key 管理")
                        .font(.headline)
                    Text("拖动列表可调整菜单中的显示顺序")
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
                .accessibilityLabel("Key 列表，可拖动排序")
                .scrollContentBackground(.hidden)
            }
        }
        .padding(14)
        .liquidGlassSurface(cornerRadius: 14)
    }

    func keyRow(_ configuration: KeyConfiguration) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
            Image(systemName: store.selectedKeyID == configuration.id ? "circle.inset.filled" : "circle")
                .foregroundStyle(store.selectedKeyID == configuration.id ? Color.accentColor : Color.secondary)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(configuration.displayName)
                Text(KeyDisplayMask.masked(suffix: configuration.keySuffix))
                    .font(.system(.caption, design: .monospaced))
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(configuration.displayName)，\(KeyDisplayMask.masked(suffix: configuration.keySuffix))")

            Spacer()

            Button("编辑") { editor = .edit(configuration) }
                .liquidGlassButton()
                .accessibilityLabel("编辑 \(configuration.displayName)")

            Button(role: .destructive) {
                pendingDeletion = configuration
            } label: {
                Image(systemName: "trash")
            }
            .liquidGlassButton()
            .accessibilityLabel("删除 \(configuration.displayName)")
            }

            if let state = store.state(for: configuration.id) {
                keyUsageDetails(state)
                    .padding(.leading, 24)
            }
        }
        .padding(8)
        .liquidGlassSurface(cornerRadius: 14)
        .contentShape(Rectangle())
        .onTapGesture { store.selectKey(configuration.id) }
    }

    @ViewBuilder
    func keyUsageDetails(_ state: KeyUsageState) -> some View {
        if let snapshot = state.snapshot {
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 12) {
                    detailItem("套餐", snapshot.planName.isEmpty ? "未命名套餐" : snapshot.planName)
                    detailItem("类型", snapshot.kind == .periodic ? "周期订阅" : "Token 资源包")
                    detailItem("状态", subscriptionStatus(snapshot.status, state: state))
                }

                HStack(spacing: 12) {
                    detailItem("订阅开始", UsageFormatter.fullDateTime(snapshot.subscriptionStartAt))
                    detailItem("订阅结束", UsageFormatter.fullDateTime(snapshot.subscriptionEndAt))
                }

                if snapshot.kind == .periodic {
                    VStack(alignment: .leading, spacing: 4) {
                        if let metric = snapshot.fiveHour {
                            HStack(spacing: 12) {
                            usageDetailItem("5 小时用量", metric)
                            detailItem("5 小时结束", UsageFormatter.fullDateTime(metric.windowEnd))
                            }
                        }
                        if let metric = snapshot.weekly {
                            HStack(spacing: 12) {
                            usageDetailItem("周用量", metric)
                            detailItem("周结束", UsageFormatter.fullDateTime(metric.windowEnd))
                            }
                        }
                    }
                } else if let token = snapshot.token {
                    usageDetailItem("Token 用量", token)
                }

                detailItem(
                    "分组倍率",
                    snapshot.groupMultipliers.isEmpty
                        ? "—"
                        : UsageFormatter.groupMultiplierText(snapshot.groupMultipliers)
                )

                if !snapshot.allowedModels.isEmpty {
                    detailItem("允许模型", snapshot.allowedModels.joined(separator: "、"))
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        } else {
            Text(UsageFormatter.statusText(state: state))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    func detailItem(_ title: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 3) {
            Text(title + "：")
                .foregroundStyle(.tertiary)
            Text(value)
                .lineLimit(2)
                .textSelection(.enabled)
        }
    }

    func usageDetailItem(_ title: String, _ metric: UsageMetric) -> some View {
        detailItem(
            title,
            "\(UsageFormatter.amount(metric))（\(UsageFormatter.percentText(metric) ?? "—")）"
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

    var displayAndRefresh: some View {
        Form {
            glassSection {
                Section("菜单栏显示") {
                    Picker("当前 Key", selection: selectedKeyBinding) {
                        ForEach(orderedKeyIDs, id: \.self) { id in
                            if let configuration = store.state(for: id)?.configuration {
                                Text(configuration.displayName).tag(Optional(id))
                            }
                        }
                    }
                    .disabled(orderedKeyIDs.isEmpty)
                    .liquidGlassControlSurface()
                    .accessibilityLabel("菜单栏当前 Key")

                    Picker("周期维度", selection: $settings.displayDimension) {
                        Text("5 小时").tag(DisplayDimension.fiveHour)
                        Text("周").tag(DisplayDimension.weekly)
                    }
                    .liquidGlassControlSurface()
                    .accessibilityLabel("周期订阅显示维度")

                    Picker("显示样式", selection: $settings.menuBarStyle) {
                        ForEach(MenuBarStyle.allCases, id: \.rawValue) { style in
                            Text(style.title).tag(style)
                        }
                    }
                    .liquidGlassControlSurface()
                    .accessibilityLabel("菜单栏显示样式")
                }
            }

            glassSection {
                Section("自动刷新") {
                    Picker("刷新间隔", selection: $settings.refreshMinutes) {
                        ForEach(AppSettings.allowedRefreshMinutes, id: \.self) { minutes in
                            Text("\(minutes) 分钟").tag(minutes)
                        }
                    }
                    .liquidGlassControlSurface()
                    .accessibilityLabel("自动刷新间隔")
                }
            }
        }
        .formStyle(.grouped)
    }

    var notificationsAndSystem: some View {
        Form {
            glassSection {
                Section("额度通知") {
                    Toggle("启用额度通知", isOn: $settings.notificationsEnabled)
                        .liquidGlassControlSurface()
                        .accessibilityLabel("启用额度通知")

                    Stepper("低阈值：\(lowThreshold)%", value: $lowThreshold, in: 1...100)
                        .disabled(!settings.notificationsEnabled)
                        .liquidGlassControlSurface()
                        .accessibilityLabel("低通知阈值，\(lowThreshold)%")
                        .onChange(of: lowThreshold) { _, _ in applyThresholds() }

                    Stepper("高阈值：\(highThreshold)%", value: $highThreshold, in: 1...100)
                        .disabled(!settings.notificationsEnabled)
                        .liquidGlassControlSurface()
                        .accessibilityLabel("高通知阈值，\(highThreshold)%")
                        .onChange(of: highThreshold) { _, _ in applyThresholds() }

                    if let thresholdError {
                        Text(thresholdError)
                            .font(.caption)
                            .foregroundStyle(.red)
                            .accessibilityLabel("阈值错误，\(thresholdError)")
                    }
                }
            }

            glassSection {
                Section("系统") {
                    Toggle("登录时启动", isOn: launchAtLoginBinding)
                        .liquidGlassControlSurface()
                        .accessibilityLabel("登录时启动")
                }
            }

            glassSection {
                Section("软件更新") {
                    LabeledContent("当前版本") {
                        Text(currentVersion)
                            .accessibilityLabel("当前版本 \(currentVersion)")
                    }
                    updateControls
                }
            }
        }
        .formStyle(.grouped)
    }

    @ViewBuilder
    func glassSection<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        content()
            .listRowBackground(Color.clear)
            .liquidGlassSurface(cornerRadius: 14)
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

    @ViewBuilder
    var updateControls: some View {
        switch updateStatus {
        case .idle:
            Button("检查更新") { Task { await checkForUpdates() } }
                .liquidGlassButton()
        case .checking:
            LabeledContent("正在检查更新") {
                ProgressView()
                    .controlSize(.small)
                    .liquidGlassProgressSurface()
            }
        case let .available(update):
            VStack(alignment: .leading, spacing: 6) {
                Text("发现新版本 \(update.version)")
                    .fontWeight(.medium)
                UpdateNotesView(notes: update.notes)
                HStack {
                    Button("安装更新") { Task { await installAvailableUpdate() } }
                        .liquidGlassButton(prominent: true)
                    Link("查看发布说明", destination: update.releaseURL)
                }
            }
        case .downloading:
            LabeledContent("正在下载并安装更新") {
                ProgressView()
                    .controlSize(.small)
                    .liquidGlassProgressSurface()
            }
        case let .failed(message):
            VStack(alignment: .leading, spacing: 6) {
                Text(message).foregroundStyle(.red)
                Button("重试") { Task { await checkForUpdates() } }
                    .liquidGlassButton()
            }
        }
    }

    var currentVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
    }

    @ViewBuilder
    private func editorView(for presentation: EditorPresentation) -> some View {
        switch presentation {
        case .add:
            KeyEditorView(save: addKey)
        case let .edit(configuration):
            KeyEditorView(
                title: "编辑 Key",
                initialName: configuration.displayName,
                initialSecret: readKey(configuration.id) ?? ""
            ) { name, secret in
                try await updateValidatedKey(configuration.id, name, secret)
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

    func deletePendingKey() {
        guard let configuration = pendingDeletion else {
            return
        }
        pendingDeletion = nil
        do {
            try store.deleteKey(configuration.id)
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
