import AppIntents
import Foundation

struct PrimaryMetricEntity: AppEntity, Identifiable, Equatable, Hashable, Sendable {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "主指标")
    static let defaultQuery = PrimaryMetricEntityQuery()

    let id: String
    let label: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(label)")
    }
}

struct PrimaryMetricEntityQuery: EntityQuery {
    private let loader: WidgetSnapshotLoader

    init() {
        loader = WidgetSharedSnapshotLoader.load
    }

    init(loader: @escaping WidgetSnapshotLoader) {
        self.loader = loader
    }

    func entities(for identifiers: [PrimaryMetricEntity.ID]) async throws -> [PrimaryMetricEntity] {
        let requested = Set(identifiers)
        return try metrics().filter { requested.contains($0.id) }
    }

    func suggestedEntities() async throws -> [PrimaryMetricEntity] {
        try metrics()
    }

    private func metrics() throws -> [PrimaryMetricEntity] {
        let metrics = (try loader()?.credentials ?? [])
            .filter(\.isEnabled)
            .flatMap(\.metrics)
        var seen = Set<String>()
        return metrics.compactMap { metric in
            guard seen.insert(metric.id).inserted else { return nil }
            return PrimaryMetricEntity(id: metric.id, label: metric.label)
        }
    }
}
