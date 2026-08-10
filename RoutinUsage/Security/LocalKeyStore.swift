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

// 仅用于兼容尚未迁移的测试注入点，生产构造不会使用此适配器。
struct KeychainLocalStoreAdapter: LocalKeyStoring {
    private let keychain: any KeychainStoring

    init(_ keychain: any KeychainStoring) {
        self.keychain = keychain
    }

    func save(_ secret: String, for id: UUID) throws {
        try keychain.save(secret, for: id)
    }

    func read(for id: UUID) throws -> String? {
        try keychain.read(for: id)
    }

    func delete(for id: UUID) throws {
        try keychain.delete(for: id)
    }
}
