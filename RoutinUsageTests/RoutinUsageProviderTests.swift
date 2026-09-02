import XCTest
@testable import RoutinUsage

final class RoutinUsageProviderTests: XCTestCase {
    func testRoutin适配器保留现有快照并写入供应商凭证身份() async throws {
        let keyID = UUID()
        let snapshot = UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: UsageMetric(used: 2, limit: 10, remaining: 8, percent: 20, unit: .usd, windowEnd: nil),
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 100)
        )
        let fetcher = ScriptedUsageFetcher(responses: ["plan-test": .success(snapshot)])
        let provider = RoutinUsageProvider(client: fetcher)
        let credential = ProviderCredential(
            credentialID: keyID,
            providerID: .routin,
            kind: .bearerAPIKey,
            secret: "plan-test"
        )

        let result = try await provider.fetchUsage(credential, now: Date(timeIntervalSince1970: 200))

        XCTAssertEqual(result?.providerID, .routin)
        XCTAssertEqual(result?.credentialID, keyID)
        XCTAssertEqual(result?.planName, "Pro")
        XCTAssertEqual(result?.fiveHour?.percent, 20)
    }

    func testRoutin适配器拒绝其它供应商凭证() async {
        let provider = RoutinUsageProvider(client: ScriptedUsageFetcher(responses: [:]))
        let credential = ProviderCredential(
            providerID: .deepseek,
            kind: .apiKey,
            secret: "sk-test"
        )

        do {
            _ = try await provider.fetchUsage(credential, now: .now)
            XCTFail("预期拒绝非 Routin 凭证")
        } catch {
            XCTAssertEqual(error as? UsageProviderError, .invalidCredential)
        }
    }
}
