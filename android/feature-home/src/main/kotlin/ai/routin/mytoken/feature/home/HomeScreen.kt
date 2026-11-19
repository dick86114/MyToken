package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.GlassButton
import ai.routin.mytoken.core.ui.GlassButtonTone
import ai.routin.mytoken.core.ui.GlassHelpButton
import ai.routin.mytoken.core.ui.HelpMarker
import ai.routin.mytoken.core.ui.HelpRow
import ai.routin.mytoken.core.ui.HelpSection
import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenBrandMark
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenPalette
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.core.ui.adaptiveGridColumns
import ai.routin.mytoken.core.ui.maxColumns
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageMetric
import ai.routin.mytoken.domain.model.UsageMetricHealthState
import ai.routin.mytoken.domain.model.UsageMetricPresentation
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefreshAll: () -> Unit,
    onRefreshCredential: (UUID) -> Unit,
    onRetryCredential: (UUID) -> Unit = onRefreshCredential,
    onOpenCredential: (UUID) -> Unit,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
    onOpenCredentials: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    hasUpdateAvailable: Boolean = false,
    updateVersion: String? = null,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
    usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    onToggleUsageCardDensity: () -> Unit = {},
) {
    var selectedProvider by remember { mutableStateOf<ProviderId?>(null) }
    val allCards = remember(state.cards) {
        state.cards.filter { it.status != RefreshStatus.Disabled }
    }
    val providerCounts = remember(allCards) {
        allCards.groupingBy { it.credential.providerId }.eachCount()
    }
    val visibleProviders = remember(state.groups, providerCounts) {
        state.groups.map { it.providerId }.filter { (providerCounts[it] ?: 0) > 0 }
    }
    val visibleCards = remember(allCards, selectedProvider) {
        selectedProvider
            ?.let { provider -> allCards.filter { it.credential.providerId == provider } }
            ?: allCards
    }
    val gridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    var highlightedCredentialId by remember { mutableStateOf<UUID?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        when {
            state.isLoading -> Column(
                modifier = contentModifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                HomeHeader(
                    credentialCount = state.cards.size,
                    usageCardDensity = usageCardDensity,
                    onToggleUsageCardDensity = onToggleUsageCardDensity,
                    onOpenCredentials = onOpenCredentials,
                    onOpenSettings = onOpenSettings,
                    hasUpdateAvailable = hasUpdateAvailable,
                    showDensitySwitch = false,
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            allCards.isEmpty() -> Column(
                modifier = contentModifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                HomeHeader(
                    credentialCount = state.cards.size,
                    usageCardDensity = usageCardDensity,
                    onToggleUsageCardDensity = onToggleUsageCardDensity,
                    onOpenCredentials = onOpenCredentials,
                    onOpenSettings = onOpenSettings,
                    hasUpdateAvailable = hasUpdateAvailable,
                    showDensitySwitch = false,
                )
                EmptyState(
                    modifier = Modifier.weight(1f),
                    onImportFromMac = onImportFromMac,
                    onAddManually = onAddManually,
                )
            }

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshingAll,
                onRefresh = onRefreshAll,
                modifier = contentModifier.testTag("home_pull_refresh"),
            ) {
                MyTokenAdaptiveContent(
                    maxWidth = 1440.dp,
                    horizontalPadding = 16.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = adaptiveGridColumns(
                            availableWidth = maxWidth,
                            maxColumns = layoutMode.maxColumns(1, 2, 3),
                            minItemWidth = if (usageCardDensity.isCompact()) 210.dp else 260.dp,
                            spacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        )
                        LazyVerticalStaggeredGrid(
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_grid_${columns}_columns"),
                            columns = StaggeredGridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                top = 12.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = if (usageCardDensity.isCompact()) 8.dp else 12.dp,
                        ) {
                            item(
                                key = "home_header",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                HomeHeader(
                                    credentialCount = state.cards.size,
                                    usageCardDensity = usageCardDensity,
                                    onToggleUsageCardDensity = onToggleUsageCardDensity,
                                    onOpenCredentials = onOpenCredentials,
                                    onOpenSettings = onOpenSettings,
                                    hasUpdateAvailable = hasUpdateAvailable,
                                )
                            }
                            if (hasUpdateAvailable) {
                                item(
                                    key = "home_update_banner",
                                    span = StaggeredGridItemSpan.FullLine,
                                ) {
                                    HomeUpdateBanner(
                                        version = updateVersion,
                                        onOpenSettings = onOpenSettings,
                                    )
                                }
                            }
                            item(
                                key = "home_health",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                HealthSummary(
                                    cards = visibleCards,
                                    lastUpdatedText = state.lastUpdatedText ?: "刚刚同步",
                                    onLocate = { id ->
                                        val index = visibleCards.indexOfFirst {
                                            it.credential.id == id
                                        }
                                        if (index >= 0) {
                                            scope.launch {
                                                gridState.animateToItemCentered(3 + index)
                                                highlightedCredentialId = id
                                                flashTick += 1
                                                delay(1_000)
                                                if (highlightedCredentialId == id) {
                                                    highlightedCredentialId = null
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                            item(
                                key = "home_filters",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                ProviderFilterRow(
                                    selectedProvider = selectedProvider,
                                    providers = visibleProviders,
                                    counts = providerCounts,
                                    total = allCards.size,
                                    onSelect = { selectedProvider = it },
                                )
                            }
                            staggeredItems(
                                visibleCards,
                                key = { it.credential.id },
                                contentType = { "credential-usage-card" },
                            ) { card ->
                                val isHighlighted =
                                    highlightedCredentialId == card.credential.id
                                CredentialUsageCard(
                                    card = card,
                                    metricColumns = 2,
                                    layoutMode = layoutMode,
                                    usageCardDensity = usageCardDensity,
                                    highlightSignal = if (isHighlighted) flashTick else 0,
                                    onOpen = { onOpenCredential(card.credential.id) },
                                    onRefresh = { onRefreshCredential(card.credential.id) },
                                    onRetry = { onRetryCredential(card.credential.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    credentialCount: Int,
    usageCardDensity: UsageCardDensity,
    onToggleUsageCardDensity: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUpdateAvailable: Boolean,
    showDensitySwitch: Boolean = true,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClick = onOpenCredentials)
                    .semantics {
                        contentDescription = "打开凭证列表，共 $credentialCount 个凭证"
                    }
                    .testTag("home_open_credentials"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                if (credentialCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .zIndex(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = credentialCount.coerceAtMost(99).toString(),
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
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
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                if (hasUpdateAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MyTokenPalette.warning)
                            .testTag("home_update_badge"),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "账户用量",
                    fontSize = 30.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.1).sp,
                )
            }
            if (showDensitySwitch) {
                DensitySwitch(
                    density = usageCardDensity,
                    onToggle = onToggleUsageCardDensity,
                )
            }
        }

    }
}

@Composable
private fun DensitySwitch(
    density: UsageCardDensity,
    onToggle: () -> Unit,
) {
    val compact = density.isCompact()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val trackColor = if (dark) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color(0xFF17222C).copy(alpha = 0.055f)
    }
    val thumbColor = if (dark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    BoxWithConstraints(
        modifier = Modifier
            .testTag("usage_density_toggle")
            .width(152.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor)
            .padding(2.dp),
    ) {
        val optionWidth = maxWidth / 2
        val thumbOffset by animateDpAsState(
            targetValue = if (compact) 0.dp else optionWidth,
            animationSpec = spring(
                dampingRatio = 1f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "densityThumbOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(optionWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(thumbColor),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            DensityOption(
                text = "简洁",
                compact = true,
                selected = compact,
                testTag = "usage_density_option_compact",
                modifier = Modifier.weight(1f),
                onClick = { if (!compact) onToggle() },
            )
            DensityOption(
                text = "完整",
                compact = false,
                selected = !compact,
                testTag = "usage_density_option_full",
                modifier = Modifier.weight(1f),
                onClick = { if (compact) onToggle() },
            )
        }
    }
}

@Composable
private fun DensityOption(
    text: String,
    compact: Boolean,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "densityOptionColor",
    )
    Row(
        modifier = modifier
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        DensityGlyph(compact = compact, color = contentColor)
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
private fun DensityGlyph(compact: Boolean, color: Color) {
    Column(
        modifier = Modifier.size(14.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = if (compact) 4.dp else 2.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(if (compact) 2 else 3) {
            Box(
                modifier = Modifier
                    .width(if (compact) 9.dp else 11.dp)
                    .height(2.dp)
                    .background(color, RoundedCornerShape(1.dp)),
            )
        }
    }
}

/**
 * 首页更新提醒条：应用启动时会检查 GitHub Release，发现新版本后在首页顶部提示。
 * 点击进入设置页，由设置页的更新卡片负责下载与安装。
 */
@Composable
private fun HomeUpdateBanner(
    version: String?,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClick = onOpenSettings)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "有新版本可用，前往设置查看并更新" }
            .testTag("home_update_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.SystemUpdate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = version?.takeIf { it.isNotBlank() }?.let { "发现新版本 $it" } ?: "发现新版本",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "前往设置查看更新内容并安装",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
        }
        Text(
            text = "去更新",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun HealthSummary(
    cards: List<CredentialCardUi>,
    lastUpdatedText: String,
    onLocate: (UUID) -> Unit,
) {
    val healthyCount = cards.count { healthTone(it) == HealthTone.Normal }
    val total = cards.size
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "账户健康",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = healthyCount.toString(),
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = " / $total",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SyncPill(text = lastUpdatedText)
                    GlassHelpButton(
                        title = "账户健康",
                        subtitle = "按首页顺序排列，点击短条定位卡片",
                        sections = listOf(
                            HelpSection(
                                title = "颜色",
                                rows = listOf(
                                    HelpRow(
                                        label = "正常",
                                        detail = "余量充足",
                                        marker = HelpMarker.Positive,
                                    ),
                                    HelpRow(
                                        label = "接近上限",
                                        detail = "建议留意",
                                        marker = HelpMarker.Warning,
                                    ),
                                    HelpRow(
                                        label = "已超限 / 不可用",
                                        detail = "建议处理",
                                        marker = HelpMarker.Critical,
                                    ),
                                ),
                            ),
                            HelpSection(
                                title = "判定阈值",
                                rows = listOf(
                                    HelpRow("通用进度", "80% 标红 · 50% 标橙"),
                                    HelpRow("小米 MiMo", "90% 标红 · 70% 标橙"),
                                    HelpRow("余额", "小于等于 0 标红"),
                                ),
                            ),
                        ),
                        modifier = Modifier.testTag("health_help"),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                cards.forEachIndexed { index, card ->
                    val tone = healthTone(card)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLocate(card.credential.id) }
                            .semantics {
                                contentDescription =
                                    "${card.credential.name}，${healthToneLabel(tone)}"
                            }
                            .testTag("health_segment_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ProviderLogoPlate(
                            painter = painterResource(
                                ProviderCatalog.logoRes(card.credential.providerId),
                            ),
                            contentDescription = null,
                            size = 20.dp,
                            shape = RoundedCornerShape(5.dp),
                            contentPadding = 2.dp,
                            modifier = Modifier
                                .testTag("health_provider_logo_${card.credential.providerId.rawValue}"),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(healthToneColor(tone)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncPill(text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(MyTokenPalette.positiveLight, CircleShape),
        )
        Text(
            text = text,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderFilterRow(
    selectedProvider: ProviderId?,
    providers: List<ProviderId>,
    counts: Map<ProviderId, Int>,
    total: Int,
    onSelect: (ProviderId?) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProviderChip(
            text = "全部 $total",
            selected = selectedProvider == null,
            testTag = "provider_filter_menu",
            onClick = { onSelect(null) },
        )
        providers.forEach { provider ->
            ProviderChip(
                text = "${ProviderCatalog.displayName(provider)} ${counts[provider] ?: 0}",
                selected = selectedProvider == provider,
                testTag = "provider_filter_item_${provider.rawValue}",
                onClick = { onSelect(provider) },
            )
        }
    }
}

@Composable
private fun ProviderChip(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) MyTokenPalette.lightText else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        label = "providerChipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "providerChipContent",
    )
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onImportFromMac: () -> Unit,
    onAddManually: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MyTokenBrandMark(size = 54.dp)
        Text(
            text = "暂无凭证",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        GlassButton(
            onClick = onImportFromMac,
            tone = GlassButtonTone.Primary,
            text = "从 Mac 导入",
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlassButton(
            onClick = onAddManually,
            text = "添加凭证",
        )
    }
}

private suspend fun LazyStaggeredGridState.animateToItemCentered(index: Int) {
    val viewportHeight = layoutInfo.viewportSize.height
    animateScrollToItem(
        index = index,
        scrollOffset = -(viewportHeight / 3),
    )
    var item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        delay(16)
        item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return

    val viewportCenter = layoutInfo.viewportSize.height / 2f
    val itemCenter = item.offset.y + item.size.height / 2f
    val delta = itemCenter - viewportCenter
    if (abs(delta) > 1f) {
        animateScrollBy(
            value = delta,
            animationSpec = tween(
                durationMillis = 280,
                easing = FastOutSlowInEasing,
            ),
        )
    }
}

private enum class HealthTone { Normal, Warning, Critical }

private fun healthTone(card: CredentialCardUi): HealthTone {
    if (card.status == RefreshStatus.Failed || card.error != null) return HealthTone.Critical
    val tones = card.snapshot?.metrics.orEmpty().mapNotNull(::metricHealthTone)
    return when {
        tones.any { it == HealthTone.Critical } -> HealthTone.Critical
        tones.any { it == HealthTone.Warning } -> HealthTone.Warning
        else -> HealthTone.Normal
    }
}

private fun metricHealthTone(metric: UsageMetric): HealthTone? = when (metric.healthState) {
    UsageMetricHealthState.Critical,
    UsageMetricHealthState.Unavailable -> HealthTone.Critical

    UsageMetricHealthState.Warning,
    UsageMetricHealthState.Stale -> HealthTone.Warning

    UsageMetricHealthState.Normal -> HealthTone.Normal
    UsageMetricHealthState.Unknown -> fallbackHealthTone(metric)
}

private fun fallbackHealthTone(metric: UsageMetric): HealthTone? {
    if (metric.presentation == UsageMetricPresentation.Progress) {
        val used = metric.used?.toDouble() ?: return null
        val limit = metric.limit?.toDouble() ?: return null
        if (limit <= 0.0) return HealthTone.Normal
        val percent = used / limit * 100.0
        return when {
            percent >= 80.0 -> HealthTone.Critical
            percent >= 50.0 -> HealthTone.Warning
            else -> HealthTone.Normal
        }
    }
    if (metric.presentation == UsageMetricPresentation.Balance) {
        return if ((metric.value ?: return null) <= java.math.BigDecimal.ZERO) {
            HealthTone.Critical
        } else {
            HealthTone.Normal
        }
    }
    return null
}

private fun healthToneColor(tone: HealthTone): Color = when (tone) {
    HealthTone.Normal -> MyTokenPalette.positiveLight
    HealthTone.Warning -> MyTokenPalette.warning
    HealthTone.Critical -> MyTokenPalette.critical
}

private fun healthToneLabel(tone: HealthTone): String = when (tone) {
    HealthTone.Normal -> "正常"
    HealthTone.Warning -> "需注意"
    HealthTone.Critical -> "已超限"
}
