package ai.routin.mytoken.core.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure double-slider normalization shared by settings and the credential editor. */
class PercentThresholdRangeTest {

    @Test
    fun valuesRoundAndKeepTheProposedOrder() {
        val range = PercentThresholdRange.normalize(
            previous = 80f..95f,
            proposed = 70.2f..90.4f,
        )
        assertEquals(70..90, range)
    }

    @Test
    fun movingTheLowThumbOntoTheHighThumbLeavesOnePoint() {
        val range = PercentThresholdRange.normalize(
            previous = 80f..95f,
            proposed = 95f..95f,
        )
        assertEquals(94..95, range)
    }

    @Test
    fun movingTheHighThumbOntoTheLowThumbLeavesOnePoint() {
        val range = PercentThresholdRange.normalize(
            previous = 80f..95f,
            proposed = 80f..80f,
        )
        assertEquals(80..81, range)
    }

    @Test
    fun boundsStayWithinOneToNinetyNine() {
        assertEquals(
            1..2,
            PercentThresholdRange.normalize(previous = 1f..2f, proposed = 0f..0f),
        )
        assertEquals(
            98..99,
            PercentThresholdRange.normalize(previous = 98f..99f, proposed = 100f..100f),
        )
    }

    @Test
    fun labelDescribesBothEnds() {
        assertEquals("低 80% · 高 95%", PercentThresholdRange.label(80, 95))
    }
}
