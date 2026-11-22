package ai.routin.mytoken.core.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定设置行的紧凑密度：行内边距收紧，分节间距可单独配置。 */
class MyTokenSettingsDensityTest {
    private val source = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        .resolve("src/main/kotlin/ai/routin/mytoken/core/ui/Components.kt")
        .readText()

    @Test
    fun `设置行垂直内边距收紧`() {
        val row = source
            .substringAfter("fun MyTokenRow(")
            .substringBefore("/** 兼容壳：旧设置页的 SettingRow")

        assertTrue("设置行上下内边距改为 2dp", row.contains(".padding(vertical = 2.dp)"))
    }

    @Test
    fun `分节卡片支持更紧凑的内容间距`() {
        assertTrue(source.contains("contentPadding: Dp = MyTokenSpacing.cardFull"))
        assertTrue(source.contains("contentSpacing: Dp = MyTokenSpacing.md"))
        assertTrue(source.contains(".padding(contentPadding)"))
        assertTrue(source.contains("Arrangement.spacedBy(contentSpacing)"))
    }
}
