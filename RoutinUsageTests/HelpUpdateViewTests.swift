import XCTest

final class HelpUpdateViewTests: XCTestCase {
    func test帮助页提供版本更新进度发布说明和反馈() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(source.contains("当前版本"))
        XCTAssertTrue(source.contains("当前版本更新日志"))
        XCTAssertTrue(source.contains("检查更新"))
        XCTAssertTrue(source.contains("检测更新"))
        XCTAssertTrue(source.contains("查看历史版本"))
        XCTAssertTrue(source.contains("Label(\"GitHub\""))
        XCTAssertFalse(source.contains("查看该版本发布页"))
        XCTAssertTrue(source.contains("ReleaseHistorySheet"))
        XCTAssertTrue(source.contains("应用更新"))
        XCTAssertFalse(source.contains("updateChannelSection"))
        XCTAssertFalse(source.contains("updateStatusSection"))
        XCTAssertTrue(source.contains("ProgressView"))
        XCTAssertTrue(source.contains("UpdateNotesView"))
        XCTAssertTrue(source.contains("提交问题"))
        XCTAssertTrue(source.contains("installAvailableUpdate"))
    }

    func test帮助页版本区块附近提供可关闭的GitHubStar引导() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(source.contains("starPromptSection"))
        XCTAssertTrue(source.contains("StarPromptBanner"))
        XCTAssertTrue(source.contains("如果 MyToken 帮到了你，请在 GitHub 点个 Star，这对我们意义重大"))
        XCTAssertTrue(source.contains("去点 Star"))
        XCTAssertTrue(source.contains("RoutinUsageApp.githubURL"))
        XCTAssertTrue(source.contains("starPromptDismissed = true"))
        XCTAssertTrue(source.contains("关闭 Star 引导"))
    }

    func test真实弹窗移除签到状态和分组检测入口() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])

        XCTAssertFalse(source.contains("Routin 签到"))
        XCTAssertFalse(source.contains("startRoutinCheckIn"))
        XCTAssertFalse(source.contains("startCodexGroupDetection"))
    }
}
