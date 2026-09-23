import XCTest
@testable import RoutinUsage

final class UsagePopoverLayoutTests: XCTestCase {
    func test弹窗定位优先跟随状态按钮所在屏幕() {
        let origin = PopoverWindowPlacement.origin(
            popoverSize: NSSize(width: 240, height: 180),
            anchorRect: NSRect(x: 150, y: 940, width: 30, height: 24),
            visibleFrame: NSRect(x: 0, y: 0, width: 800, height: 920)
        )

        XCTAssertEqual(origin.x, 45, accuracy: 0.01)
        XCTAssertEqual(origin.y, 740, accuracy: 0.01)
    }

    func test弹窗定位超出当前屏幕时保持可见() {
        let origin = PopoverWindowPlacement.origin(
            popoverSize: NSSize(width: 240, height: 180),
            anchorRect: NSRect(x: 790, y: 940, width: 30, height: 24),
            visibleFrame: NSRect(x: 0, y: 0, width: 800, height: 920)
        )

        XCTAssertEqual(origin.x, 560, accuracy: 0.01)
        XCTAssertEqual(origin.y, 740, accuracy: 0.01)
    }

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
        XCTAssertTrue(controller.contains("window.setContentSize(contentSize)"))
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

        XCTAssertTrue(popover.contains("usageCardDensity == .compact"))
        XCTAssertTrue(popover.contains("LazyVGrid(columns: [GridItem(.flexible())], spacing: 14)"))
        XCTAssertTrue(popover.contains("LazyVGrid(columns: [GridItem(.flexible())], spacing: 8)"))
        XCTAssertFalse(popover.contains("WaterfallLayout(columns: 2, spacing: 8)"))
        XCTAssertTrue(popover.contains(".id(settings.usageCardDensity)"))
        XCTAssertTrue(segmented.contains("withAnimation(.spring(response: 0.35, dampingFraction: 0.8))"))
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
        XCTAssertTrue(toolbar.contains("Image(systemName: \"arrow.triangle.2.circlepath\")"))
        XCTAssertTrue(toolbar.contains("await checkForUpdates()"))
        XCTAssertTrue(toolbar.contains("检测更新"))
        XCTAssertTrue(controller.contains("checkForUpdates: environment.checkForUpdates"))
    }
}
