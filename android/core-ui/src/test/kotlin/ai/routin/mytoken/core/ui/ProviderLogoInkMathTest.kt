package ai.routin.mytoken.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 纯像素计算的边界：只统计不透明像素，半透明与全透明像素不参与平均。 */
class ProviderLogoInkMathTest {

    @Test
    fun `average luminance ignores transparent pixels`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val transparentWhite = 0x00FFFFFF
        val translucentBlack = 0x7F000000

        // 一黑一白两个不透明像素：平均亮度 0.5；全透明与半透明像素都不计入。
        val luminance = ProviderLogoInk.averageOpaqueLuminance(
            intArrayOf(black, white, transparentWhite, translucentBlack),
        )

        assertEquals(0.5f, luminance, 0.001f)
    }

    @Test
    fun `empty logo is treated as light`() {
        assertEquals(1f, ProviderLogoInk.averageOpaqueLuminance(IntArray(0)), 0f)
    }

    @Test
    fun `dark threshold separates black from colored logos`() {
        val black = IntArray(4) { 0xFF000000.toInt() }
        val midBlue = IntArray(4) { 0xFF3B82F6.toInt() }

        assertTrue(ProviderLogoInk.averageOpaqueLuminance(black) < ProviderLogoInk.DARK_LOGO_LUMINANCE_THRESHOLD)
        assertFalse(ProviderLogoInk.averageOpaqueLuminance(midBlue) < ProviderLogoInk.DARK_LOGO_LUMINANCE_THRESHOLD)
    }
}
