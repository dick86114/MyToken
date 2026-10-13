import XCTest
@testable import RoutinUsage

final class WidgetPresentationBuilderTests: XCTestCase {
    func test_builder_limits_metrics_by_size_kind() throws {
        let credential = try fixtureCredential(id: "command-code-priority")

        let compact = WidgetPresentationBuilder.makeCredentialPresentation(
            credential: credential,
            style: .auto,
            size: .compact
        )
        let standard = WidgetPresentationBuilder.makeCredentialPresentation(
            credential: credential,
            style: .auto,
            size: .standard
        )
        let expanded = WidgetPresentationBuilder.makeCredentialPresentation(
            credential: credential,
            style: .auto,
            size: .expanded
        )

        XCTAssertEqual(compact.credentials.first?.orderedMetrics.count, 2)
        XCTAssertEqual(standard.credentials.first?.orderedMetrics.count, 4)
        XCTAssertEqual(expanded.credentials.first?.orderedMetrics.count, 6)
    }

    func test_builder_resolves_auto_style_by_size_and_component_kind() throws {
        let credential = try fixtureCredential(id: "command-code-priority")

        XCTAssertEqual(
            WidgetPresentationBuilder.makeCredentialPresentation(
                credential: credential,
                style: .auto,
                size: .compact
            ).style,
            .focusRing
        )
        XCTAssertEqual(
            WidgetPresentationBuilder.makeCredentialPresentation(
                credential: credential,
                style: .auto,
                size: .standard
            ).style,
            .focusRing
        )
        XCTAssertEqual(
            WidgetPresentationBuilder.makeCredentialPresentation(
                credential: credential,
                style: .auto,
                size: .expanded
            ).style,
            .signalTiles
        )
        XCTAssertEqual(
            WidgetPresentationBuilder.makeOverviewPresentation(
                credentials: [credential],
                style: .auto,
                size: .standard
            ).style,
            .scanBand
        )
    }

    func test_xiaomi_value_text_preserves_full_grouped_numbers() throws {
        let credential = try fixtureCredential(id: "xiaomi-preserves-order-and-full-token-text")

        let presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential: credential,
            style: .auto,
            size: .expanded
        )
        let credentialPresentation = try XCTUnwrap(presentation.credentials.first)
        let texts = [credentialPresentation.primary].compactMap { $0 }
            + credentialPresentation.secondary
        let valuesByID = Dictionary(uniqueKeysWithValues: texts.map { ($0.id, $0.valueText) })

        XCTAssertEqual(valuesByID["history"], "3,323,413")
        XCTAssertEqual(valuesByID["cache-miss"], "1,200,000")
    }

    private func fixtureCredential(id: String) throws -> WidgetSnapshotCredential {
        let fixture = try WidgetContractFixture.load(bundle: Bundle(for: Self.self))
        return try XCTUnwrap(fixture.cases.first { $0.id == id }?.credential)
    }
}
