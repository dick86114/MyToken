import SwiftUI

struct ValidatedCredentialInput: Sendable, Equatable {
    let providerID: ProviderID
    let credentialKind: CredentialKind
    let name: String
    let secret: String
    let metadata: [String: String]
}

enum CredentialWebsiteDefaults {
    static func suggestedURL(for providerID: ProviderID, customBaseURL: String = "") -> String {
        if providerID == .newAPI {
            return customBaseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return providerID.defaultConsoleURL ?? ""
    }
}

enum CredentialEditorValidation {
    static func validate(
        providerID: ProviderID,
        name: String,
        apiKey: String,
        accessKeyID: String,
        secretAccessKey: String,
        region: String,
        balanceWarningThreshold: String,
        planType: String = "agent",
        newAPIBaseURL: String = "",
        newAPIUserID: String = "",
        xiaomiUsageKind: String = "api",
        websiteURL: String = ""
    ) throws -> ValidatedCredentialInput {
        let normalizedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedName.isEmpty else { throw UsageStoreError.invalidName }
        let websiteMetadata = try websiteMetadata(from: websiteURL)

        switch providerID {
        case .routin:
            guard KeyCredentialPolicy.isSafeDisplayName(normalizedName),
                  KeyCredentialPolicy.hasValidPrefix(apiKey),
                  KeyCredentialPolicy.hasSufficientSecretPayload(apiKey)
            else { throw UsageStoreError.invalidSecret }
            return ValidatedCredentialInput(
                providerID: .routin,
                credentialKind: .bearerAPIKey,
                name: normalizedName,
                secret: apiKey,
                metadata: ["planType": planType].merging(websiteMetadata) { current, _ in current }
            )
        case .deepseek:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            var metadata: [String: String] = [:]
            if !balanceWarningThreshold.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                guard Decimal(string: balanceWarningThreshold) != nil else {
                    throw UsageStoreError.invalidSecret
                }
                metadata["balanceWarningThreshold"] = balanceWarningThreshold
            }
            return ValidatedCredentialInput(
                providerID: providerID,
                credentialKind: .apiKey,
                name: normalizedName,
                secret: secret,
                metadata: metadata.merging(websiteMetadata) { current, _ in current }
            )
        case .glm:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            return ValidatedCredentialInput(
                providerID: .glm,
                credentialKind: .apiKey,
                name: normalizedName,
                secret: secret,
                metadata: websiteMetadata
            )
        case .volcengine:
            let accessKey = accessKeyID.trimmingCharacters(in: .whitespacesAndNewlines)
            let secret = secretAccessKey.trimmingCharacters(in: .whitespacesAndNewlines)
            let resolvedRegion = region.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "cn-beijing"
                : region.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !accessKey.isEmpty, !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            return ValidatedCredentialInput(
                providerID: .volcengine,
                credentialKind: .accessKeyPair,
                name: normalizedName,
                secret: secret,
                metadata: [
                    "accessKeyID": accessKey,
                    "region": resolvedRegion,
                    "planType": planType
                ].merging(websiteMetadata) { current, _ in current }
            )
        case .newAPI:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            let rawBaseURL = newAPIBaseURL.trimmingCharacters(in: .whitespacesAndNewlines)
            let userID = newAPIUserID.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty,
                  let parsedUserID = Int(userID),
                  parsedUserID > 0,
                  let url = URL(string: rawBaseURL),
                  url.scheme != nil,
                  url.host != nil
            else { throw UsageStoreError.invalidSecret }
            var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            while components?.path.hasSuffix("/") == true {
                components?.path.removeLast()
            }
            if components?.path == "/api" {
                components?.path = ""
            }
            guard let normalizedURL = components?.url else { throw UsageStoreError.invalidSecret }
            var metadata = [
                "baseURL": normalizedURL.absoluteString,
                "userID": userID
            ]
            let threshold = balanceWarningThreshold.trimmingCharacters(in: .whitespacesAndNewlines)
            if !threshold.isEmpty {
                guard Decimal(string: threshold) != nil else { throw UsageStoreError.invalidSecret }
                metadata["balanceWarningThreshold"] = threshold
            }
            return ValidatedCredentialInput(
                providerID: .newAPI,
                credentialKind: .bearerAPIKey,
                name: normalizedName,
                secret: secret,
                metadata: metadata.merging(websiteMetadata) { current, _ in current }
            )
        case .commandCode:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            return ValidatedCredentialInput(
                providerID: .commandCode,
                credentialKind: .bearerAPIKey,
                name: normalizedName,
                secret: secret,
                metadata: websiteMetadata
            )
        case .opencode:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            return ValidatedCredentialInput(
                providerID: .opencode,
                credentialKind: .bearerAPIKey,
                name: normalizedName,
                secret: secret,
                metadata: websiteMetadata
            )
        case .xiaomi:
            let secret = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !secret.isEmpty else { throw UsageStoreError.invalidSecret }
            let usageKind = xiaomiUsageKind.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            guard usageKind == "api" || usageKind == "plan" else {
                throw UsageStoreError.invalidSecret
            }
            switch usageKind {
            case "api":
                guard secret.lowercased().hasPrefix("sk-") else {
                    throw UsageStoreError.invalidSecret
                }
            case "plan":
                guard secret.lowercased().hasPrefix("tp-") else {
                    throw UsageStoreError.invalidSecret
                }
            default:
                throw UsageStoreError.invalidSecret
            }
            var metadata = ["usageKind": usageKind]
            let threshold = balanceWarningThreshold.trimmingCharacters(in: .whitespacesAndNewlines)
            if usageKind == "api", !threshold.isEmpty {
                guard Decimal(string: threshold) != nil else {
                    throw UsageStoreError.invalidSecret
                }
                metadata["balanceWarningThreshold"] = threshold
            }
            return ValidatedCredentialInput(
                providerID: .xiaomi,
                credentialKind: .bearerAPIKey,
                name: normalizedName,
                secret: secret,
                metadata: metadata.merging(websiteMetadata) { current, _ in current }
            )
        }
    }

    /// Absolute balance warnings only apply to providers that expose a balance metric.
    static func supportsBalanceWarningThreshold(
        providerID: ProviderID,
        xiaomiUsageKind: String = "api"
    ) -> Bool {
        switch providerID {
        case .deepseek, .newAPI:
            return true
        case .xiaomi:
            return xiaomiUsageKind
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased() == "api"
        default:
            return false
        }
    }

    static func websiteMetadata(from rawValue: String) throws -> [String: String] {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [:] }

        let lowercased = trimmed.lowercased()
        let candidate = lowercased.hasPrefix("http://") || lowercased.hasPrefix("https://")
            ? trimmed
            : "https://\(trimmed)"
        guard let url = URL(string: candidate),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              components.scheme == "http" || components.scheme == "https",
              components.host != nil
        else {
            throw UsageStoreError.invalidURL
        }
        return ["websiteURL": url.absoluteString]
    }
}

@MainActor
struct CredentialEditorView: View {
    private enum Field: CaseIterable, Hashable {
        case provider
        case name
        case website
        case newAPIBaseURL
        case newAPIUserID
        case apiKey
        case accessKeyID
        case secretAccessKey
        case balanceWarningThreshold
    }

    let title: String
    let initialProviderID: ProviderID
    let initialName: String
    let initialSecret: String
    let initialMetadata: [String: String]
    let save: @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    let onSaved: @MainActor () -> Void
    let onClose: () -> Void

    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme
    @State private var providerID: ProviderID
    @State private var name: String
    @State private var apiKey: String
    @State private var accessKeyID: String
    @State private var secretAccessKey: String
    @State private var region: String
    @State private var balanceWarningThreshold: String
    @State private var planType: String
    @State private var newAPIBaseURL: String
    @State private var newAPIUserID: String
    @State private var xiaomiUsageKind: String
    @State private var websiteURL: String
    @State private var isSecretVisible = false
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var autoSuggestedWebsiteURL: String
    @State private var hasAttemptedSubmit = false
    @State private var showsDiscardConfirmation = false
    @State private var requestedScrollField: Field?

    init(
        title: String = "添加凭证",
        initialProviderID: ProviderID = .deepseek,
        initialName: String = "",
        initialSecret: String = "",
        initialMetadata: [String: String] = [:],
        save: @escaping @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult,
        onSaved: @escaping @MainActor () -> Void = {},
        onClose: @escaping () -> Void = {}
    ) {
        self.title = title
        self.initialProviderID = initialProviderID
        self.initialName = initialName
        self.initialSecret = initialSecret
        self.initialMetadata = initialMetadata
        self.save = save
        self.onSaved = onSaved
        self.onClose = onClose
        _providerID = State(initialValue: initialProviderID)
        _name = State(initialValue: initialName)
        _apiKey = State(initialValue: initialProviderID == .volcengine ? "" : initialSecret)
        _accessKeyID = State(initialValue: initialMetadata["accessKeyID"] ?? "")
        _secretAccessKey = State(initialValue: initialProviderID == .volcengine ? initialSecret : "")
        _region = State(initialValue: initialMetadata["region"] ?? "cn-beijing")
        _balanceWarningThreshold = State(initialValue: initialMetadata["balanceWarningThreshold"] ?? "")
        _planType = State(initialValue: initialMetadata["planType"] ?? "agent")
        _newAPIBaseURL = State(initialValue: initialMetadata["baseURL"] ?? "")
        _newAPIUserID = State(initialValue: initialMetadata["userID"] ?? "")
        _xiaomiUsageKind = State(initialValue: initialMetadata["usageKind"] ?? "api")
        let storedWebsiteURL = (initialMetadata["websiteURL"] ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let suggestedWebsiteURL = CredentialWebsiteDefaults.suggestedURL(
            for: initialProviderID,
            customBaseURL: initialMetadata["baseURL"] ?? ""
        )
        _websiteURL = State(initialValue: storedWebsiteURL.isEmpty ? suggestedWebsiteURL : storedWebsiteURL)
        _autoSuggestedWebsiteURL = State(initialValue: storedWebsiteURL.isEmpty ? suggestedWebsiteURL : "")
    }

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, SettingsDesign.modalPadding)
                .padding(.top, SettingsDesign.modalPadding)
                .padding(.bottom, SettingsDesign.modalHeaderSpacing)

            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)

            ScrollViewReader { proxy in
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 14) {
                        basicInformationSection
                        providerCredentialFields

                        if let errorMessage {
                            SettingsNotice(
                                tone: .error,
                                message: errorMessage,
                                title: "无法保存凭证"
                            )
                        }
                    }
                    .padding(.horizontal, SettingsDesign.modalPadding)
                    .padding(.vertical, 18)
                }
                .onChange(of: requestedScrollField) { _, field in
                    guard let field else { return }
                    withAnimation(.easeOut(duration: 0.2)) {
                        proxy.scrollTo(field, anchor: .center)
                    }
                    DispatchQueue.main.async {
                        requestedScrollField = nil
                    }
                }
            }

            footer
                .padding(.horizontal, SettingsDesign.modalPadding)
                .padding(.vertical, 16)
                .background {
                    Rectangle()
                        .fill(Color.primary.opacity(colorScheme == .dark ? 0.018 : 0.010))
                }
                .overlay(alignment: .top) {
                    Rectangle()
                        .fill(SettingsDesign.dividerColor(colorScheme))
                        .frame(height: 0.8)
                }
        }
        .frame(width: 680)
        .frame(minHeight: 460, idealHeight: 560, maxHeight: 720)
        .confirmationDialog(
            "放弃未保存的修改？",
            isPresented: $showsDiscardConfirmation,
            titleVisibility: .visible
        ) {
            Button("放弃修改", role: .destructive, action: onClose)
            Button("继续编辑", role: .cancel) {}
        } message: {
            Text("当前填写的凭证信息尚未保存，关闭后会丢失。")
        }
        .onExitCommand(perform: close)
        .onChange(of: providerID) { _, newProviderID in
            hasAttemptedSubmit = false
            isSecretVisible = false
            let suggestion = CredentialWebsiteDefaults.suggestedURL(
                for: newProviderID,
                customBaseURL: newAPIBaseURL
            )
            autoSuggestedWebsiteURL = suggestion
            websiteURL = suggestion
        }
        .onChange(of: apiKey) { _, secret in
            if !CredentialVisibility.canToggle(secret: secret) {
                isSecretVisible = false
            }
            if providerID == .xiaomi {
                let normalized = secret.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if normalized.hasPrefix("tp-") {
                    xiaomiUsageKind = "plan"
                } else if normalized.hasPrefix("sk-") {
                    xiaomiUsageKind = "api"
                }
            }
        }
        .onChange(of: newAPIBaseURL) { _, newBaseURL in
            guard providerID == .newAPI else { return }
            if websiteURL.isEmpty || websiteURL == autoSuggestedWebsiteURL {
                let suggestion = CredentialWebsiteDefaults.suggestedURL(
                    for: .newAPI,
                    customBaseURL: newBaseURL
                )
                autoSuggestedWebsiteURL = suggestion
                websiteURL = suggestion
            }
        }
        .onChange(of: secretAccessKey) { _, secret in
            if !CredentialVisibility.canToggle(secret: secret) {
                isSecretVisible = false
            }
        }
    }

    private var basicInformationSection: some View {
        SettingsGroup(title: "基本信息") {
            HStack(alignment: .top, spacing: 14) {
                providerField
                    .frame(maxWidth: .infinity)
                    .id(Field.provider)

                SettingsLabeledTextField(
                    label: "名称",
                    text: $name,
                    prompt: "例如：主力凭证",
                    errorText: fieldErrors[.name],
                    accentColor: providerAccent,
                    autoFocus: true
                )
                .frame(maxWidth: .infinity)
                .id(Field.name)
            }

            SettingsLabeledTextField(
                label: "官网 / 后台地址（可选）",
                text: $websiteURL,
                prompt: websitePrompt,
                helpText: "显示在用量详情和分享卡片中，可替换为你的自定义地址。",
                errorText: fieldErrors[.website],
                accentColor: providerAccent
            )
            .padding(.top, 10)
            .id(Field.website)
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            ProviderLogoMark(providerID: providerID, size: 36, cornerRadius: 10)

            SettingsModalTitle(
                title,
                subtitle: "配置 \(providerName) 的查询凭证"
            )

            Spacer()

            SettingsCloseButton(action: close)
                .disabled(isSaving)
        }
    }

    private var providerField: some View {
        VStack(alignment: .leading, spacing: 6) {
            SettingsFieldLabel(text: "供应商")

            Menu {
                Picker(selection: $providerID) {
                    ForEach(ProviderID.allCases) { id in
                        Label(displayName(for: id), image: id.logoAssetName)
                            .tag(id)
                    }
                } label: {
                    EmptyView()
                }
                .labelsHidden()
                .pickerStyle(.inline)
            } label: {
                HStack(spacing: 9) {
                    ProviderLogoMark(providerID: providerID, size: 26, cornerRadius: 6)
                    Text(providerName)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.tertiary)
                }
                .padding(.horizontal, 10)
                .frame(minHeight: SettingsDesign.fieldHeight)
                .background {
                    RoundedRectangle(
                        cornerRadius: SettingsDesign.controlCornerRadius,
                        style: .continuous
                    )
                    .fill(SettingsDesign.fieldFill(colorScheme))
                }
                .overlay {
                    RoundedRectangle(
                        cornerRadius: SettingsDesign.controlCornerRadius,
                        style: .continuous
                    )
                    .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 1)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
            .menuIndicator(.hidden)
            .accessibilityLabel("供应商，当前：\(providerName)")
        }
    }

    private var providerCredentialFields: some View {
        SettingsGroup(
            title: "接入凭证",
            subtitle: credentialGroupSubtitle
        ) {
            VStack(alignment: .leading, spacing: 14) {
                credentialFieldsContent
            }
        }
    }

    @ViewBuilder
    private var credentialFieldsContent: some View {
        if providerID == .newAPI {
            SettingsLabeledTextField(
                label: "New API 地址",
                text: $newAPIBaseURL,
                prompt: "https://your-new-api.com",
                errorText: fieldErrors[.newAPIBaseURL],
                accentColor: providerAccent
            )
            .id(Field.newAPIBaseURL)

            SettingsLabeledTextField(
                label: "数字用户 ID",
                text: $newAPIUserID,
                prompt: "例如：1024",
                errorText: fieldErrors[.newAPIUserID],
                accentColor: providerAccent
            )
            .id(Field.newAPIUserID)

            HStack(spacing: 8) {
                SettingsHintText(text: "只支持数字用户 ID，个人资料页会显示该值。")
                Spacer()
                Button {
                    if let url = newAPIProfileURL {
                        openURL(url)
                    }
                } label: {
                    Label("打开个人资料", systemImage: "arrow.up.right.square")
                }
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
                .font(.caption.weight(.medium))
                .foregroundStyle(newAPIProfileURL == nil ? Color.secondary.opacity(0.45) : providerAccent)
                .disabled(newAPIProfileURL == nil)
            }

            SettingsLabeledTextField(
                label: "用户访问令牌",
                text: $apiKey,
                prompt: "粘贴 New API 用户访问令牌",
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                errorText: fieldErrors[.apiKey],
                accentColor: providerAccent
            )
            .id(Field.apiKey)

            SettingsLabeledTextField(
                label: "低额度预警值（可选）",
                text: $balanceWarningThreshold,
                prompt: "例如：10",
                suffix: "额度",
                errorText: fieldErrors[.balanceWarningThreshold],
                accentColor: providerAccent
            )
            .id(Field.balanceWarningThreshold)
        } else if providerID == .volcengine {
            CredentialSegmentedField(
                label: "计划类型",
                selection: $planType,
                options: [
                    CredentialSegmentOption(id: "agent", title: "Agent Plan"),
                    CredentialSegmentOption(id: "coding", title: "Coding Plan")
                ]
            )

            HStack(alignment: .top, spacing: 14) {
                SettingsLabeledTextField(
                    label: "AccessKey ID",
                    text: $accessKeyID,
                    prompt: "AKLT...",
                    errorText: fieldErrors[.accessKeyID],
                    accentColor: providerAccent
                )
                .frame(maxWidth: .infinity)
                .id(Field.accessKeyID)

                SettingsLabeledTextField(
                    label: "区域（可选）",
                    text: $region,
                    prompt: "cn-beijing",
                    errorText: nil,
                    accentColor: providerAccent
                )
                .frame(maxWidth: .infinity)
            }

            SettingsLabeledTextField(
                label: "SecretAccessKey",
                text: $secretAccessKey,
                prompt: "粘贴 SecretAccessKey",
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: secretAccessKey),
                errorText: fieldErrors[.secretAccessKey],
                accentColor: providerAccent
            )
            .id(Field.secretAccessKey)
        } else if providerID == .xiaomi {
            CredentialSegmentedField(
                label: "查询方式",
                selection: $xiaomiUsageKind,
                options: [
                    CredentialSegmentOption(id: "api", title: "API 按量"),
                    CredentialSegmentOption(id: "plan", title: "Token Plan")
                ]
            )

            SettingsLabeledTextField(
                label: "API Key",
                text: $apiKey,
                prompt: xiaomiAPIKeyPrompt,
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                errorText: fieldErrors[.apiKey],
                accentColor: providerAccent
            )
            .id(Field.apiKey)

            SettingsHintText(text: "API 按量使用 sk- 开头的普通 API Key；Token Plan 使用 tp- 开头的专用 API Key。")

            if xiaomiUsageKind == "api" {
                SettingsLabeledTextField(
                    label: "低余额预警值（可选）",
                    text: $balanceWarningThreshold,
                    prompt: "例如：20",
                    suffix: "元",
                    errorText: fieldErrors[.balanceWarningThreshold],
                    accentColor: providerAccent
                )
                .id(Field.balanceWarningThreshold)
            }
        } else {
            SettingsLabeledTextField(
                label: apiKeyLabel,
                text: $apiKey,
                prompt: apiKeyPrompt,
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                errorText: fieldErrors[.apiKey],
                accentColor: providerAccent
            )
            .id(Field.apiKey)

            if providerID == .deepseek {
                SettingsLabeledTextField(
                    label: "低余额预警值（可选）",
                    text: $balanceWarningThreshold,
                    prompt: "例如：10",
                    suffix: "元",
                    errorText: fieldErrors[.balanceWarningThreshold],
                    accentColor: providerAccent
                )
                .id(Field.balanceWarningThreshold)
            }
        }
    }

    private var footer: some View {
        HStack(spacing: 12) {
            Label("凭证仅保存在此 Mac", systemImage: "lock.shield")
                .font(SettingsTypography.secondary)
                .foregroundStyle(.secondary)

            Spacer()

            Button("取消") {
                close()
            }
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.98))
            .foregroundStyle(.secondary)
            .disabled(isSaving)
            .keyboardShortcut(.cancelAction)

            Button {
                Task { await submit() }
            } label: {
                HStack(spacing: 7) {
                    if isSaving {
                        ProgressView()
                            .controlSize(.small)
                    }
                    Text(isSaving ? "正在验证" : "验证并保存")
                }
                .frame(minWidth: 92)
            }
            .liquidGlassButton(prominent: true)
            .disabled(isSaving)
        }
    }

    private var fieldErrors: [Field: String] {
        guard hasAttemptedSubmit else { return [:] }
        var errors: [Field: String] = [:]

        if name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors[.name] = "请输入名称"
        }

        do {
            _ = try CredentialEditorValidation.websiteMetadata(from: websiteURL)
        } catch {
            errors[.website] = "请输入有效的 HTTP(S) 地址"
        }

        let threshold = balanceWarningThreshold
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let showsBalanceThreshold = CredentialEditorValidation.supportsBalanceWarningThreshold(
            providerID: providerID,
            xiaomiUsageKind: xiaomiUsageKind
        )
        if showsBalanceThreshold, !threshold.isEmpty, Decimal(string: threshold) == nil {
            errors[.balanceWarningThreshold] = "请输入有效数字"
        }

        switch providerID {
        case .routin:
            let key = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            if key.isEmpty {
                errors[.apiKey] = "请输入 plan Key"
            } else if !KeyCredentialPolicy.hasValidPrefix(key)
                || !KeyCredentialPolicy.hasSufficientSecretPayload(key) {
                errors[.apiKey] = "plan Key 必须以 plan- 开头并包含完整密钥"
            }
        case .deepseek, .glm, .commandCode, .opencode:
            if apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors[.apiKey] = "请输入 API Key"
            }
        case .volcengine:
            if accessKeyID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors[.accessKeyID] = "请输入 AccessKey ID"
            }
            if secretAccessKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors[.secretAccessKey] = "请输入 SecretAccessKey"
            }
        case .newAPI:
            let rawURL = newAPIBaseURL.trimmingCharacters(in: .whitespacesAndNewlines)
            if let url = URL(string: rawURL), url.scheme != nil, url.host != nil {
                errors.removeValue(forKey: .newAPIBaseURL)
            } else {
                errors[.newAPIBaseURL] = "请输入有效的 API 地址"
            }

            let userID = newAPIUserID.trimmingCharacters(in: .whitespacesAndNewlines)
            if Int(userID).map({ $0 > 0 }) != true {
                errors[.newAPIUserID] = "请输入大于 0 的数字用户 ID"
            }

            if apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors[.apiKey] = "请输入用户访问令牌"
            }
        case .xiaomi:
            let key = apiKey
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()
            if key.isEmpty {
                errors[.apiKey] = "请输入 API Key"
            } else if xiaomiUsageKind == "plan", !key.hasPrefix("tp-") {
                errors[.apiKey] = "Token Plan 需要使用 tp- 开头的 Key"
            } else if xiaomiUsageKind == "api", !key.hasPrefix("sk-") {
                errors[.apiKey] = "API 按量需要使用 sk- 开头的 Key"
            }
        }

        return errors
    }

    private var isDirty: Bool {
        if providerID != initialProviderID {
            return true
        }
        if name != initialName {
            return true
        }

        let initialWebsite = (initialMetadata["websiteURL"] ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let suggestedWebsite = CredentialWebsiteDefaults.suggestedURL(
            for: initialProviderID,
            customBaseURL: initialMetadata["baseURL"] ?? ""
        )
        let expectedInitialWebsite = initialWebsite.isEmpty
            ? suggestedWebsite
            : initialWebsite

        let initialAPIKey = initialProviderID == .volcengine ? "" : initialSecret
        let initialSecretKey = initialProviderID == .volcengine ? initialSecret : ""

        return websiteURL != expectedInitialWebsite
            || apiKey != initialAPIKey
            || accessKeyID != (initialMetadata["accessKeyID"] ?? "")
            || secretAccessKey != initialSecretKey
            || region != (initialMetadata["region"] ?? "cn-beijing")
            || balanceWarningThreshold != (initialMetadata["balanceWarningThreshold"] ?? "")
            || planType != (initialMetadata["planType"] ?? "agent")
            || newAPIBaseURL != (initialMetadata["baseURL"] ?? "")
            || newAPIUserID != (initialMetadata["userID"] ?? "")
            || xiaomiUsageKind != (initialMetadata["usageKind"] ?? "api")
    }

    private var providerName: String {
        displayName(for: providerID)
    }

    private var providerAccent: Color {
        Color.accentColor
    }

    private var websitePrompt: String {
        providerID == .newAPI
            ? "https://your-new-api.com"
            : providerID.defaultConsoleURL ?? "https://"
    }

    private var apiKeyLabel: String {
        if providerID == .opencode {
            return "OpenCode Console API Key"
        }
        return "API Key"
    }

    private var apiKeyPrompt: String {
        switch providerID {
        case .routin:
            return "粘贴 plan- 开头的 Key"
        case .deepseek:
            return "粘贴 DeepSeek API Key"
        case .glm:
            return "粘贴 GLM API Key"
        case .commandCode:
            return "粘贴 Command Code API Key"
        case .opencode:
            return "粘贴 OpenCode Console API Key"
        case .newAPI, .volcengine, .xiaomi:
            return "粘贴 API Key"
        }
    }

    private var xiaomiAPIKeyPrompt: String {
        xiaomiUsageKind == "plan"
            ? "粘贴 tp- 开头的 Token Plan Key"
            : "粘贴 sk- 开头的 API Key"
    }

    private var credentialKindLabel: String {
        switch providerID {
        case .routin, .newAPI, .commandCode, .opencode:
            return "Bearer Token"
        case .deepseek, .glm:
            return "API Key"
        case .volcengine:
            return planType == "coding" ? "Coding Plan / AK-SK" : "Agent Plan / AK-SK"
        case .xiaomi:
            return xiaomiUsageKind == "plan" ? "Token Plan" : "API 按量"
        }
    }

    private var credentialGroupSubtitle: String? {
        credentialKindLabel.caseInsensitiveCompare(apiKeyLabel) == .orderedSame
            ? nil
            : credentialKindLabel
    }

    private func displayName(for id: ProviderID) -> String {
        ProviderRegistry.builtInDescriptors.first(where: { $0.id == id })?.displayName ?? id.rawValue
    }

    private func submit() async {
        guard !isSaving else { return }
        hasAttemptedSubmit = true
        errorMessage = nil

        let errors = fieldErrors
        if let firstError = Field.allCases.first(where: { errors[$0] != nil }) {
            requestedScrollField = firstError
            return
        }

        do {
            let input = try CredentialEditorValidation.validate(
                providerID: providerID,
                name: name,
                apiKey: apiKey,
                accessKeyID: accessKeyID,
                secretAccessKey: secretAccessKey,
                region: region,
                balanceWarningThreshold: balanceWarningThreshold,
                planType: planType,
                newAPIBaseURL: newAPIBaseURL,
                newAPIUserID: newAPIUserID,
                xiaomiUsageKind: xiaomiUsageKind,
                websiteURL: websiteURL
            )
            isSaving = true
            errorMessage = nil
            _ = try await save(input)
            isSaving = false
            onSaved()
            onClose()
        } catch is CancellationError {
            isSaving = false
        } catch {
            isSaving = false
            errorMessage = error.localizedDescription
        }
    }

    private var newAPIProfileURL: URL? {
        var components = URLComponents(string: newAPIBaseURL.trimmingCharacters(in: .whitespacesAndNewlines))
        guard components?.scheme != nil, components?.host != nil else { return nil }
        while components?.path.hasSuffix("/") == true {
            components?.path.removeLast()
        }
        if components?.path == "/api" {
            components?.path = ""
        }
        components?.path += "/profile"
        return components?.url
    }

    private func close() {
        guard !isSaving else { return }
        if isDirty {
            showsDiscardConfirmation = true
        } else {
            onClose()
        }
    }
}

private struct CredentialSegmentOption: Identifiable {
    let id: String
    let title: String
}

private struct CredentialSegmentedField: View {
    let label: String
    @Binding var selection: String
    let options: [CredentialSegmentOption]

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            SettingsFieldLabel(text: label)

            SettingsSegmentedControl(
                options: options.map(\.id),
                selection: $selection,
                title: { id in
                    options.first(where: { $0.id == id })?.title ?? id
                },
                accessibilityLabel: label
            )
        }
    }
}
