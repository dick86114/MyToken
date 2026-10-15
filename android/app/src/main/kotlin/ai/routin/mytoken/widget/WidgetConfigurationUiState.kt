package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.util.UUID

data class WidgetConfigurationUiState(
    val selectedCredentialId: UUID?,
    val selectedOverviewSource: WidgetOverviewSource,
    val selectableCredentials: List<Credential>,
    val overviewCredentials: List<Credential>,
    val preview: WidgetPresentation?,
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
                    size = size,
                )
            }
        WidgetKind.Overview -> WidgetPresentationBuilder.makeOverviewPresentation(
            entries = overviewCredentials.map { it to snapshots[it.id] },
            size = size,
        )
    }
    return WidgetConfigurationUiState(
        selectedCredentialId = config.credentialId,
        selectedOverviewSource = config.overviewSource,
        selectableCredentials = enabledCredentials.sortedBy(Credential::sortOrder),
        overviewCredentials = overviewCredentials,
        preview = preview,
    )
}
