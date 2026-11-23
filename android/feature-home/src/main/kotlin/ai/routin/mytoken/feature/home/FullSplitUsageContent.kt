package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import java.math.BigDecimal
import java.time.Instant

private enum class SplitTone { Normal, Warning, Critical }

private data class SplitFact(
    val label: String,
    val value: String,
    val positive: Boolean = false,
)

private data class SplitCurrencyGroup(
    val balance: UsageMetric,
    val toppedUp: UsageMetric?,
    val granted: UsageMetric?,
)

@Composable
internal fun FullSplitUsageContent(
    card: CredentialCardUi,
    metrics: List<UsageMetric>,
) {
    val byId = remember(metrics) { metrics.associateBy(UsageMetric::id) }
    val usageKind = card.credential.metadata[CredentialMetadataKey.UsageKind]
        ?: card.snapshot?.usageKind

    when {
        card.credential.providerId == ProviderId.DeepSeek -> {
            FullDeepSeekBalance(card = card, metrics = metrics)
        }

        isXiaomiApiBalanceCard(card) -> {
            FullXiaomiApiBalance(card = card, metrics = metrics)
        }

        card.credential.providerId == ProviderId.NewAPI -> {
            FullNewApiSplit(metrics = metrics)
        }

        card.credential.providerId == ProviderId.CommandCode -> {
            val primary = byId["five-hour"]
                ?: byId["weekly"]
                ?: byId["credit-progress"]
            FullQuotaSplit(
                primary = primary,
                secondary = listOfNotNull(
                    byId["weekly"]?.takeIf { it.id != primary?.id },
                    byId["credit-progress"]?.takeIf { it.id != primary?.id },
                ),
                leftLabel = primary?.let(::commandCodeWindowLabel) ?: "当前额度",
                labelFormatter = ::commandCodeWindowLabel,
            )
        }

        card.credential.providerId == ProviderId.Routin && usageKind == "tokenPack" -> {
            FullPackSplit(
                primary = byId["token"],
                leftLabel = "Token 套餐",
                endLabel = "有效期至",
                endOverride = card.snapshot?.subscriptionEndAt,
                facts = tokenPackFacts(byId["token"], card),
            )
        }

        card.credential.providerId == ProviderId.Xiaomi && usageKind == "plan" -> {
            FullPackSplit(
                primary = byId["plan-total"],
                leftLabel = "套餐",
                endLabel = "到期",
                facts = mimoPlanFacts(byId["plan-total"], metrics, card),
            )
        }

        card.credential.providerId == ProviderId.Glm -> {
            val primary = byId["five-hour"] ?: byId["weekly"]
            FullQuotaSplit(
                primary = primary,
                secondary = listOfNotNull(
                    byId["weekly"]?.takeIf { it.id != primary?.id },
                ),
                leftLabel = primary?.let(::windowLabel) ?: "当前用量",
                facts = glmActivityFacts(byId),
            )
        }

        card.credential.providerId == ProviderId.Volcengine -> {
            val ordered = listOfNotNull(
                byId["fiveHour"],
                byId["weekly"],
                byId["monthly"],
            )
            FullQuotaSplit(
                primary = ordered.firstOrNull(),
                secondary = ordered.drop(1),
                leftLabel = ordered.firstOrNull()?.let(::windowLabel) ?: "当前用量",
            )
        }

        card.credential.providerId == ProviderId.OpenCode -> {
            val ordered = listOfNotNull(
                byId["fiveHour"],
                byId["weekly"],
                byId["monthly"],
            )
            FullQuotaSplit(
                primary = ordered.firstOrNull(),
                secondary = ordered.drop(1),
                leftLabel = ordered.firstOrNull()?.let(::quotaWindowLabel) ?: "当前额度",
            )
        }

        card.credential.providerId == ProviderId.Routin -> {
            val ordered = listOfNotNull(
                byId["fiveHour"],
                byId["weekly"],
            )
            FullQuotaSplit(
                primary = ordered.firstOrNull(),
                secondary = ordered.drop(1),
                leftLabel = ordered.firstOrNull()?.let(::quotaWindowLabel) ?: "当前额度",
            )
        }

        else -> {
            val progress = metrics.filter {
                it.presentation == UsageMetricPresentation.Progress
            }
            FullQuotaSplit(
                primary = progress.firstOrNull(),
                secondary = progress.drop(1),
                leftLabel = windowLabel(progress.firstOrNull() ?: return),
            )
        }
    }
}

@Composable
private fun EmptyUsageText() {
    Text(
        text = "暂无用量数据",
        style = MyTokenType.caption,
        color = MyTokenThemeTokens.colors.muted,
    )
}

@Composable
private fun FullSplitLayout(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 窄卡或大字体会让 40/60 分栏挤成一列字，改为上下堆叠且不加分割线。
        if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                left()
                right()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(modifier = Modifier.weight(0.4f)) { left() }
                Box(modifier = Modifier.weight(0.6f)) { right() }
            }
        }
    }
}

@Composable
private fun FullQuotaSplit(
    primary: UsageMetric?,
    secondary: List<UsageMetric>,
    leftLabel: String,
    endLabel: String = "重置",
    labelFormatter: (UsageMetric) -> String = ::windowLabel,
    facts: List<SplitFact> = emptyList(),
) {
    if (primary == null) {
        EmptyUsageText()
        return
    }

    if (secondary.isEmpty()) {
        // 只有单一窗口时不再留空右栏，仪表居中铺满卡片。
        FullHeroFocus(
            metric = primary,
            label = leftLabel,
            endLabel = endLabel,
        )
        return
    }

    FullSplitLayout(
        left = {
            FullHeroFocus(
                metric = primary,
                label = leftLabel,
                endLabel = endLabel,
            )
        },
        right = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                secondary.forEach { metric ->
                    FullPeriodRow(
                        metric = metric,
                        endLabel = endLabel,
                        labelFormatter = labelFormatter,
                    )
                }
                if (facts.isNotEmpty()) {
                    FullFactsGrid(facts = facts)
                }
            }
        },
    )
}

@Composable
private fun FullPackSplit(
    primary: UsageMetric?,
    leftLabel: String,
    endLabel: String,
    facts: List<SplitFact>,
    endOverride: Instant? = null,
) {
    if (primary == null) {
        EmptyUsageText()
        return
    }

    FullSplitLayout(
        left = {
            FullHeroFocus(
                metric = primary,
                label = leftLabel,
                endLabel = endLabel,
                endOverride = endOverride,
            )
        },
        right = {
            FullFactsGrid(facts = facts)
        },
    )
}

/** 完整卡主指标：标签 + 重置卡 chip 一行，仪表居中，下方只保留重置/到期文案。 */
@Composable
private fun FullHeroFocus(
    metric: UsageMetric,
    label: String,
    endLabel: String,
    endOverride: Instant? = null,
    note: String? = null,
) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    val tone = splitTone(metric)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (metric.resetCards.isNotEmpty()) {
                ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Detail)
            }
        }
        UsageArcGauge(
            percent = percent,
            tone = splitToneUsageTone(tone),
            diameter = 64.dp,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("full_ring_primary"),
        )
        Text(
            text = note ?: quotaEndText(metric.windowEnd ?: endOverride, endLabel) ?: "—",
            modifier = Modifier.padding(top = 4.dp),
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** 完整卡周期行：标签 + 百分比，分段仪表，重置文案。 */
@Composable
private fun FullPeriodRow(
    metric: UsageMetric,
    endLabel: String,
    labelFormatter: (UsageMetric) -> String,
) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    val tone = splitTone(metric)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = labelFormatter(metric),
                modifier = Modifier.weight(1f),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (metric.resetCards.isNotEmpty()) {
                ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Detail)
            }
            Text(
                text = formatPercent(percent),
                style = MyTokenType.label.copy(fontWeight = FontWeight(650)),
                color = when (tone) {
                    SplitTone.Normal -> colors.ink
                    SplitTone.Warning -> colors.warningText
                    SplitTone.Critical -> colors.criticalText
                },
                maxLines = 1,
            )
        }
        UsageSegmentMeter(
            percent = percent,
            tone = splitToneUsageTone(tone),
            modifier = Modifier.padding(top = 6.dp),
            height = 11.dp,
        )
        quotaEndText(metric.windowEnd, endLabel)?.let { resetText ->
            Text(
                text = resetText,
                modifier = Modifier.padding(top = 3.dp),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 事实网格：前 4 项两列，其余收进一行，对应设计稿的 facts-grid / facts-line。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullFactsGrid(facts: List<SplitFact>) {
    val colors = MyTokenThemeTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        facts.chunked(2).take(3).forEach { rowFacts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowFacts.forEach { fact ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fact.label,
                            style = MyTokenType.caption,
                            color = colors.muted,
                            maxLines = 1,
                        )
                        Text(
                            text = fact.value,
                            modifier = Modifier.padding(top = 2.dp),
                            style = MyTokenType.label,
                            color = if (fact.positive) colors.positiveText else colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(2 - rowFacts.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        val rest = facts.drop(6)
        if (rest.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rest.forEach { fact ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = fact.label,
                            style = MyTokenType.caption,
                            color = colors.muted,
                        )
                        Text(
                            text = fact.value,
                            style = MyTokenType.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = if (fact.positive) colors.positiveText else colors.ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullDeepSeekBalance(
    card: CredentialCardUi,
    metrics: List<UsageMetric>,
) {
    val groups = remember(metrics) { deepSeekCurrencyGroups(metrics) }
    val primary = groups.firstOrNull()
    if (primary == null) {
        EmptyUsageText()
        return
    }

    Column {
        FullBalanceHero(
            group = primary,
            primaryLabel = "充值",
            secondaryLabel = "赠金",
        )
        groups.drop(1).forEach { group ->
            FullSecondaryBalance(group = group, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun FullXiaomiApiBalance(
    card: CredentialCardUi,
    metrics: List<UsageMetric>,
) {
    val byId = remember(metrics) { metrics.associateBy(UsageMetric::id) }
    val balance = byId["account-balance"]
        ?: metrics.firstOrNull { it.presentation == UsageMetricPresentation.Balance }
    if (balance == null) {
        EmptyUsageText()
        return
    }

    Column {
        FullBalanceHero(
            group = SplitCurrencyGroup(
                balance = balance,
                toppedUp = byId["cash-balance"],
                granted = byId["gift-balance"],
            ),
            primaryLabel = "现金",
            secondaryLabel = "赠送",
        )
        FullXiaomiUsage(metrics = metrics)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullBalanceHero(
    group: SplitCurrencyGroup,
    primaryLabel: String,
    secondaryLabel: String,
) {
    val colors = MyTokenThemeTokens.colors
    val currency = group.balance.currencyCode?.trim()?.uppercase().orEmpty()
    val title = if (currency.isEmpty()) "账户余额" else "账户余额 · $currency"
    val primaryValue = group.toppedUp?.value ?: BigDecimal.ZERO
    val secondaryValue = group.granted?.value ?: BigDecimal.ZERO
    val total = primaryValue + secondaryValue
    val primaryWeight = if (total.signum() > 0) {
        primaryValue.toDouble() / total.toDouble()
    } else {
        0.0
    }

    FullSplitLayout(
        left = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatCurrency(group.balance.value, group.balance.currencyCode),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MyTokenType.displayCompact,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        right = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "余额构成",
                    style = MyTokenType.caption,
                    color = colors.muted,
                )
                UsageCompositionMeter(
                    primaryWeight = primaryWeight.takeIf { total.signum() > 0 },
                    height = 11.dp,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_balance_legend_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FullBalanceLegend(
                        label = primaryLabel,
                        metric = group.toppedUp,
                        currencyCode = group.balance.currencyCode,
                        color = colors.ink,
                    )
                    FullBalanceLegend(
                        label = secondaryLabel,
                        metric = group.granted,
                        currencyCode = group.balance.currencyCode,
                        color = colors.actionFill,
                    )
                }
            }
        },
    )
}

@Composable
private fun FullSecondaryBalance(group: SplitCurrencyGroup, modifier: Modifier = Modifier) {
    val colors = MyTokenThemeTokens.colors
    val currency = group.balance.currencyCode?.trim()?.uppercase().orEmpty()
    val primaryValue = group.toppedUp?.value ?: BigDecimal.ZERO
    val secondaryValue = group.granted?.value ?: BigDecimal.ZERO
    val total = primaryValue + secondaryValue
    val primaryWeight = if (total.signum() > 0) {
        primaryValue.toDouble() / total.toDouble()
    } else {
        0.0
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (currency.isEmpty()) "其他币种" else "$currency 账户",
                modifier = Modifier.weight(1f),
                style = MyTokenType.label,
                color = colors.muted,
            )
            Text(
                text = formatCurrency(group.balance.value, group.balance.currencyCode),
                style = MyTokenType.numericValue,
                color = colors.ink,
                maxLines = 1,
            )
        }
        UsageCompositionMeter(
            primaryWeight = primaryWeight.takeIf { total.signum() > 0 },
            modifier = Modifier.padding(top = 5.dp),
            height = 9.dp,
        )
        Text(
            text = "充值 ${formatSplitMetricAmount(group.toppedUp?.value, group.toppedUp ?: group.balance)} · " +
                "赠金 ${formatSplitMetricAmount(group.granted?.value, group.granted ?: group.balance)}",
            modifier = Modifier.padding(top = 3.dp),
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FullBalanceLegend(
    label: String,
    metric: UsageMetric?,
    currencyCode: String?,
    color: Color,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = "$label ${formatCurrency(metric?.value, metric?.currencyCode ?: currencyCode)}",
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullXiaomiUsage(
    metrics: List<UsageMetric>,
) {
    val colors = MyTokenThemeTokens.colors
    val byId = remember(metrics) { metrics.associateBy(UsageMetric::id) }
    val consumption = byId["total-consumption"]
    val totalTokens = byId["total-tokens"]
    val inputTokens = byId["input-tokens"]
    val outputTokens = byId["output-tokens"]
    val cacheTokens = byId["cache-tokens"]
    val inputValue = inputTokens?.value ?: BigDecimal.ZERO
    val outputValue = outputTokens?.value ?: BigDecimal.ZERO
    val cacheValue = cacheTokens?.value ?: BigDecimal.ZERO

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "近 31 天",
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "消费",
                    style = MyTokenType.caption,
                    color = colors.muted,
                )
                Text(
                    text = consumption?.let { formatCurrency(it.value, it.currencyCode) } ?: "-",
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.numericValue,
                    color = colors.ink,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Tokens",
                    style = MyTokenType.caption,
                    color = colors.muted,
                )
                Text(
                    text = formatGrouped(totalTokens?.value),
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.numericValue,
                    color = colors.ink,
                    maxLines = 1,
                )
            }
        }
        UsageMultiSegmentMeter(
            segments = listOf(
                UsageMeterSegment(inputValue.toDouble(), colors.cat1),
                UsageMeterSegment(outputValue.toDouble(), colors.cat2),
                UsageMeterSegment(cacheValue.toDouble(), colors.cat3),
            ),
            modifier = Modifier.padding(top = 6.dp),
            height = 11.dp,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FullTokenLegend("未命中 ${formatGrouped(inputTokens?.value)}", colors.cat1)
            FullTokenLegend("输出 ${formatGrouped(outputTokens?.value)}", colors.cat2)
            FullTokenLegend("缓存 ${formatGrouped(cacheTokens?.value)}", colors.cat3)
        }
    }
}

@Composable
private fun FullTokenLegend(text: String, color: Color) {
    val colors = MyTokenThemeTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = text,
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun FullNewApiSplit(metrics: List<UsageMetric>) {
    val byId = remember(metrics) { metrics.associateBy(UsageMetric::id) }
    val primary = byId["quota-progress"]
        ?: metrics.firstOrNull { it.presentation == UsageMetricPresentation.Progress }
    if (primary == null) {
        EmptyUsageText()
        return
    }

    FullSplitLayout(
        left = {
            FullHeroFocus(
                metric = primary,
                label = "账户额度",
                endLabel = "重置",
                note = "不自动重置",
            )
        },
        right = {
            FullFactsGrid(facts = newApiFacts(byId))
        },
    )
}

private fun glmActivityFacts(byId: Map<String, UsageMetric>): List<SplitFact> = buildList {
    byId["model-calls"]?.let { add(SplitFact("调用次数", "${formatCompact(it.value)} 次")) }
    byId["activity-total-tokens"]?.let {
        add(SplitFact("累计 Tokens", "${formatCompact(it.value)} tokens"))
    }
}

private fun newApiFacts(byId: Map<String, UsageMetric>): List<SplitFact> = buildList {
    fun tokenFact(id: String, label: String) {
        byId[id]?.let { add(SplitFact(label, formatCompact(it.value))) }
    }
    tokenFact("today-token", "今日 Token")
    tokenFact("one-day-token", "24 小时")
    tokenFact("seven-day-token", "7 天")
    tokenFact("thirty-day-token", "30 天")
    byId["rpm"]?.let { add(SplitFact("RPM", formatGrouped(it.value))) }
    byId["tpm"]?.let { add(SplitFact("TPM", formatGrouped(it.value))) }
    byId["request-count"]?.let { add(SplitFact("累计请求", "${formatCompact(it.value)} 次")) }
}

private fun tokenPackFacts(
    metric: UsageMetric?,
    card: CredentialCardUi,
): List<SplitFact> = buildList {
    if (metric == null) return@buildList
    metric.limit?.let { add(SplitFact("总额度", "${formatCompact(it)} tokens")) }
    metric.used?.let { add(SplitFact("已使用", "${formatCompact(it)} tokens")) }
    add(
        SplitFact(
            label = "可用模型",
            value = card.snapshot?.allowedModels.orEmpty().size
                .takeIf { it > 0 }
                ?.let { "$it 个" }
                ?: "—",
        ),
    )
}

private fun mimoPlanFacts(
    plan: UsageMetric?,
    metrics: List<UsageMetric>,
    card: CredentialCardUi,
): List<SplitFact> = buildList {
    val statusMetric = metrics.firstOrNull { it.id == "plan-status" }
    val statusText = card.snapshot?.statusText?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (statusMetric?.healthState == UsageMetricHealthState.Unavailable) {
            "不可用"
        } else {
            "有效"
        }
    val active = statusMetric?.healthState != UsageMetricHealthState.Unavailable &&
        card.snapshot?.status != 0 &&
        !statusText.contains("失效")
    plan?.limit?.let { add(SplitFact("套餐总量", "${formatGrouped(it)} credits")) }
    plan?.used?.let { add(SplitFact("已使用", "${formatGrouped(it)} credits")) }
    add(SplitFact("订阅状态", statusText, positive = active))
    add(
        SplitFact(
            label = "可用模型",
            value = card.snapshot?.allowedModels.orEmpty().size
                .takeIf { it > 0 }
                ?.let { "$it 个" }
                ?: "—",
        ),
    )
}

private fun deepSeekCurrencyGroups(metrics: List<UsageMetric>): List<SplitCurrencyGroup> {
    val balances = metrics.filter { it.presentation == UsageMetricPresentation.Balance }
    if (balances.isEmpty()) return emptyList()
    val multi = balances.size > 1
    return balances.map { balance ->
        val currency = balance.currencyCode?.trim()?.uppercase().orEmpty()
        val suffix = if (multi && currency.isNotEmpty()) "-$currency" else ""
        SplitCurrencyGroup(
            balance = balance,
            toppedUp = metrics.firstOrNull { it.id == "toppedUpBalance$suffix" }
                ?: metrics.firstOrNull { !multi && it.id == "toppedUpBalance" },
            granted = metrics.firstOrNull { it.id == "grantedBalance$suffix" }
                ?: metrics.firstOrNull { !multi && it.id == "grantedBalance" },
        )
    }
}

private fun isXiaomiApiBalanceCard(card: CredentialCardUi): Boolean {
    if (card.credential.providerId != ProviderId.Xiaomi) return false
    val usageKind = card.credential.metadata[CredentialMetadataKey.UsageKind]
        ?: card.snapshot?.usageKind
    return usageKind != "plan"
}

private fun splitTone(metric: UsageMetric): SplitTone = when (metricHealthTone(metric)) {
    UsageCardHealth.Critical -> SplitTone.Critical
    UsageCardHealth.Warning -> SplitTone.Warning
    else -> SplitTone.Normal
}

private fun splitToneUsageTone(tone: SplitTone): UsageMetricTone = when (tone) {
    SplitTone.Normal -> UsageMetricTone.Normal
    SplitTone.Warning -> UsageMetricTone.Warning
    SplitTone.Critical -> UsageMetricTone.Critical
}

internal fun formatRemaining(metric: UsageMetric): String {
    val value = metric.remaining ?: return "-"
    val percentQuota = metric.limit?.compareTo(BigDecimal("100")) == 0 &&
        metric.used?.let { it >= BigDecimal.ZERO && it <= BigDecimal("100") } == true
    if (percentQuota) return "${formatGrouped(value)}%"
    return when (metric.unit) {
        UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
        UsageMetricUnit.Request -> "${formatGrouped(value)} 次"
        else -> formatGrouped(value)
    }
}

private fun formatSplitMetricAmount(
    value: BigDecimal?,
    metric: UsageMetric,
): String = when {
    value == null -> "-"
    metric.unit == UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
    metric.unit == UsageMetricUnit.Request -> "${formatGrouped(value)} 次"
    else -> formatGrouped(value)
}

internal fun quotaEndText(end: Instant?, label: String): String? {
    if (end == null) return null
    return when (label) {
        "到期" -> "${formatResetTime(end)} 到期"
        "有效期至" -> "有效期至 ${formatResetTime(end)}"
        else -> "${formatResetTime(end)} 重置"
    }
}

internal fun windowLabel(metric: UsageMetric): String {
    val label = metric.label.trim()
    return when {
        metric.id == "credit-progress" -> "本月额度"
        metric.id == "plan-total" -> "套餐额度"
        metric.id == "token" -> "Token 套餐"
        label.contains("5") && label.contains("小时") -> "5 小时用量"
        label.contains("周") -> "本周用量"
        label.contains("月") -> "本月用量"
        else -> label
    }
}

private fun commandCodeWindowLabel(metric: UsageMetric): String = when (metric.id) {
    "five-hour" -> "5 小时额度"
    "weekly" -> "本周额度"
    "credit-progress" -> "本月额度"
    else -> windowLabel(metric)
}

private fun quotaWindowLabel(metric: UsageMetric): String = when (metric.id) {
    "fiveHour" -> "5 小时额度"
    "weekly" -> "本周额度"
    "monthly" -> "本月额度"
    else -> windowLabel(metric)
}
