import Foundation

struct KeyConfiguration: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let name: String
    let keySuffix: String
    let sortOrder: Int
    let isEnabled: Bool
    let providerID: ProviderID
    let credentialKind: CredentialKind
    let metadata: [String: String]

    init(
        id: UUID,
        name: String,
        keySuffix: String,
        sortOrder: Int,
        isEnabled: Bool = true,
        providerID: ProviderID = .routin,
        credentialKind: CredentialKind = .bearerAPIKey,
        metadata: [String: String] = [:]
    ) {
        self.id = id
        self.name = name
        self.keySuffix = keySuffix
        self.sortOrder = sortOrder
        self.isEnabled = isEnabled
        self.providerID = providerID
        self.credentialKind = credentialKind
        self.metadata = metadata
    }

    private enum CodingKeys: String, CodingKey {
        case id
        case name
        case keySuffix
        case sortOrder
        case isEnabled
        case providerID
        case credentialKind
        case metadata
    }

    var websiteURL: URL? {
        guard let rawValue = metadata["websiteURL"], !rawValue.isEmpty else {
            return nil
        }
        return URL(string: rawValue)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        keySuffix = try container.decode(String.self, forKey: .keySuffix)
        sortOrder = try container.decode(Int.self, forKey: .sortOrder)
        isEnabled = try container.decodeIfPresent(Bool.self, forKey: .isEnabled) ?? true
        providerID = try container.decodeIfPresent(ProviderID.self, forKey: .providerID) ?? .routin
        credentialKind = try container.decodeIfPresent(CredentialKind.self, forKey: .credentialKind) ?? .bearerAPIKey
        metadata = try container.decodeIfPresent([String: String].self, forKey: .metadata) ?? [:]
    }
}

enum KeyCredentialPolicy {
    static let secretPrefix = "plan-"
    static let minimumVisibleSuffixLength = 4

    static func isSafeDisplayName(_ name: String) -> Bool {
        let normalized = name.trimmingCharacters(in: .whitespacesAndNewlines)
        return !normalized.isEmpty
            && !normalized.lowercased().hasPrefix(secretPrefix)
    }

    static func hasValidPrefix(_ secret: String) -> Bool {
        secret.hasPrefix(secretPrefix)
    }

    static func hasSufficientSecretPayload(_ secret: String) -> Bool {
        guard hasValidPrefix(secret) else {
            return false
        }
        return secret.dropFirst(secretPrefix.count).count >= minimumVisibleSuffixLength
    }

    static func metadataSuffix(for secret: String) -> String {
        guard hasSufficientSecretPayload(secret) else {
            return ""
        }
        return String(secret.suffix(minimumVisibleSuffixLength))
    }

    static func sanitizedMetadataSuffix(
        persistedSuffix: String,
        storedSecret: String?
    ) -> String {
        if let storedSecret {
            return metadataSuffix(for: storedSecret)
        }
        guard
            persistedSuffix.count == minimumVisibleSuffixLength,
            !isLegacyShortSecretSuffix(persistedSuffix)
        else {
            return ""
        }
        return persistedSuffix
    }

    static func isLegacyShortSecretSuffix(_ suffix: String) -> Bool {
        guard suffix.count == minimumVisibleSuffixLength else {
            return false
        }
        return suffix.hasPrefix("an-")
            || suffix.hasPrefix("n-")
            || suffix.hasPrefix("-")
    }

    static func safeDisplayName(_ name: String) -> String {
        isSafeDisplayName(name)
            ? name.trimmingCharacters(in: .whitespacesAndNewlines)
            : "未命名 Key"
    }
}

extension KeyConfiguration {
    var displayName: String {
        KeyCredentialPolicy.safeDisplayName(name)
    }
}
