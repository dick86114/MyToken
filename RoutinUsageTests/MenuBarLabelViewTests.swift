import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarLabelViewTests: XCTestCase {
    func test多指标悬停提示按凭证换行() {
        let indicators = [
            MenuBarIndicatorModel(
                shortCode: "GLM",
                percent: 27,
                healthState: .normal,
                accessibilityLabel: "GLM，武，已使用 27%"
            ),
            MenuBarIndicatorModel(
                shortCode: "DS",
                percent: nil,
                healthState: .normal,
                accessibilityLabel: "DeepSeek，亮，余额 33.19"
            )
        ]

        XCTAssertEqual(
            MenuBarIndicatorModel.hoverSummary(for: indicators),
            "GLM，武，已使用 27%\nDeepSeek，亮，余额 33.19"
        )
    }

    @MainActor
    func test多指标图标非模板且其他菜单样式保持系统模板() {
        XCTAssertFalse(MenuBarMultiUsageIcon.image(indicators: []).isTemplate)
        XCTAssertTrue(MenuBarVerticalUsageIcon.image(percent: 35).isTemplate)
        XCTAssertTrue(MenuBarLogoUsageIcon.image(percent: 35).isTemplate)
    }
}
