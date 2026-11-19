import XCTest
@testable import RoutinUsage

final class DeepSeekUsageProviderTests: XCTestCase {
    func test余额响应转换为余额型指标且不生成百分比() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: ["Content-Type": "application/json"]
            ))
            if path == "/models" {
                return (response, Data(#"{"object":"list","data":[{"id":"deepseek-v4-flash"},{"id":"deepseek-v4-pro"}]}"#.utf8))
            }
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
        XCTAssertEqual(snapshot.allowedModels, ["deepseek-v4-flash", "deepseek-v4-pro"])
        XCTAssertEqual(balance.presentation, .balance)
        XCTAssertEqual(balance.value, Decimal(string: "12.36"))
        XCTAssertNil(balance.limit)
        XCTAssertNil(balance.used)
        XCTAssertEqual(balance.currencyCode, "CNY")
        XCTAssertEqual(balance.healthState, .normal)
    }

    func test模型清单失败时保留空列表并继续显示余额() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let path = request.url?.path ?? ""
            let statusCode = path == "/models" ? 404 : 200
            let body = #"{"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"12.36","granted_balance":"2.36","topped_up_balance":"10.00"}]}"#
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: statusCode,
                httpVersion: nil,
                headerFields: nil
            ))
            return (response, Data(body.utf8))
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(providerID: .deepseek, kind: .apiKey, secret: "sk-test")

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)

        XCTAssertTrue(snapshot.allowedModels.isEmpty)
        XCTAssertTrue(snapshot.metrics.contains(where: { $0.id == "balance" }))
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

    func test多币种余额为每个币种各生成一组指标() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            let body = #"""
            {"is_available":true,"balance_infos":[
              {"currency":"CNY","total_balance":"110.00","granted_balance":"10.00","topped_up_balance":"100.00"},
              {"currency":"USD","total_balance":"23.86","granted_balance":"5.00","topped_up_balance":"18.86"}
            ]}
            """#
            return (response, Data(body.utf8))
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(providerID: .deepseek, kind: .apiKey, secret: "sk-test")

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)

        let balances = snapshot.metrics.filter { $0.presentation == .balance }
        XCTAssertEqual(balances.count, 2, "双币种账户应输出两条余额指标")
        XCTAssertEqual(balances.map(\.currencyCode), ["CNY", "USD"])
        XCTAssertEqual(balances.first?.value, Decimal(string: "110.00"))
        XCTAssertEqual(balances.last?.value, Decimal(string: "23.86"))

        // 每个币种都带自己的充值/赠金明细。
        XCTAssertNotNil(snapshot.metrics.first { $0.id == "toppedUpBalance-CNY" })
        XCTAssertNotNil(snapshot.metrics.first { $0.id == "grantedBalance-USD" })
    }

    func test账户可用状态只输出一次且反映is_available() async throws {
        let stub = URLProtocolStub.makeSession { request in
            let response = try XCTUnwrap(HTTPURLResponse(
                url: try XCTUnwrap(request.url),
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            ))
            let body = #"""
            {"is_available":false,"balance_infos":[
              {"currency":"CNY","total_balance":"0","granted_balance":"0","topped_up_balance":"0"},
              {"currency":"USD","total_balance":"0","granted_balance":"0","topped_up_balance":"0"}
            ]}
            """#
            return (response, Data(body.utf8))
        }
        let provider = DeepSeekUsageProvider(session: stub.session)
        let credential = ProviderCredential(providerID: .deepseek, kind: .apiKey, secret: "sk-test")

        let fetched = try await provider.fetchUsage(credential, now: .now)
        let snapshot = try XCTUnwrap(fetched)

        let availability = snapshot.metrics.filter { $0.id == "availability" }
        XCTAssertEqual(availability.count, 1, "账户状态属于整个账户，不应按币种重复")
        XCTAssertEqual(availability.first?.value, 0)
        XCTAssertEqual(availability.first?.healthState, .unavailable)
    }
}
