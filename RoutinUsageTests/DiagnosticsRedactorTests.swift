import Foundation
import XCTest
@testable import RoutinUsage

final class DiagnosticsRedactorTests: XCTestCase {
    func test诊断报告二次脱敏常见凭证和用户路径() {
        let redacted = DiagnosticsRedactor.redact(
            """
            Cookie: api-platform_serviceToken=cookie-secret
            Authorization: Bearer bearer-secret
            api_key=api-secret
            password=password-secret
            https://api.example.com/v1/data?token=query-secret
            plan-plan-secret
            /Users/someone/Library/Logs/MyToken/app.log
            """
        )

        XCTAssertFalse(redacted.contains("cookie-secret"))
        XCTAssertFalse(redacted.contains("bearer-secret"))
        XCTAssertFalse(redacted.contains("api-secret"))
        XCTAssertFalse(redacted.contains("password-secret"))
        XCTAssertFalse(redacted.contains("query-secret"))
        XCTAssertFalse(redacted.contains("plan-plan-secret"))
        XCTAssertFalse(redacted.contains("/Users/someone"))
        XCTAssertEqual(redacted.components(separatedBy: "<redacted>").count - 1, 6)
    }

    func test导出文件会创建缺失目录并写入UTF8内容() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("diagnostics-export-\(UUID().uuidString)", isDirectory: true)
            .appendingPathComponent("nested", isDirectory: true)
        let destination = directory.appendingPathComponent("report.txt")

        try DiagnosticsLogExporter.write("中文诊断内容", to: destination)
        let text = try String(contentsOf: destination, encoding: .utf8)

        XCTAssertEqual(text, "中文诊断内容")
        try? FileManager.default.removeItem(at: directory)
    }
}
