package ai.routin.mytoken.feature.settings

import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 刷新设置只保留打开应用时刷新与失败后自动重试。 */
data class RefreshSettings(
    val openAppRefresh: Boolean = true,
    val retryOnFailure: Boolean = true,
)

interface RefreshSettingsStore {
    val settings: Flow<RefreshSettings>
    suspend fun setOpenAppRefresh(enabled: Boolean)
    suspend fun setRetryOnFailure(enabled: Boolean)
}

/** 刷新设置直接复用 [AppPreferencesRepository] 的本地 DataStore。 */
class AppPreferencesRefreshSettingsStore(
    private val preferencesRepository: AppPreferencesRepository,
) : RefreshSettingsStore {

    override val settings: Flow<RefreshSettings> = preferencesRepository.preferences.map { prefs ->
        RefreshSettings(
            openAppRefresh = prefs.openAppRefresh,
            retryOnFailure = prefs.retryOnFailure,
        )
    }

    override suspend fun setOpenAppRefresh(enabled: Boolean) {
        preferencesRepository.setOpenAppRefresh(enabled)
    }

    override suspend fun setRetryOnFailure(enabled: Boolean) {
        preferencesRepository.setRetryOnFailure(enabled)
    }
}
