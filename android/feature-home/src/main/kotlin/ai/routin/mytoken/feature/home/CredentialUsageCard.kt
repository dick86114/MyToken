package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.usage.RefreshStatus
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag

/** 深色令牌对齐 simple-ui/dark：面板 #121722、emerald 主色、珊瑚红失败徽章。 */
internal object CardPalette {
    val darkSurface = Color(0xE6121722)
    val darkSurfaceBorder = Color(0x1AFFFFFF)
    val emeraldDark = Color(0xFF10F49C)
    val emeraldLight = Color(0xFF059669)
    val coral = Color(0xFFFF4557)

    fun emerald(isDark: Boolean) = if (isDark) emeraldDark else emeraldLight
}

internal data class StatusColors(
    val normal: Color,
    val warning: Color,
    val critical: Color,
    val neutral: Color,
) {
    companion object {
        val lightPalette = StatusColors(Color(0xFF24A148), Color(0xFFD97706), Color(0xFFDA1E28), Color(0xFF8D8D8D))
        val darkPalette = StatusColors(Color(0xFF42BE65), Color(0xFFF1C21B), Color(0xFFFA4D56), Color(0xFFA8A8A8))
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
    RefreshStatus.Ready -> if (card.isStale) "显示上次成功数据" else null
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
) {
    val accent = ProviderCatalog.accentColor(card.credential.providerId)
    val providerName = ProviderCatalog.displayName(card.credential.providerId)
    val plan = card.snapshot?.planName.orEmpty()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }
    var showsShareDialog by remember(card.credential.id) { mutableStateOf(false) }

    val cometModifier = Modifier.refreshCometBorder(
        isLoading = card.status == RefreshStatus.Loading,
        color = accent,
    )

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().then(cometModifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) CardPalette.darkSurface else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) CardPalette.darkSurfaceBorder else accent.copy(alpha = 0.22f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CredentialCardHeader(
                card = card,
                providerName = providerName,
                plan = plan,
                isDark = isDark,
                onShowFailure = { showsFailureDetails = true },
                onShare = { showsShareDialog = true },
                onRefresh = onRefresh,
            )

            val metrics = card.snapshot?.metrics.orEmpty()
            if (usageCardDensity == UsageCardDensity.FULL) {
                val start = card.snapshot?.subscriptionStartAt
                val end = card.snapshot?.subscriptionEndAt
                if (start != null || end != null) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "开始 ${formatSubscriptionTime(start)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "结束 ${formatSubscriptionTime(end)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            if (usageCardDensity == UsageCardDensity.FULL) {
                when (card.credential.providerId) {
                    ai.routin.mytoken.domain.model.ProviderId.Glm -> GLMMetrics(metrics, columns = metricColumns)
                    ai.routin.mytoken.domain.model.ProviderId.NewAPI -> NewAPIMetrics(metrics, columns = metricColumns)
                    ai.routin.mytoken.domain.model.ProviderId.Volcengine -> VolcengineMetrics(metrics, columns = metricColumns)
                    ai.routin.mytoken.domain.model.ProviderId.CommandCode -> CommandCodeMetrics(
                        metrics = metrics,
                        columns = metricColumns,
                        showsBalanceBreakdown = false,
                    )
                    ai.routin.mytoken.domain.model.ProviderId.Xiaomi ->
                        if (card.credential.metadata[ai.routin.mytoken.domain.model.CredentialMetadataKey.UsageKind] == "plan") {
                            UsageMetricGrid(metrics = metrics, columns = metricColumns)
                        } else {
                            XiaomiAPIMetrics(metrics = metrics)
                        }
                    else -> UsageMetricGrid(metrics = metrics, columns = metricColumns)
                }
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
    if (showsShareDialog) {
        UsageShareDialog(
            card = card,
            onDismiss = { showsShareDialog = false },
            layoutMode = layoutMode,
        )
    }
}

/** 仅在刷新中创建无限动画，非 Loading 卡片不再常驻空转动画。 */
@Composable
private fun Modifier.refreshCometBorder(isLoading: Boolean, color: Color): Modifier {
    if (!isLoading) return this
    val phase by rememberInfiniteTransition(label = "comet").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "cometPhase",
    )
    return drawWithContent {
        drawContent()
        drawCometBorder(cornerRadiusPx = 14.dp.toPx(), phase = phase, color = color)
    }
}

/** 刷新中：发光亮点沿卡片边框匀速环绕，身后拖出渐隐尾巴（流星）。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCometBorder(
    cornerRadiusPx: Float,
    phase: Float,
    color: Color,
) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        )
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    if (length <= 0f) return

    val head = length * phase
    val tail = length * 0.22f
    val segments = 4
    val segmentLength = tail / segments
    for (i in segments downTo 1) {
        val end = head - segmentLength * (i - 1)
        val start = end - segmentLength
        if (end <= 0f) continue
        val fade = 1f - (i - 1) / segments.toFloat()
        val stroke = Path()
        if (start < 0f) {
            measure.getSegment(length + start, length, stroke, true)
            val wrapped = Path()
            measure.getSegment(0f, end, wrapped, true)
            stroke.addPath(wrapped)
        } else {
            measure.getSegment(start, end, stroke, true)
        }
        drawPath(
            stroke,
            color = color.copy(alpha = 0.9f * fade),
            style = Stroke(width = (2.4f * fade + 0.4f).dp.toPx(), cap = StrokeCap.Round),
        )
    }
    val position = measure.getPosition(head)
    drawCircle(color = color.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = position)
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = position)
}

@Composable
private fun CredentialCardHeader(
    card: CredentialCardUi,
    providerName: String,
    plan: String,
    isDark: Boolean,
    onShowFailure: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
) {
    val metrics = card.snapshot?.metrics.orEmpty()
    val nearlyExhausted = metrics.any { metric ->
        val percent = progressPercent(metric)
        (percent != null && percent >= 80.0) || metric.healthState == UsageMetricHealthState.Critical
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarWithFailureBadge(card = card, isDark = isDark, onClick = onShowFailure)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.credential.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 96.dp),
                )
                if (nearlyExhausted) {
                    StatusPill(text = "即将耗尽", color = statusColors().critical, isDark = isDark)
                }
            }
            Text(
                text = if (plan.isEmpty()) providerName else "$providerName · $plan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }

        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                tag = "credential_share_${card.credential.id}",
                description = "分享 ${card.credential.name}",
                enabled = card.snapshot != null && card.credential.isEnabled,
                isDark = isDark,
                onClick = onShare,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            CircleIconButton(
                tag = "credential_refresh_${card.credential.id}",
                description = "刷新 ${card.credential.name}",
                enabled = card.status != RefreshStatus.Loading && card.credential.isEnabled,
                isDark = isDark,
                onClick = onRefresh,
            ) {
                if (card.status == RefreshStatus.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarWithFailureBadge(
    card: CredentialCardUi,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val accent = ProviderCatalog.accentColor(card.credential.providerId)
    val letter = card.credential.name.trim().firstOrNull()?.uppercase() ?: "M"
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
            modifier = Modifier
                .size(30.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
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
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.04f),
                CircleShape,
            )
            .border(1.dp, borderColor, CircleShape)
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
        color = CardPalette.emerald(isDark).takeIf { color == statusColors().normal } ?: color,
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
            isDark = isDark,
        )
        CompactUsageArrangement.HorizontalRings -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.forEach { metric ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .ringTileBackground(isDark, tone = CompactUsageCardPresentation.tone(metric)),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UsageRing(
                            percent = progressPercent(metric),
                            tone = CompactUsageCardPresentation.tone(metric),
                            isDark = isDark,
                            diameter = 44.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = metric.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = ringSubtitle(metric, vertical = false),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        CompactUsageArrangement.VerticalRings -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { metric ->
                val tone = CompactUsageCardPresentation.tone(metric)
                val toneColor = when (tone) {
                    UsageMetricTone.Warning -> statusColors().warning
                    UsageMetricTone.Critical -> statusColors().critical
                    UsageMetricTone.Normal -> MaterialTheme.colorScheme.onSurface
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .ringTileBackground(isDark, tone = tone),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        UsageRing(
                            percent = progressPercent(metric),
                            tone = tone,
                            isDark = isDark,
                            diameter = 40.dp,
                        )
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (tone == UsageMetricTone.Normal) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                toneColor
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ringSubtitle(metric, vertical = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tone == UsageMetricTone.Normal) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                toneColor
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        CompactUsageArrangement.ValueTiles -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.chunked(2).forEach { rowMetrics ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMetrics.forEach { metric ->
                        Box(modifier = Modifier.weight(1f)) {
                            CompactMetricRow(metric = metric)
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

private fun Modifier.ringTileBackground(isDark: Boolean, tone: UsageMetricTone): Modifier {
    val colors = StatusColors.forTheme(isDark)
    val (fill, stroke) = when (tone) {
        UsageMetricTone.Warning -> colors.warning.copy(alpha = 0.12f) to colors.warning.copy(alpha = 0.40f)
        UsageMetricTone.Critical -> colors.critical.copy(alpha = 0.12f) to colors.critical.copy(alpha = 0.40f)
        UsageMetricTone.Normal ->
            if (isDark) {
                Color.Transparent to Color.Transparent
            } else {
                Color.White.copy(alpha = 0.45f) to Color.White.copy(alpha = 0.55f)
            }
    }
    // 深色下普通用量扁平化，不铺格子底和框线。
    if (tone == UsageMetricTone.Normal && isDark) return this
    return this
        .background(fill, RoundedCornerShape(12.dp))
        .border(1.dp, stroke, RoundedCornerShape(12.dp))
}

@Composable
private fun BalanceStrip(metric: UsageMetric, isDark: Boolean) {
    val colors = statusColors()
    val statusColor = statusColor(metric, null, colors)
    val statusText = when (metric.healthState) {
        UsageMetricHealthState.Warning -> "偏低"
        UsageMetricHealthState.Critical, UsageMetricHealthState.Unavailable -> "不足"
        else -> "充足"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) CardPalette.darkSurface else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp),
            )
            .border(
                1.dp,
                if (isDark) CardPalette.darkSurfaceBorder else statusColor.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp),
            ),
    ) {
        // 底部渐隐波浪装饰
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(36.dp),
        ) {
            val waveColor = CardPalette.emerald(isDark).copy(alpha = 0.08f)
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, size.height * 0.7f)
            path.quadraticBezierTo(
                size.width * 0.25f, size.height * 0.2f,
                size.width * 0.5f, size.height * 0.5f,
            )
            path.quadraticBezierTo(
                size.width * 0.75f, size.height * 0.8f,
                size.width, size.height * 0.3f,
            )
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, waveColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCurrency(metric.value, metric.currencyCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardPalette.emerald(isDark),
                )
            }
            StatusPill(text = statusText, color = statusColor, isDark = isDark)
        }
    }
}

private fun ringSubtitle(metric: UsageMetric, vertical: Boolean): String {
    val tone = CompactUsageCardPresentation.tone(metric)
    if (vertical && metric.presentation == UsageMetricPresentation.Progress) {
        if (tone == UsageMetricTone.Critical) return "即将耗尽"
        if (tone == UsageMetricTone.Warning) return "用量偏高"
    }
    val end = metric.windowEnd ?: return if ((progressPercent(metric) ?: 0.0) <= 0.0) "待命中" else "--"
    return "重置 ${formatResetTime(end)}"
}

@Composable
private fun CompactMetricRow(metric: UsageMetric) {
    val colors = statusColors()
    val percent = progressPercent(metric)
    val isProgress = metric.presentation == UsageMetricPresentation.Progress

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isProgress && percent != null) {
                Text(
                    text = formatPercent(percent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor(percent, colors),
                )
            }
        }

        if (isProgress) {
            UsageProgressBar(
                percent = percent,
                color = progressColor(percent, colors),
            )
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "刷新失败") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = card.error?.message ?: "更新失败")
                Text(
                    text = if (card.snapshot == null) {
                        "暂无可用缓存，重试将重新请求用量数据。"
                    } else {
                        "当前显示上次成功数据。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = "重试")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭")
            }
        },
    )
}
