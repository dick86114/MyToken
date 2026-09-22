package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** 更新日志本地缓存：打开设置页默认读缓存，用户点击后再走网络。 */
interface ReleaseNotesCacheStore {
    val cachedReleases: Flow<List<AppReleaseHistoryItem>?>
    suspend fun save(releases: List<AppReleaseHistoryItem>)
}

class AppPreferencesReleaseNotesCacheStore(
    private val repository: AppPreferencesRepository,
) : ReleaseNotesCacheStore {

    override val cachedReleases: Flow<List<AppReleaseHistoryItem>?> =
        repository.releaseNotesCache.map { json -> decode(json) }

    override suspend fun save(releases: List<AppReleaseHistoryItem>) {
        val array = JSONArray()
        releases.forEach { release ->
            array.put(
                JSONObject()
                    .put("version", release.version)
                    .put("releaseNotes", release.releaseNotes)
                    .put("releaseUrl", release.releaseUrl)
                    .put("publishedAt", release.publishedAt ?: "")
            )
        }
        repository.setReleaseNotesCache(array.toString())
    }

    private fun decode(json: String): List<AppReleaseHistoryItem>? {
        if (json.isBlank()) return null
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        AppReleaseHistoryItem(
                            version = item.getString("version"),
                            releaseNotes = item.getString("releaseNotes"),
                            releaseUrl = item.getString("releaseUrl"),
                            publishedAt = item.getString("publishedAt").takeIf { it.isNotBlank() },
                        )
                    )
                }
            }
        }.getOrNull()
    }
}
