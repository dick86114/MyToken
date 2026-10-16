import XCTest
import Foundation
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

    /// 控制台票据过期时用持久登录态静默续期，成功就重试一次，用户无感。
    func test登录态过期时静默续期并重试一次() async throws {
        let requests = RequestRecorder()
        let expiredCookie = "api-platform_serviceToken=expired; userId=1"
        let renewedCookie = "api-platform_serviceToken=fresh; userId=1"
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            let url = try XCTUnwrap(request.url)
            let cookie = request.value(forHTTPHeaderField: "Cookie")
            guard cookie == renewedCookie else {
                let response = try XCTUnwrap(HTTPURLResponse(
                    url: url,
                    statusCode: 401,
                    httpVersion: nil,
                    headerFields: nil
                ))
                return (response, Data(#"{"message":"未登录"}"#.utf8))
            }
            let body = url.path == "/api/v1/balance"
                ? #"{"balance":12.5,"currency":"CNY"}"#
                : #"{"costUsage":{"totalCost":3.25},"tokenUsage":{"inputToken":1000,"outputToken":250,"cacheToken":400}}"#
            let response = try XCTUnwrap(HTTPURLResponse(
                url: url,
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(body.utf8))
        }
        let credentialID = UUID()
        let renewalCalls = RenewalCallRecorder()
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            baseURL: URL(string: "https://platform.test/api/v1")!,
            renewSession: { id, previous in
                renewalCalls.record(id: id, previous: previous)
                return previous == expiredCookie ? renewedCookie : nil
            }
        )
        let credential = ProviderCredential(
            credentialID: credentialID,
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: expiredCookie,
            metadata: ["usageKind": "api"]
        )

        let fetched = try await provider.fetchUsage(credential, now: .now)

        XCTAssertNotNil(fetched)
        XCTAssertEqual(renewalCalls.calls, [RenewalCall(id: credentialID, previous: expiredCookie)])
        // 先失败的一对请求 + 续期后重试的一对请求。
        XCTAssertEqual(requests.count, 4)
        XCTAssertEqual(
            Set(requests.all.compactMap { $0.value(forHTTPHeaderField: "Cookie") }),
            Set([expiredCookie, renewedCookie])
        )
    }

    /// 静默续期拿不到新票据时，必须保留“需要重新登录”，并且不再重试。
    func test续期失败时保留重新登录提示且不重试() async {
        let requests = RequestRecorder()
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(#"{"message":"未登录"}"#.utf8))
        }
        var renewalCount = 0
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            baseURL: URL(string: "https://platform.test/api/v1")!,
            renewSession: { _, _ in
                renewalCount += 1
                // 兑换后票据没有变化，说明账号登录态也已失效。
                return nil
            }
        )
        let credential = ProviderCredential(
            credentialID: UUID(),
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "api-platform_serviceToken=expired",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期仍然失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .providerMessage("小米 MiMo：未登录"))
        }

        XCTAssertEqual(renewalCount, 1)
        // async let 的第二个请求可能在首个 401 抛出后瞬间仍在收尾，先等它落盘再断言。
        let deadline = Date().addingTimeInterval(1)
        while requests.count < 2, Date() < deadline {
            try? await Task.sleep(nanoseconds: 10_000_000)
        }
        XCTAssertEqual(requests.count, 2)
    }

    /// 兑换回来还是同一串票据时，不浪费一次必然失败的重试。
    func test续期未换到新票据时不重试() async {
        let requests = RequestRecorder()
        let expiredCookie = "api-platform_serviceToken=expired"
        let stub = URLProtocolStub.makeSession { request in
            requests.append(request)
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
            baseURL: URL(string: "https://platform.test/api/v1")!,
            renewSession: { _, previous in previous }
        )
        let credential = ProviderCredential(
            credentialID: UUID(),
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: expiredCookie,
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期仍然失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .providerMessage("小米 MiMo：未登录"))
        }

        XCTAssertEqual(requests.count, 2)
    }

    /// 普通网络故障与登录态无关，不该触发兑换。
    func test网络故障不触发静默续期() async {
        struct Offline: Error {}
        let stub = URLProtocolStub.makeSession { _ in throw Offline() }
        var renewalCount = 0
        let provider = XiaomiMiMoUsageProvider(
            session: stub.session,
            baseURL: URL(string: "https://platform.test/api/v1")!,
            renewSession: { _, _ in
                renewalCount += 1
                return "api-platform_serviceToken=fresh"
            }
        )
        let credential = ProviderCredential(
            credentialID: UUID(),
            providerID: .xiaomi,
            kind: .bearerAPIKey,
            secret: "api-platform_serviceToken=ok",
            metadata: ["usageKind": "api"]
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期网络失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .transport)
        }

        XCTAssertEqual(renewalCount, 0)
    }

    func test登录态错误判定只覆盖认证类原因() {
        XCTAssertTrue(UsageProviderError.unauthorized.isAuthenticationFailure)
        XCTAssertTrue(UsageProviderError.providerMessage("小米 MiMo：未登录").isAuthenticationFailure)
        XCTAssertTrue(UsageProviderError.providerMessage("登录已失效").isAuthenticationFailure)
        XCTAssertFalse(UsageProviderError.rateLimited.isAuthenticationFailure)
        XCTAssertFalse(UsageProviderError.transport.isAuthenticationFailure)
        XCTAssertFalse(UsageProviderError.providerMessage("余额不足").isAuthenticationFailure)
    }
}

private struct RenewalCall: Equatable {
    let id: UUID
    let previous: String
}

private final class RenewalCallRecorder: @unchecked Sendable {
    private let lock = NSLock()
    private var recorded: [RenewalCall] = []

    var calls: [RenewalCall] {
        lock.withLock { recorded }
    }

    func record(id: UUID, previous: String) {
        lock.withLock { recorded.append(RenewalCall(id: id, previous: previous)) }
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
}
