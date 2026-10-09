import XCTest
@testable import RoutinUsage

final class OpenCodeUsageProviderTests: XCTestCase {
    func testFetchUsageMapsGoStatusToSnapshot() async throws {
        let dates = Self.dates
        let body = Self.statusBody(
            product: "go",
            renewalProduct: "go",
            cancelAtPeriodEnd: false,
            fiveHourUsed: "600000000",
            fiveHourLimit: "3000000000",
            weeklyUsed: "1500000000",
            weeklyLimit: "7500000000",
            monthlyUsed: "4500000000",
            monthlyLimit: "15000000000"
        )
        let stub = URLProtocolStub.makeSession { _ in
            Self.response(statusCode: 200, body: body)
        }
        let provider = OpenCodeUsageProvider(
            session: stub.session,
            baseURL: URL(string: "https://opencode.test/console")!
        )
        let credential = Self.credential()

        let snapshot = try await provider.fetchUsage(credential, now: dates.now)

        XCTAssertEqual(snapshot?.providerID, .opencode)
        XCTAssertEqual(snapshot?.planName, "OpenCode Go")
        XCTAssertEqual(snapshot?.statusText, "正常续费")
        XCTAssertEqual(snapshot?.subscriptionStartAt, dates.start)
        XCTAssertEqual(snapshot?.subscriptionEndAt, dates.end)
        XCTAssertEqual(snapshot?.metrics.map(\.id), ["fiveHour", "weekly", "monthly"])
        XCTAssertEqual(snapshot?.metrics.map(\.label), ["5 小时", "周", "月"])
        XCTAssertTrue(snapshot?.metrics.allSatisfy { $0.unit == .currency } == true)
        XCTAssertTrue(snapshot?.metrics.allSatisfy { $0.currencyCode == "$" } == true)
        XCTAssertTrue(snapshot?.metrics.allSatisfy { $0.presentation == .progress } == true)
        XCTAssertTrue(snapshot?.metrics.allSatisfy { $0.semantic == .usedQuota } == true)

        let fiveHour = try XCTUnwrap(snapshot?.metrics.first { $0.id == "fiveHour" })
        XCTAssertEqual(fiveHour.used, 6)
        XCTAssertEqual(fiveHour.limit, 30)
        XCTAssertEqual(fiveHour.remaining, 24)
        XCTAssertEqual(fiveHour.windowStart, dates.fiveHourStart)
        XCTAssertEqual(fiveHour.windowEnd, dates.fiveHourReset)

        let weekly = try XCTUnwrap(snapshot?.metrics.first { $0.id == "weekly" })
        XCTAssertEqual(weekly.used, 15)
        XCTAssertEqual(weekly.limit, 75)
        XCTAssertEqual(weekly.remaining, 60)
        XCTAssertEqual(weekly.windowStart, dates.weekStart)
        XCTAssertEqual(weekly.windowEnd, dates.weekReset)

        XCTAssertEqual(stub.registration.requestCount, 1)
        let request = try XCTUnwrap(stub.registration.lastRequest)
        XCTAssertEqual(request.url?.absoluteString, "https://opencode.test/console/api/go/status")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer oc_sk_test")
        XCTAssertFalse(request.url?.query?.contains("oc_sk_test") == true)
    }

    func testFetchUsageClampsRemainingAndMapsMonthlyWindow() async throws {
        let dates = Self.dates
        let body = Self.statusBody(
            product: "go-plus",
            renewalProduct: "go-plus",
            cancelAtPeriodEnd: true,
            fiveHourUsed: "100000000",
            fiveHourLimit: "100000000",
            weeklyUsed: "0",
            weeklyLimit: "100000000",
            monthlyUsed: "120000000",
            monthlyLimit: "100000000"
        )
        let stub = URLProtocolStub.makeSession { _ in
            Self.response(statusCode: 200, body: body)
        }
        let provider = OpenCodeUsageProvider(session: stub.session)

        let snapshot = try await provider.fetchUsage(Self.credential(), now: dates.now)

        XCTAssertEqual(snapshot?.planName, "OpenCode Go Plus")
        XCTAssertEqual(snapshot?.statusText, "将在当前周期结束后到期")
        XCTAssertEqual(snapshot?.billingMode, "取消续订")
        let monthly = try XCTUnwrap(snapshot?.metrics.first { $0.id == "monthly" })
        XCTAssertEqual(monthly.used, 1.2)
        XCTAssertEqual(monthly.limit, 1)
        XCTAssertEqual(monthly.remaining, 0)
        XCTAssertEqual(monthly.windowEnd, dates.monthReset)
        XCTAssertEqual(monthly.healthState, .critical)
    }

    func testFetchUsesBearerAuthorizationWithoutLeakingCredentialInURL() async throws {
        let stub = URLProtocolStub.makeSession { _ in
            Self.response(statusCode: 200, body: Self.statusBody())
        }
        let provider = OpenCodeUsageProvider(session: stub.session)

        _ = try await provider.fetchUsage(Self.credential(secret: "oc_sk_secret_value"), now: Self.dates.now)

        let request = try XCTUnwrap(stub.registration.lastRequest)
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer oc_sk_secret_value")
        XCTAssertEqual(request.url?.query, nil)
    }

    func testNullStatusThrowsProviderMessage() async {
        let stub = URLProtocolStub.makeSession { _ in
            Self.response(statusCode: 200, body: "null")
        }
        let provider = OpenCodeUsageProvider(session: stub.session)

        do {
            _ = try await provider.fetchUsage(Self.credential(), now: Self.dates.now)
            XCTFail("预期提示没有有效订阅")
        } catch {
            XCTAssertEqual(
                error as? UsageProviderError,
                .providerMessage("OpenCode：未找到有效 Go 订阅")
            )
        }
    }

    func testHTTPStatusMapsToDiagnosticErrors() async throws {
        let expectations: [(Int, UsageProviderError)] = [
            (400, .invalidResponse),
            (401, .unauthorized),
            (403, .providerMessage("OpenCode：API Key 没有 Console 状态权限，请创建带读取权限的服务账号 Key")),
            (404, .providerMessage("OpenCode：未找到订阅或接口路径已变化")),
            (429, .rateLimited),
            (500, .providerUnavailable)
        ]

        for (statusCode, expected) in expectations {
            let stub = URLProtocolStub.makeSession { _ in
                Self.response(statusCode: statusCode, body: #"{"_tag":"Error"}"#)
            }
            let provider = OpenCodeUsageProvider(session: stub.session)

            do {
                _ = try await provider.fetchUsage(Self.credential(), now: Self.dates.now)
                XCTFail("预期 HTTP \(statusCode) 映射失败")
            } catch {
                XCTAssertEqual(error as? UsageProviderError, expected, "HTTP \(statusCode) 映射错误")
            }
        }
    }
}

private extension OpenCodeUsageProviderTests {
    struct FixtureDates {
        let now: Date
        let start: Date
        let end: Date
        let fiveHourStart: Date
        let fiveHourReset: Date
        let weekStart: Date
        let weekReset: Date
        let monthReset: Date
    }

    static let dates: FixtureDates = {
        let formatter = ISO8601DateFormatter()
        return FixtureDates(
            now: formatter.date(from: "2026-10-09T12:00:00Z")!,
            start: formatter.date(from: "2026-10-01T00:00:00Z")!,
            end: formatter.date(from: "2026-11-01T00:00:00Z")!,
            fiveHourStart: formatter.date(from: "2026-10-09T08:00:00Z")!,
            fiveHourReset: formatter.date(from: "2026-10-09T13:00:00Z")!,
            weekStart: formatter.date(from: "2026-10-05T00:00:00Z")!,
            weekReset: formatter.date(from: "2026-10-12T00:00:00Z")!,
            monthReset: formatter.date(from: "2026-11-01T00:00:00Z")!
        )
    }()

    static func credential(secret: String = "oc_sk_test") -> ProviderCredential {
        ProviderCredential(
            providerID: .opencode,
            kind: .bearerAPIKey,
            secret: secret
        )
    }

    static func statusBody(
        product: String = "go",
        renewalProduct: String = "go",
        cancelAtPeriodEnd: Bool = false,
        fiveHourUsed: String = "0",
        fiveHourLimit: String = "100000000",
        weeklyUsed: String = "0",
        weeklyLimit: String = "100000000",
        monthlyUsed: String = "0",
        monthlyLimit: String = "100000000"
    ) -> String {
        let dates = Self.dates
        return """
        {
          "subscriberUserId": "user_1",
          "product": "\(product)",
          "renewalProduct": "\(renewalProduct)",
          "cancelAtPeriodEnd": \(cancelAtPeriodEnd),
          "renewalPending": false,
          "access": {
            "startsAt": "\(Self.iso.string(from: dates.start))",
            "endsAt": "\(Self.iso.string(from: dates.end))",
            "meters": {
              "fiveHour": {
                "startsAt": "\(Self.iso.string(from: dates.fiveHourStart))",
                "resetsAt": "\(Self.iso.string(from: dates.fiveHourReset))",
                "limitMicroCents": "\(fiveHourLimit)",
                "usedMicroCents": "\(fiveHourUsed)"
              },
              "week": {
                "startsAt": "\(Self.iso.string(from: dates.weekStart))",
                "resetsAt": "\(Self.iso.string(from: dates.weekReset))",
                "limitMicroCents": "\(weeklyLimit)",
                "usedMicroCents": "\(weeklyUsed)"
              },
              "month": {
                "resetsAt": "\(Self.iso.string(from: dates.monthReset))",
                "limitMicroCents": "\(monthlyLimit)",
                "usedMicroCents": "\(monthlyUsed)"
              }
            }
          }
        }
        """
    }

    static let iso: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    static func response(statusCode: Int, body: String) -> (HTTPURLResponse, Data) {
        (
            HTTPURLResponse(
                url: URL(string: "https://opencode.test/console/api/go/status")!,
                statusCode: statusCode,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            )!,
            Data(body.utf8)
        )
    }
}
