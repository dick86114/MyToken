package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.alerts.CredentialAlertThresholdStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [CredentialAlertThresholdStore] fake for editor/list tests. */
class FakeCredentialAlertThresholdStore(
    initial: Map<UUID, AlertThresholds> = emptyMap(),
) : CredentialAlertThresholdStore {

    val state = MutableStateFlow(initial)
    override val overrides: Flow<Map<UUID, AlertThresholds>> = state

    override suspend fun setOverride(credentialId: UUID, thresholds: AlertThresholds) {
        state.update { it + (credentialId to thresholds) }
    }

    override suspend fun clearOverride(credentialId: UUID) {
        state.update { it - credentialId }
    }
}
