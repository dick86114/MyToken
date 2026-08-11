import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings
    let updateStatus: AppUpdateStatus
    let installAvailableUpdate: InstallAvailableUpdate

    @Environment(\.openWindow) private var openWindow

    init(
        store: UsageStore,
        settings: AppSettings,
        updateStatus: AppUpdateStatus = .idle,
        installAvailableUpdate: @escaping InstallAvailableUpdate = {}
    ) {
        self.store = store
        self.settings = settings
        self.updateStatus = updateStatus
        self.installAvailableUpdate = installAvailableUpdate
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
            Picker("用量周期", selection: $settings.displayDimension) {
                Text("5 小时").tag(DisplayDimension.fiveHour)
                Text("周").tag(DisplayDimension.weekly)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .accessibilityLabel("用量显示周期")

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
                            state: state,
                            dimension: settings.displayDimension
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
