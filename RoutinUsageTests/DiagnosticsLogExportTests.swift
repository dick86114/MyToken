import Foundation
import XCTest
@testable import RoutinUsage

final class DiagnosticsLogExportTests: XCTestCase {
    func test诊断文件名包含应用名和时间戳() throws {
        let date = Date(timeIntervalSince1970: 1_790_000_000)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]

        XCTAssertEqual(
            DiagnosticsLogExporter.fileName(for: date),
            "MyToken-diagnostics-\(Self.expectedStamp(for: date)).txt"
        )
    }

    func test导出入口存在于设置页和状态栏菜单() throws {
        let helpView = try readSourceText(at: "RoutinUsage/Views/Settings/HelpUpdateView.swift")
        let statusBarController = try readSourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let appEnvironment = try readSourceText(at: "RoutinUsage/App/AppEnvironment.swift")

        XCTAssertTrue(helpView.contains("exportDiagnosticsLog"))
        XCTAssertTrue(helpView.contains("导出诊断日志"))
        XCTAssertTrue(statusBarController.contains("导出诊断日志"))
        XCTAssertTrue(statusBarController.contains("exportDiagnosticsLog"))
        XCTAssertTrue(appEnvironment.contains("NSSavePanel"))
        XCTAssertTrue(appEnvironment.contains("activateFileViewerSelecting"))
    }

    private static func expectedStamp(for date: Date) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Shanghai")!
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        return String(
            format: "%04d%02d%02d-%02d%02d%02d",
            components.year!,
            components.month!,
            components.day!,
            components.hour!,
            components.minute!,
            components.second!
        )
    }
}
