package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.usage.RefreshStatus
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.math.BigDecimal
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

/** 方案 A：石墨蓝承载卡片，品牌蓝表达正常用量，彩色只表达状态。 */
internal object CardPalette {
    val darkSurface = MyTokenPalette.darkSurface
    val darkSurfaceBorder = Color(0x1FFFFFFF)
    val coral = MyTokenPalette.critical
}

internal data class StatusColors(
    val brand: Color,
    val positive: Color,
    val warning: Color,
    val critical: Color,
    val neutral: Color,
) {
    companion object {
        val lightPalette = StatusColors(MyTokenPalette.brandLight, MyTokenPalette.positiveLight, MyTokenPalette.warning, MyTokenPalette.critical, Color(0xFF6F7782))
        val darkPalette = StatusColors(MyTokenPalette.brandDark, MyTokenPalette.positiveDark, MyTokenPalette.warning, MyTokenPalette.critical, MyTokenPalette.darkMutedText)
        fun forTheme(isDark: Boolean) = if (isDark) darkPalette else lightPalette
    }
}

@Composable
internal fun statusColors(): StatusColors =
    StatusColors.forTheme(MaterialTheme.colorScheme.background.luminance() < 0.5f)

internal fun statusLabel(card: CredentialCardUi): String? = when (card.status) {
    RefreshStatus.Loading -> "正在加载"
    RefreshStatus.Disabled -> "已停用"
    RefreshStatus.Failed -> "更新失败"
    RefreshStatus.Ready -> if (card.isStale) "待刷新" else null
}

private enum class CardHealth { Normal, Warning, Critical, Refreshing, Stale, Failed, Disabled }

private fun cardHealth(card: CredentialCardUi): CardHealth {
    if (card.status == RefreshStatus.Disabled) return CardHealth.Disabled
    if (card.status == RefreshStatus.Failed || card.error != null) return CardHealth.Failed
    if (card.status == RefreshStatus.Loading) return CardHealth.Refreshing
    if (card.isStale) return CardHealth.Stale
    val states = card.snapshot?.metrics.orEmpty().map { it.healthState }
    return when {
        states.any { it == UsageMetricHealthState.Critical || it == UsageMetricHealthState.Unavailable } -> CardHealth.Critical
        states.any { it == UsageMetricHealthState.Warning } -> CardHealth.Warning
        else -> CardHealth.Normal
    }
}

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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }

    val locateLift = remember { Animatable(0f) }
    LaunchedEffect(highlightSignal) {
        if (highlightSignal <= 0) {
            locateLift.snapTo(0f)
            return@LaunchedEffect
        }
        locateLift.snapTo(0f)
        locateLift.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 860
                0f at 0 using FastOutSlowInEasing
                1f at 130 using FastOutSlowInEasing
                0.18f at 270 using FastOutSlowInEasing
                0.7f at 390 using FastOutSlowInEasing
                0.08f at 530 using FastOutSlowInEasing
                0.32f at 640 using FastOutSlowInEasing
                0f at 820 using FastOutSlowInEasing
            },
        )
    }
    val locateBounceModifier = if (highlightSignal > 0) {
        Modifier.graphicsLayer {
            val lift = locateLift.value
            scaleX = 1f + 0.025f * lift
            scaleY = 1f + 0.025f * lift
            translationY = -7.dp.toPx() * lift
        }
    } else {
        Modifier
    }

    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .then(locateBounceModifier)
            .testTag("credential_card_${card.credential.id}"),
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CredentialCardHeader(
                card = card,
                isDark = isDark,
                onShowFailure = { showsFailureDetails = true },
                onRefresh = onRefresh,
            )

            if (usageCardDensity == UsageCardDensity.FULL) {
                FullUsageCardBody(card = card)
            } else {
                CompactUsageCardBody(card = card, isDark = isDark)
            }
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
private fun CredentialCardHeader(
    card: CredentialCardUi,
    isDark: Boolean,
    onShowFailure: () -> Unit,
    onRefresh: () -> Unit,
) {
    val subscriptionExpiry = card.snapshot?.subscriptionEndAt
    val lifecycleText = subscriptionExpiry?.let { expiry ->
        val lifecyclePrefix =
            if (card.credential.providerId == ProviderId.OpenCode) {
                OpenCodeCardPolicy.lifecyclePrefix(card.snapshot?.billingMode)
            } else {
                "到期"
            }
        "$lifecyclePrefix ${formatSubscriptionTime(expiry)}"
    }
    val planMetaText = listOfNotNull(
        card.snapshot?.planName?.trim()?.takeIf { it.isNotEmpty() },
        lifecycleText,
    ).joinToString(" · ").takeIf { it.isNotEmpty() }
    val health = cardHealth(card)
    val statusColor = when (health) {
        CardHealth.Critical -> MyTokenPalette.critical
        CardHealth.Warning -> MyTokenPalette.warning
        CardHealth.Refreshing -> MaterialTheme.colorScheme.primary
        CardHealth.Stale -> MaterialTheme.colorScheme.onSurfaceVariant
        CardHealth.Failed -> MyTokenPalette.critical
        CardHealth.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant
        CardHealth.Normal -> MyTokenPalette.positiveLight
    }
    val statusText = when (health) {
        CardHealth.Critical -> "已达上限"
        CardHealth.Warning -> "接近上限"
        CardHealth.Refreshing -> "刷新中"
        CardHealth.Stale -> "待刷新"
        CardHealth.Failed -> "更新失败"
        CardHealth.Disabled -> "已停用"
        CardHealth.Normal -> "正常"
    }

    val aliasSize = when {
        card.credential.name.length > 24 -> 13.sp
        card.credential.name.length > 16 -> 14.sp
        else -> 16.sp
    }
    val aliasLineHeight = when {
        card.credential.name.length > 24 -> 17.sp
        card.credential.name.length > 16 -> 18.sp
        else -> 20.sp
    }

    Row(verticalAlignment = Alignment.Top) {
        AvatarWithFailureBadge(card = card, onClick = onShowFailure)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 0.dp, end = 4.dp),
        ) {
            Text(
                text = card.credential.name,
                fontSize = aliasSize,
                lineHeight = aliasLineHeight,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (planMetaText != null) {
                Text(
                    text = planMetaText,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MyTokenType.cardMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 6.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusPill(text = statusText, color = statusColor, isDark = isDark)
                CircleIconButton(
                    tag = "credential_refresh_${card.credential.id}",
                    description = "刷新 ${card.credential.name}",
                    enabled = card.status != RefreshStatus.Loading && card.credential.isEnabled,
                    isDark = isDark,
                    onClick = onRefresh,
                ) {
                    if (card.status == RefreshStatus.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.8.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarWithFailureBadge(
    card: CredentialCardUi,
    onClick: () -> Unit,
) {
    val avatarShape = RoundedCornerShape(MyTokenVisualPolicy.innerRadiusDp)
    val failed = card.status == RefreshStatus.Failed

    // 44dp 触摸区承载 30dp 头像，失败时整块点击弹失败详情，不再落入卡片详情。
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (failed) {
                    Modifier
                        .testTag("credential_failure_${card.credential.id}")
                        .semantics { contentDescription = "查看刷新失败详情" }
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProviderLogoPlate(
                painter = painterResource(ProviderCatalog.logoRes(card.credential.providerId)),
                contentDescription = null,
                size = 30.dp,
                shape = avatarShape,
                contentPadding = 3.dp,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .testTag("credential_avatar_${card.credential.id}"),
            )
            if (failed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .size(13.dp)
                        .background(CardPalette.coral, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    tag: String,
    description: String,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.04f),
                CircleShape,
            )
            .testTag(tag)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun StatusPill(text: String, color: Color, isDark: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color,
        modifier = Modifier
            .padding(start = 6.dp)
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.30f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun CompactUsageCardBody(card: CredentialCardUi, isDark: Boolean) {
    val metrics = UsageCardCompactSpec.orderedMetrics(
        providerId = card.credential.providerId,
        metadata = card.credential.metadata,
        metrics = card.snapshot?.metrics.orEmpty(),
    )

    if (metrics.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val arrangement = CompactUsageCardPresentation.arrangement(forMetrics = metrics)
    when (arrangement) {
        CompactUsageArrangement.Balance -> CompactBalanceLayout(
            metric = metrics.first(),
            composition = CompactBalanceCompositionPolicy.segments(
                card.snapshot?.metrics.orEmpty()
            ),
            isDark = isDark,
        )
        CompactUsageArrangement.HorizontalRings,
        CompactUsageArrangement.VerticalRings -> CompactRingMetrics(
            metrics = metrics,
            isDark = isDark,
        )
        CompactUsageArrangement.ValueTiles -> CompactValueGrid(metrics = metrics)
    }
}

@Composable
private fun CompactRingMetrics(
    metrics: List<UsageMetric>,
    isDark: Boolean,
) {
    if (metrics.size == 1) {
        val metric = metrics.first()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_ring_tile_${metric.id}"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactArcGauge(
                percent = progressPercent(metric),
                tone = CompactUsageCardPresentation.tone(metric),
                isDark = isDark,
                diameter = 56.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CompactPeriodRow(metric = metric, dense = false, centered = false)
                CompactRingResetText(metric = metric, dense = false, centered = false)
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        metrics.forEachIndexed { index, metric ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(88.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            CompactRingTile(
                metric = metric,
                isDark = isDark,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompactRingTile(
    metric: UsageMetric,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag("compact_ring_tile_${metric.id}"),
    ) {
        val dense = maxWidth < 100.dp || LocalDensity.current.fontScale > 1.2f
        val gaugeDiameter = if (dense) 46.dp else 54.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (dense) 3.dp else 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompactArcGauge(
                percent = progressPercent(metric),
                tone = CompactUsageCardPresentation.tone(metric),
                isDark = isDark,
                diameter = gaugeDiameter,
            )
            Spacer(modifier = Modifier.height(5.dp))
            CompactPeriodRow(metric = metric, dense = dense, centered = true)
            CompactRingResetText(metric = metric, dense = dense, centered = true)
        }
    }
}

@Composable
private fun CompactRingResetText(
    metric: UsageMetric,
    dense: Boolean,
    centered: Boolean,
) {
    Text(
        text = ringSubtitle(metric),
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
        fontSize = if (dense) 10.sp else 10.5.sp,
        lineHeight = if (dense) 13.sp else 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CompactPeriodRow(
    metric: UsageMetric,
    dense: Boolean,
    centered: Boolean,
) {
    Row(
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) {
            Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(4.dp)
        },
    ) {
        Text(
            text = compactPeriodLabel(metric),
            fontSize = if (dense) 11.sp else 11.5.sp,
            lineHeight = if (dense) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (metric.resetCards.isNotEmpty()) {
            ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Compact)
        }
    }
}

@Composable
private fun CompactBalanceLayout(
    metric: UsageMetric,
    composition: List<CompactBalanceSegment>,
    isDark: Boolean,
) {
    val currency = metric.currencyCode?.trim()?.uppercase().orEmpty()
    val title = if (currency.isEmpty()) "账户余额" else "账户余额 · $currency"
    val compositionTotal = composition.fold(BigDecimal.ZERO) { total, segment ->
        total + segment.value
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
    ) {
        Text(
            text = title,
            style = MyTokenType.metricLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatCurrency(metric.value, metric.currencyCode),
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 34.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.35).sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (composition.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isDark) 0.09f else 0.07f,
                        ),
                    ),
            ) {
                if (compositionTotal.signum() > 0) {
                    composition.forEach { segment ->
                        val weight = segment.value.toDouble() / compositionTotal.toDouble()
                        if (weight > 0.0) {
                            Box(
                                modifier = Modifier
                                    .weight(weight.toFloat())
                                    .height(5.dp)
                                    .background(compactBalanceTone(segment)),
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
    }
}

@Composable
private fun compactBalanceTone(segment: CompactBalanceSegment): Color {
    val colors = statusColors()
    return when (segment.kind) {
        CompactBalanceSegmentKind.ToppedUp -> colors.positive
        CompactBalanceSegmentKind.Granted -> colors.brand
    }
}

@Composable
private fun CompactBalanceLegend(
    segment: CompactBalanceSegment,
    currencyCode: String?,
) {
    val colors = statusColors()
    val tint = when (segment.kind) {
        CompactBalanceSegmentKind.ToppedUp -> colors.positive
        CompactBalanceSegmentKind.Granted -> colors.brand
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
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(top = 3.dp),
    ) {
        metrics.chunked(2).forEachIndexed { rowIndex, rowMetrics ->
            if (rowIndex > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowMetrics.forEachIndexed { columnIndex, metric ->
                    if (columnIndex > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    CompactValueCell(
                        metric = metric,
                        secondColumn = columnIndex > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactValueCell(
    metric: UsageMetric,
    secondColumn: Boolean,
    modifier: Modifier = Modifier,
) {
    val value = compactValueParts(metric)
    Column(
        modifier = modifier.padding(
            start = if (secondColumn) 12.dp else 0.dp,
            end = if (secondColumn) 0.dp else 10.dp,
            top = 9.dp,
            bottom = 10.dp,
        ),
    ) {
        Text(
            text = metric.label.trim(),
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                fontSize = 19.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.55).sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            value.suffix?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
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
        CompactValueParts(
            if (metric.healthState == UsageMetricHealthState.Unavailable) "不可用" else "可用",
        )
    metric.unit == UsageMetricUnit.Token -> splitCompactValue(formatCompact(metric.value))
    metric.unit == UsageMetricUnit.Currency ->
        CompactValueParts(formatCurrency(metric.value, metric.currencyCode))
    metric.unit == UsageMetricUnit.Request ->
        CompactValueParts(formatCompact(metric.value), "次")
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
private fun FullUsageCardBody(
    card: CredentialCardUi,
) {
    val rawMetrics = card.snapshot?.metrics.orEmpty()
    if (rawMetrics.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        FullCardFooter(card)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        FullSplitUsageContent(card = card, metrics = rawMetrics)
        FullCardFooter(card)
    }
}

@Composable
private fun FullCardFooter(card: CredentialCardUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = card.freshness.text,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "详情",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun compactPeriodLabel(metric: UsageMetric): String {
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
