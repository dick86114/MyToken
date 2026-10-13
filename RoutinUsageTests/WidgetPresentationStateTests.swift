import XCTest
@testable import RoutinUsage

final class WidgetPresentationStateTests: XCTestCase {
    func test_presentation_states_render_distinct_status_copy() {
        let statuses: [WidgetPresentationStatus] = [
            .ready,
            .loading,
            .stale,
            .refreshFailed,
            .authenticationFailed,
            .disabled,
            .missingCredential,
            .noMetrics,
            .fixedMetricUnavailable
        ]
        let copies = statuses.map(WidgetPresentationStateResolver.copy)

        XCTAssertEqual(Set(copies).count, statuses.count)
        XCTAssertEqual(WidgetPresentationStateResolver.copy(for: .authenticationFailed), "需要重新登录/更换凭证")
        XCTAssertEqual(WidgetPresentationStateResolver.copy(for: .missingCredential), "凭证不存在，请重新配置")
        XCTAssertEqual(WidgetPresentationStateResolver.copy(for: .noMetrics), "该套餐暂无可用指标")
    }

    func test_state_resolver_prioritizes_missing_auth_disabled_and_failure() {
        XCTAssertEqual(
            WidgetPresentationStateResolver.resolve(
                isEnabled: true,
                hasSnapshot: false,
                isStale: false,
                isRefreshing: false,
                failure: nil,
                metricCount: 0,
                fixedMetricUnavailable: false
            ),
            .loading
        )
        XCTAssertEqual(
            WidgetPresentationStateResolver.resolve(
                isEnabled: false,
                hasSnapshot: true,
                isStale: false,
                isRefreshing: false,
                failure: nil,
                metricCount: 1,
                fixedMetricUnavailable: false
            ),
            .disabled
        )
        XCTAssertEqual(
            WidgetPresentationStateResolver.resolve(
                isEnabled: true,
                hasSnapshot: true,
                isStale: false,
                isRefreshing: false,
                failure: .authentication,
                metricCount: 1,
                fixedMetricUnavailable: false
            ),
            .authenticationFailed
        )
        XCTAssertEqual(
            WidgetPresentationStateResolver.resolve(
                isEnabled: true,
                hasSnapshot: true,
                isStale: true,
                isRefreshing: false,
                failure: nil,
                metricCount: 1,
                fixedMetricUnavailable: false
            ),
            .stale
        )
    }

    func test_long_names_and_values_remain_single_line() {
        for role in WidgetTextRole.allCases {
            XCTAssertEqual(WidgetTextLayoutPolicy.lineLimit(for: role), 1)
        }
        XCTAssertTrue(WidgetTextLayoutPolicy.usesTailTruncation(for: .credentialName))
        XCTAssertTrue(WidgetTextLayoutPolicy.usesTailTruncation(for: .metricLabel))
        XCTAssertFalse(WidgetTextLayoutPolicy.usesTailTruncation(for: .metricValue))
    }
}
