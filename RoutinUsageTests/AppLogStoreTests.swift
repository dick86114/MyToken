import Foundation
import XCTest
@testable import RoutinUsage

final class AppLogStoreTests: XCTestCase {
    func test日志写入时脱敏Key和敏感参数() async throws {
        let fileURL = temporaryLogURL()
        let store = AppLogStore(fileURL: fileURL, maxBytes: 4096)

        await store.log(
            level: .error,
            event: "update_failed",
            details: "secret=plan-abc123 token=super-secret"
        )

        let text = await store.recentText(maxCharacters: 2000)
        XCTAssertTrue(text.contains("update_failed"))
        XCTAssertTrue(text.contains("<redacted>"))
        XCTAssertFalse(text.contains("plan-abc123"))
        XCTAssertFalse(text.contains("super-secret"))
    }

    func test日志超过大小限制时只保留末尾内容() async throws {
        let fileURL = temporaryLogURL()
        let store = AppLogStore(fileURL: fileURL, maxBytes: 180)

        await store.log(level: .info, event: "first", details: String(repeating: "a", count: 300))
        await store.log(level: .info, event: "last", details: "保留末尾")

        let data = try Data(contentsOf: fileURL)
        XCTAssertLessThanOrEqual(data.count, 180)
        let text = await store.recentText(maxCharacters: 2000)
        XCTAssertTrue(text.contains("last"))
    }

    private func temporaryLogURL() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("myroutin-log-\(UUID().uuidString)")
            .appendingPathExtension("log")
    }
}
