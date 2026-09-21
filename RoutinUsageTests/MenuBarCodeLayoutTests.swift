import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarCodeLayoutTests: XCTestCase {
    func test短码字号和行距安全区内的安全边距() {
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 2).pointSize,
            9.5,
            accuracy: 0.01
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 3).pointSize,
            8.4,
            accuracy: 0.01
        )
        XCTAssertEqual(MenuBarMultiUsageIcon.codeSlotHeight(for: 2), 8, accuracy: 0.01)
        XCTAssertEqual(MenuBarMultiUsageIcon.codeSlotHeight(for: 3), 8, accuracy: 0.01)
    }

    func test短码绘制基线保留在图标安全区内() {
        let characters = Array("GLM")
        let font = MenuBarMultiUsageIcon.codeFont(for: characters.count)

        for index in characters.indices {
            let baseline = MenuBarMultiUsageIcon.codeBaselineY(
                characterCount: characters.count,
                index: index,
                font: font
            )
            let glyphTop = baseline + font.capHeight

            XCTAssertGreaterThanOrEqual(baseline, 0)
            XCTAssertLessThanOrEqual(glyphTop, MenuBarMultiUsageIcon.size.height - 2)
        }
    }

}
