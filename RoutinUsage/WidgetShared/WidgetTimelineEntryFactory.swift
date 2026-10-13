import Foundation

struct WidgetTimelineEntry: Equatable, Sendable {
    let date: Date
    let credentials: [WidgetSnapshotCredential]
}

enum WidgetTimelineEntryFactory {
    static let staleInterval: TimeInterval = 30 * 60

    static func makeEntries(
        snapshot: WidgetSnapshotBundle?,
        now: Date
    ) -> [WidgetTimelineEntry] {
        guard let snapshot else {
            return [WidgetTimelineEntry(date: now, credentials: [])]
        }

        var dates: Set<Date> = [now, now.addingTimeInterval(staleInterval)]
        for credential in snapshot.credentials {
            if let subscriptionEndAt = credential.subscriptionEndAt {
                dates.insert(subscriptionEndAt)
            }
            for metric in credential.metrics {
                if let windowEnd = metric.windowEnd {
                    dates.insert(windowEnd)
                }
            }
        }
        return dates
            .filter { $0 >= now }
            .sorted()
            .map { WidgetTimelineEntry(date: $0, credentials: snapshot.credentials) }
    }
}
