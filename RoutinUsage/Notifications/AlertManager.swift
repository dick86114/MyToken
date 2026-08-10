import Foundation
@preconcurrency import UserNotifications

struct AlertThresholds: Equatable, Sendable {
    let low: Int
    let high: Int

    init(low: Int = 80, high: Int = 95) {
        precondition(Self.isValid(low: low, high: high), "通知阈值必须位于 1...100，且低阈值小于高阈值")
        self.low = low
        self.high = high
    }

    static func isValid(low: Int, high: Int) -> Bool {
        (1...100).contains(low) && (1...100).contains(high) && low < high
    }
}

enum AlertLevel: String, Codable, Equatable, Sendable {
    case low
    case high
}

struct UsageAlert: Equatable, Sendable {
    let keyID: UUID
    let keyName: String
    let dimension: UsageDimension
    let level: AlertLevel
    let percent: Double
    let windowEnd: Date?
    fileprivate let reservationID: UUID
    fileprivate let triggeredWindows: Set<AlertWindowKey>

    var notificationTitle: String {
        "Routin 用量预警"
    }

    func notificationBody(timeZone: TimeZone = .autoupdatingCurrent) -> String {
        let base = "\(keyName) · \(dimension.notificationName)用量已达 \(formattedPercent)%"
        guard let windowEnd, dimension != .token else {
            return base
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = timeZone
        formatter.dateFormat = "HH:mm"
        return "\(base)，窗口将在 \(formatter.string(from: windowEnd)) 重置"
    }

    private var formattedPercent: String {
        let rounded = percent.rounded()
        if abs(percent - rounded) < 0.000_001 {
            return String(Int(rounded))
        }
        return String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), percent)
    }
}

private extension UsageDimension {
    var notificationName: String {
        switch self {
        case .fiveHour:
            return "5 小时"
        case .weekly:
            return "周"
        case .token:
            return "Token "
        }
    }
}

struct AlertWindowKey: Codable, Hashable, Sendable {
    let keyID: UUID
    let dimension: UsageDimension
    let windowIdentifier: String
    let threshold: Int
}

private struct AlertPeriodicWindowWatermark: Codable, Hashable, Sendable {
    let keyID: UUID
    let dimension: UsageDimension
    let windowIdentifier: String
}

private final class AlertEvaluatorSharedState: @unchecked Sendable {
    let lock = NSLock()
    var reservationOwners: [AlertWindowKey: UUID] = [:]
    var inFlightReservations: Set<UUID> = []
    var pendingResetWindows: [AlertWindowKey: UUID] = [:]
}

final class AlertEvaluator: @unchecked Sendable {
    private static let persistedKey = "usageAlertTriggeredWindows"
    private static let periodicWatermarksKey = "usageAlertLatestPeriodicWindows"
    private static let sharedState = AlertEvaluatorSharedState()

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func evaluate(
        key: KeyConfiguration,
        snapshot: UsageSnapshot,
        thresholds: AlertThresholds
    ) -> [UsageAlert] {
        Self.sharedState.lock.lock()
        defer { Self.sharedState.lock.unlock() }

        var triggeredWindows = Self.loadTriggeredWindows(from: defaults)
        var periodicWatermarks = Self.loadPeriodicWatermarks(from: defaults)
        var alerts: [UsageAlert] = []
        switch snapshot.kind {
        case .periodic:
            if let metric = snapshot.fiveHour, let windowEnd = metric.windowEnd {
                alerts += evaluate(
                    key: key,
                    metric: metric,
                    dimension: .fiveHour,
                    windowIdentifier: String(windowEnd.timeIntervalSince1970),
                    thresholds: thresholds,
                    clearsWhenBelowThreshold: false,
                    triggeredWindows: &triggeredWindows,
                    periodicWatermarks: &periodicWatermarks
                )
            }
            if let metric = snapshot.weekly, let windowEnd = metric.windowEnd {
                alerts += evaluate(
                    key: key,
                    metric: metric,
                    dimension: .weekly,
                    windowIdentifier: String(windowEnd.timeIntervalSince1970),
                    thresholds: thresholds,
                    clearsWhenBelowThreshold: false,
                    triggeredWindows: &triggeredWindows,
                    periodicWatermarks: &periodicWatermarks
                )
            }
        case .tokenPack:
            if let metric = snapshot.token {
                alerts += evaluate(
                    key: key,
                    metric: metric,
                    dimension: .token,
                    windowIdentifier: "token-pack",
                    thresholds: thresholds,
                    clearsWhenBelowThreshold: true,
                    triggeredWindows: &triggeredWindows,
                    periodicWatermarks: &periodicWatermarks
                )
            }
        }

        persistTriggeredWindows(triggeredWindows)
        persistPeriodicWatermarks(periodicWatermarks)
        return alerts
    }

    func restoreEligibility(for alerts: ArraySlice<UsageAlert>) {
        Self.sharedState.lock.lock()
        defer { Self.sharedState.lock.unlock() }

        var triggeredWindows = Self.loadTriggeredWindows(from: defaults)
        for alert in alerts {
            Self.sharedState.inFlightReservations.remove(alert.reservationID)
            for windowKey in alert.triggeredWindows
                where Self.sharedState.reservationOwners[windowKey] == alert.reservationID {
                triggeredWindows.remove(windowKey)
                Self.sharedState.reservationOwners.removeValue(forKey: windowKey)
                Self.sharedState.pendingResetWindows.removeValue(forKey: windowKey)
            }
        }
        persistTriggeredWindows(triggeredWindows)
    }

    func restoreEligibility(for alerts: [UsageAlert]) {
        restoreEligibility(for: alerts[...])
    }

    func beginDelivery(of alert: UsageAlert) -> Bool {
        Self.sharedState.lock.lock()
        defer { Self.sharedState.lock.unlock() }

        let triggeredWindows = Self.loadTriggeredWindows(from: defaults)
        let isCurrent = alert.triggeredWindows.allSatisfy { windowKey in
            triggeredWindows.contains(windowKey)
                && Self.sharedState.reservationOwners[windowKey] == alert.reservationID
        }
        guard isCurrent else {
            return false
        }

        Self.sharedState.inFlightReservations.insert(alert.reservationID)
        return true
    }

    func finishDelivery(of alert: UsageAlert) {
        Self.sharedState.lock.lock()
        defer { Self.sharedState.lock.unlock() }

        var triggeredWindows = Self.loadTriggeredWindows(from: defaults)
        Self.sharedState.inFlightReservations.remove(alert.reservationID)
        for windowKey in alert.triggeredWindows
            where Self.sharedState.reservationOwners[windowKey] == alert.reservationID {
            if Self.sharedState.pendingResetWindows[windowKey] == alert.reservationID {
                triggeredWindows.remove(windowKey)
                Self.sharedState.pendingResetWindows.removeValue(forKey: windowKey)
            }
            Self.sharedState.reservationOwners.removeValue(forKey: windowKey)
        }
        persistTriggeredWindows(triggeredWindows)
    }

    private func evaluate(
        key: KeyConfiguration,
        metric: UsageMetric,
        dimension: UsageDimension,
        windowIdentifier: String,
        thresholds: AlertThresholds,
        clearsWhenBelowThreshold: Bool,
        triggeredWindows: inout Set<AlertWindowKey>,
        periodicWatermarks: inout Set<AlertPeriodicWindowWatermark>
    ) -> [UsageAlert] {
        let levels: [(threshold: Int, level: AlertLevel)] = [
            (thresholds.low, .low),
            (thresholds.high, .high)
        ]

        guard prepareCurrentWindow(
            keyID: key.id,
            dimension: dimension,
            windowIdentifier: windowIdentifier,
            activeThresholds: Set(levels.map(\.threshold)),
            isPeriodic: !clearsWhenBelowThreshold,
            triggeredWindows: &triggeredWindows,
            periodicWatermarks: &periodicWatermarks
        ) else {
            return []
        }

        if clearsWhenBelowThreshold {
            for item in levels where metric.percent < Double(item.threshold) {
                let windowKey = AlertWindowKey(
                    keyID: key.id,
                    dimension: dimension,
                    windowIdentifier: windowIdentifier,
                    threshold: item.threshold
                )
                if
                    let reservationID = Self.sharedState.reservationOwners[windowKey],
                    Self.sharedState.inFlightReservations.contains(reservationID)
                {
                    Self.sharedState.pendingResetWindows[windowKey] = reservationID
                    continue
                }
                removeState(for: [windowKey], triggeredWindows: &triggeredWindows)
            }
        }

        let newlyReached = levels.filter { item in
            guard metric.percent >= Double(item.threshold) else {
                return false
            }
            let windowKey = AlertWindowKey(
                keyID: key.id,
                dimension: dimension,
                windowIdentifier: windowIdentifier,
                threshold: item.threshold
            )
            return !triggeredWindows.contains(windowKey)
        }

        guard let highest = newlyReached.last else {
            return []
        }

        let newlyTriggeredWindows = Set(newlyReached.map { item in
            AlertWindowKey(
                keyID: key.id,
                dimension: dimension,
                windowIdentifier: windowIdentifier,
                threshold: item.threshold
            )
        })
        let reservationID = UUID()
        triggeredWindows.formUnion(newlyTriggeredWindows)
        var reservedWindows = newlyTriggeredWindows
        for item in levels where item.threshold < highest.threshold {
            let lowerWindowKey = AlertWindowKey(
                keyID: key.id,
                dimension: dimension,
                windowIdentifier: windowIdentifier,
                threshold: item.threshold
            )
            if
                !newlyTriggeredWindows.contains(lowerWindowKey),
                Self.sharedState.reservationOwners[lowerWindowKey] != nil
            {
                reservedWindows.insert(lowerWindowKey)
            }
        }
        for windowKey in reservedWindows {
            Self.sharedState.reservationOwners[windowKey] = reservationID
            Self.sharedState.pendingResetWindows.removeValue(forKey: windowKey)
        }

        return [UsageAlert(
            keyID: key.id,
            keyName: key.name,
            dimension: dimension,
            level: highest.level,
            percent: metric.percent,
            windowEnd: metric.windowEnd,
            reservationID: reservationID,
            triggeredWindows: reservedWindows
        )]
    }

    private func prepareCurrentWindow(
        keyID: UUID,
        dimension: UsageDimension,
        windowIdentifier: String,
        activeThresholds: Set<Int>,
        isPeriodic: Bool,
        triggeredWindows: inout Set<AlertWindowKey>,
        periodicWatermarks: inout Set<AlertPeriodicWindowWatermark>
    ) -> Bool {
        let matchingKeys = triggeredWindows.filter {
            $0.keyID == keyID && $0.dimension == dimension
        }
        if isPeriodic, let currentWindow = Double(windowIdentifier) {
            let matchingWatermarks = periodicWatermarks.filter {
                $0.keyID == keyID && $0.dimension == dimension
            }
            let latestWindow = (
                matchingKeys.compactMap { Double($0.windowIdentifier) }
                    + matchingWatermarks.compactMap { Double($0.windowIdentifier) }
            ).max()
            if let latestWindow, currentWindow < latestWindow {
                return false
            }

            periodicWatermarks.subtract(matchingWatermarks)
            periodicWatermarks.insert(AlertPeriodicWindowWatermark(
                keyID: keyID,
                dimension: dimension,
                windowIdentifier: windowIdentifier
            ))
        }

        let obsoleteKeys = matchingKeys.filter {
            $0.windowIdentifier != windowIdentifier
                || !activeThresholds.contains($0.threshold)
        }
        removeState(for: obsoleteKeys, triggeredWindows: &triggeredWindows)
        return true
    }

    private func removeState(
        for windowKeys: some Sequence<AlertWindowKey>,
        triggeredWindows: inout Set<AlertWindowKey>
    ) {
        var affectedReservations: Set<UUID> = []
        for windowKey in windowKeys {
            triggeredWindows.remove(windowKey)
            Self.sharedState.pendingResetWindows.removeValue(forKey: windowKey)
            if let reservationID = Self.sharedState.reservationOwners.removeValue(forKey: windowKey) {
                affectedReservations.insert(reservationID)
            }
        }
        let remainingReservations = Set(Self.sharedState.reservationOwners.values)
        for reservationID in affectedReservations where !remainingReservations.contains(reservationID) {
            Self.sharedState.inFlightReservations.remove(reservationID)
        }
    }

    private func persistTriggeredWindows(_ triggeredWindows: Set<AlertWindowKey>) {
        guard let data = try? JSONEncoder().encode(triggeredWindows) else {
            return
        }
        defaults.set(data, forKey: Self.persistedKey)
    }

    private func persistPeriodicWatermarks(
        _ periodicWatermarks: Set<AlertPeriodicWindowWatermark>
    ) {
        guard let data = try? JSONEncoder().encode(periodicWatermarks) else {
            return
        }
        defaults.set(data, forKey: Self.periodicWatermarksKey)
    }

    private static func loadTriggeredWindows(from defaults: UserDefaults) -> Set<AlertWindowKey> {
        guard
            let data = defaults.data(forKey: persistedKey),
            let values = try? JSONDecoder().decode(Set<AlertWindowKey>.self, from: data)
        else {
            return []
        }
        return values
    }

    private static func loadPeriodicWatermarks(
        from defaults: UserDefaults
    ) -> Set<AlertPeriodicWindowWatermark> {
        guard
            let data = defaults.data(forKey: periodicWatermarksKey),
            let values = try? JSONDecoder().decode(
                Set<AlertPeriodicWindowWatermark>.self,
                from: data
            )
        else {
            return []
        }
        return values
    }
}

protocol NotificationSending: Sendable {
    func requestAuthorization() async throws -> Bool
    func send(_ alert: UsageAlert) async throws
}

struct AlertManager: Sendable {
    private let evaluator: AlertEvaluator
    private let sender: any NotificationSending

    init(evaluator: AlertEvaluator = AlertEvaluator(), sender: any NotificationSending) {
        self.evaluator = evaluator
        self.sender = sender
    }

    func evaluateAndNotify(
        key: KeyConfiguration,
        snapshot: UsageSnapshot,
        thresholds: AlertThresholds = AlertThresholds(),
        notificationsEnabled: Bool
    ) async throws -> [UsageAlert] {
        guard notificationsEnabled else {
            return []
        }

        let alerts = evaluator.evaluate(key: key, snapshot: snapshot, thresholds: thresholds)
        guard !alerts.isEmpty else {
            return []
        }
        let authorized: Bool
        do {
            authorized = try await sender.requestAuthorization()
        } catch {
            evaluator.restoreEligibility(for: alerts)
            throw error
        }
        guard authorized else {
            evaluator.restoreEligibility(for: alerts)
            return alerts
        }
        for (index, alert) in alerts.enumerated() {
            guard evaluator.beginDelivery(of: alert) else {
                evaluator.restoreEligibility(for: [alert])
                continue
            }
            do {
                try await sender.send(alert)
                evaluator.finishDelivery(of: alert)
            } catch {
                evaluator.restoreEligibility(for: alerts[index...])
                throw error
            }
        }
        return alerts
    }
}

final class UserNotificationSender: NotificationSending, @unchecked Sendable {
    private let center: UNUserNotificationCenter
    private let timeZone: TimeZone

    init(
        center: UNUserNotificationCenter = .current(),
        timeZone: TimeZone = .autoupdatingCurrent
    ) {
        self.center = center
        self.timeZone = timeZone
    }

    func requestAuthorization() async throws -> Bool {
        try await center.requestAuthorization(options: [.alert, .sound])
    }

    func send(_ alert: UsageAlert) async throws {
        let content = UNMutableNotificationContent()
        content.title = alert.notificationTitle
        content.body = alert.notificationBody(timeZone: timeZone)
        content.sound = .default
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        try await center.add(request)
    }
}
