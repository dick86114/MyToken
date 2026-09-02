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
        planType: String = "agent"
    ) throws -> ValidatedCredentialInput {
        let normalizedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedName.isEmpty else { throw UsageStoreError.invalidName }

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
                metadata: ["planType": planType]
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
                metadata: metadata
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
                metadata: ["accessKeyID": accessKey, "region": resolvedRegion]
            )
        }
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

    @Environment(\.dismiss) private var dismiss
    @State private var providerID: ProviderID
    @State private var name: String
    @State private var apiKey: String
    @State private var accessKeyID: String
    @State private var secretAccessKey: String
    @State private var region: String
    @State private var balanceWarningThreshold: String
    @State private var planType: String
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(
        title: String = "添加凭证",
        initialProviderID: ProviderID = .deepseek,
        initialName: String = "",
        initialSecret: String = "",
        initialMetadata: [String: String] = [:],
        save: @escaping @MainActor (ValidatedCredentialInput) async throws -> KeyEditorSaveResult,
        onSaved: @escaping @MainActor () -> Void = {}
    ) {
        self.title = title
        self.initialProviderID = initialProviderID
        self.initialName = initialName
        self.initialSecret = initialSecret
        self.initialMetadata = initialMetadata
        self.save = save
        self.onSaved = onSaved
        _providerID = State(initialValue: initialProviderID)
        _name = State(initialValue: initialName)
        _apiKey = State(initialValue: initialProviderID == .volcengine ? "" : initialSecret)
        _accessKeyID = State(initialValue: initialMetadata["accessKeyID"] ?? "")
        _secretAccessKey = State(initialValue: initialProviderID == .volcengine ? initialSecret : "")
        _region = State(initialValue: initialMetadata["region"] ?? "cn-beijing")
        _balanceWarningThreshold = State(initialValue: initialMetadata["balanceWarningThreshold"] ?? "")
        _planType = State(initialValue: initialMetadata["planType"] ?? "agent")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(title).font(.title3.weight(.semibold))
            Form {
                Picker("供应商", selection: $providerID) {
                    ForEach(ProviderID.allCases) { id in
                        Text(ProviderRegistry.builtInDescriptors.first(where: { $0.id == id })?.displayName ?? id.rawValue)
                            .tag(id)
                    }
                }
                TextField("名称", text: $name)

                if providerID == .volcengine {
                    Picker("计划类型", selection: $planType) {
                        Text("Agent Plan").tag("agent")
                        Text("Coding Plan").tag("coding")
                    }
                    TextField("AccessKey ID", text: $accessKeyID)
                    SecureField("SecretAccessKey", text: $secretAccessKey)
                    TextField("区域", text: $region)
                } else {
                    SecureField(providerID == .routin ? "plan-…" : "API Key", text: $apiKey)
                    if providerID == .deepseek {
                        TextField("低余额预警值（可选）", text: $balanceWarningThreshold)
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
                Button("取消") { dismiss() }
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
        .frame(width: 480)
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
                planType: planType
            )
            isSaving = true
            errorMessage = nil
            _ = try await save(input)
            isSaving = false
            onSaved()
            dismiss()
        } catch is CancellationError {
            isSaving = false
        } catch {
            isSaving = false
            errorMessage = error.localizedDescription
        }
    }
}
