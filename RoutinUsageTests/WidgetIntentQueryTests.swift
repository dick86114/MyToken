import AppIntents
import XCTest
@testable import RoutinUsage

final class WidgetIntentQueryTests: XCTestCase {
    func test_credential_entity_query_excludes_disabled_and_missing_credentials() async throws {
        let bundle = makeBundle()
        let query = CredentialEntityQuery(loader: { bundle })
        let enabledID = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"
        let disabledID = "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB"

        let suggested = try await query.suggestedEntities()
        XCTAssertEqual(suggested.map(\.id), [enabledID])

        let requested = try await query.entities(for: [enabledID, disabledID, "missing"])
        XCTAssertEqual(requested.map(\.id), [enabledID])
    }

    func test_primary_metric_entity_uses_snapshot_and_falls_back_to_empty() async throws {
        let populatedQuery = PrimaryMetricEntityQuery(loader: { self.makeBundle() })
        let populated = try await populatedQuery.suggestedEntities()
        XCTAssertTrue(populated.contains { $0.id == "five-hour" })

        let emptyQuery = PrimaryMetricEntityQuery(loader: { nil })
        let empty = try await emptyQuery.suggestedEntities()
        XCTAssertTrue(empty.isEmpty)
    }

    func test_intent_enums_expose_exact_display_styles() {
        XCTAssertEqual(
            WidgetDisplayStyleIntentEnum.allCases,
            [.automatic, .focusRing, .scanBand, .signalTiles]
        )
        XCTAssertEqual(
            OverviewSourceIntentEnum.allCases,
            [.allEnabled, .manual]
        )
    }

    func test_manual_overview_selection_is_capped_at_six() {
        let entities = (0..<8).map { index in
            CredentialEntity(id: "\(index)", name: "凭证 \(index)", providerName: "Routin")
        }
        let intent = OverviewWidgetConfigurationIntent()
        intent.credentials = entities

        XCTAssertEqual(intent.manualCredentialIDs, Array(0..<6).map(String.init))
    }

    private func makeBundle() -> WidgetSnapshotBundle {
        WidgetSnapshotBundle(
            schemaVersion: WidgetSnapshotBundle.currentSchemaVersion,
            generatedAt: Date(timeIntervalSince1970: 100),
            credentials: [
                WidgetSnapshotCredential(
                    id: UUID(uuidString: "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")!,
                    providerID: "routin",
                    providerName: "Routin",
                    name: "启用凭证",
                    planName: "Team",
                    isEnabled: true,
                    sortOrder: 0,
                    fetchedAt: Date(timeIntervalSince1970: 100),
                    lastSuccessAt: Date(timeIntervalSince1970: 100),
                    isStale: false,
                    subscriptionEndAt: nil,
                    metrics: [
                        WidgetSnapshotMetric(
                            id: "five-hour",
                            label: "五小时",
                            used: 20,
                            limit: 100,
                            remaining: 80,
                            unit: .token,
                            presentation: .progress,
                            semantic: .usedQuota,
                            healthState: .normal
                        )
                    ]
                ),
                WidgetSnapshotCredential(
                    id: UUID(uuidString: "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB")!,
                    providerID: "routin",
                    providerName: "Routin",
                    name: "停用凭证",
                    planName: "Team",
                    isEnabled: false,
                    sortOrder: 1,
                    fetchedAt: nil,
                    lastSuccessAt: nil,
                    isStale: false,
                    subscriptionEndAt: nil,
                    metrics: []
                )
            ]
        )
    }
}
