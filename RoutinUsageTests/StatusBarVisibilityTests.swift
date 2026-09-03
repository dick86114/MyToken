import XCTest

final class StatusBarVisibilityTests: XCTestCase {
    func test状态栏控制器由应用级强引用并显式保持状态项可见() throws {
        let source = try String(
            contentsOfFile: "REDACTED_PROJECT_ROOT/RoutinUsage/App/RoutinUsageApp.swift",
            encoding: .utf8
        )
        let controller = try String(
            contentsOfFile: "REDACTED_PROJECT_ROOT/RoutinUsage/App/StatusBarController.swift",
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("retainedStatusBarController"))
        XCTAssertTrue(controller.contains("statusItem.isVisible = true"))
        XCTAssertTrue(controller.contains("statusItem.autosaveName = \"ai.routin.myroutin\""))
        XCTAssertTrue(controller.contains("statusItem.length = imageWidth + 8"))
    }
}
