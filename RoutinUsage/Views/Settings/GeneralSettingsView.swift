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
    @State private var showsTransferToAndroid = false

    init(environment: AppEnvironment) {
        self.environment = environment
        self.settings = environment.settings
    }

    var body: some View {
        SettingsPageScroll {
            SettingsPageHeader(
                title: "通用",
                subtitle: "刷新、外观、启动与本地数据"
            )

            syncSection
            appearanceSection
            launchSection
            dataMigrationSection
            backupSection
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
        .sheet(isPresented: $showsTransferToAndroid) {
            TransferToAndroidView(environment: environment) {
                showsTransferToAndroid = false
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
        .onChange(of: settings.appearance) { _, appearance in
            environment.appearanceDidChange(to: appearance)
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

    private var syncSection: some View {
        SettingsGroup(title: "同步与提醒") {
            SettingsRow(
                systemImage: "arrow.triangle.2.circlepath",
                title: "刷新间隔",
                detail: "只刷新已启用的凭证，每个凭证的用量保持独立。"
            ) {
                RefreshIntervalControl(minutes: $settings.refreshMinutes)
            }

            SettingsRowDivider()

            SettingsRow(
                systemImage: "bell.badge",
                title: "启用通知",
                detail: "额度接近限制或订阅即将到期时提醒。"
            ) {
                Toggle("启用通知", isOn: $settings.notificationsEnabled)
                    .toggleStyle(.switch)
                    .labelsHidden()
                    .accessibilityLabel("启用通知")
            }
        }
    }

    private var appearanceSection: some View {
        SettingsGroup(title: "外观与显示") {
            SettingsRow(
                systemImage: "circle.lefthalf.filled",
                title: "外观方案",
                detail: "跟随系统，或固定使用浅色、深色。"
            ) {
                AppAppearanceSegmentedControl(
                    selection: $settings.appearance
                )
                .accessibilityLabel("外观")
            }

            SettingsRowDivider()

            SettingsRow(
                systemImage: "rectangle.grid.1x2",
                title: "卡片显示",
                detail: "简洁模式只保留各供应商最常用的用量信息。"
            ) {
                UsageCardDensitySegmentedControl(
                    selection: $settings.usageCardDensity
                )
                .accessibilityLabel("卡片显示")
            }
        }
    }

    private var launchSection: some View {
        SettingsGroup(title: "启动") {
            SettingsRow(
                systemImage: "power",
                title: "登录时启动",
                detail: "开机后自动启动 MyToken。"
            ) {
                Toggle("登录时启动", isOn: launchAtLoginBinding)
                    .toggleStyle(.switch)
                    .labelsHidden()
                    .accessibilityLabel("登录时启动")
            }
        }
    }

    private var backupSection: some View {
        SettingsGroup(
            title: "配置备份",
            subtitle: "导出全局偏好、展示顺序、提醒规则和凭证密钥。文件未加密，请勿发送给他人。"
        ) {
            HStack(spacing: 12) {
                Button {
                    exportConfiguration()
                } label: {
                    Label("导出配置", systemImage: "square.and.arrow.up")
                }
                .liquidGlassButton()
                .help("导出包含凭证密钥的完整配置备份")

                Button {
                    showsImportPicker = true
                } label: {
                    Label("导入配置", systemImage: "square.and.arrow.down")
                }
                .liquidGlassButton()
            }
            .padding(.vertical, 7)

            SettingsNotice(
                tone: .warning,
                message: "备份文件包含凭证密钥，请保存到可信位置。",
                systemImage: "exclamationmark.shield.fill"
            )
            .padding(.bottom, 7)
        }
    }

    private var dataMigrationSection: some View {
        SettingsGroup(
            title: "数据与迁移",
            subtitle: "在设备之间转移凭证、偏好和展示顺序。"
        ) {
            SettingsRow(
                systemImage: "iphone.and.arrow.forward",
                title: "迁移到 Android",
                detail: "通过同一局域网加密传输当前配置。"
            ) {
                Button("开始迁移") {
                    showsTransferToAndroid = true
                }
                .liquidGlassButton()
                .accessibilityLabel("迁移配置到 Android")
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
