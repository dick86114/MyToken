import CryptoKit
import Foundation

struct RoutinAccountIdentity: Codable, Equatable, Sendable {
    let fingerprint: String
    let displayName: String

    static func make(email: String, displayName: String) -> RoutinAccountIdentity {
        let normalizedEmail = email
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        let digest = SHA256.hash(data: Data(normalizedEmail.utf8))
        return RoutinAccountIdentity(
            fingerprint: digest.map { String(format: "%02x", $0) }.joined(),
            displayName: displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }
}

struct CodexGroupDetectionRecord: Codable, Equatable, Sendable {
    let keyID: UUID
    let accountFingerprint: String
    let accountDisplayName: String
    let groupName: String
    let detectedAt: Date
}
