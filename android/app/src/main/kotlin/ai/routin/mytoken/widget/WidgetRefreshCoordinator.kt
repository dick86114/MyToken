package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.usage.RefreshStatus

/** Refreshes each distinct widget credential once, then updates every installed widget. */
internal class WidgetRefreshCoordinator(
    private val refreshCredential: suspend (Credential) -> RefreshStatus,
    private val updateAll: suspend () -> Unit,
) {
    suspend fun refresh(credentials: List<Credential>): Boolean {
        var anyFailed = false
        credentials.distinctBy(Credential::id).forEach { credential ->
            if (refreshCredential(credential) == RefreshStatus.Failed) {
                anyFailed = true
            }
        }
        updateAll()
        return !anyFailed
    }
}
