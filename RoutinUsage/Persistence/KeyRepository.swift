import Foundation

enum KeyRepositoryError: LocalizedError, Equatable, Sendable {
    case invalidName
    case invalidSecret
    case configurationNotFound

    var errorDescription: String? {
        switch self {
        case .invalidName:
            return "显示名称不能是 plan Key"
        case .invalidSecret:
            return "plan Key 内容至少需要 4 位"
        case .configurationNotFound:
            return "未找到 Key 配置"
        }
    }
}

final class KeyRepository {
    private let defaults: UserDefaults
    private let localStore: any LocalKeyStoring
    private let storageKey = "keyConfigurations"

    init(defaults: UserDefaults = .standard, localStore: (any LocalKeyStoring)? = nil) {
        self.defaults = defaults
        self.localStore = localStore ?? LocalKeyStore(defaults: defaults)
        migrateLegacyMetadata()
    }

    // 保留旧初始化标签，避免无关调用方一次性迁移；实际协议已统一为本地存储抽象。
    convenience init(defaults: UserDefaults = .standard, keychain: any KeychainStoring) {
        self.init(defaults: defaults, localStore: KeychainLocalStoreAdapter(keychain))
    }

    func list() -> [KeyConfiguration] {
        guard let configurations = storedConfigurations() else {
            return []
        }
        return configurations.sorted { $0.sortOrder < $1.sortOrder }
    }

    @discardableResult
    func add(name: String, secret: String) throws -> KeyConfiguration {
        let normalizedName = try validate(name: name, secret: secret)
        var configurations = list()
        let configuration = KeyConfiguration(
            id: UUID(),
            name: normalizedName,
            keySuffix: KeyCredentialPolicy.metadataSuffix(for: secret),
            sortOrder: configurations.count
        )

        try localStore.save(secret, for: configuration.id)
        configurations.append(configuration)
        persist(configurations)
        return configuration
    }

    @discardableResult
    func update(id: UUID, name: String, secret: String) throws -> KeyConfiguration {
        let normalizedName = try validate(name: name, secret: secret)
        var configurations = list()
        guard let index = configurations.firstIndex(where: { $0.id == id }) else {
            throw KeyRepositoryError.configurationNotFound
        }
        let configuration = KeyConfiguration(
            id: id,
            name: normalizedName,
            keySuffix: KeyCredentialPolicy.metadataSuffix(for: secret),
            sortOrder: configurations[index].sortOrder
        )

        try localStore.save(secret, for: id)
        configurations[index] = configuration
        persist(configurations)
        return configuration
    }

    func delete(id: UUID) throws {
        var configurations = list()
        guard configurations.contains(where: { $0.id == id }) else {
            throw KeyRepositoryError.configurationNotFound
        }

        try localStore.delete(for: id)
        configurations.removeAll { $0.id == id }
        persist(normalized(configurations))
    }

    func move(fromOffsets source: IndexSet, toOffset destination: Int) {
        var configurations = list()
        let validSource = source.filter { configurations.indices.contains($0) }
        guard !validSource.isEmpty, (0...configurations.count).contains(destination) else {
            return
        }

        let moving = validSource.map { configurations[$0] }
        for index in validSource.reversed() {
            configurations.remove(at: index)
        }
        let removedBeforeDestination = validSource.filter { $0 < destination }.count
        let insertionIndex = destination - removedBeforeDestination
        configurations.insert(contentsOf: moving, at: insertionIndex)
        persist(normalized(configurations))
    }

    private func validate(name: String, secret: String) throws -> String {
        let normalizedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedName.isEmpty else {
            throw KeyRepositoryError.invalidName
        }
        guard KeyCredentialPolicy.isSafeDisplayName(normalizedName) else {
            throw KeyRepositoryError.invalidName
        }
        guard
            KeyCredentialPolicy.hasValidPrefix(secret),
            KeyCredentialPolicy.hasSufficientSecretPayload(secret)
        else {
            throw KeyRepositoryError.invalidSecret
        }
        return normalizedName
    }

    private func normalized(_ configurations: [KeyConfiguration]) -> [KeyConfiguration] {
        configurations.enumerated().map { index, configuration in
            KeyConfiguration(
                id: configuration.id,
                name: configuration.name,
                keySuffix: configuration.keySuffix,
                sortOrder: index
            )
        }
    }

    private func migrateLegacyMetadata() {
        guard let configurations = storedConfigurations() else {
            return
        }

        let sanitized = configurations.map { configuration in
            let secret: String?
            do {
                secret = try localStore.read(for: configuration.id)
            } catch {
                secret = nil
            }
            return KeyConfiguration(
                id: configuration.id,
                name: KeyCredentialPolicy.safeDisplayName(configuration.name),
                keySuffix: KeyCredentialPolicy.sanitizedMetadataSuffix(
                    persistedSuffix: configuration.keySuffix,
                    keychainSecret: secret
                ),
                sortOrder: configuration.sortOrder
            )
        }
        guard sanitized != configurations else {
            return
        }
        persist(sanitized)
    }

    private func storedConfigurations() -> [KeyConfiguration]? {
        guard let data = defaults.data(forKey: storageKey) else {
            return nil
        }
        return try? JSONDecoder().decode([KeyConfiguration].self, from: data)
    }

    private func persist(_ configurations: [KeyConfiguration]) {
        let data = try! JSONEncoder().encode(configurations)
        defaults.set(data, forKey: storageKey)
    }
}
