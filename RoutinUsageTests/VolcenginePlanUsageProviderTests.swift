import XCTest
@testable import RoutinUsage

final class VolcenginePlanUsageProviderTests: XCTestCase {
    func test个人AFP窗口转换为进度型指标() async throws {
        let stub = URLProtocolStub.makeSession { request in
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertTrue(request.url?.absoluteString.contains("Action=GetAFPUsage") == true)
            XCTAssertNotNil(request.value(forHTTPHeaderField: "Authorization"))
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            let body = #"{"Result":{"PlanType":"Max","AFPFiveHour":{"Quota":"100","Used":"25","ResetTime":1893456000000,"SubscribeTime":1893452400000},"AFPDaily":{"Quota":"500","Used":"100","ResetTime":1893456000000,"SubscribeTime":1893369600000}}}"#
            return (response, Data(body.utf8))
        }
        let provider = VolcenginePlanUsageProvider(session: stub.session, endpoint: URL(string: "https://ark.test/")!)
        let credential = ProviderCredential(
            credentialID: UUID(),
            providerID: .volcengine,
            kind: .accessKeyPair,
            secret: "secret-key",
            metadata: ["accessKeyID": "access-key", "region": "cn-beijing"]
        )

        let fetched = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 1_700_000_000))
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.providerID, .volcengine)
        XCTAssertEqual(snapshot.planName, "Max Plan")
        XCTAssertEqual(snapshot.metrics.count, 2)
        XCTAssertEqual(snapshot.metrics.first?.used, 25)
        XCTAssertEqual(snapshot.metrics.first?.limit, 100)
        XCTAssertEqual(snapshot.metrics.first?.presentation, .progress)
    }

    func test缺少AK或区域时拒绝凭证() async {
        let provider = VolcenginePlanUsageProvider(endpoint: URL(string: "https://ark.test/")!)
        let credential = ProviderCredential(
            providerID: .volcengine,
            kind: .accessKeyPair,
            secret: "secret-key",
            metadata: [:]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期凭证校验失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .invalidCredential)
        }
    }

    func testCodingPlan凭证使用CodingPlanAction() async throws {
        let stub = URLProtocolStub.makeSession { request in
            XCTAssertTrue(request.url?.absoluteString.contains("Action=GetCodingPlanUsage") == true)
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            let body = #"{"Result":{"PlanType":"Coding","AFPFiveHour":{"Quota":"100","Used":"10"}}}"#
            return (response, Data(body.utf8))
        }
        let provider = VolcenginePlanUsageProvider(session: stub.session, endpoint: URL(string: "https://ark.test/")!)
        let credential = ProviderCredential(
            providerID: .volcengine,
            kind: .accessKeyPair,
            secret: "secret-key",
            metadata: ["accessKeyID": "access-key", "region": "cn-beijing", "planType": "coding"]
        )

        let fetched = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 1_700_000_000))
        XCTAssertEqual(fetched?.planName, "Coding Plan")
    }
}
