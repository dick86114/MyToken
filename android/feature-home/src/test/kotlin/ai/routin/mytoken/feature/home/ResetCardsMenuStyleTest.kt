package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定重置卡浮层的实色弹层样式与列表排版。 */
class ResetCardsMenuStyleTest {
    @Test
    fun `重置卡浮窗使用mac端相同的列表样式`() {
        val source = sourceText("UsageMetricGrid.kt")
        val start = requireNotNull(source.indexOf("DropdownMenu("))
        val end = source.length
        val menu = source.substring(start, end)

        assertTrue(menu.contains("shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp)"))
        assertTrue(menu.contains("resetCardsMenuContainer()"))
        assertTrue(menu.contains("border = BorderStroke(1.dp, resetCardsMenuBorder())"))
        assertTrue(menu.contains("Modifier.width(210.dp)"))
        assertTrue(menu.contains("padding(all = 12.dp)"))
        assertTrue(menu.contains("Modifier.width(14.dp)"))
        assertTrue(menu.contains("textAlign = TextAlign.End"))
        assertTrue(menu.contains("fontFamily = FontFamily.Monospace"))
        assertFalse(menu.contains("text = \"${'$'}{index + 1}  "))
    }

    @Test
    fun `重置卡浮窗使用实色表面token`() {
        val source = sourceText("UsageMetricGrid.kt")

        assertTrue(source.contains("private fun resetCardsMenuContainer(): Color = MyTokenThemeTokens.colors.surface"))
        assertTrue(source.contains("private fun resetCardsMenuBorder(): Color = MyTokenThemeTokens.colors.line"))
        assertFalse(source.contains("copy(alpha = 0.96f)"))
        assertFalse(source.contains("Color.White.copy(alpha = 0.14f)"))
        assertFalse(source.contains("Color.Black.copy(alpha = 0.08f)"))
    }

    private fun sourceText(name: String): String {
        val workingDir = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        val candidates = listOf(
            workingDir.resolve("src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
            workingDir.resolve("android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("找不到源文件：$name")
        return file.readText()
    }
}
