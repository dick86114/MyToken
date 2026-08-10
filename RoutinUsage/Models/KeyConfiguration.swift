import Foundation

struct KeyConfiguration: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let name: String
    let keySuffix: String
    let sortOrder: Int
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
