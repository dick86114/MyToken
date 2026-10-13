package ai.routin.mytoken.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/** Glance refresh button: enqueue the one global refresh and reflect loading immediately. */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetRefresher.enqueue(context)
        CredentialGlanceWidget().update(context, glanceId)
    }
}
