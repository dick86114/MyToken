package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenStatus
import ai.routin.mytoken.core.ui.MyTokenStatusTone
import ai.routin.mytoken.core.ui.myTokenPressScale
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** 简洁卡片等高：2 列网格里页脚对齐；1.3× 字体退回单列后改为自适应高度。 */
internal val CompactCardHeight = 288.dp

internal enum class UsageCardHealth { Normal, Warning, Critical, Stale, Failed, Loading, Disabled }

internal fun usageCardHealth(card: CredentialCardUi): UsageCardHealth {
    if (card.status == RefreshStatus.Disabled) return UsageCardHealth.Disabled
    if (card.status == RefreshStatus.Failed || card.error != null) return UsageCardHealth.Failed
    if (card.status == RefreshStatus.Loading) return UsageCardHealth.Loading
    if (card.isStale) return UsageCardHealth.Stale
    val tones = card.snapshot?.metrics.orEmpty().mapNotNull(::metricHealthTone)
    return when {
        tones.any { it == UsageCardHealth.Critical } -> UsageCardHealth.Critical
        tones.any { it == UsageCardHealth.Warning } -> UsageCardHealth.Warning
        else -> UsageCardHealth.Normal
    }
}

private fun metricHealthTone(metric: UsageMetric): UsageCardHealth? = when (metric.healthState) {
    UsageMetricHealthState.Critical,
    UsageMetricHealthState.Unavailable,
    -> UsageCardHealth.Critical

    UsageMetricHealthState.Warning -> UsageCardHealth.Warning
    UsageMetricHealthState.Normal -> UsageCardHealth.Normal
    UsageMetricHealthState.Unknown -> fallbackHealthTone(metric)
    UsageMetricHealthState.Stale -> null
}

/** 接口未给健康状态时按阈值兜底：进度 80%/50%，余额 ≤ 0 视为超限。 */
private fun fallbackHealthTone(metric: UsageMetric): UsageCardHealth? {
    if (metric.presentation == UsageMetricPresentation.Progress) {
        val used = metric.used?.toDouble() ?: return null
        val limit = metric.limit?.toDouble() ?: return null
        if (limit <= 0.0) return UsageCardHealth.Normal
        val percent = used / limit * 100.0
        return when {
            percent >= 80.0 -> UsageCardHealth.Critical
            percent >= 50.0 -> UsageCardHealth.Warning
            else -> UsageCardHealth.Normal
        }
    }
    if (metric.presentation == UsageMetricPresentation.Balance) {
        return if ((metric.value ?: return null) <= java.math.BigDecimal.ZERO) {
            UsageCardHealth.Critical
        } else {
            UsageCardHealth.Normal
        }
    }
    return null
}

internal fun usageCardStatusText(health: UsageCardHealth): String = when (health) {
    UsageCardHealth.Normal -> "正常"
    UsageCardHealth.Warning -> "接近上限"
    UsageCardHealth.Critical -> "已达上限"
    UsageCardHealth.Stale -> "待刷新"
    UsageCardHealth.Failed -> "更新失败"
    UsageCardHealth.Loading -> "刷新中"
    UsageCardHealth.Disabled -> "已停用"
}

/** 正常与待刷新保持中性；只有异常才用状态色文字。 */
internal fun usageCardStatusTone(health: UsageCardHealth): MyTokenStatusTone = when (health) {
    UsageCardHealth.Warning -> MyTokenStatusTone.Warning
    UsageCardHealth.Critical, UsageCardHealth.Failed -> MyTokenStatusTone.Critical
    UsageCardHealth.Normal, UsageCardHealth.Stale, UsageCardHealth.Loading, UsageCardHealth.Disabled ->
        MyTokenStatusTone.Neutral
}

internal fun usageCardRingTone(health: UsageCardHealth): UsageMetricTone = when (health) {
    UsageCardHealth.Warning -> UsageMetricTone.Warning
    UsageCardHealth.Critical, UsageCardHealth.Failed -> UsageMetricTone.Critical
    else -> UsageMetricTone.Normal
}

internal fun usageCardRingInactive(health: UsageCardHealth): Boolean =
    health == UsageCardHealth.Stale || health == UsageCardHealth.Failed || health == UsageCardHealth.Loading

/** 卡片外框：实色表面 + hairline，无阴影；简洁模式固定高度让网格行等高。 */
@Composable
internal fun UsageCardFrame(
    density: UsageCardDensity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = density.isCompact()
    val fixedHeight = compact && LocalDensity.current.fontScale <= 1.0f
    val interactionSource = remember { MutableInteractionSource() }
    MyTokenSurface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    fixedHeight -> Modifier.height(CompactCardHeight)
                    compact -> Modifier.heightIn(min = CompactCardHeight)
                    else -> Modifier
                },
            )
            .myTokenPressScale(interactionSource, pressedScale = 0.99f)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
    ) {
        val padding = if (compact) 14.dp else 16.dp
        if (fixedHeight) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    content()
                }
                footer()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            ) {
                content()
                footer()
            }
        }
    }
}

/** 身份区：logo + 名称 + meta + 状态（简洁仅圆点 / 完整圆点+文字）+ 刷新。 */
@Composable
internal fun UsageCardIdentity(
    card: CredentialCardUi,
    density: UsageCardDensity,
    health: UsageCardHealth,
    onShowFailure: () -> Unit,
    onRetry: () -> Unit,
    showStatus: Boolean = true,
) {
    val compact = density.isCompact()
    val colors = MyTokenThemeTokens.colors
    val planMetaText = card.snapshot?.planName?.trim()?.takeIf { it.isNotEmpty() }?.let { plan ->
        if (compact) plan else "${ProviderCatalog.displayName(card.credential.providerId)} · $plan"
    }
    val statusText = usageCardStatusText(health)

    Row(modifier = Modifier.heightIn(min = 35.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(if (compact) 24.dp else 28.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            ProviderLogoPlate(
                painter = painterResource(ProviderCatalog.logoRes(card.credential.providerId)),
                contentDescription = null,
                size = if (compact) 24.dp else 28.dp,
                shape = RoundedCornerShape(if (compact) 7.dp else 8.dp),
                contentPadding = 3.dp,
                modifier = Modifier.testTag("credential_avatar_${card.credential.id}"),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 7.dp, end = 6.dp)
                .testTag("credential_identity_${card.credential.id}"),
        ) {
            Text(
                text = card.credential.name,
                modifier = Modifier.testTag("credential_name_${card.credential.id}"),
                style = if (compact) MyTokenType.label else MyTokenType.listTitle,
                fontWeight = FontWeight(650),
                color = colors.ink,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (planMetaText != null) {
                Text(
                    text = planMetaText,
                    modifier = Modifier.padding(top = 2.dp),
                    style = if (compact) {
                        MyTokenType.micro.copy(fontWeight = FontWeight.Normal)
                    } else {
                        MyTokenType.caption
                    },
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val statusModifier = if (health == UsageCardHealth.Failed) {
                Modifier
                    .testTag("credential_failure_${card.credential.id}")
                    .semantics { contentDescription = "查看刷新失败详情" }
                    .clickable(onClick = onShowFailure)
            } else {
                Modifier
            }
            if (showStatus) {
                if (compact) {
                    // 设计稿简洁卡头部只保留状态圆点，文字留给完整卡。
                    Box(
                        modifier = statusModifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (usageCardStatusTone(health)) {
                                    MyTokenStatusTone.Warning -> colors.warningFill
                                    MyTokenStatusTone.Critical -> colors.criticalFill
                                    MyTokenStatusTone.Positive -> colors.positiveFill
                                    MyTokenStatusTone.Neutral -> colors.statusDotNormal
                                },
                            ),
                    )
                } else {
                    MyTokenStatus(
                        text = statusText,
                        tone = usageCardStatusTone(health),
                        modifier = statusModifier,
                    )
                }
            }
            if (!compact || health == UsageCardHealth.Failed) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("credential_refresh_${card.credential.id}")
                        .semantics { contentDescription = "刷新 ${card.credential.name}" }
                        .clickable(
                            enabled = card.status != RefreshStatus.Loading && card.credential.isEnabled,
                            onClick = onRetry,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (card.status == RefreshStatus.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 迷你窗口行：标签 + 百分比 + 3dp 细条；可选剩余/重置说明。 */
@Composable
internal fun UsageCardWindowRow(
    metric: UsageMetric,
    dense: Boolean,
    showDetails: Boolean = !dense,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val percent = progressPercent(metric)
    val tone = CompactUsageCardPresentation.tone(metric)
    val percentColor = when (tone) {
        UsageMetricTone.Warning -> colors.warningText
        UsageMetricTone.Critical -> colors.criticalText
        UsageMetricTone.Normal -> colors.ink
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (dense) "compact_window_row_${metric.id}" else "full_window_row_${metric.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (dense) compactPeriodLabel(metric) else windowLabel(metric),
                modifier = Modifier.weight(1f),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (metric.resetCards.isNotEmpty()) {
                ResetCardsBadge(metric = metric, style = ResetCardsBadgeStyle.Compact)
            }
            Text(
                text = formatPercent(percent),
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = percentColor,
                maxLines = 1,
            )
        }
        UsageProgressBar(
            percent = percent,
            color = when (tone) {
                UsageMetricTone.Warning -> colors.warningFill
                UsageMetricTone.Critical -> colors.criticalFill
                UsageMetricTone.Normal -> colors.positiveFill
            },
            modifier = Modifier.padding(top = 3.dp),
            height = 3.dp,
        )
        if (showDetails) {
            Text(
                text = "剩余 ${formatRemaining(metric)} · ${quotaEndText(metric.windowEnd, "重置") ?: "—"}",
                modifier = Modifier.padding(top = 3.dp),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 完整模式的 facts：小标题 + 值，用间距分组而不是分割线。 */
@Composable
internal fun UsageCardFacts(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    val colors = MyTokenThemeTokens.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = label,
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MyTokenType.caption,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 页脚：简洁模式给状态/重置一句话，完整模式给更新时间 + chevron。 */
@Composable
internal fun UsageCardFooter(
    text: String,
    showChevron: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MyTokenType.caption,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 简洁页脚文案：失败保留上次成功时间，待刷新给出数据时间。 */
internal fun usageCardFooterText(card: CredentialCardUi, health: UsageCardHealth): String = when (health) {
    UsageCardHealth.Failed -> card.snapshot?.fetchedAt?.let {
        "更新失败 · 上次成功 ${formatResetTime(it)}"
    } ?: "更新失败"

    UsageCardHealth.Stale -> card.snapshot?.fetchedAt?.let { "${formatResetTime(it)} 的数据" } ?: "待刷新"
    UsageCardHealth.Loading -> "正在刷新"
    UsageCardHealth.Disabled -> "已停用"
    UsageCardHealth.Warning -> "接近上限"
    UsageCardHealth.Critical -> "已达上限"
    UsageCardHealth.Normal -> card.freshness.text
}

/** 简洁卡页脚：左状态 / 右重置或到期，对应设计稿两段式页脚。 */
internal fun usageCardFooterParts(card: CredentialCardUi, health: UsageCardHealth): Pair<String, String> {
    val snapshot = card.snapshot
    val metrics = snapshot?.metrics.orEmpty()
    val primary = metrics.firstOrNull { it.presentation == UsageMetricPresentation.Progress }
    val balance = metrics.firstOrNull { it.presentation == UsageMetricPresentation.Balance }
    val usageKind = card.credential.metadata[ai.routin.mytoken.domain.model.CredentialMetadataKey.UsageKind]
        ?: snapshot?.usageKind
    val update = snapshot?.fetchedAt?.let { "${formatResetTime(it)} 更新" } ?: "尚未更新"
    val expiry = snapshot?.subscriptionEndAt?.let(::formatShortExpiry)
    val countdown = primary?.windowEnd?.let { "${formatCompactCountdown(it)} 后重置" }.orEmpty()

    return when (health) {
        UsageCardHealth.Failed -> (snapshot?.fetchedAt?.let {
            "更新失败 · 上次成功 ${formatResetTime(it)}"
        } ?: "更新失败") to ""

        UsageCardHealth.Stale -> "待刷新" to (snapshot?.fetchedAt?.let { "${formatResetTime(it)} 的数据" } ?: "")
        UsageCardHealth.Loading -> "刷新中" to ""
        UsageCardHealth.Disabled -> "已停用" to ""
        UsageCardHealth.Warning -> "接近上限" to countdown
        UsageCardHealth.Critical -> "已达上限" to countdown
        UsageCardHealth.Normal -> when {
            balance != null -> compactAccountStatus(card) to update
            usageKind == "plan" && expiry != null ->
                (snapshot?.statusText?.takeIf { it.isNotBlank() } ?: "有效") to "$expiry 到期"
            primary?.windowEnd != null -> update to countdown
            expiry != null -> "有效" to "$expiry 到期"
            else -> update to ""
        }
    }
}

internal fun compactAccountStatus(card: CredentialCardUi): String {
    val raw = card.snapshot?.statusText?.trim()?.takeIf { it.isNotEmpty() } ?: return "账户可用"
    return if (raw.startsWith("账户")) raw else "账户$raw"
}

@Composable
internal fun UsageCardFooterColumns(
    left: String,
    right: String,
    health: UsageCardHealth,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val leftColor = when (health) {
        UsageCardHealth.Warning -> colors.warningText
        UsageCardHealth.Critical, UsageCardHealth.Failed -> colors.criticalText
        else -> colors.muted
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = left,
            modifier = Modifier
                .weight(1f)
                .testTag("card_footer_left"),
            style = MyTokenType.caption,
            fontWeight = if (leftColor != colors.muted) FontWeight(600) else FontWeight.Normal,
            color = leftColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (right.isNotEmpty()) {
            Text(
                text = right,
                modifier = Modifier.testTag("card_footer_right"),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 加载中没有缓存数据时的静态骨架：只用 track 色块，不做 shimmer。 */
@Composable
internal fun UsageCardSkeletonBody(compact: Boolean, modifier: Modifier = Modifier) {
    val colors = MyTokenThemeTokens.colors
    val widths = if (compact) listOf(0.55f, 0.8f, 0.42f) else listOf(0.45f, 0.9f, 0.7f, 0.35f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_skeleton"),
        verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 11.dp),
    ) {
        widths.forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(if (compact) 10.dp else 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.track),
            )
        }
    }
}

@Composable
internal fun UsageCardSkeletonFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.34f)
            .height(9.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MyTokenThemeTokens.colors.track),
    )
}
