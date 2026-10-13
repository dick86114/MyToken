package ai.routin.mytoken.widget

import ai.routin.mytoken.MyTokenApplication
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

/** Refreshes the union of credentials required by every installed widget once. */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val graph = (applicationContext as MyTokenApplication).graph
        val store = WidgetConfigStore(applicationContext)
        val requiredIds = store.requiredCredentialIds()
        val enabledCredentials = graph.credentialRepository.observeCredentials()
            .first()
            .filter { it.isEnabled }
        val credentials = if (store.requiresAllEnabledCredentials()) {
            enabledCredentials
        } else {
            enabledCredentials.filter { it.id in requiredIds }
        }

        val coordinator = WidgetRefreshCoordinator(
            refreshCredential = { credential ->
                graph.refreshUseCase.refresh(credential)
                graph.refreshUseCase.states.value[credential.id]?.status
                    ?: ai.routin.mytoken.domain.usage.RefreshStatus.Failed
            },
            updateAll = {
                CredentialWidgetUpdater.updateAll(applicationContext, graph)
            },
        )
        return if (coordinator.refresh(credentials)) Result.success() else Result.retry()
    }
}
