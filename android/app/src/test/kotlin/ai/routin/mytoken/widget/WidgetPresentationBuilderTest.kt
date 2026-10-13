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
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPresentationBuilderTest {

    @Test
    fun `builder limits metrics and resolves auto style`() {
        val credential = credential(ProviderId.CommandCode)
        val snapshot = UsageSnapshot(
            credentialId = credential.id,
            fetchedAt = Instant.parse("2026-01-01T00:00:00Z"),
            metrics = listOf(
                progress("weekly", "周额度", 43),
                progress("five-hour", "五小时窗口", 86),
                value("credit-progress", "购买额度", "48"),
                value("request-count", "请求数", "210"),
                value("purchased-remaining", "剩余购买额度", "52"),
                value("free-remaining", "免费额度", "18"),
            ),
        )

        val compact = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetDisplayStyle.Auto, WidgetSizeKind.Compact,
        )
        val standard = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetDisplayStyle.Auto, WidgetSizeKind.Standard,
        )
        val expanded = WidgetPresentationBuilder.makeCredentialPresentation(
            credential, snapshot, WidgetDisplayStyle.Auto, WidgetSizeKind.Expanded,
        )

        assertEquals(WidgetDisplayStyle.FocusRing, compact.style)
        assertEquals(WidgetDisplayStyle.FocusRing, standard.style)
        assertEquals(WidgetDisplayStyle.SignalTiles, expanded.style)
        assertEquals(2, compact.credential?.orderedMetrics?.size)
        assertEquals(4, standard.credential?.orderedMetrics?.size)
        assertEquals(6, expanded.credential?.orderedMetrics?.size)
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
            credential, snapshot, WidgetDisplayStyle.Auto, WidgetSizeKind.Expanded,
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
            WidgetDisplayStyle.Auto,
            WidgetSizeKind.Standard,
        )

        assertEquals(
            listOf("five-hour", "weekly", "monthly"),
            presentation.credential?.orderedMetrics?.map { it.id },
        )
        assertEquals("5h", presentation.credential?.primary?.label)
        assertEquals("", presentation.credential?.primary?.detailText)
        assertEquals("重置 14:00", presentation.credential?.primary?.resetText)
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
            WidgetDisplayStyle.Auto,
            WidgetSizeKind.Standard,
        )

        assertEquals(listOf("balance"), presentation.credential?.orderedMetrics?.map { it.id })
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
