import XCTest
@testable import RoutinUsage

@MainActor
final class UsageStoreWidgetSyncTests: XCTestCase {
    func test_usage_store_publishes_after_refresh_and_configuration_changes() async throws {
        let suiteName = "UsageStoreWidgetSyncTests-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let localStore = LocalKeyStore(defaults: defaults)
        let repository = KeyRepository(defaults: defaults, localStore: localStore)
        let secret = "sk-test-secret-value"
        let configuration = try repository.add(
            name: "测试凭证",
            secret: secret,
            providerID: .deepseek,
            credentialKind: .apiKey,
            metadata: [:]
        )
        let snapshot = UsageSnapshot(
            planName: "API 余额",
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 100),
            providerID: .deepseek,
            credentialID: configuration.id,
            metrics: [
                NormalizedUsageMetric(
                    id: "balance",
                    label: "账户余额",
                    value: 10,
                    unit: .currency,
                    presentation: .balance,
                    semantic: .balance,
                    currencyCode: "CNY"
                )
            ]
        )
        let fetcher = ScriptedUsageFetcher(responses: [secret: .success(snapshot)])
        let recorder = WidgetPublishRecorder()
        let store = UsageStore(
            keyRepository: repository,
            localStore: localStore,
            apiClient: fetcher,
            cache: InMemoryUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: NotificationSenderFake(authorized: false),
            defaults: defaults,
            notificationsEnabled: false,
            widgetPublisher: { states, orderedIDs in
                recorder.record(states: states, orderedIDs: orderedIDs)
            }
        )

        XCTAssertGreaterThanOrEqual(recorder.count, 1)

        await store.refreshAll()
        let afterRefresh = recorder.count
        XCTAssertGreaterThan(afterRefresh, 0)
        XCTAssertEqual(recorder.lastOrderedIDs, [configuration.id])
        XCTAssertNotNil(recorder.lastStates[configuration.id]?.snapshot)

        store.reloadConfigurations()
        XCTAssertGreaterThan(recorder.count, afterRefresh)

        let afterReload = recorder.count
        try store.setKeyEnabled(configuration.id, enabled: false)
        XCTAssertGreaterThan(recorder.count, afterReload)

        let afterDisable = recorder.count
        try store.deleteKey(configuration.id)
        XCTAssertGreaterThan(recorder.count, afterDisable)
        XCTAssertEqual(recorder.lastOrderedIDs, [])
    }
}

@MainActor
private final class WidgetPublishRecorder {
    private(set) var count = 0
    private(set) var lastStates: [UUID: KeyUsageState] = [:]
    private(set) var lastOrderedIDs: [UUID] = []

    func record(states: [UUID: KeyUsageState], orderedIDs: [UUID]) {
        count += 1
        lastStates = states
        lastOrderedIDs = orderedIDs
    }
}
