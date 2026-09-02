import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    typealias StartRoutinCheckIn = @MainActor () async -> Void
    typealias StartCodexGroupDetection = @MainActor (UUID) async -> Void

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings
    @Bindable var codexGroupDetection: CodexGroupDetectionService
    let updateStatus: AppUpdateStatus
    let installAvailableUpdate: InstallAvailableUpdate
    let checkInState: RoutinCheckInState
    let startRoutinCheckIn: StartRoutinCheckIn
    let startCodexGroupDetection: StartCodexGroupDetection

    @Environment(\.openWindow) private var openWindow
    @State private var pendingDetectionKeyID: UUID?

    init(
        store: UsageStore,
        settings: AppSettings,
        codexGroupDetection: CodexGroupDetectionService,
        updateStatus: AppUpdateStatus = .idle,
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        checkInState: RoutinCheckInState = .idle,
        startRoutinCheckIn: @escaping StartRoutinCheckIn = {},
        startCodexGroupDetection: @escaping StartCodexGroupDetection = { _ in }
    ) {
        self.store = store
        self.settings = settings
        self.codexGroupDetection = codexGroupDetection
        self.updateStatus = updateStatus
        self.installAvailableUpdate = installAvailableUpdate
        self.checkInState = checkInState
        self.startRoutinCheckIn = startRoutinCheckIn
        self.startCodexGroupDetection = startCodexGroupDetection
    }

    var body: some View {
        VStack(spacing: 0) {
            toolbar
                .padding(.horizontal, 12)
                .padding(.vertical, 10)

            Divider()

            usageListHeader

            usageList
                .padding(.vertical, 4)

            Divider()

            footer
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
        }
        // 让窗口按内容自然撑开，避免固定高度造成不必要的竖向滚动条。
        .frame(width: 440)
        .fixedSize(horizontal: false, vertical: true)
        .liquidGlassWindowBackground()
        .confirmationDialog(
            "获取 Codex 当前分组？",
            isPresented: Binding(
                get: { pendingDetectionKeyID != nil },
                set: { if !$0 { pendingDetectionKeyID = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("继续") {
                guard let keyID = pendingDetectionKeyID else { return }
                pendingDetectionKeyID = nil
                Task {
                    await startCodexGroupDetection(keyID)
                    if codexGroupDetection.state(for: keyID) == .needsLogin {
                        openWindow(id: "routin-check-in")
                    }
                }
            }
            Button("取消", role: .cancel) { pendingDetectionKeyID = nil }
        } message: {
            Text("将发送一次真实 Codex 请求并产生极少量额度消耗。")
        }
    }
}

private extension UsagePopoverView {
    var toolbar: some View {
        HStack(spacing: 10) {
            Link(destination: RoutinUsageApp.githubURL) {
                Text("v\(RoutinUsageApp.currentVersion)")
                    .underline()
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("打开 GitHub 项目")
            .accessibilityLabel("当前版本 v\(RoutinUsageApp.currentVersion)，打开 GitHub 项目")

            Spacer()
            Button {
                Task { await store.refreshAll() }
            } label: {
                if store.isRefreshing {
                    ProgressView()
                        .controlSize(.small)
                        .frame(width: 16, height: 16)
                } else {
                    Image(systemName: "arrow.clockwise")
                }
            }
            .liquidGlassButton()
            .disabled(store.isRefreshing || store.visibleKeyIDs.isEmpty)
            .help("刷新全部 Key")
            .accessibilityLabel(store.isRefreshing ? "正在刷新全部 Key" : "刷新全部 Key")

            if isSelectedRoutin {
                Button {
                    openWindow(id: "routin-check-in")
                    Task { await startRoutinCheckIn() }
                } label: {
                    if checkInState.isBusy {
                        ProgressView()
                            .controlSize(.small)
                            .frame(width: 16, height: 16)
                    } else if checkInState == .alreadyCheckedIn {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Color.green)
                    } else {
                        Image(systemName: "checkmark.circle")
                    }
                }
                .liquidGlassButton()
                .disabled(checkInState.isBusy)
                .help(checkInHelpText)
                .accessibilityLabel(checkInHelpText)
            }
        }
        .overlay(alignment: .center) {
            Link(destination: RoutinUsageApp.websiteURL) {
                Image(nsImage: NSImage(named: "PopoverColorBrandLogo") ?? NSApp.applicationIconImage)
                    .resizable()
                    .interpolation(.high)
                    .scaledToFit()
                    .frame(width: 28, height: 28)
                    .shadow(color: .black.opacity(0.22), radius: 1.5, y: 1)
            }
            .buttonStyle(.plain)
            .help("打开 Routin 官网")
            .accessibilityLabel("打开 Routin 官网")
        }
    }

    @ViewBuilder
    var usageList: some View {
        if store.visibleKeyIDs.isEmpty {
            VStack(spacing: 8) {
                Image(systemName: "key.slash")
                    .font(.title2)
                    .foregroundStyle(.secondary)
                Text(store.orderedKeyIDs.isEmpty ? "尚未配置 Key" : "没有启用的 Key")
                    .font(.headline)
                Text(store.orderedKeyIDs.isEmpty ? "请在设置中添加一个 plan Key" : "请在设置中启用至少一个 Key")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("空配置，尚未配置 Key")
        } else {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(providerGroups, id: \.id) { group in
                    VStack(alignment: .leading, spacing: 5) {
                        HStack(spacing: 6) {
                            Image(systemName: group.iconName)
                                .foregroundStyle(.secondary)
                            Text(group.displayName)
                                .font(.caption.weight(.semibold))
                            Text("\(group.ids.count)")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        VStack(spacing: 0) {
                            ForEach(group.ids, id: \.self) { id in
                                if let state = store.state(for: id) {
                                    UsageRowView(
                                        store: store,
                                        state: state,
                                        detectionState: codexGroupDetection.state(for: id),
                                        detectionRecord: codexGroupDetection.record(for: id),
                                        isAnotherDetectionActive: codexGroupDetection.activeKeyID != nil
                                            && codexGroupDetection.activeKeyID != id,
                                        requestDetection: { pendingDetectionKeyID = id }
                                    )
                                    if id != group.ids.last {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
        }
    }

    struct ProviderGroup: Identifiable {
        let id: ProviderID
        let displayName: String
        let iconName: String
        let ids: [UUID]
    }

    var providerGroups: [ProviderGroup] {
        var groups: [ProviderID: [UUID]] = [:]
        for id in store.visibleKeyIDs {
            guard let state = store.state(for: id) else { continue }
            groups[state.configuration.providerID, default: []].append(id)
        }
        return ProviderID.allCases.compactMap { id in
            guard let ids = groups[id], !ids.isEmpty,
                  let descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == id })
            else { return nil }
            return ProviderGroup(id: id, displayName: descriptor.displayName, iconName: descriptor.iconName, ids: ids)
        }
    }

    var isSelectedRoutin: Bool {
        store.selectedKeyID.flatMap(store.state(for:))?.configuration.providerID == .routin
    }

    var usageListHeader: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text("账户用量")
                    .font(.subheadline.weight(.semibold))
                Text("\(store.visibleKeyIDs.count) 个 Key")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 8)

            if let selectedState = store.selectedKeyID.flatMap(store.state(for:)) {
                Label("当前账户 · \(selectedState.configuration.displayName)", systemImage: "checkmark.circle.fill")
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(Color.accentColor)
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 10)
        .padding(.bottom, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            store.selectedKeyID.flatMap(store.state(for:)).map {
                "账户用量，共 \(store.visibleKeyIDs.count) 个 Key，当前账户 \($0.configuration.displayName)"
            } ?? "账户用量，共 \(store.visibleKeyIDs.count) 个 Key"
        )
    }

    var footer: some View {
        VStack(alignment: .leading, spacing: 9) {
            if case let .available(update) = updateStatus {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.down.circle.fill")
                        .foregroundStyle(.blue)
                    Text("发现新版本 \(update.version)")
                    Spacer(minLength: 4)
                    Button("安装") {
                        Task { await installAvailableUpdate() }
                    }
                    .liquidGlassButton(prominent: true)
                }
                .font(.caption)
                .accessibilityElement(children: .combine)
            }

            if case let .downloading(progress) = updateStatus {
                updateProgressView(progress)
            }

            if case let .completed(version) = updateStatus {
                Label("更新完成，当前版本 \(version)", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.green)
                    .accessibilityElement(children: .combine)
            }

            if checkInState != .idle {
                HStack(spacing: 6) {
                    Image(systemName: checkInState.isTerminalResult ? "checkmark.circle" : "checkmark.circle.badge.questionmark")
                        .accessibilityHidden(true)
                    Text(checkInState.statusText)
                        .lineLimit(2)
                }
                .font(.caption)
                .foregroundStyle(checkInState.isTerminalResult ? Color.secondary : Color.orange)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Routin 签到：\(checkInState.statusText)")
            }

            codexGroupDetectionStatus

            HStack(spacing: 6) {
                Image(systemName: store.isRefreshing ? "arrow.triangle.2.circlepath" : "clock")
                    .accessibilityHidden(true)
                Text(refreshDescription)
                    .lineLimit(1)
                Spacer(minLength: 4)
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .accessibilityLabel(refreshAccessibilityLabel)

            HStack(spacing: 10) {
                Button {
                    openWindow(id: "settings")
                } label: {
                    Image(systemName: "gearshape")
                        .frame(width: 18, height: 18)
                }
                .liquidGlassButton()
                .keyboardShortcut(",")
                .help("设置")
                .accessibilityLabel("打开设置")

                Spacer()

                Button {
                    NSApplication.shared.terminate(nil)
                } label: {
                    Image(systemName: "power")
                        .frame(width: 18, height: 18)
                }
                .liquidGlassButton()
                .keyboardShortcut("q")
                .help("退出 MyRoutin")
                .accessibilityLabel("退出 MyRoutin")
            }
        }
    }

    @ViewBuilder
    var codexGroupDetectionStatus: some View {
        let activeStates = store.visibleKeyIDs.compactMap { keyID -> (KeyUsageState, CodexGroupDetectionState)? in
            guard let keyState = store.state(for: keyID) else {
                return nil
            }
            let detectionState = codexGroupDetection.state(for: keyID)
            guard detectionState != .idle else {
                return nil
            }
            return (keyState, detectionState)
        }

        if let (keyState, detectionState) = activeStates.first(where: { $0.1.isBusy || $0.1 == .needsLogin })
            ?? activeStates.first {
            HStack(alignment: .top, spacing: 6) {
                codexGroupDetectionStatusIcon(for: detectionState)
                Text("Codex 分组：\(keyState.configuration.displayName)，\(codexGroupDetectionText(for: keyState, state: detectionState))")
                    .lineLimit(2)
                Spacer(minLength: 4)
            }
            .font(.caption)
            .foregroundStyle(codexGroupDetectionColor(for: detectionState))
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Codex 分组检测：\(keyState.configuration.displayName)，\(codexGroupDetectionText(for: keyState, state: detectionState))")
        }
    }

    @ViewBuilder
    func codexGroupDetectionStatusIcon(for state: CodexGroupDetectionState) -> some View {
        if state.isBusy {
            ProgressView()
                .controlSize(.small)
                .frame(width: 14, height: 14)
                .accessibilityHidden(true)
        } else {
            Image(systemName: codexGroupDetectionSymbol(for: state))
                .accessibilityHidden(true)
        }
    }

    func codexGroupDetectionText(
        for keyState: KeyUsageState,
        state: CodexGroupDetectionState
    ) -> String {
        if state == .succeeded,
           let groupName = codexGroupDetection.record(for: keyState.configuration.id)?.groupName {
            return "已获取当前分组：\(groupName)"
        }
        return state.statusText
    }

    func codexGroupDetectionSymbol(for state: CodexGroupDetectionState) -> String {
        if state.isBusy {
            return "arrow.triangle.2.circlepath"
        }
        if state == .succeeded {
            return "checkmark.circle.fill"
        }
        if state == .needsLogin {
            return "person.crop.circle.badge.exclamationmark"
        }
        return state.isFailure ? "exclamationmark.triangle.fill" : "info.circle"
    }

    func codexGroupDetectionColor(for state: CodexGroupDetectionState) -> Color {
        if state == .succeeded {
            return .green
        }
        return state.isFailure ? .orange : .secondary
    }

    var checkInHelpText: String {
        if checkInState.isBusy {
            return "正在处理 Routin 签到"
        }

        if checkInState == .alreadyCheckedIn {
            return "今天已签到"
        }

        return "Routin 签到"
    }

    @ViewBuilder
    func updateProgressView(_ progress: Double?) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(spacing: 6) {
                Image(systemName: "arrow.down.circle")
                    .accessibilityHidden(true)
                Text("正在下载更新")
                Spacer(minLength: 4)
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
        }
        .font(.caption)
        .foregroundStyle(.secondary)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            progress.map { "正在下载更新，已完成 \(Int($0 * 100))%" } ?? "正在下载更新"
        )
    }

    var latestRefreshDate: Date? {
        store.states.values.compactMap(\.lastSuccessAt).max()
    }

    var refreshDescription: String {
        if store.isRefreshing {
            return "正在刷新"
        }
        guard let latestRefreshDate else {
            return "尚未刷新"
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return "最后刷新 \(formatter.string(from: latestRefreshDate))"
    }

    var refreshAccessibilityLabel: String {
        if store.isRefreshing {
            return "正在刷新用量"
        }
        guard latestRefreshDate != nil else {
            return "尚未刷新用量"
        }
        return refreshDescription
    }
}
