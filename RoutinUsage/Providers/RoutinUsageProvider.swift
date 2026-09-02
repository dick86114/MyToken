import Foundation

struct RoutinUsageProvider: UsageProvider {
    let descriptor: ProviderDescriptor
    private let client: any UsageFetching

    init(client: any UsageFetching) {
        self.client = client
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .routin })!
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .routin, credential.kind == .bearerAPIKey else {
            throw UsageProviderError.invalidCredential
        }
        do {
            return try await client.fetchUsage(apiKey: credential.secret, now: now)?
                .assigningIdentity(providerID: .routin, credentialID: credential.credentialID)
        } catch let error as UsageAPIError {
            throw Self.map(error)
        }
    }

    private static func map(_ error: UsageAPIError) -> UsageProviderError {
        switch error {
        case .invalidKey:
            return .unauthorized
        case .transport:
            return .transport
        case .invalidResponse:
            return .invalidResponse
        case let .server(statusCode):
            if statusCode == 429 { return .rateLimited }
            if (500...599).contains(statusCode) { return .providerUnavailable }
            return .invalidResponse
        }
    }
}
