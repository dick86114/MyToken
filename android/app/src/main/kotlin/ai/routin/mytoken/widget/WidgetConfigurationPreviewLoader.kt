package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.UsageSnapshot
import java.util.UUID

/** Loads preview data for widget configuration, preferring memory over the persisted cache. */
internal suspend fun loadWidgetConfigurationSnapshots(
    credentialIds: List<UUID>,
    stateSnapshot: (UUID) -> UsageSnapshot?,
    cachedSnapshot: suspend (UUID) -> UsageSnapshot?,
): Map<UUID, UsageSnapshot?> = credentialIds.associateWith { id ->
    stateSnapshot(id) ?: cachedSnapshot(id)
}
