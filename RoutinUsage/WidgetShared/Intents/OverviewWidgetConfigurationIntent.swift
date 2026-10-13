import AppIntents

struct OverviewWidgetConfigurationIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "MyToken 总览"
    static let description = IntentDescription("选择总览凭证来源和视觉风格。")

    @Parameter(title: "凭证来源", default: .allEnabled)
    var source: OverviewSourceIntentEnum

    @Parameter(title: "手动凭证")
    var credentials: [CredentialEntity]?

    @Parameter(title: "视觉风格", default: .automatic)
    var style: WidgetDisplayStyleIntentEnum

    init() {}

    var manualCredentialIDs: [String] {
        Array((credentials ?? []).prefix(6).map(\.id))
    }
}
