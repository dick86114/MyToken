package ai.routin.mytoken.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** 一次全量重绘两种组件；点击刷新不再依赖具体 glanceId。 */
internal object CredentialWidgetUpdater {
    suspend fun updateAll(context: Context) {
        CredentialGlanceWidget().updateAll(context.applicationContext)
        OverviewGlanceWidget().updateAll(context.applicationContext)
    }
}
