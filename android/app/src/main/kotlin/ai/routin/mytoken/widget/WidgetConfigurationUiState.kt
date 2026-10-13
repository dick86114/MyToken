package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.util.UUID

data class WidgetConfigurationUiState(
    val selectedStyle: WidgetDisplayStyle,
    val selectedOverviewSource: WidgetOverviewSource,
    val selectedCredentialId: UUID?,
    val selectedPrimaryMetricId: String?,
    val allowedStyles: List<WidgetDisplayStyle>,
    val allowedOverviewSources: List<WidgetOverviewSource>,
    val selectableCredentials: List<Credential>,
    val overviewCredentials: List<Credential>,
    val preview: WidgetPresentation?,
    val supportsSecondaryMetricOrdering: Boolean = false,
    val supportsColorTheme: Boolean = false,
    val supportsRefreshInterval: Boolean = false,
)

fun resolveConfigurationUiState(
    config: WidgetConfig,
    credentials: List<Credential>,
    snapshots: Map<UUID, UsageSnapshot?>,
    size: WidgetSizeKind = WidgetSizeKind.Standard,
): WidgetConfigurationUiState {
    val enabledCredentials = credentials.filter(Credential::isEnabled)
    val overviewCredentials = when (config.overviewSource) {
        WidgetOverviewSource.AllEnabled -> enabledCredentials.sortedBy(Credential::sortOrder)
        WidgetOverviewSource.Manual -> {
            val byId = credentials.associateBy(Credential::id)
            config.limitedOverviewCredentialIds
                .mapNotNull(byId::get)
                .filter(Credential::isEnabled)
        }
    }
    val preview = when (config.kind) {
        WidgetKind.Credential -> config.credentialId
            ?.let { id -> credentials.firstOrNull { it.id == id } }
            ?.let { credential ->
                WidgetPresentationBuilder.makeCredentialPresentation(
                    credential = credential,
                    snapshot = snapshots[credential.id],
                    style = config.style,
                    size = size,
                    fixedMetricId = config.primaryMetricId,
                )
            }
        WidgetKind.Overview -> WidgetPresentationBuilder.makeOverviewPresentation(
            entries = overviewCredentials.map { it to snapshots[it.id] },
            style = config.style,
            size = size,
        )
    }
    return WidgetConfigurationUiState(
        selectedStyle = config.style,
        selectedOverviewSource = config.overviewSource,
        selectedCredentialId = config.credentialId,
        selectedPrimaryMetricId = config.primaryMetricId,
        allowedStyles = WidgetDisplayStyle.entries,
        allowedOverviewSources = WidgetOverviewSource.entries,
        selectableCredentials = enabledCredentials.sortedBy(Credential::sortOrder),
        overviewCredentials = overviewCredentials,
        preview = preview,
    )
}
