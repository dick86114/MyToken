import Foundation
import XCTest
@testable import RoutinUsage

final class IssueReporterTests: XCTestCase {
    func testIssueURL包含环境信息更新状态和脱敏日志() throws {
        let context = IssueReportContext(
            version: "2.1.0",
            operatingSystem: "macOS 15.6",
            architecture: "arm64",
            updateStatus: "检查更新失败",
            logs: "[error] update_failed: API 限流"
        )

        let url = try XCTUnwrap(IssueReporter.makeIssueURL(context: context))
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let query = try XCTUnwrap(components.queryItems)
        let title = try XCTUnwrap(query.first(where: { $0.name == "title" })?.value)
        let body = try XCTUnwrap(query.first(where: { $0.name == "body" })?.value)

        XCTAssertEqual(components.path, "/dick86114/MyRoutin/issues/new")
        XCTAssertTrue(title.contains("MyRoutin"))
        XCTAssertTrue(body.contains("2.1.0"))
        XCTAssertTrue(body.contains("macOS 15.6"))
        XCTAssertTrue(body.contains("arm64"))
        XCTAssertTrue(body.contains("检查更新失败"))
        XCTAssertTrue(body.contains("API 限流"))
    }

    func testIssue日志正文限制长度() throws {
        let context = IssueReportContext(
            version: "2.1.0",
            operatingSystem: "macOS 15.6",
            architecture: "arm64",
            updateStatus: "检查更新失败",
            logs: String(repeating: "x", count: 20_000)
        )

        let url = try XCTUnwrap(IssueReporter.makeIssueURL(context: context))
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let body = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "body" })?.value)

        XCTAssertLessThanOrEqual(body.count, IssueReporter.maxBodyCharacters)
    }
}
