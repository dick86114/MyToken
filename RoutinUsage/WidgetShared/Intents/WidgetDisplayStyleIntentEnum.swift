import AppIntents

enum WidgetDisplayStyleIntentEnum: String, AppEnum, CaseIterable, Sendable {
    case automatic
    case focusRing
    case scanBand
    case signalTiles

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "视觉风格")
    static let caseDisplayRepresentations: [WidgetDisplayStyleIntentEnum: DisplayRepresentation] = [
        .automatic: DisplayRepresentation(title: "自动"),
        .focusRing: DisplayRepresentation(title: "聚焦环"),
        .scanBand: DisplayRepresentation(title: "扫描带"),
        .signalTiles: DisplayRepresentation(title: "信号格")
    ]

    var displayStyle: WidgetDisplayStyle {
        switch self {
        case .automatic: .auto
        case .focusRing: .focusRing
        case .scanBand: .scanBand
        case .signalTiles: .signalTiles
        }
    }
}
