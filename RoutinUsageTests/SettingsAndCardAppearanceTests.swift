import XCTest
@testable import RoutinUsage

final class SettingsAndCardAppearanceTests: XCTestCase {
    func test弹窗设置按关闭弹窗置前注册再激活的顺序打开() throws {
        let source = try sourceText(
            at: "RoutinUsage/App/StatusBarController.swift"
        )
        let functionStart = try XCTUnwrap(
            source.range(of: "@objc private func openSettingsWindow()")
        )
        let functionEnd = try XCTUnwrap(
            source.range(of: "private func makeSettingsWindow()")
        )
        let body = source[functionStart.lowerBound..<functionEnd.lowerBound]

        let closeIndex = try XCTUnwrap(body.range(of: "closePopover()"))
        let policyIndex = try XCTUnwrap(
            body.range(of: "NSApp.setActivationPolicy(.regular)")
        )
        let orderIndex = try XCTUnwrap(
            body.range(of: "window.makeKeyAndOrderFront(nil)")
        )
        let registerIndex = try XCTUnwrap(
            body.range(of: "SettingsWindowActivationPolicy.register(window)")
        )
        let activateIndex = try XCTUnwrap(
            body.range(of: "NSApp.activate(ignoringOtherApps: true)")
        )

        XCTAssertLessThan(closeIndex.lowerBound, policyIndex.lowerBound)
        XCTAssertLessThan(policyIndex.lowerBound, orderIndex.lowerBound)
        XCTAssertLessThan(orderIndex.lowerBound, registerIndex.lowerBound)
        XCTAssertLessThan(registerIndex.lowerBound, activateIndex.lowerBound)
        XCTAssertEqual(body.components(separatedBy: "NSApp.activate(ignoringOtherApps: true)").count - 1, 1)
    }

    func test简洁卡片标题区没有分隔线() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/UsageRowView.swift"
        )
        let headerStart = try XCTUnwrap(
            source.range(of: "private func compactHeader(")
        )
        let headerEnd = try XCTUnwrap(
            source.range(of: "private func compactIdentity(")
        )
        let header = source[headerStart.lowerBound..<headerEnd.lowerBound]

        XCTAssertFalse(source.contains("compactHeader(showsDivider"))
        XCTAssertFalse(header.contains("Rectangle()"))
        XCTAssertFalse(header.contains("CompactPopoverPalette.hairline"))
    }

    func test余额卡片不渲染波浪背景() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/UsageRowView.swift"
        )
        let compactViews = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )

        XCTAssertFalse(source.contains("CompactBalanceWave()"))
        XCTAssertFalse(compactViews.contains("struct CompactBalanceWave"))
        XCTAssertFalse(source.contains("struct CompactBalanceWave"))
    }

    func test简洁用量卡片不叠加内层描边() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )
        let gaugeStart = try XCTUnwrap(
            source.range(of: "struct CompactMetricGaugeTile")
        )
        let valueStart = try XCTUnwrap(
            source.range(of: "struct CompactValueTile")
        )
        let balanceStart = try XCTUnwrap(
            source.range(of: "struct CompactBalanceStrip")
        )
        let gaugeTile = source[gaugeStart.lowerBound..<valueStart.lowerBound]
        let valueTile = source[valueStart.lowerBound..<balanceStart.lowerBound]

        XCTAssertFalse(gaugeTile.contains(".overlay"))
        XCTAssertFalse(gaugeTile.contains("strokeBorder"))
        XCTAssertFalse(valueTile.contains(".overlay"))
        XCTAssertFalse(valueTile.contains("strokeBorder"))
        XCTAssertFalse(source.contains("static func tileStroke"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent(relativePath)
        return try String(contentsOf: url, encoding: .utf8)
    }
}
