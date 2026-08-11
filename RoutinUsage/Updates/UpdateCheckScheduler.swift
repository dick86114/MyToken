import Foundation

@MainActor
protocol UpdateCheckScheduling: AnyObject {
    func start(onTick: @escaping @Sendable () -> Void)
    func stop()
}

@MainActor
final class UpdateCheckScheduler: UpdateCheckScheduling {
    private let interval: TimeInterval
    private var task: Task<Void, Never>?

    init(interval: TimeInterval = 21_600) {
        self.interval = interval
    }

    func start(onTick: @escaping @Sendable () -> Void) {
        guard task == nil else { return }

        let interval = interval
        task = Task {
            while !Task.isCancelled {
                do {
                    try await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
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
}
