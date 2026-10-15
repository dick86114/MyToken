package ai.routin.mytoken.widget

import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import ai.routin.mytoken.domain.model.UsageSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPresentationBuilderTest {

    @Test
    fun `credential widgets always render focus rings in window order`() {
        val credential = credential(ProviderId.CommandCode)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                progress("weekly", "周额度", 43),
                progress("five-hour", "五小时窗口", 86),
                progress("credit-progress", "购买额度", 48),
                value("request-count", "请求数", "210"),
                value("purchased-remaining", "剩余购买额度", "52"),
                value("free-remaining", "免费额度", "18"),
            ),
        )

        val compact = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Compact,
        )
        val standard = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Standard,
        )
        val expanded = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Expanded,
        )

        // 单凭证固定聚焦环，与尺寸无关。
        assertEquals(WidgetDisplayStyle.FocusRing, compact.style)
        assertEquals(WidgetDisplayStyle.FocusRing, standard.style)
        assertEquals(WidgetDisplayStyle.FocusRing, expanded.style)
        // 只保留带进度的用量指标，按 5 小时 / 周 / 月 顺序，最多三个。
        val expectedIds = listOf("five-hour", "weekly", "credit-progress")
        assertEquals(expectedIds, compact.credential?.orderedMetrics?.map { it.id })
        assertEquals(expectedIds, standard.credential?.orderedMetrics?.map { it.id })
        assertEquals(expectedIds, expanded.credential?.orderedMetrics?.map { it.id })
    }

    @Test
    fun `value only credentials fall back to their ranked metrics`() {
        val credential = credential(ProviderId.Xiaomi)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                value("history", "历史消耗", "3323413"),
                value("cache-miss", "未命中缓存", "1200000"),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Standard,
        )

        assertEquals(
            listOf("history", "cache-miss"),
            presentation.credential?.orderedMetrics?.map { it.id },
        )
        assertEquals(listOf(null, null), presentation.credential?.orderedMetrics?.map { it.progressPercent })
    }

    @Test
    fun `single credential keeps every window metric but trims extra values`() {
        val credential = credential(ProviderId.CommandCode)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                progress("monthly", "每月用量", 31),
                progress("weekly", "每周用量", 42),
                progress("five-hour", "5 小时用量", 20),
                value("request-count", "请求数", "210"),
                value("free-remaining", "免费额度", "18"),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Compact,
        )

        assertEquals(
            listOf("five-hour", "weekly", "monthly"),
            presentation.credential?.orderedMetrics?.map { it.id },
        )
    }

    @Test
    fun `focus ring metrics expose reset clock for the caption line`() {
        val credential = credential(ProviderId.Glm)
        val resetAt = Instant.parse("2026-01-01T06:00:00Z")
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                progress("five-hour", "5 小时用量", 20).copy(windowEnd = resetAt),
                progress("weekly", "每周用量", 42).copy(windowEnd = resetAt),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Standard,
        )
        val expectedClock = DateTimeFormatter.ofPattern("HH:mm")
            .format(resetAt.atZone(ZoneId.systemDefault()))

        assertEquals(2, presentation.credential?.orderedMetrics?.size)
        presentation.credential?.orderedMetrics?.forEach { metric ->
            assertEquals("重置 $expectedClock", metric.resetText)
            assertEquals(expectedClock, metric.resetClockText)
        }
    }

    @Test
    fun `overview widgets always render signal tiles`() {
        val credential = credential(ProviderId.CommandCode)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                progress("monthly", "每月用量", 31),
                progress("weekly", "每周用量", 42),
                progress("five-hour", "5 小时用量", 20),
                value("request-count", "请求数", "210"),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeOverviewPresentation(
            entries = listOf(credential to snapshot),
            size = WidgetSizeKind.Standard,
        )

        assertEquals(WidgetDisplayStyle.SignalTiles, presentation.style)
        assertEquals(
            listOf("five-hour", "weekly", "monthly"),
            presentation.credential?.orderedMetrics?.map { it.id },
        )
    }

    @Test
    fun `xiaomi value text preserves full grouped numbers`() {
        val credential = credential(ProviderId.Xiaomi)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                value("history", "历史消耗", "3323413"),
                value("cache-miss", "未命中缓存", "1200000"),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetSizeKind.Expanded,
        )

        assertEquals("3,323,413", presentation.credential?.primary?.valueText)
        assertEquals(
            "1,200,000",
            presentation.credential?.secondary?.firstOrNull { it.id == "cache-miss" }?.valueText,
        )
    }

    @Test
    fun `glm follows main card progress order and omits amounts`() {
        val credential = credential(ProviderId.Glm)
        val resetAt = Instant.parse("2026-01-01T06:00:00Z")
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                requestMetric("model-calls", "模型调用量", "12"),
                requestMetric("zcode-mcp", "MCP 调用量", "3"),
                progress("monthly", "每月用量", 31).copy(windowEnd = resetAt),
                progress("weekly", "每周用量", 42).copy(windowEnd = resetAt),
                progress("five-hour", "5 小时用量", 20).copy(windowEnd = resetAt),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential,
            snapshot,
            WidgetSizeKind.Standard,
        )

        assertEquals(
            listOf("five-hour", "weekly", "monthly"),
            presentation.credential?.orderedMetrics?.map { it.id },
        )
        assertEquals("5h", presentation.credential?.primary?.label)
        assertEquals("", presentation.credential?.primary?.detailText)
        assertEquals(
            "重置 " + DateTimeFormatter.ofPattern("HH:mm")
                .format(resetAt.atZone(ZoneId.systemDefault())),
            presentation.credential?.primary?.resetText,
        )
    }

    @Test
    fun `balance providers only keep the balance metric`() {
        val credential = credential(ProviderId.DeepSeek)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                value("grantedBalance", "赠金余额", "4"),
                value("toppedUpBalance", "充值余额", "6"),
                balance("balance", "账户余额", "10"),
            ),
        )

        val presentation = WidgetPresentationBuilder.makeCredentialPresentation(
            credential,
            snapshot,
            WidgetSizeKind.Standard,
        )

        assertEquals(listOf("balance"), presentation.credential?.orderedMetrics?.map { it.id })
        assertEquals(true, presentation.credential?.isBalanceOnly)
        assertEquals("¥10.00", presentation.credential?.primary?.valueText)
    }

    private fun credential(providerId: ProviderId) = Credential(
        id = UUID.randomUUID(),
        providerId = providerId,
        credentialKind = CredentialKind.ApiKey,
        name = "测试凭证",
    )

    private fun progress(id: String, label: String, percent: Int) = UsageMetric(
        id = id,
        label = label,
        used = percent.toBigDecimal(),
        limit = 100.toBigDecimal(),
        remaining = (100 - percent).toBigDecimal(),
        unit = UsageMetricUnit.Token,
        presentation = UsageMetricPresentation.Progress,
        semantic = UsageMetricSemantic.UsedQuota,
        healthState = UsageMetricHealthState.Normal,
    )

    private fun value(id: String, label: String, raw: String) = UsageMetric(
        id = id,
        label = label,
        value = raw.toBigDecimal(),
        unit = UsageMetricUnit.Token,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.Value,
        healthState = UsageMetricHealthState.Normal,
    )

    private fun requestMetric(id: String, label: String, raw: String) = UsageMetric(
        id = id,
        label = label,
        value = raw.toBigDecimal(),
        unit = UsageMetricUnit.Request,
        presentation = UsageMetricPresentation.Value,
        semantic = UsageMetricSemantic.Value,
        healthState = UsageMetricHealthState.Normal,
    )

    private fun balance(id: String, label: String, raw: String) = UsageMetric(
        id = id,
        label = label,
        value = raw.toBigDecimal(),
        unit = UsageMetricUnit.Currency,
        presentation = UsageMetricPresentation.Balance,
        semantic = UsageMetricSemantic.Balance,
        currencyCode = "CNY",
        healthState = UsageMetricHealthState.Normal,
    )
}
