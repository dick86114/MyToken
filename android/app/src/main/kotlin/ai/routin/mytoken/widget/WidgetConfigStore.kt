package ai.routin.mytoken.widget

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Persists one v2 widget configuration per app widget id and migrates legacy selections. */
class WidgetConfigStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("mytoken_widgets", Context.MODE_PRIVATE)

    fun load(appWidgetId: Int, kind: WidgetKind): WidgetConfig {
        preferences.getString(configKey(appWidgetId), null)?.let { raw ->
            runCatching { decode(JSONObject(raw)) }.getOrNull()?.let { return it }
        }
        return migrateLegacySelection(appWidgetId, kind)
    }

    fun migrateLegacySelection(
        appWidgetId: Int,
        kind: WidgetKind = WidgetKind.Credential,
    ): WidgetConfig {
        val credentialId = preferences.getString(legacyKey(appWidgetId), null)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val config = WidgetConfig(
            kind = kind,
            credentialId = credentialId,
            style = WidgetDisplayStyle.Auto,
        )
        if (credentialId != null) {
            save(appWidgetId, config)
        }
        return config
    }

    fun save(appWidgetId: Int, config: WidgetConfig) {
        val normalized = config.copy(
            overviewCredentialIds = config.limitedOverviewCredentialIds,
        )
        preferences.edit()
            .putString(configKey(appWidgetId), encode(normalized).toString())
            .apply()
    }

    fun remove(appWidgetId: Int) {
        preferences.edit()
            .remove(configKey(appWidgetId))
            .remove(legacyKey(appWidgetId))
            .apply()
    }

    fun requiredCredentialIds(): Set<UUID> {
        val result = mutableSetOf<UUID>()
        preferences.all.forEach { (key, value) ->
            if (!key.startsWith(CONFIG_PREFIX) || value !is String) return@forEach
            val config = runCatching { decode(JSONObject(value)) }.getOrNull() ?: return@forEach
            when (config.kind) {
                WidgetKind.Credential -> config.credentialId?.let(result::add)
                WidgetKind.Overview -> result.addAll(config.limitedOverviewCredentialIds)
            }
        }
        return result
    }

    private fun encode(config: WidgetConfig): JSONObject = JSONObject()
        .put("kind", config.kind.name)
        .put("credentialId", config.credentialId?.toString() ?: JSONObject.NULL)
        .put(
            "overviewCredentialIds",
            JSONArray(config.limitedOverviewCredentialIds.map(UUID::toString)),
        )
        .put("style", config.style.name)
        .put("primaryMetricId", config.primaryMetricId ?: JSONObject.NULL)
        .put("overviewSource", config.overviewSource.name)

    private fun decode(json: JSONObject): WidgetConfig = WidgetConfig(
        kind = WidgetKind.valueOf(json.optString("kind", WidgetKind.Credential.name)),
        credentialId = json.optString("credentialId")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let(UUID::fromString),
        overviewCredentialIds = json.optJSONArray("overviewCredentialIds")
            ?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optString(index).takeIf(String::isNotBlank)?.let(UUID::fromString)
                }
            }
            .orEmpty(),
        style = WidgetDisplayStyle.valueOf(json.optString("style", WidgetDisplayStyle.Auto.name)),
        primaryMetricId = json.optString("primaryMetricId")
            .takeIf { it.isNotBlank() && it != "null" },
        overviewSource = WidgetOverviewSource.valueOf(
            json.optString("overviewSource", WidgetOverviewSource.AllEnabled.name)
        ),
    )

    private fun configKey(appWidgetId: Int) = "$CONFIG_PREFIX$appWidgetId"

    private fun legacyKey(appWidgetId: Int) = "credential_$appWidgetId"

    private companion object {
        const val CONFIG_PREFIX = "config_"
    }
}
