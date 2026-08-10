import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class RefreshSchedulerTests: XCTestCase {
    func test生产Timer取消并释放句柄后不再触发() {
        let tickCount = LockedCounter()
        var timer: (any CancellableTimer)? = MainRunLoopTimerScheduler().schedule(
            every: 0.01
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

    func test启动五分钟调度三百秒周期并在到期时刷新() {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()

        scheduler.start(minutes: 5) {
            tickCount.increment()
        }

        XCTAssertEqual(timerScheduler.scheduledIntervals, [300])
        timerScheduler.timer(at: 0).fire()
        XCTAssertEqual(tickCount.value, 1)
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

    func test停止会取消周期和待处理唤醒且之后不再刷新() {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        scheduler.start(minutes: 5) {
            tickCount.increment()
        }
        scheduler.handleWake()
        let periodicTimer = timerScheduler.timer(at: 0)
        let wakeTimer = timerScheduler.timer(at: 1)

        scheduler.stop()
        periodicTimer.fire()
        wakeTimer.fire()

        XCTAssertEqual(periodicTimer.cancelCount, 1)
        XCTAssertEqual(wakeTimer.cancelCount, 1)
        XCTAssertEqual(tickCount.value, 0)
    }

    func test连续两个唤醒事件以两秒去抖且只补刷一次() {
        let timerScheduler = TimerSchedulerSpy()
        let scheduler = RefreshScheduler(
            timerScheduler: timerScheduler,
            workspaceNotificationCenter: NotificationCenter()
        )
        let tickCount = LockedCounter()
        scheduler.start(minutes: 5) {
            tickCount.increment()
        }

        scheduler.handleWake()
        let firstWakeTimer = timerScheduler.timer(at: 1)
        scheduler.handleWake()
        let secondWakeTimer = timerScheduler.timer(at: 2)

        XCTAssertEqual(timerScheduler.scheduledIntervals, [300, 2, 2])
        XCTAssertEqual(firstWakeTimer.cancelCount, 1)
        firstWakeTimer.fire()
        secondWakeTimer.fire()
        secondWakeTimer.fire()
        XCTAssertEqual(tickCount.value, 1)
        XCTAssertEqual(secondWakeTimer.cancelCount, 1)
    }
}

private final class TimerSchedulerSpy: TimerScheduling, @unchecked Sendable {
    private let lock = NSLock()
    private var scheduled: [(interval: TimeInterval, timer: TimerSpy)] = []

    var scheduledIntervals: [TimeInterval] {
        lock.withLock { scheduled.map(\.interval) }
    }

    func schedule(
        every seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer {
        let timer = TimerSpy(action: action)
        lock.withLock {
            scheduled.append((seconds, timer))
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
}

private final class LockedCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var storedValue = 0

    var value: Int {
        lock.withLock { storedValue }
    }

    func increment() {
        lock.withLock {
            storedValue += 1
        }
    }
}
