import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarLabelViewTests: XCTestCase {
    @MainActor
    func test单色状态栏图标使用系统模板着色而多指标保留风险色() {
        XCTAssertFalse(MenuBarMultiUsageIcon.image(indicators: []).isTemplate)
        XCTAssertTrue(MenuBarVerticalUsageIcon.image(percent: 35).isTemplate)
        XCTAssertTrue(MenuBarLogoUsageIcon.image(percent: 35).isTemplate)
    }
}
