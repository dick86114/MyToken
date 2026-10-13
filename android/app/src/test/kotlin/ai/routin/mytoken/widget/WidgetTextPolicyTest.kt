package ai.routin.mytoken.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTextPolicyTest {

    @Test
    fun `long provider plan credential and metric names stay single line`() {
        for (role in WidgetTextRole.entries) {
            assertEquals(1, WidgetTextLayoutPolicy.lineLimit(role))
        }
        assertTrue(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.ProviderName))
        assertTrue(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.CredentialName))
        assertTrue(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.MetricLabel))
    }
}
