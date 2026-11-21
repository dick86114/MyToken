package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.usage.RefreshStatus
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.roundToInt

/**
 * 设计稿的票据切角：左上、右下保持 20dp 圆角，右上、左下各切掉 20dp。
 * 只作为 background 的 Shape 使用，不用 Modifier.clip，避免吞掉行内点击。
 */
private class ReportCornerShape(
    private val cutCorner: Boolean,
    private val roundBottomEnd: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val limit = minOf(size.width, size.height) / 2f
        fun Dp.px(): Float = with(density) { toPx() }.coerceIn(0f, limit)
        val cut = 20.dp.px()
        val topStart = 20.dp.px()
        val bottomEnd = if (roundBottomEnd) 20.dp.px() else 0f
        val path = Path().apply {
            moveTo(topStart, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, cut)
            if (cutCorner) {
                lineTo(size.width, size.height - bottomEnd)
                arcTo(
                    rect = Rect(
                        size.width - bottomEnd * 2f,
                        size.height - bottomEnd * 2f,
                        size.width,
                        size.height,
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(cut, size.height)
                lineTo(0f, size.height - cut)
            } else {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            lineTo(0f, topStart)
            arcTo(
                rect = Rect(0f, 0f, topStart * 2f, topStart * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

private val ReportBodyShape = ReportCornerShape(cutCorner = true, roundBottomEnd = true)
private val ReportTicketShape = ReportCornerShape(cutCorner = false, roundBottomEnd = false)

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

        val metric = card.snapshot?.metrics?.firstOrNull()
        val failed = card.status == RefreshStatus.Failed || card.snapshot == null
        val value = when {
            failed -> "失败"
            metric?.presentation == UsageMetricPresentation.Progress ->
                progressPercent(metric)?.let { "${(100.0 - it).roundToInt()}%" } ?: "—"

            metric?.presentation == UsageMetricPresentation.Balance ->
                formatCurrency(metric.value, metric.currencyCode)

            else -> "—"
        }
        val valueLabel = when {
            failed -> ""
            metric?.presentation == UsageMetricPresentation.Progress -> "剩余"
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

/** 琥珀报告：固定在上方，左右滑动卡片时保持不动。 */
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
    val tail = when {
        isNormal -> "用量正常"
        findings.any { it.failed } -> "需注意"
        else -> "接近上限"
    }
    val syncLabel = syncText
        .removeSuffix("更新")
        .removeSuffix("同步")
        .trim()
        .ifBlank { syncText }
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 不用 Modifier.clip：异形切角会吞掉报告内的点击命中区。
            .background(colors.reportSurface, ReportBodyShape)
            .testTag("home_health_report"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.reportTicket, ReportTicketShape)
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
                    tint = colors.reportTicketInk,
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
                color = colors.reportAccent,
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
private fun ReportDial(
    tones: List<HomeHealthTone>,
    normal: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = modifier
            .size(88.dp)
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
            Text(
                text = "用量正常",
                style = MyTokenType.caption,
                color = colors.reportTicketMuted,
            )
        }
    }
}
