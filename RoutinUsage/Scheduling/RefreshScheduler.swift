import AppKit
import Foundation

protocol CancellableTimer: Sendable {
    func cancel()
}

protocol TimerScheduling: Sendable {
    func schedule(
        every seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer
}

struct MainRunLoopTimerScheduler: TimerScheduling {
    func schedule(
        every seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) -> any CancellableTimer {
        let timer = MainRunLoopTimer()
        DispatchQueue.main.async {
            timer.install(every: seconds, action: action)
        }
        return timer
    }
}

@MainActor
final class RefreshScheduler {
    private let timerScheduler: any TimerScheduling
    private let wakeDebounceSeconds: TimeInterval

    private var periodicTimer: (any CancellableTimer)?
    private var wakeTimer: (any CancellableTimer)?
    private var onTick: (@Sendable () -> Void)?
    private var periodicGeneration = 0
    private var wakeGeneration = 0
    private var wakeObservation: WakeNotificationObservation?

    init(
        timerScheduler: any TimerScheduling = MainRunLoopTimerScheduler(),
        workspaceNotificationCenter: NotificationCenter = NSWorkspace.shared.notificationCenter,
        wakeDebounceSeconds: TimeInterval = 2
    ) {
        self.timerScheduler = timerScheduler
        self.wakeDebounceSeconds = wakeDebounceSeconds
        wakeObservation = nil
        wakeObservation = WakeNotificationObservation(
            notificationCenter: workspaceNotificationCenter
        ) { [weak self] in
            Task { @MainActor [weak self] in
                self?.handleWake()
            }
        }
    }

    deinit {
        periodicTimer?.cancel()
        wakeTimer?.cancel()
    }

    func start(minutes: Int, onTick: @escaping @Sendable () -> Void) {
        guard AppSettings.allowedRefreshMinutes.contains(minutes) else {
            return
        }
        self.onTick = onTick
        installPeriodicTimer(minutes: minutes)
    }

    func reschedule(minutes: Int) {
        guard AppSettings.allowedRefreshMinutes.contains(minutes) else {
            return
        }
        guard onTick != nil else {
            return
        }
        installPeriodicTimer(minutes: minutes)
    }

    func handleWake() {
        wakeGeneration += 1
        let generation = wakeGeneration
        let oldTimer = wakeTimer
        wakeTimer = nil
        oldTimer?.cancel()
        guard onTick != nil else {
            return
        }

        let newTimer = timerScheduler.schedule(every: wakeDebounceSeconds) { [weak self] in
            MainActor.assumeIsolated {
                self?.fireWake(generation: generation)
            }
        }
        guard wakeGeneration == generation, onTick != nil else {
            newTimer.cancel()
            return
        }
        wakeTimer = newTimer
    }

    func stop() {
        periodicGeneration += 1
        wakeGeneration += 1
        onTick = nil
        let timers = (periodicTimer, wakeTimer)
        periodicTimer = nil
        wakeTimer = nil
        timers.0?.cancel()
        timers.1?.cancel()
    }

    private func installPeriodicTimer(minutes: Int) {
        periodicGeneration += 1
        let generation = periodicGeneration
        let oldTimer = periodicTimer
        periodicTimer = nil
        oldTimer?.cancel()

        let newTimer = timerScheduler.schedule(every: TimeInterval(minutes * 60)) {
            [weak self] in
            MainActor.assumeIsolated {
                self?.firePeriodic(generation: generation)
            }
        }
        guard periodicGeneration == generation, onTick != nil else {
            newTimer.cancel()
            return
        }
        periodicTimer = newTimer
    }

    private func firePeriodic(generation: Int) {
        guard periodicGeneration == generation else {
            return
        }
        onTick?()
    }

    private func fireWake(generation: Int) {
        guard wakeGeneration == generation else {
            return
        }
        wakeGeneration += 1
        let timer = wakeTimer
        wakeTimer = nil
        timer?.cancel()
        onTick?()
    }
}

private final class WakeNotificationObservation: @unchecked Sendable {
    private let notificationCenter: NotificationCenter
    private var token: NSObjectProtocol?

    init(
        notificationCenter: NotificationCenter,
        action: @escaping @Sendable () -> Void
    ) {
        self.notificationCenter = notificationCenter
        token = notificationCenter.addObserver(
            forName: NSWorkspace.didWakeNotification,
            object: nil,
            queue: nil
        ) { _ in
            action()
        }
    }

    deinit {
        if let token {
            notificationCenter.removeObserver(token)
        }
    }
}

private final class MainRunLoopTimer: CancellableTimer, @unchecked Sendable {
    private let lock = NSLock()
    private var timer: Timer?
    private var isCancelled = false

    func install(
        every seconds: TimeInterval,
        action: @escaping @Sendable () -> Void
    ) {
        dispatchPrecondition(condition: .onQueue(.main))
        let timer = Timer(timeInterval: seconds, repeats: true) { _ in
            action()
        }
        let shouldInvalidate = lock.withLock { () -> Bool in
            guard !isCancelled else {
                return true
            }
            self.timer = timer
            return false
        }
        if shouldInvalidate {
            timer.invalidate()
        } else {
            RunLoop.main.add(timer, forMode: .common)
        }
    }

    func cancel() {
        let shouldDispatch = lock.withLock { () -> Bool in
            guard !isCancelled else {
                return false
            }
            isCancelled = true
            return true
        }
        guard shouldDispatch else {
            return
        }
        if Thread.isMainThread {
            invalidateOnMainRunLoop()
        } else {
            DispatchQueue.main.async {
                self.invalidateOnMainRunLoop()
            }
        }
    }

    private func invalidateOnMainRunLoop() {
        dispatchPrecondition(condition: .onQueue(.main))
        let timer = lock.withLock {
            let timer = self.timer
            self.timer = nil
            return timer
        }
        timer?.invalidate()
    }
}
