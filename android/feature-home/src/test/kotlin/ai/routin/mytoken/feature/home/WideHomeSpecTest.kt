package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
    fun 只有十六比九横排报告其余竖排() {
        assertEquals(
            WideReportOrientation.Horizontal,
            WideHomeSpec.reportOrientation(1280f / 720f),
        )
        assertEquals(
            WideReportOrientation.Vertical,
            WideHomeSpec.reportOrientation(4f / 3f),
        )
        assertEquals(
            WideReportOrientation.Vertical,
            WideHomeSpec.reportOrientation(1f),
        )
        assertEquals(
            WideReportOrientation.Vertical,
            WideHomeSpec.reportOrientation(3f / 4f),
        )
        assertEquals(
            WideReportOrientation.Vertical,
            WideHomeSpec.reportOrientation(9f / 16f),
        )
    }

    @Test
    fun 瀑布流列数按二百五十最小卡宽自适应() {
        assertEquals(4, WideHomeSpec.columnCount(1176.dp, fontScale = 1f))
        assertEquals(3, WideHomeSpec.columnCount(896.dp, fontScale = 1f))
        assertEquals(2, WideHomeSpec.columnCount(604.dp, fontScale = 1f))
        assertEquals(1, WideHomeSpec.columnCount(332.dp, fontScale = 1f))
        assertEquals(1, WideHomeSpec.columnCount(1176.dp, fontScale = 1.3f))
    }

    @Test
    fun 报告短票与告警高度固定为设计值() {
        assertEquals(80.dp, WideHomeSpec.normalReportHeight)
        assertEquals(110.dp, WideHomeSpec.alertReportHeight)
    }
}
