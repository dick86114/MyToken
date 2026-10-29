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

private data class GitHubTokenUiState(
    val mask: String? = null,
    val error: String? = null,
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
    private val gitHubTokenStore: GitHubTokenStore,
    private val updateController: AppUpdateController,
) : ViewModel() {

    private val gitHubToken = MutableStateFlow(GitHubTokenUiState())

    val state: StateFlow<SettingsUiState> = combine(
        refreshStore.settings,
        displayStore.settings,
        notificationStore.settings,
        updateStore.settings,
        gitHubToken,
    ) { refresh, display, notifications, update, token ->
        SettingsUiState(
            isLoading = false,
            refresh = refresh,
            display = display,
            notifications = notifications,
            update = update.copy(
                githubTokenMask = token.mask,
                githubTokenError = token.error,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        loadGitHubTokenMask()
    }

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

    fun setGitHubToken(token: String) {
        val normalized = token.trim()
        if (normalized.isEmpty()) return
        launch {
            gitHubToken.value = try {
                gitHubTokenStore.saveToken(normalized)
                GitHubTokenUiState(mask = gitHubTokenStore.tokenMask())
            } catch (_: Exception) {
                GitHubTokenUiState(
                    mask = gitHubToken.value.mask,
                    error = "GitHub 令牌保存失败，请重试",
                )
            }
        }
    }

    fun removeGitHubToken() = launch {
        gitHubToken.value = try {
            gitHubTokenStore.clearToken()
            GitHubTokenUiState()
        } catch (_: Exception) {
            GitHubTokenUiState(
                mask = gitHubToken.value.mask,
                error = "GitHub 令牌移除失败，请重试",
            )
        }
    }

    private fun loadGitHubTokenMask() {
        launch {
            gitHubToken.value = try {
                GitHubTokenUiState(mask = gitHubTokenStore.tokenMask())
            } catch (_: Exception) {
                GitHubTokenUiState(error = "GitHub 令牌读取失败")
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
