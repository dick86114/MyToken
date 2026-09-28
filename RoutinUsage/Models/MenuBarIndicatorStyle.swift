import Foundation

/// 单个凭证在菜单栏中使用的指标呈现样式。
enum MenuBarIndicatorStyle: String, CaseIterable, Codable, Equatable, Sendable {
    case progressBar
    case stacked = "shortCodePercent"

    static func availableStyles(
        for metric: NormalizedUsageMetric?
    ) -> [Self] {
        guard let metric else {
            return [.progressBar]
        }
        switch metric.semantic {
        case .usedQuota, .remainingQuota:
            return metric.displayedPercent == nil
                ? [.progressBar]
                : [.progressBar, .stacked]
        case .balance:
            return [.progressBar, .stacked]
        case .status, .value:
            return [.progressBar]
        }
    }

    func title(for metric: NormalizedUsageMetric?) -> String {
        let semantic = metric?.semantic
        switch self {
        case .progressBar:
            switch semantic {
            case .balance:
                return "余额圆圈"
            case .status, .value, .none:
                return "状态文字"
            case .usedQuota, .remainingQuota:
                return "进度条"
            }
        case .stacked:
            switch semantic {
            case .balance:
                return "短码 + 余额"
            case .usedQuota, .remainingQuota:
                return "短码 + 百分比"
            case .status, .value, .none:
                return "短码 + 数值"
            }
        }
    }

    func systemImage(for metric: NormalizedUsageMetric?) -> String {
        switch self {
        case .progressBar:
            return metric?.semantic == .balance ? "circle" : "chart.bar.fill"
        case .stacked:
            return metric?.semantic == .balance ? "banknote" : "textformat.123"
        }
    }
}
