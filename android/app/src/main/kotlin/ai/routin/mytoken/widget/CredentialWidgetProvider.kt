package ai.routin.mytoken.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Keeps the original provider class name so existing home-screen widget ids stay valid. */
class CredentialWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CredentialGlanceWidget()
}
