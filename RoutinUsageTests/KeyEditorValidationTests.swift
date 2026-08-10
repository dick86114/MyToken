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

    func test显示名称匹配Plan秘密时返回中文错误() {
        XCTAssertThrowsError(
            try KeyEditorValidation.validate(
                name: "plan-sensitive-8F2A",
                secret: "plan-sensitive-8F2A"
            )
        ) { error in
            XCTAssertEqual(error.localizedDescription, "显示名称不能是 plan Key")
        }
    }

    func test短于四位的PlanKey内容返回中文错误() {
        XCTAssertThrowsError(
            try KeyEditorValidation.validate(name: "主账号", secret: "plan-abc")
        ) { error in
            XCTAssertEqual(error.localizedDescription, "plan Key 内容至少需要 4 位")
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

    func test取消正在验证的表单不会保存配置或本地Key() async throws {
        let suiteName = "ai.routin.usage-monitor.key-editor-tests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let secret = "plan-cancelled-8F2A"
        let localStore = LocalKeyStore(defaults: defaults)
        let repository = KeyRepository(defaults: defaults, localStore: localStore)
        let fetcher = ScriptedUsageFetcher(responses: [secret: .suspended])
        let store = UsageStore(
            keyRepository: repository,
            localStore: localStore,
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
        let persistedValues = defaults.dictionaryRepresentation().values
        XCTAssertFalse(persistedValues.contains { ($0 as? String) == secret })
        XCTAssertNil(model.saveResult)
        XCTAssertNil(model.errorMessage)
    }

    func testKey列表只显示固定掩码和四位尾号() {
        XCTAssertEqual(KeyDisplayMask.masked(suffix: "8F2A"), "plan-••••8F2A")
    }

    func test不足四位的旧后缀在设置中完全遮掩() {
        let secret = "abc"

        let displayText = KeyDisplayMask.masked(suffix: secret)

        XCTAssertEqual(displayText, "plan-••••")
        XCTAssertFalse(displayText.contains(secret))
    }

    func test真实旧短Key后缀在设置中全部遮掩() {
        for legacySuffix in ["an-a", "n-ab", "-abc"] {
            let displayText = KeyDisplayMask.masked(suffix: legacySuffix)

            XCTAssertEqual(displayText, "plan-••••")
            XCTAssertFalse(displayText.contains(legacySuffix))
        }
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
