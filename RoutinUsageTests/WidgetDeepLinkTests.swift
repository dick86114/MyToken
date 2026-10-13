import XCTest
@testable import RoutinUsage

@MainActor
final class WidgetDeepLinkTests: XCTestCase {
    func test_credential_deep_link_parses_uuid_and_requests_refresh() async throws {
        let context = try DeepLinkTestContext()
        defer { context.cleanUp() }
        let url = try XCTUnwrap(
            URL(string: "mytoken://credential/\(context.credentialID.uuidString)?refresh=1")
        )

        XCTAssertTrue(context.environment.openCredentialDeepLink(url))
        await context.fetcher.waitUntilRequested(context.secret)

        let requestCount = await context.fetcher.requestCount(for: context.secret)
        XCTAssertEqual(requestCount, 1)
        XCTAssertEqual(context.environment.pendingSettingsCredentialID, context.credentialID)
    }

    func test_invalid_credential_deep_link_is_rejected() throws {
        let context = try DeepLinkTestContext()
        defer { context.cleanUp() }

        XCTAssertFalse(
            context.environment.openCredentialDeepLink(
                URL(string: "other://credential/\(context.credentialID.uuidString)")!
            )
        )
        XCTAssertFalse(
            context.environment.openCredentialDeepLink(
                URL(string: "mytoken://other/\(context.credentialID.uuidString)")!
            )
        )
        XCTAssertFalse(
            context.environment.openCredentialDeepLink(
                URL(string: "mytoken://credential/not-a-uuid")!
            )
        )
        XCTAssertFalse(
            context.environment.openCredentialDeepLink(
                URL(string: "mytoken://credential/\(UUID().uuidString)")!
            )
        )
    }
}

@MainActor
private struct DeepLinkTestContext {
    let suiteName: String
    let defaults: UserDefaults
    let secret: String
    let credentialID: UUID
    let fetcher: ScriptedUsageFetcher
    let environment: AppEnvironment

    init() throws {
        suiteName = "WidgetDeepLinkTests-\(UUID().uuidString)"
        defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        secret = "sk-test-secret-value"

        let localStore = LocalKeyStore(defaults: defaults)
        let repository = KeyRepository(defaults: defaults, localStore: localStore)
        let configuration = try repository.add(
            name: "测试凭证",
            secret: secret,
            providerID: .deepseek,
            credentialKind: .apiKey,
            metadata: [:]
        )
        credentialID = configuration.id
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
            metrics: []
        )
        fetcher = ScriptedUsageFetcher(responses: [secret: .success(snapshot)])
        let store = UsageStore(
            keyRepository: repository,
            localStore: localStore,
            apiClient: fetcher,
            cache: InMemoryUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: NotificationSenderFake(authorized: false),
            defaults: defaults,
            notificationsEnabled: false
        )
        environment = AppEnvironment(
            settings: AppSettings(defaults: defaults),
            store: store,
            refreshScheduler: RefreshScheduler(),
            loginItemManager: LoginItemManager(),
            keyRepository: repository,
            apiClient: fetcher,
            notificationSender: NotificationSenderFake(authorized: false),
            applicationNotificationCenter: NotificationCenter()
        )
    }

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}
