package ai.routin.mytoken.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewStyleLayoutPolicyTest {

    @Test
    fun `overview explicit styles map to distinct layouts`() {
        assertEquals(
            OverviewStyleLayout.ScanRows,
            OverviewStyleLayoutPolicy.layout(WidgetDisplayStyle.ScanBand),
        )
        assertEquals(
            OverviewStyleLayout.FocusRings,
            OverviewStyleLayoutPolicy.layout(WidgetDisplayStyle.FocusRing),
        )
        assertEquals(
            OverviewStyleLayout.SignalTiles,
            OverviewStyleLayoutPolicy.layout(WidgetDisplayStyle.SignalTiles),
        )
        assertEquals(
            OverviewStyleLayout.ScanRows,
            OverviewStyleLayoutPolicy.layout(WidgetDisplayStyle.Auto),
        )
    }
}
