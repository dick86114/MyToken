package ai.routin.mytoken.widget

import ai.routin.mytoken.R
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/** Single progress palette used by every widget style, matching main-card thresholds. */
class WidgetProgressColors(private val context: Context) {
    val trackColorInt: Int = context.getColor(R.color.widget_glance_track)

    fun colorInt(severity: WidgetMetricSeverity?): Int = context.getColor(
        when (severity) {
            WidgetMetricSeverity.Warning -> R.color.widget_glance_warning
            WidgetMetricSeverity.Critical -> R.color.widget_glance_critical
            WidgetMetricSeverity.Unavailable -> R.color.widget_glance_unavailable
            WidgetMetricSeverity.Normal, null -> R.color.widget_glance_accent
        }
    )

    fun provider(severity: WidgetMetricSeverity?): ColorProvider = ColorProvider(colorInt(severity))

    fun composeColor(severity: WidgetMetricSeverity?): Color = Color(colorInt(severity))
}
