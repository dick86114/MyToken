package ai.routin.mytoken

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 视觉契约门禁：颜色、字号、表面只能来自 core-ui token。
 * 扫描 android/ 下各模块 src/main 的 Kotlin 源文件（跳过 build 产物）。
 */
class DesignTokenLintTest {

    private val androidRoot: File = File(System.getProperty("user.dir") ?: ".").parentFile

    private fun sourceFiles(): List<File> {
        return androidRoot.walkTopDown()
            .onEnter { dir -> dir.name != "build" && dir.name != ".gradle" }
            .filter { it.isFile && it.extension == "kt" && it.path.contains("${File.separator}src${File.separator}main${File.separator}") }
            .toList()
    }

    private fun violations(predicate: (File, String) -> Boolean): List<String> =
        sourceFiles().mapNotNull { file ->
            val text = file.readText()
            if (predicate(file, text)) file.relativeTo(androidRoot).path else null
        }

    @Test
    fun noInlineColorsOutsideCoreUi() {
        val offenders = violations { file, text ->
            val inCoreUi = file.path.contains("${File.separator}core-ui${File.separator}")
            val inWidget = file.path.contains("${File.separator}widget${File.separator}")
            !inCoreUi && !inWidget && Regex("""Color\(0x""").containsMatchIn(text)
        }
        assertTrue("发现 core-ui 之外的内联颜色：$offenders", offenders.isEmpty())
    }

    @Test
    fun noInlineFontSizeOrTrackingInFeatureCode() {
        val offenders = violations { file, text ->
            val inFeature = file.path.contains("${File.separator}feature-")
            val inlineSize = Regex("""fontSize = \d+(\.\d+)?\.sp""").containsMatchIn(text)
            val inlineTracking = Regex("""letterSpacing = \(?-?\d+(\.\d+)?\)?\.sp""").containsMatchIn(text)
            inFeature && (inlineSize || inlineTracking)
        }
        assertTrue("feature 模块仍有内联字号/字距：$offenders", offenders.isEmpty())
    }

    @Test
    fun noAlphaCardSurfaces() {
        val offenders = violations { file, text ->
            val inFeature = file.path.contains("${File.separator}feature-")
            inFeature && text.contains("MaterialTheme.colorScheme.surface.copy(alpha")
        }
        assertTrue("卡片不应叠半透明表面：$offenders", offenders.isEmpty())
    }

    @Test
    fun noGlassIdentifiers() {
        val pattern = Regex("GlassButton|GlassSwitch|GlassTextField|GlassHelpButton|LiquidGlassSurface")
        val offenders = violations { _, text -> pattern.containsMatchIn(text) }
        assertTrue("仍有 Glass 命名：$offenders", offenders.isEmpty())
    }

    @Test
    fun noLegacyPalette() {
        val offenders = violations { _, text -> text.contains("MyTokenPalette") }
        assertTrue("仍有 MyTokenPalette 引用：$offenders", offenders.isEmpty())
    }
}
