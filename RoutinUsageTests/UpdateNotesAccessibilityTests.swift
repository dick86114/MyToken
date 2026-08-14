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

    func testHTML更新日志去除标签后再朗读() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: "<p><strong>Full Changelog</strong>: https://example.com/releases</p>"),
            "更新日志，Full Changelog: https://example.com/releases"
        )
    }
}
