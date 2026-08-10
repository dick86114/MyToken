import Foundation
import XCTest
@testable import RoutinUsage

final class KeychainStoreTests: XCTestCase {
    func test测试清理器会传播Keychain删除错误() {
        let store = DeleteFailingKeychainStore()

        XCTAssertThrowsError(try cleanKeychainTestItem(using: store, id: UUID())) { error in
            XCTAssertEqual(error as? KeychainCleanupTestError, .deleteFailed)
        }
    }

    func test保存读取并覆盖隔离的Keychain项目() throws {
        let service = "ai.routin.usage-monitor.keychain-tests.\(UUID().uuidString)"
        let accountID = UUID()
        let store = KeychainStore(service: service)
        addTeardownBlock {
            try cleanKeychainTestItem(using: store, id: accountID)
        }

        try store.save("plan-first-1234", for: accountID)
        XCTAssert密钥相等(try store.read(for: accountID), "plan-first-1234")

        try store.save("plan-updated-5678", for: accountID)
        XCTAssert密钥相等(try store.read(for: accountID), "plan-updated-5678")
    }

    func test删除隔离的Keychain项目后读取为空() throws {
        let service = "ai.routin.usage-monitor.keychain-tests.\(UUID().uuidString)"
        let accountID = UUID()
        let store = KeychainStore(service: service)
        addTeardownBlock {
            try cleanKeychainTestItem(using: store, id: accountID)
        }
        try store.save("plan-delete-1234", for: accountID)

        try store.delete(for: accountID)

        XCTAssert密钥相等(try store.read(for: accountID), nil)
    }
}

private func cleanKeychainTestItem(using store: any KeychainStoring, id: UUID) throws {
    try store.delete(for: id)
}

private enum KeychainCleanupTestError: Error, Equatable {
    case deleteFailed
}

private struct DeleteFailingKeychainStore: KeychainStoring {
    func save(_ secret: String, for id: UUID) throws {}

    func read(for id: UUID) throws -> String? {
        nil
    }

    func delete(for id: UUID) throws {
        throw KeychainCleanupTestError.deleteFailed
    }
}
