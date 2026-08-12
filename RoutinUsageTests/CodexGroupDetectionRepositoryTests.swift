import Foundation
import XCTest
@testable import RoutinUsage

final class CodexGroupDetectionRepositoryTests: XCTestCase {
    func test规范化邮箱生成稳定摘要且不保留原文() {
        let first = RoutinAccountIdentity.make(
            email: "  TEST@Example.COM ",
            displayName: "测试账号"
        )
        let second = RoutinAccountIdentity.make(
            email: "test@example.com",
            displayName: "另一个显示名"
        )

        XCTAssertEqual(first.fingerprint, second.fingerprint)
        XCTAssertFalse(first.fingerprint.contains("test@example.com"))
        XCTAssertEqual(first.displayName, "测试账号")
    }

    func test记录按Key隔离保存读取和删除且持久化内容不含邮箱() throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let firstKeyID = UUID()
        let secondKeyID = UUID()
        let identity = RoutinAccountIdentity.make(
            email: "private@example.com",
            displayName: "测试账号"
        )
        let record = CodexGroupDetectionRecord(
            keyID: firstKeyID,
            accountFingerprint: identity.fingerprint,
            accountDisplayName: identity.displayName,
            groupName: "Codex",
            detectedAt: Date(timeIntervalSince1970: 1_786_400_000)
        )

        try context.repository.save(record)

        XCTAssertEqual(try context.repository.load(for: firstKeyID), record)
        XCTAssertNil(try context.repository.load(for: secondKeyID))
        let persisted = try XCTUnwrap(context.defaults.data(forKey: context.storageKey))
        let text = try XCTUnwrap(String(data: persisted, encoding: .utf8))
        XCTAssertFalse(text.contains("private@example.com"))

        try context.repository.delete(for: firstKeyID)

        XCTAssertNil(try context.repository.load(for: firstKeyID))
        XCTAssertNil(context.defaults.data(forKey: context.storageKey))
    }
}

private struct CodexGroupDetectionRepositoryTestContext {
    let suiteName: String
    let storageKey: String
    let defaults: UserDefaults
    let repository: CodexGroupDetectionRepository

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private func makeContext() throws -> CodexGroupDetectionRepositoryTestContext {
    let suiteName = "ai.routin.usage-monitor.codex-group-detection-tests.\(UUID().uuidString)"
    let storageKey = "codexGroupDetectionRecords"
    let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
    defaults.removePersistentDomain(forName: suiteName)
    return CodexGroupDetectionRepositoryTestContext(
        suiteName: suiteName,
        storageKey: storageKey,
        defaults: defaults,
        repository: CodexGroupDetectionRepository(defaults: defaults, storageKey: storageKey)
    )
}
