import Foundation
import XCTest
@testable import RoutinUsage

final class LocalKeyStoreTests: XCTestCase {
    func test不存在的Key读取为空() throws {
        let context = try makeContext()
        defer { context.cleanUp() }

        XCTAssertNil(try context.store.read(for: UUID()))
    }

    func test保存Key后可以读取完整内容() throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let id = UUID()

        try context.store.save("plan-local-8F2A", for: id)

        XCTAssertEqual(try context.store.read(for: id), "plan-local-8F2A")
    }

    func test保存相同标识的Key会覆盖旧内容() throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let id = UUID()

        try context.store.save("plan-old-8F2A", for: id)
        try context.store.save("plan-new-9ABC", for: id)

        XCTAssertEqual(try context.store.read(for: id), "plan-new-9ABC")
    }

    func test删除Key后读取为空() throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let id = UUID()
        try context.store.save("plan-delete-8F2A", for: id)

        try context.store.delete(for: id)

        XCTAssertNil(try context.store.read(for: id))
    }
}

private struct LocalKeyStoreTestContext {
    let suiteName: String
    let store: LocalKeyStore
    let defaults: UserDefaults

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private func makeContext() throws -> LocalKeyStoreTestContext {
    let suiteName = "ai.routin.usage-monitor.local-key-store-tests.\(UUID().uuidString)"
    let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
    defaults.removePersistentDomain(forName: suiteName)
    return LocalKeyStoreTestContext(
        suiteName: suiteName,
        store: LocalKeyStore(defaults: defaults),
        defaults: defaults
    )
}

func XCTAssert密钥相等(
    _ actual: String?,
    _ expected: String?,
    file: StaticString = #filePath,
    line: UInt = #line
) {
    guard actual == expected else {
        XCTFail("密钥状态与预期不一致", file: file, line: line)
        return
    }
}
