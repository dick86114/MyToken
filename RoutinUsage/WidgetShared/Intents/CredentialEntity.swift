import AppIntents
import Foundation

typealias WidgetSnapshotLoader = @Sendable () throws -> WidgetSnapshotBundle?

enum WidgetSharedSnapshotLoader {
    static let load: WidgetSnapshotLoader = {
        guard let directory = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.cc.idickies.mytoken.widgets"
        ) else {
            return nil
        }
        return try WidgetSnapshotStore(directory: directory).load()
    }
}

struct CredentialEntity: AppEntity, Identifiable, Equatable, Hashable, Sendable {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "凭证")
    static let defaultQuery = CredentialEntityQuery()

    let id: String
    let name: String
    let providerName: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)", subtitle: "\(providerName)")
    }
}

struct CredentialEntityQuery: EntityQuery {
    private let loader: WidgetSnapshotLoader

    init() {
        loader = WidgetSharedSnapshotLoader.load
    }

    init(loader: @escaping WidgetSnapshotLoader) {
        self.loader = loader
    }

    func entities(for identifiers: [CredentialEntity.ID]) async throws -> [CredentialEntity] {
        let requested = Set(identifiers)
        return try enabledCredentials().filter { requested.contains($0.id) }
    }

    func suggestedEntities() async throws -> [CredentialEntity] {
        try enabledCredentials()
    }

    func defaultResult() async -> CredentialEntity? {
        try? enabledCredentials().first
    }

    private func enabledCredentials() throws -> [CredentialEntity] {
        let credentials = (try loader()?.credentials ?? [])
            .filter(\.isEnabled)
            .sorted { $0.sortOrder < $1.sortOrder }
        return credentials.map {
            CredentialEntity(id: $0.id.uuidString, name: $0.name, providerName: $0.providerName)
        }
    }
}
