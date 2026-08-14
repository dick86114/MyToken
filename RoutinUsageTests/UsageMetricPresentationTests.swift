import XCTest
@testable import RoutinUsage

final class UsageMetricPresentationTests: XCTestCase {
    func test图形化用量将百分比限制在零到一百之间() {
        XCTAssertEqual(UsageMetricPresentation.clampedPercent(-12), 0)
        XCTAssertEqual(UsageMetricPresentation.clampedPercent(42.5), 42.5)
        XCTAssertEqual(UsageMetricPresentation.clampedPercent(128), 100)
        XCTAssertEqual(UsageMetricPresentation.clampedPercent(Double.nan), 0)
    }

    func test图形化用量使用统一风险色阶() {
        XCTAssertEqual(UsageMetricPresentation.tone(for: 49.9), .normal)
        XCTAssertEqual(UsageMetricPresentation.tone(for: 50), .warning)
        XCTAssertEqual(UsageMetricPresentation.tone(for: 80), .critical)
    }
}
