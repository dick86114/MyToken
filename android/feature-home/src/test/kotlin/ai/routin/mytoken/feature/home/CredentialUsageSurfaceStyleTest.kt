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

    @Test
    fun `报告在任何健康状态都保持琥珀票面`() {
        val report = sourceText("HomeHealthReport.kt")
        // 用户明确覆盖设计稿：全部正常时也不再退回灰色。
        assertTrue(report.contains(".background(colors.reportTicket, ReportTicketShape)"))
        assertFalse(report.contains("reportTicketNormal"))
        assertFalse(report.contains("reportDialTrackNormal"))
    }

    @Test
    fun `刷新按钮使用设计稿图标并反色`() {
        val report = sourceText("HomeHealthReport.kt")
        val home = sourceText("HomeScreen.kt")
        val glyph = sourceText("HomeRefreshGlyph.kt")

        assertTrue(report.contains("ReportRefreshGlyph"))
        assertTrue(home.contains("ReportRefreshGlyph"))
        assertFalse(report.contains("Icons.Outlined.Refresh"))
        assertFalse(home.contains("Icons.Outlined.Refresh"))
        assertTrue(glyph.contains("M20 7v5h-5"))
        assertTrue(glyph.contains("tint: Color"))
        assertTrue("刷新图标应使用报告底部底色对应的反色", report.contains("tint = colors.reportInk"))
        // 报告右下角只保留透明图标，不再有黄色圆形底。
        assertFalse(report.contains("report_refresh_circle"))
    }

    @Test
    fun `报告左下角同步文案加粗`() {
        val report = sourceText("HomeHealthReport.kt")
        val footer = report
            .substringAfter("text = \"最近同步")
            .substringBefore("Box(")

        assertTrue("最近同步文案应加粗", footer.contains("fontWeight = FontWeight.SemiBold"))
    }

    @Test
    fun `下拉刷新指示器不再有整行底色承托`() {
        val home = sourceText("HomeScreen.kt")
        val pullBody = home
            .substringAfter("private fun HomePullIndicator")
            .substringBefore("@Composable\nprivate fun CredentialEndActions")

        assertTrue(pullBody.contains("ReportRefreshGlyph"))
        assertFalse("下拉指示器本身不应铺整行或圆形底色", pullBody.contains(".background("))
        assertFalse(pullBody.contains(".fillMaxWidth()"))
    }

    @Test
    fun `头部底色跟随收起位移而不是留在原位遮挡卡片`() {
        val home = sourceText("HomeScreen.kt")
        val header = home
            .substringAfter("// 报告和工具条是覆盖层")
            .substringBefore(".testTag(\"home_collapsing_header\")")
        val layer = header.indexOf(".graphicsLayer")
        val background = header.indexOf(".background(colors.canvas)")

        assertTrue("头部缺少位移图层", layer >= 0)
        assertTrue("头部缺少实色底", background >= 0)
        assertTrue(
            "头部底色必须画在位移民图层内部，否则收起后仍会覆盖下方卡片",
            layer < background,
        )
    }

    @Test
    fun `密度切换组件不做加载动画`() {
        val chrome = sourceText("HomeChrome.kt")

        assertFalse("分页过程中切换组件不应播放颜色加载动画", chrome.contains("animateColorAsState"))
        assertTrue(chrome.contains("tint = if (selected) colors.canvas else colors.muted"))
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
