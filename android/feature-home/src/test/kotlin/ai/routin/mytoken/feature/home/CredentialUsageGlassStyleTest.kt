package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定 Android 简洁模式方案 A 的无内层底板、细弧与发丝分隔契约。 */
class CredentialUsageGlassStyleTest {
    private val source = sourceText("CredentialUsageCard.kt")
    private val ringSource = sourceText("UsageRing.kt")

    @Test
    fun `简洁模式不再使用内层玻璃底板`() {
        assertFalse(source.contains("usageTileGlass"))
        assertFalse(source.contains("private fun Modifier.ringTileBackground"))
        assertTrue(source.contains("private fun CompactBalanceLayout"))
        assertTrue(source.contains("private fun CompactValueGrid"))
    }

    @Test
    fun `简洁进度文字和数值不随状态变色`() {
        assertFalse(ringSource.contains("labelColor = when (tone)"))
        assertTrue(ringSource.contains("MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)"))
        assertTrue(ringSource.contains("CompactGaugeStartAngle = 135f"))
        assertTrue(ringSource.contains("CompactGaugeSweepAngle = 270f"))

        val bodyStart = requireNotNull(source.indexOf("private fun CompactUsageCardBody"))
        val ringStart = requireNotNull(source.indexOf("private fun CompactRingMetrics"))
        val compactBody = source.substring(bodyStart, ringStart)
        assertFalse(compactBody.contains("toneColor"))

        val ringTileStart = requireNotNull(source.indexOf("private fun CompactRingTile"))
        val fullBodyStart = requireNotNull(source.indexOf("private fun FullUsageCardBody"))
        val compactRing = source.substring(ringTileStart, fullBodyStart)
        assertFalse(compactRing.contains("UsageProgressBar"))
        assertTrue(compactRing.contains("CompactArcGauge"))
        assertFalse(compactRing.contains("usageTileGlass"))
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
