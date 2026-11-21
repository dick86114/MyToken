package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定“安静的仪表”设计文档 §6.1 / §6.4 的精确 token 值。 */
class MyTokenTokensTest {

    @Test
    fun `light tokens match approved design`() {
        assertEquals(Color(0xFFEEF0F2), MyTokenLightColors.canvas)
        assertEquals(Color(0xFFFFFFFF), MyTokenLightColors.surface)
        assertEquals(Color(0xFFF6F7F9), MyTokenLightColors.subtle)
        assertEquals(Color(0xFFE5E7EB), MyTokenLightColors.line)
        assertEquals(Color(0xFF17181A), MyTokenLightColors.ink)
        assertEquals(Color(0xFF6B7076), MyTokenLightColors.muted)
        assertEquals(Color(0xFF2F6BFF), MyTokenLightColors.actionFill)
        assertEquals(Color(0xFF1F5AE0), MyTokenLightColors.actionText)
        assertEquals(Color(0xFFFFFFFF), MyTokenLightColors.onAction)
        assertEquals(Color(0xFF30C48D), MyTokenLightColors.positiveFill)
        assertEquals(Color(0xFF14805A), MyTokenLightColors.positiveText)
        assertEquals(Color(0xFFF5A524), MyTokenLightColors.warningFill)
        assertEquals(Color(0xFF9A5B00), MyTokenLightColors.warningText)
        assertEquals(Color(0xFFE5484D), MyTokenLightColors.criticalFill)
        assertEquals(Color(0xFFC0272D), MyTokenLightColors.criticalText)
        assertEquals(Color(0xFF2B2D31), MyTokenLightColors.cat1)
        assertEquals(Color(0xFF8A8F96), MyTokenLightColors.cat2)
        assertEquals(Color(0xFFC9CDD3), MyTokenLightColors.cat3)
        assertEquals(Color(0xFFE9EBEE), MyTokenLightColors.track)
        assertEquals(Color(0xFFA3A9B0), MyTokenLightColors.statusDotNormal)
    }

    @Test
    fun `dark onAction is dark ink not white`() {
        assertEquals(Color(0xFF061326), MyTokenDarkColors.onAction)
        assertEquals(Color(0xFF7AA2FF), MyTokenDarkColors.actionFill)
        assertEquals(Color(0xFF16181C), MyTokenDarkColors.surface)
        assertEquals(Color(0xFF0D0F12), MyTokenDarkColors.canvas)
        assertEquals(Color(0xFF1D2025), MyTokenDarkColors.subtle)
        assertEquals(Color(0xFF292D33), MyTokenDarkColors.line)
        assertEquals(Color(0xFFF5F6F7), MyTokenDarkColors.ink)
        assertEquals(Color(0xFF9BA1A8), MyTokenDarkColors.muted)
        assertEquals(Color(0xFF35D399), MyTokenDarkColors.positiveFill)
        assertEquals(Color(0xFF5BE0AC), MyTokenDarkColors.positiveText)
        assertEquals(Color(0xFFFFB340), MyTokenDarkColors.warningFill)
        assertEquals(Color(0xFFFFC266), MyTokenDarkColors.warningText)
        assertEquals(Color(0xFFFF6369), MyTokenDarkColors.criticalFill)
        assertEquals(Color(0xFFFF8A8E), MyTokenDarkColors.criticalText)
        assertEquals(Color(0xFFE8EAED), MyTokenDarkColors.cat1)
        assertEquals(Color(0xFF9BA1A8), MyTokenDarkColors.cat2)
        assertEquals(Color(0xFF4A4F56), MyTokenDarkColors.cat3)
        assertEquals(Color(0xFF2A2E34), MyTokenDarkColors.track)
        assertEquals(Color(0xFF6B7076), MyTokenDarkColors.statusDotNormal)
    }

    @Test
    fun `spacing and radius match the approved scale`() {
        assertEquals(2.dp, MyTokenSpacing.xxs)
        assertEquals(4.dp, MyTokenSpacing.xs)
        assertEquals(8.dp, MyTokenSpacing.sm)
        assertEquals(12.dp, MyTokenSpacing.md)
        assertEquals(16.dp, MyTokenSpacing.lg)
        assertEquals(24.dp, MyTokenSpacing.xl)
        assertEquals(32.dp, MyTokenSpacing.xxl)
        assertEquals(16.dp, MyTokenSpacing.page)
        assertEquals(14.dp, MyTokenSpacing.cardCompact)
        assertEquals(16.dp, MyTokenSpacing.cardFull)
        assertEquals(8.dp, MyTokenSpacing.gridCompact)
        assertEquals(12.dp, MyTokenSpacing.gridFull)
        assertEquals(24.dp, MyTokenSpacing.section)
        assertEquals(18.dp, MyTokenSpacing.settingsSection)

        assertEquals(20.dp, MyTokenRadius.card)
        assertEquals(12.dp, MyTokenRadius.inner)
        assertEquals(18.dp, MyTokenRadius.row)
        assertEquals(24.dp, MyTokenRadius.sheetTop)

        assertEquals(48.dp, MyTokenSize.touch)
        assertEquals(44.dp, MyTokenSize.header)
        assertEquals(64.dp, MyTokenSize.listRow)
        assertEquals(4.dp, MyTokenSize.healthBar)
        assertEquals(52.dp, MyTokenSize.ringCompact)
        assertEquals(64.dp, MyTokenSize.ringFull)
        assertEquals(88.dp, MyTokenSize.ringHero)
        assertEquals(46.dp, MyTokenSize.switchWidth)
        assertEquals(28.dp, MyTokenSize.switchHeight)
        assertEquals(44.dp, MyTokenSize.settingsRowMin)
    }

    @Test
    fun `only modal surfaces may cast shadow`() {
        assertEquals(
            MyTokenVisualPolicy.Material.Solid,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Card),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Solid,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Window),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Overlay,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Modal),
        )
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Card))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Window))
    }
}
