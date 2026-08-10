import SwiftUI

enum KeyDisplayMask {
    static func masked(suffix: String) -> String {
        "plan-••••\(suffix.suffix(4))"
    }
}

@MainActor
struct SettingsView: View {
    typealias UpdateValidatedKey = @MainActor (UUID, String, String) async throws -> KeyEditorSaveResult
    typealias MoveKey = @MainActor (IndexSet, Int) -> Void

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
        moveKey: @escaping MoveKey
    ) {
        self.store = store
        self.settings = settings
        self.loginItemManager = loginItemManager
        self.updateValidatedKey = updateValidatedKey
        self.moveKey = moveKey
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
        .padding(16)
        .frame(minWidth: 520, idealWidth: 560, minHeight: 420, idealHeight: 500)
        .onChange(of: store.orderedKeyIDs) { _, ids in
            orderedKeyIDs = ids
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
            Text("将同时删除系统钥匙串中的 Key 和本地缓存，此操作无法撤销。")
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
            }
        }
    }

    func keyRow(_ configuration: KeyConfiguration) -> some View {
        HStack(spacing: 12) {
            Image(systemName: store.selectedKeyID == configuration.id ? "circle.inset.filled" : "circle")
                .foregroundStyle(store.selectedKeyID == configuration.id ? Color.accentColor : Color.secondary)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(configuration.name)
                Text(KeyDisplayMask.masked(suffix: configuration.keySuffix))
                    .font(.system(.caption, design: .monospaced))
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(configuration.name)，\(KeyDisplayMask.masked(suffix: configuration.keySuffix))")

            Spacer()

            Button("编辑") { editor = .edit(configuration) }
                .buttonStyle(.borderless)
                .accessibilityLabel("编辑 \(configuration.name)")

            Button(role: .destructive) {
                pendingDeletion = configuration
            } label: {
                Image(systemName: "trash")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("删除 \(configuration.name)")
        }
        .contentShape(Rectangle())
        .onTapGesture { store.selectKey(configuration.id) }
    }

    var displayAndRefresh: some View {
        Form {
            Section("菜单栏显示") {
                Picker("当前 Key", selection: selectedKeyBinding) {
                    ForEach(orderedKeyIDs, id: \.self) { id in
                        if let configuration = store.state(for: id)?.configuration {
                            Text(configuration.name).tag(Optional(id))
                        }
                    }
                }
                .disabled(orderedKeyIDs.isEmpty)
                .accessibilityLabel("菜单栏当前 Key")

                Picker("周期维度", selection: $settings.displayDimension) {
                    Text("五小时").tag(DisplayDimension.fiveHour)
                    Text("周").tag(DisplayDimension.weekly)
                }
                .accessibilityLabel("周期订阅显示维度")
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

    var notificationsAndSystem: some View {
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
        }
        .formStyle(.grouped)
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
                let previousValue = settings.launchAtLogin
                settings.launchAtLogin = enabled
                do {
                    try loginItemManager.setEnabled(enabled)
                } catch {
                    settings.launchAtLogin = previousValue
                    operationError = "无法更新登录启动设置：\(error.localizedDescription)"
                }
            }
        )
    }

    @ViewBuilder
    private func editorView(for presentation: EditorPresentation) -> some View {
        switch presentation {
        case .add:
            KeyEditorView(save: addKey)
        case let .edit(configuration):
            KeyEditorView(title: "编辑 Key", initialName: configuration.name) { name, secret in
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
