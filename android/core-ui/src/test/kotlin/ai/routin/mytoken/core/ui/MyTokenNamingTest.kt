package ai.routin.mytoken.core.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** P0 重命名契约：源码里不再出现旧的 Glass / LiquidGlass 命名。 */
class MyTokenNamingTest {

    @Test
    fun `main sources contain no legacy glass identifiers`() {
        val androidRoot = File(System.getProperty("user.dir") ?: error("缺少 user.dir")).parentFile
        val banned = listOf(
            "Glass" + "Button",
            "Glass" + "ButtonTone",
            "Glass" + "Switch",
            "Glass" + "TextField",
            "Glass" + "HelpButton",
            "Liquid" + "GlassSurface",
            "glass" + "FilterChipColors",
            "glass" + "FilterChipBorder",
        )
        val offenders = androidRoot.walkTopDown()
            .filter { it.isFile && it.path.contains("${File.separator}src${File.separator}main${File.separator}kotlin${File.separator}") }
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                banned.filter { text.contains(it) }.map { "${file.relativeTo(androidRoot)}: $it" }
            }
            .toList()
        assertTrue("仍存在旧命名：$offenders", offenders.isEmpty())
    }
}
