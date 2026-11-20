package ai.routin.mytoken.core.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** 锁定设计文档 §6.3 的七级字号与数字排版规则。 */
class MyTokenTypographyTest {

    @Test
    fun `seven levels match the approved scale`() {
        assertEquals(32.sp, MyTokenType.display.fontSize)
        assertEquals(36.sp, MyTokenType.display.lineHeight)
        assertEquals(FontWeight(750), MyTokenType.display.fontWeight)
        assertEquals(24.sp, MyTokenType.displayCompact.fontSize)
        assertEquals(28.sp, MyTokenType.displayCompact.lineHeight)
        assertEquals(22.sp, MyTokenType.title.fontSize)
        assertEquals(28.sp, MyTokenType.title.lineHeight)
        assertEquals(FontWeight(700), MyTokenType.title.fontWeight)
        assertEquals(16.sp, MyTokenType.section.fontSize)
        assertEquals(22.sp, MyTokenType.section.lineHeight)
        assertEquals(FontWeight(650), MyTokenType.section.fontWeight)
        assertEquals(14.sp, MyTokenType.body.fontSize)
        assertEquals(21.sp, MyTokenType.body.lineHeight)
        assertEquals(12.5.sp, MyTokenType.label.fontSize)
        assertEquals(18.sp, MyTokenType.label.lineHeight)
        assertEquals(11.5.sp, MyTokenType.caption.fontSize)
        assertEquals(16.sp, MyTokenType.caption.lineHeight)
        assertEquals(11.sp, MyTokenType.micro.fontSize)
        assertEquals(15.sp, MyTokenType.micro.lineHeight)
    }

    @Test
    fun `numeric variants match the approved scale`() {
        assertEquals(20.sp, MyTokenType.detailTitle.fontSize)
        assertEquals(15.sp, MyTokenType.listTitle.fontSize)
        assertEquals(12.5.sp, MyTokenType.ringCompact.fontSize)
        assertEquals(16.sp, MyTokenType.ringCompact.lineHeight)
        assertEquals(15.sp, MyTokenType.ringFull.fontSize)
        assertEquals(19.sp, MyTokenType.ringFull.lineHeight)
        assertEquals(9.sp, MyTokenType.percentMark.fontSize)
        assertEquals(18.sp, MyTokenType.detailRemaining.fontSize)
        assertEquals(24.sp, MyTokenType.detailRemaining.lineHeight)
        assertEquals(18.sp, MyTokenType.numericValue.fontSize)
    }

    @Test
    fun `numbers are tabular`() {
        listOf(
            MyTokenType.display,
            MyTokenType.displayCompact,
            MyTokenType.ringCompact,
            MyTokenType.ringFull,
            MyTokenType.detailRemaining,
            MyTokenType.numericValue,
        ).forEach { assertEquals("tnum", it.fontFeatureSettings) }
        assertEquals((-0.8f).sp, MyTokenType.display.letterSpacing)
        assertEquals((-0.6f).sp, MyTokenType.displayCompact.letterSpacing)
        assertEquals((-0.3f).sp, MyTokenType.ringCompact.letterSpacing)
        assertEquals((-0.5f).sp, MyTokenType.ringFull.letterSpacing)
    }

    @Test
    fun `chinese tokens do not use negative tracking`() {
        listOf(
            MyTokenType.title,
            MyTokenType.section,
            MyTokenType.body,
            MyTokenType.label,
            MyTokenType.caption,
            MyTokenType.detailTitle,
            MyTokenType.listTitle,
        ).forEach { assertEquals(0.sp, it.letterSpacing) }
    }
}
