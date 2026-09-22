import Foundation
import SwiftUI

struct ConfigurationBackupSettings: Codable, Equatable, Sendable {
    var refreshMinutes: Int
    var displayDimension: DisplayDimension
    var usageCardDensity: UsageCardDensity
    var menuBarStyle: MenuBarStyle
    var notificationsEnabled: Bool
    var thresholds: AlertThresholds
    var menuBarColorRules: MenuBarColorRules
    var launchAtLogin: Bool
    var updateChannel: UpdateChannel
    var updateCDNBase: String
    var displayOrder: CredentialDisplayOrder
    var credentialUsagePreferences: [String: CredentialUsagePreferences]

    private enum CodingKeys: String, CodingKey {
        case refreshMinutes
        case displayDimension
        case usageCardDensity
        case menuBarStyle
        case notificationsEnabled
        case thresholds
        case menuBarColorRules
        case launchAtLogin
        case updateChannel
        case updateCDNBase
        case displayOrder
        case credentialUsagePreferences
    }

    init(
        refreshMinutes: Int,
        displayDimension: DisplayDimension,
        usageCardDensity: UsageCardDensity,
        menuBarStyle: MenuBarStyle,
        notificationsEnabled: Bool,
        thresholds: AlertThresholds,
        menuBarColorRules: MenuBarColorRules,
        launchAtLogin: Bool,
        updateChannel: UpdateChannel,
        updateCDNBase: String,
        displayOrder: CredentialDisplayOrder,
        credentialUsagePreferences: [String: CredentialUsagePreferences]
    ) {
        self.refreshMinutes = refreshMinutes
        self.displayDimension = displayDimension
        self.usageCardDensity = usageCardDensity
        self.menuBarStyle = menuBarStyle
        self.notificationsEnabled = notificationsEnabled
        self.thresholds = thresholds
        self.menuBarColorRules = menuBarColorRules
        self.launchAtLogin = launchAtLogin
        self.updateChannel = updateChannel
        self.updateCDNBase = updateCDNBase
        self.displayOrder = displayOrder
        self.credentialUsagePreferences = credentialUsagePreferences
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        refreshMinutes = try container.decode(Int.self, forKey: .refreshMinutes)
        displayDimension = try container.decode(DisplayDimension.self, forKey: .displayDimension)
        usageCardDensity = try container.decodeIfPresent(
            UsageCardDensity.self,
            forKey: .usageCardDensity
        ) ?? .full
        menuBarStyle = try container.decode(MenuBarStyle.self, forKey: .menuBarStyle)
        notificationsEnabled = try container.decode(Bool.self, forKey: .notificationsEnabled)
        thresholds = try container.decode(AlertThresholds.self, forKey: .thresholds)
        menuBarColorRules = try container.decodeIfPresent(
            MenuBarColorRules.self,
            forKey: .menuBarColorRules
        ) ?? .standard
        launchAtLogin = try container.decode(Bool.self, forKey: .launchAtLogin)
        updateChannel = try container.decodeIfPresent(
            UpdateChannel.self,
            forKey: .updateChannel
        ) ?? .direct
        updateCDNBase = try container.decodeIfPresent(
            String.self,
            forKey: .updateCDNBase
        ) ?? AppSettings.cdnBases[0]
        displayOrder = try container.decode(CredentialDisplayOrder.self, forKey: .displayOrder)
        credentialUsagePreferences = try container.decode(
            [String: CredentialUsagePreferences].self,
            forKey: .credentialUsagePreferences
        )
    }
}

struct ConfigurationBackupCredential: Codable, Equatable, Identifiable, Sendable {
    var id: UUID
    var name: String
    var secret: String
    var sortOrder: Int
    var isEnabled: Bool
    var providerID: ProviderID
    var credentialKind: CredentialKind
    var metadata: [String: String]

    var configuration: KeyConfiguration {
        KeyConfiguration(
            id: id,
            name: name,
            keySuffix: KeyCredentialPolicy.metadataSuffix(for: secret),
            sortOrder: sortOrder,
            isEnabled: isEnabled,
            providerID: providerID,
            credentialKind: credentialKind,
            metadata: metadata
        )
    }

    func validate() throws {
        _ = try KeyRepository.validateConfiguration(
            name: name,
            secret: secret,
            providerID: providerID
        )
    }
}

struct ConfigurationBackupV1: Codable, Equatable, Sendable {
    static let schemaVersion = 1

    var schemaVersion: Int
    var exportedAt: Date
    var settings: ConfigurationBackupSettings
    var credentials: [ConfigurationBackupCredential]
}

enum ConfigurationBackupError: LocalizedError, Equatable {
    case unsupportedSchema
    case duplicateCredentialID
    case invalidCredential(UUID)
    case noCredentials

    var errorDescription: String? {
        switch self {
        case .unsupportedSchema:
            return "备份文件版本不受支持"
        case .duplicateCredentialID:
            return "备份文件中的凭证 ID 重复"
        case .invalidCredential(let id):
            return "备份文件中的凭证无效（\(id.uuidString.prefix(8))…）"
        case .noCredentials:
            return "备份文件中没有可导入的凭证"
        }
    }
}

enum ConfigurationBackupService {
    private static let metadataAllowlist: Set<String> = [
        "accessKeyID",
        "balanceWarningThreshold",
        "baseURL",
        "region",
        "planType",
        "usageKind",
        "userID",
        "websiteURL"
    ]

    private static var encoder: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return encoder
    }

    private static var decoder: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }

    @MainActor
    static func makeBackup(
        settings: AppSettings,
        credentials: [KeyConfiguration],
        secretReader: (UUID) -> String?,
        exportedAt: Date = .now
    ) throws -> ConfigurationBackupV1 {
        let backupCredentials = try credentials.map { configuration -> ConfigurationBackupCredential in
            guard let secret = secretReader(configuration.id), !secret.isEmpty else {
                throw ConfigurationBackupError.invalidCredential(configuration.id)
            }
            return ConfigurationBackupCredential(
                id: configuration.id,
                name: configuration.name,
                secret: secret,
                sortOrder: configuration.sortOrder,
                isEnabled: configuration.isEnabled,
                providerID: configuration.providerID,
                credentialKind: configuration.credentialKind,
                metadata: configuration.metadata.filter { metadataAllowlist.contains($0.key) }
            )
        }

        return ConfigurationBackupV1(
            schemaVersion: ConfigurationBackupV1.schemaVersion,
            exportedAt: exportedAt,
            settings: settings.backupSettings,
            credentials: backupCredentials
        )
    }

    static func data(for backup: ConfigurationBackupV1) throws -> Data {
        try encoder.encode(backup)
    }

    static func backup(from data: Data) throws -> ConfigurationBackupV1 {
        let backup = try decoder.decode(ConfigurationBackupV1.self, from: data)
        guard backup.schemaVersion == ConfigurationBackupV1.schemaVersion else {
            throw ConfigurationBackupError.unsupportedSchema
        }
        let ids = backup.credentials.map(\.id)
        guard Set(ids).count == ids.count else {
            throw ConfigurationBackupError.duplicateCredentialID
        }
        for credential in backup.credentials {
            do {
                try credential.validate()
            } catch {
                throw ConfigurationBackupError.invalidCredential(credential.id)
            }
        }
        let idSet = Set(ids)
        var result = backup
        result.credentials = backup.credentials.enumerated().map { index, credential in
            var normalized = credential
            normalized.sortOrder = index
            normalized.metadata = credential.metadata.filter { metadataAllowlist.contains($0.key) }
            return normalized
        }
        result.settings.displayOrder = CredentialDisplayOrder(
            menuBarCredentialIDs: backup.settings.displayOrder.menuBarCredentialIDs.filter(idSet.contains),
            popoverCredentialIDs: backup.settings.displayOrder.popoverCredentialIDs.filter(idSet.contains)
        )
        result.settings.credentialUsagePreferences = backup.settings.credentialUsagePreferences
            .filter { id, _ in UUID(uuidString: id).map(idSet.contains) == true }
        return result
    }
}
