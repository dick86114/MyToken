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
