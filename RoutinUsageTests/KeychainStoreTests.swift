import Foundation
import XCTest
@testable import RoutinUsage

final class KeychainStoreTests: XCTestCase {
    func test保存读取并覆盖隔离的Keychain项目() throws {
        let service = "ai.routin.usage-monitor.keychain-tests.\(UUID().uuidString)"
        let accountID = UUID()
        let store = KeychainStore(service: service)
        defer { try? store.delete(for: accountID) }

        try store.save("plan-first-1234", for: accountID)
        XCTAssert密钥相等(try store.read(for: accountID), "plan-first-1234")

        try store.save("plan-updated-5678", for: accountID)
        XCTAssert密钥相等(try store.read(for: accountID), "plan-updated-5678")
    }

    func test删除隔离的Keychain项目后读取为空() throws {
        let service = "ai.routin.usage-monitor.keychain-tests.\(UUID().uuidString)"
        let accountID = UUID()
        let store = KeychainStore(service: service)
        defer { try? store.delete(for: accountID) }
        try store.save("plan-delete-1234", for: accountID)

        try store.delete(for: accountID)

        XCTAssert密钥相等(try store.read(for: accountID), nil)
    }
}
