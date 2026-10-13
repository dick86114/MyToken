package ai.routin.mytoken.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutPolicyTest {

    @Test
    fun `compact standard and expanded map from available widget dimensions`() {
        assertEquals(WidgetSizeKind.Compact, WidgetLayoutPolicy.kindFor(110, 110))
        assertEquals(WidgetSizeKind.Compact, WidgetLayoutPolicy.kindFor(179, 110))
        assertEquals(WidgetSizeKind.Standard, WidgetLayoutPolicy.kindFor(180, 110))
        assertEquals(WidgetSizeKind.Standard, WidgetLayoutPolicy.kindFor(299, 160))
        assertEquals(WidgetSizeKind.Expanded, WidgetLayoutPolicy.kindFor(300, 250))
        assertEquals(WidgetSizeKind.Compact, WidgetLayoutPolicy.kindFor(330, 109))
    }

    @Test
    fun `all text roles use single line and ellipsize labels`() {
        for (role in WidgetTextRole.entries) {
            assertEquals(1, WidgetTextLayoutPolicy.lineLimit(role))
        }
        assertTrue(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.CredentialName))
        assertTrue(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.MetricLabel))
        assertFalse(WidgetTextLayoutPolicy.usesEllipsis(WidgetTextRole.MetricValue))
    }
}
