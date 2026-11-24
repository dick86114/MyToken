package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenMotion
import ai.routin.mytoken.core.ui.MyTokenReportBodyShape
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.rememberMyTokenMotionEnabled
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * 大屏报告：内容与手机版完全一致（数量 / 结论 / 表盘 / 告警行 / 最近同步），
 * 只按窗口比例切换横排与竖排。
 *
 * - 无告警：80dp 短票；琥珀头 + 墨色一行，不留空的发现区。
 * - 有告警：16:9 横排固定 110dp，其余比例随告警行数自然增长。
 */
@Composable
internal fun WideHomeHealthReport(
    cards: List<CredentialCardUi>,
    syncText: String,
    orientation: WideReportOrientation,
    onLocate: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val motionEnabled = rememberMyTokenMotionEnabled()
    val findings = remember(cards) { homeReportFindings(cards) }
    val total = cards.size
    val attention = findings.size
    val normal = total - attention
    val isNormal = attention == 0
    val headlineCount = if (isNormal) total else attention
    val tail = remember(findings) { homeReportTail(findings) }
    val syncLabel = reportSyncLabel(syncText)

    val fixedHeight: Dp? = when {
        isNormal -> WideHomeSpec.normalReportHeight
        orientation == WideReportOrientation.Horizontal -> WideHomeSpec.alertReportHeight
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = MyTokenMotion.reportResizeDurationMillis(motionEnabled),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MyTokenReportBodyShape)
                .background(colors.reportSurface)
                .then(
                    if (fixedHeight != null) {
                        Modifier.height(fixedHeight)
                    } else {
                        Modifier.heightIn(min = WideHomeSpec.alertReportHeight)
                    },
                )
                .testTag("home_wide_report"),
        ) {
            when (orientation) {
                WideReportOrientation.Horizontal -> HorizontalReport(
                    isNormal = isNormal,
                    headlineCount = headlineCount,
                    tail = tail,
                    normal = normal,
                    total = total,
                    tones = cards.map(::homeHealthTone),
                    findings = findings,
                    syncLabel = syncLabel,
                    onLocate = onLocate,
                )

                WideReportOrientation.Vertical -> VerticalReport(
                    isNormal = isNormal,
                    headlineCount = headlineCount,
                    tail = tail,
                    normal = normal,
                    total = total,
                    tones = cards.map(::homeHealthTone),
                    findings = findings,
                    syncLabel = syncLabel,
                    onLocate = onLocate,
                )
            }
        }
    }
}

@Composable
private fun HorizontalReport(
    isNormal: Boolean,
    headlineCount: Int,
    tail: String,
    normal: Int,
    total: Int,
    tones: List<HomeHealthTone>,
    findings: List<HomeReportFinding>,
    syncLabel: String,
    onLocate: (UUID) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Row(modifier = Modifier.fillMaxSize()) {
        ReportTicketHead(
            headlineCount = headlineCount,
            tail = tail,
            normal = normal,
            total = total,
            tones = tones,
            dialDiameter = 72.dp,
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        ) {
            if (isNormal) {
                NormalSummaryLine(
                    total = total,
                    syncLabel = syncLabel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    findings.chunked(2).forEach { rowFindings ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowFindings.forEach { finding ->
                                WideReportFindingRow(
                                    finding = finding,
                                    onClick = { onLocate(finding.credentialId) },
                                    modifier = Modifier.weight(1f),
                                    highlightedBackground = true,
                                )
                            }
                            if (rowFindings.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Text(
                    text = "最近同步 $syncLabel",
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    style = MyTokenType.micro,
                    fontWeight = FontWeight.Medium,
                    color = colors.reportMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VerticalReport(
    isNormal: Boolean,
    headlineCount: Int,
    tail: String,
    normal: Int,
    total: Int,
    tones: List<HomeHealthTone>,
    findings: List<HomeReportFinding>,
    syncLabel: String,
    onLocate: (UUID) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    ReportTicketHead(
        headlineCount = headlineCount,
        tail = tail,
        normal = normal,
        total = total,
        tones = tones,
        dialDiameter = if (isNormal) 42.dp else 64.dp,
        compact = isNormal,
        modifier = Modifier.fillMaxWidth(),
    )
    if (isNormal) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NormalCheckBadge(size = 15.dp)
            Text(
                text = "全部 $total 个凭证当前用量正常",
                modifier = Modifier.weight(1f),
                style = MyTokenType.caption,
                fontWeight = FontWeight.SemiBold,
                color = colors.reportInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "最近同步 $syncLabel",
                style = MyTokenType.micro,
                color = colors.reportMuted,
                maxLines = 1,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            findings.forEach { finding ->
                WideReportFindingRow(
                    finding = finding,
                    onClick = { onLocate(finding.credentialId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 6.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近同步 $syncLabel",
                style = MyTokenType.micro,
                fontWeight = FontWeight.Medium,
                color = colors.reportMuted,
                maxLines = 1,
            )
        }
    }
}

/** 琥珀票头：数量 / 结论 / 表盘；内容与手机报告完全一致。 */
@Composable
private fun ReportTicketHead(
    headlineCount: Int,
    tail: String,
    normal: Int,
    total: Int,
    tones: List<HomeHealthTone>,
    dialDiameter: Dp,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .background(colors.reportTicket)
            .padding(horizontal = 16.dp, vertical = if (compact) 5.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = headlineCount.toString(),
                    modifier = Modifier.testTag("report_attention_count"),
                    style = if (compact) MyTokenType.section else MyTokenType.title,
                    color = colors.reportTicketInk,
                )
                Text(
                    text = "个凭证",
                    modifier = Modifier.padding(start = 4.dp, bottom = if (compact) 1.dp else 3.dp),
                    style = if (compact) MyTokenType.caption else MyTokenType.label,
                    fontWeight = FontWeight(550),
                    color = colors.reportTicketInk,
                    maxLines = 1,
                )
            }
            Text(
                text = tail,
                modifier = Modifier.testTag("report_tail"),
                style = if (compact) MyTokenType.caption else MyTokenType.section,
                fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.reportTicketInk,
                maxLines = 1,
            )
        }
        ReportDial(
            tones = tones,
            normal = normal,
            total = total,
            diameter = dialDiameter,
            showCaption = !compact,
        )
    }
}

@Composable
private fun NormalSummaryLine(
    total: Int,
    syncLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        NormalCheckBadge(size = 19.dp)
        Text(
            text = "全部 $total 个凭证当前用量正常",
            modifier = Modifier.weight(1f),
            style = MyTokenType.label,
            fontWeight = FontWeight.SemiBold,
            color = colors.reportInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "最近同步 $syncLabel",
            style = MyTokenType.micro,
            color = colors.reportMuted,
            maxLines = 1,
        )
    }
}

/** 报告里的手绘勾：白色描边圆 + 琥珀勾，不使用 emoji。 */
@Composable
private fun NormalCheckBadge(size: Dp) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, colors.reportLine, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = colors.reportAccent,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

/** 横排告警行保持与手机报告相同的字段：logo、名称、详情、数值 + 单位、›。 */
@Composable
private fun WideReportFindingRow(
    finding: HomeReportFinding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightedBackground: Boolean = false,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .heightIn(min = if (highlightedBackground) 36.dp else 42.dp)
            .clip(shape)
            .then(
                if (highlightedBackground) {
                    Modifier
                        .background(colors.reportInk.copy(alpha = 0.06f))
                        .border(1.dp, colors.reportLine.copy(alpha = 0.45f), shape)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription =
                    "${finding.name}，${finding.detail}，${finding.value}${finding.valueLabel}"
            }
            .padding(
                horizontal = if (highlightedBackground) 8.dp else 4.dp,
                vertical = if (highlightedBackground) 2.dp else 5.dp,
            )
            .testTag("health_finding_${finding.credentialId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogoPlate(
            painter = painterResource(ProviderCatalog.logoRes(finding.providerId)),
            contentDescription = null,
            size = 22.dp,
            shape = RoundedCornerShape(6.dp),
            contentPadding = 2.5.dp,
        )
        Spacer(modifier = Modifier.size(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = finding.name,
                style = if (highlightedBackground) MyTokenType.caption else MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.reportInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = finding.detail,
                style = MyTokenType.micro.copy(fontWeight = FontWeight.Normal),
                color = colors.reportMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = finding.value,
                modifier = Modifier.alignByBaseline(),
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = severityAccent(finding.tone, colors.reportAccent, colors.reportCritical),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = finding.valueLabel,
                modifier = Modifier.alignByBaseline(),
                style = MyTokenType.micro,
                color = colors.reportMuted,
                maxLines = 1,
            )
        }
        Text(
            text = "›",
            modifier = Modifier.padding(start = 4.dp),
            style = MyTokenType.label,
            color = colors.reportMuted,
        )
    }
}
