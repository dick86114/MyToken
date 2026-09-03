import XCTest

final class UsagePopoverLayoutTests: XCTestCase {
    func test弹窗内容使用带滚动条的限高容器() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "UsagePopoverView.swift"
        ])

        XCTAssertTrue(source.contains("ScrollView(.vertical, showsIndicators: true)"))
        XCTAssertTrue(source.contains(".frame(width: 440, height: maxPopoverHeight)"))
        XCTAssertTrue(source.contains("visibleHeight * 0.9"))
        XCTAssertFalse(source.contains(".fixedSize(horizontal: false, vertical: true)"))

        let controller = try TestSourceReader.read([
            "RoutinUsage",
            "App",
            "StatusBarController.swift"
        ])
        XCTAssertTrue(controller.contains("popover.contentSize = contentSize"))
        XCTAssertTrue(controller.contains("window.setContentSize(contentSize)"))
        XCTAssertTrue(controller.contains("screenHeight * 0.9"))
    }
}
