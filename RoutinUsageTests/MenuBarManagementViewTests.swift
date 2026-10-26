import XCTest

final class MenuBarManagementViewTests: XCTestCase {
    func test菜单栏管理页使用手势整卡排序和合并预览() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])

        XCTAssertTrue(source.contains("MenuBarManagementView"))
        XCTAssertTrue(source.contains("ReorderableCredentialCardList"))
        XCTAssertTrue(source.contains("菜单栏管理"))
        XCTAssertTrue(source.contains("previewPanel(title: \"预览\""))
        XCTAssertFalse(source.contains("previewPanel(title: \"菜单栏预览\""))
        XCTAssertFalse(source.contains("previewPanel(title: \"弹窗预览\""))
        XCTAssertTrue(source.contains("HStack(alignment: .top, spacing: 20)"))
        XCTAssertTrue(source.contains("VStack(alignment: .leading, spacing: 16)"))
        XCTAssertTrue(source.contains(".frame(width: 440)"))
        XCTAssertTrue(source.contains(".frame(minWidth: 360, maxWidth: .infinity)"))
        XCTAssertTrue(source.contains("SystemPopoverArrow"))
        XCTAssertTrue(source.contains("PopoverColorBrandLogo"))
        XCTAssertTrue(source.contains("RoundedRectangle(cornerRadius: 10, style: .continuous)"))
        XCTAssertTrue(source.contains("strokeBorder(Color.primary.opacity(0.16), lineWidth: 1)"))
        XCTAssertFalse(source.contains("arrowtriangle.up.fill"))
        XCTAssertFalse(source.contains(".padding(.trailing, 62)"))
        XCTAssertTrue(source.contains(".lineLimit(1)"))
        XCTAssertTrue(source.contains("ProviderTheme.background(for:"))
        XCTAssertFalse(source.contains("CredentialSummaryRow("))
        XCTAssertTrue(source.contains("reorderingDisplay"))
        XCTAssertTrue(source.contains("Menu {"))
        XCTAssertTrue(source.contains("菜单栏指标"))
        XCTAssertTrue(source.contains("MenuBarMetricResolver.options"))
        XCTAssertTrue(source.contains("MenuBarIndicatorStyle.availableStyles"))
        XCTAssertTrue(source.contains("selectedStyle.systemImage(for: metric)"))
        XCTAssertTrue(source.contains("菜单栏指标样式"))
        XCTAssertTrue(source.contains("checkmark.circle.fill"))
        XCTAssertTrue(source.contains("plus.circle.fill"))
        XCTAssertTrue(source.contains("CompactPopoverPalette.positive(colorScheme) : CompactPopoverPalette.brand(colorScheme)"))
        XCTAssertTrue(source.contains("font(.system(size: 26, weight: .semibold))"))
        XCTAssertTrue(source.contains("setUsagePreferences"))
        XCTAssertTrue(source.contains("accessibilityLabel"))
        XCTAssertFalse(source.contains("unifiedStates.prefix(3)"))
        XCTAssertFalse(source.contains("onDrag"))
        XCTAssertFalse(source.contains("onDrop"))
        XCTAssertFalse(source.contains(".frame(height: 178)"))
    }

    func test菜单栏管理页预览和凭证卡显示供应商Logo() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "MenuBarManagementView.swift"
        ])

        let previewStart = try XCTUnwrap(
            source.range(of: "private func previewCredentialRow")
        )
        let previewEnd = try XCTUnwrap(
            source.range(of: "private func previewPanel", range: previewStart.upperBound..<source.endIndex)
        )
        let preview = source[previewStart.lowerBound..<previewEnd.lowerBound]
        XCTAssertTrue(preview.contains("providerLogo(for: state, size: 20)"))

        let cardStart = try XCTUnwrap(
            source.range(of: "private func managementCardContent")
        )
        let cardEnd = try XCTUnwrap(
            source.range(of: "private func pruneDisabledDisplayOrder", range: cardStart.upperBound..<source.endIndex)
        )
        let card = source[cardStart.lowerBound..<cardEnd.lowerBound]
        XCTAssertTrue(card.contains("providerLogo(for: state, size: 28)"))

        XCTAssertEqual(
            source.components(separatedBy: "providerLogo(for: state").count - 1,
            2
        )
        XCTAssertTrue(source.contains("private func providerLogo("))
    }
}
