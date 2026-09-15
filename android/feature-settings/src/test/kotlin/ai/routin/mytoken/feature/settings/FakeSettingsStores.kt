package ai.routin.mytoken.feature.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeRefreshSettingsStore(initial: RefreshSettings = RefreshSettings()) : RefreshSettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<RefreshSettings> = state

    override suspend fun setOpenAppRefresh(enabled: Boolean) =
        state.update { it.copy(openAppRefresh = enabled) }

    override suspend fun setRetryOnFailure(enabled: Boolean) =
        state.update { it.copy(retryOnFailure = enabled) }
}

class FakeDisplaySettingsStore(initial: DisplaySettings = DisplaySettings()) : DisplaySettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<DisplaySettings> = state

    override suspend fun setThemeMode(mode: AppThemeMode) =
        state.update { it.copy(themeMode = mode) }

    override suspend fun setShowDisabledCredentials(enabled: Boolean) =
        state.update { it.copy(showDisabledCredentials = enabled) }

    override suspend fun setDefaultExpandGroups(enabled: Boolean) =
        state.update { it.copy(defaultExpandGroups = enabled) }

    override suspend fun setShowUsageProgress(enabled: Boolean) =
        state.update { it.copy(showUsageProgress = enabled) }

    override suspend fun setShowBalance(enabled: Boolean) =
        state.update { it.copy(showBalance = enabled) }

    override suspend fun setShowResetTime(enabled: Boolean) =
        state.update { it.copy(showResetTime = enabled) }
}

class FakeNotificationSettingsStore(
    initial: NotificationSettings = NotificationSettings(),
) : NotificationSettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<NotificationSettings> = state

    override suspend fun setCredentialFailureAlertsEnabled(enabled: Boolean) =
        state.update { it.copy(credentialFailureAlertsEnabled = enabled) }
}

class FakeUpdateSettingsStore(initial: UpdateSettings = UpdateSettings()) : UpdateSettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UpdateSettings> = state

    override suspend fun setMirrorBase(base: String) =
        state.update { it.copy(mirrorBase = base) }
}

class FakeAppUpdateController : AppUpdateController {
    override val state = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    override val releaseHistoryState =
        MutableStateFlow<AppReleaseHistoryUiState>(AppReleaseHistoryUiState.Idle)
    var checked = 0
    var releaseHistoryLoaded = 0
    var downloaded: Pair<String, String>? = null

    override fun checkForUpdates() {
        checked += 1
    }

    override fun loadReleaseHistory() {
        releaseHistoryLoaded += 1
    }

    override fun downloadAndInstall(version: String, downloadUrl: String) {
        downloaded = version to downloadUrl
    }

    override fun openInstallPermissionSettings() = Unit

    override fun installDownloadedUpdate() = Unit
}
