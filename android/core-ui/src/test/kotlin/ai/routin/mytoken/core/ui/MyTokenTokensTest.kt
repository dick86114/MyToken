package ai.routin.mytoken.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
        assertEquals(Color(0xFFFFFFFF), MyTokenLightColors.overlay)
        assertEquals(Color(0xFFF6F7F9), MyTokenLightColors.subtle)
        assertEquals(Color(0xFFE5E7EB), MyTokenLightColors.line)
        assertEquals(Color(0xFF17181A), MyTokenLightColors.ink)
        assertEquals(Color(0xFF6B7076), MyTokenLightColors.muted)
        // 交互强调色统一为报告票面黄，不再保留蓝色动作色。
        assertEquals(Color(0xFFF3CB73), MyTokenLightColors.actionFill)
        assertEquals(Color(0xFF625126), MyTokenLightColors.actionText)
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
        assertEquals(Color(0xFFE7E9EE), MyTokenLightColors.hairline)
        assertEquals(Color(0xFFE2E4E9), MyTokenLightColors.controlTrack)
        assertEquals(Color(0xFFD4D7DE), MyTokenLightColors.controlBorder)
        assertEquals(Color(0xFFFFFFFF), MyTokenLightColors.controlHandle)
        assertEquals(Color(0xFFA3A9B0), MyTokenLightColors.statusDotNormal)
        // 报告票面始终保持琥珀；只有折叠行百分比与收起标题按 severity 变红。
        assertEquals(Color(0xFFFF7A7D), MyTokenLightColors.reportCritical)
        assertEquals(Color(0xFFA3151A), MyTokenLightColors.reportNavCritical)
    }

    @Test
    fun `dark action tokens stay on the amber ticket`() {
        assertEquals(Color(0xFF061326), MyTokenDarkColors.onAction)
        assertEquals(Color(0xFFF3CB73), MyTokenDarkColors.actionFill)
        assertEquals(Color(0xFFF3CB73), MyTokenDarkColors.actionText)
        assertEquals(Color(0xFF1B1E24), MyTokenDarkColors.surface)
        assertEquals(Color(0xFF252931), MyTokenDarkColors.overlay)
        assertEquals(Color(0xFF121418), MyTokenDarkColors.canvas)
        assertEquals(Color(0xFF252931), MyTokenDarkColors.subtle)
        assertEquals(Color(0xFF2E333C), MyTokenDarkColors.line)
        assertEquals(Color(0xFFE6E8EC), MyTokenDarkColors.ink)
        assertEquals(Color(0xFFA7ADB7), MyTokenDarkColors.muted)
        assertEquals(Color(0xFF35D399), MyTokenDarkColors.positiveFill)
        assertEquals(Color(0xFF5BE0AC), MyTokenDarkColors.positiveText)
        assertEquals(Color(0xFFFFB340), MyTokenDarkColors.warningFill)
        assertEquals(Color(0xFFFFC266), MyTokenDarkColors.warningText)
        assertEquals(Color(0xFFFF6369), MyTokenDarkColors.criticalFill)
        assertEquals(Color(0xFFFF8A8E), MyTokenDarkColors.criticalText)
        assertEquals(Color(0xFFE6E8EC), MyTokenDarkColors.cat1)
        assertEquals(Color(0xFFA7ADB7), MyTokenDarkColors.cat2)
        assertEquals(Color(0xFF4A4F56), MyTokenDarkColors.cat3)
        assertEquals(Color(0xFF2A2F38), MyTokenDarkColors.track)
        assertEquals(Color(0xFF2A2F38), MyTokenDarkColors.controlTrack)
        assertEquals(Color(0xFF8A909A), MyTokenDarkColors.statusDotNormal)
        // 深色外观下报告正文是浅色纸面，Critical 百分比改用深红保证对比度。
        assertEquals(Color(0xFFC0272D), MyTokenDarkColors.reportCritical)
        assertEquals(Color(0xFFA3151A), MyTokenDarkColors.reportNavCritical)
    }

    @Test
    fun `dark body text reaches fifteen to one on canvas`() {
        fun ratio(fg: Color, bg: Color): Float {
            val lighter = maxOf(fg.luminance(), bg.luminance())
            val darker = minOf(fg.luminance(), bg.luminance())
            return (lighter + 0.05f) / (darker + 0.05f)
        }
        assertTrue(ratio(MyTokenDarkColors.ink, MyTokenDarkColors.canvas) >= 15f)
        assertTrue(ratio(MyTokenDarkColors.ink, MyTokenDarkColors.surface) >= 12f)
        assertTrue(ratio(MyTokenDarkColors.ink, MyTokenDarkColors.overlay) >= 10f)
    }

    @Test
    fun `dark three layer hierarchy keeps every step visible`() {
        fun ratio(a: Color, b: Color): Float {
            val lighter = maxOf(a.luminance(), b.luminance())
            val darker = minOf(a.luminance(), b.luminance())
            return (lighter + 0.05f) / (darker + 0.05f)
        }
        val colors = MyTokenDarkColors
        // 三层标准：canvas（页面背景）→ surface（卡片/区块）→ overlay（卡片上的元素/浮层）。
        assertTrue(colors.canvas.luminance() < colors.surface.luminance())
        assertTrue(colors.surface.luminance() < colors.overlay.luminance())
        // 相邻层要能分辨（深色下约为 1.10 / 1.15）。
        assertTrue(ratio(colors.surface, colors.canvas) >= 1.08f)
        assertTrue(ratio(colors.overlay, colors.surface) >= 1.08f)
        // 正文对页面背景仍保持 ≥15:1。
        assertTrue(ratio(colors.ink, colors.canvas) >= 15f)
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
    fun `drawers and floating surfaces may cast shadow`() {
        assertEquals(
            MyTokenVisualPolicy.Material.Solid,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Card),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Solid,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.FloatingCard),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Solid,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Window),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Overlay,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Modal),
        )
        assertEquals(
            MyTokenVisualPolicy.Material.Overlay,
            MyTokenVisualPolicy.materialFor(MyTokenVisualPolicy.SurfaceRole.Drawer),
        )
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Modal))
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Drawer))
        assertTrue(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.FloatingCard))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Card))
        assertFalse(MyTokenVisualPolicy.allowsShadow(MyTokenVisualPolicy.SurfaceRole.Window))
    }
}
