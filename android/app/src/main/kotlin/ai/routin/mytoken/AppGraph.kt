package ai.routin.mytoken

import ai.routin.mytoken.core.security.AndroidKeystoreSecretStore
import ai.routin.mytoken.data.alerts.DataStoreAlertStateStore
import ai.routin.mytoken.data.local.MyTokenDatabase
import ai.routin.mytoken.data.preferences.AppPreferencesRepository
import ai.routin.mytoken.data.refresh.DataStoreRefreshStatusStore
import ai.routin.mytoken.data.repository.CredentialRepositoryImpl
import ai.routin.mytoken.domain.usage.RefreshCredentialsUseCase
import ai.routin.mytoken.feature.credentials.DataStoreCredentialOrderStore
import ai.routin.mytoken.feature.settings.AppPreferencesNotificationSettingsStore
import ai.routin.mytoken.feature.settings.AppPreferencesRefreshSettingsStore
import ai.routin.mytoken.feature.settings.AppPreferencesUpdateSettingsStore
import ai.routin.mytoken.feature.settings.DataStoreDisplaySettingsStore
import ai.routin.mytoken.feature.transfer.TransferClient
import ai.routin.mytoken.feature.transfer.TransferRepositoryImpl
import ai.routin.mytoken.provider.defaultUsageProviders
import ai.routin.mytoken.security.KeystoreGitHubTokenStore
import ai.routin.mytoken.update.GitHubAppUpdateController
import ai.routin.mytoken.feature.credentials.XiaomiSessionRenewer
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first

/**
 * Hand-rolled app graph (a full DI framework is out of scope for Task 9; Task 11 may
 * revisit the wiring when the navigation layer is rebuilt).
 */
class AppGraph(context: Context) {

    private val appContext = context.applicationContext

    val database: MyTokenDatabase by lazy {
        Room.databaseBuilder(appContext, MyTokenDatabase::class.java, MyTokenDatabase.NAME).build()
    }

    val credentialRepository: CredentialRepositoryImpl by lazy {
        CredentialRepositoryImpl(database, AndroidKeystoreSecretStore(appContext))
    }

    val providers: Map<ai.routin.mytoken.domain.model.ProviderId, ai.routin.mytoken.domain.usage.UsageProvider> by lazy {
        defaultUsageProviders()
    }

    val refreshUseCase: RefreshCredentialsUseCase by lazy {
        RefreshCredentialsUseCase(
            repository = credentialRepository,
            providers = providers,
            // 小米 MiMo 的控制台票据通常只活 1-2 天，过期时用持久登录态静默续期。
            sessionRenewer = XiaomiSessionRenewer(appContext),
        )
    }

    val credentialOrderStore: DataStoreCredentialOrderStore by lazy {
        DataStoreCredentialOrderStore(appContext)
    }

    val displaySettingsStore: DataStoreDisplaySettingsStore by lazy {
        DataStoreDisplaySettingsStore(appContext)
    }

    val refreshSettingsStore: AppPreferencesRefreshSettingsStore by lazy {
        AppPreferencesRefreshSettingsStore(AppPreferencesRepository(appContext))
    }

    val updateSettingsStore: AppPreferencesUpdateSettingsStore by lazy {
        AppPreferencesUpdateSettingsStore(AppPreferencesRepository(appContext))
    }

    val gitHubTokenStore: KeystoreGitHubTokenStore by lazy {
        KeystoreGitHubTokenStore(
            AndroidKeystoreSecretStore(appContext, keyAliasPrefix = "mytoken.githubToken.")
        )
    }

    val notificationSettingsStore: AppPreferencesNotificationSettingsStore by lazy {
        AppPreferencesNotificationSettingsStore(AppPreferencesRepository(appContext))
    }

    val refreshStatusStore: DataStoreRefreshStatusStore by lazy {
        DataStoreRefreshStatusStore(appContext)
    }

    val alertStateStore: DataStoreAlertStateStore by lazy {
        DataStoreAlertStateStore(appContext)
    }

    val usageAlertDispatcher: UsageAlertDispatcher by lazy {
        UsageAlertDispatcher(
            context = appContext,
            repository = credentialRepository,
            states = refreshUseCase.states,
            alertStateStore = alertStateStore,
            notificationSettingsStore = notificationSettingsStore,
        )
    }

    val transferRepository: TransferRepositoryImpl by lazy {
        TransferRepositoryImpl(TransferClient(), credentialRepository)
    }

    val appUpdateController: GitHubAppUpdateController by lazy {
        val versionName = runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .versionName
        }.getOrNull() ?: "0.1.0"

        GitHubAppUpdateController(
            context = appContext,
            currentVersionName = versionName,
            cacheStore = ai.routin.mytoken.feature.settings.AppPreferencesReleaseNotesCacheStore(
                AppPreferencesRepository(appContext)
            ),
            mirrorBaseProvider = {
                AppPreferencesRepository(appContext)
                    .updatePreferences
                    .first()
                    .updateMirrorBase
                    .takeIf { it.isNotBlank() }
            },
            gitHubTokenProvider = { gitHubTokenStore.readToken() },
        )
    }
}
