import Foundation

protocol UsageProvider: Sendable {
    var descriptor: ProviderDescriptor { get }
    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot?
    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot?
}

enum UsageProviderError: Error, Equatable, Sendable {
    case invalidCredential
    case unauthorized
    case rateLimited
    case transport
    case timeout
    case invalidResponse
    case providerUnavailable
}

struct ProviderRegistry: Sendable {
    private let providers: [ProviderID: any UsageProvider]

    init(providers: [any UsageProvider]) {
        self.providers = Dictionary(uniqueKeysWithValues: providers.map { ($0.descriptor.id, $0) })
    }

    func provider(for id: ProviderID) -> (any UsageProvider)? {
        providers[id]
    }

    static let builtInDescriptors: [ProviderDescriptor] = [
        ProviderDescriptor(
            id: .routin,
            displayName: "Routin",
            shortCode: "ROU",
            iconName: "routin",
            capabilities: [.quotaWindow, .tokenUsage, .resetTime, .planDetection],
            credentialSchemas: [
                .bearerAPIKey: [CredentialField(id: "secret", label: "plan Key", isSecret: true)]
            ]
        ),
        ProviderDescriptor(
            id: .deepseek,
            displayName: "DeepSeek",
            shortCode: "DS",
            iconName: "deepseek",
            capabilities: [.balance],
            credentialSchemas: [
                .apiKey: [CredentialField(id: "secret", label: "API Key", isSecret: true)]
            ]
        ),
        ProviderDescriptor(
            id: .glm,
            displayName: "GLM",
            shortCode: "GLM",
            iconName: "glm",
            capabilities: [.quotaWindow, .tokenUsage, .modelBreakdown, .toolBreakdown, .resetTime],
            credentialSchemas: [
                .apiKey: [CredentialField(id: "secret", label: "API Key", isSecret: true)]
            ]
        ),
        ProviderDescriptor(
            id: .volcengine,
            displayName: "火山方舟",
            shortCode: "VOL",
            iconName: "volcengine",
            capabilities: [.quotaWindow, .tokenUsage, .resetTime, .planDetection],
            credentialSchemas: [
                .accessKeyPair: [
                    CredentialField(id: "accessKeyID", label: "AccessKey ID", isSecret: false),
                    CredentialField(id: "secretAccessKey", label: "SecretAccessKey", isSecret: true),
                    CredentialField(id: "region", label: "区域", isSecret: false, isRequired: false, placeholder: "cn-beijing")
                ]
            ]
        )
    ]
}
