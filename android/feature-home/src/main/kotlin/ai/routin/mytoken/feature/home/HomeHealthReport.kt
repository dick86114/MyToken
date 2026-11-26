package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenReportBodyShape
import ai.routin.mytoken.core.ui.MyTokenReportTicketShape
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.model.UsageMetricSemantic
import ai.routin.mytoken.domain.usage.RefreshStatus
import java.math.BigDecimal
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.roundToInt

internal data class HomeReportFinding(
    val credentialId: UUID,
    val providerId: ProviderId,
    val name: String,
    val tone: HomeHealthTone,
    val detail: String,
    val value: String,
    val valueLabel: String,
    val failed: Boolean,
)

/** 报告只列风险账户；筛选只影响下方卡片，不改变这里的口径。 */
internal fun homeReportFindings(cards: List<CredentialCardUi>): List<HomeReportFinding> =
    cards.mapNotNull { card ->
        val tone = homeHealthTone(card)
        if (tone == HomeHealthTone.Normal) return@mapNotNull null

        val metric = alertingMetric(card)
        val failed = card.status == RefreshStatus.Failed || card.snapshot == null
        val value = when {
            failed -> "失败"
            metric?.presentation == UsageMetricPresentation.Progress ->
                displayedPercent(metric)?.let { "${it.roundToInt()}%" } ?: "—"

            metric?.presentation == UsageMetricPresentation.Balance ->
                formatCurrency(metric.value, metric.currencyCode)

            else -> "—"
        }
        val valueLabel = when {
            failed -> ""
            metric?.presentation == UsageMetricPresentation.Progress ->
                if (metric.semantic == UsageMetricSemantic.RemainingQuota) "剩余" else "已用"
            metric?.presentation == UsageMetricPresentation.Balance -> "余额"
            else -> "状态"
        }
        val detail = buildList {
            metric?.label?.takeIf { it.isNotBlank() }?.let(::add)
            metric?.windowEnd?.let { add("${formatResetTime(it)} 重置") }
            if (failed && isEmpty()) add("凭证状态异常")
        }.joinToString(" · ").ifBlank { homeHealthLabel(tone) }

        HomeReportFinding(
            credentialId = card.credential.id,
            providerId = card.credential.providerId,
            name = card.credential.name,
            tone = tone,
            detail = detail,
            value = value,
            valueLabel = valueLabel,
            failed = failed,
        )
    }

/**
 * 行文案必须指向真正触发告警的窗口：先取 Critical，再取 Warning，最后才回退首个指标。
 * 与 macOS CredentialManagementModel.attention 的选择顺序一致。
 */
private fun alertingMetric(card: CredentialCardUi): UsageMetric? {
    val metrics = card.snapshot?.metrics.orEmpty()
    return metrics.firstOrNull { metricHealthTone(it) == UsageCardHealth.Critical }
        ?: metrics.firstOrNull { metricHealthTone(it) == UsageCardHealth.Warning }
        ?: metrics.firstOrNull()
}

/** 报告整体 tone 取最差 severity：任一 Critical（含失败/不可用）即升级。 */
internal fun homeReportTone(findings: List<HomeReportFinding>): HomeHealthTone = when {
    findings.isEmpty() -> HomeHealthTone.Normal
    findings.any { it.tone == HomeHealthTone.Critical } -> HomeHealthTone.Critical
    else -> HomeHealthTone.Warning
}

/** 报告标题文案与卡片状态词保持一致；失败仍用「需注意」提示处理。 */
internal fun homeReportTail(findings: List<HomeReportFinding>): String = when {
    findings.isEmpty() -> "用量正常"
    findings.any { it.failed } -> "需注意"
    findings.any { it.tone == HomeHealthTone.Critical } -> "已达上限"
    else -> "接近上限"
}

/** 根据 severity 取颜色：Critical 用高对比红，其余沿用琥珀/次级墨色。 */
internal fun severityAccent(
    tone: HomeHealthTone,
    warningColor: Color,
    criticalColor: Color,
): Color = if (tone == HomeHealthTone.Critical) criticalColor else warningColor

/** 报告页脚的同步文案：去掉「更新/同步」后缀，只留下时间。 */
internal fun reportSyncLabel(syncText: String): String = syncText
    .removeSuffix("更新")
    .removeSuffix("同步")
    .trim()
    .ifBlank { syncText }

/** 与卡片同一方向：RemainingQuota 显示剩余百分比，其余显示已用百分比。 */
private fun displayedPercent(metric: UsageMetric): Double? {
    val limit = metric.limit ?: return null
    if (limit.compareTo(BigDecimal.ZERO) == 0) return null
    val amount = when (metric.semantic) {
        UsageMetricSemantic.RemainingQuota -> metric.remaining
        else -> metric.used
    } ?: return null
    return amount.toDouble() / limit.toDouble() * 100.0
}

/** 风险报告：票面始终保持琥珀；正文随页面明暗（浅色白纸 / 深色画布），行内百分比与收起标题按 severity 着色。 */
@Composable
internal fun HomeHealthReport(
    cards: List<CredentialCardUi>,
    syncText: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLocate: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val refreshSpin = rememberInfiniteTransition(label = "report-refresh")
    val spin by refreshSpin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "report-refresh-spin",
    )
    val findings = remember(cards) { homeReportFindings(cards) }
    val total = cards.size
    val attention = findings.size
    val normal = total - attention
    val isNormal = attention == 0
    val headlineCount = if (isNormal) total else attention
    val tail = remember(findings) { homeReportTail(findings) }
    val syncLabel = reportSyncLabel(syncText)
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 不用 Modifier.clip：异形切角会吞掉报告内的点击命中区。
            .background(colors.reportSurface, MyTokenReportBodyShape)
            .testTag("home_health_report"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.reportTicket, MyTokenReportTicketShape)
                .heightIn(min = 106.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = headlineCount.toString(),
                        modifier = Modifier.testTag("report_attention_count"),
                        style = MyTokenType.display,
                        color = colors.reportTicketInk,
                    )
                    Text(
                        text = "个凭证",
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                        style = MyTokenType.body,
                        fontWeight = FontWeight(550),
                        color = colors.reportTicketInk,
                    )
                }
                Text(
                    text = tail,
                    modifier = Modifier.testTag("report_tail"),
                    style = MyTokenType.section,
                    color = colors.reportTicketInk,
                )
            }
            ReportDial(
                tones = cards.map(::homeHealthTone),
                normal = normal,
                total = total,
            )
        }

        if (!isNormal) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                findings.forEach { finding ->
                    ReportFindingRow(
                        finding = finding,
                        onClick = { onLocate(finding.credentialId) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近同步 $syncLabel",
                modifier = Modifier.weight(1f),
                style = MyTokenType.caption,
                fontWeight = FontWeight.SemiBold,
                color = colors.reportMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = !isRefreshing,
                        role = Role.Button,
                        onClick = onRefresh,
                    )
                    .semantics {
                        contentDescription = if (isRefreshing) {
                            "正在刷新全部凭证"
                        } else {
                            "刷新全部凭证"
                        }
                    }
                    .testTag("report_refresh"),
                contentAlignment = Alignment.Center,
            ) {
                // 透明图标直接落在报告票面上，不再套黄色圆形底。
                ReportRefreshGlyph(
                    // 报告底部跟随页面明暗，图标使用与正文一致的墨色。
                    tint = colors.reportInk,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = if (isRefreshing && motionEnabled) spin else 0f
                        },
                )
            }
        }
    }
}

@Composable
private fun ReportFindingRow(
    finding: HomeReportFinding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription =
                    "${finding.name}，${finding.detail}，${finding.value}${finding.valueLabel}"
            }
            .testTag("health_finding_${finding.credentialId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogoPlate(
            painter = painterResource(ProviderCatalog.logoRes(finding.providerId)),
            contentDescription = null,
            size = 24.dp,
            shape = RoundedCornerShape(7.dp),
            contentPadding = 3.dp,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = finding.name,
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.reportInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = finding.detail,
                style = MyTokenType.caption,
                color = colors.reportMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = finding.value,
                modifier = Modifier.alignByBaseline(),
                style = MyTokenType.section,
                color = severityAccent(finding.tone, colors.reportAccent, colors.reportCritical),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = finding.valueLabel,
                modifier = Modifier.alignByBaseline(),
                style = MyTokenType.caption,
                color = colors.reportMuted,
                maxLines = 1,
            )
        }
        Text(
            text = "›",
            modifier = Modifier.padding(start = 6.dp),
            style = MyTokenType.label,
            color = colors.reportMuted,
        )
    }
}

@Composable
internal fun ReportDial(
    tones: List<HomeHealthTone>,
    normal: Int,
    total: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 88.dp,
    showCaption: Boolean = true,
) {
    val colors = MyTokenThemeTokens.colors
    // 小尺寸只画环：22sp 计数会溢出 42dp 圆环并盖住票面文字。
    val showCount = diameter >= 56.dp
    Box(
        modifier = modifier
            .size(diameter)
            .testTag("report_dial")
            .semantics {
                contentDescription = "$total 个凭证，$normal 个正常，${total - normal} 个需注意"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total <= 0) return@Canvas
            val strokeWidth = 3.5.dp.toPx()
            val inset = strokeWidth / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val gap = 7f
            val step = 270f / total
            tones.forEachIndexed { index, tone ->
                drawArc(
                    color = if (tone == HomeHealthTone.Normal) {
                        colors.reportDialTrack
                    } else {
                        colors.reportTicketInk
                    },
                    startAngle = 135f + index * step + gap / 2f,
                    sweepAngle = (step - gap).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        if (showCount) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = normal.toString(),
                        style = MyTokenType.title,
                        color = colors.reportTicketInk,
                    )
                    Text(
                        text = " / $total",
                        modifier = Modifier.padding(bottom = 2.dp),
                        style = MyTokenType.caption,
                        color = colors.reportTicketMuted,
                    )
                }
                if (showCaption) {
                    Text(
                        text = "用量正常",
                        style = MyTokenType.caption,
                        color = colors.reportTicketMuted,
                    )
                }
            }
        }
    }
}
