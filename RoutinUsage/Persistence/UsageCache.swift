import Foundation

protocol UsageCaching: Sendable {
    func load(for keyID: UUID) throws -> UsageSnapshot?
    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws
    func delete(for keyID: UUID) throws
}

final class UsageCache: UsageCaching, @unchecked Sendable {
    private let defaults: UserDefaults
    private let storageKey: String

    init(defaults: UserDefaults = .standard, storageKey: String = "usageSnapshots") {
        self.defaults = defaults
        self.storageKey = storageKey
    }

    func load(for keyID: UUID) throws -> UsageSnapshot? {
        try snapshots()[keyID]
    }

    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws {
        var values = try snapshots()
        values[keyID] = snapshot
        defaults.set(try JSONEncoder().encode(values), forKey: storageKey)
    }

    func delete(for keyID: UUID) throws {
        var values = try snapshots()
        values.removeValue(forKey: keyID)
        defaults.set(try JSONEncoder().encode(values), forKey: storageKey)
    }

    private func snapshots() throws -> [UUID: UsageSnapshot] {
        guard let data = defaults.data(forKey: storageKey) else {
            return [:]
        }
        return try JSONDecoder().decode([UUID: UsageSnapshot].self, from: data)
    }
}

enum UsageFreshness {
    static func isStale(lastSuccess: Date, now: Date, refreshMinutes: Int) -> Bool {
        let threshold = max(300, TimeInterval(refreshMinutes * 120))
        return now.timeIntervalSince(lastSuccess) >= threshold
    }
}
