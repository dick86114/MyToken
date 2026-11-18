import XCTest

final class HelpUpdateViewTests: XCTestCase {
    func test帮助页提供版本更新进度发布说明和反馈() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(source.contains("当前版本"))
        XCTAssertTrue(source.contains("更新内容"))
        XCTAssertTrue(source.contains("检查更新"))
        XCTAssertTrue(source.contains("检查更新"))
        XCTAssertTrue(source.contains("查看历史版本"))
        XCTAssertTrue(source.contains("Image(systemName: \"star.fill\")"))
        XCTAssertTrue(source.contains("advancedSection"))
        XCTAssertTrue(source.contains("showsAdvancedSettings"))
        XCTAssertFalse(source.contains("查看该版本发布页"))
        XCTAssertTrue(source.contains("ReleaseHistorySheet"))
        XCTAssertTrue(source.contains("应用更新"))
        XCTAssertFalse(source.contains("updateChannelSection"))
        XCTAssertFalse(source.contains("updateStatusSection"))
        XCTAssertTrue(source.contains("ProgressView"))
        XCTAssertTrue(source.contains("UpdateNotesView"))
        XCTAssertTrue(source.contains("CurrentReleaseNotesView"))
        XCTAssertTrue(source.contains("UpdateReleaseSummaryBuilder.items"))
        XCTAssertTrue(source.contains("UpdateReleaseSummaryBuilder.supportsSummaryLayout"))
        XCTAssertTrue(source.contains("HStack(alignment: .center, spacing: 9)"))
        XCTAssertTrue(source.contains("提交问题"))
        XCTAssertTrue(source.contains("installAvailableUpdate"))
    }

    func test当前版本号后提供轻量Star入口且不再显示独立引导条() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "HelpUpdateView.swift"
        ])

        XCTAssertTrue(source.contains("Image(systemName: \"star.fill\")"))
        XCTAssertTrue(source.contains("RoutinUsageApp.githubURL"))
        XCTAssertTrue(source.contains("isStarHovered"))
        XCTAssertTrue(source.contains(".onHover"))
        XCTAssertTrue(source.contains("rotationEffect"))
        XCTAssertTrue(source.contains("dampingFraction: 0.72"))
        XCTAssertFalse(source.contains("starPromptSection"))
        XCTAssertFalse(source.contains("StarPromptBanner"))
        XCTAssertFalse(source.contains("去点 Star"))
        XCTAssertFalse(source.contains("在 GitHub 查看当前版本"))
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
