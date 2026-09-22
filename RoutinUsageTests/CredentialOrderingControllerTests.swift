import XCTest
@testable import RoutinUsage

@MainActor
final class CredentialOrderingControllerTests: XCTestCase {
    func test停用凭证会释放菜单栏名额并移除排序项() throws {
        let suiteName = "credential-order-controller.disable.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let settings = AppSettings(defaults: defaults)
        let menuBarIDs = (0..<CredentialDisplayOrder.maximumMenuBarCount).map { _ in UUID() }
        let candidate = UUID()
        settings.displayOrder.menuBarCredentialIDs = menuBarIDs
        settings.displayOrder.popoverCredentialIDs = menuBarIDs + [candidate]
        let controller = CredentialOrderingController(
            settings: settings,
            addCredential: { _ in
                CredentialAddOutcome(saveResult: .saved, addedCredentialID: nil)
            },
            setKeyEnabled: { _, _ in },
            delete: { _ in }
        )

        try controller.setEnabled(menuBarIDs[0], enabled: false)

        XCTAssertEqual(settings.displayOrder.menuBarCredentialIDs, Array(menuBarIDs.dropFirst()))
        XCTAssertEqual(
            settings.displayOrder.popoverCredentialIDs,
            Array(menuBarIDs.dropFirst()) + [candidate]
        )

        controller.addingToMenuBar(candidate, toIndex: 0)
        XCTAssertEqual(
            settings.displayOrder.menuBarCredentialIDs,
            [candidate] + Array(menuBarIDs.dropFirst())
        )

        try controller.setEnabled(menuBarIDs[0], enabled: true)
        XCTAssertEqual(
            settings.displayOrder.popoverCredentialIDs,
            Array(menuBarIDs.dropFirst()) + [candidate, menuBarIDs[0]]
        )
        XCTAssertFalse(settings.displayOrder.menuBarCredentialIDs.contains(menuBarIDs[0]))
    }

    func test删除凭证成功时同步清理独立顺序() throws {
        let suiteName = "credential-order-controller.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let settings = AppSettings(defaults: defaults)
        var deletedIDs: [UUID] = []
        let id = UUID()
        settings.appendCredential(id)
        settings.displayOrder.menuBarCredentialIDs = [id]
        let controller = CredentialOrderingController(
            settings: settings,
            addCredential: { _ in
                CredentialAddOutcome(saveResult: .saved, addedCredentialID: nil)
            },
            setKeyEnabled: { _, _ in },
            delete: { id in
                deletedIDs.append(id)
            }
        )

        let outcome = try controller.delete(id)

        XCTAssertEqual(outcome, .deleted)
        XCTAssertFalse(settings.displayOrder.menuBarCredentialIDs.contains(id))
        XCTAssertFalse(settings.displayOrder.popoverCredentialIDs.contains(id))
        XCTAssertEqual(deletedIDs, [id])
    }

    func test删除缓存失败时仍清理顺序并返回专门结果() throws {
        let suiteName = "credential-order-controller.cache-failure.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let keychain = LocalKeyStore(defaults: defaults)
        let repository = KeyRepository(defaults: defaults, localStore: keychain)
        let key = try repository.add(name: "缓存失败", secret: "plan-cache-failure-0001")
        let settings = AppSettings(defaults: defaults)
        settings.displayOrder.menuBarCredentialIDs = [key.id]
        settings.displayOrder.popoverCredentialIDs = [key.id]
        let store = UsageStore(
            keyRepository: repository,
            localStore: keychain,
            apiClient: ScriptedUsageFetcher(responses: [:]),
            cache: DeleteFailingUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: NotificationSenderFake(),
            defaults: defaults
        )
        let environment = AppEnvironment(
            settings: settings,
            store: store,
            refreshScheduler: RefreshScheduler(),
            loginItemManager: LoginItemManager(),
            keyRepository: repository,
            apiClient: ScriptedUsageFetcher(responses: [:]),
            notificationSender: NotificationSenderFake(),
        )
        let controller = CredentialOrderingController(
            settings: settings,
            addCredential: { _ in
                CredentialAddOutcome(saveResult: .saved, addedCredentialID: nil)
            },
            setKeyEnabled: { _, _ in },
            delete: { try environment.deleteKey($0) }
        )

        let outcome = try controller.delete(key.id)

        XCTAssertEqual(outcome, .cacheCleanupFailed)
        XCTAssertNil(store.state(for: key.id))
        XCTAssertFalse(settings.displayOrder.menuBarCredentialIDs.contains(key.id))
        XCTAssertFalse(settings.displayOrder.popoverCredentialIDs.contains(key.id))
    }
}
