package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WideHomeSpecTest {
    @Test
    fun 左轨在中等与展开宽度使用不同宽度() {
        assertEquals(64.dp, WideHomeSpec.railWidth(MyTokenLayoutMode.Medium))
        assertEquals(72.dp, WideHomeSpec.railWidth(MyTokenLayoutMode.Expanded))
    }

    @Test
    fun 详情宽度按窗口比例选择() {
        assertEquals(380.dp, WideHomeSpec.detailWidth(1280f / 720f))
        assertEquals(380.dp, WideHomeSpec.detailWidth(1.6f))
        assertEquals(360.dp, WideHomeSpec.detailWidth(1f))
        assertEquals(360.dp, WideHomeSpec.detailWidth(1.05f))
        assertEquals(340.dp, WideHomeSpec.detailWidth(4f / 3f))
        assertEquals(340.dp, WideHomeSpec.detailWidth(3f / 4f))
        assertEquals(340.dp, WideHomeSpec.detailWidth(9f / 16f))
    }

    @Test
    fun 宽大于高即横排报告含四比三其余竖排() {
        assertEquals(WideReportOrientation.Horizontal, WideHomeSpec.reportOrientation(true))
        assertEquals(WideReportOrientation.Vertical, WideHomeSpec.reportOrientation(false))
    }

    @Test
    fun 简洁页以手机竖屏卡宽一百七十六为基准分列() {
        // 1280×720：中间可用 1176dp → 6 列，卡宽约 189dp（拉伸 7.6%）。
        assertEquals(6, WideHomeSpec.compactColumns(1176.dp, fontScale = 1f))
        assertEquals(5, WideHomeSpec.compactColumns(896.dp, fontScale = 1f))
        assertEquals(3, WideHomeSpec.compactColumns(604.dp, fontScale = 1f))
        // 不够两张 176 时只用 1 列，不把卡压得过窄。
        assertEquals(2, WideHomeSpec.compactColumns(332.dp, fontScale = 1f))
        // 动态字体放大时基准卡宽同步放大，列数减少。
        assertEquals(5, WideHomeSpec.compactColumns(1176.dp, fontScale = 1.3f))
    }

    @Test
    fun 完整页以手机完整卡宽三百六十为基准分列() {
        assertEquals(3, WideHomeSpec.fullColumns(1176.dp, fontScale = 1f))
        assertEquals(3, WideHomeSpec.fullColumns(920.dp, fontScale = 1f))
        assertEquals(2, WideHomeSpec.fullColumns(604.dp, fontScale = 1f))
        assertEquals(1, WideHomeSpec.fullColumns(332.dp, fontScale = 1f))
        assertEquals(3, WideHomeSpec.fullColumns(1176.dp, fontScale = 1.3f))
    }

    @Test
    fun 分列后的卡片拉伸不超过百分之十二() {
        val widths = listOf(1176.dp, 920.dp, 896.dp, 604.dp, 332.dp)
        for (width in widths) {
            for (base in listOf(WideHomeSpec.compactBaseCardWidth, WideHomeSpec.fullBaseCardWidth)) {
                val spacing = if (base == WideHomeSpec.compactBaseCardWidth) {
                    WideHomeSpec.compactCardSpacing
                } else {
                    WideHomeSpec.fullCardSpacing
                }
                val columns = WideHomeSpec.columnCount(width, base, spacing, fontScale = 1f)
                val cardWidth = (width - spacing * (columns - 1)) / columns
                assertTrue(
                    "card $cardWidth exceeds 12% stretch at $width / $base",
                    cardWidth <= base * WideHomeSpec.maxCardStretch + 0.01.dp,
                )
            }
        }
    }

    @Test
    fun 全部供应商合成图标按数量分列() {
        assertEquals(1, WideHomeSpec.allProviderLogoColumns(1))
        assertEquals(2, WideHomeSpec.allProviderLogoColumns(2))
        assertEquals(2, WideHomeSpec.allProviderLogoColumns(4))
        assertEquals(3, WideHomeSpec.allProviderLogoColumns(5))
        assertEquals(3, WideHomeSpec.allProviderLogoColumns(6))
        assertEquals(3, WideHomeSpec.allProviderLogoColumns(9))
    }

    @Test
    fun 报告短票与告警高度固定为设计值() {
        assertEquals(80.dp, WideHomeSpec.normalReportHeight)
        assertEquals(110.dp, WideHomeSpec.alertReportHeight)
    }
}
