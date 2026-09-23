import XCTest

final class ProgressColorConfigurationTests: XCTestCase {
    func test卡片进度色统一读取设置颜色规则() throws {
        let presentation = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageMetricPresentation.swift"
        ])
        let row = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageRowView.swift"
        ])
        let cards = try TestSourceReader.read([
            "RoutinUsage", "Views", "CompactUsageCardViews.swift"
        ])
        let popover = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])
        let settings = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "SettingsWindowView.swift"
        ])

        XCTAssertTrue(presentation.contains("@Environment(\\.menuBarColorRules)"))
        XCTAssertTrue(presentation.contains("rules: MenuBarColorRules"))
        XCTAssertTrue(row.contains("@Environment(\\.menuBarColorRules)"))
        XCTAssertTrue(row.contains("rules: menuBarColorRules"))
        XCTAssertTrue(cards.contains("@Environment(\\.menuBarColorRules)"))
        XCTAssertTrue(popover.contains(".environment(\\.menuBarColorRules"))
        XCTAssertTrue(settings.contains(".environment(\\.menuBarColorRules"))
    }
}
