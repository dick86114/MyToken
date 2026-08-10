import Foundation

enum KeyRepositoryError: Error, Equatable, Sendable {
    case invalidName
    case invalidSecret
    case configurationNotFound
}

final class KeyRepository {
    private let defaults: UserDefaults
    private let keychain: any KeychainStoring
    private let storageKey = "keyConfigurations"

    init(defaults: UserDefaults = .standard, keychain: any KeychainStoring = KeychainStore()) {
        self.defaults = defaults
        self.keychain = keychain
    }

    func list() -> [KeyConfiguration] {
        guard
            let data = defaults.data(forKey: storageKey),
            let configurations = try? JSONDecoder().decode([KeyConfiguration].self, from: data)
        else {
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
            keySuffix: String(secret.suffix(4)),
            sortOrder: configurations.count
        )

        try keychain.save(secret, for: configuration.id)
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
            keySuffix: String(secret.suffix(4)),
            sortOrder: configurations[index].sortOrder
        )

        try keychain.save(secret, for: id)
        configurations[index] = configuration
        persist(configurations)
        return configuration
    }

    func delete(id: UUID) throws {
        var configurations = list()
        guard configurations.contains(where: { $0.id == id }) else {
            throw KeyRepositoryError.configurationNotFound
        }

        try keychain.delete(for: id)
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
        guard secret.hasPrefix("plan-"), secret.count > "plan-".count else {
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

    private func persist(_ configurations: [KeyConfiguration]) {
        let data = try! JSONEncoder().encode(configurations)
        defaults.set(data, forKey: storageKey)
    }
}
