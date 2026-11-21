package ai.routin.mytoken.core.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定下拉菜单的实色表面、hairline 与统一字号。 */
class MyTokenMenuStyleTest {
    @Test
    fun 下拉菜单使用实色表面与hairline() {
        val source = sourceText("MyTokenMenu.kt")

        assertTrue(source.contains("shape = RoundedCornerShape(MyTokenRadius.inner)"))
        assertTrue(source.contains("containerColor = colors.surface"))
        assertTrue(source.contains("tonalElevation = 0.dp"))
        assertTrue(source.contains("shadowElevation = 12.dp"))
        assertTrue(source.contains("border = BorderStroke(1.dp, colors.line)"))
        assertTrue(source.contains("MyTokenType.body"))
        assertFalse(source.contains("HorizontalDivider"))
    }

    private fun sourceText(name: String): String {
        val workingDir = File(System.getProperty("user.dir") ?: error("缺少 user.dir"))
        val candidates = listOf(
            workingDir.resolve("src/main/kotlin/ai/routin/mytoken/core/ui/$name"),
            workingDir.resolve("android/core-ui/src/main/kotlin/ai/routin/mytoken/core/ui/$name"),
        )
        val file = candidates.firstOrNull(File::isFile) ?: error("找不到源文件：$name")
        return file.readText()
    }
}
