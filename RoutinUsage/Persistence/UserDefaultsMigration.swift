import Foundation

enum UserDefaultsMigration {
    static let legacyBundleIdentifier = "ai.routin.usage-monitor"
    static let currentBundleIdentifier = "ai.routin.myroutin"
    private static let markerKey = "didMigrateLegacyBundlePreferences"

    static func migrateLegacyBundlePreferences(
        standard: UserDefaults = .standard,
        legacy: UserDefaults? = nil,
        currentDomain: String = currentBundleIdentifier,
        legacyDomain: String = legacyBundleIdentifier
    ) {
        guard !standard.bool(forKey: markerKey) else {
            return
        }

        let legacyDefaults = legacy ?? UserDefaults(suiteName: legacyBundleIdentifier)
        let oldValues = legacyDefaults?.persistentDomain(forName: legacyDomain) ?? [:]
        var currentValues = standard.persistentDomain(forName: currentDomain) ?? [:]

        for (key, value) in oldValues where currentValues[key] == nil {
            currentValues[key] = value
        }
        currentValues[markerKey] = true
        standard.setPersistentDomain(currentValues, forName: currentDomain)
    }
}
