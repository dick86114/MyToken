import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    @Environment(\.colorScheme) private var colorScheme

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
                .padding(.horizontal, 14)
                .padding(.top, 14)
                .padding(.bottom, 8)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    usageListHeader

                    usageList
                        .padding(.top, 2)
                        .padding(.bottom, 8)

                    footerStatuses
                        .padding(.horizontal, 16)
                        .padding(.bottom, 8)

                    Color.clear
                        .frame(height: 58)
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
                PopoverScrollIndicatorHost()
                    .frame(width: 4)
                    .padding(.trailing, 1)
            }
            .overlay(alignment: .bottom) {
                bottomBar
                    .padding(.horizontal, 14)
                    .padding(.bottom, 12)
            }
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
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
                    .background {
                        Capsule()
                            .fill(CompactPopoverPalette.chipFill(hovered: isVersionLinkHovered, colorScheme))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(
                                Color.white.opacity(colorScheme == .dark ? 0.12 : 0.80),
                                lineWidth: 1
                            )
                    }
                    .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.20 : 0.04), radius: 2, y: 1)
                }
                .buttonStyle(.plain)
                .onHover { isVersionLinkHovered = $0 }
                .animation(.easeInOut(duration: 0.15), value: isVersionLinkHovered)
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
                    .frame(width: 36, height: 36)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .background {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.black.opacity(0.88))
                    }
                    .overlay {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(Color.white.opacity(0.40), lineWidth: 1)
                    }
                    .shadow(color: Color.black.opacity(0.25), radius: 8, y: 3)
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
            densityListContent
            .id(settings.usageCardDensity)
            .transaction { $0.animation = nil }
            .padding(.horizontal, 12)
        }
    }

    @ViewBuilder
    private var densityListContent: some View {
        if settings.usageCardDensity == .compact {
            LazyVGrid(columns: [GridItem(.flexible())], spacing: 14) {
                cardViews
            }
        } else {
            LazyVGrid(columns: [GridItem(.flexible())], spacing: 8) {
                cardViews
            }
        }
    }

    @ViewBuilder
    private var cardViews: some View {
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
        HStack(alignment: .bottom, spacing: 10) {
            HStack(alignment: .center, spacing: 8) {
                Text("账户用量")
                    .font(.system(size: 23, weight: .bold))
                    .foregroundStyle(.primary)

                Text("\(filteredPopoverKeyIDs.count) 个 Key")
                    .font(.system(size: 11, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(CompactPopoverPalette.badgeGreen(colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background {
                        Capsule()
                            .fill(CompactPopoverPalette.badgeGreen(colorScheme).opacity(0.10))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(CompactPopoverPalette.badgeGreen(colorScheme).opacity(colorScheme == .dark ? 0.30 : 0.20), lineWidth: 1)
                    }
            }

            Spacer(minLength: 8)

            ProviderFilterMenu(
                selection: $providerFilter,
                options: filterOptions
            )
            .frame(maxWidth: 210, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.top, 4)
        .padding(.bottom, 12)
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
        .overlay {
            Text(refreshDescription)
                .lineLimit(1)
                .font(.system(size: 11, design: .monospaced))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                .allowsHitTesting(false)
                .accessibilityLabel(refreshAccessibilityLabel)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background {
            Capsule(style: .continuous)
                .fill(.thinMaterial)
                .overlay {
                    Capsule(style: .continuous)
                        .fill(
                            colorScheme == .dark
                                ? CompactPopoverPalette.darkCanvas.opacity(0.45)
                                : .clear
                        )
                }
        }
        .overlay {
            Capsule(style: .continuous)
                .strokeBorder(
                    Color.white.opacity(colorScheme == .dark ? 0.14 : 0.78),
                    lineWidth: 1
                )
                .allowsHitTesting(false)
        }
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.28 : 0.12), radius: 16, y: 6)
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

/// 滚动指标状态只留在指示器内部，滚动帧不再触发整个弹窗重算。
private struct PopoverScrollIndicatorHost: View {
    @State private var metrics = PopoverScrollMetrics()

    var body: some View {
        GeometryReader { geometry in
            ThinVerticalScrollIndicator(
                metrics: metrics,
                viewportHeight: geometry.size.height
            )
        }
        .onPreferenceChange(PopoverScrollMetricsKey.self) { metrics = $0 }
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

/// 更新弹窗：玻璃卡片、emerald 版本徽章与内嵌日志卡，深浅色随系统切换。
private struct UpdateReleasePopup: View {
    let update: AppUpdate
    let status: AppUpdateStatus
    let onCancel: () -> Void
    let onBackground: () -> Void
    let onInstall: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @State private var isReleaseLinkHovered = false
    @State private var isLaterHovered = false
    @State private var isInstallHovered = false

    private let actionBlue = Color(red: 0.15, green: 0.42, blue: 0.95)

    private var isDark: Bool { colorScheme == .dark }
    private var emerald: Color { CompactPopoverPalette.badgeGreen(colorScheme) }
    private var mutedText: Color { CompactPopoverPalette.subtitle(colorScheme) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header
            publishedRow
            phaseContent
        }
        .padding(18)
        .frame(width: 360)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(popupSurface)
        }
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.ultraThinMaterial)
        }
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(Color.white.opacity(isDark ? 0.14 : 0.85), lineWidth: 1)
                .allowsHitTesting(false)
        }
        .shadow(color: .black.opacity(isDark ? 0.45 : 0.30), radius: 22, y: 10)
    }

    private var popupSurface: Color {
        isDark
            ? Color(red: 0.071, green: 0.090, blue: 0.133).opacity(0.88)
            : Color.white.opacity(0.88)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            updateIcon

            VStack(alignment: .leading, spacing: 5) {
                Text("发现新版本")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(emerald)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background {
                        Capsule()
                            .fill(emerald.opacity(0.12))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(emerald.opacity(0.30), lineWidth: 1)
                    }

                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text("v\(update.version)")
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(.primary)

                    if update.version != RoutinUsageApp.currentVersion {
                        Text("v\(RoutinUsageApp.currentVersion)")
                            .font(.system(size: 12, weight: .medium, design: .monospaced))
                            .strikethrough()
                            .foregroundStyle(mutedText)
                    }
                }
            }

            Spacer(minLength: 8)

            releaseLink
        }
    }

    private var updateIcon: some View {
        Image(systemName: "arrow.up")
            .font(.system(size: 21, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 48, height: 48)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 0.36, green: 0.86, blue: 0.60),
                                Color(red: 0.05, green: 0.76, blue: 0.52)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            }
            .shadow(color: Color.green.opacity(0.35), radius: 8, y: 3)
            .accessibilityHidden(true)
    }

    private var releaseLink: some View {
        Link(destination: update.releaseURL) {
            HStack(spacing: 5) {
                Text("发布页")
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 11, weight: .semibold))
            }
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(CompactPopoverPalette.chipFill(hovered: isReleaseLinkHovered, colorScheme))
            }
            .overlay {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .strokeBorder(Color.white.opacity(isDark ? 0.14 : 0.85), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .onHover { isReleaseLinkHovered = $0 }
        .help("打开发布页")
        .accessibilityLabel("打开 v\(update.version) 发布页")
    }

    private var publishedRow: some View {
        HStack(spacing: 6) {
            Image(systemName: "clock")
                .font(.system(size: 12))
            Text("发布时间：")
            Text(publishedText)
                .monospacedDigit()
        }
        .font(.system(size: 12))
        .foregroundStyle(mutedText)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var phaseContent: some View {
        switch status {
        case .downloading(let progress):
            downloadingContent(progress)

        case .completed(let version):
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                Text("更新完成，v\(version) 正在重启")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 8)

        case .failed(let message):
            VStack(alignment: .leading, spacing: 6) {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(.red)
                Text("可关闭后稍后在设置里重试。")
                    .font(.system(size: 11))
                    .foregroundStyle(mutedText)
            }

        default:
            VStack(alignment: .leading, spacing: 12) {
                notesCard
                HStack(spacing: 10) {
                    laterButton
                    installButton
                }
            }
        }
    }

    private func downloadingContent(_ progress: Double?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("正在下载更新")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.primary)
                Spacer(minLength: 8)
                if let progress {
                    Text("\(Int((progress * 100).rounded()))%")
                        .font(.system(size: 13, weight: .semibold, design: .monospaced))
                        .monospacedDigit()
                        .foregroundStyle(actionBlue)
                }
            }

            ProgressView(value: progress ?? 0, total: 1)
                .progressViewStyle(.linear)
                .tint(actionBlue)

            Text("下载完成后将自动安装并重启 MyToken")
                .font(.system(size: 11))
                .foregroundStyle(mutedText)

            HStack {
                Spacer(minLength: 8)
                ghostButton(title: "后台更新", action: onBackground)
                    .help("隐藏更新窗口，在底部继续查看下载进度")
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(
            progress.map { "正在下载更新，已完成 \(Int($0 * 100))%" } ?? "正在下载更新"
        )
    }

    private var notesCard: some View {
        ViewThatFits(in: .vertical) {
            UpdateNotesView(notes: update.notes)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)

            ScrollView(.vertical, showsIndicators: false) {
                UpdateNotesView(notes: update.notes)
                    .lineSpacing(3)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(height: 150)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(
                    isDark
                        ? Color(red: 0.039, green: 0.051, blue: 0.078).opacity(0.60)
                        : Color.white
                )
        }
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(
                    isDark ? Color.white.opacity(0.08) : Color.black.opacity(0.06),
                    lineWidth: 1
                )
                .allowsHitTesting(false)
        }
    }

    private var laterButton: some View {
        ghostButton(title: "稍后更新", action: onCancel, fillWidth: true)
    }

    private var installButton: some View {
        Button {
            onInstall()
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "arrow.down")
                Text("立即更新")
            }
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(actionBlue.opacity(isInstallHovered ? 0.88 : 1))
        }
        .shadow(color: actionBlue.opacity(0.35), radius: 8, y: 2)
        .onHover { isInstallHovered = $0 }
        .accessibilityLabel("立即更新到 v\(update.version)")
    }

    private func ghostButton(
        title: String,
        action: @escaping () -> Void,
        fillWidth: Bool = false
    ) -> some View {
        Button {
            action()
        } label: {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .frame(maxWidth: fillWidth ? .infinity : nil)
                .foregroundStyle(
                    isDark
                        ? Color.white.opacity(isLaterHovered ? 1 : 0.90)
                        : Color(red: 0.11, green: 0.11, blue: 0.12)
                )
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(
                    isDark
                        ? Color.white.opacity(isLaterHovered ? 0.10 : 0.06)
                        : Color.white.opacity(isLaterHovered ? 0.97 : 0.92)
                )
        }
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(
                    isDark ? Color.white.opacity(0.12) : Color.black.opacity(0.08),
                    lineWidth: 1
                )
        }
        .onHover { isLaterHovered = $0 }
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
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button {
            action()
        } label: {
            label()
                .frame(width: 28, height: 28)
                .background {
                    Circle()
                        .fill(Color.primary.opacity(isPressed ? 0.10 : isHovered ? 0.08 : 0.035))
                }
                .overlay {
                    Circle()
                        .strokeBorder(
                            Color.white.opacity(
                                colorScheme == .dark
                                    ? (isHovered ? 0.22 : 0.12)
                                    : (isHovered ? 0.80 : 0.55)
                            ),
                            lineWidth: 1
                        )
                }
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .animation(.easeInOut(duration: 0.15), value: isHovered)
        .onHover { isHovered = $0 }
        .help(help)
    }
}
