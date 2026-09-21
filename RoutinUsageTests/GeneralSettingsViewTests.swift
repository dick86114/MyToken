import XCTest

final class GeneralSettingsViewTests: XCTestCase {
    func test通用页只保留刷新启动和通知总开关() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])

        XCTAssertTrue(source.contains("刷新"))
        XCTAssertTrue(source.contains("登录时启动"))
        XCTAssertTrue(source.contains("notificationsEnabled"))
        XCTAssertTrue(source.contains("settingSection"))
        XCTAssertTrue(source.contains("MenuBarColorRulesEditor"))
        XCTAssertTrue(source.contains(".liquidGlassSurface(cornerRadius: 16)"))
        XCTAssertFalse(source.contains("Form {"))
        XCTAssertFalse(source.contains(".formStyle(.grouped)"))
        XCTAssertFalse(source.contains("displayDimension"))
        XCTAssertFalse(source.contains("settings.thresholds"))
    }

    func test进度颜色设置使用色带和色板而不是独立滑杆() throws {
        let editor = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "MenuBarColorRulesEditor.swift"
        ])
        let menuBar = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])

        XCTAssertTrue(editor.contains("MenuBarThresholdDragGesture"))
        XCTAssertTrue(editor.contains("ColorSwatchButton"))
        XCTAssertTrue(editor.contains("liquidGlassInteractiveControl("))
        XCTAssertTrue(editor.contains("workingThresholds"))
        XCTAssertTrue(editor.contains("interactiveSpring"))
        XCTAssertTrue(editor.contains("ColorPanelLauncher"))
        XCTAssertTrue(editor.contains("maxWidth: 520"))
        XCTAssertFalse(editor.contains("Slider("))
        XCTAssertFalse(menuBar.contains("colorRulesSection"))
        XCTAssertFalse(menuBar.contains("ColorPicker("))
    }
}
