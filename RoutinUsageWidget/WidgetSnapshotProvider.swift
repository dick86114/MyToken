import WidgetKit

struct CredentialWidgetEntry: TimelineEntry {
    let date: Date
    let credential: WidgetSnapshotCredential?
    let style: WidgetDisplayStyleIntentEnum
    let primaryMetricID: String?
    let deepLinkURL: URL?
    let missingCredential: Bool

    static let placeholder = CredentialWidgetEntry(
        date: Date(),
        credential: WidgetSnapshotCredential(
            id: UUID(uuidString: "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")!,
            providerID: "routin",
            providerName: "Routin",
            name: "主力账号",
            planName: "Team Max",
            isEnabled: true,
            sortOrder: 0,
            fetchedAt: Date(),
            lastSuccessAt: Date(),
            isStale: false,
            subscriptionEndAt: nil,
            metrics: [
                WidgetSnapshotMetric(
                    id: "five-hour",
                    label: "五小时窗口",
                    used: 61,
                    limit: 100,
                    remaining: 39,
                    unit: .token,
                    presentation: .progress,
                    semantic: .usedQuota,
                    healthState: .warning
                ),
                WidgetSnapshotMetric(
                    id: "weekly",
                    label: "本周用量",
                    used: 24,
                    limit: 100,
                    remaining: 76,
                    unit: .token,
                    presentation: .progress,
                    semantic: .usedQuota,
                    healthState: .normal
                )
            ]
        ),
        style: .automatic,
        primaryMetricID: nil,
        deepLinkURL: URL(string: "mytoken://credential/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA?refresh=1"),
        missingCredential: false
    )
}

struct OverviewWidgetEntry: TimelineEntry {
    let date: Date
    let credentials: [WidgetSnapshotCredential]
    let style: WidgetDisplayStyleIntentEnum

    static let placeholder = OverviewWidgetEntry(
        date: Date(),
        credentials: [CredentialWidgetEntry.placeholder.credential].compactMap { $0 },
        style: .automatic
    )
}

struct CredentialWidgetIntentProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> CredentialWidgetEntry {
        .placeholder
    }

    func snapshot(
        for configuration: CredentialWidgetConfigurationIntent,
        in context: Context
    ) async -> CredentialWidgetEntry {
        makeEntry(configuration: configuration)
    }

    func timeline(
        for configuration: CredentialWidgetConfigurationIntent,
        in context: Context
    ) async -> Timeline<CredentialWidgetEntry> {
        makeTimeline(entry: makeEntry(configuration: configuration))
    }

    private func makeEntry(
        configuration: CredentialWidgetConfigurationIntent
    ) -> CredentialWidgetEntry {
        let bundle = try? WidgetSharedSnapshotLoader.load()
        let credentials = bundle?.credentials ?? []
        let configuredID = configuration.credential.flatMap {
            UUID(uuidString: $0.id)
        }
        let configuredCredential = configuredID.flatMap { id in
            credentials.first { $0.id == id }
        }
        let missingCredential = configuredID != nil && configuredCredential == nil
        let credential = configuredCredential
            ?? credentials.first(where: \.isEnabled)
            ?? credentials.first

        return CredentialWidgetEntry(
            date: bundle?.generatedAt ?? Date(),
            credential: credential,
            style: configuration.style,
            primaryMetricID: configuration.primaryMetric?.id,
            deepLinkURL: credential.map {
                URL(string: "mytoken://credential/\($0.id.uuidString)?refresh=1")
            } ?? nil,
            missingCredential: missingCredential
        )
    }

    private func makeTimeline(
        entry: CredentialWidgetEntry
    ) -> Timeline<CredentialWidgetEntry> {
        let timelineMoments = WidgetTimelineEntryFactory.makeEntries(
            snapshot: try? WidgetSharedSnapshotLoader.load(),
            now: entry.date
        )
        let entries = timelineMoments.map { moment in
            CredentialWidgetEntry(
                date: moment.date,
                credential: entry.credential,
                style: entry.style,
                primaryMetricID: entry.primaryMetricID,
                deepLinkURL: entry.deepLinkURL,
                missingCredential: entry.missingCredential
            )
        }
        return Timeline(
            entries: entries,
            policy: .after(Date().addingTimeInterval(30 * 60))
        )
    }
}

struct OverviewWidgetIntentProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> OverviewWidgetEntry {
        .placeholder
    }

    func snapshot(
        for configuration: OverviewWidgetConfigurationIntent,
        in context: Context
    ) async -> OverviewWidgetEntry {
        makeEntry(configuration: configuration)
    }

    func timeline(
        for configuration: OverviewWidgetConfigurationIntent,
        in context: Context
    ) async -> Timeline<OverviewWidgetEntry> {
        let entry = makeEntry(configuration: configuration)
        let moments = WidgetTimelineEntryFactory.makeEntries(
            snapshot: try? WidgetSharedSnapshotLoader.load(),
            now: entry.date
        )
        return Timeline(
            entries: moments.map {
                OverviewWidgetEntry(date: $0.date, credentials: entry.credentials, style: entry.style)
            },
            policy: .after(Date().addingTimeInterval(30 * 60))
        )
    }

    private func makeEntry(
        configuration: OverviewWidgetConfigurationIntent
    ) -> OverviewWidgetEntry {
        let bundle = try? WidgetSharedSnapshotLoader.load()
        let allCredentials = bundle?.credentials.filter(\.isEnabled) ?? []
        let selectedIDs = Set(configuration.manualCredentialIDs)
        let credentials = configuration.source == .manual
            ? allCredentials.filter { selectedIDs.contains($0.id.uuidString) }
            : allCredentials
        return OverviewWidgetEntry(
            date: bundle?.generatedAt ?? Date(),
            credentials: credentials,
            style: configuration.style
        )
    }
}

extension WidgetSizeKind {
    init(family: WidgetFamily) {
        switch family {
        case .systemLarge:
            self = .expanded
        case .systemMedium:
            self = .standard
        default:
            self = .compact
        }
    }
}
