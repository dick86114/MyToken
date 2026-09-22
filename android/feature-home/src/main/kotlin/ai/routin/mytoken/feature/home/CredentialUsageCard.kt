package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.usage.RefreshStatus
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag

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
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }
    var showsShareDialog by remember(card.credential.id) { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (usageCardDensity == UsageCardDensity.COMPACT) {
                CompactUsageCardBody(
                    card = card,
                    providerName = providerName,
                    plan = plan,
                    layoutMode = layoutMode,
                    onRefresh = onRefresh,
                    onRetry = onRetry,
                )
            } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = card.credential.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 96.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, top = 4.dp),
                ) {
                    Text(
                        text = if (plan.isEmpty()) providerName else "$providerName · $plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val start = card.snapshot?.subscriptionStartAt
                    val end = card.snapshot?.subscriptionEndAt
                    if (start != null || end != null) {
                        Row(modifier = Modifier.padding(top = 2.dp)) {
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
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (card.status == RefreshStatus.Failed) {
                        IconButton(
                            onClick = { showsFailureDetails = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("credential_failure_${card.credential.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "查看刷新失败详情",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = { showsShareDialog = true },
                        enabled = card.snapshot != null && card.credential.isEnabled,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("credential_share_${card.credential.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "分享 ${card.credential.name}",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = card.status != RefreshStatus.Loading && card.credential.isEnabled,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("credential_refresh_${card.credential.id}"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "刷新 ${card.credential.name}",
                            )
                            if (card.status == RefreshStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(6.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }

            val metrics = card.snapshot?.metrics.orEmpty()
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

            if (card.status == RefreshStatus.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
            }
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

@Composable
private fun CompactUsageCardBody(
    card: CredentialCardUi,
    providerName: String,
    plan: String,
    layoutMode: MyTokenLayoutMode,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    var showsFailureDetails by remember(card.credential.id) { mutableStateOf(false) }
    var showsShareDialog by remember(card.credential.id) { mutableStateOf(false) }
    val metrics = UsageCardCompactSpec.orderedMetrics(
        providerId = card.credential.providerId,
        metadata = card.credential.metadata,
        metrics = card.snapshot?.metrics.orEmpty(),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = card.credential.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(modifier = Modifier.padding(start = 4.dp)) {
                if (card.status == RefreshStatus.Failed) {
                    IconButton(
                        onClick = { showsFailureDetails = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("credential_failure_${card.credential.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "查看刷新失败详情",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { showsShareDialog = true },
                    enabled = card.snapshot != null && card.credential.isEnabled,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("credential_share_${card.credential.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "分享 ${card.credential.name}",
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = card.status != RefreshStatus.Loading && card.credential.isEnabled,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("credential_refresh_${card.credential.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "刷新 ${card.credential.name}",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Text(
            text = if (plan.isEmpty()) providerName else "$providerName · $plan",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (metrics.isEmpty()) {
            Text(
                text = "暂无用量数据",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metrics.forEach { metric -> CompactMetricRow(metric = metric) }
            }
        }

        if (card.status == RefreshStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
        }
    }

    if (showsFailureDetails) {
        RefreshFailureDialog(
            card = card,
            onDismiss = { showsFailureDetails = false },
            onRetry = onRetry,
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

@Composable
private fun CompactMetricRow(metric: UsageMetric) {
    val colors = statusColors()
    val percent = progressPercent(metric)
    val isProgress = metric.presentation == ai.routin.mytoken.domain.model.UsageMetricPresentation.Progress

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
    metric.presentation == ai.routin.mytoken.domain.model.UsageMetricPresentation.Balance ->
        formatCurrency(metric.value, metric.currencyCode)
    metric.presentation == ai.routin.mytoken.domain.model.UsageMetricPresentation.Status ->
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
