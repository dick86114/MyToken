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

    func test简洁用量卡片不绘制内层底卡() throws {
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

        XCTAssertFalse(gaugeTile.contains(".background"))
        XCTAssertFalse(valueTile.contains(".background"))
    }

    func test用量小卡片使用无色液态玻璃() throws {
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
        let glassModifier = ".compactUsageGlass(cornerRadius: CompactPopoverMetrics.metricCornerRadius)"

        XCTAssertTrue(gaugeTile.contains(glassModifier))
        XCTAssertTrue(valueTile.contains(glassModifier))
        XCTAssertTrue(source.contains("if #available(macOS 26.0, *)"))
        XCTAssertTrue(source.contains(".glassEffect(.regular, in: shape)"))
        XCTAssertFalse(gaugeTile.contains(".regular.tint"))
        XCTAssertFalse(valueTile.contains(".regular.tint"))
    }

    func test进度状态色只作用于进度本体() throws {
        let gridSource = try sourceText(
            at: "RoutinUsage/Views/NormalizedUsageMetricGrid.swift"
        )
        let compactSource = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )

        let progressStart = try XCTUnwrap(
            gridSource.range(of: "private var progressCell")
        )
        let progressBarStart = try XCTUnwrap(
            gridSource.range(of: "UsageMetricProgressBar", range: progressStart.upperBound..<gridSource.endIndex)
        )
        let progressHeader = gridSource[progressStart.lowerBound..<progressBarStart.lowerBound]
        XCTAssertFalse(progressHeader.contains("healthColor"))
        XCTAssertTrue(progressHeader.contains(".foregroundStyle(.primary)"))

        let gaugeStart = try XCTUnwrap(
            compactSource.range(of: "struct CompactUsageGauge")
        )
        let gaugeEnd = try XCTUnwrap(
            compactSource.range(of: "struct CompactMetricGaugeTile")
        )
        let gauge = compactSource[gaugeStart.lowerBound..<gaugeEnd.lowerBound]
        let statusColorCount = gauge.components(
            separatedBy: "UsageMetricPresentation.color"
        ).count - 1
        let labelStart = try XCTUnwrap(
            gauge.range(of: "private var gaugeLabel")
        )
        XCTAssertEqual(statusColorCount, 1)
        XCTAssertTrue(gauge.contains("private var gaugeColor"))
        XCTAssertLessThan(
            try XCTUnwrap(gauge.range(of: "UsageMetricPresentation.color")).lowerBound,
            labelStart.lowerBound
        )

        let tileStart = try XCTUnwrap(
            compactSource.range(of: "struct CompactMetricGaugeTile")
        )
        let valueStart = try XCTUnwrap(
            compactSource.range(of: "struct CompactValueTile")
        )
        let gaugeTile = compactSource[tileStart.lowerBound..<valueStart.lowerBound]
        XCTAssertFalse(gaugeTile.contains("UsageMetricPresentation.color"))
        XCTAssertFalse(gaugeTile.contains("Circle()"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent(relativePath)
        return try String(contentsOf: url, encoding: .utf8)
    }
}
