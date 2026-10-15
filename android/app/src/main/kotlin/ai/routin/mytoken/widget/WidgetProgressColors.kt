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

    /**
     * RemoteViews 只能通过资源 id 或 ARGB 色值设置颜色。
     * 这里必须传 [Color]（色值），传 [Int] 会被当成资源 id 解析并导致组件加载失败。
     */
    fun provider(severity: WidgetMetricSeverity?): ColorProvider = ColorProvider(composeColor(severity))

    fun composeColor(severity: WidgetMetricSeverity?): Color = Color(colorInt(severity))
}
