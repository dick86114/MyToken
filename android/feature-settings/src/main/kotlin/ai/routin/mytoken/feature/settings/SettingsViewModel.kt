package ai.routin.mytoken.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whole-screen state rendered by [SettingsScreen]. */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val refresh: RefreshSettings = RefreshSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val update: UpdateSettings = UpdateSettings(),
)

/**
 * Settings state holder. All preferences are Android-local; macOS-only options
 * (menu bar style, login item, window behavior) are intentionally absent.
 */
class SettingsViewModel(
    private val refreshStore: RefreshSettingsStore,
    private val displayStore: DisplaySettingsStore,
    private val notificationStore: NotificationSettingsStore,
    private val updateStore: UpdateSettingsStore,
    private val updateController: AppUpdateController,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        refreshStore.settings,
        displayStore.settings,
        notificationStore.settings,
        updateStore.settings,
    ) { refresh, display, notifications, update ->
        SettingsUiState(
            isLoading = false,
            refresh = refresh,
            display = display,
            notifications = notifications,
            update = update,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    val updateState: StateFlow<AppUpdateUiState> = updateController.state

    val releaseHistoryState: StateFlow<AppReleaseHistoryUiState> = updateController.releaseHistoryState

    fun checkForUpdates() = updateController.checkForUpdates()

    fun loadReleaseHistory() = updateController.loadReleaseHistory()

    fun loadCachedReleaseHistory() = updateController.loadCachedReleaseHistory()

    fun downloadAndInstall(version: String, downloadUrl: String) =
        updateController.downloadAndInstall(version, downloadUrl)

    fun openInstallPermissionSettings() = updateController.openInstallPermissionSettings()

    fun installDownloadedUpdate() = updateController.installDownloadedUpdate()

    fun setOpenAppRefresh(enabled: Boolean) = launch { refreshStore.setOpenAppRefresh(enabled) }

    fun setRetryOnFailure(enabled: Boolean) = launch { refreshStore.setRetryOnFailure(enabled) }

    fun setThemeMode(mode: AppThemeMode) = launch { displayStore.setThemeMode(mode) }

    fun setShowDisabledCredentials(enabled: Boolean) =
        launch { displayStore.setShowDisabledCredentials(enabled) }

    fun setDefaultExpandGroups(enabled: Boolean) =
        launch { displayStore.setDefaultExpandGroups(enabled) }

    fun setShowUsageProgress(enabled: Boolean) =
        launch { displayStore.setShowUsageProgress(enabled) }

    fun setShowBalance(enabled: Boolean) = launch { displayStore.setShowBalance(enabled) }

    fun setShowResetTime(enabled: Boolean) = launch { displayStore.setShowResetTime(enabled) }

    fun setCredentialFailureAlertsEnabled(enabled: Boolean) =
        launch { notificationStore.setCredentialFailureAlertsEnabled(enabled) }

    fun setUpdateMirrorBase(base: String) = launch { updateStore.setMirrorBase(base) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
