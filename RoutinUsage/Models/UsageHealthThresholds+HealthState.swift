import Foundation

extension UsageHealthThresholds {
    /// Uses-based percentage → health state. Mirrors Android UsageHealthThresholds.
    static func healthState(forUsedPercent percent: Double) -> UsageMetricHealthState {
        guard percent.isFinite else { return .unknown }
        if percent >= criticalUsedPercent { return .critical }
        if percent >= warningUsedPercent { return .warning }
        return .normal
    }
}
