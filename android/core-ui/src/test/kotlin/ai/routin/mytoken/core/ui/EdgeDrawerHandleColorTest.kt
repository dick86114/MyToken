package ai.routin.mytoken.core.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定抽屉拖拽手柄的黄色强调，避免回退到灰色半透明。 */
class EdgeDrawerHandleColorTest {
    private val source = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        .resolve("src/main/kotlin/ai/routin/mytoken/core/ui/EdgeDrawer.kt")
        .readText()

    @Test
    fun `抽屉拖拽手柄使用黄色`() {
        val handle = source
            .substringAfter(".width(3.dp)")
            .substringBefore(".testTag(")

        assertTrue(
            "拖拽手柄应使用行动黄",
            handle.contains("background(MyTokenThemeTokens.colors.actionFill)"),
        )
        assertFalse("拖拽手柄不再使用灰色遮罩", handle.contains("onSurface.copy"))
        assertTrue("拖拽手柄长度缩短到 48dp", handle.contains(".height(48.dp)"))
    }
}
