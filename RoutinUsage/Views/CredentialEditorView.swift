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
        case .deepseek, .glm:
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
        VStack(alignment: .leading, spacing: 20) {
            header

            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 14) {
                    providerField
                        .frame(maxWidth: .infinity)
                    CredentialInputField(
                        label: "名称",
                        text: $name,
                        prompt: "例如：工作账号",
                        accentColor: providerAccent,
                        autoFocus: true
                    )
                    .frame(maxWidth: .infinity)
                }

                CredentialInputField(
                    label: "官网 / 后台地址（可选）",
                    text: $websiteURL,
                    prompt: websitePrompt,
                    accentColor: providerAccent,
                    helpText: "显示在用量详情和分享卡片中，可替换为你的自定义地址。"
                )
            }

            Divider()
                .overlay(fieldBorder.opacity(0.8))

            providerCredentialFields

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                    .font(.callout)
                    .foregroundStyle(.red)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color.red.opacity(colorScheme == .dark ? 0.12 : 0.07))
                    }
            }

            footer
        }
        .padding(24)
        .frame(width: 680)
        .fixedSize(horizontal: false, vertical: true)
        .onChange(of: providerID) { _, newProviderID in
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

    private var header: some View {
        HStack(spacing: 12) {
            ProviderLogoMark(providerID: providerID, size: 38, cornerRadius: 10)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.title3.weight(.semibold))
                Text("配置 \(providerName) 的查询凭证")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button {
                close()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .frame(width: 28, height: 28)
                    .background {
                        Circle()
                            .fill(Color.primary.opacity(colorScheme == .dark ? 0.10 : 0.06))
                    }
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("关闭")
            .accessibilityLabel("关闭")
        }
    }

    private var providerField: some View {
        VStack(alignment: .leading, spacing: 6) {
            CredentialFieldLabel(text: "供应商")

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
                .frame(height: 40)
                .background {
                    fieldShape.fill(fieldFill)
                }
                .overlay {
                    fieldShape.strokeBorder(fieldBorder, lineWidth: 1)
                }
                .contentShape(fieldShape)
            }
            .buttonStyle(.plain)
            .menuIndicator(.hidden)
            .accessibilityLabel("供应商，当前：\(providerName)")
        }
    }

    private var providerCredentialFields: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                Text("接入凭证")
                    .font(.headline)
                Spacer()
                Text(credentialKindLabel)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }

            credentialFieldsContent
        }
    }

    @ViewBuilder
    private var credentialFieldsContent: some View {
        if providerID == .newAPI {
            CredentialInputField(
                label: "New API 地址",
                text: $newAPIBaseURL,
                prompt: "https://your-new-api.com",
                accentColor: providerAccent
            )

            CredentialInputField(
                label: "数字用户 ID",
                text: $newAPIUserID,
                prompt: "例如：1024",
                accentColor: providerAccent
            )

            HStack(spacing: 8) {
                CredentialHint(text: "只支持数字用户 ID，个人资料页会显示该值。")
                Spacer()
                Button {
                    if let url = newAPIProfileURL {
                        openURL(url)
                    }
                } label: {
                    Label("打开个人资料", systemImage: "arrow.up.right.square")
                }
                .buttonStyle(.plain)
                .font(.caption.weight(.medium))
                .foregroundStyle(newAPIProfileURL == nil ? Color.secondary.opacity(0.45) : providerAccent)
                .disabled(newAPIProfileURL == nil)
            }

            CredentialInputField(
                label: "用户访问令牌",
                text: $apiKey,
                prompt: "粘贴 New API 用户访问令牌",
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                accentColor: providerAccent
            )

            CredentialInputField(
                label: "低额度预警值（可选）",
                text: $balanceWarningThreshold,
                prompt: "例如：10",
                suffix: "额度",
                accentColor: providerAccent
            )
        } else if providerID == .volcengine {
            CredentialSegmentedField(
                label: "计划类型",
                selection: $planType,
                options: [
                    CredentialSegmentOption(id: "agent", title: "Agent Plan"),
                    CredentialSegmentOption(id: "coding", title: "Coding Plan")
                ],
                accentColor: providerAccent
            )

            HStack(alignment: .top, spacing: 14) {
                CredentialInputField(
                    label: "AccessKey ID",
                    text: $accessKeyID,
                    prompt: "AKLT...",
                    accentColor: providerAccent
                )
                .frame(maxWidth: .infinity)

                CredentialInputField(
                    label: "区域（可选）",
                    text: $region,
                    prompt: "cn-beijing",
                    accentColor: providerAccent
                )
                .frame(maxWidth: .infinity)
            }

            CredentialInputField(
                label: "SecretAccessKey",
                text: $secretAccessKey,
                prompt: "粘贴 SecretAccessKey",
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: secretAccessKey),
                accentColor: providerAccent
            )
        } else if providerID == .xiaomi {
            CredentialSegmentedField(
                label: "查询方式",
                selection: $xiaomiUsageKind,
                options: [
                    CredentialSegmentOption(id: "api", title: "API 按量"),
                    CredentialSegmentOption(id: "plan", title: "Token Plan")
                ],
                accentColor: providerAccent
            )

            CredentialInputField(
                label: "API Key",
                text: $apiKey,
                prompt: xiaomiAPIKeyPrompt,
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                accentColor: providerAccent
            )

            CredentialHint(text: "API 按量使用 sk- 开头的普通 API Key；Token Plan 使用 tp- 开头的专用 API Key。")

            if xiaomiUsageKind == "api" {
                CredentialInputField(
                    label: "低余额预警值（可选）",
                    text: $balanceWarningThreshold,
                    prompt: "例如：20",
                    suffix: "元",
                    accentColor: providerAccent
                )
            }
        } else {
            CredentialInputField(
                label: apiKeyLabel,
                text: $apiKey,
                prompt: apiKeyPrompt,
                isSecret: true,
                visibility: $isSecretVisible,
                canReveal: CredentialVisibility.canToggle(secret: apiKey),
                accentColor: providerAccent
            )

            if providerID == .deepseek {
                CredentialInputField(
                    label: "低余额预警值（可选）",
                    text: $balanceWarningThreshold,
                    prompt: "例如：10",
                    suffix: "元",
                    accentColor: providerAccent
                )
            }
        }
    }

    private var footer: some View {
        HStack(spacing: 12) {
            Label("凭证仅保存在本机", systemImage: "lock.fill")
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            Button("取消") {
                close()
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
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

    private var providerName: String {
        displayName(for: providerID)
    }

    private var providerAccent: Color {
        ProviderTheme.accentColor(for: providerID)
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

    private var fieldFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.055) : Color.black.opacity(0.035)
    }

    private var fieldBorder: Color {
        colorScheme == .dark ? Color.white.opacity(0.16) : Color.black.opacity(0.14)
    }

    private var fieldShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
    }

    private func displayName(for id: ProviderID) -> String {
        ProviderRegistry.builtInDescriptors.first(where: { $0.id == id })?.displayName ?? id.rawValue
    }

    private func submit() async {
        guard !isSaving else { return }
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
            close()
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
        onClose()
    }
}

private struct CredentialFieldLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(.secondary)
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
    let accentColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            CredentialFieldLabel(text: label)

            Picker("", selection: $selection) {
                ForEach(options) { option in
                    Text(option.title)
                        .tag(option.id)
                }
            }
            .labelsHidden()
            .pickerStyle(.segmented)
            .tint(accentColor)
        }
    }
}

private struct CredentialHint: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 6) {
            Image(systemName: "info.circle")
                .font(.caption)
                .padding(.top, 1)
            Text(text)
                .fixedSize(horizontal: false, vertical: true)
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}

private struct CredentialInputField: View {
    let label: String
    @Binding var text: String
    let prompt: String
    let isSecret: Bool
    let visibility: Binding<Bool>?
    let canReveal: Bool
    let suffix: String?
    let accentColor: Color
    let helpText: String?
    let autoFocus: Bool

    @FocusState private var isFocused: Bool
    @Environment(\.colorScheme) private var colorScheme

    init(
        label: String,
        text: Binding<String>,
        prompt: String,
        isSecret: Bool = false,
        visibility: Binding<Bool>? = nil,
        canReveal: Bool = true,
        suffix: String? = nil,
        accentColor: Color = .accentColor,
        helpText: String? = nil,
        autoFocus: Bool = false
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.isSecret = isSecret
        self.visibility = visibility
        self.canReveal = canReveal
        self.suffix = suffix
        self.accentColor = accentColor
        self.helpText = helpText
        self.autoFocus = autoFocus
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            CredentialFieldLabel(text: label)

            HStack(spacing: 8) {
                inputField

                if isSecret, let visibility {
                    Button {
                        visibility.wrappedValue.toggle()
                    } label: {
                        Image(systemName: CredentialVisibility.iconName(isVisible: visibility.wrappedValue))
                            .font(.system(size: 12, weight: .medium))
                            .frame(width: 22, height: 22)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(canReveal ? Color.secondary : Color.secondary.opacity(0.35))
                    .disabled(!canReveal)
                    .help(visibility.wrappedValue ? "隐藏\(label)" : "显示\(label)")
                    .accessibilityLabel(visibility.wrappedValue ? "隐藏\(label)" : "显示\(label)")
                }

                if let suffix {
                    Rectangle()
                        .fill(fieldBorder)
                        .frame(width: 1, height: 18)
                    Text(suffix)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .padding(.trailing, 2)
                }
            }
            .padding(.horizontal, 11)
            .frame(height: 40)
            .background {
                fieldShape.fill(fieldFill)
            }
            .overlay {
                fieldShape.strokeBorder(
                    isFocused ? accentColor.opacity(0.78) : fieldBorder,
                    lineWidth: isFocused ? 1.5 : 1
                )
            }

            if let helpText {
                Text(helpText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .onAppear {
            guard autoFocus else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                isFocused = true
            }
        }
    }

    @ViewBuilder
    private var inputField: some View {
        if isSecret, let visibility, !visibility.wrappedValue {
            SecureField("", text: $text, prompt: Text(prompt))
                .textFieldStyle(.plain)
                .font(.system(size: 13))
                .focused($isFocused)
                .accessibilityLabel(label)
        } else {
            TextField("", text: $text, prompt: Text(prompt))
                .textFieldStyle(.plain)
                .font(.system(size: 13))
                .focused($isFocused)
                .accessibilityLabel(label)
        }
    }

    private var fieldFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.055) : Color.black.opacity(0.035)
    }

    private var fieldBorder: Color {
        colorScheme == .dark ? Color.white.opacity(0.16) : Color.black.opacity(0.14)
    }

    private var fieldShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
    }
}
