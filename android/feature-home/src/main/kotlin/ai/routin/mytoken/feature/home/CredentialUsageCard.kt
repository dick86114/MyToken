package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.ProviderId
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay

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
    selected: Boolean = false,
    flat: Boolean = false,
) {
    val compact = usageCardDensity.isCompact()
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }
    val health = usageCardHealth(card)
    // 只有“首次加载且没有缓存数据”才显示骨架；带数据刷新时保留内容，避免闪烁。
    val showsSkeleton = health == UsageCardHealth.Loading && card.snapshot?.metrics.isNullOrEmpty()

    val motionEnabled = rememberMyTokenMotionEnabled()
    val wide = layoutMode != MyTokenLayoutMode.Compact
    val locateLift = remember { Animatable(0f) }
    LaunchedEffect(highlightSignal) {
        if (highlightSignal <= 0) {
            locateLift.snapTo(0f)
            return@LaunchedEffect
        }
        locateLift.snapTo(0f)
        val duration = MyTokenMotion.locateDurationMillis(motionEnabled)
        val pulses = if (wide) MyTokenMotion.wideLocatePulseCount else 1
        repeat(pulses) { pulse ->
            locateLift.animateTo(
                1f,
                tween(durationMillis = duration, easing = FastOutSlowInEasing),
            )
            locateLift.animateTo(
                0f,
                tween(durationMillis = duration, easing = FastOutSlowInEasing),
            )
            if (pulse < pulses - 1) delay(MyTokenMotion.locatePulseGapMillis.toLong())
        }
    }
    val highlightInk = MyTokenThemeTokens.colors.ink
    val locateBounceModifier = if (highlightSignal > 0) {
        Modifier
            .graphicsLayer {
                val lift = locateLift.value
                val scale = if (wide) 0.045f else 0.02f
                scaleX = 1f + scale * lift
                scaleY = 1f + scale * lift
                translationY = (if (wide) -9.dp else -4.dp).toPx() * lift
            }
            // 大屏定位用高对比描边 + 位移，多次脉冲时远看也能辨认。
            .then(
                if (wide) {
                    Modifier.drawWithContent {
                        drawContent()
                        val lift = locateLift.value
                        if (lift > 0.01f) {
                            val stroke = 2.dp.toPx()
                            drawRoundRect(
                                color = highlightInk,
                                alpha = (0.18f + 0.82f * lift).coerceIn(0f, 1f),
                                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(
                                    x = MyTokenRadius.card.toPx(),
                                    y = MyTokenRadius.card.toPx(),
                                ),
                                style = Stroke(width = stroke),
                            )
                        }
                    }
                } else {
                    Modifier
                },
            )
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

    val selectionModifier = if (selected) {
        Modifier.border(
            width = 2.dp,
            color = MyTokenThemeTokens.colors.ink,
            shape = RoundedCornerShape(MyTokenRadius.card),
        )
    } else {
        Modifier
    }
    UsageCardFrame(
        density = usageCardDensity,
        onClick = onOpen,
        flat = flat,
        modifier = modifier
            .then(selectionModifier)
            .then(locateBounceModifier)
            .semantics { contentDescription = cardDescription }
            .testTag("credential_card_${card.credential.id}"),
        footer = {
            when {
                showsSkeleton -> UsageCardSkeletonFooter()
                compact -> {
                    val (left, right) = usageCardFooterParts(card, health)
                    UsageCardFooterColumns(left = left, right = right, health = health)
                }
                else -> UsageCardFooter(
                    text = usageCardFooterText(card, health),
                    showChevron = true,
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
            granted = CompactBalanceCompositionPolicy.segments(
                card.snapshot?.metrics.orEmpty(),
            ).firstOrNull { it.kind == CompactBalanceSegmentKind.Granted }?.value,
        )

        CompactUsageArrangement.RingWithWindows -> CompactRingWithWindows(
            card = card,
            metrics = metrics,
            health = health,
        )

        CompactUsageArrangement.ValueTiles -> CompactValueGrid(
            metrics = if (card.credential.providerId == ProviderId.NewAPI) {
                // 简洁卡只保留周期区分文案，Token 单位由数值本身表达。
                metrics.map { it.copy(label = it.label.removeSuffix(" Token")) }
            } else {
                metrics
            },
        )
    }
}

@Composable
private fun CompactRingWithWindows(
    card: CredentialCardUi,
    metrics: List<UsageMetric>,
    health: UsageCardHealth,
) {
    val colors = MyTokenThemeTokens.colors
    val primary = metrics.first()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
                modifier = Modifier.height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = compactPeriodLabel(primary),
                    style = MyTokenType.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                    maxLines = 1,
                )
                if (primary.resetCards.isNotEmpty()) {
                    ResetCardsBadge(metric = primary, style = ResetCardsBadgeStyle.Compact)
                }
            }
        }

        val miniRows = compactMiniRows(metrics = metrics)
        if (miniRows.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                miniRows.forEach { metric ->
                    CompactMiniProgressRow(metric)
                }
            }
        }
    }
}

private fun compactMiniRows(metrics: List<UsageMetric>): List<UsageMetric> =
    metrics.drop(1).filter { it.presentation == UsageMetricPresentation.Progress }

@Composable
private fun CompactMiniProgressRow(metric: UsageMetric) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    val tone = CompactUsageCardPresentation.tone(metric)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .testTag("compact_window_row_${metric.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = compactPeriodLabel(metric),
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
        )
        UsageSegmentMeter(
            percent = percent,
            tone = tone,
            modifier = Modifier.weight(1f),
            height = 9.dp,
        )
        Text(
            text = formatPercent(percent),
            style = MyTokenType.caption.copy(fontWeight = FontWeight.SemiBold),
            color = when (tone) {
                UsageMetricTone.Warning -> colors.warningText
                UsageMetricTone.Critical -> colors.criticalText
                UsageMetricTone.Normal -> colors.ink
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactBalanceLayout(
    metric: UsageMetric,
    granted: BigDecimal?,
) {
    val colors = MyTokenThemeTokens.colors
    val amount = splitBalanceAmount(formatCurrency(metric.value, metric.currencyCode))
    val markSpan = SpanStyle(
        fontSize = 0.6.em,
        fontWeight = FontWeight(650),
        color = colors.muted,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
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
            modifier = Modifier.weight(1f),
            style = MyTokenType.displayCompact,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (granted != null && granted.signum() > 0) {
            Text(
                text = "含赠金 ${formatCurrency(granted, metric.currencyCode)}",
                modifier = Modifier
                    .padding(start = 6.dp, bottom = 2.dp)
                    .testTag("compact_balance_granted"),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
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
