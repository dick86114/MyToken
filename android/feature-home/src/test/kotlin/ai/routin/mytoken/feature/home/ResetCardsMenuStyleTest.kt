package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定 Android 重置卡浮窗与 macOS popover 相同的卡片式排版。 */
class ResetCardsMenuStyleTest {
    @Test
    fun `重置卡浮窗使用mac端相同的列表样式`() {
        val source = sourceText("UsageMetricGrid.kt")
        val start = requireNotNull(source.indexOf("DropdownMenu("))
        val end = source.length
        val menu = source.substring(start, end)

        assertTrue(menu.contains("shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp)"))
        assertTrue(menu.contains("resetCardsMenuContainer(isDark)"))
        assertTrue(menu.contains("border = BorderStroke(1.dp, resetCardsMenuBorder(isDark))"))
        assertTrue(menu.contains("Modifier.width(210.dp)"))
        assertTrue(menu.contains("padding(all = 12.dp)"))
        assertTrue(menu.contains("HorizontalDivider"))
        assertTrue(menu.contains("Modifier.width(14.dp)"))
        assertTrue(menu.contains("textAlign = TextAlign.End"))
        assertTrue(menu.contains("fontFamily = FontFamily.Monospace"))
        assertFalse(menu.contains("text = \"${'$'}{index + 1}  "))
    }

    @Test
    fun `重置卡浮窗使用中性弹层底色`() {
        val source = sourceText("UsageMetricGrid.kt")

        assertTrue(source.contains("private fun resetCardsMenuContainer(isDark: Boolean): Color"))
        assertTrue(source.contains("Color.White.copy(alpha = 0.96f)"))
        assertTrue(source.contains("MyTokenPalette.darkSurface.copy(alpha = 0.96f)"))
        assertTrue(source.contains("private fun resetCardsMenuBorder(isDark: Boolean): Color"))
        assertTrue(source.contains("Color.Black.copy(alpha = 0.08f)"))
        assertTrue(source.contains("Color.White.copy(alpha = 0.14f)"))
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
