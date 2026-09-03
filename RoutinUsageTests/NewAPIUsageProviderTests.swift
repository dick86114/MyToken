import XCTest
@testable import RoutinUsage

final class NewAPIUsageProviderTests: XCTestCase {
    func test账户额度和消费统计转换为统一指标() async throws {
        let now = Date(timeIntervalSince1970: 1_788_048_000)
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let body: String
            switch path {
            case "/api/user/self":
                body = #"{"success":true,"data":{"username":"alice","group":"default","quota":5000000,"used_quota":1250000,"request_count":42}}"#
            case "/api/log/self/stat":
                let components = try XCTUnwrap(URLComponents(url: try XCTUnwrap(request.url), resolvingAgainstBaseURL: false))
                let start = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "start_timestamp" })?.value).flatMap(Int64.init) ?? 0
                let end = try XCTUnwrap(components.queryItems?.first(where: { $0.name == "end_timestamp" })?.value).flatMap(Int64.init) ?? 0
                switch end - start {
                case 0...172_800:
                    body = #"{"success":true,"data":{"quota":120000,"rpm":3,"tpm":4500}}"#
                case 172_801...691_200:
                    body = #"{"success":true,"data":{"quota":600000,"rpm":3,"tpm":4500}}"#
                default:
                    body = #"{"success":true,"data":{"quota":1800000,"rpm":3,"tpm":4500}}"#
                }
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
                "balanceWarningThreshold": "4000000"
            ]
        )

        let fetched = try await provider.fetchUsage(credential, now: now)
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.providerID, .newAPI)
        XCTAssertEqual(snapshot.planName, "New API · default")
        XCTAssertEqual(snapshot.metrics.map(\.id), [
            "remaining-quota", "total-quota", "used-quota", "today-quota",
            "seven-day-quota", "thirty-day-quota", "rpm", "tpm", "request-count"
        ])
        XCTAssertEqual(snapshot.metrics[0].value, 3_750_000)
        XCTAssertEqual(snapshot.metrics[0].healthState, .warning)
        XCTAssertEqual(snapshot.metrics[3].value, 120_000)
        XCTAssertEqual(snapshot.metrics[4].value, 600_000)
        XCTAssertEqual(snapshot.metrics[5].value, 1_800_000)
        XCTAssertEqual(snapshot.metrics[6].value, 3)
        XCTAssertEqual(snapshot.metrics[7].value, 4_500)
        XCTAssertEqual(snapshot.metrics[8].value, 42)
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
