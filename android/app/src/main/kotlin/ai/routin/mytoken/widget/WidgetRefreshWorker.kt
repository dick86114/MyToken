package ai.routin.mytoken.widget

import ai.routin.mytoken.MyTokenApplication
import ai.routin.mytoken.domain.usage.RefreshStatus
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/** 刷新一批凭证后的收尾动作。 */
internal enum class WidgetRefreshOutcome {
    Success,
    Retry,
    GiveUp,
}

/** 最多重试 [MAX_REFRESH_ATTEMPTS] 次；否则唯一任务会长期占位，新点击永远排不进去。 */
internal fun widgetRefreshOutcome(succeeded: Boolean, runAttemptCount: Int): WidgetRefreshOutcome = when {
    succeeded -> WidgetRefreshOutcome.Success
    runAttemptCount < MAX_REFRESH_ATTEMPTS -> WidgetRefreshOutcome.Retry
    else -> WidgetRefreshOutcome.GiveUp
}

internal const val MAX_REFRESH_ATTEMPTS = 3

/** 一次刷新所有组件需要的凭证集合，然后只重绘一次。 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val graph = (applicationContext as MyTokenApplication).graph
        val refreshStateStore = WidgetRefreshStateStore(applicationContext)
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
                    ?: RefreshStatus.Failed
            },
        )
        val succeeded = try {
            coordinator.refresh(credentials)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            false
        }

        // 先结束刷新态再重绘，避免组件停在“正在获取”且重试前又被清空。
        refreshStateStore.setRefreshing(false)
        runCatching { CredentialWidgetUpdater.updateAll(applicationContext) }

        return when (widgetRefreshOutcome(succeeded, runAttemptCount)) {
            WidgetRefreshOutcome.Success -> Result.success()
            WidgetRefreshOutcome.Retry -> Result.retry()
            WidgetRefreshOutcome.GiveUp -> Result.failure()
        }
    }
}
