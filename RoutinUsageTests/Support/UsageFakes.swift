import Foundation
@testable import RoutinUsage

actor ScriptedUsageFetcher: UsageFetching {
    enum Response: Sendable {
        case success(UsageSnapshot?)
        case failure(UsageAPIError)
        case suspended
    }

    private var responses: [String: Response]
    private var requestCounts: [String: Int] = [:]
    private var continuations: [String: [CheckedContinuation<Result<UsageSnapshot?, UsageAPIError>, Never>]] = [:]

    init(responses: [String: Response]) {
        self.responses = responses
    }

    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot? {
        requestCounts[apiKey, default: 0] += 1
        switch responses[apiKey] ?? .failure(.invalidResponse) {
        case let .success(snapshot):
            return snapshot
        case let .failure(error):
            throw error
        case .suspended:
            let result = await withCheckedContinuation { continuation in
                continuations[apiKey, default: []].append(continuation)
            }
            return try result.get()
        }
    }

    func requestCount(for apiKey: String) -> Int {
        requestCounts[apiKey, default: 0]
    }

    func setResponse(_ response: Response, for apiKey: String) {
        responses[apiKey] = response
    }

    func waitUntilRequested(_ apiKey: String) async {
        while requestCounts[apiKey, default: 0] == 0 {
            await Task.yield()
        }
    }

    func resume(_ apiKey: String, with result: Result<UsageSnapshot?, UsageAPIError>) {
        let pending = continuations.removeValue(forKey: apiKey) ?? []
        for continuation in pending {
            continuation.resume(returning: result)
        }
    }
}

actor CancellationAwareUsageFetcher: UsageFetching {
    private let suspension = CancellationSuspension()
    private let initialFailureCount: Int
    private var requestCounts: [String: Int] = [:]

    init(initialFailureCount: Int = 0) {
        self.initialFailureCount = initialFailureCount
    }

    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot? {
        requestCounts[apiKey, default: 0] += 1
        if requestCounts[apiKey, default: 0] <= initialFailureCount {
            throw UsageAPIError.transport
        }
        try await suspension.wait()
        return nil
    }

    func waitUntilRequested(_ apiKey: String, count expectedCount: Int = 1) async {
        while requestCounts[apiKey, default: 0] < expectedCount {
            await Task.yield()
        }
    }
}

private final class CancellationSuspension: @unchecked Sendable {
    private let lock = NSLock()
    private var continuation: CheckedContinuation<Void, Error>?
    private var isCancelled = false

    func wait() async throws {
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                let shouldCancel = lock.withLock {
                    if isCancelled {
                        return true
                    }
                    self.continuation = continuation
                    return false
                }
                if shouldCancel {
                    continuation.resume(throwing: CancellationError())
                }
            }
        } onCancel: {
            let pending = self.lock.withLock {
                self.isCancelled = true
                defer { self.continuation = nil }
                return self.continuation
            }
            pending?.resume(throwing: CancellationError())
        }
    }
}

final class InMemoryUsageCache: UsageCaching, @unchecked Sendable {
    private let lock = NSLock()
    private var snapshots: [UUID: UsageSnapshot]

    init(snapshots: [UUID: UsageSnapshot] = [:]) {
        self.snapshots = snapshots
    }

    func load(for keyID: UUID) throws -> UsageSnapshot? {
        lock.withLock { snapshots[keyID] }
    }

    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws {
        lock.withLock {
            snapshots[keyID] = snapshot
        }
    }

    func delete(for keyID: UUID) throws {
        _ = lock.withLock {
            snapshots.removeValue(forKey: keyID)
        }
    }
}

actor NotificationSenderFake: NotificationSending {
    private let authorized: Bool
    private var alerts: [UsageAlert] = []

    init(authorized: Bool = true) {
        self.authorized = authorized
    }

    func requestAuthorization() async throws -> Bool {
        authorized
    }

    func send(_ alert: UsageAlert) async throws {
        alerts.append(alert)
    }

    func sentAlerts() -> [UsageAlert] {
        alerts
    }
}

actor BlockingNotificationSender: NotificationSending {
    private let blockedKeyID: UUID
    private var sendingKeyIDs: Set<UUID> = []
    private var continuations: [UUID: [CheckedContinuation<Void, Never>]] = [:]

    init(blockedKeyID: UUID) {
        self.blockedKeyID = blockedKeyID
    }

    func requestAuthorization() async throws -> Bool {
        true
    }

    func send(_ alert: UsageAlert) async throws {
        guard alert.keyID == blockedKeyID else {
            return
        }
        await withCheckedContinuation { continuation in
            sendingKeyIDs.insert(alert.keyID)
            continuations[alert.keyID, default: []].append(continuation)
        }
    }

    func waitUntilSending(keyID: UUID) async {
        while !sendingKeyIDs.contains(keyID) {
            await Task.yield()
        }
    }

    func isSending(keyID: UUID) -> Bool {
        sendingKeyIDs.contains(keyID)
    }

    func resume(keyID: UUID) {
        sendingKeyIDs.remove(keyID)
        let pending = continuations.removeValue(forKey: keyID) ?? []
        for continuation in pending {
            continuation.resume()
        }
    }
}

actor BlockingAuthorizationNotificationSender: NotificationSending {
    private var authorizationRequests = 0
    private var authorizationContinuation: CheckedContinuation<Bool, Never>?
    private var alerts: [UsageAlert] = []

    func requestAuthorization() async throws -> Bool {
        authorizationRequests += 1
        return await withCheckedContinuation { continuation in
            authorizationContinuation = continuation
        }
    }

    func send(_ alert: UsageAlert) async throws {
        alerts.append(alert)
    }

    func waitUntilAuthorizationRequested() async {
        while authorizationRequests == 0 {
            await Task.yield()
        }
    }

    func authorizationRequestCount() -> Int {
        authorizationRequests
    }

    func resolveAuthorization(_ authorized: Bool) {
        authorizationContinuation?.resume(returning: authorized)
        authorizationContinuation = nil
    }

    func sentAlerts() -> [UsageAlert] {
        alerts
    }
}

struct UsageFakeError: Error, Equatable, Sendable {}

final class DeleteFailingUsageCache: UsageCaching, @unchecked Sendable {
    private let wrapped: InMemoryUsageCache

    init(snapshots: [UUID: UsageSnapshot] = [:]) {
        wrapped = InMemoryUsageCache(snapshots: snapshots)
    }

    func load(for keyID: UUID) throws -> UsageSnapshot? {
        try wrapped.load(for: keyID)
    }

    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws {
        try wrapped.save(snapshot, for: keyID)
    }

    func delete(for keyID: UUID) throws {
        throw UsageFakeError()
    }
}
