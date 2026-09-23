import XCTest

final class ReleaseNotesAndUpdateFeedbackTests: XCTestCase {
    func test当前版本日志使用本地缓存并提供手动获取入口() throws {
        let environment = try TestSourceReader.read([
            "RoutinUsage", "App", "AppEnvironment.swift"
        ])
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(environment.contains("releaseHistoryCache"))
        XCTAssertTrue(environment.contains("releaseHistoryCache?.load"))
        XCTAssertTrue(environment.contains("releaseHistoryCache?.save"))
        XCTAssertTrue(view.contains("获取当前版本日志"))
        XCTAssertTrue(view.contains("loadReleaseHistoryIfNeeded(force: true)"))
    }

    func test菜单栏检测更新失败显示可见提示() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])
        let toolbarStart = try XCTUnwrap(source.range(of: "var toolbar: some View"))
        let toolbar = String(source[toolbarStart.lowerBound...])

        XCTAssertTrue(toolbar.contains("if case let .failed"))
        XCTAssertTrue(toolbar.contains("检测更新失败，请稍后重试"))
        XCTAssertTrue(toolbar.contains("CompactPopoverPalette.criticalColor"))
    }
}
