import SwiftUI
import WidgetKit

struct MyTokenCredentialWidget: Widget {
    private let kind = "MyTokenCredentialWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: CredentialWidgetConfigurationIntent.self,
            provider: CredentialWidgetIntentProvider()
        ) { entry in
            CredentialWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("MyToken 凭证")
        .description("查看一个凭证的主用量、次要指标和更新时间。")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

private struct CredentialWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: CredentialWidgetEntry

    var body: some View {
        Group {
            if let credential = entry.credential {
                let presentation = WidgetPresentationBuilder.makeCredentialPresentation(
                    credential: credential,
                    style: entry.style.displayStyle,
                    size: WidgetSizeKind(family: family),
                    fixedMetricID: entry.primaryMetricID
                )
                if let item = presentation.credentials.first {
                    WidgetCredentialContent(item: item, style: presentation.style)
                } else {
                    WidgetEmptyView()
                }
            } else {
                WidgetEmptyView()
            }
        }
        .containerBackground(for: .widget) {
            Color(nsColor: .windowBackgroundColor)
        }
        .widgetURL(entry.deepLinkURL)
    }
}
