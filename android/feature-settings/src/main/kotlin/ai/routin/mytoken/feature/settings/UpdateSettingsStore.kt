package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 更新通道偏好：mirrorBase 为空 = GitHub 直连，非空 = CDN 镜像前缀。 */
data class UpdateSettings(
    val mirrorBase: String = "",
    val githubTokenMask: String? = null,
    val githubTokenError: String? = null,
)

/**
 * GitHub API 令牌的本机安全存储。设置页只能拿到脱敏值；更新控制器通过单独的
 * provider 读取完整令牌，避免原始密钥进入 UI 状态。
 */
interface GitHubTokenStore {
    suspend fun readToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    suspend fun tokenMask(): String?
}

interface UpdateSettingsStore {
    val settings: Flow<UpdateSettings>
    suspend fun setMirrorBase(base: String)
}

/** 更新日志和错误提示只展示首尾四位，不保留完整令牌。 */
object GitHubTokenMask {
    fun mask(token: String?): String? {
        val value = token?.takeIf { it.isNotEmpty() } ?: return null
        if (value.length <= 8) return "•".repeat(value.length)
        return "${value.take(4)}…${value.takeLast(4)}"
    }
}

class AppPreferencesUpdateSettingsStore(
    private val preferencesRepository: AppPreferencesRepository,
) : UpdateSettingsStore {

    override val settings: Flow<UpdateSettings> = preferencesRepository.updatePreferences.map { prefs ->
        UpdateSettings(mirrorBase = prefs.updateMirrorBase)
    }

    override suspend fun setMirrorBase(base: String) {
        preferencesRepository.setUpdateMirrorBase(base)
    }
}
