import XCTest
@testable import RoutinUsage

final class UpdateNotesAccessibilityTests: XCTestCase {
    func testMarkdown更新日志转换为VoiceOver可读正文() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: "## 改进\n\n- 修复 **更新检查**"),
            "更新日志，改进\n修复 更新检查"
        )
    }

    func testMarkdown更新日志渲染块级标题列表和代码块() throws {
        let attributedText = try XCTUnwrap(
            UpdateNotesRenderer.attributedText(
                notes: "## 改进\n\n- 修复 **更新检查**\n- 支持 `Markdown`\n\n```swift\nlet version = 1\n```"
            )
        )

        let text = String(attributedText.characters)
        XCTAssertTrue(text.contains("•  修复 更新检查"))
        XCTAssertTrue(text.contains("•  支持 Markdown"))
        XCTAssertTrue(text.contains("let version = 1"))
        XCTAssertFalse(text.contains("##"))
        XCTAssertFalse(text.contains("- "))
    }

    func testHTML更新日志压缩列表缩进并保留条目文本() throws {
        let attributedText = try XCTUnwrap(
            UpdateNotesRenderer.attributedText(
                notes: "<ul>\n<li>新增简洁/完整模式切换</li>\n<li>支持更加简洁的方式呈现用量</li>\n<li>优化排版布局</li>\n</ul>"
            )
        )

        let text = String(attributedText.characters)
        XCTAssertTrue(text.contains("新增简洁/完整模式切换"))
        XCTAssertTrue(text.contains("支持更加简洁的方式呈现用量"))
        XCTAssertTrue(text.contains("优化排版布局"))
        XCTAssertFalse(text.contains("<li>"))
    }

    func testHTML渲染注入紧凑列表样式() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateNotesView.swift"
        ])

        XCTAssertTrue(source.contains("padding-left: 16px"))
        XCTAssertTrue(source.contains("li { margin: 0; }"))
    }

    func test空更新日志朗读明确空状态() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: " \n "),
            "更新日志，此版本未提供更新日志"
        )
    }

    func test更新通知文案移除HTML标签并压缩空白() {
        XCTAssertEqual(
            UpdateNotesRenderer.notificationText(
                notes: "<p>优化布局、修复安卓端若干 bug</p>\n<p>补充说明</p>"
            ),
            "优化布局、修复安卓端若干 bug 补充说明"
        )
    }

    func testHTML更新日志去除标签后再朗读() {
        XCTAssertEqual(
            UpdateNotesAccessibility.label(notes: "<p><strong>Full Changelog</strong>: https://example.com/releases</p>"),
            "更新日志，Full Changelog: https://example.com/releases"
        )
    }
}
