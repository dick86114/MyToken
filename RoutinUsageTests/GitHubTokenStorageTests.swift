import XCTest
@testable import RoutinUsage

final class GitHubTokenStorageTests: XCTestCase {
    func testGitHub令牌保存读取移除和掩码使用应用本地存储() throws {
        let (defaults, suiteName) = Self.cleanDefaults()
        addTeardownBlock {
            defaults.removePersistentDomain(forName: suiteName)
        }
        let store = AppLocalGitHubTokenStore(defaults: defaults)

        XCTAssertNil(store.readToken())
        XCTAssertNil(store.tokenMask())

        try store.saveToken("  ghp_1234567890abcd  ")

        XCTAssertEqual(store.readToken(), "ghp_1234567890abcd")
        XCTAssertEqual(store.tokenMask(), "ghp_…abcd")

        store.removeToken()

        XCTAssertNil(store.readToken())
        XCTAssertNil(store.tokenMask())
    }

    func testGitHub令牌保存空白值时会移除已有令牌() throws {
        let (defaults, suiteName) = Self.cleanDefaults()
        addTeardownBlock {
            defaults.removePersistentDomain(forName: suiteName)
        }
        let store = AppLocalGitHubTokenStore(defaults: defaults)

        try store.saveToken("ghp_1234567890abcd")
        try store.saveToken("   ")

        XCTAssertNil(store.readToken())
        XCTAssertNil(store.tokenMask())
    }

    func testGitHub令牌与启动路径不访问钥匙串() throws {
        let tokenSource = try TestSourceReader.read([
            "RoutinUsage", "Security", "GitHubTokenStore.swift"
        ])
        let appSource = try TestSourceReader.read([
            "RoutinUsage", "App", "AppEnvironment.swift"
        ])

        XCTAssertTrue(tokenSource.contains("AppLocalGitHubTokenStore"))
        XCTAssertFalse(tokenSource.contains("Keychain"))
        XCTAssertFalse(tokenSource.contains("SecItem"))
        XCTAssertFalse(appSource.contains("KeychainGitHubTokenStore"))
        XCTAssertFalse(appSource.contains("KeychainSecretStore"))
        XCTAssertFalse(appSource.contains("KeychainMigration"))
        XCTAssertFalse(appSource.contains("SecItem"))
    }

    private static func cleanDefaults() -> (UserDefaults, String) {
        let suiteName = "GitHubTokenStorageTests.\(UUID().uuidString)"
        return (UserDefaults(suiteName: suiteName)!, suiteName)
    }
}
