import Foundation
import XCTest
@testable import RoutinUsage

final class AlertManagerTests: XCTestCase {
    func test默认阈值为八十和九十五且只允许有效顺序与范围() {
        XCTAssertEqual(AlertThresholds().low, 80)
        XCTAssertEqual(AlertThresholds().high, 95)
        XCTAssertTrue(AlertThresholds.isValid(low: 1, high: 100))
        XCTAssertFalse(AlertThresholds.isValid(low: 0, high: 95))
        XCTAssertFalse(AlertThresholds.isValid(low: 80, high: 101))
        XCTAssertFalse(AlertThresholds.isValid(low: 80, high: 80))
        XCTAssertFalse(AlertThresholds.isValid(low: 95, high: 80))
    }

    func test五小时额度在低阈值以下不通知达到阈值后同窗口不重复() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()
        let windowEnd = Date(timeIntervalSince1970: 10_000)

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(fiveHourPercent: 79, windowEnd: windowEnd),
                thresholds: .init(low: 80, high: 95)
            ),
            []
        )

        let alerts = context.evaluator.evaluate(
            key: key,
            snapshot: periodicSnapshot(fiveHourPercent: 80, windowEnd: windowEnd),
            thresholds: .init(low: 80, high: 95)
        )

        XCTAssertEqual(alerts.map(\.level), [.low])
        XCTAssertEqual(alerts.first?.keyName, "主账号")
        XCTAssertEqual(alerts.first?.dimension, .fiveHour)
        XCTAssertEqual(alerts.first?.percent, 80)
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(fiveHourPercent: 81, windowEnd: windowEnd),
                thresholds: .init(low: 80, high: 95)
            ),
            []
        )
    }

    func test一次跨越多个阈值只通知最高级且不会随后补发低级() {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()
        let windowEnd = Date(timeIntervalSince1970: 10_000)

        _ = context.evaluator.evaluate(
            key: key,
            snapshot: periodicSnapshot(fiveHourPercent: 79, windowEnd: windowEnd),
            thresholds: .init(low: 80, high: 95)
        )
        let alerts = context.evaluator.evaluate(
            key: key,
            snapshot: periodicSnapshot(fiveHourPercent: 96, windowEnd: windowEnd),
            thresholds: .init(low: 80, high: 95)
        )

        XCTAssertEqual(alerts.map(\.level), [.high])
        XCTAssertEqual(alerts.first?.keyName, "主账号")
        XCTAssertEqual(alerts.first?.dimension, .fiveHour)
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(fiveHourPercent: 82, windowEnd: windowEnd),
                thresholds: .init(low: 80, high: 95)
            ),
            []
        )
    }

    func test周期额度分别评估五小时和周额度() {
        let context = makeContext()
        defer { context.cleanUp() }

        let alerts = context.evaluator.evaluate(
            key: makeKey(),
            snapshot: periodicSnapshot(
                fiveHourPercent: 80,
                fiveHourWindowEnd: Date(timeIntervalSince1970: 10_000),
                weeklyPercent: 96,
                weeklyWindowEnd: Date(timeIntervalSince1970: 20_000)
            ),
            thresholds: .init(low: 80, high: 95)
        )

        XCTAssertEqual(alerts.map(\.dimension), [.fiveHour, .weekly])
        XCTAssertEqual(alerts.map(\.level), [.low, .high])
    }

    func test新周期窗口恢复通知资格且旧窗口记录跨评估器持久去重() {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(
                    fiveHourPercent: 80,
                    windowEnd: Date(timeIntervalSince1970: 10_000)
                ),
                thresholds: .init()
            ).map(\.level),
            [.low]
        )

        let reloadedEvaluator = AlertEvaluator(defaults: context.defaults)
        XCTAssertEqual(
            reloadedEvaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(
                    fiveHourPercent: 90,
                    windowEnd: Date(timeIntervalSince1970: 10_000)
                ),
                thresholds: .init()
            ),
            []
        )
        XCTAssertEqual(
            reloadedEvaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(
                    fiveHourPercent: 90,
                    windowEnd: Date(timeIntervalSince1970: 20_000)
                ),
                thresholds: .init()
            ).map(\.level),
            [.low]
        )
    }

    func test周期窗口和阈值变化后只保留当前去重记录() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()

        for windowIndex in 1...10 {
            XCTAssertEqual(
                context.evaluator.evaluate(
                    key: key,
                    snapshot: periodicSnapshot(
                        fiveHourPercent: 96,
                        windowEnd: Date(timeIntervalSince1970: Double(windowIndex * 10_000))
                    ),
                    thresholds: .init()
                ).map(\.level),
                [.high]
            )
        }

        var persistedKeys = try persistedWindowKeys(from: context.defaults)
        var currentKeys = persistedKeys.filter {
            $0.keyID == key.id && $0.dimension == .fiveHour
        }
        XCTAssertEqual(currentKeys.count, 2)
        XCTAssertEqual(Set(currentKeys.map(\.windowIdentifier)), ["100000.0"])
        XCTAssertEqual(Set(currentKeys.map(\.threshold)), [80, 95])

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: periodicSnapshot(
                    fiveHourPercent: 99,
                    windowEnd: Date(timeIntervalSince1970: 100_000)
                ),
                thresholds: .init(low: 85, high: 98)
            ).map(\.level),
            [.high]
        )

        persistedKeys = try persistedWindowKeys(from: context.defaults)
        currentKeys = persistedKeys.filter {
            $0.keyID == key.id && $0.dimension == .fiveHour
        }
        XCTAssertEqual(currentKeys.count, 2)
        XCTAssertEqual(Set(currentKeys.map(\.windowIdentifier)), ["100000.0"])
        XCTAssertEqual(Set(currentKeys.map(\.threshold)), [85, 98])
    }

    func test跨评估器写入不同Key不会覆盖既有去重记录() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let firstEvaluator = context.evaluator
        let secondDefaults = try XCTUnwrap(UserDefaults(suiteName: context.suiteName))
        let secondEvaluator = AlertEvaluator(defaults: secondDefaults)
        let firstKey = makeKey()
        let secondKey = makeKey()
        let snapshot = periodicSnapshot(
            fiveHourPercent: 80,
            windowEnd: Date(timeIntervalSince1970: 10_000)
        )

        XCTAssertEqual(
            firstEvaluator.evaluate(
                key: firstKey,
                snapshot: snapshot,
                thresholds: .init()
            ).map(\.level),
            [.low]
        )
        XCTAssertEqual(
            secondEvaluator.evaluate(
                key: secondKey,
                snapshot: snapshot,
                thresholds: .init()
            ).map(\.level),
            [.low]
        )

        let reloadedDefaults = try XCTUnwrap(UserDefaults(suiteName: context.suiteName))
        let reloadedEvaluator = AlertEvaluator(defaults: reloadedDefaults)
        XCTAssertEqual(
            reloadedEvaluator.evaluate(
                key: firstKey,
                snapshot: snapshot,
                thresholds: .init()
            ),
            []
        )
        XCTAssertEqual(
            reloadedEvaluator.evaluate(
                key: secondKey,
                snapshot: snapshot,
                thresholds: .init()
            ),
            []
        )
        XCTAssertEqual(try persistedWindowKeys(from: context.defaults).count, 2)
    }

    func test跨评估器评估同一窗口只产生一次通知() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let firstEvaluator = context.evaluator
        let secondDefaults = try XCTUnwrap(UserDefaults(suiteName: context.suiteName))
        let secondEvaluator = AlertEvaluator(defaults: secondDefaults)
        let key = makeKey()
        let snapshot = tokenSnapshot(percent: 96)

        XCTAssertEqual(
            firstEvaluator.evaluate(
                key: key,
                snapshot: snapshot,
                thresholds: .init()
            ).map(\.level),
            [.high]
        )
        XCTAssertEqual(
            secondEvaluator.evaluate(
                key: key,
                snapshot: snapshot,
                thresholds: .init()
            ),
            []
        )
    }

    func test资源包只评估Token且回落到阈值以下后可再次通知() {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()

        let initialAlerts = context.evaluator.evaluate(
            key: key,
            snapshot: tokenSnapshot(percent: 96),
            thresholds: .init()
        )
        XCTAssertEqual(initialAlerts.map(\.dimension), [.token])
        XCTAssertEqual(initialAlerts.map(\.level), [.high])
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 94),
                thresholds: .init()
            ),
            []
        )
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 96),
                thresholds: .init()
            ).map(\.level),
            [.high]
        )
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 79),
                thresholds: .init()
            ),
            []
        )
        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 81),
                thresholds: .init()
            ).map(\.level),
            [.low]
        )
    }

    func test关闭通知时不请求授权不发送且不消耗阈值资格() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let sender = NotificationSenderSpy(authorizationGranted: true)
        let manager = AlertManager(evaluator: context.evaluator, sender: sender)
        let key = makeKey()
        let snapshot = periodicSnapshot(
            fiveHourPercent: 96,
            windowEnd: Date(timeIntervalSince1970: 10_000)
        )

        let disabledAlerts = try await manager.evaluateAndNotify(
            key: key,
            snapshot: snapshot,
            thresholds: .init(),
            notificationsEnabled: false
        )

        XCTAssertEqual(disabledAlerts, [])
        let disabledSenderState = await sender.state
        XCTAssertEqual(disabledSenderState.authorizationRequestCount, 0)
        XCTAssertEqual(disabledSenderState.sentAlerts, [])

        let enabledAlerts = try await manager.evaluateAndNotify(
            key: key,
            snapshot: snapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        XCTAssertEqual(enabledAlerts.map(\.level), [.high])
        let enabledSenderState = await sender.state
        XCTAssertEqual(enabledSenderState.authorizationRequestCount, 1)
        XCTAssertEqual(enabledSenderState.sentAlerts.map(\.level), [.high])
    }

    func test授权未通过时不发送通知且下次授权后仍可提醒() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let sender = NotificationSenderSpy(authorizationResults: [false, true])
        let manager = AlertManager(evaluator: context.evaluator, sender: sender)
        let key = makeKey()
        let snapshot = tokenSnapshot(percent: 80)

        let alerts = try await manager.evaluateAndNotify(
            key: key,
            snapshot: snapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )

        XCTAssertEqual(alerts.map(\.level), [.low])
        let senderState = await sender.state
        XCTAssertEqual(senderState.authorizationRequestCount, 1)
        XCTAssertEqual(senderState.sentAlerts, [])

        let retriedAlerts = try await manager.evaluateAndNotify(
            key: key,
            snapshot: snapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        let retriedSenderState = await sender.state
        XCTAssertEqual(retriedAlerts.map(\.level), [.low])
        XCTAssertEqual(retriedSenderState.authorizationRequestCount, 2)
        XCTAssertEqual(retriedSenderState.sentAlerts.map(\.level), [.low])
    }

    func test发送失败后保留提醒资格供下次重试() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let sender = NotificationSenderSpy(
            authorizationResults: [true, true],
            sendFailureCount: 1
        )
        let manager = AlertManager(evaluator: context.evaluator, sender: sender)
        let key = makeKey()
        let snapshot = tokenSnapshot(percent: 96)

        do {
            _ = try await manager.evaluateAndNotify(
                key: key,
                snapshot: snapshot,
                thresholds: .init(),
                notificationsEnabled: true
            )
            XCTFail("首次发送应失败")
        } catch {
            XCTAssertEqual(error as? NotificationSenderSpyError, .发送失败)
        }

        let retriedAlerts = try await manager.evaluateAndNotify(
            key: key,
            snapshot: snapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        let senderState = await sender.state
        XCTAssertEqual(retriedAlerts.map(\.level), [.high])
        XCTAssertEqual(senderState.authorizationRequestCount, 2)
        XCTAssertEqual(senderState.sentAlerts.map(\.level), [.high])
    }

    func test旧授权请求回滚不会删除较新成功通知的同一Token资格() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()
        let delayedSender = ControlledAuthorizationSender()
        let delayedManager = AlertManager(evaluator: context.evaluator, sender: delayedSender)
        let highSnapshot = tokenSnapshot(percent: 96)
        let delayedTask = Task {
            try await delayedManager.evaluateAndNotify(
                key: key,
                snapshot: highSnapshot,
                thresholds: .init(),
                notificationsEnabled: true
            )
        }
        await delayedSender.waitUntilAuthorizationRequestCount(1)

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 94),
                thresholds: .init()
            ),
            []
        )
        let successfulSender = NotificationSenderSpy(authorizationGranted: true)
        let successfulManager = AlertManager(evaluator: context.evaluator, sender: successfulSender)
        let successfulAlerts = try await successfulManager.evaluateAndNotify(
            key: key,
            snapshot: highSnapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        XCTAssertEqual(successfulAlerts.map(\.level), [.high])

        await delayedSender.resolveNextAuthorization(false)
        _ = try await delayedTask.value

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: highSnapshot,
                thresholds: .init()
            ),
            []
        )
    }

    func test发送挂起期间Token回落再升高不会重复派发高阈值通知() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()
        let highSnapshot = tokenSnapshot(percent: 96)
        let delayedSender = ControlledSendingSender()
        let delayedManager = AlertManager(evaluator: context.evaluator, sender: delayedSender)
        let delayedTask = Task {
            try await delayedManager.evaluateAndNotify(
                key: key,
                snapshot: highSnapshot,
                thresholds: .init(),
                notificationsEnabled: true
            )
        }
        await delayedSender.waitUntilSendCount(1)

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 94),
                thresholds: .init()
            ),
            []
        )
        let competingSender = NotificationSenderSpy(authorizationGranted: true)
        let competingManager = AlertManager(evaluator: context.evaluator, sender: competingSender)
        let competingAlerts = try await competingManager.evaluateAndNotify(
            key: key,
            snapshot: highSnapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        XCTAssertEqual(competingAlerts, [])
        let competingState = await competingSender.state
        XCTAssertEqual(competingState.authorizationRequestCount, 0)
        XCTAssertEqual(competingState.sentAlerts, [])

        await delayedSender.resolveNextSend()
        _ = try await delayedTask.value
    }

    func test发送挂起期间Token回落会在发送完成后恢复高阈值资格() async throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = makeKey()
        let highSnapshot = tokenSnapshot(percent: 96)
        let delayedSender = ControlledSendingSender()
        let delayedManager = AlertManager(evaluator: context.evaluator, sender: delayedSender)
        let delayedTask = Task {
            try await delayedManager.evaluateAndNotify(
                key: key,
                snapshot: highSnapshot,
                thresholds: .init(),
                notificationsEnabled: true
            )
        }
        await delayedSender.waitUntilSendCount(1)

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: key,
                snapshot: tokenSnapshot(percent: 94),
                thresholds: .init()
            ),
            []
        )
        await delayedSender.resolveNextSend()
        _ = try await delayedTask.value

        let retrySender = NotificationSenderSpy(authorizationGranted: true)
        let retryManager = AlertManager(evaluator: context.evaluator, sender: retrySender)
        let retryAlerts = try await retryManager.evaluateAndNotify(
            key: key,
            snapshot: highSnapshot,
            thresholds: .init(),
            notificationsEnabled: true
        )
        XCTAssertEqual(retryAlerts.map(\.level), [.high])
        let retryState = await retrySender.state
        XCTAssertEqual(retryState.sentAlerts.map(\.level), [.high])
    }

    func test周期额度缺少窗口结束时间时不产生无法去重的通知() {
        let context = makeContext()
        defer { context.cleanUp() }
        let snapshot = UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: metric(percent: 96, windowEnd: nil),
            weekly: metric(percent: 96, windowEnd: nil),
            token: nil,
            allowedModels: [],
            fetchedAt: .now
        )

        XCTAssertEqual(
            context.evaluator.evaluate(
                key: makeKey(),
                snapshot: snapshot,
                thresholds: .init()
            ),
            []
        )
    }

    func test通知文案只含名称额度百分比与周期重置时间() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let key = KeyConfiguration(
            id: UUID(),
            name: "主账号",
            keySuffix: "绝不能出现在通知里的Key尾号",
            sortOrder: 0
        )
        let timeZone = try XCTUnwrap(TimeZone(secondsFromGMT: 0))
        let periodicAlert = try XCTUnwrap(context.evaluator.evaluate(
            key: key,
            snapshot: periodicSnapshot(
                fiveHourPercent: 96,
                windowEnd: Date(timeIntervalSince1970: 50_400)
            ),
            thresholds: .init()
        ).first)

        XCTAssertEqual(periodicAlert.notificationTitle, "Routin 用量预警")
        XCTAssertEqual(
            periodicAlert.notificationBody(timeZone: timeZone),
            "主账号 · 5 小时用量已达 96%，窗口将在 14:00 重置"
        )
        XCTAssertFalse(periodicAlert.notificationBody(timeZone: timeZone).contains(key.keySuffix))

        let tokenAlert = try XCTUnwrap(context.evaluator.evaluate(
            key: key,
            snapshot: tokenSnapshot(percent: 80),
            thresholds: .init()
        ).first)
        XCTAssertEqual(tokenAlert.notificationBody(timeZone: timeZone), "主账号 · Token 用量已达 80%")
    }

    private func makeContext() -> AlertEvaluatorTestContext {
        let suiteName = "ai.routin.usage-monitor.alert-tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.removePersistentDomain(forName: suiteName)
        return AlertEvaluatorTestContext(
            suiteName: suiteName,
            defaults: defaults,
            evaluator: AlertEvaluator(defaults: defaults)
        )
    }

    private func makeKey() -> KeyConfiguration {
        KeyConfiguration(id: UUID(), name: "主账号", keySuffix: "abcd", sortOrder: 0)
    }

    private func periodicSnapshot(
        fiveHourPercent: Double,
        windowEnd: Date
    ) -> UsageSnapshot {
        periodicSnapshot(
            fiveHourPercent: fiveHourPercent,
            fiveHourWindowEnd: windowEnd,
            weeklyPercent: nil,
            weeklyWindowEnd: nil
        )
    }

    private func periodicSnapshot(
        fiveHourPercent: Double,
        fiveHourWindowEnd: Date,
        weeklyPercent: Double?,
        weeklyWindowEnd: Date?
    ) -> UsageSnapshot {
        UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: metric(percent: fiveHourPercent, windowEnd: fiveHourWindowEnd),
            weekly: weeklyPercent.map { metric(percent: $0, windowEnd: weeklyWindowEnd) },
            token: nil,
            allowedModels: [],
            fetchedAt: .now
        )
    }

    private func tokenSnapshot(percent: Double) -> UsageSnapshot {
        UsageSnapshot(
            planName: "资源包",
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: metric(percent: percent, windowEnd: nil, unit: .token),
            allowedModels: [],
            fetchedAt: .now
        )
    }

    private func metric(
        percent: Double,
        windowEnd: Date?,
        unit: UsageUnit = .usd
    ) -> UsageMetric {
        UsageMetric(
            used: Decimal(percent),
            limit: 100,
            remaining: Decimal(100 - percent),
            percent: percent,
            unit: unit,
            windowEnd: windowEnd
        )
    }

    private func persistedWindowKeys(from defaults: UserDefaults) throws -> Set<AlertWindowKey> {
        let data = try XCTUnwrap(defaults.data(forKey: "usageAlertTriggeredWindows"))
        return try JSONDecoder().decode(Set<AlertWindowKey>.self, from: data)
    }
}

private struct AlertEvaluatorTestContext {
    let suiteName: String
    let defaults: UserDefaults
    let evaluator: AlertEvaluator

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private actor NotificationSenderSpy: NotificationSending {
    private var authorizationResults: [Bool]
    private var sendFailureCount: Int
    private(set) var authorizationRequestCount = 0
    private(set) var sentAlerts: [UsageAlert] = []

    init(authorizationGranted: Bool) {
        authorizationResults = [authorizationGranted]
        sendFailureCount = 0
    }

    init(authorizationResults: [Bool], sendFailureCount: Int = 0) {
        self.authorizationResults = authorizationResults
        self.sendFailureCount = sendFailureCount
    }

    var state: (authorizationRequestCount: Int, sentAlerts: [UsageAlert]) {
        (authorizationRequestCount, sentAlerts)
    }

    func requestAuthorization() async throws -> Bool {
        authorizationRequestCount += 1
        return authorizationResults.removeFirst()
    }

    func send(_ alert: UsageAlert) async throws {
        if sendFailureCount > 0 {
            sendFailureCount -= 1
            throw NotificationSenderSpyError.发送失败
        }
        sentAlerts.append(alert)
    }
}

private enum NotificationSenderSpyError: Error, Equatable {
    case 发送失败
}

private actor ControlledAuthorizationSender: NotificationSending {
    private var authorizationRequestCount = 0
    private var authorizationContinuations: [CheckedContinuation<Bool, Never>] = []
    private var requestWaiters: [(count: Int, continuation: CheckedContinuation<Void, Never>)] = []

    func waitUntilAuthorizationRequestCount(_ expectedCount: Int) async {
        guard authorizationRequestCount < expectedCount else {
            return
        }
        await withCheckedContinuation { continuation in
            requestWaiters.append((expectedCount, continuation))
        }
    }

    func resolveNextAuthorization(_ granted: Bool) {
        authorizationContinuations.removeFirst().resume(returning: granted)
    }

    func requestAuthorization() async throws -> Bool {
        authorizationRequestCount += 1
        let readyWaiters = requestWaiters.filter { authorizationRequestCount >= $0.count }
        requestWaiters.removeAll { authorizationRequestCount >= $0.count }
        for waiter in readyWaiters {
            waiter.continuation.resume()
        }
        return await withCheckedContinuation { continuation in
            authorizationContinuations.append(continuation)
        }
    }

    func send(_ alert: UsageAlert) async throws {}
}

private actor ControlledSendingSender: NotificationSending {
    private var sendCount = 0
    private var sendContinuations: [CheckedContinuation<Void, Never>] = []
    private var sendWaiters: [(count: Int, continuation: CheckedContinuation<Void, Never>)] = []

    func waitUntilSendCount(_ expectedCount: Int) async {
        guard sendCount < expectedCount else {
            return
        }
        await withCheckedContinuation { continuation in
            sendWaiters.append((expectedCount, continuation))
        }
    }

    func resolveNextSend() {
        sendContinuations.removeFirst().resume()
    }

    func requestAuthorization() async throws -> Bool {
        true
    }

    func send(_ alert: UsageAlert) async throws {
        sendCount += 1
        let readyWaiters = sendWaiters.filter { sendCount >= $0.count }
        sendWaiters.removeAll { sendCount >= $0.count }
        for waiter in readyWaiters {
            waiter.continuation.resume()
        }
        await withCheckedContinuation { continuation in
            sendContinuations.append(continuation)
        }
    }
}
