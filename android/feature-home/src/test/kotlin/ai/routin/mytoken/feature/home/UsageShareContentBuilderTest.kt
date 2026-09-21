package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageGroupMultiplier
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.usage.RefreshStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageShareContentBuilderTest {
    private val now = Instant.parse("2026-09-20T08:30:00Z")
    private val credentialId = UUID.fromString("12345678-0000-0000-0000-000000000000")

    @Test
    fun `build maps provider plan pass code and metric cost companion`() {
        val credential = Credential(
            id = credentialId,
            providerId = ProviderId.NewAPI,
            credentialKind = CredentialKind.BearerApiKey,
            name = "主力账户",
        )
        val snapshot = UsageSnapshot(
            credentialId = credentialId,
            fetchedAt = now,
            planName = "Team",
            metrics = listOf(
                progress("fiveHour", "5 小时"),
                value("fiveHour-cost", "消耗", "2.5"),
            ),
        )
        val card = testCard(credential, snapshot)
        val content = UsageShareContentBuilder.build(card, now)

        assertEquals("Routin · Team".replace("Routin", "New API"), content?.subtitle)
        assertEquals("PASS #TK-1234", content?.passCode)
        assertEquals("主", content?.avatarLetter)
        assertEquals(1, content?.metrics?.size)
        assertEquals("≈ $2.50", content?.metrics?.first()?.companionText)
    }

    @Test
    fun `render honors visibility switches and hidden metrics`() {
        val credential = Credential(credentialId, ProviderId.Routin, CredentialKind.BearerApiKey, "Main")
        val snapshot = UsageSnapshot(
            credentialId = credentialId,
            fetchedAt = now,
            planName = "Pro",
            groupMultipliers = listOf(UsageGroupMultiplier("default", BigDecimal("2"))),
            metrics = listOf(
                progress("fiveHour", "5 小时"),
                value("requests", "请求数", "120"),
            ),
        )
        val content = UsageShareContentBuilder.build(testCard(credential, snapshot), now)!!
        val draft = UsageShareContentBuilder.makeDraft(content).copy(
            showsGroupMultiplier = false,
            showsAmounts = false,
            showsResetTimes = false,
            hiddenMetricIds = setOf("requests"),
        )
        val rendered = UsageShareContentBuilder.render(content, draft)

        assertEquals(listOf("fiveHour"), rendered.metrics.map { it.id })
        assertNull(rendered.metrics.first().usedText)
        assertNull(rendered.metrics.first().resetBadgeText)
        assertNull(rendered.groupMultiplierText)
        assertTrue(rendered.showsWatermark)
    }

    @Test
    fun `templates and file names mirror macOS labels`() {
        assertEquals("深色票根", UsageShareTemplate.Ticket.title)
        assertEquals("浅色票根", UsageShareTemplate.TicketLight.title)
        assertEquals("暗色极客", UsageShareTemplate.Dark.title)
        assertEquals("雅致浅色", UsageShareTemplate.Light.title)
        val card = UsageShareRenderedCard(
            displayName = "Test/A:ccount ",
            providerName = "Routin",
            subtitle = "",
            note = null,
            subscriptionStartText = null,
            subscriptionEndText = null,
            cycleRemainingText = null,
            groupMultiplierText = null,
            metrics = emptyList(),
            capturedAtText = "2026.09.20 16:30",
            showsWatermark = true,
            showsStatus = true,
            template = UsageShareTemplate.Ticket,
            passCode = "PASS #TK-1234",
            avatarLetter = "T",
            isAvailable = true,
        )
        assertEquals(
            "MyToken-Test-A-ccount-20260920-1630.png",
            UsageShareContentBuilder.fileName(card, now, ZoneId.of("Asia/Shanghai")),
        )
    }

    @Test
    fun `field toggles follow ticket reading order`() {
        val content = UsageShareContent(
            displayName = "Main",
            providerName = "Routin",
            planName = "Pro",
            subtitle = "Routin · Pro",
            subscriptionStartText = "2026-09-01",
            subscriptionEndText = "2026-10-01",
            cycleRemainingText = "10天",
            groupMultiplierText = "default ×1",
            metrics = listOf(
                UsageShareMetricItem(
                    id = "fiveHour",
                    title = "5 小时",
                    headline = "25%",
                    percent = 25.0,
                    amountDetails = emptyList(),
                    timeDetails = listOf("剩余 1小时"),
                    usedText = null,
                    limitText = null,
                    remainingAmountText = null,
                    resetBadgeText = null,
                    companionText = null,
                    healthStateIsAvailable = true,
                    spansFullWidth = false,
                ),
                UsageShareMetricItem(
                    id = "requests",
                    title = "请求数",
                    headline = "120",
                    percent = null,
                    amountDetails = emptyList(),
                    timeDetails = emptyList(),
                    usedText = null,
                    limitText = null,
                    remainingAmountText = null,
                    resetBadgeText = null,
                    companionText = null,
                    healthStateIsAvailable = true,
                    spansFullWidth = false,
                ),
            ),
            capturedAt = now,
            capturedAtText = "2026.09.20 16:30",
            providerId = ProviderId.Routin,
            passCode = "PASS #TK-1234",
            avatarLetter = "M",
            isAvailable = true,
        )

        assertEquals(
            listOf(
                "可用状态徽章",
                "套餐规格",
                "订阅周期/到期",
                "周期剩余",
                "附加备注框",
                "重置时间与倒计时",
                "5 小时",
                "请求数",
                "分组倍率",
                "快照水印与防伪",
            ),
            visibleToggles(content).map { it.title },
        )
    }

    private fun testCard(credential: Credential, snapshot: UsageSnapshot) = CredentialCardUi(
        credential = credential,
        status = RefreshStatus.Ready,
        snapshot = snapshot,
        isStale = false,
        error = null,
        freshness = Freshness(FreshnessLevel.JUST_NOW, "刚刚更新"),
    )

    private fun progress(
        id: String,
        label: String,
    ): UsageMetric {
        val used = BigDecimal("2.5")
        val limit = BigDecimal("10")
        return UsageMetric(
            id = id,
            label = label,
            used = used,
            limit = limit,
            remaining = limit.subtract(used),
            unit = UsageMetricUnit.Currency,
            windowEnd = now.plusSeconds(3600),
            presentation = UsageMetricPresentation.Progress,
            semantic = UsageMetricSemantic.UsedQuota,
            currencyCode = "USD",
        )
    }

    private fun value(id: String, label: String, amount: String) = UsageMetric(
        id = id,
        label = label,
        value = BigDecimal(amount),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.Value,
        currencyCode = "USD",
    )
}
