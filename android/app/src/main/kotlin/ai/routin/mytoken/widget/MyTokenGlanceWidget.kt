package ai.routin.mytoken.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** Shared Glance shell: reads the v2 config, maps widget size, then delegates rendering. */
abstract class MyTokenGlanceWidget : GlanceAppWidget() {
    abstract val widgetKind: WidgetKind

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 250.dp),
        )
    )

    abstract suspend fun makePresentation(
        context: Context,
        config: WidgetConfig,
        size: WidgetSizeKind,
    ): WidgetPresentation

    @Composable
    abstract fun Content(
        context: Context,
        config: WidgetConfig,
        presentation: WidgetPresentation,
    )

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(id)
        val config = WidgetConfigStore(context).load(appWidgetId, widgetKind)
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val size = WidgetLayoutPolicy.kindFor(
            widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        )
        val presentation = makePresentation(context, config, size)
        provideContent {
            Content(context, config, presentation)
        }
    }
}
