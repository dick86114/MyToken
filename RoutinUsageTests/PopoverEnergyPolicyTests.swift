import Foundation
import XCTest
@testable import RoutinUsage

final class PopoverEnergyPolicyTests: XCTestCase {
    func test刷新边框在低电量或减弱动态时暂停() {
        XCTAssertTrue(RefreshAnimationPolicy.isPaused(reduceMotion: true, lowPowerModeEnabled: false))
        XCTAssertTrue(RefreshAnimationPolicy.isPaused(reduceMotion: false, lowPowerModeEnabled: true))
        XCTAssertFalse(RefreshAnimationPolicy.isPaused(reduceMotion: false, lowPowerModeEnabled: false))
    }

    func test刷新边框最低帧率不超过12fps() {
        XCTAssertEqual(RefreshAnimationPolicy.minimumInterval, 1.0 / 12.0, accuracy: 0.0001)
    }

    func test状态栏弹窗只在打开时创建内容并在关闭后释放() throws {
        let controller = try readSourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let configureRange = try XCTUnwrap(controller.range(of: "private func configurePopover() {"))
        let ensureRange = try XCTUnwrap(controller.range(of: "private func ensurePopoverContent() {"))

        let configureBody = String(controller[configureRange.lowerBound..<ensureRange.lowerBound])
        XCTAssertFalse(configureBody.contains("NSHostingController"))
        XCTAssertTrue(controller.contains("ensurePopoverContent()"))
        XCTAssertTrue(controller.contains("releasePopoverContent()"))
        XCTAssertTrue(controller.contains("popover.contentViewController = nil"))
    }
}
