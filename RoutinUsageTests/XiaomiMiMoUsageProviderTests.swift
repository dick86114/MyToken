import XCTest
@testable import RoutinUsage

final class XiaomiMiMoUsageProviderTests: XCTestCase {
    func testAPI模式读取余额和按量用量并转换指标() async throws {
        let requests = RequestRecorder()
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            let body: String
            switch request.url?.path {
            case "/api/v1/balance":
                body = #"{"balance":12.5,"frozenBalance":1,"giftBalance":2.5,"cashBalance":10,"currency":"CNY"}"#
            case "/api/v1/usage":
                body = #"{"costUsage":{"totalCost":3.25},"tokenUsage":{"inputToken":1000,"outputToken":250,"cacheToken":400},"pluginUsage":{"totalRequestCount":3}}"#
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
            baseURL: URL(string: "https://platform.test/api/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "session-token",
            metadata: ["usageKind": "api", "balanceWarningThreshold": "10"]
        )

        let fetched = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 100))
        let snapshot = try XCTUnwrap(fetched)
        let balance = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "account-balance" }))
        let totalTokens = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "total-tokens" }))

        XCTAssertEqual(snapshot.providerID, .xiaomi)
        XCTAssertEqual(snapshot.planName, "API 按量")
        XCTAssertEqual(balance.value, Decimal(string: "12.5"))
        XCTAssertEqual(balance.currencyCode, "CNY")
        XCTAssertEqual(balance.healthState, .normal)
        XCTAssertEqual(totalTokens.value, 1250)
        XCTAssertEqual(totalTokens.label, "历史消耗")
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "input-tokens" })?.value,
            600
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "input-tokens" })?.label,
            "未命中缓存"
        )
        XCTAssertEqual(
            snapshot.metrics.first(where: { $0.id == "cache-tokens" })?.label,
            "命中缓存"
        )
        XCTAssertEqual(requests.count, 2)
        XCTAssertTrue(requests.allSatisfy {
            $0.value(forHTTPHeaderField: "Cookie") == "api-platform_serviceToken=session-token"
        })
    }

    func testPlan模式读取订阅和额度明细() async throws {
        let requests = RequestRecorder()
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            let body: String
            switch request.url?.path {
            case "/api/v1/tokenPlan/detail":
                body = #"{"code":0,"data":{"planCode":"pro_month","planName":"Pro 月度套餐","expired":false,"enableAutoRenew":true,"currentPeriodStart":"2026-09-01T00:00:00Z","currentPeriodEnd":"2026-10-01T00:00:00Z"}}"#
            case "/api/v1/tokenPlan/usage":
                body = #"{"code":0,"data":{"usage":{"items":[{"name":"five_hour_token","used":200,"limit":1000,"percent":20},{"name":"total_token","used":5000,"limit":20000,"percent":25},{"name":"compensation_total_token","used":300,"limit":300,"percent":100}]}}}"#
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
            baseURL: URL(string: "https://platform.test/api/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "Cookie: api-platform_serviceToken=abc; api-platform_ph=def",
            metadata: ["usageKind": "plan"]
        )

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)
        let fiveHour = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "plan-five-hour" }))
        let total = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "plan-total" }))
        let compensation = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "plan-compensation" }))

        XCTAssertEqual(snapshot.planName, "Pro 月度套餐")
        XCTAssertEqual(snapshot.statusText, "有效")
        XCTAssertEqual(snapshot.billingMode, "自动续费")
        XCTAssertEqual(fiveHour.used, 200)
        XCTAssertEqual(fiveHour.limit, 1000)
        XCTAssertEqual(fiveHour.remaining, 800)
        XCTAssertEqual(total.used, 5000)
        XCTAssertEqual(total.limit, 20000)
        XCTAssertEqual(compensation.value, 300)
        XCTAssertEqual(snapshot.subscriptionEndAt, ISO8601DateFormatter().date(from: "2026-10-01T00:00:00Z"))
        XCTAssertEqual(requests.count, 2)
        XCTAssertTrue(requests.allSatisfy {
            $0.value(forHTTPHeaderField: "Cookie") == "api-platform_serviceToken=abc; api-platform_ph=def"
        })
    }

    func test认证失败映射为统一未授权错误() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(#"{"message":"未登录"}"#.utf8))
        }
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            baseURL: URL(string: "https://platform.test/api/v1")!
        )
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "token",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期认证失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .providerMessage("小米 MiMo：未登录"))
        }
    }

    func testCookie包含换行时拒绝凭证() async {
        let provider = XiaomiMiMoUsageProvider()
        let credential = ProviderCredential(
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "cookie=a\nInjected: true",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期拒绝凭证")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .invalidCredential)
        }
    }
}

private final class RequestRecorder: @unchecked Sendable {
    private let lock = NSLock()
    private var requests: [URLRequest] = []

    var count: Int {
        lock.withLock { requests.count }
    }

    func append(_ request: URLRequest) {
        lock.withLock {
            requests.append(request)
        }
    }

    func allSatisfy(_ predicate: (URLRequest) -> Bool) -> Bool {
        lock.withLock { requests.allSatisfy(predicate) }
    }
}
