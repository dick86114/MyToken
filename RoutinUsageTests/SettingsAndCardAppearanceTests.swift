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

        let closeIndex = try XCTUnwrap(body.range(of: "popover.performClose(nil)"))
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

    func test卡片头像使用低饱和供应商色() throws {
        let source = try sourceText(
            at: "RoutinUsage/Views/CompactUsageCardViews.swift"
        )
        let avatarStart = try XCTUnwrap(
            source.range(of: "struct CompactAccountAvatar: View")
        )
        let avatarEnd = try XCTUnwrap(
            source.range(of: "struct CompactCardActionButton")
        )
        let avatar = source[avatarStart.lowerBound..<avatarEnd.lowerBound]

        XCTAssertTrue(avatar.contains(".saturation(0.55)"))
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

    private func sourceText(at relativePath: String) throws -> String {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent(relativePath)
        return try String(contentsOf: url, encoding: .utf8)
    }
}
