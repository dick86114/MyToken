import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    typealias StartRoutinCheckIn = @MainActor () async -> Void

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings
    let updateStatus: AppUpdateStatus
    let installAvailableUpdate: InstallAvailableUpdate
    let checkInState: RoutinCheckInState
    let startRoutinCheckIn: StartRoutinCheckIn

    @Environment(\.openWindow) private var openWindow

    init(
        store: UsageStore,
        settings: AppSettings,
        updateStatus: AppUpdateStatus = .idle,
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        checkInState: RoutinCheckInState = .idle,
        startRoutinCheckIn: @escaping StartRoutinCheckIn = {}
    ) {
        self.store = store
        self.settings = settings
        self.updateStatus = updateStatus
        self.installAvailableUpdate = installAvailableUpdate
        self.checkInState = checkInState
        self.startRoutinCheckIn = startRoutinCheckIn
    }

    var body: some View {
        VStack(spacing: 0) {
            toolbar
                .padding(.horizontal, 12)
                .padding(.vertical, 10)

            Divider()

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
            .disabled(store.isRefreshing || store.orderedKeyIDs.isEmpty)
            .help("刷新全部 Key")
            .accessibilityLabel(store.isRefreshing ? "正在刷新全部 Key" : "刷新全部 Key")

            Button {
                Task { await startRoutinCheckIn() }
            } label: {
                if checkInState.isBusy {
                    ProgressView()
                        .controlSize(.small)
                        .frame(width: 16, height: 16)
                } else {
                    Image(systemName: "checkmark.circle")
                }
            }
            .liquidGlassButton()
            .disabled(checkInState.isBusy)
            .help("Routin 签到")
            .accessibilityLabel(checkInState.isBusy ? "正在处理 Routin 签到" : "Routin 签到")
        }
        .overlay(alignment: .center) {
            Image(nsImage: NSApp.applicationIconImage)
                .resizable()
                .interpolation(.high)
                .scaledToFit()
                .frame(width: 22, height: 22)
                .accessibilityLabel("MyRoutin")
        }
    }

    @ViewBuilder
    var usageList: some View {
        if store.orderedKeyIDs.isEmpty {
            VStack(spacing: 8) {
                Image(systemName: "key.slash")
                    .font(.title2)
                    .foregroundStyle(.secondary)
                Text("尚未配置 Key")
                    .font(.headline)
                Text("请在设置中添加一个 plan Key")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("空配置，尚未配置 Key")
        } else {
            VStack(spacing: 0) {
                ForEach(store.orderedKeyIDs, id: \.self) { id in
                    if let state = store.state(for: id) {
                        UsageRowView(
                            store: store,
                            state: state
                        )
                        if id != store.orderedKeyIDs.last {
                            Divider()
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
        }
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

            HStack {
                Button("设置") {
                    openWindow(id: "settings")
                }
                .liquidGlassButton()
                .keyboardShortcut(",")

                Spacer()

                Button("退出 MyRoutin") {
                    NSApplication.shared.terminate(nil)
                }
                .liquidGlassButton()
                .keyboardShortcut("q")
            }
        }
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
