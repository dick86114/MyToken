import XCTest

final class GeneralSettingsViewTests: XCTestCase {
    func test通用页只保留刷新启动和通知总开关() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])

        XCTAssertTrue(source.contains("刷新"))
        XCTAssertTrue(source.contains("登录时启动"))
        XCTAssertTrue(source.contains("notificationsEnabled"))
        XCTAssertTrue(source.contains("SettingsPageScroll"))
        XCTAssertTrue(source.contains("SettingsGroup"))
        XCTAssertTrue(source.contains("SettingsRow"))
        XCTAssertTrue(source.contains("dataMigrationSection"))
        XCTAssertTrue(source.contains("TransferToAndroidView"))
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
        XCTAssertTrue(editor.contains("ColorWellButton"))
        XCTAssertTrue(editor.contains("MenuBarColorPanelLauncher"))
        XCTAssertTrue(editor.contains("colorLevelControl"))
        XCTAssertTrue(editor.contains("workingThresholds"))
        XCTAssertTrue(editor.contains("interactiveSpring"))
        XCTAssertTrue(editor.contains("恢复默认"))
        XCTAssertTrue(editor.contains("showsResetConfirmation"))
        XCTAssertTrue(editor.contains("恢复默认进度颜色？"))
        XCTAssertTrue(editor.contains("将同时重置两个阈值和正常、提醒、紧急三种颜色"))
        XCTAssertTrue(editor.contains("maxWidth: 520"))
        XCTAssertFalse(editor.contains("Slider("))
        XCTAssertTrue(menuBar.contains("colorRulesSection"))
        XCTAssertTrue(menuBar.contains("MenuBarColorRulesEditor"))
        let general = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])
        XCTAssertFalse(general.contains("MenuBarColorRulesEditor"))
        XCTAssertFalse(menuBar.contains("ColorPicker("))
    }

    func test通用页提供卡片简洁完整显示() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])

        XCTAssertTrue(source.contains("卡片显示"))
        XCTAssertTrue(source.contains("简洁模式只保留各供应商最常用的用量信息。"))
        XCTAssertTrue(source.contains("settings.usageCardDensity"))
    }

    func test通用页提供横向刷新和外观控件() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])

        XCTAssertTrue(source.contains("RefreshIntervalControl"))
        XCTAssertFalse(source.contains(".pickerStyle(.menu)"))
        XCTAssertTrue(source.contains("AppAppearanceSegmentedControl"))
        XCTAssertTrue(source.contains("settings.appearance"))
    }

    func test外观设置显式同步菜单栏弹窗() throws {
        let environment = try TestSourceReader.read([
            "RoutinUsage", "App", "AppEnvironment.swift"
        ])
        let controller = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift"
        ])

        XCTAssertTrue(environment.contains(
            "StatusBarController.shared?.appearanceDidChange(to: appearance)"
        ))
        XCTAssertTrue(controller.contains("popover.appearance = resolved"))
        XCTAssertTrue(controller.contains("settingsWindow?.appearance = resolved"))
    }

    func test帮助页提供GitHub令牌设置() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(source.contains("GitHub 令牌"))
        XCTAssertTrue(source.contains("SettingsLabeledTextField("))
        XCTAssertTrue(source.contains("isSecret: true"))
        XCTAssertTrue(source.contains("Button(\"移除\")"))
        XCTAssertTrue(source.contains("tokenMask()"))
        XCTAssertTrue(source.contains("应用本地存储"))
    }

    func test刷新间隔控件提供预设和自定义输入() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "RefreshIntervalControl.swift"
        ])

        XCTAssertTrue(source.contains("1 分钟"))
        XCTAssertTrue(source.contains("30 分钟"))
        XCTAssertTrue(source.contains("自定义"))
        XCTAssertTrue(source.contains("TextField"))
        XCTAssertFalse(source.contains(".pickerStyle(.menu)"))
    }

    func test导出按钮与导入按钮使用一致的普通样式() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "GeneralSettingsView.swift"
        ])
        let exportStart = try XCTUnwrap(
            source.range(of: "Label(\"导出配置\"")
        )
        let importStart = try XCTUnwrap(
            source.range(of: "Label(\"导入配置\"")
        )

        XCTAssertTrue(source[exportStart.lowerBound...].contains(".liquidGlassButton()"))
        XCTAssertTrue(source[importStart.lowerBound...].contains(".liquidGlassButton()"))
    }
}
