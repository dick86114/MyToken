import AppIntents

struct CredentialWidgetConfigurationIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "MyToken 凭证"
    static let description = IntentDescription("选择要显示的一个凭证、视觉风格和主指标。")

    @Parameter(title: "凭证")
    var credential: CredentialEntity?

    @Parameter(title: "视觉风格", default: .automatic)
    var style: WidgetDisplayStyleIntentEnum

    @Parameter(title: "主指标")
    var primaryMetric: PrimaryMetricEntity?

    init() {}
}
