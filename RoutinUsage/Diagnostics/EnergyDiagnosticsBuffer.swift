import Foundation

/// 面向能耗排查的内存诊断事件；只保留最近的少量聚合计数，不写入磁盘。
struct EnergyDiagnosticsEvent: Equatable, Sendable {
    let timestamp: Date
    let name: String
    let details: [String: String]
}

actor EnergyDiagnosticsBuffer {
    static let shared = EnergyDiagnosticsBuffer()

    private var events: [EnergyDiagnosticsEvent] = []
    private let maximumEventCount: Int
    private let now: @Sendable () -> Date

    init(
        maximumEventCount: Int = 200,
        now: @escaping @Sendable () -> Date = { Date() }
    ) {
        self.maximumEventCount = max(1, maximumEventCount)
        self.now = now
    }

    func record(name: String, details: [String: String] = [:]) {
        events.append(EnergyDiagnosticsEvent(timestamp: now(), name: name, details: details))
        if events.count > maximumEventCount {
            events.removeFirst(events.count - maximumEventCount)
        }
    }

    func currentEvents() -> [EnergyDiagnosticsEvent] {
        events
    }
}
