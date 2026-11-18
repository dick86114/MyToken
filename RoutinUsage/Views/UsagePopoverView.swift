import AppKit
import SwiftUI

@MainActor
struct UsagePopoverView: View {
    typealias InstallAvailableUpdate = @MainActor () async -> Void
    typealias CheckForUpdates = @MainActor () async -> Void
    typealias ShowVersionDetails = @MainActor () -> Void
    @Environment(\.colorScheme) private var colorScheme

    @Bindable var store: UsageStore
    @Bindable var settings: AppSettings
    let updateStatus: AppUpdateStatus
    let checkForUpdates: CheckForUpdates
    let installAvailableUpdate: InstallAvailableUpdate
    let refreshCredential: @MainActor (UUID) async -> Void
    let retryCredential: @MainActor (UUID) async -> Void
    let openSettings: @MainActor () -> Void
    let showVersionDetails: ShowVersionDetails

    @State private var providerFilter: ProviderID?
    @State private var selectedUpdate: AppUpdate?
    @State private var showRefreshSuccess = false
    @State private var isUpdateChipHovered = false
    @State private var isBrandLogoHovered = false

    init(
        store: UsageStore,
        settings: AppSettings,
        updateStatus: AppUpdateStatus = .idle,
        checkForUpdates: @escaping CheckForUpdates = {},
        installAvailableUpdate: @escaping InstallAvailableUpdate = {},
        refreshCredential: @escaping @MainActor (UUID) async -> Void = { _ in },
        retryCredential: @escaping @MainActor (UUID) async -> Void = { _ in },
        openSettings: @escaping @MainActor () -> Void = {},
        showVersionDetails: @escaping ShowVersionDetails = {}
    ) {
        self.store = store
        self.settings = settings
        self.updateStatus = updateStatus
        self.checkForUpdates = checkForUpdates
        self.installAvailableUpdate = installAvailableUpdate
        self.refreshCredential = refreshCredential
        self.retryCredential = retryCredential
        self.openSettings = openSettings
        self.showVersionDetails = showVersionDetails
    }

    var body: some View {
        VStack(spacing: 0) {
            toolbar
                .padding(.horizontal, 15)
                .padding(.top, 13)
                .padding(.bottom, 8)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    usageListHeader

                    usageList
                        .padding(.top, 2)
                        .padding(.bottom, 10)

                    footerStatuses
                        .padding(.horizontal, 16)
                        .padding(.bottom, 10)
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

            bottomBar
        }
        .frame(width: 440)
        .environment(\.menuBarColorRules, settings.menuBarColorRules)
        // 窗口偶尔高于内容时固定顶部对齐，避免内容悬浮居中。
        .frame(maxHeight: maxPopoverHeight, alignment: .top)
        .liquidGlassWindowBackground()
        .overlay {
            updateReleaseOverlay
        }
    }
}

private extension UsagePopoverView {
    private var popoverBrandLogo: some View {
        Image("PopoverColorBrandLogo")
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .frame(width: 25, height: 25)
    }

    var toolbar: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                HStack(spacing: 7) {
                    Button {
                        Task { await checkForUpdates() }
                    } label: {
                        popoverBrandLogo
                            .overlay {
                                if updateStatus == .checking {
                                    TimelineView(.animation) { timeline in
                                        let angle = timeline.date.timeIntervalSinceReferenceDate
                                            .truncatingRemainder(dividingBy: 1) * 360
                                        Circle()
                                            .trim(from: 0.08, to: 0.78)
                                            .stroke(
                                                CompactPopoverPalette.brand(colorScheme),
                                                style: StrokeStyle(lineWidth: 1.6, lineCap: .round)
                                            )
                                            .rotationEffect(.degrees(angle))
                                            .padding(-3)
                                    }
                                    .allowsHitTesting(false)
                                }
                            }
                            .scaleEffect(isBrandLogoHovered ? 1.05 : 1)
                            .animation(
                                .spring(response: 0.3, dampingFraction: 0.9),
                                value: isBrandLogoHovered
                            )
                    }
                    .buttonStyle(.plain)
                    .disabled(updateStatus == .checking || isDownloadingUpdate)
                    .onHover { isBrandLogoHovered = $0 }
                    .help(checkForUpdatesHelp)
                    .accessibilityLabel(checkForUpdatesAccessibilityLabel)

                    Link(destination: RoutinUsageApp.websiteURL) {
                        Text("MyToken")
                            .font(.system(size: 13, weight: .semibold))
                            .tracking(-0.01)
                            .foregroundStyle(.primary)
                    }
                    .buttonStyle(.plain)
                    .help("打开 MyToken 官网")
                    .accessibilityLabel("打开 MyToken 官网")
                }

                versionOrUpdateControl

                Spacer(minLength: 8)

                HStack(spacing: 6) {
                    UsageCardDensitySegmentedControl(
                        selection: $settings.usageCardDensity
                    )

                    GlassIconButton(
                        action: { openSettings() },
                        help: "设置"
                    ) {
                        Image(systemName: "gearshape")
                            .font(.system(size: 14, weight: .medium))
                    }
                    .keyboardShortcut(",")
                    .accessibilityLabel("打开设置")
                }
            }

            if case let .failed(message) = updateStatus {
                // 失败只会在手动检测后出现，这里直接把具体原因交给用户。
                Label(message, systemImage: "exclamationmark.triangle.fill")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(CompactPopoverPalette.criticalColor)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .help(message)
                    .accessibilityElement(children: .combine)
            }
        }
    }

    @ViewBuilder
    private var versionOrUpdateControl: some View {
        if case let .available(update) = updateStatus {
            Button {
                selectedUpdate = update
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 11, weight: .semibold))
                    Text("新版本")
                        .font(.system(size: 11, weight: .semibold))
                    Text(update.version)
                        .font(.system(size: 10, weight: .semibold, design: .monospaced))
                        .monospacedDigit()
                }
                .foregroundStyle(CompactPopoverPalette.brand(colorScheme))
                .padding(.horizontal, 7)
                .frame(height: 22)
                .background {
                    Capsule(style: .continuous)
                        .fill(CompactPopoverPalette.brand(colorScheme).opacity(isUpdateChipHovered ? 0.16 : 0.10))
                }
                .overlay {
                    Capsule(style: .continuous)
                        .strokeBorder(
                            CompactPopoverPalette.brand(colorScheme).opacity(isUpdateChipHovered ? 0.28 : 0.16),
                            lineWidth: 1
                        )
                }
                .contentShape(Capsule(style: .continuous))
            }
            .buttonStyle(.plain)
            .onHover { isUpdateChipHovered = $0 }
            .animation(.easeOut(duration: 0.14), value: isUpdateChipHovered)
            .help("查看 v\(update.version) 更新详情")
            .accessibilityLabel("发现新版本 v\(update.version)，查看更新详情")
        } else {
            Button {
                showVersionDetails()
            } label: {
                Text("v\(RoutinUsageApp.currentVersion)")
                    .font(.system(size: 10, weight: .medium, design: .monospaced))
                    .monospacedDigit()
                    .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                    .padding(.horizontal, 3)
                    .frame(height: 22)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .help("查看当前版本更新内容")
            .accessibilityLabel("当前版本 v\(RoutinUsageApp.currentVersion)，查看版本更新内容")
        }
    }

    var checkForUpdatesHelp: String {
        switch updateStatus {
        case .checking:
            return "正在检查更新"
        case .downloading:
            return "正在下载更新"
        case .failed:
            return "检查更新（上次检查失败）"
        case .idle, .available, .completed:
            return "检查更新"
        }
    }

    var checkForUpdatesAccessibilityLabel: String {
        switch updateStatus {
        case .checking:
            return "正在检查更新"
        case .downloading:
            return "正在下载更新"
        case .failed:
            return "检查更新，上次检查失败，点击重试"
        case .idle, .available, .completed:
            return "检查更新"
        }
    }

    var isDownloadingUpdate: Bool {
        if case .downloading = updateStatus {
            return true
        }
        return false
    }

    @ViewBuilder
    var usageList: some View {
        if store.visibleKeyIDs.isEmpty {
            VStack(spacing: 8) {
                Image(systemName: "key.slash")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.secondary)
                Text(store.orderedKeyIDs.isEmpty ? "尚未配置 Key" : "没有启用的 Key")
                    .font(.system(size: 14, weight: .semibold))
                Text(store.orderedKeyIDs.isEmpty ? "请在设置中添加一个 plan Key" : "请在设置中启用至少一个 Key")
                    .font(.system(size: 12))
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
        switch settings.usageCardDensity {
        case .compact:
            LazyVGrid(columns: [GridItem(.flexible())], spacing: 7) {
                compactCardViews
            }
        case .full:
            LazyVGrid(columns: [GridItem(.flexible())], spacing: 7) {
                fullCardViews
            }
        }
    }

    @ViewBuilder
    private var compactCardViews: some View {
        ForEach(filteredPopoverKeyIDs, id: \.self) { id in
            usageRow(for: id, density: .compact)
        }
    }

    @ViewBuilder
    private var fullCardViews: some View {
        ForEach(Array(filteredPopoverKeyIDs.enumerated()), id: \.element) { index, id in
            usageRow(for: id, density: .full)
        }
    }

    @ViewBuilder
    private func usageRow(
        for id: UUID,
        density: UsageCardDensity
    ) -> some View {
        if let state = store.state(for: id) {
            UsageRowView(
                state: state,
                density: density,
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
            VStack(alignment: .leading, spacing: 5) {
                Text("账户用量")
                    .font(.system(size: 23, weight: .bold))
                    .tracking(-0.02)
                    .foregroundStyle(.primary)

                HStack(spacing: 5) {
                    Circle()
                        .fill(accountStatusColor)
                        .frame(width: 5, height: 5)
                        .overlay {
                            Circle()
                                .stroke(accountStatusColor.opacity(0.16), lineWidth: 2)
                        }

                    Text("\(filteredPopoverKeyIDs.count) 个账户")
                        .monospacedDigit()

                    if accountAttentionCount > 0 {
                        Text("· \(accountAttentionCount) 个需注意")
                            .monospacedDigit()
                            .foregroundStyle(CompactPopoverPalette.warningColor)
                    }

                    Text("· \(shortSyncDescription)")
                        .monospacedDigit()
                }
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
            }

            Spacer(minLength: 8)

            ProviderFilterMenu(
                selection: $providerFilter,
                options: filterOptions
            )
            .frame(maxWidth: 180, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .padding(.bottom, 10)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "账户用量，共 \(filteredPopoverKeyIDs.count) 个账户"
                + (accountAttentionCount > 0 ? "，\(accountAttentionCount) 个需注意" : "")
                + "，\(shortSyncDescription)"
        )
    }

    var accountStatusColor: Color {
        if filteredPopoverKeyIDs.isEmpty {
            return Color.secondary.opacity(0.55)
        }
        return accountAttentionCount > 0
            ? CompactPopoverPalette.warningColor
            : CompactPopoverPalette.positive(colorScheme)
    }

    var accountAttentionCount: Int {
        filteredPopoverKeyIDs.reduce(into: 0) { count, id in
            guard let state = store.state(for: id) else { return }
            let unhealthyMetric = state.snapshot?.normalizedMetrics.contains { metric in
                switch metric.healthState {
                case .warning, .critical, .unavailable:
                    return true
                case .normal, .stale, .unknown:
                    return false
                }
            } ?? false
            if state.error != nil || state.isStale || unhealthyMetric {
                count += 1
            }
        }
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
                    .font(.system(size: 12))
                    .foregroundStyle(CompactPopoverPalette.positive(colorScheme))
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
        VStack(spacing: 0) {
            Rectangle()
            .fill(CompactPopoverPalette.cardStroke(colorScheme).opacity(0.72))
            .frame(height: 1)

            HStack(spacing: 8) {
                HStack(spacing: 6) {
                    Circle()
                        .fill(footerStatusColor)
                        .frame(width: 5, height: 5)
                        .overlay {
                            Circle()
                                .stroke(footerStatusColor.opacity(0.16), lineWidth: 2)
                        }

                    Text(refreshDescription)
                        .lineLimit(1)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(CompactPopoverPalette.subtitle(colorScheme))
                        .monospacedDigit()
                        .allowsHitTesting(false)
                        .accessibilityLabel(refreshAccessibilityLabel)
                }

                Spacer(minLength: 8)

                PopoverFooterActionButton(
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
                                .foregroundStyle(CompactPopoverPalette.positive(colorScheme))
                                .transition(.scale.combined(with: .opacity))
                        } else {
                            Image(systemName: "arrow.clockwise")
                                .transition(.opacity)
                        }
                    }
                    .font(.system(size: 14, weight: .medium))
                }
                .disabled(store.isRefreshing || store.visibleKeyIDs.isEmpty)
                .accessibilityLabel(store.isRefreshing ? "正在刷新全部 Key" : "刷新全部 Key")
                .onChange(of: store.isRefreshing) { _, isRefreshing in
                    if !isRefreshing {
                        withAnimation(.spring(response: 0.3, dampingFraction: 1)) {
                            showRefreshSuccess = true
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                            withAnimation(.easeOut(duration: 0.3)) {
                                showRefreshSuccess = false
                            }
                        }
                    }
                }

                Rectangle()
                    .fill(CompactPopoverPalette.cardStroke(colorScheme))
                    .frame(width: 1, height: 15)
                    .padding(.horizontal, 2)

                PopoverFooterActionButton(
                    action: { NSApplication.shared.terminate(nil) },
                    help: "退出 MyToken"
                ) {
                    Image(systemName: "power")
                        .font(.system(size: 13, weight: .medium))
                }
                .keyboardShortcut("q")
                .accessibilityLabel("退出 MyToken")
            }
            .padding(.horizontal, 15)
            .frame(height: 48)
        }
    }

    var footerStatusColor: Color {
        if store.isRefreshing {
            return CompactPopoverPalette.brand(colorScheme)
        }
        return latestRefreshDate == nil
            ? Color.secondary.opacity(0.35)
            : CompactPopoverPalette.positive(colorScheme)
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
        .font(.system(size: 12))
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
            return "正在刷新用量"
        }
        guard let latestRefreshDate else {
            return "尚未刷新"
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        let calendar = Calendar.current
        if calendar.isDateInToday(latestRefreshDate) {
            formatter.dateFormat = "HH:mm"
            return "今天 \(formatter.string(from: latestRefreshDate)) 已同步"
        }
        formatter.dateFormat = "MM-dd HH:mm"
        return "\(formatter.string(from: latestRefreshDate)) 已同步"
    }

    var shortSyncDescription: String {
        if store.isRefreshing {
            return "正在同步"
        }
        guard let latestRefreshDate else {
            return "尚未同步"
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = .current
        if Calendar.current.isDateInToday(latestRefreshDate) {
            formatter.dateFormat = "HH:mm"
            return "\(formatter.string(from: latestRefreshDate)) 同步"
        }
        formatter.dateFormat = "MM-dd HH:mm"
        return "\(formatter.string(from: latestRefreshDate)) 同步"
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

/// 更新弹窗：玻璃模态、品牌蓝版本徽章与内嵌日志卡，深浅色随系统切换。
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


    private var isDark: Bool { colorScheme == .dark }
    private var accent: Color { CompactPopoverPalette.brand(colorScheme) }
    private var positive: Color { CompactPopoverPalette.positive(colorScheme) }
    private var mutedText: Color { CompactPopoverPalette.subtitle(colorScheme) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header
            publishedRow
            phaseContent
        }
        .padding(18)
        .frame(width: 360)
        .liquidGlassModalSurface()
    }


    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            updateIcon

            VStack(alignment: .leading, spacing: 5) {
                Text("发现新版本")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background {
                        Capsule()
                            .fill(accent.opacity(0.12))
                    }
                    .overlay {
                        Capsule()
                            .strokeBorder(accent.opacity(0.28), lineWidth: 1)
                    }

                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text("v\(update.version)")
                        .font(.system(size: 20, weight: .bold, design: .monospaced))
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
            .font(.system(size: 20, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 48, height: 48)
            .background {
                RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                    .fill(accent)
            }
            .accessibilityHidden(true)
    }

    private var releaseLink: some View {
        Link(destination: update.releaseURL) {
            HStack(spacing: 5) {
                Text("发布页")
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 12, weight: .semibold))
            }
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background {
                RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                    .fill(CompactPopoverPalette.chipFill(hovered: isReleaseLinkHovered, colorScheme))
            }
            .overlay {
                RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                    .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
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
                    .foregroundStyle(positive)
                Text("更新完成，v\(version) 正在重启")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 8)

        case .failed(let message):
            VStack(alignment: .leading, spacing: 6) {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(CompactPopoverPalette.criticalColor)
                Text("可关闭后稍后在设置里重试。")
                    .font(.system(size: 12))
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
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.primary)
                Spacer(minLength: 8)
                if let progress {
                    Text("\(Int((progress * 100).rounded()))%")
                        .font(.system(size: 14, weight: .semibold, design: .monospaced))
                        .monospacedDigit()
                        .foregroundStyle(accent)
                }
            }

            ProgressView(value: progress ?? 0, total: 1)
                .progressViewStyle(.linear)
                .tint(accent)

            Text("下载完成后将自动安装并重启 MyToken")
                .font(.system(size: 12))
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
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                .fill(CompactPopoverPalette.surface(.metric, colorScheme))
        }
        .overlay {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                .strokeBorder(
                    CompactPopoverPalette.cardStroke(colorScheme),
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
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                .fill(accent.opacity(isInstallHovered ? 0.88 : 1))
        }
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
                .font(.system(size: 14, weight: .semibold))
                .frame(maxWidth: fillWidth ? .infinity : nil)
                .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                .fill(CompactPopoverPalette.chipFill(hovered: isLaterHovered, colorScheme))
        }
        .overlay {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                .strokeBorder(
                    CompactPopoverPalette.cardStroke(colorScheme),
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

/// 底栏操作保持无边框，只在悬停时出现轻量圆形反馈。
private struct PopoverFooterActionButton<Label: View>: View {
    var action: () -> Void
    var help: String
    @ViewBuilder var label: () -> Label

    @State private var isHovered = false

    var body: some View {
        Button(action: action) {
            label()
                .foregroundStyle(isHovered ? Color.primary : Color.secondary)
                .frame(width: 28, height: 28)
                .background {
                    Circle()
                        .fill(Color.primary.opacity(isHovered ? 0.08 : 0.001))
                }
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .onHover { isHovered = $0 }
        .animation(.easeOut(duration: 0.14), value: isHovered)
        .help(help)
    }
}

/// 弹窗内统一的实色图标按钮：整个 30×30 边框区域都是点击热区，
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
                        .fill(Color.primary.opacity(isPressed ? 0.10 : isHovered ? 0.075 : 0.05))
                }
                .overlay {
                    Circle()
                        .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
                }
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .animation(.easeInOut(duration: 0.15), value: isHovered)
        .onHover { isHovered = $0 }
        .help(help)
    }
}
