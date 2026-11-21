package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenDropdownMenu
import ai.routin.mytoken.core.ui.MyTokenDropdownMenuItem
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.roundToInt

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
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
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
                .widthIn(min = MyTokenSize.touch)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onOpenCredentials)
                .semantics { contentDescription = "MyToken，打开凭证列表，共 $credentialCount 个凭证" }
                .testTag("home_open_credentials"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MyTokenBrandMark(
                size = 28.dp,
                modifier = Modifier.testTag("home_brand"),
            )
            Text(
                text = "MyToken",
                modifier = Modifier.testTag("home_wordmark"),
                style = MyTokenType.section,
                fontWeight = FontWeight(700),
                color = colors.ink,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
                tint = colors.ink,
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                modifier = Modifier.size(18.dp),
            )
        }
        MyTokenDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MyTokenDropdownMenuItem(
                text = "全部",
                count = allTotal.toString(),
                selected = selectedProvider == null,
                onClick = {
                    expanded = false
                    onSelect(null)
                },
                modifier = Modifier.testTag("provider_filter_item_all"),
            )
            providers.forEach { provider ->
                MyTokenDropdownMenuItem(
                    text = ProviderCatalog.displayName(provider),
                    count = (counts[provider] ?: 0).toString(),
                    selected = provider == selectedProvider,
                    onClick = {
                        expanded = false
                        onSelect(provider)
                    },
                    modifier = Modifier.testTag("provider_filter_item_${provider.rawValue}"),
                )
            }
        }
    }
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
    BoxWithConstraints(
        modifier = modifier
            .width(104.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(13.dp))
            .testTag("usage_density_toggle"),
    ) {
        val inset = 3.dp
        val segmentWidth = (maxWidth - inset * 2) / 2
        val segmentHeight = maxHeight - inset * 2
        val insetPx = with(LocalDensity.current) { inset.toPx() }
        val segmentWidthPx = with(LocalDensity.current) { segmentWidth.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = insetPx.roundToInt() +
                            (segmentWidthPx * progress().coerceIn(0f, 1f)).roundToInt(),
                        y = insetPx.roundToInt(),
                    )
                }
                .width(segmentWidth)
                .height(segmentHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.ink)
                .testTag("usage_density_thumb"),
        )
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
        DensityGlyph(
            compact = compact,
            tint = if (selected) colors.canvas else colors.muted,
        )
    }
}

@Composable
private fun DensityGlyph(
    compact: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val padding = 1.5.dp.toPx()
        val gap = 2.5.dp.toPx()
        val radius = CornerRadius(1.6.dp.toPx())
        if (compact) {
            val cellWidth = (size.width - padding * 2 - gap) / 2
            val cellHeight = size.height - padding * 2
            drawRoundRect(
                color = tint,
                topLeft = Offset(padding, padding),
                size = Size(cellWidth, cellHeight),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(padding + cellWidth + gap, padding),
                size = Size(cellWidth, cellHeight),
                cornerRadius = radius,
            )
        } else {
            val cellWidth = size.width - padding * 2
            val cellHeight = (size.height - padding * 2 - gap * 2) / 3
            repeat(3) { index ->
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(padding, padding + index * (cellHeight + gap)),
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}
