import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings

    @Environment(\.openSettings) private var openSettings

    var body: some View {
        VStack(spacing: 0) {
            toolbar
                .padding(12)

            Divider()

            usageList

            Divider()

            footer
                .padding(12)
        }
        .frame(width: 360)
        .frame(maxHeight: 520)
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
            .buttonStyle(.borderless)
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
            ScrollView {
                LazyVStack(spacing: 0) {
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
            .frame(maxHeight: 380)
        }
    }

    var footer: some View {
        VStack(alignment: .leading, spacing: 9) {
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
                    openSettings()
                }
                .keyboardShortcut(",")

                Spacer()

                Button("退出 Routin Usage") {
                    NSApplication.shared.terminate(nil)
                }
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
        return "最后刷新 \(latestRefreshDate.formatted(date: .abbreviated, time: .shortened))"
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
