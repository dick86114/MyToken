package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class SplitAccountStatus(
    val text: String,
    val healthy: Boolean?,
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
                bottom = { FullCommandCodeSummary(byId) },
            )
        }

        card.credential.providerId == ProviderId.Routin && usageKind == "tokenPack" -> {
            FullPackSplit(
                primary = byId["token"],
                leftLabel = "Token 套餐",
                endLabel = "有效期至",
                endOverride = card.snapshot?.subscriptionEndAt,
                remainingFormatter = { metric ->
                    "${formatCompact(metric.remaining)} tokens"
                },
                facts = tokenPackFacts(byId["token"], card),
            )
        }

        card.credential.providerId == ProviderId.Xiaomi && usageKind == "plan" -> {
            FullPackSplit(
                primary = byId["plan-total"],
                leftLabel = "套餐额度",
                endLabel = "到期",
                remainingFormatter = { metric ->
                    "${formatGrouped(metric.remaining)} credits"
                },
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
                facts = glmFacts(byId),
                leftLabel = primary?.let(::windowLabel) ?: "当前用量",
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
private fun FullSplitLayout(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                left()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                right()
            }
        } else {
            val dividerColor = MaterialTheme.colorScheme.outlineVariant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val x = size.width * 0.4f
                        drawLine(
                            color = dividerColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(end = 12.dp),
                ) {
                    left()
                }
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(start = 13.dp),
                ) {
                    right()
                }
            }
        }
    }
}

@Composable
private fun FullQuotaSplit(
    primary: UsageMetric?,
    secondary: List<UsageMetric>,
    facts: List<SplitFact> = emptyList(),
    leftLabel: String,
    endLabel: String = "重置",
    labelFormatter: (UsageMetric) -> String = ::windowLabel,
    bottom: (@Composable () -> Unit)? = null,
) {
    if (primary == null) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FullSplitLayout(
            left = {
                FullQuotaFocus(
                    metric = primary,
                    label = leftLabel,
                    endLabel = endLabel,
                )
            },
            right = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    secondary.forEach { metric ->
                        FullWindowRow(
                            metric = metric,
                            endLabel = endLabel,
                            labelFormatter = labelFormatter,
                        )
                    }
                    if (facts.isNotEmpty()) {
                        FullFacts(facts = facts, topPadding = if (secondary.isEmpty()) 0.dp else 8.dp)
                    }
                }
            },
        )
        bottom?.invoke()
    }
}

@Composable
private fun FullPackSplit(
    primary: UsageMetric?,
    leftLabel: String,
    endLabel: String,
    remainingFormatter: (UsageMetric) -> String,
    facts: List<SplitFact>,
    endOverride: Instant? = null,
) {
    if (primary == null) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    FullSplitLayout(
        left = {
            FullQuotaFocus(
                metric = primary,
                label = leftLabel,
                endLabel = endLabel,
                endOverride = endOverride,
                remainingFormatter = remainingFormatter,
            )
        },
        right = {
            FullFacts(facts = facts, topPadding = 0.dp)
        },
    )
}

@Composable
private fun FullQuotaFocus(
    metric: UsageMetric,
    label: String,
    endLabel: String,
    endOverride: Instant? = null,
    remainingFormatter: (UsageMetric) -> String = ::formatRemaining,
    note: String? = null,
) {
    val colors = statusColors()
    val percent = progressPercent(metric)
    val tone = splitTone(metric, percent)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MyTokenType.metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (metric.resetCards.isNotEmpty()) {
                ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Compact)
            }
        }
        Text(
            text = formatPercent(percent),
            modifier = Modifier.padding(top = 4.dp),
            style = MyTokenType.heroNumber,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        FullSplitProgress(
            percent = percent,
            color = splitToneColor(tone, colors),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "剩余 ${remainingFormatter(metric)}",
            modifier = Modifier.padding(top = 5.dp),
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = note ?: quotaEndText(metric.windowEnd ?: endOverride, endLabel) ?: "—",
            modifier = Modifier.padding(top = 2.dp),
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FullWindowRow(
    metric: UsageMetric,
    endLabel: String,
    labelFormatter: (UsageMetric) -> String,
) {
    val colors = statusColors()
    val percent = progressPercent(metric)
    val tone = splitTone(metric, percent)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = labelFormatter(metric),
                style = MyTokenType.metricLabelTight,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (metric.resetCards.isNotEmpty()) {
                ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Compact)
            }
            Text(
                text = formatPercent(percent),
                style = MyTokenType.metricValue,
                color = when (tone) {
                    SplitTone.Normal -> MaterialTheme.colorScheme.onSurface
                    SplitTone.Warning -> colors.warning
                    SplitTone.Critical -> colors.critical
                },
                maxLines = 1,
            )
        }
        FullSplitProgress(
            percent = percent,
            color = splitToneColor(tone, colors),
            modifier = Modifier.padding(top = 4.dp),
            height = 3.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "剩余 ${formatRemaining(metric)}",
                modifier = Modifier.weight(1f),
                style = MyTokenType.metricCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = quotaEndText(metric.windowEnd, endLabel) ?: "—",
                style = MyTokenType.metricCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FullFacts(
    facts: List<SplitFact>,
    topPadding: androidx.compose.ui.unit.Dp,
) {
    val colors = statusColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        facts.forEach { fact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 1.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = fact.label,
                    style = MyTokenType.factLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = fact.value,
                    modifier = Modifier.weight(1f),
                    style = MyTokenType.factValue,
                    color = if (fact.positive) {
                        colors.positive
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    },
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FullCommandCodeSummary(byId: Map<String, UsageMetric>) {
    val composition = listOfNotNull(
        byId["monthly-remaining"]?.let {
            "月度 ${formatCurrency(it.value, it.currencyCode)}"
        },
        byId["purchased-remaining"]?.let {
            "购买 ${formatCurrency(it.value, it.currencyCode)}"
        },
        byId["free-remaining"]?.let {
            "赠送 ${formatCurrency(it.value, it.currencyCode)}"
        },
    ).joinToString(" · ")
    val periodSpent = byId["period-spent"]
    val requests = byId["request-count"]
    if (composition.isEmpty() && periodSpent == null && requests == null) return

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (composition.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "额度构成",
                    style = MyTokenType.factLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = composition,
                    modifier = Modifier.weight(1f),
                    style = MyTokenType.factValue,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (periodSpent != null || requests != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CommandCodeSummaryValue(
                    label = "本周期消费",
                    value = periodSpent?.let { formatCurrency(it.value, it.currencyCode) } ?: "-",
                    modifier = Modifier.weight(1f),
                )
                CommandCodeSummaryValue(
                    label = "累计请求",
                    value = requests?.let { "${formatCompact(it.value)} 次" } ?: "-",
                    modifier = Modifier.weight(1f),
                    alignEnd = true,
                )
            }
        }
    }
}

@Composable
private fun CommandCodeSummaryValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 1.dp),
            style = MyTokenType.factValue,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun FullSplitProgress(
    percent: Double?,
    color: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 4.dp,
) {
    val fraction = ((percent ?: 0.0).coerceIn(0.0, 100.0) / 100.0).toFloat()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = trackColor,
            cornerRadius = radius,
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = color,
                size = Size(size.width * fraction, size.height),
                cornerRadius = radius,
            )
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
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column {
        FullBalanceHero(
            group = primary,
            primaryLabel = "充值",
            secondaryLabel = "赠金",
            status = balanceAccountStatus(card, metrics),
            modelsText = modelCountText(card),
        )
        groups.drop(1).forEach { group ->
            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            FullSecondaryBalance(group = group)
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
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            status = balanceAccountStatus(card, metrics),
            modelsText = modelCountText(card),
        )
        FullXiaomiUsage(metrics = metrics)
    }
}

@Composable
private fun FullBalanceHero(
    group: SplitCurrencyGroup,
    primaryLabel: String,
    secondaryLabel: String,
    status: SplitAccountStatus,
    modelsText: String,
) {
    val colors = statusColors()
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MyTokenType.metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status.healthy == false) {
                FullAccountStatus(status = status)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatCurrency(group.balance.value, group.balance.currencyCode),
                modifier = Modifier.weight(1f),
                style = MyTokenType.balanceNumber,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier.padding(start = 10.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "可用模型",
                    style = MyTokenType.metricCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = modelsText,
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.metricValue,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
        ) {
            if (total.signum() > 0) {
                if (primaryWeight > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(primaryWeight.toFloat())
                            .height(5.dp)
                            .background(colors.positive),
                    )
                }
                if (primaryWeight < 1.0) {
                    Box(
                        modifier = Modifier
                            .weight((1.0 - primaryWeight).toFloat())
                            .height(5.dp)
                            .background(colors.brand),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FullBalanceLegend(
                label = primaryLabel,
                metric = group.toppedUp,
                currencyCode = group.balance.currencyCode,
                color = colors.positive,
            )
            FullBalanceLegend(
                label = secondaryLabel,
                metric = group.granted,
                currencyCode = group.balance.currencyCode,
                color = colors.brand,
            )
        }
    }
}

@Composable
private fun FullSecondaryBalance(group: SplitCurrencyGroup) {
    val colors = statusColors()
    val currency = group.balance.currencyCode?.trim()?.uppercase().orEmpty()
    val primaryValue = group.toppedUp?.value ?: BigDecimal.ZERO
    val secondaryValue = group.granted?.value ?: BigDecimal.ZERO
    val total = primaryValue + secondaryValue
    val primaryWeight = if (total.signum() > 0) {
        primaryValue.toDouble() / total.toDouble()
    } else {
        0.0
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (currency.isEmpty()) "其他币种" else "$currency 账户",
                modifier = Modifier.weight(1f),
                style = MyTokenType.metricLabelTight,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCurrency(group.balance.value, group.balance.currencyCode),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.35).sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
        ) {
            if (total.signum() > 0) {
                if (primaryWeight > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(primaryWeight.toFloat())
                            .height(4.dp)
                            .background(colors.positive),
                    )
                }
                if (primaryWeight < 1.0) {
                    Box(
                        modifier = Modifier
                            .weight((1.0 - primaryWeight).toFloat())
                            .height(4.dp)
                            .background(colors.brand),
                    )
                }
            }
        }
        Text(
            text = "充值 ${formatSplitMetricAmount(group.toppedUp?.value, group.toppedUp ?: group.balance)} · " +
                "赠金 ${formatSplitMetricAmount(group.granted?.value, group.granted ?: group.balance)}",
            modifier = Modifier.padding(top = 3.dp),
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val colors = statusColors()
    val byId = remember(metrics) { metrics.associateBy(UsageMetric::id) }
    val consumption = byId["total-consumption"]
    val totalTokens = byId["total-tokens"]
    val inputTokens = byId["input-tokens"]
    val outputTokens = byId["output-tokens"]
    val cacheTokens = byId["cache-tokens"]
    val inputValue = inputTokens?.value ?: BigDecimal.ZERO
    val outputValue = outputTokens?.value ?: BigDecimal.ZERO
    val cacheValue = cacheTokens?.value ?: BigDecimal.ZERO
    val tokenTotal = inputValue + outputValue + cacheValue

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "近 31 天",
                style = MyTokenType.metricLabel,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                    style = MyTokenType.metricCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = consumption?.let { formatCurrency(it.value, it.currencyCode) } ?: "-",
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.metricValueStrong,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Tokens",
                    style = MyTokenType.metricCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 1.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = formatGrouped(totalTokens?.value),
                        style = MyTokenType.metricValueStrong,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = "tokens",
                        modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
        ) {
            if (tokenTotal.signum() > 0) {
                val total = tokenTotal.toDouble()
                if (inputValue.signum() > 0) {
                    Box(
                        modifier = Modifier
                            .weight((inputValue.toDouble() / total).toFloat())
                            .height(4.dp)
                            .background(colors.brand),
                    )
                }
                if (outputValue.signum() > 0) {
                    Box(
                        modifier = Modifier
                            .weight((outputValue.toDouble() / total).toFloat())
                            .height(4.dp)
                            .background(colors.warning),
                    )
                }
                if (cacheValue.signum() > 0) {
                    Box(
                        modifier = Modifier
                            .weight((cacheValue.toDouble() / total).toFloat())
                            .height(4.dp)
                            .background(colors.neutral),
                    )
                }
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FullTokenLegend("未命中 ${formatGrouped(inputTokens?.value)}", colors.brand)
            FullTokenLegend("输出 ${formatGrouped(outputTokens?.value)}", colors.warning)
            FullTokenLegend("缓存 ${formatGrouped(cacheTokens?.value)}", colors.neutral)
        }
    }
}

@Composable
private fun FullTokenLegend(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color, CircleShape),
        )
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val windows = listOf(
        "今日" to ("today-token" to "today-token-cost"),
        "近 24 小时" to ("one-day-token" to "one-day-token-cost"),
        "近 7 天" to ("seven-day-token" to "seven-day-token-cost"),
        "近 30 天" to ("thirty-day-token" to "thirty-day-token-cost"),
    )

    Column {
        FullSplitLayout(
            left = {
                FullQuotaFocus(
                    metric = primary,
                    label = "账户额度",
                    endLabel = "重置",
                    note = "不自动重置",
                )
            },
            right = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    windows.forEach { (label, ids) ->
                        val tokens = byId[ids.first]
                        val cost = byId[ids.second]
                        FullNewApiWindowRow(
                            label = label,
                            tokens = tokens,
                            cost = cost,
                        )
                    }
                }
            },
        )
        FullRealtimeLine(
            items = listOfNotNull(
                byId["rpm"]?.let { "RPM" to formatGrouped(it.value) },
                byId["tpm"]?.let { "TPM" to formatGrouped(it.value) },
                byId["request-count"]?.let {
                    "累计请求" to "${formatCompact(it.value)} 次"
                },
            ),
        )
    }
}

@Composable
private fun FullNewApiWindowRow(
    label: String,
    tokens: UsageMetric?,
    cost: UsageMetric?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(64.dp),
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = tokens?.value?.let { "${formatCompact(it)} tokens" } ?: "-",
            modifier = Modifier.weight(1f),
            style = MyTokenType.metricValue,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = cost?.let { formatCurrency(it.value, it.currencyCode) } ?: "-",
            style = MyTokenType.metricCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun FullRealtimeLine(items: List<Pair<String, String>>) {
    if (items.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(13.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.first,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = item.second,
                    style = MyTokenType.metricValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FullAccountStatus(status: SplitAccountStatus) {
    val colors = statusColors()
    val color = when (status.healthy) {
        true -> colors.positive
        false -> colors.critical
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color, CircleShape),
        )
        Text(
            text = status.text,
            style = MyTokenType.badge.copy(fontSize = 9.5.sp, lineHeight = 12.sp),
            color = color,
            maxLines = 1,
        )
    }
}

private fun glmFacts(byId: Map<String, UsageMetric>): List<SplitFact> = buildList {
    byId["model-calls"]?.let {
        add(SplitFact("模型调用", "${formatCompact(it.value)} 次"))
    }
    byId["zcode-mcp"]?.let {
        add(
            SplitFact(
                "MCP 调用",
                "${formatGrouped(it.used ?: it.value)} / ${formatGrouped(it.limit)}",
            ),
        )
    }
    byId["activity-total-tokens"]?.let {
        add(SplitFact("累计 Tokens", formatCompact(it.value)))
    }
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

private fun modelCountText(card: CredentialCardUi): String =
    card.snapshot?.allowedModels.orEmpty().size
        .takeIf { it > 0 }
        ?.let { "$it 个" }
        ?: "—"

private fun isXiaomiApiBalanceCard(card: CredentialCardUi): Boolean {
    if (card.credential.providerId != ProviderId.Xiaomi) return false
    val usageKind = card.credential.metadata[CredentialMetadataKey.UsageKind]
        ?: card.snapshot?.usageKind
    return usageKind != "plan"
}

private fun balanceAccountStatus(
    card: CredentialCardUi,
    metrics: List<UsageMetric>,
): SplitAccountStatus {
    val availability = metrics.firstOrNull { it.id == "availability" }
    if (availability != null) {
        val available = (availability.value?.signum() ?: 0) > 0
        return SplitAccountStatus(
            text = if (available) "账户可用" else "账户不可用",
            healthy = available,
        )
    }
    val raw = card.snapshot?.statusText?.trim()?.takeIf { it.isNotEmpty() }
    if (raw != null) {
        val text = if (raw.startsWith("账户")) raw else "账户$raw"
        val healthy = !raw.contains("不可用") &&
            !raw.contains("失效") &&
            card.snapshot?.status != 0
        return SplitAccountStatus(text = text, healthy = healthy)
    }
    return SplitAccountStatus(text = "账户状态 —", healthy = null)
}

private fun splitTone(metric: UsageMetric, percent: Double?): SplitTone {
    if (metric.healthState == UsageMetricHealthState.Unavailable ||
        metric.healthState == UsageMetricHealthState.Critical ||
        (percent ?: 0.0) >= 80
    ) {
        return SplitTone.Critical
    }
    if (metric.healthState == UsageMetricHealthState.Warning || (percent ?: 0.0) >= 50) {
        return SplitTone.Warning
    }
    return SplitTone.Normal
}

@Composable
private fun splitToneColor(tone: SplitTone, colors: StatusColors): Color = when (tone) {
    SplitTone.Normal -> colors.brand
    SplitTone.Warning -> colors.warning
    SplitTone.Critical -> colors.critical
}

private fun formatRemaining(metric: UsageMetric): String {
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

private fun quotaEndText(end: Instant?, label: String): String? {
    if (end == null) return null
    return when (label) {
        "到期" -> "${formatResetTime(end)} 到期"
        "有效期至" -> "有效期至 ${formatResetTime(end)}"
        else -> "${formatResetTime(end)} 重置"
    }
}

private fun windowLabel(metric: UsageMetric): String {
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
