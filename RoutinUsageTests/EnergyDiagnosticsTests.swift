import Foundation
import XCTest
@testable import RoutinUsage

final class EnergyDiagnosticsTests: XCTestCase {
    func test诊断环形缓冲只保留最新事件() async {
        let buffer = EnergyDiagnosticsBuffer(maximumEventCount: 3, now: { Date(timeIntervalSince1970: 100) })

        for index in 0..<5 {
            await buffer.record(name: "event_\(index)", details: ["index": "\(index)"])
        }

        let events = await buffer.currentEvents()
        XCTAssertEqual(events.map(\.name), ["event_2", "event_3", "event_4"])
        XCTAssertEqual(events.last?.details["index"], "4")
    }

    func test唤醒和弹窗生命周期进入能耗诊断() throws {
        let refreshScheduler = try readSourceText(at: "RoutinUsage/Scheduling/RefreshScheduler.swift")
        let statusBarController = try readSourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(refreshScheduler.contains("wake_refresh_triggered"))
        XCTAssertTrue(statusBarController.contains("popover_opened"))
        XCTAssertTrue(statusBarController.contains("popover_closed"))
    }

    func test诊断报告包含环境信息和事件并再次脱敏() async {
        let buffer = EnergyDiagnosticsBuffer(maximumEventCount: 10, now: { Date(timeIntervalSince1970: 100) })
        await buffer.record(name: "refresh_completed", details: ["duration": "1.20"])
        let metadata = DiagnosticsReportMetadata(
            version: "5.6.8",
            operatingSystem: "macOS 26.0",
            architecture: "arm64",
            exportedAt: Date(timeIntervalSince1970: 200),
            updateStatus: "空闲",
            refreshMinutes: 5,
            notificationsEnabled: true,
            widgetSyncEnabled: true,
            configuredCredentialCount: 2,
            enabledCredentialCount: 2,
            providerCounts: ["deepseek": 2]
        )

        let report = await DiagnosticsReportBuilder.makeReport(
            metadata: metadata,
            diagnostics: buffer,
            logs: "token=super-secret /Users/dickies/Library/Logs/MyToken/app.log"
        )

        let text = report
        XCTAssertTrue(text.contains("MyToken 诊断报告"))
        XCTAssertTrue(text.contains("版本：5.6.8"))
        XCTAssertTrue(text.contains("刷新间隔：5 分钟"))
        XCTAssertTrue(text.contains("deepseek: 2"))
        XCTAssertTrue(text.contains("refresh_completed"))
        XCTAssertTrue(text.contains("duration=1.20"))
        XCTAssertFalse(text.contains("super-secret"))
        XCTAssertFalse(text.contains("/Users/dickies"))
        XCTAssertTrue(text.contains("<redacted>"))
    }
}
