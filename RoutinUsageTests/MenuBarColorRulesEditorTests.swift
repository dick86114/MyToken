import XCTest
@testable import RoutinUsage

final class MenuBarColorRulesEditorTests: XCTestCase {
    func test拖动起点选择距离最近的阈值手柄() {
        XCTAssertEqual(
            MenuBarColorRulesEditor.activeBoundary(
                horizontalRatio: 0.22,
                warning: 15,
                critical: 80
            ),
            .warning
        )
        XCTAssertEqual(
            MenuBarColorRulesEditor.activeBoundary(
                horizontalRatio: 0.72,
                warning: 15,
                critical: 80
            ),
            .critical
        )
    }

    func test拖动色带时阈值保持有效顺序() {
        let warning = MenuBarColorRulesEditor.updatedValue(
            horizontalRatio: 0.86,
            active: .warning,
            warning: 40,
            critical: 80
        )
        let critical = MenuBarColorRulesEditor.updatedValue(
            horizontalRatio: 0.02,
            active: .critical,
            warning: 40,
            critical: 80
        )

        XCTAssertEqual(warning, 79)
        XCTAssertEqual(critical, 41)
    }
}
