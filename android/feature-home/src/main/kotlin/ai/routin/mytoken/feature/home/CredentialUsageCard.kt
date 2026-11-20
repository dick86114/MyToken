package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/** 状态色：只用于数据可视化（弧线、细条、构成条）。 */
internal data class StatusColors(
    val brand: Color,
    val positive: Color,
    val warning: Color,
    val critical: Color,
    val neutral: Color,
)

@Composable
internal fun statusColors(): StatusColors {
    val colors = MyTokenThemeTokens.colors
    return StatusColors(
        brand = colors.actionFill,
        positive = colors.positiveFill,
        warning = colors.warningFill,
        critical = colors.criticalFill,
        neutral = colors.statusDotNormal,
    )
}

/** 一张凭证卡片：身份 → 主指标 → 次级窗口 → facts → 页脚。 */
@Composable
fun CredentialUsageCard(
    card: CredentialCardUi,
    metricColumns: Int = 2,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = onRetry,
    modifier: Modifier = Modifier,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    highlightSignal: Int = 0,
) {
    val compact = usageCardDensity.isCompact()
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }
    val health = usageCardHealth(card)
    // 只有“首次加载且没有缓存数据”才显示骨架；带数据刷新时保留内容，避免闪烁。
    val showsSkeleton = health == UsageCardHealth.Loading && card.snapshot?.metrics.isNullOrEmpty()

    val motionEnabled = rememberMyTokenMotionEnabled()
    val locateLift = remember { Animatable(0f) }
    LaunchedEffect(highlightSignal) {
        if (highlightSignal <= 0) {
            locateLift.snapTo(0f)
            return@LaunchedEffect
        }
        locateLift.snapTo(0f)
        val duration = MyTokenMotion.locateDurationMillis(motionEnabled)
        locateLift.animateTo(1f, tween(durationMillis = duration, easing = FastOutSlowInEasing))
        locateLift.animateTo(0f, tween(durationMillis = duration, easing = FastOutSlowInEasing))
    }
    val locateBounceModifier = if (highlightSignal > 0) {
        Modifier.graphicsLayer {
            val lift = locateLift.value
            scaleX = 1f + 0.02f * lift
            scaleY = 1f + 0.02f * lift
            translationY = -4.dp.toPx() * lift
        }
    } else {
        Modifier
    }

    val metrics = card.snapshot?.metrics.orEmpty()
    val primaryMetric = metrics.firstOrNull()
    val primaryText = when {
        primaryMetric == null -> ""
        primaryMetric.presentation == UsageMetricPresentation.Balance ->
            formatCurrency(primaryMetric.value, primaryMetric.currencyCode)
        else -> formatPercent(progressPercent(primaryMetric))
    }
    val cardDescription = listOfNotNull(
        card.credential.name,
        usageCardStatusText(health),
        primaryText.takeIf { it.isNotBlank() },
    ).joinToString("，")

    UsageCardFrame(
        density = usageCardDensity,
        onClick = onOpen,
        modifier = modifier
            .then(locateBounceModifier)
            .semantics { contentDescription = cardDescription }
            .testTag("credential_card_${card.credential.id}"),
        footer = {
            if (showsSkeleton) {
                UsageCardSkeletonFooter()
            } else {
                UsageCardFooter(
                    text = if (compact) usageCardFooterText(card, health) else card.freshness.text,
                    showChevron = !compact,
                )
            }
        },
    ) {
        UsageCardIdentity(
            card = card,
            density = usageCardDensity,
            health = health,
            onShowFailure = { showsFailureDetails = true },
            onRetry = onRefresh,
            showStatus = !showsSkeleton,
        )
        if (showsSkeleton) {
            UsageCardSkeletonBody(compact = compact)
        } else if (compact) {
            CompactUsageCardBody(card = card, health = health)
        } else {
            FullUsageCardBody(card = card)
        }
    }

    if (showsFailureDetails) {
        RefreshFailureDialog(
            card = card,
            onDismiss = { showsFailureDetails = false },
            onRetry = {
                showsFailureDetails = false
                onRetry()
            },
        )
    }
}

@Composable
private fun CompactUsageCardBody(card: CredentialCardUi, health: UsageCardHealth) {
    val metrics = UsageCardCompactSpec.orderedMetrics(
        providerId = card.credential.providerId,
        metadata = card.credential.metadata,
        metrics = card.snapshot?.metrics.orEmpty(),
    )

    if (metrics.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MyTokenType.caption,
            color = MyTokenThemeTokens.colors.muted,
        )
        return
    }

    when (CompactUsageCardPresentation.arrangement(metrics)) {
        CompactUsageArrangement.Balance -> CompactBalanceLayout(
            metric = metrics.first(),
            composition = CompactBalanceCompositionPolicy.segments(
                card.snapshot?.metrics.orEmpty(),
            ),
            modelCount = card.snapshot?.allowedModels.orEmpty().size,
        )

        CompactUsageArrangement.RingWithWindows -> CompactRingWithWindows(
            metrics = metrics,
            health = health,
        )

        CompactUsageArrangement.ValueTiles -> CompactValueGrid(metrics = metrics)
    }
}

@Composable
private fun CompactRingWithWindows(
    metrics: List<UsageMetric>,
    health: UsageCardHealth,
) {
    val colors = MyTokenThemeTokens.colors
    val primary = metrics.first()
    val secondary = metrics.drop(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.testTag("compact_ring_primary"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsageArcGauge(
                percent = progressPercent(primary),
                tone = usageCardRingTone(health),
                diameter = 52.dp,
                inactive = usageCardRingInactive(health),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = compactPeriodLabel(primary),
                    style = MyTokenType.label,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    maxLines = 1,
                )
                if (primary.resetCards.isNotEmpty()) {
                    ResetCardsBadge(metric = primary, style = ResetCardsBadgeStyle.Compact)
                }
            }
            if (secondary.isEmpty()) {
                Text(
                    text = ringSubtitle(primary),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }
        if (secondary.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                secondary.forEach { metric ->
                    UsageCardWindowRow(metric = metric, dense = true)
                }
            }
        }
    }
}

@Composable
private fun CompactBalanceLayout(
    metric: UsageMetric,
    composition: List<CompactBalanceSegment>,
    modelCount: Int,
) {
    val colors = MyTokenThemeTokens.colors
    val currency = metric.currencyCode?.trim()?.uppercase().orEmpty()
    val title = if (currency.isEmpty()) "账户余额" else "账户余额 · $currency"
    val compositionTotal = composition.fold(BigDecimal.ZERO) { total, segment ->
        total + segment.value
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        Text(
            text = title,
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val amount = splitBalanceAmount(formatCurrency(metric.value, metric.currencyCode))
        val markSpan = SpanStyle(
            fontSize = 0.6.em,
            fontWeight = FontWeight(650),
            color = colors.muted,
        )
        Text(
            text = buildAnnotatedString {
                if (amount.prefix.isNotEmpty()) {
                    withStyle(markSpan) { append(amount.prefix) }
                }
                append(amount.core)
                if (amount.suffix.isNotEmpty()) {
                    append(" ")
                    withStyle(markSpan) { append(amount.suffix) }
                }
            },
            modifier = Modifier.padding(top = 4.dp),
            style = MyTokenType.displayCompact,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (composition.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.track),
            ) {
                if (compositionTotal.signum() > 0) {
                    composition.forEach { segment ->
                        val weight = segment.value.toDouble() / compositionTotal.toDouble()
                        if (weight > 0.0) {
                            Box(
                                modifier = Modifier
                                    .weight(weight.toFloat())
                                    .height(5.dp)
                                    .background(
                                        when (segment.kind) {
                                            CompactBalanceSegmentKind.ToppedUp -> colors.cat1
                                            CompactBalanceSegmentKind.Granted -> colors.cat2
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                composition.forEach { segment ->
                    CompactBalanceLegend(
                        segment = segment,
                        currencyCode = metric.currencyCode,
                    )
                }
            }
        }
        if (modelCount > 0) {
            Text(
                text = "可用模型 $modelCount 个",
                modifier = Modifier.padding(top = 6.dp),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactBalanceLegend(
    segment: CompactBalanceSegment,
    currencyCode: String?,
) {
    val colors = MyTokenThemeTokens.colors
    val tint = when (segment.kind) {
        CompactBalanceSegmentKind.ToppedUp -> colors.cat1
        CompactBalanceSegmentKind.Granted -> colors.cat2
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(tint, CircleShape),
        )
        Text(
            text = "${segment.label} ${formatCurrency(segment.value, currencyCode)}",
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactValueGrid(metrics: List<UsageMetric>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowMetrics.forEach { metric ->
                    CompactValueCell(metric = metric, modifier = Modifier.weight(1f))
                }
                repeat(2 - rowMetrics.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CompactValueCell(metric: UsageMetric, modifier: Modifier = Modifier) {
    val colors = MyTokenThemeTokens.colors
    val value = compactValueParts(metric)
    BoxWithConstraints(modifier = modifier) {
        val narrow = maxWidth < 96.dp || LocalDensity.current.fontScale > 1.2f
        Column {
            Text(
                text = metric.label.trim(),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value.main,
                    modifier = Modifier.weight(1f, fill = false),
                    style = if (narrow) MyTokenType.label else MyTokenType.numericValue,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                value.suffix?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                        style = MyTokenType.micro,
                        color = colors.muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private data class CompactValueParts(
    val main: String,
    val suffix: String? = null,
)

private fun compactValueParts(metric: UsageMetric): CompactValueParts = when {
    metric.presentation == UsageMetricPresentation.Balance ->
        CompactValueParts(formatCurrency(metric.value, metric.currencyCode))

    metric.presentation == UsageMetricPresentation.Status ->
        CompactValueParts("—")

    metric.unit == UsageMetricUnit.Token -> splitCompactValue(formatCompact(metric.value))
    metric.unit == UsageMetricUnit.Currency ->
        CompactValueParts(formatCurrency(metric.value, metric.currencyCode))

    metric.unit == UsageMetricUnit.Request -> CompactValueParts(formatCompact(metric.value), "次")
    else -> CompactValueParts(formatDecimal(metric.value))
}

private fun splitCompactValue(text: String): CompactValueParts {
    val suffixStart = text.indexOfFirst { character ->
        !character.isDigit() && character != '.' && character != '-'
    }
    return if (suffixStart > 0) {
        CompactValueParts(
            main = text.substring(0, suffixStart),
            suffix = text.substring(suffixStart),
        )
    } else {
        CompactValueParts(text)
    }
}

@Composable
private fun FullUsageCardBody(card: CredentialCardUi) {
    val rawMetrics = card.snapshot?.metrics.orEmpty()
    if (rawMetrics.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MyTokenType.caption,
            color = MyTokenThemeTokens.colors.muted,
        )
        return
    }
    FullSplitUsageContent(card = card, metrics = rawMetrics)
}

internal fun compactPeriodLabel(metric: UsageMetric): String {
    val label = metric.label.trim()
    return when {
        label.contains("5") && label.contains("小时") -> "5 小时"
        label.contains("周") -> "周"
        label.contains("月") -> "月"
        else -> label.removeSuffix("用量").trim()
    }
}

private fun ringSubtitle(metric: UsageMetric): String {
    val end = metric.windowEnd ?: return if ((progressPercent(metric) ?: 0.0) <= 0.0) "待命中" else "--"
    return formatResetTime(end)
}

@Composable
private fun RefreshFailureDialog(
    card: CredentialCardUi,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    MyTokenDialog(
        title = "刷新失败",
        subtitle = "请检查网络或凭证状态",
        icon = Icons.Outlined.Warning,
        tone = MyTokenDialogTone.Warning,
        onDismissRequest = onDismiss,
        content = {
            MyTokenDialogSection(title = "失败原因") {
                MyTokenDialogRow(label = card.error?.message ?: "更新失败")
            }
            MyTokenDialogStatus(
                message = if (card.snapshot == null) {
                    "暂无可用缓存，重试将重新请求用量数据。"
                } else {
                    "当前仍显示上次成功同步的数据。"
                },
                tone = MyTokenDialogTone.Info,
            )
        },
        actions = {
            MyTokenButton(
                text = "关闭",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Neutral,
            )
            MyTokenButton(
                text = "重试",
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Primary,
            )
        },
    )
}
