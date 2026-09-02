import XCTest
@testable import RoutinUsage

final class DeepSeekUsageProviderTests: XCTestCase {
    func test余额响应转换为余额型指标且不生成百分比() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            return (response, Data(#"{"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"12.36","granted_balance":"2.36","topped_up_balance":"10.00"}]}"#.utf8))
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(
            credentialID: UUID(),
            providerID: .deepseek,
            kind: .apiKey,
            secret: "sk-test"
        )

        let fetched = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 100))
        let snapshot = try XCTUnwrap(fetched)
        let balance = try XCTUnwrap(snapshot.metrics.first(where: { $0.id == "balance" }))

        XCTAssertEqual(snapshot.providerID, .deepseek)
        XCTAssertEqual(balance.presentation, .balance)
        XCTAssertEqual(balance.value, Decimal(string: "12.36"))
        XCTAssertNil(balance.limit)
        XCTAssertNil(balance.used)
        XCTAssertEqual(balance.currencyCode, "CNY")
        XCTAssertEqual(balance.healthState, .normal)
    }

    func test认证失败映射为统一未授权错误() async {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 401,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data())
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(providerID: .deepseek, kind: .apiKey, secret: "sk-test")

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期认证失败")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .unauthorized)
        }
    }

    func test余额为零或账户不可用时使用风险状态() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(#"{"is_available":false,"balance_infos":[{"currency":"CNY","total_balance":"0","granted_balance":"0","topped_up_balance":"0"}]}"#.utf8))
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(providerID: .deepseek, kind: .apiKey, secret: "sk-test")

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertEqual(snapshot.metrics.first?.healthState, .unavailable)
        XCTAssertEqual(snapshot.metrics.first?.presentation, .balance)
    }

    func test余额低于预警值时使用黄色状态() {
        XCTAssertEqual(
            UsageMetricHealthEvaluator.balanceState(
                balance: 5,
                warningThreshold: 10,
                isAvailable: true
            ),
            .warning
        )
    }
}
