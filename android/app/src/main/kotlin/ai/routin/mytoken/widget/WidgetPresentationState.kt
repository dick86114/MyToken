package ai.routin.mytoken.widget

enum class WidgetPresentationStatus {
    Ready,
    Loading,
    Stale,
    RefreshFailed,
    AuthenticationFailed,
    Disabled,
    MissingCredential,
    NoMetrics,
    FixedMetricUnavailable,
}

enum class WidgetFailureKind {
    Authentication,
    Other,
}

object WidgetPresentationStateResolver {
    fun resolve(
        isEnabled: Boolean,
        hasSnapshot: Boolean,
        isStale: Boolean,
        isRefreshing: Boolean,
        failure: WidgetFailureKind?,
        metricCount: Int,
        fixedMetricUnavailable: Boolean,
    ): WidgetPresentationStatus {
        if (!isEnabled) return WidgetPresentationStatus.Disabled
        if (failure == WidgetFailureKind.Authentication) {
            return WidgetPresentationStatus.AuthenticationFailed
        }
        if (!hasSnapshot || isRefreshing) return WidgetPresentationStatus.Loading
        if (failure != null) return WidgetPresentationStatus.RefreshFailed
        if (isStale) return WidgetPresentationStatus.Stale
        if (metricCount == 0) return WidgetPresentationStatus.NoMetrics
        if (fixedMetricUnavailable) return WidgetPresentationStatus.FixedMetricUnavailable
        return WidgetPresentationStatus.Ready
    }

    fun copy(status: WidgetPresentationStatus): String = when (status) {
        WidgetPresentationStatus.Ready -> "数据已更新"
        WidgetPresentationStatus.Loading -> "正在获取"
        WidgetPresentationStatus.Stale -> "缓存数据"
        WidgetPresentationStatus.RefreshFailed -> "更新失败 · 显示上次成功数据"
        WidgetPresentationStatus.AuthenticationFailed -> "需要重新登录/更换凭证"
        WidgetPresentationStatus.Disabled -> "已停用 · 显示缓存"
        WidgetPresentationStatus.MissingCredential -> "凭证不存在，请重新配置"
        WidgetPresentationStatus.NoMetrics -> "该套餐暂无可用指标"
        WidgetPresentationStatus.FixedMetricUnavailable -> "主指标暂不可用"
    }

    fun usesHighUsageColor(status: WidgetPresentationStatus): Boolean =
        status != WidgetPresentationStatus.Stale
}
