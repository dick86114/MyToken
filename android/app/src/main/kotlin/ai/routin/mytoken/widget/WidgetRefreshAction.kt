package ai.routin.mytoken.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * 点击刷新后的统一流程：先亮起刷新态并重绘一次，再入队一次全局刷新。
 * 两种组件共用同一入口，因此不会再出现“总览组件用凭证 glanceId 重绘”的类型错配。
 */
internal object WidgetRefreshTrigger {
    suspend fun trigger(
        setRefreshing: (Boolean) -> Unit,
        enqueueRefresh: () -> Unit,
        redrawAll: suspend () -> Unit,
    ) {
        setRefreshing(true)
        enqueueRefresh()
        redrawAll()
    }
}

/** Glance refresh button: enqueue the one global refresh and reflect loading immediately. */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val store = WidgetRefreshStateStore(context)
        WidgetRefreshTrigger.trigger(
            setRefreshing = store::setRefreshing,
            enqueueRefresh = { WidgetRefresher.enqueue(context) },
            redrawAll = { CredentialWidgetUpdater.updateAll(context) },
        )
    }
}
