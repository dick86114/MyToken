package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenUpdateStarIcon
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.isMyTokenDarkTheme
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    summaryTone: HomeHealthTone,
    alertProviders: List<ProviderId>,
    collapseProgress: () -> Float,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val darkTheme = isMyTokenDarkTheme()
    // 进度只在绘制/图层阶段读取；滚动时这里不会整棵导航重组。
    fun progress(): Float = collapseProgress().coerceIn(0f, 1f)

    val summaryVisible by remember {
        derivedStateOf { progress() > 0.01f }
    }
    val collapsed by remember {
        derivedStateOf { progress() > 0.5f }
    }
    // 页面上滑时整页背景由画布色过渡到报告票面黄，导航保持透明；
    // 品牌字标上移淡出，报告摘要与报警供应商 logo 从右侧淡入。
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
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
                    contentDescription = if (collapsed) {
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
                            .graphicsLayer { alpha = 1f - progress() }
                            .testTag("home_brand"),
                    )
                    // 黄色导航上用黑色标，避免深色外观的白标失去对比。
                    MyTokenBrandMark(
                        size = 28.dp,
                        darkTheme = false,
                        modifier = Modifier.graphicsLayer { alpha = progress() },
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
                            val p = progress()
                            alpha = (1f - p * 2.2f).coerceIn(0f, 1f)
                            translationY = -6.dp.toPx() * p
                        }
                        .testTag("home_wordmark"),
                    style = MyTokenType.section,
                    fontWeight = FontWeight(700),
                    color = colors.ink,
                    maxLines = 1,
                )
                if (summaryVisible) {
                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                val p = progress()
                                val alpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
                                this.alpha = alpha
                                translationY = (1f - alpha) * 4.dp.toPx()
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
                            color = severityAccent(
                                summaryTone,
                                colors.reportTicketMuted,
                                colors.reportNavCritical,
                            ),
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
            if (darkTheme) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { alpha = 1f - progress() },
                )
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = colors.reportTicketInk,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { alpha = progress() },
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (hasUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.reportTicket)
                        .border(1.dp, colors.surface, CircleShape)
                        .testTag("home_update_badge"),
                )
            }
        }
    }
}

/** 启动时检查到新版本后，压在凭证报告上方的黄色提醒条。 */
@Composable
internal fun HomeUpdateBanner(
    version: String?,
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(16.dp)
    val title = version
        ?.takeIf { it.isNotBlank() }
        ?.let { "发现新版本 $it" }
        ?: "发现新版本"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.reportTicket, shape)
            .clickable(role = Role.Button, onClick = onOpenUpdate)
            .semantics { contentDescription = "有新版本可用，查看更新日志并更新" }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("home_update_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MyTokenUpdateStarIcon(
            modifier = Modifier
                .size(30.dp)
                .testTag("home_update_star"),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
            )
            Text(
                text = "查看更新日志并确认安装",
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
        Text(
            text = "点击更新",
            modifier = Modifier
                .padding(end = 10.dp)
                .testTag("home_update_action"),
            style = MyTokenType.caption,
            fontWeight = FontWeight.SemiBold,
            color = colors.actionText,
            maxLines = 1,
        )
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
            .height(42.dp)
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
internal fun ProviderFilter(
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
                .height(42.dp)
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
        containerColor = colors.overlay,
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
            // 供应商筛选是底部浮层，选项块属于浮层内的元素。
            .background(colors.overlay)
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
            .background(colors.overlay)
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
        tint = MyTokenThemeTokens.colors.actionText,
        modifier = modifier.size(14.dp),
    )
}

/** 密度切换在手机/横屏报告右侧是横条，在大屏报告右上角默认竖排。 */
internal enum class DensitySwitchOrientation { Horizontal, Vertical }

/** 设计稿 §5：32dp 轨道 + 34×28dp 按钮，选中块随分页进度滑动。 */
private object DensitySwitchSpec {
    val trackWidth = 74.dp
    val trackHeight = 32.dp
    val verticalTrackWidth = 32.dp
    val verticalTrackHeight = 74.dp
    val trackRadius = 16.dp
    val padding = 2.dp
    val gap = 2.dp
    val itemWidth = 34.dp
    val itemHeight = 28.dp
    val verticalItemWidth = 28.dp
    val verticalItemHeight = 34.dp
    val itemRadius = 14.dp
    val iconSize = 13.dp
}

@Composable
internal fun DensityModeSwitch(
    density: UsageCardDensity,
    onToggle: () -> Unit,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    orientation: DensitySwitchOrientation = DensitySwitchOrientation.Horizontal,
) {
    val compact = density.isCompact()
    val colors = MyTokenThemeTokens.colors
    val vertical = orientation == DensitySwitchOrientation.Vertical
    val trackWidth = if (vertical) {
        DensitySwitchSpec.verticalTrackWidth
    } else {
        DensitySwitchSpec.trackWidth
    }
    val trackHeight = if (vertical) {
        DensitySwitchSpec.verticalTrackHeight
    } else {
        DensitySwitchSpec.trackHeight
    }
    val itemWidth = if (vertical) {
        DensitySwitchSpec.verticalItemWidth
    } else {
        DensitySwitchSpec.itemWidth
    }
    val itemHeight = if (vertical) {
        DensitySwitchSpec.verticalItemHeight
    } else {
        DensitySwitchSpec.itemHeight
    }
    val itemTravelPx = with(LocalDensity.current) {
        val travel = if (vertical) {
            DensitySwitchSpec.verticalItemHeight + DensitySwitchSpec.gap
        } else {
            DensitySwitchSpec.itemWidth + DensitySwitchSpec.gap
        }
        travel.toPx()
    }
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(DensitySwitchSpec.trackRadius))
            // 轨道用卡片 surface：浅色白底、深色卡片深色，和页面协调。
            .background(colors.surface)
            .testTag("usage_density_toggle"),
    ) {
        Box(
            modifier = Modifier
                .padding(DensitySwitchSpec.padding)
                .width(itemWidth)
                .height(itemHeight)
                // 只用 draw 阶段位移，拖动分页时不会整块重新布局而闪动。
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    if (vertical) translationY = itemTravelPx * p else translationX = itemTravelPx * p
                }
                .clip(RoundedCornerShape(DensitySwitchSpec.itemRadius))
                // 选中块与报告票头同色：两套外观都是琥珀，图标用琥珀上的深色 ink。
                .background(colors.reportTicket)
                .testTag("usage_density_thumb"),
        )
        if (vertical) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DensitySwitchSpec.padding)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(DensitySwitchSpec.gap),
            ) {
                DensityIconOption(
                    compact = true,
                    selected = compact,
                    testTag = "usage_density_option_compact",
                    onClick = { if (!compact) onToggle() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                DensityIconOption(
                    compact = false,
                    selected = !compact,
                    testTag = "usage_density_option_full",
                    onClick = { if (compact) onToggle() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DensitySwitchSpec.padding)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(DensitySwitchSpec.gap),
            ) {
                DensityIconOption(
                    compact = true,
                    selected = compact,
                    testTag = "usage_density_option_compact",
                    onClick = { if (!compact) onToggle() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                DensityIconOption(
                    compact = false,
                    selected = !compact,
                    testTag = "usage_density_option_full",
                    onClick = { if (compact) onToggle() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DensityIconOption(
    compact: Boolean,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = modifier
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
        // 分页过程中滑块已跟手；图标直接使用当前选中态，禁止加载动画。
        DensityGlyph(
            compact = compact,
            tint = if (selected) colors.reportTicketInk else colors.muted,
            emphasized = selected,
        )
    }
}

/**
 * SF Symbol 风格的密度图标：简洁为 `app.grid.2x2`，完整为 `rectangle.grid.1x2`。
 * 统一 24×24 几何缩放到 13dp；选中时线宽略增。
 */
@Composable
private fun DensityGlyph(
    compact: Boolean,
    tint: Color,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(DensitySwitchSpec.iconSize)) {
        val scale = size.width / 24f
        val stroke = Stroke(width = (if (emphasized) 1.9f else 1.7f) * scale)
        val radius = CornerRadius(2.2f * scale)
        if (compact) {
            val cell = 7.2f * scale
            listOf(3.5f, 13.3f).forEach { x ->
                listOf(3.5f, 13.3f).forEach { y ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(x * scale, y * scale),
                        size = Size(cell, cell),
                        cornerRadius = radius,
                        style = stroke,
                    )
                }
            }
        } else {
            drawRoundRect(
                color = tint,
                topLeft = Offset(3.5f * scale, 3.5f * scale),
                size = Size(17f * scale, 17f * scale),
                cornerRadius = CornerRadius(2.6f * scale),
                style = stroke,
            )
            drawLine(
                color = tint,
                start = Offset(3.5f * scale, 12f * scale),
                end = Offset(20.5f * scale, 12f * scale),
                strokeWidth = stroke.width,
            )
        }
    }
}
