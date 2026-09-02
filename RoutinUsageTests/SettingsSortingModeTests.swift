import XCTest

final class SettingsSortingModeTests: XCTestCase {
    func test拖拽行为只在排序模式启用且使用三横线手柄() throws {
        let source = try String(
            contentsOfFile: "REDACTED_PROJECT_ROOT/RoutinUsage/Views/SettingsView.swift",
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("CredentialSortInteraction"))
        XCTAssertTrue(source.contains("enabled: isReordering"))
        XCTAssertTrue(source.contains("line.3.horizontal"))
        XCTAssertTrue(source.contains("if !isReordering"))
    }
}
