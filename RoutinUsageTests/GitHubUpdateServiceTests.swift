import Foundation
import XCTest
@testable import RoutinUsage

final class GitHubUpdateServiceTests: XCTestCase {
    func test发现较新Release并选择DMG资源() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(url: try XCTUnwrap(request.url), statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(#"{"tag_name":"v1.2.0","html_url":"https://github.com/dick86114/MyRoutin/releases/tag/v1.2.0","body":"修复问题","assets":[{"name":"MyRoutin.dmg","browser_download_url":"https://example.com/MyRoutin.dmg"}]}"#.utf8))
        }
        let service = GitHubUpdateService(session: stub.session, currentVersion: "1.1.9")

        let update = try await service.checkForUpdate()

        XCTAssertEqual(stub.registration.lastRequest?.url, GitHubUpdateService.releasesURL)
        XCTAssertEqual(update?.version, "1.2.0")
        XCTAssertEqual(update?.downloadURL.absoluteString, "https://example.com/MyRoutin.dmg")
    }

    func test相同或更旧Release不提示更新() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(url: try XCTUnwrap(request.url), statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(#"{"tag_name":"v1.2.0","html_url":"https://github.com/dick86114/MyRoutin/releases/tag/v1.2.0","assets":[{"name":"MyRoutin.dmg","browser_download_url":"https://example.com/MyRoutin.dmg"}]}"#.utf8))
        }
        let service = GitHubUpdateService(session: stub.session, currentVersion: "1.2.0")

        let update = try await service.checkForUpdate()
        XCTAssertNil(update)
    }
}
