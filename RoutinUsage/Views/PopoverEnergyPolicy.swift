import Foundation

/// 刷新边框动画只在可见且系统未进入省电状态时运行。
enum RefreshAnimationPolicy {
    static let minimumInterval: TimeInterval = 1.0 / 12.0

    static func isPaused(
        reduceMotion: Bool,
        lowPowerModeEnabled: Bool
    ) -> Bool {
        reduceMotion || lowPowerModeEnabled
    }

    static func phase(for date: Date, orbitPeriod: Double = 2.4) -> CGFloat {
        CGFloat(
            (date.timeIntervalSinceReferenceDate / orbitPeriod)
                .truncatingRemainder(dividingBy: 1)
        )
    }
}
