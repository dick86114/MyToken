import Foundation

enum WidgetPresentationStatus: String, Equatable, Sendable, CaseIterable {
    case ready
    case loading
    case stale
    case refreshFailed
    case authenticationFailed
    case disabled
    case missingCredential
    case noMetrics
    case fixedMetricUnavailable
}

enum WidgetPresentationStateResolver {
    static func resolve(
        isEnabled: Bool,
        hasSnapshot: Bool,
        isStale: Bool,
        isRefreshing: Bool,
        failure: WidgetSnapshotFailure?,
        metricCount: Int,
        fixedMetricUnavailable: Bool
    ) -> WidgetPresentationStatus {
        guard isEnabled else { return .disabled }
        if failure == .authentication { return .authenticationFailed }
        if !hasSnapshot || isRefreshing { return .loading }
        if failure != nil { return .refreshFailed }
        if isStale { return .stale }
        if metricCount == 0 { return .noMetrics }
        if fixedMetricUnavailable { return .fixedMetricUnavailable }
        return .ready
    }

    static func copy(for status: WidgetPresentationStatus) -> String {
        switch status {
        case .ready: return "数据已更新"
        case .loading: return "正在获取"
        case .stale: return "缓存数据"
        case .refreshFailed: return "更新失败 · 显示上次成功数据"
        case .authenticationFailed: return "需要重新登录/更换凭证"
        case .disabled: return "已停用 · 显示缓存"
        case .missingCredential: return "凭证不存在，请重新配置"
        case .noMetrics: return "该套餐暂无可用指标"
        case .fixedMetricUnavailable: return "主指标暂不可用"
        }
    }
}

enum WidgetTextRole: String, CaseIterable, Sendable {
    case providerName
    case credentialName
    case planName
    case metricLabel
    case metricValue
    case statusText
}

enum WidgetTextLayoutPolicy {
    static func lineLimit(for role: WidgetTextRole) -> Int {
        1
    }

    static func usesTailTruncation(for role: WidgetTextRole) -> Bool {
        switch role {
        case .providerName, .credentialName, .planName, .metricLabel:
            return true
        case .metricValue, .statusText:
            return false
        }
    }
}
