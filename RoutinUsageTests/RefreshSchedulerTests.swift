import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class RefreshSchedulerTests: XCTestCase {
    func test生产Timer取消并释放句柄后不再触发() {
        let tickCount = LockedCounter()
        var timer: (any CancellableTimer)? = MainRunLoopTimerScheduler().schedule(
            every: 0.01,
            tolerance: 0
        ) {
            tickCount.increment()
        }
        let firstTickDeadline = Date().addingTimeInterval(1)
        while tickCount.value == 0, Date() < firstTickDeadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.01))
        }
        XCTAssertGreaterThan(tickCount.value, 0)

        timer?.cancel()
        timer = nil
        let countWhenCancelled = tickCount.value
        RunLoop.main.run(until: Date().addingTimeInterval(0.08))

        XCTAssertEqual(tickCount.value, countWhenCancelled)
    }

    func test启动五分钟调度三百秒周期并在到期时刷新() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        let refreshed = expectation(description: "完成周期刷新")

        scheduler.start(minutes: 5) {
            tickCount.increment()
            refreshed.fulfill()
        }

        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.interval), [300])
        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.tolerance), [60])
        timerScheduler.timer(at: 0).fire()
        await fulfillment(of: [refreshed], timeout: 1)
        XCTAssertEqual(tickCount.value, 1)
    }

    func test后台触发周期回调会安全切换到主执行器刷新() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let refreshed = expectation(description: "主执行器完成周期刷新")
        scheduler.start(minutes: 5) {
            XCTAssertTrue(Thread.isMainThread)
            refreshed.fulfill()
        }

        await timerScheduler.timer(at: 0).fireFromBackground()
        await fulfillment(of: [refreshed], timeout: 1)
    }

    func test改为十五分钟会取消旧任务并安排九百秒() {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        scheduler.start(minutes: 5) {}
        let oldTimer = timerScheduler.timer(at: 0)

        scheduler.reschedule(minutes: 15)

        XCTAssertEqual(oldTimer.cancelCount, 1)
        XCTAssertEqual(timerScheduler.scheduledIntervals, [300, 900])
    }

    func test周期Timer使用间隔的两成作为tolerance() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )

        scheduler.start(minutes: 1) {}
        scheduler.reschedule(minutes: 30)

        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.interval), [60, 1800])
        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.tolerance), [12, 360])
    }

    func test停止会取消周期和待处理唤醒且之后不再刷新() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        let unexpectedRefresh = expectation(description: "停止后不应刷新")
        unexpectedRefresh.isInverted = true
        scheduler.start(minutes: 5) {
            tickCount.increment()
            unexpectedRefresh.fulfill()
        }
        scheduler.handleWake()
        let periodicTimer = timerScheduler.timer(at: 0)
        let wakeTimer = timerScheduler.timer(at: 1)

        scheduler.stop()
        periodicTimer.fire()
        wakeTimer.fire()
        await fulfillment(of: [unexpectedRefresh], timeout: 0.05)

        XCTAssertEqual(periodicTimer.cancelCount, 1)
        XCTAssertEqual(wakeTimer.cancelCount, 1)
        XCTAssertEqual(tickCount.value, 0)
    }

    func test连续两个唤醒事件以两秒去抖且只补刷一次() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        let refreshed = expectation(description: "完成一次唤醒补刷")
        let periodicRefreshed = expectation(description: "唤醒后完成重置的周期刷新")
        let unexpectedRefresh = expectation(description: "不应重复唤醒补刷")
        unexpectedRefresh.isInverted = true
        scheduler.start(minutes: 5) {
            let count = tickCount.increment()
            if count == 1 {
                refreshed.fulfill()
            } else if count == 2 {
                periodicRefreshed.fulfill()
            } else {
                unexpectedRefresh.fulfill()
            }
        }

        scheduler.handleWake()
        let firstWakeTimer = timerScheduler.timer(at: 1)
        scheduler.handleWake()
        let secondWakeTimer = timerScheduler.timer(at: 2)

        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.interval), [300, 2, 2])
        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.isOneShot), [false, true, true])
        XCTAssertEqual(timerScheduler.scheduledRequests.map(\.tolerance), [60, 0, 0])
        XCTAssertEqual(firstWakeTimer.cancelCount, 1)

        let oldPeriodicTimer = timerScheduler.timer(at: 0)
        firstWakeTimer.fire()
        XCTAssertEqual(tickCount.value, 0)

        secondWakeTimer.fire()
        await fulfillment(of: [refreshed], timeout: 1)
        await fulfillment(of: [unexpectedRefresh], timeout: 0.05)
        XCTAssertEqual(tickCount.value, 1)
        XCTAssertEqual(secondWakeTimer.cancelCount, 1)
        XCTAssertEqual(oldPeriodicTimer.cancelCount, 1)
        XCTAssertEqual(timerScheduler.scheduledRequests.count, 4)
        XCTAssertEqual(timerScheduler.scheduledRequests.last?.interval, 300)

        timerScheduler.timer(at: 3).fire()
        await fulfillment(of: [periodicRefreshed], timeout: 1)
        XCTAssertEqual(tickCount.value, 2)
    }

    func test后台触发唤醒回调会安全切换到主执行器补刷并取消定时器() async {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        let refreshed = expectation(description: "主执行器完成唤醒补刷")
        scheduler.start(minutes: 5) {
            XCTAssertTrue(Thread.isMainThread)
            tickCount.increment()
            refreshed.fulfill()
        }
        scheduler.handleWake()
        let wakeTimer = timerScheduler.timer(at: 1)

        await wakeTimer.fireFromBackground()
        await fulfillment(of: [refreshed], timeout: 1)

        XCTAssertEqual(tickCount.value, 1)
        XCTAssertEqual(wakeTimer.cancelCount, 1)
    }
}

private struct ScheduledTimerRequest {
    let interval: TimeInterval
    let tolerance: TimeInterval
    let isOneShot: Bool
    let timer: TimerSpy
}

private final class TimerSchedulerSpy: TimerScheduling, @unchecked Sendable {
    private let lock = NSLock()
    private var scheduled: [ScheduledTimerRequest] = []

    var scheduledRequests: [ScheduledTimerRequest] {
        lock.withLock { scheduled }
    }

    var scheduledIntervals: [TimeInterval] {
        scheduledRequests.map(\.interval)
    }

    func schedule(
        every seconds: TimeInterval,
        tolerance: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer {
        let timer = TimerSpy(action: action)
        lock.withLock {
            scheduled.append(ScheduledTimerRequest(
                interval: seconds,
                tolerance: tolerance,
                isOneShot: false,
                timer: timer
            ))
        }
        return timer
    }

    func scheduleOneShot(
        after seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer {
        let timer = TimerSpy(action: action)
        lock.withLock {
            scheduled.append(ScheduledTimerRequest(
                interval: seconds,
                tolerance: 0,
                isOneShot: true,
                timer: timer
            ))
        }
        return timer
    }

    func timer(at index: Int) -> TimerSpy {
        lock.withLock { scheduled[index].timer }
    }
}

private final class TimerSpy: CancellableTimer, @unchecked Sendable {
    private let lock = NSLock()
    private let action: @Sendable () -> Void
    private var storedCancelCount = 0

    var cancelCount: Int {
        lock.withLock { storedCancelCount }
    }

    init(action: @escaping @Sendable () -> Void) {
        self.action = action
    }

    func cancel() {
        lock.withLock {
            storedCancelCount += 1
        }
    }

    func fire() {
        action()
    }

    func fireFromBackground() async {
        await withCheckedContinuation { continuation in
            DispatchQueue.global().async {
                self.fire()
                continuation.resume()
            }
        }
    }
}

private final class LockedCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var storedValue = 0

    var value: Int {
        lock.withLock { storedValue }
    }

    @discardableResult
    func increment() -> Int {
        lock.withLock {
            storedValue += 1
            return storedValue
        }
    }
}
