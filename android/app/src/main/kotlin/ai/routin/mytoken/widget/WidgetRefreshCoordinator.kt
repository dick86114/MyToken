package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.usage.RefreshStatus

/** 手动/后台刷新共用：去重后逐个刷新组件需要的凭证。 */
internal class WidgetRefreshCoordinator(
    private val refreshCredential: suspend (Credential) -> RefreshStatus,
) {
    /** 每个去重后的凭证只刷新一次；返回是否全部成功。重绘交给调用方统一收尾。 */
    suspend fun refresh(credentials: List<Credential>): Boolean {
        var anyFailed = false
        credentials.distinctBy(Credential::id).forEach { credential ->
            if (refreshCredential(credential) == RefreshStatus.Failed) {
                anyFailed = true
            }
        }
        return !anyFailed
    }
}
