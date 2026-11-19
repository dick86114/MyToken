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

    func test简洁余额使用明细行直接展示两类余额() throws {
        let compactViews = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )

        XCTAssertTrue(compactViews.contains("struct BalanceCompositionDetails"))
        XCTAssertTrue(compactViews.contains("struct BalanceCompositionDetailItem"))
        XCTAssertTrue(compactViews.contains("ViewThatFits(in: .horizontal)"))
        XCTAssertFalse(compactViews.contains("caption: String = \"可用余额\""))
        XCTAssertFalse(compactViews.contains("Text(caption)"))
        XCTAssertTrue(compactViews.contains("HStack(alignment: .firstTextBaseline, spacing: 6)"))
        XCTAssertTrue(compactViews.contains("Text(segment.compactLabel)"))
        XCTAssertTrue(compactViews.contains("UsageFormatter.currencyText(segment.value, currencyCode: currencyCode)"))
        XCTAssertTrue(compactViews.contains("case \"toppedUpBalance\":"))
        XCTAssertTrue(compactViews.contains("case \"grantedBalance\":"))
        XCTAssertTrue(compactViews.contains("case \"cash-balance\":"))
        XCTAssertTrue(compactViews.contains("case \"gift-balance\":"))
        XCTAssertTrue(compactViews.contains("Circle()"))
        XCTAssertFalse(compactViews.contains("separatorSize"))
        XCTAssertFalse(compactViews.contains("static let width: CGFloat = 164"))
        XCTAssertFalse(compactViews.contains("BalanceCompositionBar"))
        XCTAssertFalse(compactViews.contains("BalanceCompositionHoverRegion"))
        XCTAssertFalse(compactViews.contains("BalanceCompositionDetailCard"))
        XCTAssertFalse(compactViews.contains("BalanceCompositionChip"))
        XCTAssertFalse(compactViews.contains(".popover(isPresented: $isHovered"))
        XCTAssertFalse(compactViews.contains("static let hoverHeight"))
        XCTAssertFalse(compactViews.contains("BalanceCompositionCalloutShape"))
        XCTAssertFalse(compactViews.contains("onContinuousHover"))
        XCTAssertFalse(compactViews.contains("CompactUsageCardPresentation.balanceStatusText"))
        XCTAssertFalse(compactViews.contains("balanceStatusText"))
    }

    func test余额卡片移除聚焦环上方状态文案() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/UsageRowView.swift"
        )
        let selectorStart = try XCTUnwrap(source.range(of: "private static func balanceCompositionMetrics"))
        let selectorEnd = try XCTUnwrap(
            source.range(of: "\n}", range: selectorStart.lowerBound..<source.endIndex)
        )
        let selector = String(source[selectorStart.lowerBound..<selectorEnd.lowerBound])
        let balanceStart = try XCTUnwrap(source.range(of: "private func compactBalanceCard"))
        let headerStart = try XCTUnwrap(
            source.range(of: "private func compactHeader", range: balanceStart.lowerBound..<source.endIndex)
        )
        let balanceCard = String(source[balanceStart.lowerBound..<headerStart.lowerBound])

        XCTAssertFalse(balanceCard.contains("balanceStatusText"))
        XCTAssertFalse(balanceCard.contains("Capsule().fill(balanceAccent.opacity(0.10))"))
        XCTAssertTrue(balanceCard.contains("compositionMetrics"))
        XCTAssertTrue(selector.contains("[\"toppedUpBalance\", \"grantedBalance\", \"cash-balance\", \"gift-balance\"]"))
    }

    func test简洁指标卡使用一层细描边() throws {
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

        XCTAssertTrue(gaugeTile.contains(".overlay"))
        XCTAssertTrue(gaugeTile.contains("strokeBorder"))
        XCTAssertTrue(valueTile.contains(".overlay"))
        XCTAssertTrue(valueTile.contains("strokeBorder"))
        XCTAssertFalse(source.contains("static func tileStroke"))
    }

    func test简洁指标卡使用实色底卡() throws {
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

        XCTAssertTrue(gaugeTile.contains(".background"))
        XCTAssertTrue(gaugeTile.contains("CompactPopoverPalette.surface(.metric, colorScheme)"))
        XCTAssertTrue(valueTile.contains(".background"))
        XCTAssertTrue(valueTile.contains("CompactPopoverPalette.surface(.metric, colorScheme)"))
    }

    func test简洁指标卡不再叠加玻璃材质() throws {
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
        XCTAssertFalse(source.contains(".compactUsageGlass"))
        XCTAssertFalse(source.contains(".glassEffect(.regular, in: shape)"))
        XCTAssertTrue(gaugeTile.contains("RoundedRectangle("))
        XCTAssertTrue(gaugeTile.contains(".fill(CompactPopoverPalette.surface(.metric, colorScheme))"))
        XCTAssertTrue(valueTile.contains(".fill(CompactPopoverPalette.surface(.metric, colorScheme))"))
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
            separatedBy: "CompactPopoverPalette.statusColor"
        ).count - 1
        let labelStart = try XCTUnwrap(
            gauge.range(of: "private var gaugeLabel")
        )
        XCTAssertEqual(statusColorCount, 1)
        XCTAssertTrue(gauge.contains("private var gaugeColor"))
        XCTAssertTrue(gauge.contains("var color: Color? = nil"))
        XCTAssertLessThan(
            try XCTUnwrap(gauge.range(of: "CompactPopoverPalette.statusColor")).lowerBound,
            labelStart.lowerBound
        )

        let tileStart = try XCTUnwrap(
            compactSource.range(of: "struct CompactMetricGaugeTile")
        )
        let valueStart = try XCTUnwrap(
            compactSource.range(of: "struct CompactValueTile")
        )
        let gaugeTile = compactSource[tileStart.lowerBound..<valueStart.lowerBound]
        XCTAssertTrue(gaugeTile.contains("CompactUsageCardPresentation.gaugeTone"))
        XCTAssertTrue(gaugeTile.contains("UsageMetricPresentation.color("))
        XCTAssertTrue(gaugeTile.contains("compactPeriodLabel(for: metric)"))
        XCTAssertTrue(gaugeTile.contains("UsageResetCardsBadge("))
        XCTAssertFalse(gaugeTile.contains("UsageMetricProgressBar"))
        XCTAssertFalse(gaugeTile.contains("Circle()"))
    }

    func test深色供应商Logo使用明亮底板避免失去对比度() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/ProviderLogoBadge.swift"
        )

        XCTAssertTrue(source.contains("Color.white.opacity(0.92)"))
    }

    func test简洁仪表使用扇形填充面积() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )

        XCTAssertTrue(source.contains("GaugeSectorShape(progress: clamped / 100)"))
        XCTAssertTrue(source.contains(".fill(ringColor.opacity("))
        XCTAssertTrue(source.contains("style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent(relativePath)
        return try String(contentsOf: url, encoding: .utf8)
    }
}
