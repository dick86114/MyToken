import Foundation

@MainActor
protocol UpdateCheckScheduling: AnyObject {
    func start(onTick: @escaping @Sendable () -> Void)
    func stop()
}

protocol UpdateCheckWaiting: Sendable {
    func sleep(for interval: TimeInterval) async throws
}

struct TaskUpdateCheckWaiter: UpdateCheckWaiting {
    func sleep(for interval: TimeInterval) async throws {
        try await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
    }
}

@MainActor
final class UpdateCheckScheduler: UpdateCheckScheduling {
    private let interval: TimeInterval
    private let waiter: any UpdateCheckWaiting
    private var task: Task<Void, Never>?

    init(
        interval: TimeInterval = 21_600,
        waiter: any UpdateCheckWaiting = TaskUpdateCheckWaiter()
    ) {
        self.interval = interval
        self.waiter = waiter
    }

    func start(onTick: @escaping @Sendable () -> Void) {
        guard task == nil else { return }

        let interval = interval
        let waiter = waiter
        task = Task {
            while !Task.isCancelled {
                do {
                    try await waiter.sleep(for: interval)
                } catch {
                    return
                }
                guard !Task.isCancelled else { return }
                onTick()
            }
        }
    }

    func stop() {
        task?.cancel()
        task = nil
    }

    deinit {
        task?.cancel()
    }
}
