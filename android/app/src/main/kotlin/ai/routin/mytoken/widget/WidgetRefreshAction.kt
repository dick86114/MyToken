package ai.routin.mytoken.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** Glance refresh button: enqueue the one global refresh and reflect loading immediately. */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetRefreshStateStore(context).setRefreshing(true)
        WidgetRefresher.enqueue(context)
        CredentialGlanceWidget().update(context, glanceId)
        OverviewGlanceWidget().updateAll(context)
    }
}
