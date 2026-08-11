import Foundation

protocol LocalKeyStoring: Sendable {
    func save(_ secret: String, for id: UUID) throws
    func read(for id: UUID) throws -> String?
    func delete(for id: UUID) throws
}

final class LocalKeyStore: LocalKeyStoring, @unchecked Sendable {
    private let defaults: UserDefaults
    private let keyPrefix: String

    init(defaults: UserDefaults = .standard, keyPrefix: String = "planKey.") {
        self.defaults = defaults
        self.keyPrefix = keyPrefix
    }

    func save(_ secret: String, for id: UUID) throws {
        defaults.set(secret, forKey: storageKey(for: id))
    }

    func read(for id: UUID) throws -> String? {
        defaults.string(forKey: storageKey(for: id))
    }

    func delete(for id: UUID) throws {
        defaults.removeObject(forKey: storageKey(for: id))
    }

    private func storageKey(for id: UUID) -> String {
        "\(keyPrefix)\(id.uuidString)"
    }
}
