import UniformTypeIdentifiers
import SwiftUI

struct GeneralSettingsView: View {
    @Bindable var environment: AppEnvironment
    @Bindable var settings: AppSettings
    @State private var operationError: String?
    @State private var operationNotice: String?
    @State private var showsImportPicker = false
    @State private var showsImportConfirmation = false
    @State private var pendingImportURL: URL?

    init(environment: AppEnvironment) {
        self.environment = environment
        self.settings = environment.settings
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                SettingsPageHeader(
                    title: "通用",
                    subtitle: "刷新、启动、提醒与显示"
                )

                refreshSection
                launchSection
                notificationSection
                colorRulesSection
                backupSection
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fileImporter(
            isPresented: $showsImportPicker,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                pendingImportURL = urls.first
                showsImportConfirmation = pendingImportURL != nil
            case .failure(let error):
                operationError = "无法选择备份文件：\(error.localizedDescription)"
            }
        }
        .confirmationDialog(
            "确定导入并替换当前配置？",
            isPresented: $showsImportConfirmation,
            titleVisibility: .visible
        ) {
            Button("导入并替换", role: .destructive) {
                importSelectedBackup()
            }
            Button("取消", role: .cancel) {
                pendingImportURL = nil
            }
        } message: {
            Text("当前凭证、用量提醒和展示顺序会被备份文件覆盖。导入后建议立即刷新用量。")
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
        .alert(
            "操作完成",
            isPresented: Binding(
                get: { operationNotice != nil },
                set: { if !$0 { operationNotice = nil } }
            )
        ) {
            Button("好") { operationNotice = nil }
        } message: {
            Text(operationNotice ?? "")
        }
    }

    private var refreshSection: some View {
        settingSection {
            HStack(alignment: .center, spacing: 16) {
                settingLabel(
                    title: "刷新间隔",
                    message: "只刷新已启用的凭证，每个凭证的用量保持独立。"
                )

                Spacer(minLength: 16)

                Picker("刷新间隔", selection: $settings.refreshMinutes) {
                    ForEach(AppSettings.allowedRefreshMinutes, id: \.self) { minutes in
                        Text("每 \(minutes) 分钟")
                            .tag(minutes)
                    }
                }
                .pickerStyle(.menu)
                .labelsHidden()
                .frame(width: 150)
                .accessibilityLabel("自动刷新间隔")
            }
        }
    }

    private var launchSection: some View {
        settingSection {
            HStack {
                settingLabel(title: "登录时启动", message: "开机后自动启动 MyToken")

                Spacer(minLength: 16)

                Toggle("登录时启动", isOn: launchAtLoginBinding)
                    .toggleStyle(.switch)
                    .labelsHidden()
                    .accessibilityLabel("登录时启动")
            }
        }
    }

    private var notificationSection: some View {
        settingSection {
            HStack {
                settingLabel(
                    title: "启用通知",
                    message: "额度接近限制或订阅即将到期时提醒"
                )

                Spacer(minLength: 16)

                Toggle("启用通知", isOn: $settings.notificationsEnabled)
                    .toggleStyle(.switch)
                    .labelsHidden()
                    .accessibilityLabel("启用通知")
            }
        }
    }

    private var backupSection: some View {
        settingSection {
            settingLabel(
                title: "配置备份",
                message: "导出全局偏好、展示顺序、提醒规则和凭证密钥；文件未加密，请勿发送给他人。"
            )

            HStack(spacing: 12) {
                Button {
                    exportConfiguration()
                } label: {
                    Label("导出配置", systemImage: "square.and.arrow.up")
                }
                .buttonStyle(.borderedProminent)
                .help("导出包含凭证密钥的完整配置备份")

                Button {
                    showsImportPicker = true
                } label: {
                    Label("导入配置", systemImage: "square.and.arrow.down")
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private func exportConfiguration() {
        do {
            let data = try environment.configurationBackupData()
            let panel = NSSavePanel()
            panel.allowedContentTypes = [.json]
            panel.nameFieldStringValue = backupFileName()
            panel.canCreateDirectories = true
            guard panel.runModal() == .OK, let url = panel.url else { return }
            try data.write(to: url, options: .atomic)
            operationNotice = "配置已导出。备份文件包含凭证密钥，请妥善保存。"
        } catch {
            operationError = "无法导出配置：\(error.localizedDescription)"
        }
    }

    private func importSelectedBackup() {
        guard let url = pendingImportURL else { return }
        pendingImportURL = nil
        let secured = url.startAccessingSecurityScopedResource()
        defer {
            if secured {
                url.stopAccessingSecurityScopedResource()
            }
        }

        do {
            let data = try Data(contentsOf: url)
            try environment.importConfigurationBackup(from: data)
            operationNotice = "配置已导入，当前应用已切换到备份中的凭证和偏好。"
        } catch {
            operationError = "导入失败：\(error.localizedDescription)"
        }
    }

    private func backupFileName() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd-HHmm"
        return "MyToken-Config-\(formatter.string(from: .now)).json"
    }

    private var colorRulesSection: some View {
        settingSection {
            settingLabel(
                title: "进度颜色",
                message: "拖动色带上的两个分界点，再为每个百分比区间选择颜色。"
            )

            MenuBarColorRulesEditor(rules: $settings.menuBarColorRules)
                .frame(maxWidth: 520, alignment: .leading)
        }
    }

    private func settingLabel(title: String, message: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
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
        .liquidGlassSurface(cornerRadius: 16)
    }

    private var launchAtLoginBinding: Binding<Bool> {
        Binding(
            get: { environment.settings.launchAtLogin },
            set: { enabled in
                do {
                    try LoginItemSettingSynchronizer.setEnabled(
                        enabled,
                        settings: environment.settings,
                        manager: environment.loginItemManager
                    )
                } catch {
                    operationError = "无法更新登录启动设置：\(error.localizedDescription)"
                }
            }
    )
    }

}
