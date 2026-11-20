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
}
