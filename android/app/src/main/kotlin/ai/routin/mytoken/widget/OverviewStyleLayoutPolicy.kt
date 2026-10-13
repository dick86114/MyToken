package ai.routin.mytoken.widget

enum class OverviewStyleLayout {
    ScanRows,
    FocusRings,
    SignalTiles,
}

object OverviewStyleLayoutPolicy {
    fun layout(style: WidgetDisplayStyle): OverviewStyleLayout = when (style) {
        WidgetDisplayStyle.FocusRing -> OverviewStyleLayout.FocusRings
        WidgetDisplayStyle.SignalTiles -> OverviewStyleLayout.SignalTiles
        WidgetDisplayStyle.ScanBand, WidgetDisplayStyle.Auto -> OverviewStyleLayout.ScanRows
    }
}
