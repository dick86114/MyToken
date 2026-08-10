import Foundation
import XCTest
@testable import RoutinUsage

final class KeyRepositoryTests: XCTestCase {
    func test添加配置只把非敏感元数据写入偏好设置() throws {
        let context = makeContext()
        defer { context.cleanUp() }

        let saved = try context.repository.add(name: "主账号", secret: "plan-secret-8F2A")

        XCTAssertEqual(saved.name, "主账号")
        XCTAssertEqual(saved.keySuffix, "8F2A")
        let persistedData = try XCTUnwrap(context.defaults.data(forKey: "keyConfigurations"))
        let persistedText = try XCTUnwrap(String(data: persistedData, encoding: .utf8))
        XCTAssertFalse(persistedText.contains("plan-secret"))
        XCTAssert密钥相等(try context.keychain.read(for: saved.id), "plan-secret-8F2A")
    }

    func test添加配置会去除名称首尾空白() throws {
        let context = makeContext()
        defer { context.cleanUp() }

        let saved = try context.repository.add(name: "  主账号\n", secret: "plan-key-1234")

        XCTAssertEqual(saved.name, "主账号")
    }

    func test添加配置拒绝空名称() {
        let context = makeContext()
        defer { context.cleanUp() }

        XCTAssertThrowsError(try context.repository.add(name: " \n ", secret: "plan-key-1234")) { error in
            XCTAssertEqual(error as? KeyRepositoryError, .invalidName)
        }
        XCTAssertTrue(context.repository.list().isEmpty)
    }

    func test添加配置拒绝非Plan前缀密钥() {
        let context = makeContext()
        defer { context.cleanUp() }

        XCTAssertThrowsError(try context.repository.add(name: "主账号", secret: "invalid-key-1234")) { error in
            XCTAssertEqual(error as? KeyRepositoryError, .invalidSecret)
        }
        XCTAssertTrue(context.repository.list().isEmpty)
    }

    func test添加配置拒绝匹配Plan秘密的显示名称且偏好设置不落秘密() {
        let context = makeContext()
        defer { context.cleanUp() }
        let secret = "plan-secret-8F2A"

        for name in [secret, "plan-other-name"] {
            XCTAssertThrowsError(try context.repository.add(name: name, secret: secret)) { error in
                XCTAssertEqual(error as? KeyRepositoryError, .invalidName)
                XCTAssertEqual(error.localizedDescription, "显示名称不能是 plan Key")
            }
        }

        XCTAssertTrue(context.repository.list().isEmpty)
        let persisted = context.defaults.data(forKey: "keyConfigurations")
            .flatMap { String(data: $0, encoding: .utf8) } ?? ""
        XCTAssertFalse(persisted.contains(secret))
    }

    func test短于四位的Key内容被拒绝且不会写入后缀元数据() {
        let context = makeContext()
        defer { context.cleanUp() }
        let secret = "plan-abc"

        XCTAssertThrowsError(try context.repository.add(name: "主账号", secret: secret)) { error in
            XCTAssertEqual(error as? KeyRepositoryError, .invalidSecret)
            XCTAssertEqual(error.localizedDescription, "plan Key 内容至少需要 4 位")
        }

        let persisted = context.defaults.data(forKey: "keyConfigurations")
            .flatMap { String(data: $0, encoding: .utf8) } ?? ""
        XCTAssertFalse(persisted.contains("abc"))
        XCTAssertTrue(context.repository.list().isEmpty)
    }

    func test启动迁移清洗真实旧短Key后缀且偏好设置不保留可还原片段() throws {
        let legacyValues = [
            (secret: "plan-a", suffix: "an-a"),
            (secret: "plan-ab", suffix: "n-ab"),
            (secret: "plan-abc", suffix: "-abc")
        ]

        for legacy in legacyValues {
            let id = UUID()
            let context = try makeLegacyContext(
                configuration: KeyConfiguration(
                    id: id,
                    name: "旧账号",
                    keySuffix: legacy.suffix,
                    sortOrder: 0
                ),
                secret: legacy.secret
            )
            defer { context.cleanUp() }

            let configuration = try XCTUnwrap(context.repository.list().first)
            XCTAssertEqual(configuration.keySuffix, "")
            XCTAssertEqual(KeyDisplayMask.masked(suffix: configuration.keySuffix), "plan-••••")

            let persistedData = try XCTUnwrap(
                context.defaults.data(forKey: "keyConfigurations")
            )
            let persistedText = try XCTUnwrap(String(data: persistedData, encoding: .utf8))
            XCTAssertFalse(persistedText.contains(legacy.suffix))
            XCTAssertFalse(persistedText.contains(legacy.secret))
            XCTAssert密钥相等(try context.keychain.read(for: id), legacy.secret)
        }
    }

    func test启动迁移清洗完整Key旧名称且通知不包含秘密() throws {
        let secret = "plan-sensitive-8F2A"
        let id = UUID()
        let context = try makeLegacyContext(
            configuration: KeyConfiguration(
                id: id,
                name: secret,
                keySuffix: "8F2A",
                sortOrder: 0
            ),
            secret: secret
        )
        defer { context.cleanUp() }

        let configuration = try XCTUnwrap(context.repository.list().first)
        XCTAssertEqual(configuration.name, "未命名 Key")
        XCTAssertEqual(configuration.displayName, "未命名 Key")
        let persistedData = try XCTUnwrap(context.defaults.data(forKey: "keyConfigurations"))
        let persistedText = try XCTUnwrap(String(data: persistedData, encoding: .utf8))
        XCTAssertFalse(persistedText.contains(secret))

        let snapshot = UsageSnapshot(
            planName: "Token Pack",
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: UsageMetric(
                used: 80,
                limit: 100,
                remaining: 20,
                percent: 80,
                unit: .token,
                windowEnd: nil
            ),
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 10_000)
        )
        let evaluator = AlertEvaluator(
            defaults: context.defaults,
            deliveryCoordinator: AlertDeliveryCoordinator()
        )
        let alert = try XCTUnwrap(evaluator.evaluate(
            key: configuration,
            snapshot: snapshot,
            thresholds: .init()
        ).first)
        XCTAssertFalse(alert.notificationBody().contains(secret))
        XCTAssertTrue(alert.notificationBody().contains("未命名 Key"))
    }

    func test更新配置同步更新名称后缀与Keychain() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let saved = try context.repository.add(name: "旧名称", secret: "plan-key-1234")

        let updated = try context.repository.update(
            id: saved.id,
            name: "  新名称 ",
            secret: "plan-updated-9ABC"
        )

        XCTAssertEqual(updated.id, saved.id)
        XCTAssertEqual(updated.name, "新名称")
        XCTAssertEqual(updated.keySuffix, "9ABC")
        XCTAssertEqual(updated.sortOrder, saved.sortOrder)
        XCTAssert密钥相等(try context.keychain.read(for: saved.id), "plan-updated-9ABC")
        XCTAssertEqual(context.repository.list(), [updated])
    }

    func test更新不存在的配置会被拒绝且不保存密钥() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let missingID = UUID()

        XCTAssertThrowsError(
            try context.repository.update(id: missingID, name: "主账号", secret: "plan-key-1234")
        ) { error in
            XCTAssertEqual(error as? KeyRepositoryError, .configurationNotFound)
        }
        XCTAssert密钥相等(try context.keychain.read(for: missingID), nil)
    }

    func test移动配置后排序会持久化() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let first = try context.repository.add(name: "一", secret: "plan-key-0001")
        let second = try context.repository.add(name: "二", secret: "plan-key-0002")
        let third = try context.repository.add(name: "三", secret: "plan-key-0003")

        context.repository.move(fromOffsets: IndexSet(integer: 0), toOffset: 3)

        let reloaded = KeyRepository(defaults: context.defaults, keychain: context.keychain).list()
        XCTAssertEqual(reloaded.map(\.id), [second.id, third.id, first.id])
        XCTAssertEqual(reloaded.map(\.sortOrder), [0, 1, 2])
    }

    func test删除配置同步清除Keychain并重排剩余配置() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let first = try context.repository.add(name: "一", secret: "plan-key-0001")
        let second = try context.repository.add(name: "二", secret: "plan-key-0002")

        try context.repository.delete(id: first.id)

        XCTAssert密钥相等(try context.keychain.read(for: first.id), nil)
        XCTAssertEqual(context.repository.list(), [
            KeyConfiguration(id: second.id, name: "二", keySuffix: "0002", sortOrder: 0)
        ])
    }

    private func makeContext() -> RepositoryTestContext {
        let suiteName = "ai.routin.usage-monitor.repository-tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.removePersistentDomain(forName: suiteName)
        let keychain = InMemoryKeychainStore()
        return RepositoryTestContext(
            suiteName: suiteName,
            defaults: defaults,
            keychain: keychain,
            repository: KeyRepository(defaults: defaults, keychain: keychain)
        )
    }

    private func makeLegacyContext(
        configuration: KeyConfiguration,
        secret: String?
    ) throws -> RepositoryTestContext {
        let suiteName = "ai.routin.usage-monitor.repository-legacy-tests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        let keychain = InMemoryKeychainStore()
        if let secret {
            try keychain.save(secret, for: configuration.id)
        }
        defaults.set(
            try JSONEncoder().encode([configuration]),
            forKey: "keyConfigurations"
        )
        return RepositoryTestContext(
            suiteName: suiteName,
            defaults: defaults,
            keychain: keychain,
            repository: KeyRepository(defaults: defaults, keychain: keychain)
        )
    }
}

private struct RepositoryTestContext {
    let suiteName: String
    let defaults: UserDefaults
    let keychain: InMemoryKeychainStore
    let repository: KeyRepository

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private final class InMemoryKeychainStore: KeychainStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var secrets: [UUID: String] = [:]

    func save(_ secret: String, for id: UUID) throws {
        lock.withLock {
            secrets[id] = secret
        }
    }

    func read(for id: UUID) throws -> String? {
        lock.withLock {
            secrets[id]
        }
    }

    func delete(for id: UUID) throws {
        _ = lock.withLock {
            secrets.removeValue(forKey: id)
        }
    }
}

func XCTAssert密钥相等(
    _ actual: String?,
    _ expected: String?,
    file: StaticString = #filePath,
    line: UInt = #line
) {
    guard actual == expected else {
        XCTFail("Keychain 密钥状态与预期不一致", file: file, line: line)
        return
    }
}
