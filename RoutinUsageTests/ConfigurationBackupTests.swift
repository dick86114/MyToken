import XCTest
@testable import RoutinUsage

final class ConfigurationBackupTests: XCTestCase {
    @MainActor
    func test配置备份往返保留凭证和偏好() throws {
        let context = try makeContext()
        let id = UUID()
        try context.repository.replaceAll(with: [
            makeImport(
                id: id,
                name: "工作机",
                secret: "sk-test",
                providerID: .deepseek
            ),
        ])
        let settings = AppSettings(defaults: context.defaults)
        settings.refreshMinutes = 30
        settings.displayDimension = .weekly
        var colorRules = settings.menuBarColorRules
        colorRules.warningThreshold = 35
        colorRules.criticalThreshold = 75
        settings.menuBarColorRules = colorRules
        settings.displayOrder = CredentialDisplayOrder(
            menuBarCredentialIDs: [id],
            popoverCredentialIDs: [id]
        )
        var preferences = CredentialUsagePreferences.defaultValue
        preferences.menuBarMetricID = "weekly"
        settings.setUsagePreferences(preferences, for: id)

        let backup = try ConfigurationBackupService.makeBackup(
            settings: settings,
            credentials: context.repository.list(),
            secretReader: { try? context.store.read(for: $0) }
        )
        let data = try ConfigurationBackupService.data(for: backup)
        let decoded = try ConfigurationBackupService.backup(from: data)

        XCTAssertEqual(decoded.schemaVersion, 1)
        XCTAssertEqual(decoded.credentials.map(\.id), [id])
        XCTAssertEqual(decoded.credentials.first?.secret, "sk-test")
        XCTAssertEqual(decoded.settings.refreshMinutes, 30)
        XCTAssertEqual(decoded.settings.displayDimension, .weekly)
        XCTAssertEqual(decoded.settings.menuBarColorRules, colorRules)
        XCTAssertEqual(decoded.settings.updateChannel, .direct)
        XCTAssertEqual(decoded.settings.displayOrder.menuBarCredentialIDs, [id])
        XCTAssertEqual(decoded.settings.credentialUsagePreferences[id.uuidString]?.menuBarMetricID, "weekly")
    }

    @MainActor
    func test配置备份保留更新通道和CDN源() throws {
        let context = try makeContext()
        let settings = AppSettings(defaults: context.defaults)
        settings.updateChannel = .cdn
        settings.updateCDNBase = AppSettings.cdnBases[2]

        let backup = try ConfigurationBackupService.makeBackup(
            settings: settings,
            credentials: context.repository.list(),
            secretReader: { _ in nil }
        )
        let decoded = try ConfigurationBackupService.backup(
            from: ConfigurationBackupService.data(for: backup)
        )

        XCTAssertEqual(decoded.settings.updateChannel, .cdn)
        XCTAssertEqual(decoded.settings.updateCDNBase, AppSettings.cdnBases[2])
    }

    @MainActor
    func test旧火山备份缺少AccessKeyID时仍按原样导入() throws {
        let context = try makeContext()
        let id = UUID()
        let backup = ConfigurationBackupV1(
            schemaVersion: ConfigurationBackupV1.schemaVersion,
            exportedAt: .now,
            settings: AppSettings(defaults: context.defaults).backupSettings,
            credentials: [
                ConfigurationBackupCredential(
                    id: id,
                    name: "火山方舟",
                    secret: "secret-access-key",
                    sortOrder: 0,
                    isEnabled: true,
                    providerID: .volcengine,
                    credentialKind: .accessKeyPair,
                    metadata: ["region": "cn-beijing", "planType": "agent"]
                )
            ]
        )

        let decoded = try ConfigurationBackupService.backup(
            from: ConfigurationBackupService.data(for: backup)
        )

        XCTAssertEqual(decoded.credentials.first?.id, id)
        XCTAssertEqual(decoded.credentials.first?.secret, "secret-access-key")
        XCTAssertEqual(decoded.credentials.first?.metadata["region"], "cn-beijing")
        XCTAssertNil(decoded.credentials.first?.metadata["accessKeyID"])
    }

    @MainActor
    func test配置备份保留火山方舟AccessKeyID和阈值元数据() throws {
        let context = try makeContext()
        let id = UUID()
        try context.repository.replaceAll(with: [
            KeyRepository.CredentialImport(
                configuration: KeyConfiguration(
                    id: id,
                    name: "Agent Plan",
                    keySuffix: "test",
                    sortOrder: 0,
                    providerID: .volcengine,
                    credentialKind: .accessKeyPair,
                    metadata: [
                        "accessKeyID": "AKLT-example",
                        "region": "cn-beijing",
                        "planType": "agent",
                        "balanceWarningThreshold": "12.5",
                        "internalUnsafeValue": "should-remove"
                    ]
                ),
                secret: "secret-access-key"
            )
        ])

        let backup = try ConfigurationBackupService.makeBackup(
            settings: AppSettings(defaults: context.defaults),
            credentials: context.repository.list(),
            secretReader: { try? context.store.read(for: $0) }
        )
        let decoded = try ConfigurationBackupService.backup(
            from: ConfigurationBackupService.data(for: backup)
        )

        XCTAssertEqual(decoded.credentials.first?.metadata["accessKeyID"], "AKLT-example")
        XCTAssertEqual(decoded.credentials.first?.metadata["region"], "cn-beijing")
        XCTAssertEqual(decoded.credentials.first?.metadata["planType"], "agent")
        XCTAssertEqual(decoded.credentials.first?.metadata["balanceWarningThreshold"], "12.5")
        XCTAssertNil(decoded.credentials.first?.metadata["internalUnsafeValue"])
    }

    @MainActor
    func test导入备份替换旧凭证并保留凭证ID() throws {
        let context = try makeContext()
        let oldConfiguration = try context.repository.add(name: "旧账号", secret: "plan-old1")

        let importedID = UUID()
        let configuration = KeyConfiguration(
            id: importedID,
            name: "新账号",
            keySuffix: "test",
            sortOrder: 0,
            isEnabled: false,
            providerID: .deepseek,
            credentialKind: .apiKey
        )
        try context.repository.replaceAll(with: [
            KeyRepository.CredentialImport(configuration: configuration, secret: "sk-imported"),
        ])

        let configurations = context.repository.list()
        XCTAssertEqual(configurations.map(\.id), [importedID])
        XCTAssertEqual(configurations.first?.name, "新账号")
        XCTAssertEqual(configurations.first?.isEnabled, false)
        XCTAssertEqual(configurations.first?.keySuffix, "")
        XCTAssertEqual(try context.repository.read(id: importedID), "sk-imported")
        XCTAssertNil(try? context.store.read(for: oldConfiguration.id))
    }

    private func makeContext() throws -> (defaults: UserDefaults, repository: KeyRepository, store: LocalKeyStore) {
        let suiteName = "ConfigurationBackupTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.removePersistentDomain(forName: suiteName)
        let store = LocalKeyStore(defaults: defaults)
        return (defaults, KeyRepository(defaults: defaults, localStore: store), store)
    }

    private func makeImport(
        id: UUID,
        name: String,
        secret: String,
        providerID: ProviderID
    ) -> KeyRepository.CredentialImport {
        KeyRepository.CredentialImport(
            configuration: KeyConfiguration(
                id: id,
                name: name,
                keySuffix: KeyCredentialPolicy.metadataSuffix(for: secret),
                sortOrder: 0,
                providerID: providerID,
                credentialKind: .apiKey
            ),
            secret: secret
        )
    }
}
