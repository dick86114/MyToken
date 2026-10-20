import AppKit
import Foundation
import XCTest
@testable import RoutinUsage

final class UpdateCompletionNoticeTests: XCTestCase {
    func test旧版完成标记仍能降级恢复版本提示() throws {
        let suiteName = "UpdateCompletionNoticeTests.legacy-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        defaults.set("5.6.9", forKey: "updateCompletionVersion")

        let notice = try XCTUnwrap(UpdateCompletionNotice.consume(defaults: defaults))

        XCTAssertEqual(notice.version, "5.6.9")
        XCTAssertNil(notice.previousVersion)
        XCTAssertTrue(notice.notes.isEmpty)
        XCTAssertEqual(
            notice.releaseURL.absoluteString,
            "https://github.com/dick86114/MyToken/releases/tag/v5.6.9"
        )
        XCTAssertNil(UpdateCompletionNotice.consume(defaults: defaults))
    }

    func test更新日志摘要只保留macOS区并按分类整理() {
        let notes = """
        ## macOS
        ### 新增
        - 支持更新完成弹窗展示当前版本日志
        ### 修复
        - 修复深色模式下弹窗文字对比度
        - 修复更新后可能重复弹窗

        ## Android
        - 修复 Android 登录问题
        """

        let items = UpdateReleaseSummaryBuilder.items(from: notes)

        XCTAssertEqual(items.map(\.category), [.added, .fixed, .fixed])
        XCTAssertEqual(
            items.map(\.text),
            [
                "支持更新完成弹窗展示当前版本日志",
                "修复深色模式下弹窗文字对比度",
                "修复更新后可能重复弹窗"
            ]
        )
        XCTAssertFalse(items.contains { $0.text.contains("Android") })
    }

    func test日志摘要可从条目文字识别分类() {
        let notes = """
        新增：展示更新日志
        修复：避免重复提示
        优化：压缩弹窗高度
        更新应用图标
        """

        let items = UpdateReleaseSummaryBuilder.items(from: notes)

        XCTAssertEqual(items.map(\.category), [.added, .fixed, .improved, .other])
        XCTAssertEqual(items.map(\.text), [
            "展示更新日志",
            "避免重复提示",
            "压缩弹窗高度",
            "更新应用图标"
        ])
    }

    func test没有更新日志时摘要为空() {
        XCTAssertTrue(UpdateReleaseSummaryBuilder.items(from: " \n ").isEmpty)
    }


    func test更新完成弹窗高度随条目收紧且不会出现大段留白() {
        let notes = """
        ## macOS
        - 新增一
        - 新增二
        - 修复三
        - 修复四
        - 优化五
        """
        let notice = UpdateCompletionNotice(
            version: "5.7.0",
            previousVersion: "5.6.9",
            notes: notes,
            releaseURL: URL(string: "https://example.com/v5.7.0")!,
            publishedAt: nil
        )

        let height = UpdateCompletionNoticeView.preferredHeight(for: notice)

        XCTAssertGreaterThanOrEqual(height, 440)
        XCTAssertLessThanOrEqual(height, 500)
    }

    func test更新完成窗口使用无标题栏可键盘激活窗口() throws {
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateCompletionNoticeView.swift"
        ])

        XCTAssertTrue(view.contains("UpdateCompletionNoticeWindow"))
        XCTAssertTrue(view.contains("override var canBecomeKey"))
        XCTAssertTrue(view.contains(".borderless"))
    }

    func test更新完成窗口置顶并跨空间显示() throws {
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateCompletionNoticeView.swift"
        ])

        XCTAssertTrue(view.contains("window.level = .floating"))
        XCTAssertTrue(view.contains("window.collectionBehavior"))
        XCTAssertTrue(view.contains(".canJoinAllSpaces"))
        XCTAssertTrue(view.contains(".fullScreenAuxiliary"))
        XCTAssertTrue(view.contains("window.hidesOnDeactivate = false"))
        XCTAssertTrue(view.contains("orderFrontRegardless"))
    }

    func test更新完成窗口默认居中于当前屏幕可视区域() {
        let origin = UpdateCompletionWindowPlacement.centeredOrigin(
            windowSize: NSSize(width: 640, height: 480),
            visibleFrame: NSRect(x: 0, y: 0, width: 2048, height: 1120)
        )

        XCTAssertEqual(origin.x, 704, accuracy: 0.01)
        XCTAssertEqual(origin.y, 320, accuracy: 0.01)
    }

    func test更新完成弹窗按钮悬停显示手指光标() throws {
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateCompletionNoticeView.swift"
        ])
        let cursorSupport = try TestSourceReader.read([
            "RoutinUsage", "Views", "PointingHandCursor.swift"
        ])

        XCTAssertTrue(cursorSupport.contains("NSCursor.pointingHand.push()"))
        XCTAssertTrue(cursorSupport.contains("NSCursor.pop()"))
        XCTAssertGreaterThanOrEqual(
            view.components(separatedBy: "pointingHandCursorOnHover()").count - 1,
            3
        )
        XCTAssertFalse(view.contains("addCursorRect"))
    }

    func test更新完成窗口标题日志和空白区域支持鼠标拖动() throws {
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateCompletionNoticeView.swift"
        ])

        XCTAssertTrue(view.contains("UpdateCompletionDragHandle"))
        XCTAssertTrue(view.contains("performDrag(with: event)"))
        XCTAssertTrue(view.contains("draggableSpacer"))
        XCTAssertTrue(view.contains("draggableNotesContent"))
        XCTAssertTrue(view.contains("acceptsFirstMouse"))
        XCTAssertTrue(view.contains("ZStack(alignment: .topLeading)"))
        XCTAssertGreaterThanOrEqual(
            view.components(separatedBy: "UpdateCompletionDragHandle()").count - 1,
            5
        )
    }

    func test当前版本详情复用窗口但使用当前版本标题() {
        let presentation = UpdateNoticePresentation.currentVersion

        XCTAssertEqual(presentation.heading(version: "5.7.3"), "当前版本 v5.7.3")
        XCTAssertEqual(presentation.windowTitle, "版本更新内容")
    }

    func test更新完成弹窗展示版本关系日志摘要和完整入口() throws {
        let view = try TestSourceReader.read([
            "RoutinUsage", "Views", "UpdateCompletionNoticeView.swift"
        ])

        XCTAssertTrue(view.contains("已更新到 v"))
        XCTAssertTrue(view.contains("以来的更新"))
        XCTAssertTrue(view.contains("UpdateReleaseSummaryBuilder.items"))
        XCTAssertTrue(view.contains("查看完整更新内容"))
        XCTAssertTrue(view.contains("知道了"))
        XCTAssertTrue(view.contains("UpdateCompletionNoticeWindowController"))
    }

    func test更新完成后使用自定义弹窗并传递安装前版本() throws {
        let environment = try TestSourceReader.read([
            "RoutinUsage", "App", "AppEnvironment.swift"
        ])
        let service = try TestSourceReader.read([
            "RoutinUsage", "Updates", "GitHubUpdateService.swift"
        ])
        let noticeStart = try XCTUnwrap(
            environment.range(of: "func presentUpdateCompletionNoticeIfNeeded()")
        )
        let noticeEnd = try XCTUnwrap(
            environment.range(
                of: "/// 检测到新版本时发系统通知",
                range: noticeStart.lowerBound..<environment.endIndex
            )
        )
        let noticeMethod = environment[noticeStart.lowerBound..<noticeEnd.lowerBound]

        XCTAssertTrue(environment.contains("previousVersion: RoutinUsageApp.currentVersion"))
        XCTAssertTrue(service.contains("UpdateCompletionNotice.record("))
        XCTAssertTrue(service.contains("notes: update.notes"))
        XCTAssertTrue(service.contains("releaseURL: update.releaseURL"))
        XCTAssertTrue(noticeMethod.contains("UpdateCompletionNoticeWindowController.shared.present"))
        XCTAssertFalse(noticeMethod.contains("NSAlert"))
    }
}
