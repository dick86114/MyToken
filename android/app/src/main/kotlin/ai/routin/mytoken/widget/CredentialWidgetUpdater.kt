package ai.routin.mytoken.widget

import ai.routin.mytoken.AppGraph
import android.content.Context
import androidx.glance.appwidget.updateAll

/** Updates both Glance widget kinds after a background or manual refresh pass. */
internal object CredentialWidgetUpdater {
    suspend fun updateAll(context: Context, @Suppress("UNUSED_PARAMETER") graph: AppGraph) {
        CredentialGlanceWidget().updateAll(context.applicationContext)
        OverviewGlanceWidget().updateAll(context.applicationContext)
    }
}
