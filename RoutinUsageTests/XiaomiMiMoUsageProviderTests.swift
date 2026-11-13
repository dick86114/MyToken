import XCTest
import Foundation
@testable import RoutinUsage

final class XiaomiMiMoUsageProviderTests: XCTestCase {
    func test官方APIKey读取余额和近31天用量() async throws {
        let requests = RequestRecorder()
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            let body: String
            switch request.url?.path {
            case "/v1/platform/balance":
                body = """
                {"object":"balance","is_available":true,"balance_infos":[{"currency":"CNY","cash_balance":"10.00","gift_balance":"2.50","frozen_balance":"0.00","credit_limit":"0.00","credit_available":"0.00","total_available":"12.50"}]}
                """
            case "/v1/platform/usage/daily":
                body = """
                {"object":"list","data":[{"date":"2026-11-12","resource_type":"model","name":"mimo-v2.6","currency":"CNY","consumed_amount":"1.250000","total_tokens":1000,"input_cache_hit_tokens":200,"input_cache_miss_tokens":600,"output_tokens":200},{"date":"2026-11-11","resource_type":"model","name":"mimo-v2.6","currency":"CNY","consumed_amount":"0.750000","total_tokens":500,"input_cache_hit_tokens":100,"input_cache_miss_tokens":300,"output_tokens":100}]}
                """
            default:
                XCTFail("意外请求：\(request.url?.path ?? "")")
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
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            officialBaseURL: URL(string: "https://api.test/v1")!,
            tokenPlanBaseURL: URL(string: "https://token-plan.test/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "sk-official-key",
            metadata: ["usageKind": "api", "balanceWarningThreshold": "15"]
        )
        let now = try XCTUnwrap(ISO8601DateFormatter().date(from: "2026-11-13T03:00:00Z"))

        let fetched = try await provider.fetchUsage(credential, now: now)
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.planName, "API 按量")
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "account-balance" })?.value,
            Decimal(string: "12.5")
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "account-balance" })?.healthState,
            .warning
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "total-consumption" })?.value,
            Decimal(string: "2.0")
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "total-tokens" })?.value,
            1500
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "input-tokens" })?.value,
            900
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "cache-tokens" })?.value,
            300
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "output-tokens" })?.value,
            300
        )
        await requests.waitForCount(2)
        XCTAssertEqual(requests.count, 2)
        XCTAssertTrue(requests.allSatisfy {
            $0.value(forHTTPHeaderField: "Authorization") == "Bearer sk-official-key"
                && $0.value(forHTTPHeaderField: "Cookie") == nil
        })
        let usageURL = try XCTUnwrap(
            requests.all.first(where: { $0.url?.path == "/v1/platform/usage/daily" })?.url
        )
        let query = try XCTUnwrap(URLComponents(url: usageURL, resolvingAgainstBaseURL: false)?.queryItems)
        XCTAssertEqual(query.first(where: { $0.name == "start_date" })?.value, "2026-10-14")
        XCTAssertEqual(query.first(where: { $0.name == "end_date" })?.value, "2026-11-13")
    }

    func test官方TokenPlanKey读取套餐余量和过期时间() async throws {
        let requests = RequestRecorder()
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            XCTAssertEqual(request.url?.path, "/v1/platform/token_plan")
            let body = """
            {"object":"token_plan","plan_code":"pro_month","period":{"start_at":"2026-11-01 00:00:00","end_at":"2026-12-01 00:00:00"},"quota":{"total_credits":20000,"used_credits":5000,"remaining_credits":15000},"auto_renew":true,"claw_enabled":false}
            """
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(body.utf8))
        }
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            officialBaseURL: URL(string: "https://api.test/v1")!,
            tokenPlanBaseURL: URL(string: "https://token-plan.test/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "tp-plan-key",
            metadata: ["usageKind": "plan"]
        )
        let now = try XCTUnwrap(ISO8601DateFormatter().date(from: "2026-11-13T03:00:00Z"))

        let fetched = try await provider.fetchUsage(credential, now: now)
        let snapshot = try XCTUnwrap(fetched)
        let total = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "plan-total" }))

        XCTAssertEqual(snapshot.planName, "Pro 月度套餐")
        XCTAssertEqual(snapshot.billingMode, "自动续费")
        XCTAssertEqual(snapshot.status, 1)
        XCTAssertEqual(total.used, 5000)
        XCTAssertEqual(total.limit, 20000)
        XCTAssertEqual(total.remaining, 15000)
        XCTAssertEqual(
            snapshot.subscriptionEndAt,
            ISO8601DateFormatter().date(from: "2026-12-01T00:00:00Z")
        )
        await requests.waitForCount(1)
        XCTAssertEqual(requests.count, 1)
        XCTAssertEqual(
            requests.all.first?.value(forHTTPHeaderField: "Authorization"),
            "Bearer tp-plan-key"
        )
    }

    func test官方APIKey认证失败不触发任何网页登录() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(#"{"error":{"message":"Invalid API key.","type":"invalid_request_error","code":"invalid_api_key"}}"#.utf8))
        }
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            officialBaseURL: URL(string: "https://api.test/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "sk-invalid",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期 API Key 无效")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("小米 MiMo：Invalid API key.")
            )
        }
    }

    func test官方Key与查询方式不匹配时给出明确提示() async {
        let provider = XiaomiMiMoUsageProvider()
        let apiCredential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "tp-plan-key",
            metadata: ["usageKind": "api"]
        )
        let planCredential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "sk-api-key",
            metadata: ["usageKind": "plan"]
        )

        do {
            _ = try await provider.fetchUsage(apiCredential, now: .now)
            XCTFail("预期提示查询方式不匹配")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("小米 MiMo：API 按量请使用 sk- 开头的普通 API Key")
            )
        }
        do {
            _ = try await provider.fetchUsage(planCredential, now: .now)
            XCTFail("预期提示查询方式不匹配")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("小米 MiMo：Token Plan 请使用 tp- 开头的专用 API Key")
            )
        }
    }

    func test网页Cookie凭证被彻底拒绝() async {
        let provider = XiaomiMiMoUsageProvider()
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "api-platform_serviceToken=legacy; userId=1",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期拒绝旧版 Cookie")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("小米 MiMo：请使用官方 API Key（API 按量 sk-，Token Plan tp-）")
            )
        }
    }

    func test网络故障映射为传输错误() async {
        struct Offline: Error {}
        let stub = URLProtocolStub.makeSession { _ in throw Offline() }
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            officialBaseURL: URL(string: "https://api.test/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "sk-api-key",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期网络失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .transport)
        }
    }
}

private final class RequestRecorder: @unchecked Sendable {
    private let lock = NSLock()
    private var requests: [URLRequest] = []

    var count: Int {
        lock.withLock { requests.count }
    }

    var all: [URLRequest] {
        lock.withLock { requests }
    }

    func append(_ request: URLRequest) {
        lock.withLock {
            requests.append(request)
        }
    }

    func allSatisfy(_ predicate: (URLRequest) -> Bool) -> Bool {
        lock.withLock { requests.allSatisfy(predicate) }
    }

    func waitForCount(_ expectedCount: Int, timeout: TimeInterval = 1) async {
        let deadline = Date().addingTimeInterval(timeout)
        while count < expectedCount, Date() < deadline {
            try? await Task.sleep(nanoseconds: 10_000_000)
        }
    }
}
