import AppKit
import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class AppLifecycleTests: XCTestCase {
    func test启动先保留缓存并立即刷新随后启动调度() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let key = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        let cached = makeSnapshot(planName: "缓存")
        let refreshed = makeSnapshot(planName: "网络")
        try context.cache.save(cached, for: key.id)
        await context.fetcher.setResponse(.success(refreshed), for: "plan-main-0001")
        let environment = context.makeEnvironment()

        XCTAssertEqual(environment.store.state(for: key.id)?.snapshot, cached)

        await environment.start()

        let requestCount = await context.fetcher.requestCount(for: "plan-main-0001")
        XCTAssertEqual(requestCount, 1)
        XCTAssertEqual(environment.store.state(for: key.id)?.snapshot, refreshed)
        XCTAssertEqual(context.scheduler.startedMinutes, [5])
    }

    func test周期触发后再次刷新全部Key() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        await context.fetcher.setResponse(.success(makeSnapshot()), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()

        context.scheduler.fireTick()
        await 等待条件 {
            await context.fetcher.requestCount(for: "plan-main-0001") == 2
        }

        let requestCount = await context.fetcher.requestCount(for: "plan-main-0001")
        XCTAssertEqual(requestCount, 2)
    }

    func test修改刷新间隔只重新调度而不发请求() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        await context.fetcher.setResponse(.success(makeSnapshot()), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()

        environment.settings.refreshMinutes = 15
        environment.refreshIntervalDidChange(to: 15)

        XCTAssertEqual(context.scheduler.rescheduledMinutes, [15])
        let requestCount = await context.fetcher.requestCount(for: "plan-main-0001")
        XCTAssertEqual(requestCount, 1)
    }

    func test运行期通知开关立即同步到用量刷新() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        let highUsage = makeSnapshot(planName: "高用量", percent: 96)
        await context.fetcher.setResponse(.success(highUsage), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()

        environment.settings.notificationsEnabled = true
        await environment.notificationsDidChange(enabled: true)
        await environment.store.refreshAll()
        await 等待条件 {
            await context.notificationSender.sentAlerts().count == 1
        }
        await context.fetcher.setResponse(.success(makeSnapshot(percent: 20)), for: "plan-main-0001")
        await environment.store.refreshAll()
        environment.settings.notificationsEnabled = false
        await environment.notificationsDidChange(enabled: false)
        await context.fetcher.setResponse(.success(highUsage), for: "plan-main-0001")
        await environment.store.refreshAll()

        let alerts = await context.notificationSender.sentAlerts()
        XCTAssertEqual(alerts.count, 1)
        XCTAssertEqual(alerts.first?.level, .high)
    }

    func test运行期阈值变更立即影响通知去重() async throws {
        let context = try makeContext(notificationsEnabled: true)
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        let usage = makeSnapshot(planName: "中高用量", percent: 92)
        await context.fetcher.setResponse(.success(usage), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()
        await 等待条件 {
            await context.notificationSender.sentAlerts().count == 1
        }

        environment.settings.thresholds = AlertThresholds(low: 80, high: 90)
        environment.thresholdsDidChange(to: environment.settings.thresholds)
        await environment.store.refreshAll()
        await 等待条件 {
            await context.notificationSender.sentAlerts().count == 2
        }

        let alerts = await context.notificationSender.sentAlerts()
        XCTAssertEqual(alerts.map(\.level), [.low, .high])
    }

    func test运行期刷新间隔变更立即影响过期判断() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        context.defaults.set(15, forKey: "refreshMinutes")
        let key = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        let cached = makeSnapshot(fetchedAt: Date(timeIntervalSince1970: 9_500))
        try context.cache.save(cached, for: key.id)
        await context.fetcher.setResponse(.success(cached), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()
        let stateBefore = try XCTUnwrap(environment.store.state(for: key.id))
        XCTAssertFalse(stateBefore.isStale)

        environment.settings.refreshMinutes = 1
        environment.refreshIntervalDidChange(to: 1)

        XCTAssertTrue(environment.store.state(for: key.id)?.isStale == true)
        XCTAssertEqual(context.scheduler.rescheduledMinutes, [1])
    }

    func test启动使用启动时最新通知阈值与刷新间隔设置() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        let usage = makeSnapshot(planName: "中高用量", percent: 92)
        await context.fetcher.setResponse(.success(usage), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        environment.settings.notificationsEnabled = true
        environment.settings.thresholds = AlertThresholds(low: 80, high: 90)
        environment.settings.refreshMinutes = 1

        await environment.start()
        await 等待条件 {
            await context.notificationSender.sentAlerts().count == 1
        }

        let alerts = await context.notificationSender.sentAlerts()
        XCTAssertEqual(alerts.map(\.level), [.high])
        XCTAssertEqual(context.scheduler.startedMinutes, [1])
    }

    func test首次刷新等待期间停止不会重新启动调度器() async throws {
        let suiteName = "AppLifecycleTests.start-stop-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let keychain = LifecycleKeychainFake()
        let repository = KeyRepository(defaults: defaults, localStore: keychain)
        let key = try repository.add(name: "主账号", secret: "plan-gated-0001")
        let fetcher = ScriptedUsageFetcher(responses: ["plan-gated-0001": .suspended])
        let sender = LifecycleNotificationSender()
        let store = UsageStore(
            keyRepository: repository,
            localStore: keychain,
            apiClient: fetcher,
            cache: LifecycleUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: sender,
            defaults: defaults,
            now: { Date(timeIntervalSince1970: 10_000) }
        )
        let scheduler = LifecycleRefreshSchedulerSpy()
        let environment = AppEnvironment(
            settings: AppSettings(defaults: defaults),
            store: store,
            refreshScheduler: scheduler,
            loginItemManager: LifecycleLoginItemManager(),
            keyRepository: repository,
            apiClient: fetcher,
            notificationSender: sender,
            applicationNotificationCenter: NotificationCenter()
        )

        let start = Task { @MainActor in await environment.start() }
        await fetcher.waitUntilRequested("plan-gated-0001")
        environment.stop()
        await fetcher.resume("plan-gated-0001", with: .success(nil))
        await start.value

        XCTAssertEqual(scheduler.startedMinutes, [])
        XCTAssertEqual(scheduler.stopCount, 1)
        XCTAssertEqual(environment.store.state(for: key.id)?.error, .noSubscription)
    }

    func test编辑Key验证成功与旧刷新重叠时最终发布新数据() async throws {
        let suiteName = "AppLifecycleTests.edit-refresh-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let keychain = LifecycleKeychainFake()
        let repository = KeyRepository(defaults: defaults, localStore: keychain)
        let key = try repository.add(name: "旧名称", secret: "plan-old-0001")
        let oldSnapshot = makeSnapshot(planName: "旧数据", percent: 96)
        let newSnapshot = makeSnapshot(planName: "新数据", percent: 20)
        let fetcher = ScriptedUsageFetcher(responses: [
            "plan-old-0001": .suspended,
            "plan-new-0002": .success(newSnapshot)
        ])
        let sender = LifecycleNotificationSender()
        let store = UsageStore(
            keyRepository: repository,
            localStore: keychain,
            apiClient: fetcher,
            cache: LifecycleUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: sender,
            defaults: defaults,
            now: { Date(timeIntervalSince1970: 10_000) }
        )
        let environment = AppEnvironment(
            settings: AppSettings(defaults: defaults),
            store: store,
            refreshScheduler: LifecycleRefreshSchedulerSpy(),
            loginItemManager: LifecycleLoginItemManager(),
            keyRepository: repository,
            apiClient: fetcher,
            notificationSender: sender,
            applicationNotificationCenter: NotificationCenter()
        )
        let oldRefresh = Task { @MainActor in await store.refresh(keyID: key.id) }
        await fetcher.waitUntilRequested("plan-old-0001")

        let edit = Task { @MainActor in
            try await environment.updateValidatedKey(
                id: key.id,
                name: "新名称",
                secret: "plan-new-0002"
            )
        }
        _ = try await edit.value
        await fetcher.resume("plan-old-0001", with: .success(oldSnapshot))
        await oldRefresh.value

        XCTAssertEqual(store.state(for: key.id)?.configuration.name, "新名称")
        XCTAssertEqual(store.state(for: key.id)?.snapshot, newSnapshot)
        let newRequestCount = await fetcher.requestCount(for: "plan-new-0002")
        XCTAssertEqual(newRequestCount, 1)
    }

    func test唤醒事件去抖后补刷全部Key() async throws {
        let context = try makeContext(useRealScheduler: true)
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        await context.fetcher.setResponse(.success(makeSnapshot()), for: "plan-main-0001")
        let environment = context.makeEnvironment()
        await environment.start()

        context.workspaceNotificationCenter.post(
            name: NSWorkspace.didWakeNotification,
            object: nil
        )
        await 等待条件 { context.timerScheduler.scheduledIntervals.count == 2 }
        context.timerScheduler.timer(at: 1).fire()
        await 等待条件 {
            await context.fetcher.requestCount(for: "plan-main-0001") == 2
        }

        let requestCount = await context.fetcher.requestCount(for: "plan-main-0001")
        XCTAssertEqual(requestCount, 2)
    }

    func test开启通知在应用生命周期内只请求一次授权() async throws {
        let context = try makeContext(notificationsEnabled: true)
        defer { context.cleanUp() }
        let environment = context.makeEnvironment()

        await environment.start()
        await environment.start()
        await environment.notificationsDidChange(enabled: true)
        await environment.notificationsDidChange(enabled: true)

        let authorizationRequestCount = await context.notificationSender.authorizationRequestCount
        XCTAssertEqual(authorizationRequestCount, 1)
    }

    func test通知关闭后重新启用会再次检查系统授权() async throws {
        let context = try makeContext(notificationsEnabled: true)
        defer { context.cleanUp() }
        let environment = context.makeEnvironment()

        await environment.start()
        await 等待条件 {
            await context.notificationSender.authorizationRequestCount == 1
        }

        await environment.notificationsDidChange(enabled: false)
        await environment.notificationsDidChange(enabled: true)
        await 等待条件 {
            await context.notificationSender.authorizationRequestCount == 2
        }

        let authorizationRequestCount = await context.notificationSender.authorizationRequestCount
        XCTAssertEqual(authorizationRequestCount, 2)
    }

    func test通知授权挂起不会阻塞启动完成与刷新调度() async throws {
        let suiteName = "AppLifecycleTests.authorization-blocked-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let keychain = LifecycleKeychainFake()
        let repository = KeyRepository(defaults: defaults, localStore: keychain)
        let key = try repository.add(name: "主账号", secret: "plan-main-0001")
        let snapshot = makeSnapshot(percent: 96)
        let fetcher = ScriptedUsageFetcher(responses: ["plan-main-0001": .success(snapshot)])
        let blockingSender = BlockingAuthorizationNotificationSender()
        let notificationSender = AuthorizationCachingNotificationSender(sender: blockingSender)
        let settings = AppSettings(defaults: defaults)
        settings.notificationsEnabled = true
        let store = UsageStore(
            keyRepository: repository,
            localStore: keychain,
            apiClient: fetcher,
            cache: LifecycleUsageCache(),
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: notificationSender,
            defaults: defaults,
            notificationsEnabled: true,
            now: { Date(timeIntervalSince1970: 10_000) }
        )
        let scheduler = LifecycleRefreshSchedulerSpy()
        let environment = AppEnvironment(
            settings: settings,
            store: store,
            refreshScheduler: scheduler,
            loginItemManager: LifecycleLoginItemManager(),
            keyRepository: repository,
            apiClient: fetcher,
            notificationSender: notificationSender,
            applicationNotificationCenter: NotificationCenter()
        )
        var didStart = false

        let start = Task { @MainActor in
            await environment.start()
            didStart = true
        }
        await blockingSender.waitUntilAuthorizationRequested()

        await 等待条件 { didStart }
        XCTAssertTrue(didStart)
        XCTAssertEqual(environment.store.state(for: key.id)?.snapshot, snapshot)
        XCTAssertEqual(scheduler.startedMinutes, [5])

        await blockingSender.resolveAuthorization(true)
        await start.value
    }

    func test启动时设置显示系统实际登录启动状态() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let loginItemManager = EnabledLifecycleLoginItemManager()
        let settings = AppSettings(defaults: context.defaults)
        let store = UsageStore(
            keyRepository: context.repository,
            localStore: context.keychain,
            apiClient: context.fetcher,
            cache: context.cache,
            alertEvaluator: AlertEvaluator(defaults: context.defaults),
            notificationSender: context.notificationSender,
            defaults: context.defaults,
            notificationsEnabled: false,
            now: { Date(timeIntervalSince1970: 10_000) }
        )
        let environment = AppEnvironment(
            settings: settings,
            store: store,
            refreshScheduler: context.refreshScheduler,
            loginItemManager: loginItemManager,
            keyRepository: context.repository,
            apiClient: context.fetcher,
            notificationSender: context.notificationSender,
            applicationNotificationCenter: context.applicationNotificationCenter
        )

        await environment.start()

        XCTAssertTrue(settings.launchAtLogin)
    }

    func test并发授权检查共享同一个进行中请求() async throws {
        let sender = BlockingAuthorizationNotificationSender()
        let cachingSender = AuthorizationCachingNotificationSender(sender: sender)

        let first = Task { try await cachingSender.requestAuthorization() }
        await sender.waitUntilAuthorizationRequested()
        let second = Task { try await cachingSender.requestAuthorization() }
        await Task.yield()

        let authorizationRequestCount = await sender.authorizationRequestCount()
        XCTAssertEqual(authorizationRequestCount, 1)
        await sender.resolveAuthorization(true)
        let firstResult = try await first.value
        let secondResult = try await second.value
        XCTAssertTrue(firstResult)
        XCTAssertTrue(secondResult)
    }

    func test完成的授权结果不会被永久缓存() async throws {
        let sender = SequencedLifecycleNotificationSender(results: [false, true])
        let cachingSender = AuthorizationCachingNotificationSender(sender: sender)

        let firstResult = try await cachingSender.requestAuthorization()
        let secondResult = try await cachingSender.requestAuthorization()

        XCTAssertFalse(firstResult)
        XCTAssertTrue(secondResult)
        let authorizationRequestCount = await sender.authorizationRequestCount
        XCTAssertEqual(authorizationRequestCount, 2)
    }

    func test没有Key时请求引导而已有Key时不请求() async throws {
        let emptyContext = try makeContext()
        defer { emptyContext.cleanUp() }
        let emptyEnvironment = emptyContext.makeEnvironment()

        await emptyEnvironment.start()

        XCTAssertTrue(emptyEnvironment.showsOnboarding)

        let configuredContext = try makeContext()
        defer { configuredContext.cleanUp() }
        _ = try configuredContext.repository.add(
            name: "主账号",
            secret: "plan-main-0001"
        )
        await configuredContext.fetcher.setResponse(.success(nil), for: "plan-main-0001")
        let configuredEnvironment = configuredContext.makeEnvironment()

        await configuredEnvironment.start()

        XCTAssertFalse(configuredEnvironment.showsOnboarding)
    }

    func test应用终止会停止刷新调度() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let environment = context.makeEnvironment()
        await environment.start()

        context.applicationNotificationCenter.post(
            name: NSApplication.willTerminateNotification,
            object: nil
        )
        await 等待条件 { context.scheduler.stopCount == 1 }

        XCTAssertEqual(context.scheduler.stopCount, 1)
    }

    func test启动立即检查更新并启动六小时调度() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let service = LifecycleUpdateService(result: .none)
        let scheduler = LifecycleUpdateSchedulerSpy()
        let environment = context.makeEnvironment(
            updateService: service,
            updateCheckScheduler: scheduler
        )

        await environment.start()
        await 等待条件 { await service.checkCount == 1 }

        XCTAssertEqual(scheduler.startCount, 1)
    }

    func test更新调度触发再次检查且停止时取消调度() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let service = LifecycleUpdateService(result: .none)
        let scheduler = LifecycleUpdateSchedulerSpy()
        let environment = context.makeEnvironment(
            updateService: service,
            updateCheckScheduler: scheduler
        )

        await environment.start()
        await 等待条件 { await service.checkCount == 1 }
        scheduler.fireTick()
        await 等待条件 { await service.checkCount == 2 }
        environment.stop()

        XCTAssertEqual(scheduler.stopCount, 1)
    }

    func test后台检查失败时保留已有可安装更新() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let update = AppUpdate(
            version: "1.2.0",
            releaseURL: try XCTUnwrap(URL(string: "https://example.com/release")),
            downloadURL: try XCTUnwrap(URL(string: "https://example.com/download")),
            notes: "修复问题"
        )
        let service = LifecycleUpdateService(results: [
            .available(update),
            .failure(.unavailable)
        ])
        let scheduler = LifecycleUpdateSchedulerSpy()
        let environment = context.makeEnvironment(
            updateService: service,
            updateCheckScheduler: scheduler
        )

        await environment.start()
        await 等待条件 { await service.checkCount == 1 }
        XCTAssertEqual(environment.updateStatus, .available(update))
        scheduler.fireTick()
        await 等待条件 {
            await service.checkCount == 2 && environment.updateStatus == .available(update)
        }

        XCTAssertEqual(environment.updateStatus, .available(update))
    }

    func test切换Key时有缓存不请求而无数据只刷新所选Key() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let cachedKey = try context.repository.add(
            name: "有缓存",
            secret: "plan-cached-0001"
        )
        let emptyKey = try context.repository.add(
            name: "无缓存",
            secret: "plan-empty-0002"
        )
        try context.cache.save(makeSnapshot(planName: "缓存"), for: cachedKey.id)
        await context.fetcher.setResponse(.success(makeSnapshot(planName: "新数据")), for: "plan-empty-0002")
        let environment = context.makeEnvironment()

        environment.store.selectKey(cachedKey.id)
        await environment.selectedKeyDidChange(to: cachedKey.id)
        environment.store.selectKey(emptyKey.id)
        await environment.selectedKeyDidChange(to: emptyKey.id)

        let cachedRequestCount = await context.fetcher.requestCount(for: "plan-cached-0001")
        let emptyRequestCount = await context.fetcher.requestCount(for: "plan-empty-0002")
        XCTAssertEqual(cachedRequestCount, 0)
        XCTAssertEqual(emptyRequestCount, 1)
        XCTAssertEqual(environment.store.state(for: emptyKey.id)?.snapshot?.planName, "新数据")
    }

    func test切换显示周期立即重新计算菜单栏且不发请求() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        let key = try context.repository.add(name: "主账号", secret: "plan-main-0001")
        try context.cache.save(makePeriodicSnapshot(), for: key.id)
        let environment = context.makeEnvironment()
        let state = try XCTUnwrap(environment.store.state(for: key.id))

        let fiveHourText = UsageFormatter.menuBarText(
            state: state,
            dimension: environment.settings.displayDimension
        )
        environment.settings.displayDimension = .weekly
        let weeklyText = UsageFormatter.menuBarText(
            state: state,
            dimension: environment.settings.displayDimension
        )

        let requestCount = await context.fetcher.requestCount(for: "plan-main-0001")
        XCTAssertEqual(fiveHourText, "20%")
        XCTAssertEqual(weeklyText, "60%")
        XCTAssertEqual(requestCount, 0)
    }

    func test调整Key排序只同步顺序不额外刷新() async throws {
        let context = try makeContext()
        defer { context.cleanUp() }
        _ = try context.repository.add(name: "一", secret: "plan-one-0001")
        _ = try context.repository.add(name: "二", secret: "plan-two-0002")
        await context.fetcher.setResponse(.success(makeSnapshot()), for: "plan-one-0001")
        await context.fetcher.setResponse(.success(makeSnapshot()), for: "plan-two-0002")
        let environment = context.makeEnvironment()
        await environment.start()
        let ids = environment.store.orderedKeyIDs

        environment.moveKey(fromOffsets: IndexSet(integer: 0), toOffset: 2)
        await 等待条件 { environment.store.orderedKeyIDs == [ids[1], ids[0]] }

        let firstRequestCount = await context.fetcher.requestCount(for: "plan-one-0001")
        let secondRequestCount = await context.fetcher.requestCount(for: "plan-two-0002")
        XCTAssertEqual(environment.store.orderedKeyIDs, [ids[1], ids[0]])
        XCTAssertEqual(firstRequestCount, 1)
        XCTAssertEqual(secondRequestCount, 1)
    }
}

private extension AppLifecycleTests {
    func makeContext(
        notificationsEnabled: Bool = false,
        useRealScheduler: Bool = false
    ) throws -> AppLifecycleTestContext {
        let suiteName = "AppLifecycleTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defaults.removePersistentDomain(forName: suiteName)
        let keychain = LifecycleKeychainFake()
        let repository = KeyRepository(defaults: defaults, localStore: keychain)
        let cache = LifecycleUsageCache()
        let fetcher = LifecycleUsageFetcher()
        let notificationSender = LifecycleNotificationSender()
        let scheduler = LifecycleRefreshSchedulerSpy()
        let workspaceNotificationCenter = NotificationCenter()
        let timerScheduler = LifecycleTimerScheduler()
        let refreshScheduler: any RefreshScheduling
        if useRealScheduler {
            refreshScheduler = RefreshScheduler(
                timerScheduler: timerScheduler,
                workspaceNotificationCenter: workspaceNotificationCenter
            )
        } else {
            refreshScheduler = scheduler
        }
        return AppLifecycleTestContext(
            suiteName: suiteName,
            defaults: defaults,
            repository: repository,
            keychain: keychain,
            cache: cache,
            fetcher: fetcher,
            notificationSender: notificationSender,
            scheduler: scheduler,
            refreshScheduler: refreshScheduler,
            applicationNotificationCenter: NotificationCenter(),
            workspaceNotificationCenter: workspaceNotificationCenter,
            timerScheduler: timerScheduler,
            notificationsEnabled: notificationsEnabled
        )
    }

    func makeSnapshot(
        planName: String = "Pro",
        percent: Double = 20,
        fetchedAt: Date = Date(timeIntervalSince1970: 1_786_291_200)
    ) -> UsageSnapshot {
        UsageSnapshot(
            planName: planName,
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: UsageMetric(
                used: Decimal(percent),
                limit: 100,
                remaining: Decimal(100 - percent),
                percent: percent,
                unit: .token,
                windowEnd: nil
            ),
            allowedModels: [],
            fetchedAt: fetchedAt
        )
    }

    func makePeriodicSnapshot() -> UsageSnapshot {
        UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: UsageMetric(
                used: 20,
                limit: 100,
                remaining: 80,
                percent: 20,
                unit: .usd,
                windowEnd: Date(timeIntervalSince1970: 1_786_294_800)
            ),
            weekly: UsageMetric(
                used: 60,
                limit: 100,
                remaining: 40,
                percent: 60,
                unit: .usd,
                windowEnd: Date(timeIntervalSince1970: 1_786_896_000)
            ),
            token: nil,
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 1_786_291_200)
        )
    }

    func 等待条件(
        timeout: TimeInterval = 1,
        _ condition: @escaping @MainActor () async -> Bool
    ) async {
        let deadline = Date().addingTimeInterval(timeout)
        while !(await condition()), Date() < deadline {
            await Task.yield()
        }
    }
}

@MainActor
private struct AppLifecycleTestContext {
    let suiteName: String
    let defaults: UserDefaults
    let repository: KeyRepository
    let keychain: LifecycleKeychainFake
    let cache: LifecycleUsageCache
    let fetcher: LifecycleUsageFetcher
    let notificationSender: LifecycleNotificationSender
    let scheduler: LifecycleRefreshSchedulerSpy
    let refreshScheduler: any RefreshScheduling
    let applicationNotificationCenter: NotificationCenter
    let workspaceNotificationCenter: NotificationCenter
    let timerScheduler: LifecycleTimerScheduler
    let notificationsEnabled: Bool

    func makeEnvironment() -> AppEnvironment {
        makeEnvironment(
            updateService: NoUpdateService(),
            updateCheckScheduler: LifecycleUpdateSchedulerSpy()
        )
    }

    func makeEnvironment(
        updateService: any UpdateChecking,
        updateCheckScheduler: any UpdateCheckScheduling
    ) -> AppEnvironment {
        let settings = AppSettings(defaults: defaults)
        settings.notificationsEnabled = notificationsEnabled
        let store = UsageStore(
            keyRepository: repository,
            localStore: keychain,
            apiClient: fetcher,
            cache: cache,
            alertEvaluator: AlertEvaluator(defaults: defaults),
            notificationSender: notificationSender,
            defaults: defaults,
            refreshMinutes: settings.refreshMinutes,
            thresholds: settings.thresholds,
            notificationsEnabled: settings.notificationsEnabled,
            now: { Date(timeIntervalSince1970: 10_000) }
        )
        return AppEnvironment(
            settings: settings,
            store: store,
            refreshScheduler: refreshScheduler,
            loginItemManager: LifecycleLoginItemManager(),
            keyRepository: repository,
            apiClient: fetcher,
            notificationSender: notificationSender,
            applicationNotificationCenter: applicationNotificationCenter,
            updateService: updateService,
            updateCheckScheduler: updateCheckScheduler
        )
    }

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private actor LifecycleUsageFetcher: UsageFetching {
    enum Response: Sendable {
        case success(UsageSnapshot?)
        case failure(UsageAPIError)
    }

    private var responses: [String: Response] = [:]
    private var requestCounts: [String: Int] = [:]

    func setResponse(_ response: Response, for secret: String) {
        responses[secret] = response
    }

    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot? {
        requestCounts[apiKey, default: 0] += 1
        switch responses[apiKey] ?? .failure(.invalidResponse) {
        case let .success(snapshot):
            return snapshot
        case let .failure(error):
            throw error
        }
    }

    func requestCount(for secret: String) -> Int {
        requestCounts[secret, default: 0]
    }
}

private final class LifecycleKeychainFake: LocalKeyStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var secrets: [UUID: String] = [:]

    func save(_ secret: String, for id: UUID) throws {
        lock.withLock { secrets[id] = secret }
    }

    func read(for id: UUID) throws -> String? {
        lock.withLock { secrets[id] }
    }

    func delete(for id: UUID) throws {
        _ = lock.withLock { secrets.removeValue(forKey: id) }
    }
}

private final class LifecycleUsageCache: UsageCaching, @unchecked Sendable {
    private let lock = NSLock()
    private var snapshots: [UUID: UsageSnapshot] = [:]

    func load(for keyID: UUID) throws -> UsageSnapshot? {
        lock.withLock { snapshots[keyID] }
    }

    func save(_ snapshot: UsageSnapshot, for keyID: UUID) throws {
        lock.withLock { snapshots[keyID] = snapshot }
    }

    func delete(for keyID: UUID) throws {
        _ = lock.withLock { snapshots.removeValue(forKey: keyID) }
    }
}

private actor LifecycleNotificationSender: NotificationSending {
    private(set) var authorizationRequestCount = 0
    private var alerts: [UsageAlert] = []

    func requestAuthorization() async throws -> Bool {
        authorizationRequestCount += 1
        return true
    }

    func send(_ alert: UsageAlert) async throws {
        alerts.append(alert)
    }

    func sentAlerts() -> [UsageAlert] {
        alerts
    }
}

private actor SequencedLifecycleNotificationSender: NotificationSending {
    private var results: [Bool]
    private(set) var authorizationRequestCount = 0

    init(results: [Bool]) {
        self.results = results
    }

    func requestAuthorization() async throws -> Bool {
        authorizationRequestCount += 1
        return results.removeFirst()
    }

    func send(_: UsageAlert) async throws {}
}

@MainActor
private final class LifecycleRefreshSchedulerSpy: RefreshScheduling {
    private(set) var startedMinutes: [Int] = []
    private(set) var rescheduledMinutes: [Int] = []
    private(set) var stopCount = 0
    private var tick: (@Sendable () -> Void)?

    func start(minutes: Int, onTick: @escaping @Sendable () -> Void) {
        startedMinutes.append(minutes)
        tick = onTick
    }

    func reschedule(minutes: Int) {
        rescheduledMinutes.append(minutes)
    }

    func stop() {
        stopCount += 1
        tick = nil
    }

    func fireTick() {
        tick?()
    }
}

private actor LifecycleUpdateService: UpdateChecking {
    enum Result: Sendable {
        case none
        case available(AppUpdate)
        case failure(UpdateServiceError)
    }

    private var results: [Result]
    private(set) var checkCount = 0

    init(result: Result) {
        results = [result]
    }

    init(results: [Result]) {
        self.results = results
    }

    func checkForUpdate() async throws -> AppUpdate? {
        checkCount += 1
        switch results.isEmpty ? .none : results.removeFirst() {
        case .none:
            return nil
        case let .available(update):
            return update
        case let .failure(error):
            throw error
        }
    }

    func download(_: AppUpdate) async throws -> URL {
        throw UpdateServiceError.unavailable
    }
}

@MainActor
private final class LifecycleUpdateSchedulerSpy: UpdateCheckScheduling {
    private(set) var startCount = 0
    private(set) var stopCount = 0
    private var tick: (@Sendable () -> Void)?

    func start(onTick: @escaping @Sendable () -> Void) {
        startCount += 1
        tick = onTick
    }

    func stop() {
        stopCount += 1
        tick = nil
    }

    func fireTick() {
        tick?()
    }
}

private final class LifecycleTimerScheduler: TimerScheduling, @unchecked Sendable {
    private let lock = NSLock()
    private var scheduled: [(TimeInterval, LifecycleTimer)] = []

    var scheduledIntervals: [TimeInterval] {
        lock.withLock { scheduled.map(\.0) }
    }

    func schedule(
        every seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer {
        let timer = LifecycleTimer(action: action)
        lock.withLock { scheduled.append((seconds, timer)) }
        return timer
    }

    func timer(at index: Int) -> LifecycleTimer {
        lock.withLock { scheduled[index].1 }
    }
}

private final class LifecycleTimer: CancellableTimer, @unchecked Sendable {
    private let action: @Sendable () -> Void

    init(action: @escaping @Sendable () -> Void) {
        self.action = action
    }

    func cancel() {}

    func fire() {
        action()
    }
}

private struct LifecycleLoginItemManager: LoginItemManaging {
    var isEnabled: Bool { false }

    func setEnabled(_ enabled: Bool) throws {}
}

private final class EnabledLifecycleLoginItemManager: LoginItemManaging, @unchecked Sendable {
    var isEnabled = true

    func setEnabled(_ enabled: Bool) throws {
        isEnabled = enabled
    }
}
