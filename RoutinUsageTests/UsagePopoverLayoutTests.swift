import XCTest
@testable import RoutinUsage

final class UsagePopoverLayoutTests: XCTestCase {
    func test弹窗按内容展示并在超过屏幕时滚动() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "UsagePopoverView.swift"
        ])

        XCTAssertTrue(source.contains("ScrollView(.vertical, showsIndicators: false)"))
        XCTAssertTrue(source.contains("ThinVerticalScrollIndicator"))
        XCTAssertTrue(source.contains(".frame(width: 440)"))
        XCTAssertTrue(source.contains(".frame(maxHeight: maxPopoverHeight, alignment: .top)"))
        XCTAssertTrue(source.contains("visibleHeight * 0.9"))

        let controller = try TestSourceReader.read([
            "RoutinUsage",
            "App",
            "StatusBarController.swift"
        ])
        XCTAssertTrue(controller.contains("popover.contentSize = contentSize"))
        XCTAssertFalse(controller.contains("window.setContentSize(contentSize)"))
        XCTAssertTrue(controller.contains("window.invalidateShadow()"))
        XCTAssertTrue(controller.contains("screenHeight * 0.9"))
        XCTAssertTrue(controller.contains("view.fittingSize"))
        XCTAssertTrue(controller.contains("min(max(idealSize.height, 1), maximumHeight)"))
    }

    func test弹窗顶栏设置左侧有简洁完整分段() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("UsageCardDensitySegmentedControl"))
        XCTAssertTrue(source.contains("openSettings()"))
        let toolbar = try XCTUnwrap(source.range(of: "var toolbar: some View"))
        let toolbarSlice = source[toolbar.lowerBound...]
        let segmentedIndex = try XCTUnwrap(
            toolbarSlice.range(of: "UsageCardDensitySegmentedControl")
        ).lowerBound
        let gearIndex = try XCTUnwrap(
            toolbarSlice.range(of: "openSettings()")
        ).lowerBound

        XCTAssertTrue(segmentedIndex < gearIndex)
    }

    func test弹窗简洁模式单列排布并带切换动画() throws {
        let popover = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
            encoding: .utf8
        )
        let segmented = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsageCardDensitySegmentedControl.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(popover.contains("switch settings.usageCardDensity"))
        XCTAssertTrue(popover.contains("LazyVGrid(columns: [GridItem(.flexible())], spacing: 7)"))
        XCTAssertTrue(popover.contains("fullCardViews"))
        XCTAssertFalse(popover.contains("showsTopDivider: index > 0"))
        XCTAssertFalse(popover.contains("WaterfallLayout(columns: 2, spacing: 8)"))
        XCTAssertTrue(popover.contains(".id(settings.usageCardDensity)"))
        XCTAssertTrue(segmented.contains("response: 0.32"))
        XCTAssertTrue(segmented.contains("dampingFraction: 0.86"))
        XCTAssertTrue(segmented.contains("return \"rectangle.stack\""))
        XCTAssertTrue(segmented.contains("return \"list.bullet.rectangle\""))
        XCTAssertTrue(segmented.contains("DragGesture(minimumDistance: 6)"))
    }

    func test瀑布流布局按最短列放置卡片() {
        let layout = WaterfallLayout(columns: 2, spacing: 8)

        XCTAssertEqual(layout.columnWidth(forTotalWidth: 416), 204)
        XCTAssertEqual(layout.shortestColumnIndex(in: [120, 80]), 1)
        XCTAssertEqual(layout.shortestColumnIndex(in: [80, 120]), 0)
        XCTAssertEqual(layout.shortestColumnIndex(in: [60, 60]), 0)
    }

    func test供应商筛选使用统一实色控件() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/ProviderFilterMenu.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("chevron.down"))
        XCTAssertTrue(source.contains(".buttonStyle(.plain)"))
        XCTAssertTrue(source.contains(".menuIndicator(.hidden)"))
        XCTAssertFalse(source.contains("chevron.up.chevron.down"))
        XCTAssertFalse(source.contains(".menuStyle(.borderlessButton)"))
        XCTAssertTrue(source.contains("PopoverVisualPolicy.cornerRadius(for: .button)"))
    }

    func test卡片装饰层关闭命中测试且列表切换不带动画() throws {
        let row = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsageRowView.swift"),
            encoding: .utf8
        )
        let popover = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(row.contains(".allowsHitTesting(false)"))
        XCTAssertFalse(row.contains(".frame(maxWidth: .infinity, maxHeight: .infinity"))
        XCTAssertTrue(popover.contains(".transaction { $0.animation = nil }"))
    }

    func test弹窗设置按钮复用右键菜单激活逻辑() throws {
        let popover = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "UsagePopoverView.swift"
        ])
        let controller = try TestSourceReader.read([
            "RoutinUsage",
            "App",
            "StatusBarController.swift"
        ])

        XCTAssertTrue(popover.contains("openSettings()"))
        XCTAssertTrue(popover.contains("let openSettings: @MainActor () -> Void"))
        XCTAssertTrue(controller.contains("openSettings: { [weak self] in"))
        XCTAssertTrue(controller.contains("self?.openSettingsWindow()"))
    }

    func test弹窗版本号旁按更新状态切换检测与更新入口() throws {
        let popover = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "UsagePopoverView.swift"
        ])
        let controller = try TestSourceReader.read([
            "RoutinUsage",
            "App",
            "StatusBarController.swift"
        ])
        let toolbarStart = try XCTUnwrap(popover.range(of: "var toolbar: some View"))
        let toolbar = String(popover[toolbarStart.lowerBound...])

        XCTAssertTrue(popover.contains("typealias CheckForUpdates = @MainActor () async -> Void"))
        XCTAssertTrue(popover.contains("let checkForUpdates: CheckForUpdates"))
        XCTAssertTrue(toolbar.contains("if case let .available(update) = updateStatus"))
        XCTAssertTrue(toolbar.contains("selectedUpdate = update"))
        XCTAssertTrue(toolbar.contains("Circle()"))
        XCTAssertTrue(toolbar.contains(".trim(from: 0.08, to: 0.78)"))
        XCTAssertTrue(toolbar.contains("await checkForUpdates()"))
        XCTAssertTrue(toolbar.contains("检查更新"))
        XCTAssertTrue(controller.contains("checkForUpdates: environment.checkForUpdates"))
    }

    func test新版本提示位于左侧品牌区() throws {
        let popover = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])

        let brand = try XCTUnwrap(
            popover.range(of: "Link(destination: RoutinUsageApp.websiteURL)")
        )
        let updateControl = try XCTUnwrap(
            popover.range(of: "versionOrUpdateControl")
        )
        let spacer = try XCTUnwrap(
            popover.range(of: "Spacer(minLength: 8)")
        )

        XCTAssertLessThan(brand.lowerBound, updateControl.lowerBound)
        XCTAssertLessThan(updateControl.lowerBound, spacer.lowerBound)
        XCTAssertTrue(popover.contains("新版本"))
        XCTAssertTrue(popover.contains("arrow.up.circle.fill"))
        XCTAssertTrue(popover.contains("selectedUpdate = update"))
        XCTAssertFalse(popover.contains(".overlay(alignment: .center)"))
    }

    func test完整信息使用独立供应商卡片() throws {
        let popover = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])
        let row = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageRowView.swift"
        ])

        XCTAssertTrue(popover.contains("fullCardViews"))
        XCTAssertFalse(popover.contains("CompactPopoverPalette.surface(.card, colorScheme)"))
        XCTAssertFalse(popover.contains("showsTopDivider: index > 0"))
        XCTAssertFalse(row.contains("var showsTopDivider"))

        let fullStart = try XCTUnwrap(row.range(of: "private func fullCard(now: Date)"))
        let compactStart = try XCTUnwrap(row.range(of: "private func compactCard(now: Date)"))
        let fullBody = String(row[fullStart.lowerBound..<compactStart.lowerBound])
        XCTAssertTrue(fullBody.contains(".modifier(UsageCardChrome("))
    }

    func test底栏作为固定页脚不再悬浮覆盖内容() throws {
        let popover = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])

        XCTAssertTrue(popover.contains("bottomBar"))
        XCTAssertFalse(popover.contains(".overlay(alignment: .bottom)"))
        XCTAssertTrue(popover.contains("PopoverFooterActionButton"))
        XCTAssertTrue(popover.contains(".frame(height: 48)"))
    }

    func test供应商筛选使用设计稿漏斗图标() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "ProviderFilterMenu.swift"
        ])

        XCTAssertTrue(source.contains("FunnelIcon()"))
        XCTAssertFalse(source.contains("line.3.horizontal.decrease"))
    }
}
