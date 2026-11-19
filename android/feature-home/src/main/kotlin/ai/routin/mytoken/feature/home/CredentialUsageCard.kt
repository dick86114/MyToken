package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.usage.RefreshStatus
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    val refreshModifier = Modifier.refreshPulseBorder(
        isLoading = card.status == RefreshStatus.Loading,
        color = MaterialTheme.colorScheme.primary,
    )
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
    val locateBounceModifier = Modifier.graphicsLayer {
        val lift = locateLift.value
        scaleX = 1f + 0.025f * lift
        scaleY = 1f + 0.025f * lift
        translationY = -7.dp.toPx() * lift
    }

    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .then(refreshModifier)
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
                FullUsageCardBody(card = card, isDark = isDark)
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

/** 仅在刷新中绘制一次与卡片同圆角的呼吸描边，避免沿线动画的错位和高开销。 */
@Composable
private fun Modifier.refreshPulseBorder(isLoading: Boolean, color: Color): Modifier {
    if (!isLoading) return this
    val pulse = rememberInfiniteTransition(label = "refreshPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "refreshPulsePhase",
    )
    return drawWithContent {
        drawContent()
        val stroke = 1.5.dp.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            color = color.copy(alpha = 0.16f + 0.46f * pulse.value),
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(
                (MyTokenVisualPolicy.outerRadiusDp.toPx() - inset).coerceAtLeast(0f),
                (MyTokenVisualPolicy.outerRadiusDp.toPx() - inset).coerceAtLeast(0f),
            ),
            style = Stroke(width = stroke),
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
        card.credential.name.length > 24 -> 12.sp
        card.credential.name.length > 16 -> 14.sp
        else -> 15.sp
    }
    val aliasLineHeight = when {
        card.credential.name.length > 24 -> 15.sp
        card.credential.name.length > 16 -> 17.sp
        else -> 18.sp
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
            if (subscriptionExpiry != null) {
                val lifecyclePrefix =
                    if (card.credential.providerId == ai.routin.mytoken.domain.model.ProviderId.OpenCode) {
                        OpenCodeCardPolicy.lifecyclePrefix(card.snapshot?.billingMode)
                    } else {
                        "到期"
                    }
                Text(
                    text = "$lifecyclePrefix ${formatSubscriptionTime(subscriptionExpiry)}",
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
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
        CompactUsageArrangement.Balance -> BalanceStrip(
            metric = metrics.first(),
            composition = CompactBalanceCompositionPolicy.segments(
                card.snapshot?.metrics.orEmpty()
            ),
            isDark = isDark,
            compact = true,
        )
        CompactUsageArrangement.HorizontalRings,
        CompactUsageArrangement.VerticalRings -> Row(
            horizontalArrangement = Arrangement.spacedBy(if (metrics.size >= 3) 5.dp else 8.dp),
        ) {
            metrics.forEach { metric ->
                CompactRingTile(
                    metric = metric,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CompactUsageArrangement.ValueTiles -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.chunked(2).forEach { rowMetrics ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMetrics.forEach { metric ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .usageTileGlass(isDark),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                CompactMetricRow(metric = metric)
                            }
                        }
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
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
            .usageTileGlass(isDark)
            .testTag("compact_ring_tile_${metric.id}"),
    ) {
        val dense = maxWidth < 140.dp || LocalDensity.current.fontScale > 1.2f
        val ringDiameter = if (dense) 30.dp else 36.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (dense) 6.dp else 10.dp,
                    vertical = 9.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 9.dp),
        ) {
            UsageRing(
                percent = progressPercent(metric),
                tone = CompactUsageCardPresentation.tone(metric),
                isDark = isDark,
                diameter = ringDiameter,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CompactPeriodRow(metric = metric, dense = dense)
                Text(
                    text = ringSubtitle(metric),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = if (dense) 8.sp else 9.sp,
                    lineHeight = if (dense) 10.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactPeriodRow(metric: UsageMetric, dense: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (dense) 3.dp else 5.dp),
    ) {
        Text(
            text = compactPeriodLabel(metric),
            fontSize = if (dense) 11.sp else 13.sp,
            lineHeight = if (dense) 14.sp else 16.sp,
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
private fun FullUsageCardBody(
    card: CredentialCardUi,
    isDark: Boolean,
) {
    val metrics = fullCardMetrics(card)
    if (metrics.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        FullCardFooter(card)
        return
    }

    val balanceMetrics = metrics.filter { it.presentation == UsageMetricPresentation.Balance }
    val progress = metrics.filter { it.presentation == UsageMetricPresentation.Progress }
    val values = metrics.filter {
        it.presentation == UsageMetricPresentation.Value ||
            it.presentation == UsageMetricPresentation.Status
    }
    val isDeepSeek = card.credential.providerId ==
        ai.routin.mytoken.domain.model.ProviderId.DeepSeek
    val rawMetrics = card.snapshot?.metrics.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (isDeepSeek) {
            // 与 macOS DeepSeekMetricsView 对齐：按固定字段列出接口返回的全部内容，
            // 零值也展示，缺失显示占位符而不是直接隐藏。
            DeepSeekMetricsGrid(
                metrics = rawMetrics,
                allowedModels = card.snapshot?.allowedModels.orEmpty(),
                isDark = isDark,
            )
        } else if (balanceMetrics.isNotEmpty()) {
            val balance = balanceMetrics.first()
            BalanceStrip(
                metric = balance,
                composition = CompactBalanceCompositionPolicy.segments(rawMetrics),
                isDark = isDark,
            )
        }
        if (progress.isNotEmpty()) {
            FullProgressMetrics(progress)
        }
        if (!isDeepSeek && values.isNotEmpty()) {
            FullValueMetrics(values)
        }
        FullCardFooter(card)
    }
}

/** 账户可用状态行，展示接口返回的 is_available。 */
@Composable
private fun FullAccountStatusRow(metric: UsageMetric) {
    val isAvailable = (metric.value?.signum() ?: 0) > 0
    val tint = if (isAvailable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            text = if (isAvailable) "账户可用" else "账户不可用",
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
        Text(
            text = "· 余额不足时自动提醒",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * DeepSeek 完整模式：参照 MiMo 的字段网格样式（两列卡片格子），
 * 把接口返回的全部字段固定列出，零值也展示，缺失显示占位符。
 */
@Composable
private fun DeepSeekMetricsGrid(
    metrics: List<UsageMetric>,
    allowedModels: List<String>,
    isDark: Boolean,
) {
    val balances = metrics.filter { it.presentation == UsageMetricPresentation.Balance }
    if (balances.isEmpty()) {
        Text(
            text = "暂无用量数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        return
    }

    val isMultiCurrency = balances.size > 1
    val availability = metrics.firstOrNull { it.id == "availability" }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        balances.forEach { balance ->
            val currency = balance.currencyCode?.uppercase().orEmpty()
            val suffix = if (isMultiCurrency && currency.isNotEmpty()) "-$currency" else ""
            if (isMultiCurrency && currency.isNotEmpty()) {
                Text(
                    text = currency,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val cells = listOfNotNull(
                "余额" to fullValueText(balance),
                "充值" to fullValueTextOf(
                    metrics.firstOrNull { it.id == "toppedUpBalance$suffix" }
                        ?: metrics.firstOrNull { it.id == "toppedUpBalance" }
                ),
                "赠金" to fullValueTextOf(
                    metrics.firstOrNull { it.id == "grantedBalance$suffix" }
                        ?: metrics.firstOrNull { it.id == "grantedBalance" }
                ),
                "账户状态" to if (availability == null) {
                    "—"
                } else if ((availability.value?.signum() ?: 0) > 0) {
                    "可用"
                } else {
                    "不可用"
                },
                "可用模型" to if (allowedModels.isEmpty()) {
                    "—"
                } else {
                    "${allowedModels.size} 个"
                }
            )
            DeepSeekMetricCells(cells)
        }
    }
}

/** 与 MiMo 的 FullValueMetrics 一致的卡片格子布局。 */
@Composable
private fun DeepSeekMetricCells(cells: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        cells.chunked(2).forEach { rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCells.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(2 - rowCells.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 与 fullValueText 一致的取值，但允许 null 显示占位符。 */
private fun fullValueTextOf(metric: UsageMetric?): String =
    metric?.let(::fullValueText) ?: "—"


private fun fullCardMetrics(card: CredentialCardUi): List<UsageMetric> {
    val all = card.snapshot?.metrics.orEmpty()
    val base = UsageCardCompactSpec.orderedMetrics(
        providerId = card.credential.providerId,
        metadata = card.credential.metadata,
        metrics = all,
    )
    val extras = when (card.credential.providerId) {
        ai.routin.mytoken.domain.model.ProviderId.CommandCode ->
            listOfNotNull(all.firstOrNull { it.id == "request-count" })
        ai.routin.mytoken.domain.model.ProviderId.NewAPI ->
            listOfNotNull(all.firstOrNull { it.id == "quota-progress" })
        ai.routin.mytoken.domain.model.ProviderId.Xiaomi ->
            if (card.credential.metadata[ai.routin.mytoken.domain.model.CredentialMetadataKey.UsageKind] == "plan") {
                emptyList()
            } else {
                all.filter {
                    it.id in setOf(
                        "total-consumption",
                        "cash-balance",
                        "gift-balance",
                        "total-tokens",
                    )
                }.take(4)
            }
        else -> emptyList()
    }
    val baseIds = base.mapTo(mutableSetOf()) { it.id }
    // 多币种账户可能有多条 balance 指标，需要全部保留，不能只取第一条。
    val missingBalances = all.filter {
        it.presentation == UsageMetricPresentation.Balance && it.id !in baseIds
    }
    val extrasIds = extras.mapTo(mutableSetOf()) { it.id }
    val missingValues = all
        .filter {
            it.id !in baseIds &&
                it.id !in extrasIds &&
                (it.presentation == UsageMetricPresentation.Value || it.presentation == UsageMetricPresentation.Status) &&
                it.value?.signum() == 1
        }
        .take(3)
    return (base + extras + missingBalances + missingValues).distinctBy { it.id }
}

@Composable
private fun FullProgressMetrics(metrics: List<UsageMetric>) {
    if (metrics.size == 1) {
        FullProgressMetricCell(metric = metrics.first(), modifier = Modifier.fillMaxWidth())
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            metrics.forEach { metric ->
                FullProgressMetricCell(
                    metric = metric,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FullProgressMetricCell(
    metric: UsageMetric,
    modifier: Modifier = Modifier,
) {
    val percent = progressPercent(metric)
    val colors = statusColors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = metricLabel(metric),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatPercent(percent),
            fontSize = 21.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        UsageProgressBar(
            percent = percent,
            color = progressColor(percent, colors),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "已用 ${formatMetricAmount(metric.used, metric)}",
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "剩余 ${formatMetricAmount(metric.remaining, metric)}",
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        metric.windowEnd?.let { end ->
            Text(
                text = "重置 ${formatResetTime(end)} · ${formatRemainingDuration(end, java.time.Instant.now())}",
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (metric.resetCards.isNotEmpty()) {
            ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Detail)
        }
    }
}

@Composable
private fun FullValueMetrics(metrics: List<UsageMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowMetrics.forEach { metric ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = metric.label,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = fullValueText(metric),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(2 - rowMetrics.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "详情",
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun metricLabel(metric: UsageMetric): String = when (metric.label) {
    "5 小时" -> "5 小时窗口"
    "本周", "周" -> "本周用量"
    "月" -> "本月用量"
    else -> metric.label
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

private fun formatMetricAmount(value: java.math.BigDecimal?, metric: UsageMetric): String = when {
    value == null -> "-"
    metric.unit == UsageMetricUnit.Currency -> formatCurrency(value, metric.currencyCode)
    metric.unit == UsageMetricUnit.Request -> "${formatGrouped(value)} 次"
    else -> formatGrouped(value)
}

private fun fullValueText(metric: UsageMetric): String = when (metric.presentation) {
    UsageMetricPresentation.Status ->
        if (metric.healthState == UsageMetricHealthState.Unavailable) "不可用" else "可用"
    UsageMetricPresentation.Balance ->
        formatCurrency(metric.value, metric.currencyCode)
    UsageMetricPresentation.Value ->
        if (metric.unit == UsageMetricUnit.Request) {
            "${formatCompact(metric.value)} 次"
        } else if (metric.unit == UsageMetricUnit.Currency) {
            formatCurrency(metric.value, metric.currencyCode)
        } else {
            formatGrouped(metric.value)
        }
    UsageMetricPresentation.Progress -> formatPercent(progressPercent(metric))
}

/** 简洁用量小卡片只保留一层无色透明玻璃，状态色仍只出现在圆环或进度条。 */
private fun Modifier.usageTileGlass(isDark: Boolean): Modifier {
    val shape = RoundedCornerShape(MyTokenVisualPolicy.innerRadiusDp)
    val fillTopAlpha = if (isDark) 0.11f else 0.56f
    val fillBottomAlpha = if (isDark) 0.05f else 0.28f
    val fill = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = fillTopAlpha),
            Color.White.copy(alpha = fillBottomAlpha),
        )
    )
    return this
        .clip(shape)
        .background(fill, shape)
}

@Composable
private fun BalanceStrip(
    metric: UsageMetric,
    composition: List<CompactBalanceSegment>,
    isDark: Boolean,
    compact: Boolean = false,
) {
    val colors = statusColors()
    val statusColor = statusColor(metric, null, colors)
    val resetText = metric.windowEnd?.let { "重置 ${formatResetTime(it)}" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .usageTileGlass(isDark),
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (metric.id == "balance" || metric.id.startsWith("balance-")) {
                            "账户余额"
                        } else {
                            metric.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(metric.value, metric.currencyCode),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor(metric, null, colors),
                    )
                }
                if (composition.isNotEmpty() || resetText != null) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        composition.forEach { segment ->
                            BalanceCompositionItem(
                                segment = segment,
                                currencyCode = metric.currencyCode,
                                compact = true,
                            )
                        }
                        if (resetText != null) {
                            StatusPill(text = resetText, color = statusColor, isDark = isDark)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (metric.id == "balance" || metric.id.startsWith("balance-")) {
                            "账户余额"
                        } else {
                            metric.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCurrency(metric.value, metric.currencyCode),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor(metric, null, colors),
                    )
                    if (composition.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            composition.forEachIndexed { index, segment ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(2.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                                    .copy(alpha = 0.35f),
                                                CircleShape,
                                            )
                                    )
                                }
                                BalanceCompositionItem(
                                    segment = segment,
                                    currencyCode = metric.currencyCode,
                                )
                            }
                        }
                    }
                }
                if (resetText != null) {
                    StatusPill(text = resetText, color = statusColor, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun BalanceCompositionItem(
    segment: CompactBalanceSegment,
    currencyCode: String?,
    compact: Boolean = false,
) {
    val colors = statusColors()
    val tint = when (segment.kind) {
        CompactBalanceSegmentKind.ToppedUp -> colors.positive
        CompactBalanceSegmentKind.Granted -> colors.brand
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(tint, CircleShape)
        )
        Text(
            text = if (compact) {
                when (segment.kind) {
                    CompactBalanceSegmentKind.ToppedUp -> "充值余额"
                    CompactBalanceSegmentKind.Granted -> "赠送余额"
                }
            } else {
                segment.label
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
        )
        Text(
            text = formatCurrency(segment.value, currencyCode),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
        )
    }
}

private fun ringSubtitle(metric: UsageMetric): String {
    val end = metric.windowEnd ?: return if ((progressPercent(metric) ?: 0.0) <= 0.0) "待命中" else "--"
    return formatResetTime(end)
}

@Composable
private fun CompactMetricRow(metric: UsageMetric) {
    val colors = statusColors()
    val percent = progressPercent(metric)
    val isProgress = metric.presentation == UsageMetricPresentation.Progress

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = compactPeriodLabel(metric),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (isProgress && percent != null) {
                Text(
                    text = formatPercent(percent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (percent <= 0.0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    },
                )
            }
        }

        if (isProgress) {
            metric.windowEnd?.let { end ->
                Text(
                    text = "重置 ${formatResetTime(end)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = compactValueText(metric),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor(metric, percent, colors),
            )
        }
    }
}

private fun compactValueText(metric: UsageMetric): String = when {
    metric.presentation == UsageMetricPresentation.Balance ->
        formatCurrency(metric.value, metric.currencyCode)
    metric.presentation == UsageMetricPresentation.Status ->
        if (metric.healthState == UsageMetricHealthState.Unavailable) "不可用" else "可用"
    metric.unit == UsageMetricUnit.Token -> formatGrouped(metric.value)
    else -> formatDecimal(metric.value)
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
            GlassButton(
                text = "关闭",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = GlassButtonTone.Neutral,
            )
            GlassButton(
                text = "重试",
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                tone = GlassButtonTone.Primary,
            )
        },
    )
}
