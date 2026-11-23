import XCTest
@testable import RoutinUsage

/// 共享能力契约是阈值唯一来源；macOS 常量与 80/95 分档必须与其保持一致。
final class UsageHealthThresholdsTests: XCTestCase {
    private var fixtureBundle: Bundle { Bundle(for: UsageHealthThresholdsTests.self) }

    func test共享契约与两端阈值一致() throws {
        let url = try XCTUnwrap(
            fixtureBundle.url(forResource: "provider-capabilities", withExtension: "json")
        )
        let root = try XCTUnwrap(
            JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any]
        )
        let thresholds = try XCTUnwrap(root["healthThresholds"] as? [String: Any])
        XCTAssertEqual(thresholds["warningUsedPercent"] as? Int, 80)
        XCTAssertEqual(thresholds["criticalUsedPercent"] as? Int, 95)
        XCTAssertEqual(UsageHealthThresholds.warningUsedPercent, 80)
        XCTAssertEqual(UsageHealthThresholds.criticalUsedPercent, 95)
    }

    func test已用百分比分档为80和95() {
        XCTAssertEqual(UsageHealthThresholds.healthState(forUsedPercent: 79.9), .normal)
        XCTAssertEqual(UsageHealthThresholds.healthState(forUsedPercent: 80), .warning)
        XCTAssertEqual(UsageHealthThresholds.healthState(forUsedPercent: 94.9), .warning)
        XCTAssertEqual(UsageHealthThresholds.healthState(forUsedPercent: 95), .critical)
    }
}
