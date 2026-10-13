import WidgetKit

struct CredentialWidgetEntry: TimelineEntry {
    let date: Date
    let credential: WidgetSnapshotCredential?
    let deepLinkURL: URL?

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
        deepLinkURL: URL(string: "mytoken://credential/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA?refresh=1")
    )
}

struct WidgetSnapshotProvider: TimelineProvider {
    func placeholder(in context: Context) -> CredentialWidgetEntry {
        .placeholder
    }

    func getSnapshot(
        in context: Context,
        completion: @escaping (CredentialWidgetEntry) -> Void
    ) {
        completion(makeEntry())
    }

    func getTimeline(
        in context: Context,
        completion: @escaping (Timeline<CredentialWidgetEntry>) -> Void
    ) {
        let entry = makeEntry()
        completion(
            Timeline(
                entries: [entry],
                policy: .after(Date().addingTimeInterval(30 * 60))
            )
        )
    }

    private func makeEntry() -> CredentialWidgetEntry {
        guard
            let directory = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: "group.cc.idickies.mytoken.widgets"
            ),
            let bundle = try? WidgetSnapshotStore(directory: directory).load(),
            let credential = bundle.credentials.first(where: \.isEnabled)
                ?? bundle.credentials.first
        else {
            return CredentialWidgetEntry(date: Date(), credential: nil, deepLinkURL: nil)
        }

        return CredentialWidgetEntry(
            date: bundle.generatedAt,
            credential: credential,
            deepLinkURL: URL(
                string: "mytoken://credential/\(credential.id.uuidString)?refresh=1"
            )
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
