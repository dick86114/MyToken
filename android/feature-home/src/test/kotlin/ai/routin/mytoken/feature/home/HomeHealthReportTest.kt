package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 报告风险行必须指向真正触发告警的窗口，并与卡片保持同一百分比方向。 */
class HomeHealthReportTest {

    private fun progress(
        id: String,
        label: String,
        used: Double,
        health: UsageMetricHealthState,
        windowEnd: Instant? = null,
        semantic: UsageMetricSemantic = UsageMetricSemantic.UsedQuota,
    ) = UsageMetric(
        id = id,
        label = label,
        used = BigDecimal.valueOf(used),
        limit = BigDecimal.valueOf(100),
        remaining = BigDecimal.valueOf(100 - used),
        unit = UsageMetricUnit.Token,
        windowEnd = windowEnd,
        presentation = UsageMetricPresentation.Progress,
        semantic = semantic,
        healthState = health,
    )

    private fun card(vararg metrics: UsageMetric): CredentialCardUi {
        val id = UUID.randomUUID()
        return CredentialCardUi(
            credential = Credential(
                id = id,
                providerId = ProviderId.OpenCode,
                credentialKind = CredentialKind.BearerApiKey,
                name = "洋2",
            ),
            status = RefreshStatus.Ready,
            snapshot = UsageSnapshot(
                credentialId = id,
                fetchedAt = Instant.parse("2026-11-23T04:00:00Z"),
                metrics = metrics.toList(),
            ),
            isStale = false,
            error = null,
            freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
        )
    }

    private fun finding(
        tone: HomeHealthTone,
        failed: Boolean = false,
    ) = HomeReportFinding(
        credentialId = UUID.randomUUID(),
        providerId = ProviderId.OpenCode,
        name = "测试凭证",
        tone = tone,
        detail = "月",
        value = "96%",
        valueLabel = "已用",
        failed = failed,
    )

    @Test
    fun `report tone escalates to the worst finding severity`() {
        assertEquals(HomeHealthTone.Normal, homeReportTone(emptyList()))
        assertEquals(
            HomeHealthTone.Warning,
            homeReportTone(listOf(finding(HomeHealthTone.Warning))),
        )
        assertEquals(
            HomeHealthTone.Critical,
            homeReportTone(
                listOf(finding(HomeHealthTone.Warning), finding(HomeHealthTone.Critical)),
            ),
        )
    }

    @Test
    fun `report tail uses card-consistent copy for each severity`() {
        assertEquals("用量正常", homeReportTail(emptyList()))
        assertEquals("接近上限", homeReportTail(listOf(finding(HomeHealthTone.Warning))))
        assertEquals("已达上限", homeReportTail(listOf(finding(HomeHealthTone.Critical))))
        assertEquals(
            "需注意",
            homeReportTail(listOf(finding(HomeHealthTone.Critical, failed = true))),
        )
    }

    @Test
    fun `severity accent switches to the critical color only for critical findings`() {
        val warning = Color(0xFFFFCC00)
        val critical = Color(0xFFFF0000)

        assertEquals(warning, severityAccent(HomeHealthTone.Normal, warning, critical))
        assertEquals(warning, severityAccent(HomeHealthTone.Warning, warning, critical))
        assertEquals(critical, severityAccent(HomeHealthTone.Critical, warning, critical))
    }

    @Test
    fun `finding uses the metric that triggered the warning`() {
        val finding = homeReportFindings(
            listOf(
                card(
                    progress("fiveHour", "5 小时", 4.0, UsageMetricHealthState.Normal, Instant.parse("2026-11-23T08:00:00Z")),
                    progress("weekly", "周", 6.0, UsageMetricHealthState.Normal),
                    progress("monthly", "月", 83.0, UsageMetricHealthState.Warning, Instant.parse("2026-12-01T00:00:00Z")),
                ),
            ),
        ).single()

        assertEquals("83%", finding.value)
        assertEquals("已用", finding.valueLabel)
        assertTrue("应展示触发告警的月窗口，实际=${finding.detail}", finding.detail.startsWith("月"))
    }

    @Test
    fun `critical metric outranks an earlier warning metric`() {
        val finding = homeReportFindings(
            listOf(
                card(
                    progress("weekly", "周", 85.0, UsageMetricHealthState.Warning),
                    progress("monthly", "月", 96.0, UsageMetricHealthState.Critical),
                ),
            ),
        ).single()

        assertEquals("96%", finding.value)
        assertEquals("已用", finding.valueLabel)
        assertTrue(finding.detail.startsWith("月"))
    }

    @Test
    fun `remaining quota metrics keep their own direction`() {
        val finding = homeReportFindings(
            listOf(
                card(
                    progress(
                        id = "package",
                        label = "套餐",
                        used = 83.0,
                        health = UsageMetricHealthState.Warning,
                        semantic = UsageMetricSemantic.RemainingQuota,
                    ),
                ),
            ),
        ).single()

        assertEquals("17%", finding.value)
        assertEquals("剩余", finding.valueLabel)
    }
}
