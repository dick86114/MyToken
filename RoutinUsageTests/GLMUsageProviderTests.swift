import XCTest
@testable import RoutinUsage

final class GLMUsageProviderTests: XCTestCase {
    func test配额模型和工具响应转换为通用指标() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let body: String
            if path.contains("quota/limit") {
                body = #"{"data":{"limits":[{"type":"TOKENS_LIMIT","percentage":42},{"type":"TIME_LIMIT","percentage":12}]}}"#
            } else if path.contains("model-usage") {
                body = #"{"data":{"items":[{"modelName":"glm-4.5","tokens":1234}]}}"#
            } else {
                body = #"{"data":{"items":[{"toolName":"search","count":9}]}}"#
            }
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(body.utf8))
        }
        let provider = GLMUsageProvider(session: stub.session, baseURL: URL(string: "https://glm.test")!)
        let credential = ProviderCredential(providerID: .glm, kind: .apiKey, secret: "glm-token")

        let fetched = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 100))
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.providerID, .glm)
        XCTAssertEqual(snapshot.metrics.count, 4)
        XCTAssertEqual(snapshot.metrics.first?.presentation, .progress)
        XCTAssertEqual(snapshot.metrics.first?.used, 42)
        XCTAssertTrue(snapshot.metrics.contains(where: { $0.label.contains("glm-4.5") }))
        XCTAssertTrue(snapshot.metrics.contains(where: { $0.label.contains("search") }))
    }

    func test单个接口认证失败映射为统一错误() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data())
        }
        let provider = GLMUsageProvider(session: stub.session, baseURL: URL(string: "https://glm.test")!)
        let credential = ProviderCredential(providerID: .glm, kind: .apiKey, secret: "glm-token")

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期认证失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .unauthorized)
        }
    }

    func test配额百分比按官网语义显示剩余值() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let body = path.contains("quota/limit")
                ? #"{"data":{"limits":[{"type":"TIME_LIMIT","percentage":26},{"type":"TOKENS_LIMIT","percentage":1,"unit":"week","number":7}]}}"#
                : #"{"data":{"items":[]}}"#
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(body.utf8))
        }
        let provider = GLMUsageProvider(session: stub.session, baseURL: URL(string: "https://glm.test")!)
        let credential = ProviderCredential(providerID: .glm, kind: .apiKey, secret: "glm-token")

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)
        let mcp = try XCTUnwrap(snapshot.metrics.first(where: { $0.label.contains("MCP") }))
        let weekly = try XCTUnwrap(snapshot.metrics.first(where: { $0.label.contains("每周") }))

        XCTAssertEqual(mcp.used, 26)
        XCTAssertEqual(mcp.remaining, 74)
        XCTAssertEqual(try XCTUnwrap(mcp.displayedPercent), 74, accuracy: 0.001)
        XCTAssertTrue(mcp.label.contains("剩余额度"))
        XCTAssertTrue(weekly.label.contains("每周"))
    }
}
