import XCTest
@testable import RoutinUsage

final class WidgetMetricRankerTests: XCTestCase {
    func test_ranker_preserves_provider_metric_priority() throws {
        let fixture = try WidgetContractFixture.load(bundle: Bundle(for: Self.self))

        for testCase in fixture.cases {
            let selection = WidgetMetricRanker.rank(
                providerID: testCase.providerID,
                metrics: testCase.credential.metrics,
                fixedMetricID: testCase.fixedMetricID,
                capacity: testCase.capacity
            )

            XCTAssertEqual(
                selection.orderedMetrics.map(\.id),
                testCase.expectedMetricIDs,
                testCase.id
            )
            XCTAssertEqual(
                selection.primary?.id,
                testCase.expectedPrimaryMetricID,
                testCase.id
            )
            XCTAssertEqual(
                selection.fixedMetricUnavailable,
                testCase.expectedFixedMetricUnavailable,
                testCase.id
            )
        }
    }

    func test_ranker_reports_missing_fixed_metric_without_dropping_other_metrics() throws {
        let fixture = try WidgetContractFixture.load(bundle: Bundle(for: Self.self))
        let testCase = try XCTUnwrap(fixture.cases.first { $0.id == "command-code-fixed-missing" })

        let selection = WidgetMetricRanker.rank(
            providerID: testCase.providerID,
            metrics: testCase.credential.metrics,
            fixedMetricID: testCase.fixedMetricID,
            capacity: testCase.capacity
        )

        XCTAssertTrue(selection.fixedMetricUnavailable)
        XCTAssertEqual(selection.primary?.id, "five-hour")
        XCTAssertEqual(
            selection.orderedMetrics.map(\.id),
            ["five-hour", "weekly", "request-count", "purchased-remaining"]
        )
    }
}

struct WidgetContractFixture: Decodable {
    let schemaVersion: Int
    let cases: [WidgetContractCase]

    static func load(bundle: Bundle) throws -> WidgetContractFixture {
        let url = try XCTUnwrap(
            bundle.url(forResource: "widget-cases", withExtension: "json"),
            "缺少 widget-cases.json 测试资源"
        )
        return try JSONDecoder().decode(WidgetContractFixture.self, from: Data(contentsOf: url))
    }
}

struct WidgetContractCase: Decodable {
    let id: String
    let providerID: String
    let fixedMetricID: String?
    let capacity: Int
    let credential: WidgetSnapshotCredential
    let expectedPrimaryMetricID: String
    let expectedMetricIDs: [String]
    let expectedFixedMetricUnavailable: Bool
    let expectedValueTexts: [String: String]
}
