import XCTest
@testable import RoutinUsage

final class UpdateNotesAccessibilityTests: XCTestCase {
    func testMarkdown更新日志转换为VoiceOver可读正文() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: "## 改进\n\n- 修复 **更新检查**"),
            "更新日志，改进\n修复 更新检查"
        )
    }

    func test空更新日志朗读明确空状态() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: " \n "),
            "更新日志，此版本未提供更新日志"
        )
    }
}
