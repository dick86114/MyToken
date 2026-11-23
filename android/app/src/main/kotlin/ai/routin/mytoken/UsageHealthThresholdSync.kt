package ai.routin.mytoken

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.domain.model.UsageHealthThresholdConfig
import ai.routin.mytoken.domain.usage.UsageHealthThresholdSource
import ai.routin.mytoken.feature.settings.NotificationSettings
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect

/** Effective global + per-credential thresholds assembled from the settings flows. */
data class AppliedHealthThresholds(
    val global: UsageHealthThresholdConfig,
    val perCredential: Map<UUID, UsageHealthThresholdConfig>,
)

/**
 * App-layer, thread-safe holder read by [ai.routin.mytoken.domain.usage.RefreshCredentialsUseCase]
 * at fetch/normalize time. Values are updated from the DataStore flows, so the
 * use case never captures a stale constructor-time threshold snapshot.
 */
class AppUsageHealthThresholdSource : UsageHealthThresholdSource {

    @Volatile
    private var global: UsageHealthThresholdConfig = UsageHealthThresholdConfig.DEFAULT

    @Volatile
    private var perCredential: Map<UUID, UsageHealthThresholdConfig> = emptyMap()

    override fun thresholdsFor(credentialId: UUID): UsageHealthThresholdConfig =
        perCredential[credentialId] ?: global

    fun update(thresholds: AppliedHealthThresholds) {
        global = thresholds.global
        perCredential = thresholds.perCredential
    }
}

/** Maps the two settings flows into effective thresholds, falling back safely if data is corrupt. */
fun appliedHealthThresholds(
    settings: NotificationSettings,
    overrides: Map<UUID, AlertThresholds>,
): AppliedHealthThresholds = AppliedHealthThresholds(
    global = healthThresholdConfig(
        settings.lowThresholdPercent,
        settings.highThresholdPercent,
    ),
    perCredential = overrides.mapValues { (_, thresholds) ->
        healthThresholdConfig(thresholds.lowPercent, thresholds.highPercent)
    },
)

internal fun healthThresholdConfig(
    lowPercent: Int,
    highPercent: Int,
): UsageHealthThresholdConfig = runCatching {
    UsageHealthThresholdConfig(lowPercent = lowPercent, highPercent = highPercent)
}.getOrDefault(UsageHealthThresholdConfig.DEFAULT)

/**
 * Observes the global notification thresholds and per-credential overrides and calls
 * [onChanged] for the initial values and every distinct subsequent change. The flow
 * never writes back into the settings stores, so no update loop is possible.
 */
suspend fun observeUsageHealthThresholdChanges(
    settings: Flow<NotificationSettings>,
    overrides: Flow<Map<UUID, AlertThresholds>>,
    onChanged: suspend (AppliedHealthThresholds) -> Unit,
) {
    combine(settings, overrides, ::appliedHealthThresholds)
        .distinctUntilChanged()
        .collect(onChanged)
}
