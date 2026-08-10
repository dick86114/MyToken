import XCTest
@testable import RoutinUsage

@MainActor
final class KeyEditorValidationTests: XCTestCase {
    func test空名称返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: " \n ", secret: "plan-valid-1234")) { error in
            XCTAssertEqual(error.localizedDescription, "请输入 Key 名称")
        }
    }

    func test空Key返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: "主账号", secret: "")) { error in
            XCTAssertEqual(error.localizedDescription, "请输入 plan Key")
        }
    }

    func test非PlanKey返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: "主账号", secret: "sk-invalid")) { error in
            XCTAssertEqual(error.localizedDescription, "Key 必须以 plan- 开头")
        }
    }

    func test低阈值不小于高阈值返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validateThresholds(low: 95, high: 80)) { error in
            XCTAssertEqual(error.localizedDescription, "低阈值必须小于高阈值")
        }
    }

    func test合法输入只规范化名称并保持Key原值() throws {
        let input = try KeyEditorValidation.validate(
            name: "  主账号 \n",
            secret: "plan-AbC-8F2A "
        )

        XCTAssertEqual(input.name, "主账号")
        XCTAssertEqual(input.secret, "plan-AbC-8F2A ")
    }

    func test网络验证失败保留名称和Key以便重试() async {
        let model = KeyEditorModel(name: " 主账号 ", secret: "plan-sensitive-8F2A")

        await model.save { _, _ in
            throw UsageStoreError.network
        }

        XCTAssertEqual(model.name, " 主账号 ")
        XCTAssertEqual(model.secret, "plan-sensitive-8F2A")
        XCTAssertEqual(model.errorMessage, "网络连接失败，请检查网络后重试")
        XCTAssertFalse(model.isSaving)
    }

    func test服务端四零一响应显示Key无效() async {
        let model = KeyEditorModel(name: "主账号", secret: "plan-invalid-401")

        await model.save { _, _ in
            throw UsageStoreError.server(statusCode: 401)
        }

        XCTAssertEqual(model.errorMessage, "Key 无效")
    }

    func test取消正在验证的表单不会保存配置或Keychain() async throws {
        let suiteName = "ai.routin.usage-monitor.key-editor-tests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let secret = "plan-cancelled-8F2A"
        let keychain = KeyEditorKeychainFake()
        let repository = KeyRepository(defaults: defaults, keychain: keychain)
        let fetcher = ScriptedUsageFetcher(responses: [secret: .suspended])
        let store = UsageStore(
            keyRepository: repository,
            keychain: keychain,
            apiClient: fetcher,
            cache: InMemoryUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: NotificationSenderFake(),
            defaults: defaults
        )
        let model = KeyEditorModel(name: "主账号", secret: secret)

        model.startSaving { name, secret in
            try await store.addValidatedKey(name: name, secret: secret)
            return .saved
        }
        await fetcher.waitUntilRequested(secret)

        model.cancelSaving()
        await fetcher.resume(secret, with: .success(nil))
        let didFinishCancellation = await waitUntil { !model.isSaving }

        XCTAssertTrue(didFinishCancellation)
        XCTAssertTrue(repository.list().isEmpty)
        XCTAssertFalse(keychain.contains(secret: secret))
        XCTAssertNil(model.saveResult)
        XCTAssertNil(model.errorMessage)
    }

    func testKey列表只显示固定掩码和四位尾号() {
        XCTAssertEqual(KeyDisplayMask.masked(suffix: "8F2A"), "plan-••••8F2A")
    }

    private func waitUntil(_ condition: @MainActor () -> Bool) async -> Bool {
        for _ in 0..<1_000 {
            if condition() {
                return true
            }
            await Task.yield()
        }
        return condition()
    }
}

private final class KeyEditorKeychainFake: KeychainStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var secrets: [UUID: String] = [:]

    func save(_ secret: String, for id: UUID) throws {
        lock.withLock { secrets[id] = secret }
    }

    func read(for id: UUID) throws -> String? {
        lock.withLock { secrets[id] }
    }

    func delete(for id: UUID) throws {
        _ = lock.withLock { secrets.removeValue(forKey: id) }
    }

    func contains(secret: String) -> Bool {
        lock.withLock { secrets.values.contains(secret) }
    }
}
