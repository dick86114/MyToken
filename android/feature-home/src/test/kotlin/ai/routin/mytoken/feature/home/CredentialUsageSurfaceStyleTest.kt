package ai.routin.mytoken.feature.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 锁定卡片骨架的实色表面、无内部线、270° 弧线与统一状态表达。 */
class CredentialUsageSurfaceStyleTest {
    private val source = sourceText("CredentialUsageCard.kt")
    private val skeleton = sourceText("UsageCardSkeleton.kt")
    private val ringSource = sourceText("UsageRing.kt")

    @Test
    fun `卡片内部不再有分割线`() {
        assertFalse(source.contains("HorizontalDivider"))
        assertFalse(skeleton.contains("HorizontalDivider"))
        assertFalse(source.contains("Modifier.width(1.dp)"))
    }

    @Test
    fun `卡片不再叠半透明表面`() {
        assertFalse(source.contains("surface.copy(alpha"))
        assertFalse(source.contains("StatusPill("))
        assertFalse(skeleton.contains("surface.copy(alpha"))
    }

    @Test
    fun `弧线统一为270度几何`() {
        assertTrue(ringSource.contains("ArcStartAngle = 135f"))
        assertTrue(ringSource.contains("ArcSweepAngle = 270f"))
        assertTrue(source.contains("UsageArcGauge"))
        assertFalse(ringSource.contains("drawArc(\n                color = track,\n                startAngle = 0f"))
    }

    @Test
    fun `简洁卡片使用骨架与迷你窗口行`() {
        assertTrue(source.contains("UsageCardFrame"))
        assertTrue(source.contains("UsageCardIdentity"))
        assertTrue(skeleton.contains("compact_window_row_"))
        assertTrue(source.contains("RingWithWindows"))
    }

    @Test
    fun `正常账户状态使用中性色文字`() {
        val full = sourceText("FullSplitUsageContent.kt")
        // 规格 §7.6：正常 = 中性点 + 中性文字；绿色只用于数据可视化。
        assertTrue(full.contains("true -> MyTokenStatusTone.Neutral"))
        assertFalse(full.contains("true -> MyTokenStatusTone.Positive"))
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
