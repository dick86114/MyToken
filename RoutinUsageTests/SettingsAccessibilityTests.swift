import XCTest

final class SettingsAccessibilityTests: XCTestCase {
    func test设置页统一使用玻璃扩展并支持减少动态效果() throws {
        let files = [
            "SettingsWindowView",
            "CredentialManagementView",
            "MenuBarManagementView",
            "GeneralSettingsView",
            "HelpUpdateView"
        ]

        for file in files {
            let source = try TestSourceReader.read([
                "RoutinUsage", "Views", "Settings", "\(file).swift"
            ])
            XCTAssertFalse(
                source.contains("glassEffect("),
                "\(file) 必须通过 LiquidGlassSurface 使用玻璃效果"
            )
        }

        for file in ["SettingsDesignSystem", "SettingsSidebar"] {
            let source = try TestSourceReader.read([
                "RoutinUsage", "Views", "Settings", "Components", "\(file).swift"
            ])
            XCTAssertFalse(
                source.contains("glassEffect("),
                "\(file) 必须通过 LiquidGlassSurface 使用玻璃效果"
            )
        }

        let ordering = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])
        let segmented = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsSegmentedControl.swift"
        ])
        XCTAssertTrue(ordering.contains("accessibilityAction"))
        XCTAssertTrue(ordering.contains("accessibilityLabel"))
        XCTAssertTrue(ordering.contains("reduceMotion"))
        XCTAssertTrue(segmented.contains("Picker(accessibilityLabel"))
        XCTAssertTrue(segmented.contains(".pickerStyle(.segmented)"))
    }

    func test设置弹窗共享标题关闭按钮按压反馈与减少透明度策略() throws {
        let design = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsDesignSystem.swift"
        ])
        let glass = try TestSourceReader.read([
            "RoutinUsage", "Views", "LiquidGlassSurface.swift"
        ])
        let modalFiles = [
            "RoutinUsage/Views/CredentialEditorView.swift",
            "RoutinUsage/Views/Settings/CredentialDetailsView.swift",
            "RoutinUsage/Views/Settings/CredentialAlertSettingsView.swift",
            "RoutinUsage/Views/Settings/TransferToAndroidView.swift",
            "RoutinUsage/Views/UsageShareEditorView.swift"
        ]

        XCTAssertTrue(design.contains("struct SettingsModalTitle"))
        XCTAssertTrue(design.contains("struct SettingsCloseButton"))
        XCTAssertTrue(design.contains("struct SettingsPressFeedbackButtonStyle"))
        XCTAssertTrue(design.contains("hoverScale"))
        XCTAssertTrue(design.contains("hoverRotationDegrees"))
        XCTAssertTrue(design.contains("dampingFraction: 0.72"))
        XCTAssertTrue(design.contains("accessibilityReduceMotion"))
        XCTAssertTrue(design.contains("struct SettingsFieldSurface"))
        XCTAssertTrue(design.contains("struct SettingsNotice"))
        XCTAssertTrue(design.contains("HStack(alignment: .center, spacing: 8)"))
        XCTAssertTrue(glass.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(glass.contains("onExitCommand"))

        for path in modalFiles {
            let components = path.split(separator: "/").map(String.init)
            let source = try TestSourceReader.read(components)
            XCTAssertTrue(
                source.contains("SettingsModalTitle(") || source.contains("SettingsCloseButton("),
                "\(path) 必须复用设置弹窗标题或关闭按钮"
            )
        }

        let keyEditor = try TestSourceReader.read([
            "RoutinUsage", "Views", "KeyEditorView.swift"
        ])
        XCTAssertTrue(keyEditor.contains("SettingsCloseButton("))
        XCTAssertFalse(keyEditor.contains("Image(systemName: \"xmark\")"))
    }

    func test自定义控件支持键盘方向键焦点环并跟随系统强调色() throws {
        let density = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageCardDensitySegmentedControl.swift"
        ])
        let credentials = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "CredentialManagementView.swift"
        ])
        let sidebar = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsSidebar.swift"
        ])
        let design = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsDesignSystem.swift"
        ])

        XCTAssertTrue(density.contains(".focusable(true)"))
        XCTAssertTrue(density.contains(".focused($isFocused)"))
        XCTAssertTrue(density.contains(".focusEffectDisabled()"))
        XCTAssertTrue(density.contains(".onKeyPress(.leftArrow)"))
        XCTAssertTrue(density.contains(".onKeyPress(.rightArrow)"))
        XCTAssertTrue(density.contains("strokeBorder(Color.accentColor"))

        XCTAssertTrue(credentials.contains("Menu {"))
        XCTAssertTrue(credentials.contains("CredentialFilterMenuControl("))
        XCTAssertTrue(credentials.contains("title: \"状态\""))
        XCTAssertTrue(credentials.contains("title: \"供应商\""))
        XCTAssertTrue(credentials.contains("Picker(selection: $model.filter.status)"))
        XCTAssertTrue(credentials.contains("Picker(selection: $model.filter.provider)"))
        XCTAssertTrue(credentials.contains(".pickerStyle(.inline)"))
        XCTAssertFalse(credentials.contains("ProviderFilterChips("))

        XCTAssertTrue(sidebar.contains("Color.accentColor"))
        XCTAssertTrue(design.contains("Color.accentColor"))
        XCTAssertFalse(sidebar.contains("CompactPopoverPalette.brand("))
        XCTAssertFalse(design.contains("CompactPopoverPalette.brand("))
    }
}
