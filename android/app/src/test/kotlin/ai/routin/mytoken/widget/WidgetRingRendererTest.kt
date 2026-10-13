package ai.routin.mytoken.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRingRendererTest {

    @Test
    fun `ring sweep follows progress percentage`() {
        assertEquals(0f, WidgetRingRenderer.sweepAngle(null), 0.001f)
        assertEquals(72f, WidgetRingRenderer.sweepAngle(20), 0.001f)
        assertEquals(360f, WidgetRingRenderer.sweepAngle(100), 0.001f)
        assertEquals(360f, WidgetRingRenderer.sweepAngle(140), 0.001f)
    }
}
