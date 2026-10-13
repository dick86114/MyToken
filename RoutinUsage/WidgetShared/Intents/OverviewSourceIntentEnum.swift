import AppIntents

enum OverviewSourceIntentEnum: String, AppEnum, CaseIterable, Sendable {
    case allEnabled
    case manual

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "凭证来源")
    static let caseDisplayRepresentations: [OverviewSourceIntentEnum: DisplayRepresentation] = [
        .allEnabled: DisplayRepresentation(title: "全部启用"),
        .manual: DisplayRepresentation(title: "手动选择")
    ]
}
