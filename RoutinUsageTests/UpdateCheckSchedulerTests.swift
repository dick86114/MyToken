import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class UpdateCheckSchedulerTests: XCTestCase {
    func test默认六小时等待后会持续触发更新检查() async {
        let waiter = ControlledUpdateCheckWaiter()
        let tickCounter = UpdateCheckTickCounter()
        let scheduler = UpdateCheckScheduler(waiter: waiter)

        scheduler.start {
            Task { await tickCounter.increment() }
        }
        await waiter.waitUntilSleepCount(reaches: 1)
        let intervals = await waiter.recordedIntervals()
        XCTAssertEqual(intervals, [21_600])

        await waiter.resumeNextSleep()
        await tickCounter.waitUntilCount(reaches: 1)
        await waiter.waitUntilSleepCount(reaches: 2)
        await waiter.resumeNextSleep()
        await tickCounter.waitUntilCount(reaches: 2)
        scheduler.stop()
    }

    func test停止会取消等待且之后不触发更新检查() async {
        let waiter = ControlledUpdateCheckWaiter()
        let tickCounter = UpdateCheckTickCounter()
        let scheduler = UpdateCheckScheduler(waiter: waiter)

        scheduler.start {
            Task { await tickCounter.increment() }
        }
        await waiter.waitUntilSleepCount(reaches: 1)
        scheduler.stop()
        await waiter.waitUntilCancellation()
        await Task.yield()

        let tickCount = await tickCounter.currentCount()
        XCTAssertEqual(tickCount, 0)
    }

    func test释放调度器会取消等待且之后不触发更新检查() async {
        let waiter = ControlledUpdateCheckWaiter()
        let tickCounter = UpdateCheckTickCounter()
        weak var releasedScheduler: UpdateCheckScheduler?

        do {
            let scheduler = UpdateCheckScheduler(waiter: waiter)
            releasedScheduler = scheduler
            scheduler.start {
                Task { await tickCounter.increment() }
            }
            await waiter.waitUntilSleepCount(reaches: 1)
        }
        await waiter.waitUntilCancellation()
        await Task.yield()

        XCTAssertNil(releasedScheduler)
        let tickCount = await tickCounter.currentCount()
        XCTAssertEqual(tickCount, 0)
    }
}

private actor ControlledUpdateCheckWaiter: UpdateCheckWaiting {
    private var sleepContinuations: [CheckedContinuation<Void, Error>] = []
    private var sleepCountWaiters: [(Int, CheckedContinuation<Void, Never>)] = []
    private var cancellationWaiters: [CheckedContinuation<Void, Never>] = []
    private(set) var intervals: [TimeInterval] = []
    private var cancellationCount = 0

    func sleep(for interval: TimeInterval) async throws {
        intervals.append(interval)
        resumeSleepCountWaiters()
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                sleepContinuations.append(continuation)
            }
        } onCancel: {
            Task { await self.cancelPendingSleep() }
        }
    }

    func waitUntilSleepCount(reaches expectedCount: Int) async {
        guard intervals.count < expectedCount else { return }
        await withCheckedContinuation { continuation in
            sleepCountWaiters.append((expectedCount, continuation))
        }
    }

    func resumeNextSleep() {
        guard !sleepContinuations.isEmpty else { return }
        sleepContinuations.removeFirst().resume()
    }

    func recordedIntervals() -> [TimeInterval] {
        intervals
    }

    func waitUntilCancellation() async {
        guard cancellationCount == 0 else { return }
        await withCheckedContinuation { continuation in
            cancellationWaiters.append(continuation)
        }
    }

    private func cancelPendingSleep() {
        cancellationCount += 1
        let continuations = sleepContinuations
        sleepContinuations.removeAll()
        continuations.forEach { $0.resume(throwing: CancellationError()) }
        let waiters = cancellationWaiters
        cancellationWaiters.removeAll()
        waiters.forEach { $0.resume() }
    }

    private func resumeSleepCountWaiters() {
        let readyWaiters = sleepCountWaiters.filter { $0.0 <= intervals.count }
        sleepCountWaiters.removeAll { $0.0 <= intervals.count }
        readyWaiters.forEach { $0.1.resume() }
    }
}

private actor UpdateCheckTickCounter {
    private(set) var count = 0
    private var waiters: [(Int, CheckedContinuation<Void, Never>)] = []

    func increment() {
        count += 1
        let readyWaiters = waiters.filter { $0.0 <= count }
        waiters.removeAll { $0.0 <= count }
        readyWaiters.forEach { $0.1.resume() }
    }

    func waitUntilCount(reaches expectedCount: Int) async {
        guard count < expectedCount else { return }
        await withCheckedContinuation { continuation in
            waiters.append((expectedCount, continuation))
        }
    }

    func currentCount() -> Int {
        count
    }
}
