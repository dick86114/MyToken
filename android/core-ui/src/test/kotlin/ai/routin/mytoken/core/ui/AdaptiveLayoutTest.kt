package ai.routin.mytoken.core.ui

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun 窗口宽度映射到三档布局模式() {
        assertEquals(
            MyTokenLayoutMode.Compact,
            WindowSizeClass.compute(599f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Medium,
            WindowSizeClass.compute(600f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Medium,
            WindowSizeClass.compute(839f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
        assertEquals(
            MyTokenLayoutMode.Expanded,
            WindowSizeClass.compute(840f, 800f).windowWidthSizeClass.toMyTokenLayoutMode(),
        )
    }

    @Test
    fun 网格列数按可用宽度限制() {
        assertEquals(1, adaptiveGridColumns(484.dp, 2, 260.dp, 12.dp))
        assertEquals(2, adaptiveGridColumns(596.dp, 2, 260.dp, 12.dp))
        assertEquals(3, adaptiveGridColumns(808.dp, 3, 260.dp, 12.dp))
        assertEquals(2, adaptiveGridColumns(812.dp, 3, 400.dp, 12.dp))
    }

    @Test
    fun 布局模式为各页面提供列数上限() {
        assertEquals(1, MyTokenLayoutMode.Compact.maxColumns(1, 2, 3))
        assertEquals(2, MyTokenLayoutMode.Medium.maxColumns(1, 2, 3))
        assertEquals(3, MyTokenLayoutMode.Expanded.maxColumns(1, 2, 3))
    }
}
