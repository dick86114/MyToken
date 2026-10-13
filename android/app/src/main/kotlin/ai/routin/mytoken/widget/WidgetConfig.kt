package ai.routin.mytoken.widget

import java.util.UUID

enum class WidgetKind {
    Credential,
    Overview,
}

enum class WidgetDisplayStyle {
    Auto,
    FocusRing,
    ScanBand,
    SignalTiles,
}

enum class WidgetOverviewSource {
    AllEnabled,
    Manual,
}

enum class WidgetSizeKind {
    Compact,
    Standard,
    Expanded,
}

data class WidgetConfig(
    val kind: WidgetKind = WidgetKind.Credential,
    val credentialId: UUID? = null,
    val overviewCredentialIds: List<UUID> = emptyList(),
    val style: WidgetDisplayStyle = WidgetDisplayStyle.Auto,
    val primaryMetricId: String? = null,
    val overviewSource: WidgetOverviewSource = WidgetOverviewSource.AllEnabled,
) {
    val limitedOverviewCredentialIds: List<UUID>
        get() = overviewCredentialIds.distinct().take(MAX_OVERVIEW_CREDENTIALS)

    companion object {
        const val MAX_OVERVIEW_CREDENTIALS = 6
    }
}
