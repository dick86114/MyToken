import XCTest
@testable import RoutinUsage

final class UserDefaultsMigrationTests: XCTestCase {
    func test旧应用偏好迁移到新应用身份() throws {
        let currentSuite = "migration-current-\(UUID().uuidString)"
        let legacySuite = "migration-legacy-\(UUID().uuidString)"
        let current = try XCTUnwrap(UserDefaults(suiteName: currentSuite))
        let legacy = try XCTUnwrap(UserDefaults(suiteName: legacySuite))
        defer {
            current.removePersistentDomain(forName: currentSuite)
            legacy.removePersistentDomain(forName: legacySuite)
        }

        legacy.setPersistentDomain(
            ["keyConfigurations": "old", "selectedCredentialIDs": ["account"]],
            forName: legacySuite
        )

        UserDefaultsMigration.migrateLegacyBundlePreferences(
            standard: current,
            legacy: legacy,
            currentDomain: currentSuite,
            legacyDomain: legacySuite
        )

        XCTAssertEqual(current.string(forKey: "keyConfigurations"), "old")
        XCTAssertEqual(current.stringArray(forKey: "selectedCredentialIDs"), ["account"])
        XCTAssertTrue(current.bool(forKey: "didMigrateLegacyBundlePreferences"))
    }
}
