import Foundation
import XCTest
@testable import RoutinUsage

final class UsageAPIClientTests: XCTestCase {
    func test请求固定端点并映射订阅用量() async throws {
        let client = makeClient(statusCode: 200, body: Self.periodicUsageJSON)
        let fixedDate = Date(timeIntervalSince1970: 1_786_370_400)

        let result = try await client.fetchUsage(apiKey: "plan-test", now: fixedDate)

        XCTAssertEqual(client.lastRequest?.url?.absoluteString, "https://api.routin.ai/plan/v1/usage")
        XCTAssertEqual(client.lastRequest?.httpMethod, "GET")
        XCTAssertEqual(client.lastRequest?.value(forHTTPHeaderField: "Authorization"), "Bearer plan-test")
        XCTAssertEqual(client.lastRequest?.value(forHTTPHeaderField: "Accept"), "application/json")
        XCTAssertEqual(client.lastRequest?.timeoutInterval, 15)
        XCTAssertEqual(result?.planName, "Pro")
        XCTAssertEqual(result?.fetchedAt, fixedDate)
    }

    func test响应为JSONNull时返回无订阅() async throws {
        let client = makeClient(statusCode: 200, body: "null")

        let result = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)

        XCTAssertNil(result)
    }

    func test带空白的JSONNull不是精确空响应() async {
        let client = makeClient(statusCode: 200, body: " null")

        await XCTAssert抛出API错误(.invalidResponse) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test无效密钥响应映射为InvalidKey() async {
        let client = makeClient(statusCode: 401, body: #"{"error":"invalid_api_key"}"#)

        await XCTAssert抛出API错误(.invalidKey) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test其它HTTP错误保留状态码() async {
        let client = makeClient(statusCode: 500, body: #"{"error":"internal_error"}"#)

        await XCTAssert抛出API错误(.server(statusCode: 500)) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test服务器错误正文为Null时仍先映射状态码() async {
        let client = makeClient(statusCode: 500, body: "null")

        await XCTAssert抛出API错误(.server(statusCode: 500)) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test其它401错误保留状态码() async {
        let client = makeClient(statusCode: 401, body: #"{"error":"expired_subscription"}"#)

        await XCTAssert抛出API错误(.server(statusCode: 401)) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test损坏JSON映射为InvalidResponse() async {
        let client = makeClient(statusCode: 200, body: "{损坏")

        await XCTAssert抛出API错误(.invalidResponse) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test传输失败映射为通用Transport错误() async {
        let client = makeClient { _ in
            throw URLError(.notConnectedToInternet)
        }

        await XCTAssert抛出API错误(.transport) {
            _ = try await client.fetchUsage(apiKey: "plan-test", now: .distantPast)
        }
    }

    func test并发客户端使用各自独立响应注册() async throws {
        let firstClient = makeClient(
            statusCode: 200,
            body: Self.periodicUsageJSON.replacingOccurrences(of: "\"Pro\"", with: "\"First\"")
        )
        let secondClient = makeClient(
            statusCode: 200,
            body: Self.periodicUsageJSON.replacingOccurrences(of: "\"Pro\"", with: "\"Second\"")
        )

        async let firstResult = firstClient.fetchUsage(apiKey: "plan-first", now: .distantPast)
        async let secondResult = secondClient.fetchUsage(apiKey: "plan-second", now: .distantPast)
        let (first, second) = try await (firstResult, secondResult)

        XCTAssertEqual(first?.planName, "First")
        XCTAssertEqual(second?.planName, "Second")
        XCTAssertEqual(
            firstClient.lastRequest?.value(forHTTPHeaderField: "Authorization"),
            "Bearer plan-first"
        )
        XCTAssertEqual(
            secondClient.lastRequest?.value(forHTTPHeaderField: "Authorization"),
            "Bearer plan-second"
        )
    }

    private func makeClient(statusCode: Int, body: String) -> StubbedUsageClient {
        makeClient { request in
            let response = try XCTUnwrap(
                HTTPURLResponse(
                    url: try XCTUnwrap(request.url),
                    statusCode: statusCode,
                    httpVersion: nil,
                    headerFields: ["Content-Type": "application/json"]
                )
            )
            return (response, Data(body.utf8))
        }
    }

    private func makeClient(
        handler: @escaping URLProtocolStub.Handler
    ) -> StubbedUsageClient {
        let stub = URLProtocolStub.makeSession(handler: handler)
        return StubbedUsageClient(
            client: UsageAPIClient(session: stub.session, mapper: UsageMapper()),
            registration: stub.registration
        )
    }

    private static let periodicUsageJSON = #"""
    {
        "planName": "Pro",
        "type": 1,
        "dailyLimitUsd": 10,
        "weeklyLimitUsd": 50,
        "dailyUsedUsd": 6.8,
        "weeklyUsedUsd": 21,
        "dailyRemainingUsd": 3.2,
        "weeklyRemainingUsd": 29,
        "dayWindowEndAt": "2026-08-10T14:00:00Z",
        "weekWindowEndAt": "2026-08-15T00:00:00Z",
        "totalTokens": null,
        "consumedTokens": null,
        "remainingTokens": null,
        "allowedModels": ["gpt-4.1"]
    }
    """#
}

private struct StubbedUsageClient: Sendable {
    let client: UsageAPIClient
    let registration: URLProtocolStub.Registration

    var lastRequest: URLRequest? {
        registration.lastRequest
    }

    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot? {
        try await client.fetchUsage(apiKey: apiKey, now: now)
    }
}

private func XCTAssert抛出API错误(
    _ expected: UsageAPIError,
    operation: () async throws -> Void,
    file: StaticString = #filePath,
    line: UInt = #line
) async {
    do {
        try await operation()
        XCTFail("预期抛出 API 错误", file: file, line: line)
    } catch {
        XCTAssertEqual(error as? UsageAPIError, expected, file: file, line: line)
    }
}
