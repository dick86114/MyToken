import XCTest
@testable import RoutinUsage

final class WidgetTimelineEntryFactoryTests: XCTestCase {
    func test_timeline_adds_reset_expiry_and_stale_entries_without_fabricating_usage() {
        let now = Date(timeIntervalSince1970: 1_000)
        let bundle = WidgetSnapshotBundle(
            schemaVersion: WidgetSnapshotBundle.currentSchemaVersion,
            generatedAt: now,
            credentials: [
                WidgetSnapshotCredential(
                    id: UUID(uuidString: "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")!,
                    providerID: "routin",
                    providerName: "Routin",
                    name: "主力账号",
                    planName: "Team",
                    isEnabled: true,
                    sortOrder: 0,
                    fetchedAt: now,
                    lastSuccessAt: now,
                    isStale: false,
                    subscriptionEndAt: now.addingTimeInterval(7_200),
                    metrics: [
                        WidgetSnapshotMetric(
                            id: "five-hour",
                            label: "五小时",
                            used: 20,
                            limit: 100,
                            remaining: 80,
                            unit: .token,
                            windowEnd: now.addingTimeInterval(3_600),
                            presentation: .progress,
                            semantic: .usedQuota,
                            healthState: .normal
                        )
                    ]
                )
            ]
        )

        let entries = WidgetTimelineEntryFactory.makeEntries(snapshot: bundle, now: now)

        XCTAssertEqual(
            entries.map(\.date),
            [
                now,
                now.addingTimeInterval(1_800),
                now.addingTimeInterval(3_600),
                now.addingTimeInterval(7_200)
            ]
        )
        XCTAssertTrue(entries.allSatisfy { $0.credentials == bundle.credentials })
    }

    func test_timeline_without_snapshot_returns_single_empty_entry() {
        let now = Date(timeIntervalSince1970: 2_000)

        let entries = WidgetTimelineEntryFactory.makeEntries(snapshot: nil, now: now)

        XCTAssertEqual(entries, [WidgetTimelineEntry(date: now, credentials: [])])
    }
}
