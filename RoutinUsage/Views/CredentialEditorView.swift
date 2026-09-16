import SwiftUI

struct ValidatedCredentialInput: Sendable, Equatable {
    let providerID: ProviderID
    let credentialKind: CredentialKind
    let name: String
    let secret: String
    let metadata: [String: String]
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
        case .xiaomi:
            let cookie = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !cookie.isEmpty else { throw UsageStoreError.invalidSecret }
            let usageKind = xiaomiUsageKind.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            guard usageKind == "api" || usageKind == "plan" else {
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
                secret: cookie,
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
    let xiaomiWebSession: XiaomiWebSession?
    let save: @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult
    let onSaved: @MainActor () -> Void
    let onClose: () -> Void

    @Environment(\.openURL) private var openURL
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
    @State private var showsXiaomiLogin = false
    @State private var errorMessage: String?

    init(
        title: String = "添加凭证",
        initialProviderID: ProviderID = .deepseek,
        initialName: String = "",
        initialSecret: String = "",
        initialMetadata: [String: String] = [:],
        xiaomiWebSession: XiaomiWebSession? = nil,
        save: @escaping @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult,
        onSaved: @escaping @MainActor () -> Void = {},
        onClose: @escaping () -> Void = {}
    ) {
        self.title = title
        self.initialProviderID = initialProviderID
        self.initialName = initialName
        self.initialSecret = initialSecret
        self.initialMetadata = initialMetadata
        self.xiaomiWebSession = xiaomiWebSession
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
        _websiteURL = State(initialValue: initialMetadata["websiteURL"] ?? "")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Text(title).font(.title3.weight(.semibold))
                Spacer()
                Button {
                    close()
                } label: {
                    Image(systemName: "xmark")
                }
                .buttonStyle(.borderless)
                .help("关闭")
                .accessibilityLabel("关闭")
            }
            Form {
                Picker("供应商", selection: $providerID) {
                    ForEach(ProviderID.allCases) { id in
                        Text(ProviderRegistry.builtInDescriptors.first(where: { $0.id == id })?.displayName ?? id.rawValue)
                            .tag(id)
                    }
                }
                TextField("名称", text: $name)
                    .frame(minWidth: 360)
                TextField("官网链接（可选）", text: $websiteURL)
                    .frame(minWidth: 360)

                if providerID == .newAPI {
                    TextField("New API 地址", text: $newAPIBaseURL)
                        .frame(minWidth: 360)
                    HStack(spacing: 8) {
                        TextField("数字用户 ID", text: $newAPIUserID)
                            .frame(minWidth: 360)
                        Button {
                            if let url = newAPIProfileURL {
                                openURL(url)
                            }
                        } label: {
                            Image(systemName: "person.crop.rectangle")
                        }
                        .buttonStyle(.borderless)
                        .disabled(newAPIProfileURL == nil)
                        .help("打开 New API 个人资料页查看用户 ID")
                        .accessibilityLabel("打开 New API 个人资料页查看用户 ID")
                    }
                    Text("New API 只支持数字用户 ID，不支持用户名；个人资料页会显示“用户 ID”。")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        if isSecretVisible {
                            TextField("用户访问令牌", text: $apiKey)
                                .frame(minWidth: 360)
                        } else {
                            SecureField("用户访问令牌", text: $apiKey)
                                .frame(minWidth: 360)
                        }
                        Button {
                            isSecretVisible.toggle()
                        } label: {
                            Image(systemName: CredentialVisibility.iconName(isVisible: isSecretVisible))
                        }
                        .buttonStyle(.borderless)
                        .foregroundStyle(CredentialVisibility.canToggle(secret: apiKey) ? .primary : .tertiary)
                        .disabled(!CredentialVisibility.canToggle(secret: apiKey))
                        .help(isSecretVisible ? "隐藏用户访问令牌" : "显示用户访问令牌")
                        .accessibilityLabel(isSecretVisible ? "隐藏用户访问令牌" : "显示用户访问令牌")
                    }
                    HStack(spacing: 8) {
                        TextField("低额度预警值（可选）", text: $balanceWarningThreshold)
                        Text("额度")
                            .foregroundStyle(.secondary)
                    }
                } else if providerID == .volcengine {
                    Picker("计划类型", selection: $planType) {
                        Text("Agent Plan").tag("agent")
                        Text("Coding Plan").tag("coding")
                    }
                    TextField("AccessKey ID", text: $accessKeyID)
                        .frame(minWidth: 360)
                    HStack(spacing: 8) {
                        if isSecretVisible {
                            TextField("SecretAccessKey", text: $secretAccessKey)
                                .frame(minWidth: 360)
                        } else {
                            SecureField("SecretAccessKey", text: $secretAccessKey)
                                .frame(minWidth: 360)
                        }
                        Button {
                            isSecretVisible.toggle()
                        } label: {
                            Image(systemName: CredentialVisibility.iconName(isVisible: isSecretVisible))
                        }
                        .buttonStyle(.borderless)
                        .foregroundStyle(CredentialVisibility.canToggle(secret: secretAccessKey) ? .primary : .tertiary)
                        .disabled(!CredentialVisibility.canToggle(secret: secretAccessKey))
                        .help(isSecretVisible ? "隐藏 SecretAccessKey" : "显示 SecretAccessKey")
                        .accessibilityLabel(isSecretVisible ? "隐藏 SecretAccessKey" : "显示 SecretAccessKey")
                    }
                    TextField("区域", text: $region)
                } else if providerID == .xiaomi {
                    Picker("查询方式", selection: $xiaomiUsageKind) {
                        Text("API 按量").tag("api")
                        Text("Token Plan").tag("plan")
                    }
                    HStack(spacing: 8) {
                        if isSecretVisible {
                            TextField("网页 Cookie 或 serviceToken", text: $apiKey)
                                .frame(minWidth: 360)
                        } else {
                            SecureField("网页 Cookie 或 serviceToken", text: $apiKey)
                                .frame(minWidth: 360)
                        }
                        Button {
                            isSecretVisible.toggle()
                        } label: {
                            Image(systemName: CredentialVisibility.iconName(isVisible: isSecretVisible))
                        }
                        .buttonStyle(.borderless)
                        .foregroundStyle(CredentialVisibility.canToggle(secret: apiKey) ? .primary : .tertiary)
                        .disabled(!CredentialVisibility.canToggle(secret: apiKey))
                        .help(isSecretVisible ? "隐藏 Cookie" : "显示 Cookie")
                        .accessibilityLabel(isSecretVisible ? "隐藏 Cookie" : "显示 Cookie")
                    }
                    HStack(spacing: 8) {
                        Text("登录 platform.xiaomimimo.com 后复制 Cookie；也可只粘贴 api-platform_serviceToken 的值。")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Spacer(minLength: 8)
                        Button("登录并获取 Cookie") {
                            showsXiaomiLogin = true
                        }
                        .buttonStyle(.borderless)
                        .disabled(xiaomiWebSession == nil)
                        Button("打开控制台") {
                            if let url = URL(string: "https://platform.xiaomimimo.com/console/balance") {
                                openURL(url)
                            }
                        }
                        .buttonStyle(.borderless)
                    }
                    if xiaomiUsageKind == "api" {
                        HStack(spacing: 8) {
                            TextField("低余额预警值（可选）", text: $balanceWarningThreshold)
                            Text("元")
                                .foregroundStyle(.secondary)
                        }
                    }
                } else {
                    HStack(spacing: 8) {
                        if isSecretVisible {
                            TextField(providerID == .routin ? "plan-…" : "API Key", text: $apiKey)
                                .frame(minWidth: 360)
                        } else {
                            SecureField(providerID == .routin ? "plan-…" : "API Key", text: $apiKey)
                                .frame(minWidth: 360)
                        }
                        Button {
                            isSecretVisible.toggle()
                        } label: {
                            Image(systemName: CredentialVisibility.iconName(isVisible: isSecretVisible))
                        }
                        .buttonStyle(.borderless)
                        .foregroundStyle(CredentialVisibility.canToggle(secret: apiKey) ? .primary : .tertiary)
                        .disabled(!CredentialVisibility.canToggle(secret: apiKey))
                        .help(isSecretVisible ? "隐藏凭证" : "显示凭证")
                        .accessibilityLabel(isSecretVisible ? "隐藏凭证" : "显示凭证")
                    }
                    if providerID == .deepseek {
                        HStack(spacing: 8) {
                            TextField(
                                providerID == .deepseek ? "低余额预警值（可选）" : "低额度预警值（可选）",
                                text: $balanceWarningThreshold
                            )
                            Text(providerID == .deepseek ? "元" : "额度")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .formStyle(.grouped)

            if let errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .font(.callout)
            }

            HStack {
                Button("取消") { close() }
                    .buttonStyle(.borderless)
                    .keyboardShortcut(.cancelAction)
                Spacer()
                Button {
                    Task { await submit() }
                } label: {
                    HStack(spacing: 7) {
                        if isSaving { ProgressView().controlSize(.small) }
                        Text(isSaving ? "正在验证" : "验证并保存")
                    }
                }
                .liquidGlassButton(prominent: true)
                .disabled(isSaving)
            }
        }
        .padding(28)
        .frame(width: 680)
        .fixedSize(horizontal: false, vertical: true)
        .onChange(of: providerID) { _, _ in
            isSecretVisible = false
        }
        .onChange(of: apiKey) { _, secret in
            if !CredentialVisibility.canToggle(secret: secret) {
                isSecretVisible = false
            }
        }
        .onChange(of: secretAccessKey) { _, secret in
            if !CredentialVisibility.canToggle(secret: secret) {
                isSecretVisible = false
            }
        }
        .sheet(isPresented: $showsXiaomiLogin) {
            if let xiaomiWebSession {
                XiaomiLoginWindow(
                    session: xiaomiWebSession,
                    onCaptured: { cookie in
                        apiKey = cookie
                        showsXiaomiLogin = false
                    },
                    onClose: { showsXiaomiLogin = false }
                )
            }
        }
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
