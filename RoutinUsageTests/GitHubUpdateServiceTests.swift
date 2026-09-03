import Foundation
import XCTest
@testable import RoutinUsage

final class GitHubUpdateServiceTests: XCTestCase {
    func test发现较新Release并选择DMG资源() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(url: try XCTUnwrap(request.url), statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(#"{"tag_name":"v1.2.0","html_url":"https://github.com/dick86114/MyRoutin/releases/tag/v1.2.0","body":"修复问题","assets":[{"name":"MyToken.dmg","browser_download_url":"https://example.com/MyToken.dmg"}]}"#.utf8))
        }
        let logger = UpdateLogWriter()
        let service = GitHubUpdateService(
            session: stub.session,
            currentVersion: "1.1.9",
            logWriter: logger
        )

        let update = try await service.checkForUpdate()

        XCTAssertEqual(stub.registration.lastRequest?.url, GitHubUpdateService.releasesURL)
        XCTAssertEqual(update?.version, "1.2.0")
        XCTAssertEqual(update?.downloadURL.absoluteString, "https://example.com/MyToken.dmg")
        let events = await logger.events
        XCTAssertTrue(events.contains { $0 == "update_check_succeeded" })
    }

    func test相同或更旧Release不提示更新() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(url: try XCTUnwrap(request.url), statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(#"{"tag_name":"v1.2.0","html_url":"https://github.com/dick86114/MyRoutin/releases/tag/v1.2.0","assets":[{"name":"MyToken.dmg","browser_download_url":"https://example.com/MyToken.dmg"}]}"#.utf8))
        }
        let service = GitHubUpdateService(session: stub.session, currentVersion: "1.2.0")

        let update = try await service.checkForUpdate()
        XCTAssertNil(update)
    }

    func testGitHubAPI限流时回退到AtomFeed() async throws {
        let atom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <id>tag:github.com,2008:Repository/1/v1.3.0</id>
            <link rel="alternate" href="https://github.com/dick86114/MyRoutin/releases/tag/v1.3.0" />
            <title>MyToken v1.3.0</title>
            <content type="html">修复 API 限流检查</content>
          </entry>
        </feed>
        """
        let stub = URLProtocolStub.makeSession { request in
            let url = try XCTUnwrap(request.url)
            if url == GitHubUpdateService.releasesURL {
                let response = try XCTUnwrap(
                    HTTPURLResponse(url: url, statusCode: 403, httpVersion: nil, headerFields: nil)
                )
                return (response, Data(#"{"message":"API rate limit exceeded"}"#.utf8))
            }
            XCTAssertEqual(url, GitHubUpdateService.releasesAtomURL)
            let response = try XCTUnwrap(
                HTTPURLResponse(url: url, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(atom.utf8))
        }
        let service = GitHubUpdateService(session: stub.session, currentVersion: "1.2.0")

        let update = try await service.checkForUpdate()

        XCTAssertEqual(update?.version, "1.3.0")
        XCTAssertEqual(
            update?.downloadURL.absoluteString,
            "https://github.com/dick86114/MyRoutin/releases/download/v1.3.0/MyToken.dmg"
        )
    }

    func test下载更新逐步报告百分比并保存完整文件() async throws {
        let payload = Data(repeating: 7, count: 100)
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(
                HTTPURLResponse(
                    url: try XCTUnwrap(request.url),
                    statusCode: 200,
                    httpVersion: nil,
                    headerFields: ["Content-Length": "100"]
                )
            )
            return (response, payload)
        }
        let service = GitHubUpdateService(session: stub.session, currentVersion: "1.0.0")
        let update = AppUpdate(
            version: "1.1.0",
            releaseURL: URL(string: "https://example.com/release")!,
            downloadURL: URL(string: "https://example.com/MyToken.dmg")!,
            notes: ""
        )
        let progress = DownloadProgressCapture()

        let url = try await service.download(update) { value in
            await progress.append(value)
        }
        defer { try? FileManager.default.removeItem(at: url) }

        XCTAssertEqual(try Data(contentsOf: url), payload)
        let values = await progress.values
        XCTAssertTrue(values.contains { ($0 ?? 0) > 0 })
        let finalProgress = try XCTUnwrap(values.last ?? nil)
        XCTAssertEqual(finalProgress, 1, accuracy: 0.001)
    }

    func test更新完成标记只会被新进程消费一次() throws {
        let suiteName = "GitHubUpdateServiceTests.notice-(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        UpdateCompletionNotice.record(version: "1.3.0", defaults: defaults)

        XCTAssertEqual(UpdateCompletionNotice.consume(defaults: defaults), "1.3.0")
        XCTAssertNil(UpdateCompletionNotice.consume(defaults: defaults))
    }
}

private actor DownloadProgressCapture {
    private(set) var values: [Double?] = []

    func append(_ value: Double?) {
        values.append(value)
    }
}

private actor UpdateLogWriter: AppLogWriting {
    private(set) var events: [String] = []

    func log(level: AppLogLevel, event: String, details: String?) async {
        events.append(event)
    }

    func recentText(maxCharacters: Int) async -> String {
        ""
    }
}
