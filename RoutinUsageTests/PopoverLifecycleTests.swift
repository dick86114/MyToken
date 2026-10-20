import XCTest

final class PopoverLifecycleTests: XCTestCase {
    func test弹窗关闭动画结束后才释放内容并允许再次打开() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift"
        ])

        XCTAssertTrue(source.contains("NSObject, NSPopoverDelegate"))
        XCTAssertTrue(source.contains("popover.delegate = self"))
        XCTAssertTrue(source.contains("isPopoverClosing"))
        XCTAssertTrue(source.contains("func popoverDidClose"))
        XCTAssertTrue(source.contains("guard !isPopoverClosing"))
        XCTAssertTrue(source.contains("releasePopoverContent()"))

        let closeStart = try XCTUnwrap(source.range(of: "private func closePopover()"))
        let closeEnd = try XCTUnwrap(
            source.range(
                of: "/// 关闭动画结束前",
                range: closeStart.lowerBound..<source.endIndex
            )
        )
        let closeMethod = source[closeStart.lowerBound..<closeEnd.lowerBound]
        XCTAssertTrue(closeMethod.contains("popover.performClose(nil)"))
        XCTAssertFalse(closeMethod.contains("releasePopoverContent()"))
    }


    func test弹窗尺寸刷新通过NSPopover公开接口并刷新窗口阴影() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift"
        ])

        XCTAssertTrue(source.contains("popover.contentSize = refreshedSize"))
        XCTAssertTrue(source.contains("window.invalidateShadow()"))
        XCTAssertFalse(source.contains("window.setContentSize(refreshedSize)"))
        XCTAssertFalse(source.contains("self.positionPopoverWindow(window, anchoredTo: button)"))
    }

    func test右键菜单关闭弹窗时同样等待关闭动画完成() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift"
        ])
        let menuStart = try XCTUnwrap(source.range(of: "private func showContextMenu"))
        let menuMethod = source[menuStart.lowerBound...]

        XCTAssertTrue(menuMethod.contains("closePopover()"))
        XCTAssertFalse(menuMethod.contains("popover.performClose(nil)"))
    }
}
