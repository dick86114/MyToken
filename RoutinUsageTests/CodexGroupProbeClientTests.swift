import Foundation
import XCTest
@testable import RoutinUsage

final class CodexGroupProbeClientTests: XCTestCase {
    func test探测请求使用最小Responses请求和唯一标识() async throws {
        let client = makeClient(statusCode: 200, body: #"{"id":"resp_123"}"#)
        let marker = CodexGroupProbeRequestMarker(id: UUID())

        try await client.probe(apiKey: "plan-probe-1234", marker: marker)

        let request = try XCTUnwrap(client.lastRequest)
        XCTAssertEqual(request.url?.absoluteString, "https://api.routin.ai/plan/v1/responses")
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer plan-probe-1234")
        XCTAssertEqual(request.value(forHTTPHeaderField: "User-Agent"), marker.userAgent)
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
        XCTAssertEqual(request.timeoutInterval, 15)

        let body = try XCTUnwrap(request.httpBody)
        let payload = try XCTUnwrap(try JSONSerialization.jsonObject(with: body) as? [String: Any])
        XCTAssertEqual(payload["model"] as? String, "gpt-5.6-luna")
        XCTAssertEqual(payload["input"] as? String, "返回 OK")
        XCTAssertEqual(payload["max_output_tokens"] as? Int, 16)
        XCTAssertNil(payload["tools"])
    }

    func test无效Key映射为无效Key错误() async {
        let client = makeClient(statusCode: 401, body: #"{"error":"invalid_api_key"}"#)

        await XCTAssert抛出Codex探测错误(.invalidKey) {
            try await client.probe(
                apiKey: "plan-invalid-1234",
                marker: CodexGroupProbeRequestMarker(id: UUID())
            )
        }
    }

    func test模型不存在映射为模型不可用且不自行重试() async {
        let client = makeClient(statusCode: 404, body: #"{"error":"model_not_found"}"#)

        await XCTAssert抛出Codex探测错误(.modelUnavailable) {
            try await client.probe(
                apiKey: "plan-probe-1234",
                marker: CodexGroupProbeRequestMarker(id: UUID())
            )
        }
        XCTAssertEqual(client.requestCount, 1)
    }

    private func makeClient(statusCode: Int, body: String) -> StubbedCodexGroupProbeClient {
        let stub = URLProtocolStub.makeSession { request in
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
        return StubbedCodexGroupProbeClient(
            client: CodexGroupProbeClient(session: stub.session),
            registration: stub.registration
        )
    }
}

private final class StubbedCodexGroupProbeClient: CodexGroupProbing, @unchecked Sendable {
    let client: CodexGroupProbeClient
    let registration: URLProtocolStub.Registration

    init(client: CodexGroupProbeClient, registration: URLProtocolStub.Registration) {
        self.client = client
        self.registration = registration
    }

    var lastRequest: URLRequest? {
        registration.lastRequest
    }

    var requestCount: Int {
        registration.requestCount
    }

    func probe(apiKey: String, marker: CodexGroupProbeRequestMarker) async throws {
        try await client.probe(apiKey: apiKey, marker: marker)
    }
}

func XCTAssert抛出Codex探测错误(
    _ expected: CodexGroupProbeError,
    operation: @escaping () async throws -> Void,
    file: StaticString = #filePath,
    line: UInt = #line
) async {
    do {
        try await operation()
        XCTFail("预期抛出 Codex 探测错误", file: file, line: line)
    } catch let actual as CodexGroupProbeError {
        XCTAssertEqual(actual, expected, file: file, line: line)
    } catch {
        XCTFail("错误类型不符合预期：\(error)", file: file, line: line)
    }
}
