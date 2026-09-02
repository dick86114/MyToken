import XCTest
@testable import RoutinUsage

final class KeychainMigrationTests: XCTestCase {
    func test旧Routin密钥迁移成功后删除旧值() throws {
        let id = UUID()
        let old = TestSecretStore(values: [id: "plan-secret-1234"])
        let new = TestSecretStore()

        try KeychainMigration.migrate(
            ids: [id],
            from: old,
            to: new
        )

        XCTAssertNil(try old.read(for: id))
        XCTAssertEqual(try new.read(for: id), "plan-secret-1234")
    }

    func test新存储失败时保留旧值() {
        let id = UUID()
        let old = TestSecretStore(values: [id: "plan-secret-1234"])
        let new = TestSecretStore(failingOnSave: true)

        XCTAssertThrowsError(
            try KeychainMigration.migrate(ids: [id], from: old, to: new)
        )
        XCTAssertEqual(try? old.read(for: id), "plan-secret-1234")
        XCTAssertNil(try? new.read(for: id))
    }
}

private final class TestSecretStore: LocalKeyStoring, @unchecked Sendable {
    private var values: [UUID: String]
    private let failingOnSave: Bool

    init(values: [UUID: String] = [:], failingOnSave: Bool = false) {
        self.values = values
        self.failingOnSave = failingOnSave
    }

    func save(_ secret: String, for id: UUID) throws {
        if failingOnSave {
            throw TestSecretStoreError.saveFailed
        }
        values[id] = secret
    }

    func read(for id: UUID) throws -> String? { values[id] }

    func delete(for id: UUID) throws { values.removeValue(forKey: id) }
}

private enum TestSecretStoreError: Error {
    case saveFailed
}
