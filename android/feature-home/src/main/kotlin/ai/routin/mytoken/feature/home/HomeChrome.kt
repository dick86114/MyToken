package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.isMyTokenDarkTheme
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlinx.coroutines.launch

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

/** 固定头部：品牌 / 凭证入口 + 设置入口。 */
@Composable
internal fun HomeToolbar(
    credentialCount: Int,
    headlineCount: Int,
    summaryTail: String,
    alertProviders: List<ProviderId>,
    collapseProgress: Float,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val darkTheme = isMyTokenDarkTheme()
    val progress = collapseProgress.coerceIn(0f, 1f)
    // 字标先退场、摘要再进场，避免两段文案在中间状态叠在一起。
    val wordmarkAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
    val summaryAlpha = ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
    // 页面上滑时导航底色由画布色过渡到报告票面黄，logo 保持原位；
    // 品牌字标上移淡出，报告摘要与报警供应商 logo 从右侧淡入。
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(lerp(colors.canvas, colors.reportTicket, progress))
            .testTag("home_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onOpenCredentials)
                .semantics {
                    contentDescription = if (progress > 0.5f) {
                        "MyToken，$headlineCount 个凭证$summaryTail，打开凭证列表"
                    } else {
                        "MyToken，打开凭证列表，共 $credentialCount 个凭证"
                    }
                }
                .testTag("home_open_credentials"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(28.dp)) {
                if (darkTheme) {
                    MyTokenBrandMark(
                        size = 28.dp,
                        modifier = Modifier
                            .alpha(1f - progress)
                            .testTag("home_brand"),
                    )
                    // 黄色导航上用黑色标，避免深色外观的白标失去对比。
                    MyTokenBrandMark(
                        size = 28.dp,
                        darkTheme = false,
                        modifier = Modifier.alpha(progress),
                    )
                } else {
                    MyTokenBrandMark(
                        size = 28.dp,
                        modifier = Modifier.testTag("home_brand"),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(start = 9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "MyToken",
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = wordmarkAlpha
                            translationY = -6.dp.toPx() * progress
                        }
                        .testTag("home_wordmark"),
                    style = MyTokenType.section,
                    fontWeight = FontWeight(700),
                    color = colors.ink,
                    maxLines = 1,
                )
                if (summaryAlpha > 0.01f) {
                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = summaryAlpha
                                translationY = (1f - summaryAlpha) * 4.dp.toPx()
                            }
                            .testTag("home_nav_summary"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "${headlineCount}个凭证",
                            modifier = Modifier.testTag("home_nav_summary_count"),
                            style = MyTokenType.label,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.reportTicketInk,
                            maxLines = 1,
                        )
                        Text(
                            text = summaryTail,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("home_nav_summary_tail"),
                            style = MyTokenType.caption,
                            fontWeight = FontWeight.Medium,
                            color = colors.reportTicketMuted,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        alertProviders.take(4).forEachIndexed { index, provider ->
                            ProviderLogoPlate(
                                painter = painterResource(ProviderCatalog.logoRes(provider)),
                                contentDescription = null,
                                size = 20.dp,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = 2.dp,
                                modifier = Modifier.testTag("home_nav_alert_logo_$index"),
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
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
                tint = lerp(colors.ink, colors.reportTicketInk, progress),
                modifier = Modifier.size(20.dp),
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

/** 报告下方的供应商筛选 + 纯图标密度滑块。 */
@Composable
internal fun HomeProviderToolbar(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    allTotal: Int,
    onSelect: (ProviderId?) -> Unit,
    density: UsageCardDensity,
    onToggleDensity: () -> Unit,
    densityProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("home_provider_toolbar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProviderFilter(
            selectedProvider = selectedProvider,
            providers = providers,
            counts = counts,
            allTotal = allTotal,
            onSelect = onSelect,
        )
        DensityModeSwitch(
            density = density,
            onToggle = onToggleDensity,
            progress = densityProgress,
        )
    }
}

@Composable
private fun ProviderFilter(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    allTotal: Int,
    onSelect: (ProviderId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    var expanded by remember { mutableStateOf(false) }
    val filterLabel = selectedProvider
        ?.let { ProviderCatalog.displayName(it) }
        ?: "全部供应商"
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = MyTokenSize.touch)
                .clickable(role = Role.Button) { expanded = true }
                .semantics { selected = selectedProvider != null }
                .testTag("provider_filter_menu"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = filterLabel,
                style = MyTokenType.section,
                color = colors.ink,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    if (expanded) {
        ProviderPickerSheet(
            selectedProvider = selectedProvider,
            providers = providers,
            counts = counts,
            allTotal = allTotal,
            onSelect = onSelect,
            onDismiss = { expanded = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPickerSheet(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    allTotal: Int,
    onSelect: (ProviderId?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 先播放收起动画再移除，避免 dialog 窗口被直接丢弃。
    val dismissThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(
            topStart = MyTokenRadius.sheetTop,
            topEnd = MyTokenRadius.sheetTop,
        ),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(MyTokenRadius.pill))
                        .background(colors.line),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .testTag("provider_filter_sheet"),
        ) {
            Text(
                text = "选择供应商",
                style = MyTokenType.section,
                fontWeight = FontWeight(700),
                color = colors.ink,
            )
            Text(
                text = "仅筛选下方用量卡片，报告始终展示全部凭证",
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
                style = MyTokenType.caption,
                color = colors.muted,
            )
            ProviderAllTile(
                selected = selectedProvider == null,
                count = allTotal,
                onClick = {
                    onSelect(null)
                    dismissThen(onDismiss)
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            providers.chunked(2).forEach { rowProviders ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowProviders.forEach { provider ->
                        ProviderSheetTile(
                            provider = provider,
                            count = counts[provider] ?: 0,
                            selected = provider == selectedProvider,
                            onClick = {
                                onSelect(provider)
                                dismissThen(onDismiss)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowProviders.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "取消",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(MyTokenRadius.pill))
                    .clickable { dismissThen(onDismiss) }
                    .padding(vertical = 12.dp)
                    .testTag("provider_filter_cancel"),
                style = MyTokenType.label,
                fontWeight = FontWeight(600),
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProviderAllTile(
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.actionFill else colors.line,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("provider_filter_item_all")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "全部供应商",
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(
                text = "$count 个凭证",
                modifier = Modifier.padding(top = 2.dp),
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }
        if (selected) {
            SelectedCheck()
        }
    }
}

@Composable
private fun ProviderSheetTile(
    provider: ProviderId,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.actionFill else colors.line,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "${ProviderCatalog.displayName(provider)}，$count 个凭证"
            }
            .testTag("provider_filter_item_${provider.rawValue}")
            .padding(12.dp),
    ) {
        Column {
            ProviderLogoPlate(
                painter = painterResource(ProviderCatalog.logoRes(provider)),
                contentDescription = null,
                size = 26.dp,
                shape = RoundedCornerShape(7.dp),
                contentPadding = 3.dp,
            )
            Text(
                text = ProviderCatalog.displayName(provider),
                modifier = Modifier.padding(top = 6.dp),
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
            )
            Text(
                text = "$count 个凭证",
                modifier = Modifier.padding(top = 2.dp),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
        if (selected) {
            SelectedCheck(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun SelectedCheck(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint = MyTokenThemeTokens.colors.actionFill,
        modifier = modifier.size(14.dp),
    )
}

/** 设计稿的纯图标密度滑块：双列 / 单列图标，选中块随分页进度滑动。 */
@Composable
private fun DensityModeSwitch(
    density: UsageCardDensity,
    onToggle: () -> Unit,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val compact = density.isCompact()
    val colors = MyTokenThemeTokens.colors
    // 视觉高度保持设计稿的 42dp，但把触控区扩展到 48dp；滑块仍在 42dp 轨道内移动。
    Box(
        modifier = modifier
            .width(104.dp)
            .height(MyTokenSize.touch)
            .testTag("usage_density_toggle"),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(104.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(13.dp)),
        ) {
            val inset = 3.dp
            val segmentWidth = (maxWidth - inset * 2) / 2
            val segmentHeight = maxHeight - inset * 2
            val segmentWidthPx = with(LocalDensity.current) { segmentWidth.toPx() }
            Box(
                modifier = Modifier
                    .offset(x = inset, y = inset)
                    .width(segmentWidth)
                    .height(segmentHeight)
                    // 只用 draw 阶段位移，拖动分页时不会整块重新布局而闪动。
                    .graphicsLayer {
                        translationX = segmentWidthPx * progress().coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.ink)
                    .testTag("usage_density_thumb"),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            DensityIconOption(
                compact = true,
                selected = compact,
                testTag = "usage_density_option_compact",
                onClick = { if (!compact) onToggle() },
            )
            DensityIconOption(
                compact = false,
                selected = !compact,
                testTag = "usage_density_option_full",
                onClick = { if (compact) onToggle() },
            )
        }
    }
}

@Composable
private fun RowScope.DensityIconOption(
    compact: Boolean,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .weight(1f)
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
        // 选中态在落页时切换；用短过渡代替瞬时反色，避免整个切换控件闪一下。
        val tint by animateColorAsState(
            targetValue = if (selected) colors.canvas else colors.muted,
            animationSpec = tween(durationMillis = 180),
            label = "density-glyph-tint",
        )
        DensityGlyph(
            compact = compact,
            tint = tint,
            emphasized = selected,
        )
    }
}

/**
 * 按设计稿 24×24 图标几何等比缩放到 19dp：简洁为两块竖版圆角矩形，
 * 完整为三条横向圆角矩形；选中时线宽从 1.7 提到 1.9。
 */
@Composable
private fun DensityGlyph(
    compact: Boolean,
    tint: Color,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val scale = size.width / 24f
        val stroke = Stroke(width = (if (emphasized) 1.9f else 1.7f) * scale)
        if (compact) {
            val top = 4f * scale
            val height = 16f * scale
            val width = 7.5f * scale
            val radius = CornerRadius(2f * scale)
            listOf(3.5f, 13f).forEach { left ->
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left * scale, top),
                    size = Size(width, height),
                    cornerRadius = radius,
                    style = stroke,
                )
            }
        } else {
            val width = 17f * scale
            val height = 4.5f * scale
            val radius = CornerRadius(1.6f * scale)
            repeat(3) { index ->
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(3.5f * scale, (4f + index * 6f) * scale),
                    size = Size(width, height),
                    cornerRadius = radius,
                    style = stroke,
                )
            }
        }
    }
}
