package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.model.UsageMetricUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 与 macOS UsageShareTemplate 一一对应的四套分享模板。 */
enum class UsageShareTemplate(val title: String, val previewTag: String) {
    Ticket("深色票根", "深色票根模式"),
    TicketLight("浅色票根", "浅色票根模式"),
    Dark("暗色极客", "极客暗色模式"),
    Light("雅致浅色", "雅致浅色模式");
}

enum class UsageShareAction { Copy, Save, Share }

data class UsageShareMetricItem(
    val id: String,
    val title: String,
    val headline: String,
    val percent: Double?,
    val amountDetails: List<String>,
    val timeDetails: List<String>,
    val usedText: String?,
    val limitText: String?,
    val remainingAmountText: String?,
    val resetBadgeText: String?,
    val companionText: String?,
    val healthStateIsAvailable: Boolean,
    val spansFullWidth: Boolean,
)

data class UsageShareContent(
    val displayName: String,
    val providerName: String,
    val planName: String,
    val subtitle: String,
    val subscriptionStartText: String?,
    val subscriptionEndText: String?,
    val cycleRemainingText: String?,
    val groupMultiplierText: String?,
    val metrics: List<UsageShareMetricItem>,
    val capturedAt: Instant,
    val capturedAtText: String,
    val providerId: ProviderId,
    val passCode: String,
    val avatarLetter: String,
    val isAvailable: Boolean,
)

data class UsageShareDraft(
    val displayName: String,
    val subtitle: String,
    val note: String = "",
    val showsSubtitle: Boolean = true,
    val showsSubscriptionDates: Boolean = true,
    val showsCycleRemaining: Boolean = true,
    val showsGroupMultiplier: Boolean = true,
    val showsAmounts: Boolean = true,
    val showsResetTimes: Boolean = true,
    val showsWatermark: Boolean = true,
    val showsStatus: Boolean = true,
    val showsNote: Boolean = true,
    val hiddenMetricIds: Set<String> = emptySet(),
    val template: UsageShareTemplate = UsageShareTemplate.Ticket,
) {
    fun isMetricVisible(id: String): Boolean = id !in hiddenMetricIds

    fun setMetricVisible(id: String, visible: Boolean): UsageShareDraft =
        copy(
            hiddenMetricIds = if (visible) {
                hiddenMetricIds - id
            } else {
                hiddenMetricIds + id
            },
        )

    fun showAll(): UsageShareDraft = copy(
        showsSubtitle = true,
        showsSubscriptionDates = true,
        showsCycleRemaining = true,
        showsGroupMultiplier = true,
        showsAmounts = true,
        showsResetTimes = true,
        showsWatermark = true,
        showsStatus = true,
        showsNote = true,
        hiddenMetricIds = emptySet(),
    )
}

data class UsageShareRenderedCard(
    val displayName: String,
    val providerName: String,
    val subtitle: String,
    val note: String?,
    val subscriptionStartText: String?,
    val subscriptionEndText: String?,
    val cycleRemainingText: String?,
    val groupMultiplierText: String?,
    val metrics: List<UsageShareMetricItem>,
    val capturedAtText: String,
    val showsWatermark: Boolean,
    val showsStatus: Boolean,
    val template: UsageShareTemplate,
    val passCode: String,
    val avatarLetter: String,
    val isAvailable: Boolean,
)

/** Android 侧复刻 macOS UsageShareContentBuilder 的内容与展示开关规则。 */
object UsageShareContentBuilder {
    fun build(
        card: CredentialCardUi,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): UsageShareContent? {
        val snapshot = card.snapshot ?: return null
        val providerId = card.credential.providerId
        val providerName = ProviderCatalog.displayName(providerId)
        val hasSubscriptionDates = snapshot.subscriptionStartAt != null || snapshot.subscriptionEndAt != null
        val subscriptionExpired = snapshot.subscriptionEndAt?.let { !it.isAfter(now) } == true

        return UsageShareContent(
            displayName = card.credential.name,
            providerName = providerName,
            planName = snapshot.planName,
            subtitle = snapshot.planName.ifEmpty { providerName }.let { plan ->
                if (plan == providerName) providerName else "$providerName · $plan"
            },
            subscriptionStartText = if (hasSubscriptionDates) {
                snapshot.subscriptionStartAt?.let { subscriptionDateText(it, zone) }
            } else {
                null
            },
            subscriptionEndText = if (hasSubscriptionDates) {
                snapshot.subscriptionEndAt?.let { subscriptionDateText(it, zone) }
            } else {
                null
            },
            cycleRemainingText = snapshot.subscriptionEndAt?.let { cycleRemainingText(it, now, zone) },
            groupMultiplierText = snapshot.groupMultipliers
                .takeIf { it.isNotEmpty() }
                ?.joinToString("、") { "${it.name} ×${formatGrouped(it.multiplier)}" },
            metrics = snapshot.metrics
                .filterNot { it.id.endsWith("-cost") }
                .map { metric -> metricItem(metric, snapshot.metrics, providerId != ProviderId.Glm, now, zone) },
            capturedAt = now,
            capturedAtText = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(zone)
                .format(now),
            providerId = providerId,
            passCode = "PASS #TK-${card.credential.id.toString().replace("-", "").take(4).uppercase(Locale.US)}",
            avatarLetter = card.credential.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M",
            isAvailable = card.error == null && !subscriptionExpired,
        )
    }

    fun makeDraft(content: UsageShareContent): UsageShareDraft = UsageShareDraft(
        displayName = content.displayName,
        subtitle = content.planName.ifEmpty { content.providerName },
        showsSubscriptionDates = content.subscriptionStartText != null ||
            content.subscriptionEndText != null ||
            content.cycleRemainingText != null,
        showsCycleRemaining = content.cycleRemainingText != null,
        showsGroupMultiplier = content.groupMultiplierText != null,
    )

    fun render(content: UsageShareContent, draft: UsageShareDraft): UsageShareRenderedCard {
        val displayName = draft.displayName.trim().ifEmpty { content.displayName }
        val note = draft.note.trim().takeIf { it.isNotEmpty() && draft.showsNote }
        return UsageShareRenderedCard(
            displayName = displayName,
            providerName = content.providerName,
            subtitle = if (draft.showsSubtitle) {
                draft.subtitle.trim().ifEmpty { content.planName.ifEmpty { content.providerName } }
            } else {
                ""
            },
            note = note,
            subscriptionStartText = if (draft.showsSubscriptionDates) content.subscriptionStartText else null,
            subscriptionEndText = if (draft.showsSubscriptionDates) content.subscriptionEndText else null,
            cycleRemainingText = if (draft.showsCycleRemaining) content.cycleRemainingText else null,
            groupMultiplierText = if (draft.showsGroupMultiplier) content.groupMultiplierText else null,
            metrics = content.metrics
                .filter { draft.isMetricVisible(it.id) }
                .map { item ->
                    item.copy(
                        amountDetails = if (draft.showsAmounts) item.amountDetails else emptyList(),
                        timeDetails = if (draft.showsResetTimes) item.timeDetails else emptyList(),
                        usedText = if (draft.showsAmounts) item.usedText else null,
                        limitText = if (draft.showsAmounts) item.limitText else null,
                        remainingAmountText = if (draft.showsAmounts) item.remainingAmountText else null,
                        resetBadgeText = if (draft.showsResetTimes) item.resetBadgeText else null,
                    )
                },
            capturedAtText = content.capturedAtText,
            showsWatermark = draft.showsWatermark,
            showsStatus = draft.showsStatus,
            template = draft.template,
            passCode = content.passCode,
            avatarLetter = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M",
            isAvailable = content.isAvailable,
        )
    }

    fun fileName(card: UsageShareRenderedCard, capturedAt: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val safe = card.displayName.replace(Regex("[/\\\\:?*\"<>|]"), "-")
            .trim()
            .ifEmpty { "账户" }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(zone).format(capturedAt)
        return "MyToken-$safe-$stamp.png"
    }

    private fun metricItem(
        metric: UsageMetric,
        allMetrics: List<UsageMetric>,
        showsAmountDetails: Boolean,
        now: Instant,
        zone: ZoneId,
    ): UsageShareMetricItem = when (metric.presentation) {
        UsageMetricPresentation.Progress -> {
            val headline = displayPercent(displayedPercent(metric))
            val used = if (showsAmountDetails) amountText(metric.used, metric) else null
            val limit = if (showsAmountDetails) amountText(metric.limit, metric) else null
            val remaining = if (showsAmountDetails) amountText(metric.remaining, metric) else null
            val companion = allMetrics.firstOrNull { it.id == "${metric.id}-cost" }
                ?.let { "≈ ${formatCurrency(it.value, it.currencyCode)}" }
            UsageShareMetricItem(
                id = metric.id,
                title = metric.label,
                headline = headline,
                percent = displayedPercent(metric),
                amountDetails = buildList {
                    if (showsAmountDetails && metric.used != null && metric.limit != null) {
                        add("已用 ${amountText(metric.used, metric)} / ${amountText(metric.limit, metric)}")
                        metric.remaining?.let { add("剩余 ${amountText(it, metric)}") }
                    }
                    companion?.let(::add)
                },
                timeDetails = metric.windowEnd?.let { end ->
                    listOf("重置 ${formatResetTime(end)}", "剩余 ${formatRemainingDuration(end, now)}")
                } ?: emptyList(),
                usedText = used,
                limitText = limit,
                remainingAmountText = remaining,
                resetBadgeText = metric.windowEnd?.let { resetBadgeText(it, now, zone) },
                companionText = companion,
                healthStateIsAvailable = metric.healthState != ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable,
                spansFullWidth = metric.id == "monthly" || metric.id == "quota-progress" ||
                    (headline.length > 9 || (metric.used?.let { amountText(it, metric) }?.length ?: 0) > 18),
            )
        }
        UsageMetricPresentation.Balance -> UsageShareMetricItem(
            id = metric.id,
            title = metric.label.ifEmpty { "账户余额" },
            headline = formatCurrency(metric.value, metric.currencyCode),
            percent = null,
            amountDetails = emptyList(),
            timeDetails = emptyList(),
            usedText = null,
            limitText = null,
            remainingAmountText = null,
            resetBadgeText = null,
            companionText = null,
            healthStateIsAvailable = metric.healthState != ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable,
            spansFullWidth = headlineLength(metric, metric.value).let { it.first } > 9,
        )
        UsageMetricPresentation.Status -> UsageShareMetricItem(
            id = metric.id,
            title = metric.label.ifEmpty { "账户状态" },
            headline = if (metric.healthState == ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable) "不可用" else "可用",
            percent = null,
            amountDetails = emptyList(),
            timeDetails = emptyList(),
            usedText = null,
            limitText = null,
            remainingAmountText = null,
            resetBadgeText = null,
            companionText = null,
            healthStateIsAvailable = metric.healthState != ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable,
            spansFullWidth = false,
        )
        UsageMetricPresentation.Value -> {
            val headline = valueText(metric)
            val companion = allMetrics.firstOrNull { it.id == "${metric.id}-cost" }
                ?.let { "≈ ${formatCurrency(it.value, it.currencyCode)}" }
            UsageShareMetricItem(
                id = metric.id,
                title = metric.label,
                headline = headline,
                percent = null,
                amountDetails = listOfNotNull(companion),
                timeDetails = emptyList(),
                usedText = null,
                limitText = null,
                remainingAmountText = null,
                resetBadgeText = null,
                companionText = companion,
                healthStateIsAvailable = metric.healthState != ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable,
                spansFullWidth = headline.length > 9 || (companion?.length ?: 0) > 18,
            )
        }
    }

    private fun headlineLength(metric: UsageMetric, value: BigDecimal?): Pair<Int, String> {
        val text = when (metric.unit) {
            UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
            else -> formatGrouped(value)
        }
        return text.length to text
    }

    private fun displayedPercent(metric: UsageMetric): Double? {
        val limit = metric.limit ?: return null
        if (limit.signum() <= 0) return null
        val number = when (metric.semantic) {
            UsageMetricSemantic.UsedQuota -> metric.used
            UsageMetricSemantic.RemainingQuota -> metric.remaining
            else -> null
        } ?: return null
        return number.toDouble() / limit.toDouble() * 100.0
    }

    private fun displayPercent(value: Double?): String {
        value ?: return "—"
        val rounded = BigDecimal(value).setScale(0, RoundingMode.HALF_UP)
        return "${rounded.toPlainString()}%"
    }

    private fun amountText(value: BigDecimal?, metric: UsageMetric): String = when (metric.unit) {
        UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
        else -> formatGrouped(value)
    }

    private fun valueText(metric: UsageMetric): String = when (metric.unit) {
        UsageMetricUnit.Currency -> formatCurrency(metric.value, metric.currencyCode)
        UsageMetricUnit.Token -> formatGrouped(metric.value)
        UsageMetricUnit.Request -> "${formatGrouped(metric.value)} 次"
        UsageMetricUnit.Boolean_ -> when {
            metric.healthState == ai.routin.mytoken.domain.model.UsageMetricHealthState.Unavailable -> "不可用"
            metric.value?.compareTo(BigDecimal.ONE) == 0 -> "可用"
            else -> formatGrouped(metric.value)
        }
        UsageMetricUnit.Text -> formatGrouped(metric.value)
    }

    private fun resetBadgeText(end: Instant, now: Instant, zone: ZoneId): String {
        val duration = formatRemainingDuration(end, now)
        if (duration == "已结束") return "已结束"
        val clock = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(end)
        return "$duration 后重置 ($clock)"
    }

    private fun cycleRemainingText(end: Instant, now: Instant, zone: ZoneId): String {
        if (!end.isAfter(now)) return "已过期"
        val days = maxOf(1, Duration.between(now, end).toDays().toInt())
        val date = DateTimeFormatter.ofPattern("MM.dd").withZone(zone).format(end)
        return "余 $days 天 ($date)"
    }

    private fun subscriptionDateText(instant: Instant, zone: ZoneId): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone).format(instant)
}
