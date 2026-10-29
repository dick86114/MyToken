package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定 Android 简洁用量小卡片与 macOS 一致的无色玻璃契约。 */
class CredentialUsageGlassStyleTest {
    private val source = sourceText("CredentialUsageCard.kt")
    private val ringSource = sourceText("UsageRing.kt")

    @Test
    fun `简洁用量小卡片使用无色玻璃底`() {
        val glassStart = requireNotNull(source.indexOf("private fun Modifier.usageTileGlass"))
        val glassEnd = source.indexOf("private fun BalanceStrip")
        assertTrue(glassStart >= 0)
        assertTrue(glassEnd > glassStart)
        val glass = source.substring(glassStart, glassEnd)

        assertFalse(source.contains("private fun Modifier.ringTileBackground"))
        assertTrue(source.count("usageTileGlass(isDark)") >= 4)
        assertTrue(glass.contains("Color.White.copy"))
        assertFalse(glass.contains("colors.warning"))
        assertFalse(glass.contains("colors.critical"))
        assertFalse(glass.contains("tone"))
    }

    @Test
    fun `简洁进度文字和数值不随状态变色`() {
        assertFalse(ringSource.contains("labelColor = when (tone)"))
        assertTrue(ringSource.contains("MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)"))

        val bodyStart = requireNotNull(source.indexOf("private fun CompactUsageCardBody"))
        val glassStart = requireNotNull(source.indexOf("private fun Modifier.usageTileGlass"))
        val compactBody = source.substring(bodyStart, glassStart)
        assertFalse(compactBody.contains("toneColor"))

        val rowStart = requireNotNull(source.indexOf("private fun CompactMetricRow"))
        val rowEnd = requireNotNull(source.indexOf("private fun compactValueText"))
        val percentTextEnd = requireNotNull(source.indexOf("UsageProgressBar", rowStart))
        val percentText = source.substring(rowStart, percentTextEnd)
        assertFalse(percentText.contains("progressColor(percent, colors)"))
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

    private fun String.count(value: String): Int = split(value).size - 1
}
