import XCTest

enum TestSourceReader {
    static func read(_ pathComponents: [String]) throws -> String {
        var url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()

        for pathComponent in pathComponents {
            url.appendPathComponent(pathComponent)
        }
        return try String(contentsOf: url, encoding: .utf8)
    }
}

final class SettingsSortingModeTests: XCTestCase {
    func test排序仅在显示与刷新页的菜单栏指标中启用() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "SettingsView.swift"
        ])

        XCTAssertTrue(source.contains("MenuBarIndicatorCardDropDelegate"))
        XCTAssertTrue(source.contains("MenuBarIndicatorCardDragControl"))
        XCTAssertTrue(source.contains(".onDrag"))
        XCTAssertTrue(source.contains("DropDelegate"))
        XCTAssertTrue(source.contains("draggingIndicatorID"))
        XCTAssertTrue(source.contains("withAnimation(.spring(response: 0.32, dampingFraction: 0.82)"))
        XCTAssertTrue(source.contains("scaleEffect(draggingIndicatorID == configuration.id ? 1.02 : 1)"))
        XCTAssertTrue(source.contains("shadow(color: .black.opacity(draggingIndicatorID == configuration.id ? 0.18 : 0)"))
        XCTAssertFalse(source.contains(".onMove(perform: settings.moveSelectedCredential)"))
        XCTAssertFalse(source.contains("chevron.up"))
        XCTAssertFalse(source.contains("chevron.down"))
        XCTAssertFalse(source.contains("private struct CredentialSortInteraction"))
        XCTAssertFalse(source.contains("兼容选项"))
        XCTAssertFalse(source.contains("额度通知"))
    }

    func test卡片拖拽保持原始拖拽ID并避免目标卡片闪烁() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "SettingsView.swift"
        ])
        let delegateStart = try XCTUnwrap(source.range(of: "struct MenuBarIndicatorCardDropDelegate"))
        let delegateEnd = try XCTUnwrap(source.range(of: "struct MenuBarIndicatorCardDragControl"))
        let delegate = String(source[delegateStart.lowerBound..<delegateEnd.lowerBound])
        let rowStart = try XCTUnwrap(source.range(of: "func menuBarIndicatorRow"))
        let availableStart = try XCTUnwrap(source.range(of: "func availableMenuBarIndicatorRow"))
        let row = String(source[rowStart.lowerBound..<availableStart.lowerBound])

        XCTAssertTrue(delegate.contains("move(draggedID, targetID)"))
        XCTAssertFalse(delegate.contains("self.draggedID = targetID"))
        XCTAssertTrue(row.contains(".onDrag {"))
        XCTAssertTrue(row.contains("scaleEffect(draggingIndicatorID == configuration.id ? 1.02 : 1)"))
    }
}
