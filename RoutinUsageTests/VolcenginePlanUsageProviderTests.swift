import XCTest
@testable import RoutinUsage

final class VolcenginePlanUsageProviderTests: XCTestCase {
    func test个人AFP窗口转换为进度型指标() async throws {
        let stub = URLProtocolStub.makeSession { request in
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertTrue(request.url?.absoluteString.contains("Action=GetAgentPlanAFPUsage") == true)
            XCTAssertNotNil(request.value(forHTTPHeaderField: "Authorization"))
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            let body = #"{"Result":{"PlanType":"Max","AFPFiveHour":{"Quota":"10000","Used":"0","ResetTime":1893456000000},"AFPDaily":{"Quota":"50000","Used":"0"},"AFPWeekly":{"Quota":"35000","Used":"3252.2867","ResetTime":1893456000000},"AFPMonthly":{"Quota":"100000","Used":"41222.3834","ResetTime":1893456000000}}}"#
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
        XCTAssertEqual(snapshot.metrics.map(\.id), ["fiveHour", "weekly", "monthly"])
        XCTAssertEqual(snapshot.metrics.map(\.label), ["近 5 小时用量", "近一周用量", "近一月用量"])
        XCTAssertEqual(snapshot.metrics[1].used, Decimal(string: "3252.2867"))
        XCTAssertEqual(snapshot.metrics[1].limit, 35_000)
        XCTAssertEqual(snapshot.metrics[2].used, Decimal(string: "41222.3834"))
        XCTAssertEqual(snapshot.metrics[2].limit, 100_000)
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
            let body = #"{"Result":{"QuotaUsage":[{"Level":"5h","Percent":10,"ResetTimestamp":1893456000}]}}"#
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

    func test验证先确认个人套餐再查询用量() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let action = request.url?.absoluteString ?? ""
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            if action.contains("GetPersonalPlan") {
                return (response, Data(#"{"Result":{"PlanType":"Max","Status":"Running"}}"#.utf8))
            }
            return (response, Data(#"{"Result":{"PlanType":"Max","AFPFiveHour":{"Quota":"100","Used":"10"}}}"#.utf8))
        }
        let provider = VolcenginePlanUsageProvider(session: stub.session, endpoint: URL(string: "https://ark.test/")!)
        let credential = ProviderCredential(
            providerID: .volcengine,
            kind: .accessKeyPair,
            secret: "secret-key",
            metadata: ["accessKeyID": "access-key", "region": "cn-beijing", "planType": "agent"]
        )

        let fetched = try await provider.validate(credential, now: Date(timeIntervalSince1970: 1_700_000_000))

        XCTAssertEqual(fetched?.planName, "Max Plan")
    }
}
