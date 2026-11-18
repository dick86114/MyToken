import SwiftUI

struct HelpUpdateView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Bindable var environment: AppEnvironment
    @State private var showingReleaseHistory = false
    @State private var showsAdvancedSettings = false
    @State private var githubTokenInput = ""
    @State private var githubTokenMask: String?
    @State private var githubTokenMessage: String?
    @State private var githubTokenMessageTone: SettingsNoticeTone = .info
    @State private var isStarHovered = false

    var body: some View {
        SettingsPageScroll {
            SettingsPageHeader(
                title: "帮助与更新",
                subtitle: "版本维护、更新通道与问题反馈"
            )

            currentVersionSection
            updateSection
            advancedSection
            feedbackSection
        }
        .task {
            await environment.loadReleaseHistoryIfNeeded()
            githubTokenMask = environment.gitHubTokenStore.tokenMask()
        }
        .sheet(isPresented: $showingReleaseHistory) {
            ReleaseHistorySheet(state: environment.releaseHistoryState) {
                await environment.loadReleaseHistoryIfNeeded(force: true)
            }
        }
    }

    private var currentVersionSection: some View {
        SettingsGroup(
            title: "当前版本"
        ) {
            HStack(spacing: 6) {
                Text("MyToken")
                    .font(SettingsTypography.body.weight(.medium))

                Text(RoutinUsageApp.currentVersion)
                    .font(SettingsTypography.numeric.weight(.semibold))
                    .monospacedDigit()

                Link(destination: RoutinUsageApp.githubURL) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 10.5, weight: .semibold))
                        .foregroundStyle(CompactPopoverPalette.warningColor)
                        .frame(width: 22, height: 22)
                        .background {
                            Circle()
                                .fill(
                                    CompactPopoverPalette.warningColor.opacity(
                                        isStarHovered ? 0.14 : 0
                                    )
                                )
                        }
                        .scaleEffect(
                            isStarHovered && !reduceMotion ? 1.18 : 1
                        )
                        .rotationEffect(
                            .degrees(isStarHovered && !reduceMotion ? -8 : 0)
                        )
                        .shadow(
                            color: CompactPopoverPalette.warningColor.opacity(
                                isStarHovered ? 0.28 : 0
                            ),
                            radius: isStarHovered ? 6 : 0
                        )
                        .contentShape(Circle())
                }
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.92))
                .onHover { isStarHovered = $0 }
                .animation(
                    reduceMotion
                        ? nil
                        : .spring(response: 0.28, dampingFraction: 0.72),
                    value: isStarHovered
                )
                .help("在 GitHub 为 MyToken 点 Star")
                .accessibilityLabel("在 GitHub 为 MyToken 点 Star")

                Spacer(minLength: 12)
            }
            .padding(.vertical, 7)

            SettingsRowDivider()

            VStack(alignment: .leading, spacing: 10) {
                SettingsSectionHeader(
                    title: "更新内容",
                    systemImage: "doc.text"
                )

                currentReleaseNotes

                Button("查看历史版本") {
                    showingReleaseHistory = true
                }
                .liquidGlassButton()
                .accessibilityLabel("查看历史版本更新日志")
            }
            .padding(.top, 12)
            .padding(.bottom, 7)
        }
    }

    @ViewBuilder
    private var currentReleaseNotes: some View {
        switch environment.releaseHistoryState {
        case .idle, .loading:
            HStack(spacing: 8) {
                ProgressView()
                    .controlSize(.small)
                Text("正在加载更新日志")
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
        case let .failed(message):
            VStack(alignment: .leading, spacing: 10) {
                Text(message)
                    .foregroundStyle(CompactPopoverPalette.criticalColor)

                Button("重试") {
                    Task { await environment.loadReleaseHistoryIfNeeded(force: true) }
                }
                .liquidGlassButton()
            }
        case let .loaded(releases):
            if let current = releases.first(where: { $0.version == RoutinUsageApp.currentVersion }) {
                CurrentReleaseNotesView(notes: current.notes)
            } else {
                Text("此版本未提供更新日志")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var advancedSection: some View {
        SettingsGroup(
            title: "高级",
            subtitle: "仅当更新检查频繁受限或网络环境特殊时使用。"
        ) {
            Button {
                withAnimation(
                    reduceMotion
                        ? nil
                        : .spring(response: 0.3, dampingFraction: 0.9)
                ) {
                    showsAdvancedSettings.toggle()
                }
            } label: {
                HStack(spacing: 12) {
                    SettingsIconBadge(
                        systemImage: "key.horizontal",
                        tint: Color.accentColor
                    )

                    VStack(alignment: .leading, spacing: 3) {
                        Text("GitHub 令牌")
                            .font(SettingsTypography.rowTitle)
                        Text("公开仓库只读令牌即可；配置后可提高更新检查频率。")
                            .font(SettingsTypography.secondary)
                            .foregroundStyle(.secondary)
                    }

                    Spacer(minLength: 12)

                    Image(systemName: "chevron.down")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .rotationEffect(.degrees(showsAdvancedSettings ? 180 : 0))
                }
                .padding(.vertical, 9)
                .contentShape(Rectangle())
            }
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.995))
            .accessibilityLabel("GitHub 令牌高级设置")
            .accessibilityValue(showsAdvancedSettings ? "已展开" : "已收起")

            if showsAdvancedSettings {
                SettingsRowDivider()
                    .padding(.top, 2)

                githubTokenContent
                    .padding(.top, 10)
                    .padding(.bottom, 5)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    private var githubTokenContent: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                if let githubTokenMask {
                    Text(githubTokenMask)
                        .font(.system(size: 12.5, design: .monospaced))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .accessibilityLabel("已保存 GitHub 令牌")

                    Spacer(minLength: 12)

                    Button("移除") {
                        removeGitHubToken()
                    }
                    .liquidGlassButton()
                    .accessibilityLabel("移除 GitHub 令牌")
                } else {
                    SettingsLabeledTextField(
                        text: $githubTokenInput,
                        prompt: "ghp_ 或 github_pat_ 令牌",
                        isSecret: true,
                        canReveal: !githubTokenInput.isEmpty
                    )
                    .accessibilityLabel("GitHub 令牌输入框")

                    Button("保存") {
                        saveGitHubToken()
                    }
                    .liquidGlassButton(prominent: true)
                    .disabled(githubTokenInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityLabel("保存 GitHub 令牌")
                }
            }
            .padding(.vertical, 7)

            if let githubTokenMessage {
                SettingsNotice(
                    tone: githubTokenMessageTone,
                    message: githubTokenMessage
                )
                .padding(.bottom, 7)
            }
        }
    }

    private func saveGitHubToken() {
        do {
            try environment.gitHubTokenStore.saveToken(githubTokenInput)
            githubTokenInput = ""
            githubTokenMask = environment.gitHubTokenStore.tokenMask()
            githubTokenMessage = "GitHub 令牌已保存到应用本地存储"
            githubTokenMessageTone = .success
        } catch {
            githubTokenMessage = "GitHub 令牌保存失败，请重试"
            githubTokenMessageTone = .error
        }
    }

    private func removeGitHubToken() {
        environment.gitHubTokenStore.removeToken()
        githubTokenMask = nil
        githubTokenMessage = nil
        githubTokenMessageTone = .info
    }

    private var updateSection: some View {
        @Bindable var settings = environment.settings
        return SettingsGroup(
            title: "应用更新",
            subtitle: "选择检查与下载更新时使用的网络通道。"
        ) {
            VStack(alignment: .leading, spacing: 10) {
                SettingsSegmentedControl(
                    options: UpdateChannel.allCases,
                    selection: $settings.updateChannel,
                    title: { channel in
                        channel == .direct ? "GitHub 直连" : "CDN 加速"
                    },
                    accessibilityLabel: "更新通道"
                )

                if environment.settings.updateChannel == .cdn {
                    Picker("CDN 源", selection: $settings.updateCDNBase) {
                        ForEach(AppSettings.cdnBases, id: \.self) { base in
                            Text(base.replacingOccurrences(of: "https://", with: "")).tag(base)
                        }
                    }
                    .accessibilityLabel("CDN 加速源")

                    SettingsHintText(text: "大陆网络建议选择 CDN 加速；若某镜像不可用可切换其他源。")
                }

                SettingsRowDivider()
                    .padding(.vertical, 2)

                updateStatusContent
            }
            .padding(.bottom, 7)
        }
    }

    @ViewBuilder
    private var updateStatusContent: some View {
        switch environment.updateStatus {
        case .idle:
            HStack(spacing: 12) {
                Button("检查更新") {
                    Task { await environment.checkForUpdates() }
                }
                .liquidGlassButton()
                .accessibilityLabel("检查更新")

                Spacer(minLength: 0)
            }
        case .checking:
            HStack {
                Text("正在检查更新")
                    .font(SettingsTypography.body)
                Spacer(minLength: 8)
                ProgressView()
                    .controlSize(.small)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("正在检查更新")
        case let .available(update):
            availableUpdate(update)
        case let .downloading(progress):
            downloadingUpdate(progress)
        case let .completed(version):
            SettingsNotice(
                tone: .success,
                message: "当前版本 \(version)",
                title: "更新完成"
            )
        case let .failed(message):
            VStack(alignment: .leading, spacing: 12) {
                SettingsNotice(
                    tone: .error,
                    message: message,
                    title: "检查更新失败"
                )

                Button("重试") {
                    Task { await environment.checkForUpdates() }
                }
                .liquidGlassButton()
            }
        }
    }

    private func availableUpdate(_ update: AppUpdate) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            SettingsNotice(
                tone: .info,
                message: "下载完成后可替换当前版本。",
                title: "发现新版本 \(update.version)",
                systemImage: "arrow.down.circle.fill"
            )

            UpdateNotesView(notes: update.notes, style: .reading)

            HStack(spacing: 12) {
                Button("安装更新") {
                    Task { await environment.installAvailableUpdate() }
                }
                .liquidGlassButton(prominent: true)
                .accessibilityLabel("安装更新")

                Link("查看发布说明", destination: update.releaseURL)
                    .font(SettingsTypography.secondary)
                    .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
                    .accessibilityLabel("查看发布说明")

                Spacer(minLength: 0)
            }
        }
    }

    private func downloadingUpdate(_ progress: Double?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("正在下载更新")
                    .font(SettingsTypography.body)
                Spacer(minLength: 8)

                if let progress {
                    Text("\(Int(progress * 100))%")
                        .font(SettingsTypography.numeric)
                        .monospacedDigit()
                }
            }

            if let progress {
                ProgressView(value: progress, total: 1)
            } else {
                ProgressView()
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            progress.map { "正在下载更新，已完成 \(Int($0 * 100))%" } ?? "正在下载更新"
        )
    }

    private var feedbackSection: some View {
        SettingsGroup(
            title: "帮助与反馈",
            subtitle: "发送问题描述和本地诊断日志，帮助我们定位问题。"
        ) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    Button("导出诊断日志") {
                        Task { await environment.exportDiagnosticsLog() }
                    }
                    .liquidGlassButton()
                    .accessibilityLabel("导出诊断日志")

                    Button("提交问题") {
                        Task { await environment.openIssueReport() }
                    }
                    .liquidGlassButton()
                    .accessibilityLabel("提交问题")

                    Spacer(minLength: 0)
                }
                .padding(.vertical, 7)

                if let message = environment.diagnosticsExportMessage {
                    SettingsNotice(tone: .info, message: message)
                        .padding(.bottom, 7)
                }
            }
        }
    }
}

private struct CurrentReleaseNotesView: View {
    let notes: String

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let items = UpdateReleaseSummaryBuilder.items(from: notes)
        if items.isEmpty || !UpdateReleaseSummaryBuilder.supportsSummaryLayout(notes: notes) {
            UpdateNotesView(notes: notes, style: .reading)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    summaryRow(item)
                    .padding(.vertical, 8)

                    if index < items.count - 1 {
                        SettingsRowDivider()
                            .padding(.leading, 31)
                    }
                }
            }
        }
    }

    private func summaryRow(_ item: UpdateReleaseSummaryItem) -> some View {
        HStack(alignment: .center, spacing: 9) {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(item.category.tint(colorScheme).opacity(0.12))
                .frame(width: 22, height: 22)
                .overlay {
                    Image(systemName: item.category.systemImage)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(item.category.tint(colorScheme))
                }
                .accessibilityHidden(true)

            Text(item.text)
                .font(SettingsTypography.body)
                .lineSpacing(2)
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(item.category.title)，\(item.text)")
    }
}

private extension UpdateReleaseChangeCategory {
    var systemImage: String {
        switch self {
        case .added:
            return "plus"
        case .fixed:
            return "wrench.and.screwdriver"
        case .improved:
            return "sparkles"
        case .other:
            return "circle.fill"
        }
    }

    func tint(_ colorScheme: ColorScheme) -> Color {
        switch self {
        case .added:
            return .accentColor
        case .fixed:
            return CompactPopoverPalette.warningColor
        case .improved:
            return CompactPopoverPalette.positive(colorScheme)
        case .other:
            return .secondary
        }
    }
}

private struct ReleaseHistorySheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    let state: AppReleaseHistoryState
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                SettingsModalTitle(
                    "历史版本更新日志",
                    subtitle: "按发布时间倒序展示 macOS 版本"
                )

                Spacer(minLength: 16)

                SettingsCloseButton(action: { dismiss() })
            }
            .padding(.horizontal, SettingsDesign.modalPadding)
            .padding(.top, SettingsDesign.modalPadding)
            .padding(.bottom, SettingsDesign.modalHeaderSpacing)

            Rectangle()
                .fill(Color.primary.opacity(0.08))
                .frame(height: 0.8)

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(minWidth: 600, minHeight: 540)
        .onExitCommand(perform: { dismiss() })
    }

    @ViewBuilder
    private var content: some View {
        switch state {
        case .idle, .loading:
            HStack(spacing: 10) {
                ProgressView()
                    .controlSize(.small)
                Text("正在加载更新日志")
                    .font(SettingsTypography.body)
                    .foregroundStyle(.secondary)
            }
            .padding(24)
        case let .failed(message):
            VStack(alignment: .leading, spacing: 12) {
                SettingsNotice(
                    tone: .error,
                    message: message,
                    title: "无法加载更新日志"
                )

                Button("重试") {
                    Task { await onRetry() }
                }
                .liquidGlassButton()
            }
            .padding(24)
        case let .loaded(releases):
            if releases.isEmpty {
                ContentUnavailableView(
                    "暂无历史版本更新日志",
                    systemImage: "clock.arrow.circlepath"
                )
                .padding(24)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(releases, id: \.id) { release in
                            releaseRow(release)
                        }
                    }
                    .padding(SettingsDesign.modalPadding)
                }
            }
        }
    }

    private func releaseRow(_ release: AppReleaseHistoryItem) -> some View {
        SettingsPanel {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline) {
                    Text("v\(release.version)")
                        .font(.system(size: 12.5, weight: .semibold, design: .monospaced))
                        .monospacedDigit()
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            Color.primary.opacity(0.055),
                            in: Capsule()
                        )

                    Spacer(minLength: 12)

                    if let publishedAt = release.publishedAt {
                        Text(UsageFormatter.shortDateText(publishedAt))
                            .font(SettingsTypography.secondary)
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                    }
                }

                UpdateNotesView(notes: release.notes, style: .reading)

                Link(destination: release.releaseURL) {
                    Label("在 GitHub 查看", systemImage: "arrow.up.right")
                        .font(SettingsTypography.secondary.weight(.medium))
                }
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
                .foregroundStyle(Color.accentColor)
            }
        }
        .accessibilityElement(children: .contain)
    }
}
