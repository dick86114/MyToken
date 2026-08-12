import Foundation

protocol CodexGroupDetectionStoring: Sendable {
    func load(for keyID: UUID) throws -> CodexGroupDetectionRecord?
    func save(_ record: CodexGroupDetectionRecord) throws
    func delete(for keyID: UUID) throws
}

final class CodexGroupDetectionRepository: CodexGroupDetectionStoring, @unchecked Sendable {
    private let defaults: UserDefaults
    private let storageKey: String

    init(
        defaults: UserDefaults = .standard,
        storageKey: String = "codexGroupDetectionRecords"
    ) {
        self.defaults = defaults
        self.storageKey = storageKey
    }

    func load(for keyID: UUID) throws -> CodexGroupDetectionRecord? {
        try records()[keyID]
    }

    func save(_ record: CodexGroupDetectionRecord) throws {
        var values = try records()
        values[record.keyID] = record
        defaults.set(try JSONEncoder().encode(values), forKey: storageKey)
    }

    func delete(for keyID: UUID) throws {
        var values = try records()
        values.removeValue(forKey: keyID)
        if values.isEmpty {
            defaults.removeObject(forKey: storageKey)
        } else {
            defaults.set(try JSONEncoder().encode(values), forKey: storageKey)
        }
    }

    private func records() throws -> [UUID: CodexGroupDetectionRecord] {
        guard let data = defaults.data(forKey: storageKey) else {
            return [:]
        }
        do {
            return try JSONDecoder().decode([UUID: CodexGroupDetectionRecord].self, from: data)
        } catch {
            defaults.removeObject(forKey: storageKey)
            return [:]
        }
    }
}
