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
    fun `仪表盘改为主机面板分段样式`() {
        assertTrue("12 段仪表刻度", ringSource.contains("SegmentCount = 12"))
        assertTrue("从 135° 起", ringSource.contains("ArcStartAngle = 135f"))
        assertTrue("每段 18.1°", ringSource.contains("SegmentSweepAngle = 18.1f"))
        assertTrue("段间留 2.2°", ringSource.contains("SegmentGapDegrees = 2.2f"))
        assertTrue("外圈表框", ringSource.contains("drawCircle"))
        assertTrue("内圈轴心", ringSource.contains("HubRadiusRatio"))
        assertTrue(source.contains("UsageArcGauge"))
    }

    @Test
    fun `进度条改为十二格分段仪表`() {
        val meter = sourceText("UsageSegmentMeter.kt")
        assertTrue("统一 12 格", meter.contains("SegmentCells = 12"))
        assertTrue("正常态使用墨色", meter.contains("UsageMetricTone.Normal -> colors.ink"))
        assertTrue("简洁卡窗口行使用分段仪表", source.contains("UsageSegmentMeter("))
        assertTrue(
            "完整卡周期行使用分段仪表",
            sourceText("FullSplitUsageContent.kt").contains("UsageSegmentMeter("),
        )
    }

    @Test
    fun `完整卡按设计稿收敛为周期账本`() {
        val full = sourceText("FullSplitUsageContent.kt")
        assertTrue("主指标使用统一 Hero 组件", full.contains("FullHeroFocus("))
        assertTrue("完整卡周期行独立组件", full.contains("FullPeriodRow("))
        assertTrue("单窗口完整卡居中展示", full.contains("if (secondary.isEmpty())"))
        assertTrue("New API 使用事实网格", full.contains("FullFactsGrid("))
        assertFalse("完整额度卡不再平铺事实列表", full.contains("FullFacts(facts = facts"))
        assertFalse("CommandCode 完整卡不再内嵌额度构成", full.contains("FullCommandCodeSummary(byId)"))
        assertFalse("余额构成不再带账户事实", full.contains("full_balance_facts"))
    }

    @Test
    fun `完整卡补回调用事实并把余额构成移到右侧`() {
        val full = sourceText("FullSplitUsageContent.kt")
        assertTrue("GLM 完整卡展示调用次数", full.contains("调用次数"))
        assertTrue("GLM 完整卡展示累计 Tokens", full.contains("累计 Tokens"))
        val balanceHero = full
            .substringAfter("private fun FullBalanceHero")
            .substringBefore("@Composable\nprivate fun FullSecondaryBalance")
        assertTrue("余额构成改为左右分栏", balanceHero.contains("FullSplitLayout("))
        assertTrue("余额构成使用分段条", balanceHero.contains("UsageCompositionMeter("))
    }

    @Test
    fun `重置卡徽标只显示图标与数量`() {
        val grid = sourceText("UsageMetricGrid.kt")
        assertTrue("完整/详情徽标显示 × 数量", grid.contains("\"× "))
        assertFalse("徽标不再写重置卡文字", grid.contains("\"重置卡 "))
    }

    @Test
    fun `第一张卡片完整可见后报告与卡片一起下移`() {
        val home = sourceText("HomeScreen.kt")
        assertTrue(
            "需要判断第一张卡片是否完整可见",
            home.contains("fun activePageFirstCardFullyVisible"),
        )
        assertTrue(
            "卡片完整可见后，下拉消耗多少报告就展开多少",
            home.contains("consumed.y > 0f && activePageFirstCardFullyVisible()"),
        )
        assertTrue(
            "报告展开量与卡片下移量同步",
            home.contains("collapsedPx - consumed.y"),
        )
        assertFalse(
            "不再用 firstVisibleItemIndex 提前拦截下拉",
            home.contains("firstVisibleItemIndex == 0"),
        )
    }

    @Test
    fun `详情页沿用分段仪表与票据重置卡`() {
        val detail = sourceText("CredentialDetailHero.kt")
        val screen = sourceText("CredentialDetailScreen.kt")
        assertTrue("详情窗口使用分段仪表", detail.contains("UsageSegmentMeter("))
        assertTrue("详情余额构成使用分段条", detail.contains("UsageCompositionMeter("))
        assertTrue("重置卡改为票据", detail.contains("MyTokenReportBodyShape"))
        assertTrue("票据行有测试标记", detail.contains("detail_reset_ticket"))
        assertTrue(
            "删除按钮离开右边缘",
            screen.contains("modifier = Modifier.padding(end = 8.dp).testTag(\"detail_delete\")"),
        )
    }

    @Test
    fun `简洁卡片使用骨架与迷你窗口行`() {
        assertTrue(source.contains("UsageCardFrame"))
        assertTrue(source.contains("UsageCardIdentity"))
        assertTrue(skeleton.contains("compact_window_row_"))
        assertTrue(source.contains("RingWithWindows"))
    }

    @Test
    fun `完整卡不再内嵌账户状态行`() {
        val full = sourceText("FullSplitUsageContent.kt")
        // 新设计把状态收敛到卡片头部，余额主体只保留金额与构成。
        assertFalse(full.contains("MyTokenStatus("))
        assertFalse(full.contains("FullAccountStatus("))
    }

    @Test
    fun `报告在任何健康状态都保持琥珀票面`() {
        val report = sourceText("HomeHealthReport.kt")
        // 用户明确覆盖设计稿：全部正常时也不再退回灰色。
        assertTrue(report.contains(".background(colors.reportTicket, MyTokenReportTicketShape)"))
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
    fun `头部渐变底色跟随收起位移而不是留在原位遮挡卡片`() {
        val home = sourceText("HomeScreen.kt")
        val header = home
            .substringAfter("// 报告和工具条是覆盖层")
            .substringBefore(".testTag(\"home_collapsing_header\")")
        val layer = header.indexOf(".graphicsLayer")
        val backdrop = header.indexOf(".drawBehind")

        assertTrue("头部缺少位移图层", layer >= 0)
        assertTrue("头部缺少渐变遮罩", backdrop >= 0)
        assertTrue(
            "头部遮罩必须画在位移民图层内部，否则收起后仍会覆盖下方卡片",
            layer < backdrop,
        )
        assertFalse("头部不再铺实色画布", header.contains(".background(colors.canvas)"))
    }

    @Test
    fun `主页背景从上到下渐变为黄色`() {
        val home = sourceText("HomeScreen.kt")
        val root = home
            .substringAfter("Scaffold { padding ->")
            .substringBefore("MyTokenAdaptiveContent(")

        assertTrue("整页背景应使用垂直渐变", root.contains("Brush.verticalGradient"))
        assertTrue("渐变顶部随收起进度变黄", root.contains("collapseProgress()"))
        assertTrue("渐变底部回落到画布色", root.contains("1f to colors.canvas"))
        assertFalse("不再单独铺 60dp 导航色块", root.contains("height(60.dp)"))

        val chrome = sourceText("HomeChrome.kt")
        assertFalse(
            "导航栏不再单独绘制实色底",
            chrome.contains("drawRect(lerp(colors.canvas, colors.reportTicket"),
        )
    }

    @Test
    fun `详情与分享页使用指定图标`() {
        val detail = sourceText("CredentialDetailScreen.kt")
        val share = sourceText("UsageShareDialog.kt")

        assertTrue("分享入口应使用机票+飞机图标", detail.contains("AirplaneTicketGlyph"))
        assertTrue("机票图标内应叠加飞机", detail.contains("Icons.Filled.Flight"))
        assertTrue("删除按钮应为红色", detail.contains("tint = colors.criticalFill"))
        assertTrue("分享按钮应使用右转箭头", share.contains("AutoMirrored.Outlined.Forward"))
        assertTrue("复制按钮保持双文档图标", share.contains("Icons.Outlined.ContentCopy"))
        assertTrue("保存按钮保持托盘下箭头", share.contains("Icons.Outlined.SaveAlt"))
    }

    @Test
    fun `首页使用新更新星星与密度图标`() {
        val chrome = sourceText("HomeChrome.kt")
        val density = chrome.substringAfter("private fun DensityGlyph")

        assertTrue(chrome.contains("MyTokenUpdateStarIcon"))
        assertFalse(chrome.contains("UpdateStarGlyph"))
        assertTrue("首页设置入口使用实心 gear", chrome.contains("Icons.Filled.Settings"))
        assertFalse(chrome.contains("Icons.Outlined.Settings"))
        assertTrue("简洁密度应为 2x2 网格", density.contains("listOf(3.5f, 13.3f)"))
        assertTrue("完整密度应为带分隔线的 1x2 矩形", density.contains("drawLine("))
    }

    @Test
    fun `密度切换组件不做加载动画`() {
        val chrome = sourceText("HomeChrome.kt")

        assertFalse("分页过程中切换组件不应播放颜色加载动画", chrome.contains("animateColorAsState"))
        assertTrue(chrome.contains("tint = if (selected) colors.canvas else colors.muted"))
    }

    @Test
    fun `主页渐变延伸到状态栏形成沉浸式导航`() {
        val home = sourceText("HomeScreen.kt")
        val root = home
            .substringAfter("Scaffold { padding ->")
            .substringBefore("MyTokenAdaptiveContent(")
        val header = home
            .substringAfter("// 报告和工具条是覆盖层")
            .substringBefore(".testTag(\"home_collapsing_header\")")

        assertFalse("背景层不能被系统栏内边距裁掉", root.contains("padding(padding)"))
        assertTrue("内容区仍需避开系统栏", home.contains("padding(padding)"))
        assertTrue("头部渐变切片需要计入状态栏高度", header.contains("calculateTopPadding"))
    }

    @Test
    fun `正常进度条由绿色改为墨色`() {
        val ring = sourceText("UsageRing.kt")
        assertTrue("弧线正常态应使用墨色", ring.contains("else -> colors.ink"))
        assertFalse(ring.contains("else -> colors.positiveFill"))

        listOf("UsageCardSkeleton.kt", "CredentialUsageCard.kt", "CredentialDetailHero.kt").forEach { name ->
            val text = sourceText(name)
            assertFalse("$name 正常细条不再使用绿色", text.contains("UsageMetricTone.Normal -> colors.positiveFill"))
        }
        assertTrue(
            "紧凑卡细条正常态应使用墨色",
            sourceText("UsageCardSkeleton.kt").contains("UsageMetricTone.Normal -> colors.ink"),
        )
        assertTrue(
            "详情页进度条改用分段仪表",
            sourceText("CredentialDetailHero.kt").contains("UsageSegmentMeter("),
        )
        assertTrue(
            "紧凑卡窗口行改用分段仪表",
            sourceText("CredentialUsageCard.kt").contains("UsageSegmentMeter("),
        )
        assertTrue(
            "完整卡周期行改用分段仪表",
            sourceText("FullSplitUsageContent.kt").contains("UsageSegmentMeter("),
        )
    }

    @Test
    fun `额度构成条统一黑黄双色`() {
        val full = sourceText("FullSplitUsageContent.kt")
        listOf(
            full.substringAfter("private fun FullBalanceHero")
                .substringBefore("@Composable\nprivate fun FullSecondaryBalance"),
            full.substringAfter("private fun FullSecondaryBalance")
                .substringBefore("@Composable\nprivate fun FullBalanceLegend"),
        ).forEach { composition ->
            assertFalse("额度构成不再使用灰色分类色", composition.contains("colors.cat1"))
            assertFalse("额度构成不再使用灰色分类色", composition.contains("colors.cat2"))
            assertTrue("额度构成改用统一分段条", composition.contains("UsageCompositionMeter("))
        }

        val detail = sourceText("CredentialDetailHero.kt")
        val composition = detail.substringAfter("private fun BalanceHero")
            .substringBefore("@Composable\nprivate fun BalanceLegend")
        assertFalse("详情页额度构成不再使用灰色分类色", composition.contains("colors.cat1"))
        assertFalse("详情页额度构成不再使用灰色分类色", composition.contains("colors.cat2"))
        assertTrue("详情页构成条改用统一分段条", composition.contains("UsageCompositionMeter("))

        val meter = sourceText("UsageSegmentMeter.kt")
        assertTrue("充值/现金构成段为墨色", meter.contains("index < primaryCells -> colors.ink"))
        assertTrue("赠送构成段为黄色", meter.contains("else -> colors.actionFill"))
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
