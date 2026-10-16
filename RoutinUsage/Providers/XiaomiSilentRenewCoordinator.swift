import CryptoKit
import Foundation

/// 串行化同一小米凭证的静默续期，并为连续失败建立最长 6 小时的退避。
@MainActor
final class XiaomiSilentRenewCoordinator {
    typealias RenewOperation = @Sendable () async -> String?

    private struct FailureState {
        let fingerprint: String
        let retryAt: Date
        let failureCount: Int
    }

    private var failures: [UUID: FailureState] = [:]
    private var inFlight: [UUID: Task<String?, Never>] = [:]
    private let now: @Sendable () -> Date

    init(now: @escaping @Sendable () -> Date = { Date() }) {
        self.now = now
    }

    func renew(
        credentialID: UUID,
        previousSecret: String,
        operation: @escaping RenewOperation
    ) async -> String? {
        let fingerprint = Self.fingerprint(for: previousSecret)

        if let failure = failures[credentialID] {
            if failure.fingerprint != fingerprint {
                failures.removeValue(forKey: credentialID)
            } else if now() < failure.retryAt {
                return nil
            }
        }

        if let existing = inFlight[credentialID] {
            return await existing.value
        }

        let task = Task { await operation() }
        inFlight[credentialID] = task
        let result = await task.value
        inFlight.removeValue(forKey: credentialID)

        if let result {
            failures.removeValue(forKey: credentialID)
            return result
        }

        let previousFailureCount = failures[credentialID]?.failureCount ?? 0
        let failureCount = previousFailureCount + 1
        failures[credentialID] = FailureState(
            fingerprint: fingerprint,
            retryAt: now().addingTimeInterval(Self.backoffInterval(forFailureCount: failureCount)),
            failureCount: failureCount
        )
        return nil
    }

    func clearFailure(credentialID: UUID) {
        failures.removeValue(forKey: credentialID)
    }

    static func backoffInterval(forFailureCount failureCount: Int) -> TimeInterval {
        let boundedCount = max(1, failureCount)
        var interval: TimeInterval = 3600
        for _ in 1..<boundedCount where interval < 21600 {
            interval *= 2
        }
        return min(interval, 21600)
    }

    private static func fingerprint(for secret: String) -> String {
        SHA256.hash(data: Data(secret.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}
