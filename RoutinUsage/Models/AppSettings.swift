import Foundation
import Observation

@Observable
final class AppSettings {
    static let allowedRefreshMinutes = [1, 5, 15, 30]

    @ObservationIgnored private let defaults: UserDefaults

    var refreshMinutes: Int {
        didSet {
            guard Self.allowedRefreshMinutes.contains(refreshMinutes) else {
                refreshMinutes = oldValue
                return
            }
            defaults.set(refreshMinutes, forKey: Keys.refreshMinutes)
        }
    }

    var displayDimension: DisplayDimension {
        didSet {
            defaults.set(displayDimension.rawValue, forKey: Keys.displayDimension)
        }
    }

    var notificationsEnabled: Bool {
        didSet {
            defaults.set(notificationsEnabled, forKey: Keys.notificationsEnabled)
        }
    }

    var thresholds: AlertThresholds {
        didSet {
            defaults.set(thresholds.low, forKey: Keys.lowThreshold)
            defaults.set(thresholds.high, forKey: Keys.highThreshold)
        }
    }

    var launchAtLogin: Bool {
        didSet {
            defaults.set(launchAtLogin, forKey: Keys.launchAtLogin)
        }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        let storedRefreshMinutes = defaults.object(forKey: Keys.refreshMinutes) as? Int
        if let storedRefreshMinutes,
           Self.allowedRefreshMinutes.contains(storedRefreshMinutes) {
            refreshMinutes = storedRefreshMinutes
        } else {
            refreshMinutes = 5
        }

        let storedDimension = defaults.string(forKey: Keys.displayDimension)
            .flatMap(DisplayDimension.init(rawValue:))
        displayDimension = storedDimension ?? .fiveHour

        if defaults.object(forKey: Keys.notificationsEnabled) == nil {
            notificationsEnabled = true
        } else {
            notificationsEnabled = defaults.bool(forKey: Keys.notificationsEnabled)
        }

        let low = defaults.object(forKey: Keys.lowThreshold) as? Int
        let high = defaults.object(forKey: Keys.highThreshold) as? Int
        if let low, let high, AlertThresholds.isValid(low: low, high: high) {
            thresholds = AlertThresholds(low: low, high: high)
        } else {
            thresholds = AlertThresholds()
        }

        launchAtLogin = defaults.bool(forKey: Keys.launchAtLogin)
    }
}

private extension AppSettings {
    enum Keys {
        static let refreshMinutes = "refreshMinutes"
        static let displayDimension = "displayDimension"
        static let notificationsEnabled = "notificationsEnabled"
        static let lowThreshold = "notificationLowThreshold"
        static let highThreshold = "notificationHighThreshold"
        static let launchAtLogin = "launchAtLogin"
    }
}
