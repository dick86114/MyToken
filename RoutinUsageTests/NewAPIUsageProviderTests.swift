import XCTest
@testable import RoutinUsage

final class NewAPIUsageProviderTests: XCTestCase {
    func test账户额度和消费统计转换为统一指标() async throws {
        let now = Date(timeIntervalSince1970: 1_788_048_000)
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let body: String
            switch path {
            case "/api/status":
                body = #"{"success":true,"data":{"quota_per_unit":500000,"quota_display_type":"CNY","usd_exchange_rate":6.8}}"#
            case "/api/user/self":
                body = #"{"success":true,"data":{"username":"alice","group":"default","quota":5000000,"used_quota":1250000,"request_count":42}}"#
            case "/api/log/self/stat":
                let components = try XCTUnwrap(URLComponents(url: try XCTUnwrap(request.url), resolvingAgainstBaseURL: false))
                let start = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "start_timestamp" })?.value).flatMap(Int64.init) ?? 0
                let end = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "end_timestamp" })?.value).flatMap(Int64.init) ?? 0
                body = #"{"success":true,"data":{"quota":120000,"rpm":3,"tpm":4500}}"#
            case "/api/data/self":
                let components = try XCTUnwrap(URLComponents(url: try XCTUnwrap(request.url), resolvingAgainstBaseURL: false))
                let start = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "start_timestamp" })?.value).flatMap(Int64.init) ?? 0
                let end = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "end_timestamp" })?.value).flatMap(Int64.init) ?? 0
                let tokenUsed: Int
                switch end - start {
                case ..<86_400:
                    tokenUsed = 12_000
                case 86_400:
                    tokenUsed = 34_000
                case 604_800:
                    tokenUsed = 56_000
                default:
                    tokenUsed = 78_000
                }
                let quota: Int
                switch end - start {
                case ..<86_400:
                    quota = 120_000
                case 86_400:
                    quota = 240_000
                case 604_800:
                    quota = 600_000
                default:
                    quota = 1_800_000
                }
                body = #"{"success":true,"data":[{"token_used":\#(tokenUsed),"count":10,"quota":\#(quota)}]}"#
            default:
                XCTFail("意外请求：\(path)")
                body = "{}"
            }
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(body.utf8))
        }
        let provider = NewAPIUsageProvider(session: stub.session)
        let credential = ProviderCredential(
            providerID: .newAPI,
            kind: .bearerAPIKey,
            secret: "personal-access-token",
            metadata: [
                "baseURL": "https://newapi.test/api/",
                "userID": "42",
                "balanceWarningThreshold": "7000000"
            ]
        )

        let fetched = try await provider.fetchUsage(credential, now: now)
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.providerID, .newAPI)
        XCTAssertEqual(snapshot.planName, "New API · default")
        XCTAssertEqual(snapshot.metrics.map(\.id), [
            "quota-progress", "today-token", "one-day-token",
            "seven-day-token", "thirty-day-token", "today-token-cost",
            "one-day-token-cost", "seven-day-token-cost",
            "thirty-day-token-cost", "rpm", "tpm", "request-count"
        ])
        XCTAssertEqual(snapshot.metrics[0].currencyCode, "¥")
        XCTAssertEqual(snapshot.metrics[0].used, 17)
        XCTAssertEqual(snapshot.metrics[0].limit, 85)
        XCTAssertEqual(snapshot.metrics[0].remaining, 68)
        XCTAssertEqual(snapshot.metrics[0].healthState, .warning)
        XCTAssertEqual(snapshot.metrics[1].label, "今日 Token")
        XCTAssertEqual(snapshot.metrics[1].unit, .token)
        XCTAssertEqual(snapshot.metrics[1].value, 12_000)
        XCTAssertEqual(snapshot.metrics[2].value, 34_000)
        XCTAssertEqual(snapshot.metrics[3].value, 56_000)
        XCTAssertEqual(snapshot.metrics[4].value, 78_000)
        XCTAssertEqual(snapshot.metrics[5].value, Decimal(string: "1.632"))
        XCTAssertEqual(snapshot.metrics[6].value, Decimal(string: "3.264"))
        XCTAssertEqual(snapshot.metrics[7].value, Decimal(string: "8.16"))
        XCTAssertEqual(snapshot.metrics[8].value, Decimal(string: "24.48"))
        XCTAssertEqual(snapshot.metrics[9].value, 3)
        XCTAssertEqual(snapshot.metrics[10].value, 4_500)
        XCTAssertEqual(snapshot.metrics[11].label, "账户累计请求")
        XCTAssertEqual(snapshot.metrics[11].value, 42)
        XCTAssertEqual(stub.registration.lastRequest?.value(forHTTPHeaderField: "Authorization"), "Bearer personal-access-token")
        XCTAssertEqual(stub.registration.lastRequest?.value(forHTTPHeaderField: "New-Api-User"), "42")
    }

    func test认证失败映射为统一错误() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data())
        }
        let provider = NewAPIUsageProvider(session: stub.session)
        let credential = ProviderCredential(
            providerID: .newAPI,
            kind: .bearerAPIKey,
            secret: "personal-access-token",
            metadata: ["baseURL": "https://newapi.test", "userID": "42"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期认证失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .unauthorized)
        }
    }

    func testHTTP200但success为false时展示供应商返回的认证错误() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(#"{"success":false,"message":"Unauthorized, invalid access token"}"#.utf8))
        }
        let provider = NewAPIUsageProvider(session: stub.session)
        let credential = ProviderCredential(
            providerID: .newAPI,
            kind: .bearerAPIKey,
            secret: "model-api-key",
            metadata: ["baseURL": "https://newapi.test", "userID": "42"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期认证失败")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("New API：Unauthorized, invalid access token")
            )
        }
    }

    func test凭证配置保留规范化实例地址与低额度预警() throws {
        let result = try CredentialEditorValidation.validate(
            providerID: .newAPI,
            name: "我的 New API",
            apiKey: "personal-access-token",
            accessKeyID: "",
            secretAccessKey: "",
            region: "",
            balanceWarningThreshold: "100000",
            newAPIBaseURL: "https://newapi.example.com/api/",
            newAPIUserID: "7"
        )

        XCTAssertEqual(result.credentialKind, .bearerAPIKey)
        XCTAssertEqual(result.metadata["baseURL"], "https://newapi.example.com")
        XCTAssertEqual(result.metadata["userID"], "7")
        XCTAssertEqual(result.metadata["balanceWarningThreshold"], "100000")
    }
}
