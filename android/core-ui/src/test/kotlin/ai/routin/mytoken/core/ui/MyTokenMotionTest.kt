package ai.routin.mytoken.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MyTokenMotionTest {
    @Test
    fun motionDurationsCollapseWhenDisabled() {
        assertEquals(500, MyTokenMotion.ringDurationMillis(enabled = true))
        assertEquals(0, MyTokenMotion.ringDurationMillis(enabled = false))
        assertEquals(300, MyTokenMotion.locateDurationMillis(enabled = true))
        assertEquals(0, MyTokenMotion.locateDurationMillis(enabled = false))
        assertEquals(320, MyTokenMotion.modeSwitchDurationMillis(enabled = true))
        assertEquals(0, MyTokenMotion.modeSwitchDurationMillis(enabled = false))
    }

    @Test
    fun 大屏三栏的动画时长在关闭动画时归零() {
        assertEquals(260, MyTokenMotion.detailOpenDurationMillis(enabled = true))
        assertEquals(240, MyTokenMotion.detailCloseDurationMillis(enabled = true))
        assertEquals(200, MyTokenMotion.detailReflowDurationMillis(enabled = true))
        assertEquals(160, MyTokenMotion.filterDurationMillis(enabled = true))
        assertEquals(220, MyTokenMotion.reportResizeDurationMillis(enabled = true))
        assertEquals(0, MyTokenMotion.detailOpenDurationMillis(enabled = false))
        assertEquals(0, MyTokenMotion.detailCloseDurationMillis(enabled = false))
        assertEquals(0, MyTokenMotion.detailReflowDurationMillis(enabled = false))
        assertEquals(0, MyTokenMotion.filterDurationMillis(enabled = false))
        assertEquals(0, MyTokenMotion.reportResizeDurationMillis(enabled = false))
    }
}
