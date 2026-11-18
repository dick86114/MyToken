import XCTest

final class MenuBarManagementViewTests: XCTestCase {
    func test菜单栏管理页用单行状态栏替代预览区并保持整卡排序() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])

        XCTAssertTrue(source.contains("MenuBarManagementView"))
        XCTAssertTrue(source.contains("ReorderableCredentialCardList"))
        XCTAssertTrue(source.contains("菜单栏管理"))
        XCTAssertFalse(source.contains("previewPanel"))
        XCTAssertFalse(source.contains("popoverContent"))
        XCTAssertFalse(source.contains("previewCredentialRow"))
        XCTAssertFalse(source.contains("ViewThatFits"))
        XCTAssertTrue(source.contains("systemMenuBarStrip(highlightIndicator: true)"))
        XCTAssertTrue(source.contains("appearancePanel"))
        XCTAssertTrue(source.contains("usageCardPreview"))
        XCTAssertTrue(source.contains("MenuBarUsageCardThresholdPreview("))
        XCTAssertTrue(source.contains("systemImage: \"arrow.up.arrow.down\""))
        XCTAssertTrue(source.contains("struct MenuBarSelectionControl"))
        XCTAssertTrue(source.contains("Text(value)"))
        XCTAssertTrue(source.contains("Image(systemName: \"chevron.down\")"))
        XCTAssertTrue(source.contains("accessibilityValue(value)"))
        XCTAssertFalse(source.contains("Button(\"自动\")"))
        XCTAssertFalse(source.contains("自动（原指标当前不可用）"))
        XCTAssertTrue(source.contains("CompactUsageGauge("))
        XCTAssertTrue(source.contains("UsageMetricProgressBar("))
        XCTAssertTrue(source.contains("用量卡片预览"))
        XCTAssertTrue(source.contains("模拟数据"))
        XCTAssertTrue(source.contains("正常区间"))
        XCTAssertTrue(source.contains("提醒区间"))
        XCTAssertTrue(source.contains("紧急区间"))
        XCTAssertTrue(source.contains("layout: .compact"))
        XCTAssertTrue(source.contains("frame(width: appearancePanelWidth"))
        XCTAssertTrue(source.contains("itemHeight: orderRowHeight"))
        XCTAssertTrue(source.contains("private let orderRowHeight: CGFloat = 52"))
        XCTAssertTrue(source.contains("HStack(spacing: 6)"))
        XCTAssertTrue(source.contains(".lineLimit(1)"))
        XCTAssertTrue(source.contains("reorderingDisplay"))
        XCTAssertTrue(source.contains("Menu {"))
        XCTAssertTrue(source.contains("菜单栏指标"))
        XCTAssertTrue(source.contains("MenuBarMetricResolver.options"))
        XCTAssertTrue(source.contains("MenuBarIndicatorStyle.availableStyles"))
        XCTAssertTrue(source.contains("selectedStyle.systemImage(for: metric)"))
        XCTAssertTrue(source.contains("value: selectedStyle.title(for: metric)"))
        XCTAssertTrue(source.contains("菜单栏指标样式"))
        XCTAssertTrue(source.contains("checkmark.circle.fill"))
        XCTAssertTrue(source.contains("plus.circle.fill"))
        XCTAssertTrue(source.contains("CompactPopoverPalette.positive(colorScheme) : Color.accentColor"))
        XCTAssertTrue(source.contains("font(.system(size: 21, weight: .semibold))"))
        XCTAssertTrue(source.contains("setUsagePreferences"))
        XCTAssertTrue(source.contains("accessibilityLabel"))
        XCTAssertTrue(source.contains("前往凭证管理"))
        XCTAssertTrue(source.contains("openCredentials"))
        XCTAssertFalse(source.contains("unifiedStates.prefix(3)"))
        XCTAssertFalse(source.contains("onDrag"))
        XCTAssertFalse(source.contains("onDrop"))
        XCTAssertFalse(source.contains(".frame(height: 178)"))
        XCTAssertFalse(source.contains(".liquidGlassControlSurface()"))
        XCTAssertTrue(source.contains("hoveredManagementID"))
        XCTAssertTrue(source.contains("SettingsDesign.hoverFill(colorScheme)"))
    }

    func test菜单栏排序卡显示供应商Logo() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])

        let cardStart = try XCTUnwrap(
            source.range(of: "private func managementCardContent")
        )
        let cardEnd = try XCTUnwrap(
            source.range(of: "private func pruneDisabledDisplayOrder", range: cardStart.upperBound..<source.endIndex)
        )
        let card = source[cardStart.lowerBound..<cardEnd.lowerBound]
        XCTAssertTrue(card.contains("providerLogo(for: state, size: 24)"))

        XCTAssertEqual(
            source.components(separatedBy: "providerLogo(for: state").count - 1,
            1
        )
        XCTAssertTrue(source.contains("private func providerLogo("))
    }
}
