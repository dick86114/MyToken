import Foundation

/// Quota health thresholds shared by the app, widget and provider adapters.
///
/// Mirrors `healthThresholds` in shared/provider-contracts/provider-capabilities.json
/// (enforced by UsageHealthThresholdsTests): warning from 80% used, critical from 95%.
/// Kept dependency-free so the widget extension can compile this file too.
enum UsageHealthThresholds {
    static let warningUsedPercent: Double = 80
    static let criticalUsedPercent: Double = 95
}
