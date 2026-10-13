import SwiftUI
import WidgetKit

struct MyTokenCredentialWidget: Widget {
    private let kind = "MyTokenCredentialWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetSnapshotProvider()) { entry in
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
                    style: .auto,
                    size: WidgetSizeKind(family: family)
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
