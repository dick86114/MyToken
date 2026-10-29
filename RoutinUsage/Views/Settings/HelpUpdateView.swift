import SwiftUI

struct HelpUpdateView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Bindable var environment: AppEnvironment
    @State private var showingReleaseHistory = false
    @State private var githubTokenInput = ""
    @State private var githubTokenMask: String?
    @State private var githubTokenMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                SettingsPageHeader(
                    title: "帮助与更新",
                    subtitle: "版本维护和问题反馈"
                )

                currentVersionSection
                starPromptSection
                githubTokenSection
                updateSection
                feedbackSection
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
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
        settingSection {
            HStack(spacing: 12) {
                Text("当前版本")

                Text(RoutinUsageApp.currentVersion)
                    .monospacedDigit()

                Spacer(minLength: 12)

                Link(destination: currentReleaseURL) {
                    Label("GitHub", systemImage: "arrow.up.right.square")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .accessibilityLabel("在 GitHub 查看当前版本")

                Button {
                    Task { await environment.loadReleaseHistoryIfNeeded(force: true) }
                } label: {
                    Label("获取当前版本日志", systemImage: "arrow.clockwise")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .accessibilityLabel("获取当前版本日志")
            }

            Divider()

            Text("当前版本更新日志")
                .font(.system(size: 14, weight: .semibold))

            currentReleaseNotes

            HStack {
                Button("查看历史版本") {
                    showingReleaseHistory = true
                }
                .liquidGlassButton()
                .accessibilityLabel("查看历史版本更新日志")

                Spacer(minLength: 0)
            }
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
                UpdateNotesView(notes: current.notes)
            } else {
                Text("此版本未提供更新日志")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
        }
    }

    /// 版本区块下方的 GitHub Star 引导；用户关闭后不再显示。
    @ViewBuilder
    private var starPromptSection: some View {
        if !environment.settings.starPromptDismissed {
            StarPromptBanner {
                environment.settings.starPromptDismissed = true
            }
        }
    }

    private var currentReleaseURL: URL {
        guard case let .loaded(releases) = environment.releaseHistoryState,
              let current = releases.first(where: { $0.version == RoutinUsageApp.currentVersion }) else {
            return RoutinUsageApp.releasesURL
        }
        return current.releaseURL
    }

    private var githubTokenSection: some View {
        settingSection {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("GitHub 令牌")
                        .font(.system(size: 14, weight: .semibold))

                    Text("检查更新时使用。带令牌每小时 5000 次请求，不带仅 60 次；公开仓库只读权限即可。")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 12)

                if let githubTokenMask {
                    Text(githubTokenMask)
                        .font(.system(size: 13, design: .monospaced))
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("已保存 GitHub 令牌")

                    Button("移除") {
                        removeGitHubToken()
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .accessibilityLabel("移除 GitHub 令牌")
                }
            }

            if githubTokenMask == nil {
                HStack(spacing: 12) {
                    SecureField("ghp_ 或 github_pat_ 令牌", text: $githubTokenInput)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityLabel("GitHub 令牌输入框")

                    Button("保存") {
                        saveGitHubToken()
                    }
                    .buttonStyle(.bordered)
                    .disabled(githubTokenInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityLabel("保存 GitHub 令牌")
                }
            }

            if let githubTokenMessage {
                Text(githubTokenMessage)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func saveGitHubToken() {
        do {
            try environment.gitHubTokenStore.saveToken(githubTokenInput)
            githubTokenInput = ""
            githubTokenMask = environment.gitHubTokenStore.tokenMask()
            githubTokenMessage = "GitHub 令牌已保存到本机钥匙串"
        } catch {
            githubTokenMessage = "GitHub 令牌保存失败，请重试"
        }
    }

    private func removeGitHubToken() {
        environment.gitHubTokenStore.removeToken()
        githubTokenMask = nil
        githubTokenMessage = nil
    }

    private var updateSection: some View {
        @Bindable var settings = environment.settings
        return settingSection {
            Text("应用更新")
                .font(.system(size: 14, weight: .semibold))

            Picker("更新通道", selection: $settings.updateChannel) {
                Text("GitHub 直连").tag(UpdateChannel.direct)
                Text("CDN 加速").tag(UpdateChannel.cdn)
            }
            .pickerStyle(.segmented)
            .accessibilityLabel("更新通道")

            if environment.settings.updateChannel == .cdn {
                Picker("CDN 源", selection: $settings.updateCDNBase) {
                    ForEach(AppSettings.cdnBases, id: \.self) { base in
                        Text(base.replacingOccurrences(of: "https://", with: "")).tag(base)
                    }
                }
                .accessibilityLabel("CDN 加速源")

                Text("大陆网络建议选择 CDN 加速；若某镜像不可用可切换其他源。")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }

            Divider()

            updateStatusContent
        }
    }

    @ViewBuilder
    private var updateStatusContent: some View {
        switch environment.updateStatus {
        case .idle:
            HStack(spacing: 12) {
                Button("检测更新") {
                    Task { await environment.checkForUpdates() }
                }
                .liquidGlassButton()
                .accessibilityLabel("检测更新")

                Spacer(minLength: 0)
            }
        case .checking:
            HStack {
                Text("正在检查更新")
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
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(CompactPopoverPalette.positive(colorScheme))
                    .accessibilityHidden(true)
                Text("更新完成，当前版本 \(version)")
            }
            .accessibilityElement(children: .combine)
        case let .failed(message):
            VStack(alignment: .leading, spacing: 12) {
                Text(message)
                    .foregroundStyle(CompactPopoverPalette.criticalColor)

                Button("重试") {
                    Task { await environment.checkForUpdates() }
                }
                .liquidGlassButton()
            }
        }
    }

    private func availableUpdate(_ update: AppUpdate) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("发现新版本 \(update.version)")
                .font(.system(size: 14, weight: .semibold))

            UpdateNotesView(notes: update.notes)

            HStack(spacing: 12) {
                Button("安装更新") {
                    Task { await environment.installAvailableUpdate() }
                }
                .liquidGlassButton(prominent: true)
                .accessibilityLabel("安装更新")

                Link("查看发布说明", destination: update.releaseURL)
                    .font(.system(size: 12))
                    .accessibilityLabel("查看发布说明")

                Spacer(minLength: 0)
            }
        }
    }

    private func downloadingUpdate(_ progress: Double?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("正在下载更新")
                Spacer(minLength: 8)

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
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            progress.map { "正在下载更新，已完成 \(Int($0 * 100))%" } ?? "正在下载更新"
        )
    }

    private var feedbackSection: some View {
        settingSection {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("遇到问题？")
                        .font(.system(size: 14, weight: .semibold))

                    Text("发送问题描述和本地诊断日志")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 16)

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
            }

            if let message = environment.diagnosticsExportMessage {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func settingSection<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .liquidGlassSurface()
    }
}

/// 版本区块附近的 GitHub Star 引导条：星标图标 + 引导文案 + 去点 Star 按钮 + 关闭按钮。
private struct StarPromptBanner: View {
    @Environment(\.colorScheme) private var colorScheme
    let onDismiss: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: "star.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.warningColor)
                .accessibilityHidden(true)

            Text("如果 MyToken 帮到了你，请在 GitHub 点个 Star，这对我们意义重大")
                .font(.system(size: 13))
                .fixedSize(horizontal: false, vertical: true)

            Spacer(minLength: 12)

            Link(destination: RoutinUsageApp.githubURL) {
                Label("去点 Star", systemImage: "arrow.up.right.square")
                    .font(.system(size: 12, weight: .semibold))
            }
            .liquidGlassButton(prominent: true)
            .accessibilityLabel("在 GitHub 打开 MyToken 仓库并为项目点 Star")

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .semibold))
                    .frame(width: 20, height: 20)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .accessibilityLabel("关闭 Star 引导")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(CompactPopoverPalette.warningColor.opacity(colorScheme == .dark ? 0.14 : 0.10))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(CompactPopoverPalette.warningColor.opacity(0.28), lineWidth: 1)
        )
    }
}

private struct ReleaseHistorySheet: View {
    @Environment(\.dismiss) private var dismiss

    let state: AppReleaseHistoryState
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("历史版本更新日志")
                    .font(.system(size: 20, weight: .semibold))

                Spacer(minLength: 16)

                Button("完成") {
                    dismiss()
                }
                .keyboardShortcut(.cancelAction)
            }
            .padding(20)

            Divider()

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(minWidth: 560, minHeight: 520)
    }

    @ViewBuilder
    private var content: some View {
        switch state {
        case .idle, .loading:
            HStack(spacing: 10) {
                ProgressView()
                    .controlSize(.small)
                Text("正在加载更新日志")
                    .foregroundStyle(.secondary)
            }
            .padding(24)
        case let .failed(message):
            VStack(alignment: .leading, spacing: 12) {
                Text(message)
                    .foregroundStyle(CompactPopoverPalette.criticalColor)

                Button("重试") {
                    Task { await onRetry() }
                }
                .liquidGlassButton()
            }
            .padding(24)
        case let .loaded(releases):
            if releases.isEmpty {
                Text("暂无历史版本更新日志")
                    .foregroundStyle(.secondary)
                    .padding(24)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(Array(releases.enumerated()), id: \.element.id) { index, release in
                            releaseRow(release)

                            if index < releases.count - 1 {
                                Divider()
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                }
            }
        }
    }

    private func releaseRow(_ release: AppReleaseHistoryItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text("v\(release.version)")
                    .font(.system(size: 14, weight: .semibold))

                Spacer(minLength: 12)

                if let publishedAt = release.publishedAt {
                    Text(UsageFormatter.shortDateText(publishedAt))
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }

            UpdateNotesView(notes: release.notes)

            Link("在 GitHub 查看", destination: release.releaseURL)
                .font(.system(size: 12))
        }
        .padding(.vertical, 16)
        .accessibilityElement(children: .contain)
    }
}
