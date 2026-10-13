import SwiftUI
import WidgetKit

struct MyTokenOverviewWidget: Widget {
    private let kind = "MyTokenOverviewWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: OverviewWidgetConfigurationIntent.self,
            provider: OverviewWidgetIntentProvider()
        ) { entry in
            OverviewWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("MyToken 总览")
        .description("快速查看多个凭证的用量与风险状态。")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

private struct OverviewWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: OverviewWidgetEntry

    var body: some View {
        let presentation = WidgetPresentationBuilder.makeOverviewPresentation(
            credentials: entry.credentials,
            style: entry.style.displayStyle,
            size: WidgetSizeKind(family: family)
        )
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text("MyToken 总览")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer(minLength: 4)
                if let generatedAt = entry.credentials.first?.fetchedAt {
                    Text(generatedAt, style: .time)
                        .font(.system(size: 8))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            if presentation.credentials.isEmpty {
                WidgetEmptyView()
            } else {
                ForEach(presentation.credentials) { credential in
                    Link(destination: credentialURL(credential.id)) {
                        OverviewCredentialRow(credential: credential)
                    }
                    .buttonStyle(.plain)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(2)
        .containerBackground(for: .widget) {
            Color(nsColor: .windowBackgroundColor)
        }
        .widgetURL(URL(string: "mytoken://open"))
    }

    private func credentialURL(_ id: UUID) -> URL {
        URL(string: "mytoken://credential/\(id.uuidString)?refresh=0")!
    }
}

private struct OverviewCredentialRow: View {
    let credential: WidgetCredentialPresentation

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 1) {
                Text(credential.name)
                    .font(.system(size: 11, weight: .semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)
                Text(credential.providerName)
                    .font(.system(size: 8))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let primary = credential.primary {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(primary.valueText)
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .lineLimit(1)
                    Text(primary.label)
                        .font(.system(size: 8))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .frame(width: 82, alignment: .trailing)
            }
        }
        .padding(.vertical, 2)
    }
}
