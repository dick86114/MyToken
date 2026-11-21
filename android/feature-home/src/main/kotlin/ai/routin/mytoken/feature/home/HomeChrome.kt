package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenSpacing
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class HomeHealthTone { Normal, Warning, Critical }

internal fun homeHealthLabel(tone: HomeHealthTone): String = when (tone) {
    HomeHealthTone.Normal -> "正常"
    HomeHealthTone.Warning -> "需注意"
    HomeHealthTone.Critical -> "已超限"
}

/** 卡片健康：失败/不可用为 critical，warning 次之；待刷新与加载保持中性。 */
internal fun homeHealthTone(card: CredentialCardUi): HomeHealthTone =
    when (usageCardHealth(card)) {
        UsageCardHealth.Critical, UsageCardHealth.Failed -> HomeHealthTone.Critical
        UsageCardHealth.Warning -> HomeHealthTone.Warning
        UsageCardHealth.Normal,
        UsageCardHealth.Stale,
        UsageCardHealth.Loading,
        UsageCardHealth.Disabled,
        -> HomeHealthTone.Normal
    }

private fun metricHealthTone(metric: UsageMetric): HomeHealthTone? = when (metric.healthState) {
    UsageMetricHealthState.Critical,
    UsageMetricHealthState.Unavailable,
    -> HomeHealthTone.Critical

    UsageMetricHealthState.Warning -> HomeHealthTone.Warning

    UsageMetricHealthState.Normal -> HomeHealthTone.Normal

    UsageMetricHealthState.Unknown -> fallbackHealthTone(metric)
    UsageMetricHealthState.Stale -> null
}

/** 接口未给健康状态时按阈值兜底：进度 80%/50%，余额 ≤ 0 视为超限。 */
private fun fallbackHealthTone(metric: UsageMetric): HomeHealthTone? {
    if (metric.presentation == UsageMetricPresentation.Progress) {
        val used = metric.used?.toDouble() ?: return null
        val limit = metric.limit?.toDouble() ?: return null
        if (limit <= 0.0) return HomeHealthTone.Normal
        val percent = used / limit * 100.0
        return when {
            percent >= 80.0 -> HomeHealthTone.Critical
            percent >= 50.0 -> HomeHealthTone.Warning
            else -> HomeHealthTone.Normal
        }
    }
    if (metric.presentation == UsageMetricPresentation.Balance) {
        return if ((metric.value ?: return null) <= java.math.BigDecimal.ZERO) {
            HomeHealthTone.Critical
        } else {
            HomeHealthTone.Normal
        }
    }
    return null
}

internal data class HealthRulerEntry(
    val credentialId: UUID,
    val name: String,
    val tone: HomeHealthTone,
)

/** 固定头部：凭证入口 / 简洁-完整切换 / 设置入口。 */
@Composable
internal fun HomeToolbar(
    credentialCount: Int,
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MyTokenSize.header)
            .testTag("home_toolbar"),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(MyTokenSize.touch)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpenCredentials)
                .semantics { contentDescription = "打开凭证列表，共 $credentialCount 个凭证" }
                .testTag("home_open_credentials"),
            contentAlignment = Alignment.Center,
        ) {
            // 设计稿入口图标：两长一短三横线（20dp viewport，1.6dp 圆头描边）。
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .testTag("home_menu_icon"),
            ) {
                val stroke = 1.6.dp.toPx()
                fun line(y: Float, endX: Float) {
                    drawLine(
                        color = colors.muted,
                        start = Offset(3.dp.toPx(), y.dp.toPx()),
                        end = Offset(endX.dp.toPx(), y.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                line(y = 5f, endX = 17f)
                line(y = 10f, endX = 17f)
                line(y = 15f, endX = 12f)
            }
        }

        Box(modifier = Modifier.align(Alignment.Center)) {
            DensitySwitch(
                density = density,
                onToggle = onToggleDensity,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(MyTokenSize.touch)
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .semantics {
                    contentDescription = if (hasUpdateAvailable) {
                        "打开设置，有可用更新"
                    } else {
                        "打开设置"
                    }
                }
                .testTag("home_open_settings"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(19.dp),
            )
            if (hasUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.actionFill)
                        .testTag("home_update_badge"),
                )
            }
        }
    }
}

/** 纯文字模式切换：选中项使用强调色 + 2dp 下划线。 */
@Composable
private fun DensitySwitch(
    density: UsageCardDensity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = density.isCompact()
    val colors = MyTokenThemeTokens.colors
    val optionWidth = 52.dp
    val underlineWidth = 26.dp
    val underlineOffset by animateDpAsState(
        targetValue = if (compact) 0.dp else optionWidth + 6.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "densityUnderlineOffset",
    )
    Box(
        modifier = modifier
            .height(MyTokenSize.header)
            .testTag("usage_density_toggle"),
    ) {
        Row(
            modifier = Modifier
                .height(MyTokenSize.header)
                .selectableGroup(),
        ) {
            DensityOption(
                text = "简洁",
                compact = true,
                selected = compact,
                testTag = "usage_density_option_compact",
                width = optionWidth,
                onClick = { if (!compact) onToggle() },
            )
            Spacer(modifier = Modifier.width(6.dp))
            DensityOption(
                text = "完整",
                compact = false,
                selected = !compact,
                testTag = "usage_density_option_full",
                width = optionWidth,
                onClick = { if (compact) onToggle() },
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(
                    x = underlineOffset + (optionWidth - underlineWidth) / 2 +
                        if (compact) 0.dp else 6.dp,
                    y = (-8).dp,
                )
                .width(underlineWidth)
                .height(2.dp)
                .clip(CircleShape)
                .background(colors.actionFill),
        )
    }
}

@Composable
private fun DensityOption(
    text: String,
    compact: Boolean,
    selected: Boolean,
    testTag: String,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (compact) "简洁模式" else "完整模式"
                stateDescription = if (selected) "已选" else "未选"
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MyTokenType.label,
            fontWeight = if (selected) FontWeight(650) else FontWeight(550),
            color = if (selected) colors.ink else colors.muted,
            maxLines = 1,
        )
    }
}

/** 状态行：账户数 / 需注意数量 / 同步时间 + 供应商筛选。 */
@Composable
internal fun HomeStatusRow(
    total: Int,
    allTotal: Int = total,
    attention: Int,
    syncText: String,
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    onSelect: (ProviderId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    var expanded by remember { mutableStateOf(false) }
    val filterLabel = selectedProvider
        ?.let { ProviderCatalog.displayName(it) }
        ?: "全部供应商"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .testTag("home_status_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                append("$total 个账户 · ")
                if (attention > 0) {
                    withStyle(SpanStyle(color = colors.warningText, fontWeight = FontWeight(650))) {
                        append("$attention 个需注意")
                    }
                } else {
                    append("全部正常")
                }
                append(" · $syncText")
            },
            modifier = Modifier.weight(1f),
            style = MyTokenType.label,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(MyTokenSpacing.sm))
        Box(
            modifier = Modifier
                .height(34.dp)
                .widthIn(min = MyTokenSize.touch)
                .clip(RoundedCornerShape(MyTokenSize.switchHeight))
                .clickable(role = Role.Button) { expanded = true }
                .semantics { selected = selectedProvider != null }
                .testTag("provider_filter_menu"),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(MyTokenSize.switchHeight))
                    .background(colors.surface)
                    .border(
                        width = 1.dp,
                        color = colors.line,
                        shape = RoundedCornerShape(MyTokenSize.switchHeight),
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = filterLabel,
                    style = MyTokenType.label,
                    color = colors.ink,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = "全部 $allTotal") },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
                providers.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${ProviderCatalog.displayName(provider)} ${counts[provider] ?: 0}",
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(provider)
                        },
                        modifier = Modifier
                            .testTag("provider_filter_item_${provider.rawValue}")
                            .semantics { selected = provider == selectedProvider },
                    )
                }
            }
        }
    }
}

/** 健康标尺：4dp 视觉条 + 48dp 触控列，点击定位对应卡片。 */
@Composable
internal fun HealthRuler(
    entries: List<HealthRulerEntry>,
    onLocate: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onLocate(entry.credentialId) }
                    .semantics {
                        contentDescription = "${entry.name}，${homeHealthLabel(entry.tone)}"
                    }
                    .testTag("health_segment_$index"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MyTokenSize.healthBar)
                        .clip(CircleShape)
                        .background(
                            when (entry.tone) {
                                HomeHealthTone.Normal -> colors.statusDotNormal.copy(alpha = 0.5f)
                                HomeHealthTone.Warning -> colors.warningFill
                                HomeHealthTone.Critical -> colors.criticalFill
                            },
                        ),
                )
            }
        }
    }
}

/** 更新提示：28dp 单行，✦ + 新版本号 + ›；设计稿为静态样式。 */
@Composable
internal fun HomeUpdateBanner(
    version: String?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clickable(role = Role.Button, onClick = onOpenSettings)
            .semantics { contentDescription = "有新版本可用，前往设置查看并更新" }
            .testTag("home_update_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "✦",
            modifier = Modifier.testTag("home_update_banner_icon"),
            style = MyTokenType.micro,
            color = colors.actionText,
        )
        Text(
            text = version?.takeIf { it.isNotBlank() }?.let { "新版本 $it 可用" } ?: "有新版本可用",
            style = MyTokenType.micro.copy(fontWeight = FontWeight.Normal),
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "›",
            style = MyTokenType.label,
            color = colors.muted,
        )
    }
}

@Composable
private fun UpdateStar(modifier: Modifier = Modifier) {
    val colors = MyTokenThemeTokens.colors
    val animationsEnabled = remember { android.animation.ValueAnimator.areAnimatorsEnabled() }
    val phase = if (animationsEnabled) {
        rememberInfiniteTransition(label = "updateStar").animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2_400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "updateStarPhase",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.85f) }
    }

    Canvas(
        modifier = modifier
            .size(16.dp)
            .graphicsLayer {
                alpha = 0.72f + 0.28f * phase.value
                scaleX = 0.94f + 0.06f * phase.value
                scaleY = 0.94f + 0.06f * phase.value
            },
    ) {
        val outer = size.minDimension * 0.48f
        val inner = size.minDimension * 0.21f
        val star = Path().apply {
            repeat(10) { index ->
                val radius = if (index % 2 == 0) outer else inner
                val angle = (-90f + index * 36f) * PI.toFloat() / 180f
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path = star, color = colors.warningFill)
    }
}
