import Foundation

protocol UsageCaching: Sendable {
    func load(for keyID: UUID) throws -> UsageSnapshot?
    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws
    func delete(for keyID: UUID) throws
}

final class UsageCache: UsageCaching, @unchecked Sendable {
    static let currentSchemaVersion = 2
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
        let envelope = UsageCacheEnvelope(
            schemaVersion: Self.currentSchemaVersion,
            snapshots: values
        )
        defaults.set(try JSONEncoder().encode(envelope), forKey: storageKey)
    }

    func delete(for keyID: UUID) throws {
        var values = try snapshots()
        values.removeValue(forKey: keyID)
        if values.isEmpty {
            defaults.removeObject(forKey: storageKey)
        } else {
            defaults.set(try JSONEncoder().encode(values), forKey: storageKey)
        }
    }

    private func snapshots() throws -> [UUID: UsageSnapshot] {
        guard let data = defaults.data(forKey: storageKey) else {
            return [:]
        }
        do {
            if let envelope = try? JSONDecoder().decode(UsageCacheEnvelope.self, from: data) {
                return envelope.snapshots
            }
            // 兼容 v1 直接保存字典的缓存格式。
            return try JSONDecoder().decode([UUID: UsageSnapshot].self, from: data)
        } catch {
            defaults.removeObject(forKey: storageKey)
            return [:]
        }
    }
}

private struct UsageCacheEnvelope: Codable {
    let schemaVersion: Int
    let snapshots: [UUID: UsageSnapshot]
}

enum UsageFreshness {
    static func isStale(lastSuccess: Date, now: Date, refreshMinutes: Int) -> Bool {
        let threshold = max(60, TimeInterval(refreshMinutes * 60))
        return now.timeIntervalSince(lastSuccess) >= threshold
    }
}
