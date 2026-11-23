package ai.routin.mytoken.data.alerts

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.alerts.CredentialAlertThresholdStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * JSON codec for the per-credential threshold map. Entries that are not valid
 * UUIDs or not valid 1..99/low<high thresholds are dropped on read, so a damaged
 * preference can never crash the notification dispatcher.
 */
object CredentialAlertThresholdCodec {

    fun encode(overrides: Map<UUID, AlertThresholds>): String {
        val root = JSONObject()
        overrides.entries
            .sortedBy { it.key.toString() }
            .forEach { (credentialId, thresholds) ->
                root.put(
                    credentialId.toString(),
                    JSONObject()
                        .put("low", thresholds.lowPercent)
                        .put("high", thresholds.highPercent),
                )
            }
        return root.toString()
    }

    fun decode(json: String): Map<UUID, AlertThresholds> {
        if (json.isBlank()) return emptyMap()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        val decoded = linkedMapOf<UUID, AlertThresholds>()
        root.keys().forEach { key ->
            val credentialId = runCatching { UUID.fromString(key) }.getOrNull()
                ?: return@forEach
            val value = root.optJSONObject(key) ?: return@forEach
            val thresholds = runCatching {
                AlertThresholds(
                    lowPercent = value.optInt("low", -1),
                    highPercent = value.optInt("high", -1),
                )
            }.getOrNull() ?: return@forEach
            decoded[credentialId] = thresholds
        }
        return decoded
    }
}

private val Context.credentialAlertThresholdDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mytoken_credential_alert_thresholds"
)

/**
 * DataStore persistence for [CredentialAlertThresholdStore]: one JSON object maps
 * credential UUIDs to notification-only low/high percentages. No Room migration is
 * involved, and deleted credentials are pruned by the credential list flow.
 */
class DataStoreCredentialAlertThresholdStore(
    private val context: Context,
) : CredentialAlertThresholdStore {

    override val overrides: Flow<Map<UUID, AlertThresholds>> =
        context.credentialAlertThresholdDataStore.data.map { prefs ->
            CredentialAlertThresholdCodec.decode(prefs[OVERRIDES_KEY].orEmpty())
        }

    override suspend fun setOverride(credentialId: UUID, thresholds: AlertThresholds) {
        context.credentialAlertThresholdDataStore.edit { prefs ->
            val current = CredentialAlertThresholdCodec.decode(prefs[OVERRIDES_KEY].orEmpty())
            prefs[OVERRIDES_KEY] = CredentialAlertThresholdCodec.encode(
                current + (credentialId to thresholds),
            )
        }
    }

    override suspend fun clearOverride(credentialId: UUID) {
        context.credentialAlertThresholdDataStore.edit { prefs ->
            val current = CredentialAlertThresholdCodec.decode(prefs[OVERRIDES_KEY].orEmpty())
            if (credentialId in current) {
                prefs[OVERRIDES_KEY] = CredentialAlertThresholdCodec.encode(current - credentialId)
            }
        }
    }

    private companion object {
        val OVERRIDES_KEY = stringPreferencesKey("credentialAlertThresholds")
    }
}
