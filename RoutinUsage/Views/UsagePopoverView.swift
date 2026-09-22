import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings
    let updateStatus: AppUpdateStatus
    let installAvailableUpdate: InstallAvailableUpdate
    let refreshCredential: @MainActor (UUID) async -> Void
    let retryCredential: @MainActor (UUID) async -> Void
    let openSettings: @MainActor () -> Void

    @State private var providerFilter: ProviderID?
    @State private var selectedUpdate: AppUpdate?
    @State private var isUpdateIndicatorVisible = true
    @State private var showRefreshSuccess = false
    @State private var isVersionLinkHovered = false
    @State private var isUpdateBadgeHovered = false
    @State private var scrollMetrics = PopoverScrollMetrics(contentHeight: 0, contentOffset: 0)

    init(
        store: UsageStore,
        settings: AppSettings,
        updateStatus: AppUpdateStatus = .idle,
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        refreshCredential: @escaping @MainActor (UUID) async -> Void = { _ in },
        retryCredential: @escaping @MainActor (UUID) async -> Void = { _ in },
        openSettings: @escaping @MainActor () -> Void = {}
    ) {
        self.store = store
        self.settings = settings
        self.updateStatus = updateStatus
        self.installAvailableUpdate = installAvailableUpdate
        self.refreshCredential = refreshCredential
        self.retryCredential = retryCredential
        self.openSettings = openSettings
    }

    var body: some View {
        VStack(spacing: 0) {
            toolbar
                .padding(.horizontal, 12)
                .padding(.vertical, 10)

            Divider()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    usageListHeader

                    usageList
                        .padding(.vertical, 4)
                        .padding(.bottom, 8)

                    footerStatuses
                        .padding(.horizontal, 16)
                        .padding(.bottom, 8)
                }
                .background(
                    GeometryReader { geometry in
                        Color.clear.preference(
                            key: PopoverScrollMetricsKey.self,
                            value: PopoverScrollMetrics(
                                contentHeight: geometry.size.height,
                                contentOffset: -geometry.frame(in: .named("popoverScroll")).minY
                            )
                        )
                    }
                )
            }
            .coordinateSpace(name: "popoverScroll")
            .overlay(alignment: .trailing) {
                GeometryReader { geometry in
                    ThinVerticalScrollIndicator(
                        metrics: scrollMetrics,
                        viewportHeight: geometry.size.height
                    )
                }
                .frame(width: 4)
                .padding(.trailing, 1)
            }
            .onPreferenceChange(PopoverScrollMetricsKey.self) { scrollMetrics = $0 }

            Divider()

            bottomBar
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
        }
        .frame(width: 440)
        // 窗口偶尔高于内容时固定顶部对齐，避免内容悬浮居中。
        .frame(maxHeight: maxPopoverHeight, alignment: .top)
        .liquidGlassWindowBackground()
        .overlay {
            updateReleaseOverlay
        }
    }
}

private extension UsagePopoverView {
    var toolbar: some View {
        HStack(spacing: 10) {
            HStack(spacing: 6) {
                Link(destination: RoutinUsageApp.releasesURL) {
                    HStack(spacing: 4) {
                        Image(systemName: "tag")
                            .imageScale(.small)
                        Text("v\(RoutinUsageApp.currentVersion)")
                            .monospacedDigit()
                    }
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background {
                        Capsule()
                            .fill(Color.primary.opacity(isVersionLinkHovered ? 0.12 : 0.055))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(Color.primary.opacity(isVersionLinkHovered ? 0.24 : 0.12))
                    }
                }
                .buttonStyle(.plain)
                .onHover { isVersionLinkHovered = $0 }
                .animation(.easeInOut(duration: 0.15), value: isVersionLinkHovered)
                .foregroundStyle(.secondary)
                .help("打开 Releases 页面")
                .accessibilityLabel("当前版本 v\(RoutinUsageApp.currentVersion)，打开 Releases 页面")

                if case let .available(update) = updateStatus {
                    Button {
                        selectedUpdate = update
                    } label: {
                        Image(systemName: "arrow.up")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.green)
                            .frame(width: 22, height: 22)
                            .background {
                                Circle()
                                    .fill(Color.green.opacity(0.10))
                            }
                            .overlay {
                                Circle()
                                    .strokeBorder(Color.green.opacity(0.55), lineWidth: 1)
                            }
                            .opacity(isUpdateIndicatorVisible ? 1 : 0.28)
                            .scaleEffect(isUpdateBadgeHovered ? 1.08 : isUpdateIndicatorVisible ? 1 : 0.94)
                            .onAppear {
                                withAnimation(.easeInOut(duration: 0.72).repeatForever(autoreverses: true)) {
                                    isUpdateIndicatorVisible = false
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .onHover { isUpdateBadgeHovered = $0 }
                    .help("查看 v\(update.version) 更新详情")
                    .accessibilityLabel("发现新版本 v\(update.version)，查看更新详情")
                }
            }
            .overlay(alignment: .leading) {
                EmptyView()
            }

            Spacer()

            HStack(spacing: 8) {
                UsageCardDensitySegmentedControl(
                    selection: $settings.usageCardDensity
                )

                GlassIconButton(
                    action: { openSettings() },
                    help: "设置"
                ) {
                    Image(systemName: "gearshape")
                        .font(.system(size: 13, weight: .medium))
                }
                .keyboardShortcut(",")
                .accessibilityLabel("打开设置")
            }
        }
        .overlay(alignment: .center) {
            Link(destination: RoutinUsageApp.websiteURL) {
                Image(nsImage: NSImage(named: "PopoverColorBrandLogo") ?? NSApp.applicationIconImage)
                    .resizable()
                    .interpolation(.high)
                    .scaledToFit()
                    .padding(4)
                    .frame(width: 32, height: 32)
                    .background {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(Color.black.opacity(0.72))
                    }
                    .overlay {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .strokeBorder(
                                LinearGradient(
                                    colors: [
                                        .white.opacity(0.52),
                                        .white.opacity(0.14),
                                        .white.opacity(0.38)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1
                            )
                    }
                    .shadow(color: .white.opacity(0.16), radius: 5, x: 0, y: 0)
                    .shadow(color: .white.opacity(0.07), radius: 12, x: 0, y: 0)
                    .shadow(color: .black.opacity(0.26), radius: 2.5, y: 1.5)
            }
            .buttonStyle(.plain)
            .help("打开 MyToken 官网")
            .accessibilityLabel("打开 MyToken 官网")
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
            LazyVGrid(columns: cardColumns, spacing: 8) {
                ForEach(filteredPopoverKeyIDs, id: \.self) { id in
                    if let state = store.state(for: id) {
                        UsageRowView(
                            state: state,
                            density: settings.usageCardDensity,
                            actions: nil,
                            refreshCredential: {
                                Task { await refreshCredential(id) }
                            },
                            retryCredential: {
                                Task { await retryCredential(id) }
                            },
                            onShare: {
                                if let content = UsageShareContentBuilder.build(state: state) {
                                    UsageSharePanelController.shared.present(content: content)
                                }
                            }
                        )
                    }
                }
            }
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: settings.usageCardDensity)
            .padding(.horizontal, 12)
        }
    }

    var cardColumns: [GridItem] {
        settings.usageCardDensity == .compact
            ? [
                GridItem(.flexible(), spacing: 8),
                GridItem(.flexible())
            ]
            : [GridItem(.flexible())]
    }

    var popoverKeyIDs: [UUID] {
        let enabledIDs = Set(store.visibleKeyIDs)
        let visibility = settings.displayOrder.visible(enabledIDs: enabledIDs)
        return visibility.popoverIDs
    }

    var maxPopoverHeight: CGFloat {
        let visibleFrame = NSScreen.main?.visibleFrame
        let visibleHeight = visibleFrame?.height ?? 800
        return visibleHeight * 0.9
    }

    var usageListHeader: some View {
        HStack(alignment: .center, spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Text("账户用量")
                    .font(.title3.weight(.semibold))
                Text("\(filteredPopoverKeyIDs.count) 个 Key")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 8)

            ProviderFilterMenu(
                selection: $providerFilter,
                options: filterOptions
            )
            .frame(maxWidth: 210, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("账户用量，共 \(filteredPopoverKeyIDs.count) 个 Key")
    }

    @ViewBuilder
    var updateReleaseOverlay: some View {
        if let selectedUpdate {
            ZStack {
                Color.black.opacity(0.34)
                    .ignoresSafeArea()

                UpdateReleasePopup(
                    update: selectedUpdate,
                    status: updateStatus,
                    onCancel: { self.selectedUpdate = nil },
                    onBackground: { self.selectedUpdate = nil },
                    onInstall: { Task { await installAvailableUpdate() } }
                )
                .padding(.horizontal, 28)
            }
            .transition(.opacity)
        }
    }

    var filteredPopoverKeyIDs: [UUID] {
        guard let providerFilter else { return popoverKeyIDs }
        return popoverKeyIDs.filter {
            store.state(for: $0)?.configuration.providerID == providerFilter
        }
    }

    var visibleProviderIDs: [ProviderID] {
        let counts = providerCounts
        return ProviderID.allCases.filter { (counts[$0] ?? 0) > 0 }
    }

    var providerCounts: [ProviderID: Int] {
        popoverKeyIDs.reduce(into: [:]) { counts, keyID in
            guard let providerID = store.state(for: keyID)?.configuration.providerID else {
                return
            }
            counts[providerID, default: 0] += 1
        }
    }

    var filterOptions: [ProviderFilterOption] {
        let counts = providerCounts
        let allCount = counts.values.reduce(0, +)
        return ProviderFilterMenuModel.options(
            counts: counts,
            allCount: allCount,
            providerName: providerName
        )
    }

    func providerName(_ providerID: ProviderID) -> String {
        ProviderRegistry.builtInDescriptors.first(where: { $0.id == providerID })?.displayName
            ?? providerID.rawValue
    }

}

private extension UsagePopoverView {
    @ViewBuilder
    var footerStatuses: some View {
        VStack(alignment: .leading, spacing: 9) {
            if shouldShowFooterUpdateProgress,
               case let .downloading(progress) = updateStatus {
                updateProgressView(progress)
            }

            if case let .completed(version) = updateStatus {
                Label("更新完成，当前版本 \(version)", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.green)
                    .accessibilityElement(children: .combine)
            }
        }
    }

    private var shouldShowFooterUpdateProgress: Bool {
        guard selectedUpdate == nil else {
            return false
        }
        guard case .downloading = updateStatus else {
            return false
        }
        return true
    }

    var bottomBar: some View {
        HStack(spacing: 10) {
            GlassIconButton(
                action: { Task { await store.refreshAll() } },
                help: "刷新全部 Key"
            ) {
                Group {
                    if store.isRefreshing {
                        TimelineView(.animation) { timeline in
                            let angle = timeline.date.timeIntervalSinceReferenceDate
                                .truncatingRemainder(dividingBy: 1) * 360
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .rotationEffect(.degrees(angle))
                        }
                    } else if showRefreshSuccess {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                            .transition(.scale.combined(with: .opacity))
                    } else {
                        Image(systemName: "arrow.clockwise")
                            .transition(.opacity)
                    }
                }
                .font(.system(size: 13, weight: .medium))
            }
            .disabled(store.isRefreshing || store.visibleKeyIDs.isEmpty)
            .accessibilityLabel(store.isRefreshing ? "正在刷新全部 Key" : "刷新全部 Key")
            .onChange(of: store.isRefreshing) { _, isRefreshing in
                if !isRefreshing {
                    withAnimation(.spring(duration: 0.3)) {
                        showRefreshSuccess = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        withAnimation(.easeOut(duration: 0.35)) {
                            showRefreshSuccess = false
                        }
                    }
                }
            }

            Text(refreshDescription)
                .lineLimit(1)
            .font(.caption)
            .foregroundStyle(.secondary)
            .accessibilityLabel(refreshAccessibilityLabel)

            Spacer(minLength: 4)

            GlassIconButton(
                action: { NSApplication.shared.terminate(nil) },
                help: "退出 MyToken"
            ) {
                Image(systemName: "power")
                    .font(.system(size: 13, weight: .medium))
            }
            .keyboardShortcut("q")
            .accessibilityLabel("退出 MyToken")
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

extension AppUpdate: Identifiable {
    var id: String { version }
}

private struct PopoverScrollMetrics: Equatable {
    var contentHeight: CGFloat = 0
    var contentOffset: CGFloat = 0
}

private struct PopoverScrollMetricsKey: PreferenceKey {
    static var defaultValue = PopoverScrollMetrics()

    static func reduce(value: inout PopoverScrollMetrics, nextValue: () -> PopoverScrollMetrics) {
        value = nextValue()
    }
}

private struct ThinVerticalScrollIndicator: View {
    let metrics: PopoverScrollMetrics
    let viewportHeight: CGFloat

    var body: some View {
        let visibleHeight = min(max(viewportHeight, 1), max(metrics.contentHeight, 1))
        let scrollRange = max(metrics.contentHeight - visibleHeight, 0)
        let thumbHeight = max(32, visibleHeight * visibleHeight / max(metrics.contentHeight, 1))
        let travelRange = max(visibleHeight - thumbHeight, 0)
        let thumbOffset = scrollRange > 0
            ? min(max(metrics.contentOffset / scrollRange, 0), 1) * travelRange
            : 0

        Capsule()
            .fill(Color.primary.opacity(0.14))
            .frame(width: 3, height: visibleHeight)
            .overlay(alignment: .top) {
                Capsule()
                    .fill(Color.primary.opacity(0.36))
                    .frame(height: thumbHeight)
                    .offset(y: thumbOffset)
            }
            .clipShape(Capsule())
            .opacity(scrollRange > 0 ? 1 : 0)
            .animation(.easeOut(duration: 0.12), value: metrics)
            .accessibilityHidden(true)
    }
}

private struct UpdateReleasePopup: View {
    let update: AppUpdate
    let status: AppUpdateStatus
    let onCancel: () -> Void
    let onBackground: () -> Void
    let onInstall: () -> Void

    @State private var isCancelHovered = false
    @State private var isInstallHovered = false
    @State private var isBackgroundHovered = false

    private let titleColor = Color(red: 0.10, green: 0.11, blue: 0.13)
    private let secondaryGray = Color(red: 0.45, green: 0.48, blue: 0.52)
    private let notesBackground = Color(red: 0.96, green: 0.96, blue: 0.97)
    private let cancelFill = Color(red: 0.94, green: 0.94, blue: 0.96)
    private let installBlue = Color(red: 0.09, green: 0.36, blue: 0.83)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("发现新版本")
                        .font(.caption)
                        .foregroundStyle(secondaryGray)

                    Text("v\(update.version)")
                        .font(.system(size: 22, weight: .bold))
                        .monospacedDigit()
                        .foregroundStyle(titleColor)
                }

                Spacer(minLength: 8)

                Link(destination: update.releaseURL) {
                    Text("发布页")
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background {
                            Capsule()
                                .fill(Color.black.opacity(0.05))
                        }
                        .overlay {
                            Capsule()
                                .strokeBorder(Color.black.opacity(0.10))
                        }
                        .foregroundStyle(Color(red: 0.25, green: 0.27, blue: 0.30))
                }
                .buttonStyle(.plain)
                .onHover { hovering in
                    // Link 自带下划线悬停态之外，这里补一个指针提示可点击。
                    if hovering {
                        NSCursor.pointingHand.push()
                    } else {
                        NSCursor.pop()
                    }
                }
            }

            HStack(spacing: 6) {
                Image(systemName: "clock")
                    .font(.caption2)
                    .foregroundStyle(secondaryGray)

                Text("发布时间")
                    .font(.caption2)
                    .foregroundStyle(secondaryGray)

                Spacer(minLength: 8)

                Text(publishedText)
                    .font(.caption)
                    .foregroundStyle(secondaryGray)
                    .monospacedDigit()
            }

            Divider().overlay(Color.black.opacity(0.08))

            phaseContent
        }
        .padding(16)
        .frame(width: 320)
        .background {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(red: 0.99, green: 0.99, blue: 1.0))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(Color.black.opacity(0.10))
        }
        .shadow(color: .black.opacity(0.30), radius: 22, y: 10)
        .environment(\.colorScheme, .light)
    }

    @ViewBuilder
    private var phaseContent: some View {
        switch status {
        case .downloading(let progress):
            VStack(alignment: .leading, spacing: 8) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("正在下载更新")
                            .font(.callout.weight(.medium))
                            .foregroundStyle(titleColor)
                        Spacer(minLength: 8)
                        if let progress {
                            Text("\(Int((progress * 100).rounded()))%")
                                .font(.callout.weight(.semibold))
                                .monospacedDigit()
                                .foregroundStyle(installBlue)
                        }
                    }
                    ProgressView(value: progress ?? 0, total: 1)
                        .progressViewStyle(.linear)
                        .tint(installBlue)
                    Text("下载完成后将自动安装并重启 MyToken")
                        .font(.caption)
                        .foregroundStyle(secondaryGray)
                }

                HStack {
                    Spacer(minLength: 8)

                    Button {
                        onBackground()
                    } label: {
                        Text("后台更新")
                            .font(.callout.weight(.medium))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                    }
                    .buttonStyle(.plain)
                    .background {
                        Capsule()
                            .fill(isBackgroundHovered ? Color.black.opacity(0.09) : cancelFill)
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(Color.black.opacity(isBackgroundHovered ? 0.16 : 0.08))
                    }
                    .foregroundStyle(titleColor)
                    .onHover { isBackgroundHovered = $0 }
                    .help("隐藏更新窗口，在底部继续查看下载进度")
                    .accessibilityLabel("后台更新")
                    .accessibilityHint("隐藏更新窗口，在弹窗底部继续查看下载进度")
                }
            }

        case .completed(let version):
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                Text("更新完成，v\(version) 正在重启")
                    .font(.callout.weight(.medium))
                    .foregroundStyle(titleColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 8)

        case .failed(let message):
            VStack(alignment: .leading, spacing: 6) {
                Text(message)
                    .font(.callout)
                    .foregroundStyle(.red)
                Text("可关闭后稍后在设置里重试。")
                    .font(.caption)
                    .foregroundStyle(secondaryGray)
            }

        default:
            VStack(alignment: .leading, spacing: 6) {
                Text("更新日志")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(secondaryGray)

                ScrollView(.vertical, showsIndicators: false) {
                    UpdateNotesView(notes: update.notes)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(height: 120)
                .padding(10)
                .background {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(notesBackground)
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .strokeBorder(Color.black.opacity(0.06))
                }
            }

            HStack(spacing: 8) {
                Button {
                    onCancel()
                } label: {
                    Text("取消")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(isCancelHovered ? Color.black.opacity(0.09) : cancelFill)
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(Color.black.opacity(isCancelHovered ? 0.16 : 0.08))
                }
                .foregroundStyle(titleColor)
                .onHover { isCancelHovered = $0 }

                Button {
                    onInstall()
                } label: {
                    Text("立即更新")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(isInstallHovered ? installBlue.opacity(0.85) : installBlue)
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(installBlue.opacity(isInstallHovered ? 0.9 : 0.6))
                }
                .foregroundStyle(Color.white)
                .onHover { isInstallHovered = $0 }
            }
            .font(.callout.weight(.medium))
        }
    }

    private var publishedText: String {
        guard let publishedAt = update.publishedAt else { return "—" }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter.string(from: publishedAt)
    }
}

/// 弹窗内统一的玻璃图标按钮：整个 30×30 边框区域都是点击热区，
/// 悬停时底色和描边加深，按下时再加深，给出明确的可点击反馈。
private struct GlassIconButton<Label: View>: View {
    var action: () -> Void
    var help: String
    @ViewBuilder var label: () -> Label

    @State private var isHovered = false
    @State private var isPressed = false

    var body: some View {
        Button {
            action()
        } label: {
            label()
                .frame(width: 30, height: 30)
                .background {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.primary.opacity(isPressed ? 0.12 : isHovered ? 0.14 : 0.07))
                }
                .overlay {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(Color.primary.opacity(isHovered ? 0.26 : 0.12))
                }
                .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
        .animation(.easeInOut(duration: 0.15), value: isHovered)
        .onHover { isHovered = $0 }
        .help(help)
    }
}
