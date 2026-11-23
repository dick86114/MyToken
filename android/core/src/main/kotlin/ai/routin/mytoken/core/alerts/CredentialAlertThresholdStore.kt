package ai.routin.mytoken.core.alerts

import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Per-credential percentage-threshold overrides used for notification evaluation
 * and for the credential's home-screen health colors. Empty means the credential
 * follows the global thresholds; DataStore-backed in :data.
 */
interface CredentialAlertThresholdStore {
    val overrides: Flow<Map<UUID, AlertThresholds>>

    suspend fun setOverride(credentialId: UUID, thresholds: AlertThresholds)

    /** Removes an override (also used when the credential is deleted). */
    suspend fun clearOverride(credentialId: UUID)
}
