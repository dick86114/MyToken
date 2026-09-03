import XCTest
@testable import RoutinUsage

final class SettingsProviderGroupingTests: XCTestCase {
    func test凭证配置支持非Routin供应商并保留元数据() throws {
        let suite = "provider-grouping-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defaults.removePersistentDomain(forName: suite)
        defer { defaults.removePersistentDomain(forName: suite) }
        let repository = KeyRepository(defaults: defaults, localStore: LocalKeyStore(defaults: defaults))

        let configuration = try repository.add(
            name: "DeepSeek",
            secret: "sk-test",
            providerID: .deepseek,
            credentialKind: .apiKey,
            metadata: ["balanceWarningThreshold": "10"]
        )

        XCTAssertEqual(configuration.providerID, .deepseek)
        XCTAssertEqual(configuration.credentialKind, .apiKey)
        XCTAssertEqual(repository.list().first?.metadata["balanceWarningThreshold"], "10")
    }

    func test菜单栏选择最多五个且去重() {
        let suite = "provider-selection-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        defer { defaults.removePersistentDomain(forName: suite) }
        let settings = AppSettings(defaults: defaults)
        let ids = (0..<6).map { _ in UUID() }

        settings.selectedCredentialIDs = [ids[0], ids[1], ids[0], ids[2], ids[3], ids[4]]

        XCTAssertEqual(
            settings.selectedCredentialIDs,
            [ids[0], ids[1], ids[2], ids[3], ids[4]]
        )
        XCTAssertEqual(defaults.stringArray(forKey: "selectedCredentialIDs"), [
            ids[0].uuidString,
            ids[1].uuidString,
            ids[2].uuidString,
            ids[3].uuidString,
            ids[4].uuidString
        ])
    }
}
